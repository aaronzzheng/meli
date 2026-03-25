package com.example.meli.data.repository

import android.util.Base64
import com.example.meli.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun getCurrentUser() = auth.currentUser

    suspend fun register(email: String, pass: String, displayName: String = ""): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val firebaseUser = result.user ?: throw Exception("User creation failed")
            val normalizedName = displayName.trim()
            if (normalizedName.isNotBlank()) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(normalizedName)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
            }
            val username = email.substringBefore("@")
            val savedDisplayName = normalizedName.ifBlank { firebaseUser.displayName.orEmpty() }
            val user = User(
                uid = firebaseUser.uid,
                email = email,
                displayName = savedDisplayName,
                username = username,
                createdAt = Timestamp.now()
            )
            firestore.collection("users").document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, pass: String): Result<Unit> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            result.user?.let { syncUserProfileToFirestore(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDisplayName(newName: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No signed-in user")
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()
            user.updateProfile(profileUpdates).await()
            firestore.collection("users")
                .document(user.uid)
                .set(mapOf("displayName" to newName), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEmail(newEmail: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No signed-in user")
            user.updateEmail(newEmail).await()
            firestore.collection("users")
                .document(user.uid)
                .set(
                    mapOf(
                        "email" to newEmail,
                        "username" to newEmail.substringBefore("@")
                    ),
                    SetOptions.merge()
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No signed-in user")
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadProfilePhoto(imageBytes: ByteArray): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No signed-in user")
            val encoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            firestore.collection("users")
                .document(user.uid)
                .set(mapOf("profileImageBase64" to encoded), SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfilePhotoBytes(uid: String? = auth.currentUser?.uid): Result<ByteArray?> {
        return try {
            val resolvedUid = uid ?: return Result.success(null)
            val userDoc = firestore.collection("users").document(resolvedUid).get().await()
            val encoded = userDoc.getString("profileImageBase64")
            if (encoded.isNullOrBlank()) {
                return Result.success(null)
            }
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No signed-in user")
            val uid = user.uid
            val userRef = firestore.collection("users").document(uid)

            // Remove nested ranking entries first.
            val rankingLists = userRef.collection("rankingLists").get().await()
            for (listDoc in rankingLists.documents) {
                deleteSubcollection(listDoc.reference, "entries")
                listDoc.reference.delete().await()
            }

            // Remove known per-user subcollections.
            deleteSubcollection(userRef, "friends")
            deleteSubcollection(userRef, "ratings")
            deleteSubcollection(userRef, "tests_manual")

            // Remove user profile document.
            userRef.delete().await()

            // Finally remove Firebase Auth account.
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun deleteSubcollection(parent: DocumentReference, subcollection: String) {
        val snapshot = parent.collection(subcollection).get().await()
        for (doc in snapshot.documents) {
            doc.reference.delete().await()
        }
    }

    fun logout() = auth.signOut()

    private suspend fun syncUserProfileToFirestore(user: FirebaseUser) {
        val email = user.email.orEmpty()
        val username = email.substringBefore("@").takeIf { it.isNotBlank() }.orEmpty()
        val displayName = user.displayName.orEmpty()

        firestore.collection("users")
            .document(user.uid)
            .set(
                mapOf(
                    "uid" to user.uid,
                    "email" to email,
                    "displayName" to displayName,
                    "username" to username,
                    "createdAt" to Timestamp.now()
                ),
                SetOptions.merge()
            )
            .await()
    }
}

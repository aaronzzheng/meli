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

    private fun userDocument(uid: String) = firestore.collection("users").document(uid)

    private fun usernameDocument(username: String) = firestore.collection("usernames")
        .document(username.trim().lowercase())

    private fun emailDocument(email: String) = firestore.collection("emails")
        .document(email.trim().lowercase())

    fun getCurrentUser() = auth.currentUser

    suspend fun getCurrentUsername(): Result<String> {
        return try {
            val user = auth.currentUser ?: throw Exception("No signed-in user")
            val username = firestore.collection("users")
                .document(user.uid)
                .get()
                .await()
                .getString("username")
                .orEmpty()
            Result.success(username)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        email: String,
        pass: String,
        displayName: String = "",
        username: String = ""
    ): Result<Unit> {
        var firebaseUser: FirebaseUser? = null
        return try {
            val normalizedEmail = email.trim()
            val normalizedUsername = username.trim().lowercase()
            validateUsername(normalizedUsername)
            val result = auth.createUserWithEmailAndPassword(normalizedEmail, pass).await()
            firebaseUser = result.user ?: throw Exception("User creation failed")
            val normalizedName = displayName.trim()
            if (normalizedName.isNotBlank()) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(normalizedName)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
            }
            val savedDisplayName = normalizedName.ifBlank { firebaseUser.displayName.orEmpty() }
            val user = User(
                uid = firebaseUser.uid,
                email = normalizedEmail,
                displayName = savedDisplayName,
                username = normalizedUsername,
                createdAt = Timestamp.now()
            )
            val userRef = userDocument(user.uid)
            val usernameRef = usernameDocument(normalizedUsername)
            val emailRef = emailDocument(normalizedEmail)

            firestore.runTransaction { transaction ->
                val existingUsername = transaction.get(usernameRef)
                if (existingUsername.exists() && existingUsername.getString("uid") != user.uid) {
                    throw IllegalArgumentException("Username is already taken")
                }

                val existingEmail = transaction.get(emailRef)
                if (existingEmail.exists() && existingEmail.getString("uid") != user.uid) {
                    throw IllegalArgumentException("Email is already registered")
                }

                transaction.set(userRef, user)
                transaction.set(usernameRef, mapOf("uid" to user.uid))
                transaction.set(emailRef, mapOf("uid" to user.uid))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            firebaseUser?.let { createdUser ->
                runCatching { createdUser.delete().await() }
            }
            Result.failure(e)
        }
    }

    suspend fun login(email: String, pass: String): Result<Unit> {
        return try {
            val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
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
                    mapOf("email" to newEmail),
                    SetOptions.merge()
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUsername(newUsername: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No signed-in user")
            val normalizedUsername = newUsername.trim().lowercase()
            validateUsername(normalizedUsername)

            val userRef = userDocument(user.uid)
            val userSnapshot = userRef.get().await()
            val currentUsername = userSnapshot.getString("username").orEmpty().trim().lowercase()
            val newUsernameRef = usernameDocument(normalizedUsername)

            firestore.runTransaction { transaction ->
                val existingUsername = transaction.get(newUsernameRef)
                if (existingUsername.exists() && existingUsername.getString("uid") != user.uid) {
                    throw IllegalArgumentException("Username is already taken")
                }

                transaction.set(userRef, mapOf("username" to normalizedUsername), SetOptions.merge())
                transaction.set(newUsernameRef, mapOf("uid" to user.uid))

                if (currentUsername.isNotBlank() && currentUsername != normalizedUsername) {
                    transaction.delete(usernameDocument(currentUsername))
                }
            }.await()

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
            val userRef = userDocument(uid)
            val userSnapshot = userRef.get().await()
            val username = userSnapshot.getString("username").orEmpty().trim().lowercase()
            val email = userSnapshot.getString("email")
                ?.takeIf { it.isNotBlank() }
                ?: user.email.orEmpty()

            // Remove nested ranking entries first.
            val rankingLists = userRef.collection("rankingLists").get().await()
            for (listDoc in rankingLists.documents) {
                deleteSubcollection(listDoc.reference, "entries")
                listDoc.reference.delete().await()
            }

            // Remove known per-user subcollections.
            deleteSubcollection(userRef, "friends")
            deleteSubcollection(userRef, "ratings")
            deleteSubcollection(userRef, "notifications")
            deleteSubcollection(userRef, "tests_manual")

            val notes = firestore.collection("notes")
                .whereEqualTo("ownerUid", uid)
                .get()
                .await()
            for (noteDoc in notes.documents) {
                noteDoc.reference.delete().await()
            }

            val usersSnapshot = firestore.collection("users").get().await()
            for (otherUserDoc in usersSnapshot.documents) {
                if (otherUserDoc.id == uid) continue
                val otherUserRef = otherUserDoc.reference

                otherUserRef.collection("friends").document(uid).delete().await()

                val notifications = otherUserRef.collection("notifications")
                    .whereEqualTo("actorUid", uid)
                    .get()
                    .await()
                for (notificationDoc in notifications.documents) {
                    notificationDoc.reference.delete().await()
                }
            }

            // Remove user profile document.
            userRef.delete().await()
            if (username.isNotBlank()) {
                usernameDocument(username).delete().await()
            }
            if (email.isNotBlank()) {
                emailDocument(email).delete().await()
            }

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
        val existingUser = firestore.collection("users")
            .document(user.uid)
            .get()
            .await()
        val username = existingUser.getString("username")
            ?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@").takeIf { it.isNotBlank() }.orEmpty()
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

    private fun validateUsername(username: String) {
        if (username.length !in 3..20) {
            throw IllegalArgumentException("Username must be 3-20 characters")
        }
        if (!username.matches(Regex("^[a-z0-9._]+$"))) {
            throw IllegalArgumentException("Username can only use letters, numbers, dots, and underscores")
        }
    }
}

package com.example.meli.data.repository

import android.net.Uri
import com.example.meli.model.User
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun getCurrentUser() = auth.currentUser

    suspend fun register(email: String, pass: String): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val firebaseUser = result.user ?: throw Exception("User creation failed")
            val user = User(firebaseUser.uid, email, Timestamp.now())
            firestore.collection("users").document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, pass: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEmail(newEmail: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No signed-in user")
            user.updateEmail(newEmail).await()
            firestore.collection("users").document(user.uid).update("email", newEmail).await()
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

    suspend fun uploadProfilePhoto(imageUri: Uri): Result<String> {
        return try {
            val user = auth.currentUser ?: throw Exception("No signed-in user")
            val photoRef = storage.reference.child("profile_pictures/${user.uid}.jpg")
            photoRef.putFile(imageUri).await()
            val downloadUri = photoRef.downloadUrl.await()

            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setPhotoUri(downloadUri)
                    .build()
            ).await()

            firestore.collection("users")
                .document(user.uid)
                .set(mapOf("profileImageUrl" to downloadUri.toString()), SetOptions.merge())
                .await()

            Result.success(downloadUri.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfilePhotoBytes(maxBytes: Long = 5 * 1024 * 1024): Result<ByteArray?> {
        return try {
            val user = auth.currentUser ?: return Result.success(null)
            val userDoc = firestore.collection("users").document(user.uid).get().await()
            val firestoreUrl = userDoc.getString("profileImageUrl")
            val photoUrl = firestoreUrl ?: user.photoUrl?.toString()
            if (photoUrl.isNullOrBlank()) {
                return Result.success(null)
            }
            val bytes = storage.getReferenceFromUrl(photoUrl).getBytes(maxBytes).await()
            Result.success(bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() = auth.signOut()
}

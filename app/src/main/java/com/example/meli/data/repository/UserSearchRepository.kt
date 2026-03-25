package com.example.meli.data.repository

import com.example.meli.model.UserSearchResult
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Locale

class UserSearchRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun searchUsers(query: String, currentUid: String): Result<List<UserSearchResult>> {
        return try {
            val rawQuery = query.trim()
            val normalizedQuery = normalizeQuery(rawQuery)
            val isHandleSearch = rawQuery.startsWith("@")
            if (normalizedQuery.isBlank()) {
                return Result.success(emptyList())
            }

            val friendshipSnapshot = firestore.collection("users")
                .document(currentUid)
                .collection("friends")
                .get()
                .await()
            val friendshipMap = friendshipSnapshot.documents.associate { doc ->
                doc.id to socialRepository.mapFriendshipStatus(
                    status = doc.getString("status"),
                    direction = doc.getString("direction")
                )
            }

            val snapshot = firestore.collection("users").get().await()
            val matches = snapshot.documents
                .asSequence()
                .filter { it.id != currentUid }
                .mapNotNull { doc ->
                    val email = doc.getString("email").orEmpty()
                    val username = doc.getString("username")
                        ?.takeIf { it.isNotBlank() }
                        ?: email.substringBefore("@").takeIf { it.isNotBlank() }.orEmpty()
                    val displayName = doc.getString("displayName")
                        ?.takeIf { it.isNotBlank() }
                        ?: username.ifBlank { "Unknown user" }

                    val normalizedDisplayName = normalizeText(displayName)
                    val normalizedUsername = normalizeText(username)
                    val normalizedEmail = normalizeText(email)
                    val normalizedEmailPrefix = normalizeText(email.substringBefore("@"))

                    val isMatch = if (isHandleSearch) {
                        normalizedUsername.startsWith(normalizedQuery) ||
                            normalizedEmailPrefix.startsWith(normalizedQuery)
                    } else {
                        normalizedDisplayName.contains(normalizedQuery) ||
                            normalizedUsername.contains(normalizedQuery) ||
                            normalizedEmail.contains(normalizedQuery) ||
                            normalizedEmailPrefix.contains(normalizedQuery)
                    }

                    if (!isMatch) {
                        null
                    } else {
                        UserSearchResult(
                            uid = doc.id,
                            displayName = displayName,
                            username = username,
                            email = email,
                            friendshipStatus = friendshipMap[doc.id] ?: com.example.meli.model.FriendshipStatus.NONE
                        )
                    }
                }
                .sortedWith(
                    compareBy<UserSearchResult> {
                        val display = it.displayName.lowercase(Locale.US)
                        val user = it.username.lowercase(Locale.US)
                        val emailPrefix = it.email.substringBefore("@").lowercase(Locale.US)
                        val starts = if (isHandleSearch) {
                            user.startsWith(normalizedQuery) || emailPrefix.startsWith(normalizedQuery)
                        } else {
                            display.startsWith(normalizedQuery) ||
                                user.startsWith(normalizedQuery) ||
                                emailPrefix.startsWith(normalizedQuery)
                        }
                        !starts
                    }.thenBy { it.displayName.lowercase(Locale.US) }
                )
                .take(10)
                .toList()

            Result.success(matches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun normalizeQuery(query: String): String {
        return normalizeText(query.trim().removePrefix("@"))
    }

    private fun normalizeText(value: String): String {
        return value.trim().lowercase(Locale.US)
    }

    private val socialRepository = SocialRepository(firestore)
}

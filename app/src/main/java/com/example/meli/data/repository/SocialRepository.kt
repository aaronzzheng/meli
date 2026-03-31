package com.example.meli.data.repository

import com.example.meli.model.FriendshipStatus
import com.example.meli.model.FriendListItem
import com.example.meli.model.UserProfileSummary
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class SocialRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val notificationRepository = NotificationRepository(firestore)

    suspend fun getUserProfile(viewedUid: String, currentUid: String?): Result<UserProfileSummary> {
        return try {
            val userDoc = firestore.collection("users").document(viewedUid).get().await()
            val email = userDoc.getString("email").orEmpty()
            val username = userDoc.getString("username")
                ?.takeIf { it.isNotBlank() }
                ?: email.substringBefore("@").takeIf { it.isNotBlank() }.orEmpty()
            val displayName = userDoc.getString("displayName")
                ?.takeIf { it.isNotBlank() }
                ?: username.ifBlank { "Unknown user" }

            val friendCount = firestore.collection("users")
                .document(viewedUid)
                .collection("friends")
                .whereEqualTo("status", "ACCEPTED")
                .get()
                .await()
                .size()

            val friendshipStatus = when {
                currentUid.isNullOrBlank() -> FriendshipStatus.NONE
                currentUid == viewedUid -> FriendshipStatus.SELF
                else -> {
                    val friendshipDoc = firestore.collection("users")
                        .document(currentUid)
                        .collection("friends")
                        .document(viewedUid)
                        .get()
                        .await()
                    mapFriendshipStatus(
                        status = friendshipDoc.getString("status"),
                        direction = friendshipDoc.getString("direction")
                    )
                }
            }

            Result.success(
                UserProfileSummary(
                    uid = viewedUid,
                    displayName = displayName,
                    username = username,
                    email = email,
                    friendCount = friendCount,
                    friendshipStatus = friendshipStatus
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendFriendRequest(currentUid: String, targetUid: String): Result<FriendshipStatus> {
        return try {
            if (currentUid == targetUid) {
                return Result.success(FriendshipStatus.SELF)
            }

            val existingDoc = firestore.collection("users")
                .document(currentUid)
                .collection("friends")
                .document(targetUid)
                .get()
                .await()
            val existingStatus = mapFriendshipStatus(
                status = existingDoc.getString("status"),
                direction = existingDoc.getString("direction")
            )
            if (existingStatus != FriendshipStatus.NONE) {
                if (existingStatus == FriendshipStatus.REQUESTED) {
                    notificationRepository.ensureFriendRequestNotification(
                        targetUid = targetUid,
                        actorUid = currentUid,
                        actorName = resolveActorName(currentUid)
                    )
                }
                return Result.success(existingStatus)
            }

            val now = Timestamp.now()
            val outgoingRef = firestore.collection("users")
                .document(currentUid)
                .collection("friends")
                .document(targetUid)
            val incomingRef = firestore.collection("users")
                .document(targetUid)
                .collection("friends")
                .document(currentUid)

            firestore.runBatch { batch ->
                batch.set(
                    outgoingRef,
                    mapOf(
                        "status" to "PENDING",
                        "requestedAt" to now,
                        "direction" to "OUTGOING"
                    ),
                    SetOptions.merge()
                )
                batch.set(
                    incomingRef,
                    mapOf(
                        "status" to "PENDING",
                        "requestedAt" to now,
                        "direction" to "INCOMING"
                    ),
                    SetOptions.merge()
                )
            }.await()

            notificationRepository.ensureFriendRequestNotification(
                targetUid = targetUid,
                actorUid = currentUid,
                actorName = resolveActorName(currentUid)
            )

            Result.success(FriendshipStatus.REQUESTED)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(currentUid: String, targetUid: String): Result<FriendshipStatus> {
        return respondToFriendRequest(currentUid, targetUid, accept = true)
    }

    suspend fun declineFriendRequest(currentUid: String, targetUid: String): Result<FriendshipStatus> {
        return respondToFriendRequest(currentUid, targetUid, accept = false)
    }

    suspend fun cancelOutgoingRequest(currentUid: String, targetUid: String): Result<Unit> {
        return try {
            val currentRef = firestore.collection("users")
                .document(currentUid)
                .collection("friends")
                .document(targetUid)
            val targetRef = firestore.collection("users")
                .document(targetUid)
                .collection("friends")
                .document(currentUid)

            firestore.runBatch { batch ->
                batch.delete(currentRef)
                batch.delete(targetRef)
            }.await()
            notificationRepository.cancelPendingFriendRequestNotification(
                targetUid = targetUid,
                actorUid = currentUid
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unfriend(currentUid: String, targetUid: String): Result<Unit> {
        return try {
            val currentRef = firestore.collection("users")
                .document(currentUid)
                .collection("friends")
                .document(targetUid)
            val targetRef = firestore.collection("users")
                .document(targetUid)
                .collection("friends")
                .document(currentUid)

            firestore.runBatch { batch ->
                batch.delete(currentRef)
                batch.delete(targetRef)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadFriends(uid: String): Result<List<FriendListItem>> {
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("friends")
                .whereEqualTo("status", "ACCEPTED")
                .get()
                .await()

            val friends = mutableListOf<FriendListItem>()
            for (doc in snapshot.documents) {
                val friendUid = doc.id
                val userDoc = firestore.collection("users").document(friendUid).get().await()
                val email = userDoc.getString("email").orEmpty()
                val username = userDoc.getString("username")
                    ?.takeIf { it.isNotBlank() }
                    ?: email.substringBefore("@").takeIf { it.isNotBlank() }.orEmpty()
                val displayName = userDoc.getString("displayName")
                    ?.takeIf { it.isNotBlank() }
                    ?: username.ifBlank { "Unknown user" }

                friends += FriendListItem(
                    uid = friendUid,
                    displayName = displayName,
                    username = username,
                    email = email,
                    friendshipStatus = FriendshipStatus.FRIENDS
                )
            }

            Result.success(friends.sortedBy { it.displayName.lowercase() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun mapFriendshipStatus(status: String?, direction: String?): FriendshipStatus {
        return when (status) {
            "ACCEPTED" -> FriendshipStatus.FRIENDS
            "PENDING" -> if (direction == "INCOMING") {
                FriendshipStatus.RECEIVED
            } else {
                FriendshipStatus.REQUESTED
            }
            else -> FriendshipStatus.NONE
        }
    }

    private suspend fun resolveActorName(uid: String): String {
        val userDoc = firestore.collection("users").document(uid).get().await()
        val email = userDoc.getString("email").orEmpty()
        return userDoc.getString("displayName")?.takeIf { it.isNotBlank() }
            ?: userDoc.getString("username")?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@").takeIf { it.isNotBlank() }
            ?: "Someone"
    }

    private suspend fun respondToFriendRequest(
        currentUid: String,
        targetUid: String,
        accept: Boolean
    ): Result<FriendshipStatus> {
        return try {
            val currentFriendRef = firestore.collection("users")
                .document(currentUid)
                .collection("friends")
                .document(targetUid)
            val senderFriendRef = firestore.collection("users")
                .document(targetUid)
                .collection("friends")
                .document(currentUid)

            if (accept) {
                firestore.runBatch { batch ->
                    batch.set(
                        currentFriendRef,
                        mapOf(
                            "status" to "ACCEPTED",
                            "acceptedAt" to Timestamp.now(),
                            "direction" to "INCOMING"
                        ),
                        SetOptions.merge()
                    )
                    batch.set(
                        senderFriendRef,
                        mapOf(
                            "status" to "ACCEPTED",
                            "acceptedAt" to Timestamp.now(),
                            "direction" to "OUTGOING"
                        ),
                        SetOptions.merge()
                    )
                }.await()
                notificationRepository.createFriendRequestAcceptedNotification(
                    targetUid = targetUid,
                    actorUid = currentUid,
                    actorName = resolveActorName(currentUid)
                )
                Result.success(FriendshipStatus.FRIENDS)
            } else {
                firestore.runBatch { batch ->
                    batch.delete(currentFriendRef)
                    batch.delete(senderFriendRef)
                }.await()
                notificationRepository.createFriendRequestDeclinedNotification(
                    targetUid = targetUid,
                    actorUid = currentUid,
                    actorName = resolveActorName(currentUid)
                )
                Result.success(FriendshipStatus.NONE)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

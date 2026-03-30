package com.example.meli.data.repository

import com.example.meli.model.AppNotification
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class NotificationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        fun friendRequestNotificationId(actorUid: String): String = "friend_request_$actorUid"
    }

    suspend fun loadNotifications(uid: String): Result<List<AppNotification>> {
        return try {
            val notificationsSnapshot = firestore.collection("users")
                .document(uid)
                .collection("notifications")
                .get()
                .await()

            val storedNotifications = notificationsSnapshot.documents.map { doc ->
                val type = doc.getString("type").orEmpty()
                val actorUid = doc.getString("actorUid").orEmpty()
                val status = doc.getString("status").orEmpty().ifBlank { "PENDING" }
                AppNotification(
                    id = doc.id,
                    type = type,
                    actorUid = actorUid,
                    actorName = doc.getString("actorName").orEmpty().ifBlank { "Someone" },
                    message = doc.getString("message").orEmpty(),
                    createdAtMillis = extractTimestampMillis(doc.get("createdAt")),
                    status = status,
                    isRead = doc.getBoolean("isRead") == true
                )
            }

            val pendingFriendActorIds = storedNotifications
                .filter { it.type == "FRIEND_REQUEST" && it.status == "PENDING" }
                .map { it.actorUid }
                .toSet()

            val friendRequestsSnapshot = firestore.collection("users")
                .document(uid)
                .collection("friends")
                .whereEqualTo("status", "PENDING")
                .whereEqualTo("direction", "INCOMING")
                .get()
                .await()

            val synthesizedFriendNotifications = friendRequestsSnapshot.documents
                .filter { it.id !in pendingFriendActorIds }
                .map { doc ->
                    val actorUid = doc.id
                    val actorName = resolveActorName(actorUid)
                    AppNotification(
                        id = "friend_request_$actorUid",
                        type = "FRIEND_REQUEST",
                        actorUid = actorUid,
                        actorName = actorName,
                        message = "$actorName wants to add you as a friend!",
                        createdAtMillis = extractTimestampMillis(doc.get("requestedAt")),
                        status = "PENDING",
                        isRead = false
                    )
                }

            Result.success(
                (storedNotifications + synthesizedFriendNotifications)
                    .sortedByDescending { it.createdAtMillis }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFriendRequestNotification(
        targetUid: String,
        actorUid: String,
        actorName: String
    ) {
        firestore.collection("users")
            .document(targetUid)
            .collection("notifications")
            .document(friendRequestNotificationId(actorUid))
            .set(
                mapOf(
                    "type" to "FRIEND_REQUEST",
                    "actorUid" to actorUid,
                    "actorName" to actorName,
                    "message" to "$actorName wants to add you as a friend!",
                    "createdAt" to Timestamp.now(),
                    "status" to "PENDING",
                    "isRead" to false
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun createFriendRequestDeclinedNotification(
        targetUid: String,
        actorUid: String,
        actorName: String
    ) {
        firestore.collection("users")
            .document(targetUid)
            .collection("notifications")
            .document()
            .set(
                mapOf(
                    "type" to "FRIEND_REQUEST_DECLINED",
                    "actorUid" to actorUid,
                    "actorName" to actorName,
                    "message" to "$actorName declined your friend request.",
                    "createdAt" to Timestamp.now(),
                    "status" to "INFO",
                    "isRead" to false
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun createFriendRequestAcceptedNotification(
        targetUid: String,
        actorUid: String,
        actorName: String
    ) {
        firestore.collection("users")
            .document(targetUid)
            .collection("notifications")
            .document()
            .set(
                mapOf(
                    "type" to "FRIEND_REQUEST_ACCEPTED",
                    "actorUid" to actorUid,
                    "actorName" to actorName,
                    "message" to "$actorName accepted your friend request.",
                    "createdAt" to Timestamp.now(),
                    "status" to "INFO",
                    "isRead" to false
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun ensureFriendRequestNotification(
        targetUid: String,
        actorUid: String,
        actorName: String
    ) {
        createFriendRequestNotification(
            targetUid = targetUid,
            actorUid = actorUid,
            actorName = actorName
        )
    }

    suspend fun cancelPendingFriendRequestNotification(targetUid: String, actorUid: String) {
        firestore.collection("users")
            .document(targetUid)
            .collection("notifications")
            .document(friendRequestNotificationId(actorUid))
            .set(
                mapOf(
                    "type" to "FRIEND_REQUEST",
                    "actorUid" to actorUid,
                    "status" to "CANCELED",
                    "createdAt" to Timestamp.now(),
                    "isRead" to false
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun createFeedLikeNotification(
        targetUid: String,
        actorUid: String,
        actorName: String,
        trackTitle: String
    ) {
        firestore.collection("users")
            .document(targetUid)
            .collection("notifications")
            .document()
            .set(
                mapOf(
                    "type" to "FEED_LIKE",
                    "actorUid" to actorUid,
                    "actorName" to actorName,
                    "message" to "$actorName liked your ranking of $trackTitle.",
                    "createdAt" to Timestamp.now(),
                    "status" to "INFO",
                    "isRead" to false
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun createFeedCommentNotification(
        targetUid: String,
        actorUid: String,
        actorName: String,
        trackTitle: String,
        commentText: String
    ) {
        val preview = commentText.trim().take(60)
        firestore.collection("users")
            .document(targetUid)
            .collection("notifications")
            .document()
            .set(
                mapOf(
                    "type" to "FEED_COMMENT",
                    "actorUid" to actorUid,
                    "actorName" to actorName,
                    "message" to "$actorName commented on your ranking of $trackTitle: $preview",
                    "createdAt" to Timestamp.now(),
                    "status" to "INFO",
                    "isRead" to false
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun loadUnreadCount(uid: String): Result<Int> {
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("notifications")
                .whereEqualTo("isRead", false)
                .get()
                .await()
            Result.success(snapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllAsRead(uid: String): Result<Unit> {
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("notifications")
                .whereEqualTo("isRead", false)
                .get()
                .await()
            firestore.runBatch { batch ->
                snapshot.documents.forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun respondToFriendRequest(
        currentUid: String,
        notification: AppNotification,
        accept: Boolean
    ): Result<Unit> {
        return try {
            val currentFriendRef = firestore.collection("users")
                .document(currentUid)
                .collection("friends")
                .document(notification.actorUid)
            val senderFriendRef = firestore.collection("users")
                .document(notification.actorUid)
                .collection("friends")
                .document(currentUid)
            val notificationRef = firestore.collection("users")
                .document(currentUid)
                .collection("notifications")
                .document(notification.id)
            val hasStoredNotification = !notification.id.startsWith("friend_request_")
            val matchingPendingNotifications = firestore.collection("users")
                .document(currentUid)
                .collection("notifications")
                .whereEqualTo("type", "FRIEND_REQUEST")
                .whereEqualTo("actorUid", notification.actorUid)
                .whereEqualTo("status", "PENDING")
                .get()
                .await()

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
                    if (hasStoredNotification) {
                        batch.update(notificationRef, mapOf("status" to "ACCEPTED"))
                    }
                    matchingPendingNotifications.documents.forEach { doc ->
                        batch.update(doc.reference, mapOf("status" to "ACCEPTED"))
                    }
                }.await()
                createFriendRequestAcceptedNotification(
                    targetUid = notification.actorUid,
                    actorUid = currentUid,
                    actorName = resolveActorName(currentUid)
                )
            } else {
                firestore.runBatch { batch ->
                    batch.delete(currentFriendRef)
                    batch.delete(senderFriendRef)
                    if (hasStoredNotification) {
                        batch.update(notificationRef, mapOf("status" to "DECLINED"))
                    }
                    matchingPendingNotifications.documents.forEach { doc ->
                        batch.update(doc.reference, mapOf("status" to "DECLINED"))
                    }
                }.await()
                createFriendRequestDeclinedNotification(
                    targetUid = notification.actorUid,
                    actorUid = currentUid,
                    actorName = resolveActorName(currentUid)
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractTimestampMillis(rawTimestamp: Any?): Long {
        return when (rawTimestamp) {
            is Timestamp -> rawTimestamp.toDate().time
            is Number -> rawTimestamp.toLong()
            else -> 0L
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
}

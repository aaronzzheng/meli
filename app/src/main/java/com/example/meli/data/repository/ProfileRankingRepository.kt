package com.example.meli.data.repository

import com.example.meli.model.FeedComment
import com.example.meli.model.ProfileRankingActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class ProfileRankingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val notificationRepository = NotificationRepository(firestore)

    companion object {
        private val LEGACY_MOCK_SONG_KEYS = setOf(
            "good 4 u|olivia rodrigo",
            "drivers license|olivia rodrigo",
            "deja vu|olivia rodrigo",
            "traitor|olivia rodrigo",
            "brutal|olivia rodrigo",
            "cruel summer|taylor swift",
            "as it was|harry styles",
            "levitating|dua lipa",
            "blinding lights|the weeknd"
        )
    }

    suspend fun loadUserRankings(uid: String, viewerUid: String?): Result<List<ProfileRankingActivity>> {
        return try {
            Result.success(loadRankingsForUser(uid, viewerUid))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadFeedRankings(uid: String): Result<List<ProfileRankingActivity>> {
        return try {
            val feedUserIds = linkedSetOf(uid)
            val friendsSnapshot = firestore.collection("users")
                .document(uid)
                .collection("friends")
                .whereEqualTo("status", "ACCEPTED")
                .get()
                .await()

            for (friendDoc in friendsSnapshot.documents) {
                feedUserIds += friendDoc.id
            }

            val activities = mutableListOf<ProfileRankingActivity>()
            for (feedUid in feedUserIds) {
                activities += loadRankingsForUser(feedUid, uid)
            }

            Result.success(activities.sortedByDescending { it.updatedAtMillis })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractArtistText(rawArtistField: Any?): String {
        return when (rawArtistField) {
            is String -> rawArtistField
            is List<*> -> rawArtistField.filterIsInstance<String>().joinToString(", ")
            else -> ""
        }
    }

    private suspend fun loadRankingsForUser(uid: String, viewerUid: String?): List<ProfileRankingActivity> {
        val userDoc = firestore.collection("users")
            .document(uid)
            .get()
            .await()
        val actorName = resolveActorName(uid, userDoc)

        val rankingListsSnapshot = firestore.collection("users")
            .document(uid)
            .collection("rankingLists")
            .get()
            .await()

        val activities = mutableListOf<ProfileRankingActivity>()

        for (listDoc in rankingListsSnapshot.documents) {
            val listName = listDoc.getString("name")
            val entriesSnapshot = listDoc.reference
                .collection("entries")
                .get()
                .await()

            for (entryDoc in entriesSnapshot.documents) {
                if (entryDoc.isLegacyMockEntry()) continue
                activities += buildActivity(uid, actorName, listDoc.id, listName, entryDoc, viewerUid)
            }
        }

        return activities.sortedByDescending { it.updatedAtMillis }
    }

    private suspend fun buildActivity(
        uid: String,
        actorName: String,
        listId: String,
        listName: String?,
        entryDoc: com.google.firebase.firestore.DocumentSnapshot,
        viewerUid: String?
    ): ProfileRankingActivity {
        val title = entryDoc.getString("trackTitle").orEmpty().ifBlank { "Unknown Song" }
        val artistText = extractArtistText(entryDoc.get("artistNames"))
        val rankingScore = extractRankingScore(
            entryDoc.get("score") ?: entryDoc.get("rankingScore")
        ) ?: entryDoc.getLong("rankPosition")?.toDouble()
            ?: entryDoc.getLong("rank_position")?.toDouble()
        val updatedAtMillis = extractTimestampMillis(
            entryDoc.get("updatedAt") ?: entryDoc.get("updated_at")
        )

        return ProfileRankingActivity(
            id = "${uid}_${listId}_${entryDoc.id}",
            actorUid = uid,
            actorName = actorName,
            listId = listId,
            entryId = entryDoc.id,
            trackTitle = title,
            artistText = artistText,
            rankingScore = rankingScore,
            updatedAtMillis = updatedAtMillis,
            listName = listName,
            imageUrl = entryDoc.getString("imageUrl"),
            notes = entryDoc.getString("comment")
                ?: entryDoc.getString("notes")
                ?: "",
            likeCount = (entryDoc.getLong("likeCount") ?: 0L).toInt(),
            commentCount = (entryDoc.getLong("commentCount") ?: 0L).toInt(),
            likedByCurrentUser = false
        )
    }

    private fun resolveActorName(uid: String, userDoc: com.google.firebase.firestore.DocumentSnapshot): String {
        return userDoc.getString("displayName")?.takeIf { it.isNotBlank() }
            ?: userDoc.getString("username")?.takeIf { it.isNotBlank() }
            ?: userDoc.getString("email")?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: uid.take(8)
    }

    private suspend fun resolveActorName(uid: String): String {
        val userDoc = firestore.collection("users")
            .document(uid)
            .get()
            .await()
        return resolveActorName(uid, userDoc)
    }

    private fun extractTimestampMillis(rawTimestamp: Any?): Long {
        return when (rawTimestamp) {
            is Timestamp -> rawTimestamp.toDate().time
            is Number -> rawTimestamp.toLong()
            else -> 0L
        }
    }

    private fun extractRankingScore(rawScore: Any?): Double? {
        return when (rawScore) {
            is Number -> rawScore.toDouble()
            is String -> rawScore.toDoubleOrNull()
            else -> null
        }
    }

    suspend fun toggleLike(activity: ProfileRankingActivity, currentUid: String): Result<Boolean> {
        return try {
            val likeRef = firestore.collection("users")
                .document(activity.actorUid)
                .collection("rankingLists")
                .document(activity.listId)
                .collection("entries")
                .document(activity.entryId)
                .collection("likes")
                .document(currentUid)

            val existing = likeRef.get().await()
            val entryRef = firestore.collection("users")
                .document(activity.actorUid)
                .collection("rankingLists")
                .document(activity.listId)
                .collection("entries")
                .document(activity.entryId)
            if (existing.exists()) {
                firestore.runBatch { batch ->
                    batch.delete(likeRef)
                    batch.update(entryRef, "likeCount", (activity.likeCount - 1).coerceAtLeast(0))
                }.await()
                Result.success(false)
            } else {
                firestore.runBatch { batch ->
                    batch.set(
                        likeRef,
                        mapOf(
                            "uid" to currentUid,
                            "createdAt" to Timestamp.now()
                        ),
                        SetOptions.merge()
                    )
                    batch.set(
                        entryRef,
                        mapOf("likeCount" to activity.likeCount + 1),
                        SetOptions.merge()
                    )
                }.await()

                if (activity.actorUid != currentUid) {
                    notificationRepository.createFeedLikeNotification(
                        targetUid = activity.actorUid,
                        actorUid = currentUid,
                        actorName = resolveActorName(currentUid),
                        trackTitle = activity.trackTitle
                    )
                }
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadComments(activity: ProfileRankingActivity): Result<List<FeedComment>> {
        return try {
            val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            val snapshot = firestore.collection("users")
                .document(activity.actorUid)
                .collection("rankingLists")
                .document(activity.listId)
                .collection("entries")
                .document(activity.entryId)
                .collection("comments")
                .get()
                .await()

            Result.success(
                snapshot.documents.map { doc ->
                    val actorUid = doc.getString("actorUid").orEmpty()
                    val userDoc = if (actorUid.isBlank()) null else firestore.collection("users")
                        .document(actorUid)
                        .get()
                        .await()
                    FeedComment(
                        id = doc.id,
                        actorUid = actorUid,
                        actorName = doc.getString("actorName").orEmpty().ifBlank { "Someone" },
                        actorImageBase64 = userDoc?.getString("profileImageBase64"),
                        text = doc.getString("text").orEmpty(),
                        createdAtMillis = extractTimestampMillis(doc.get("createdAt")),
                        likeCount = (doc.getLong("likeCount") ?: 0L).toInt(),
                        likedByCurrentUser = currentUid != null && doc.reference
                            .collection("likes")
                            .document(currentUid)
                            .get()
                            .await()
                            .exists(),
                        replyToName = doc.getString("replyToName")
                    )
                }.sortedBy { it.createdAtMillis }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addComment(
        activity: ProfileRankingActivity,
        currentUid: String,
        text: String,
        replyToName: String? = null
    ): Result<Unit> {
        return try {
            val actorName = resolveActorName(currentUid)
            firestore.collection("users")
                .document(activity.actorUid)
                .collection("rankingLists")
                .document(activity.listId)
                .collection("entries")
                .document(activity.entryId)
                .collection("comments")
                .document()
                .set(
                    mapOf(
                        "actorUid" to currentUid,
                        "actorName" to actorName,
                        "text" to text.trim(),
                        "createdAt" to Timestamp.now(),
                        "likeCount" to 0,
                        "replyToName" to replyToName
                    ),
                    SetOptions.merge()
                )
                .await()

            firestore.collection("users")
                .document(activity.actorUid)
                .collection("rankingLists")
                .document(activity.listId)
                .collection("entries")
                .document(activity.entryId)
                .set(
                    mapOf("commentCount" to activity.commentCount + 1),
                    SetOptions.merge()
                )
                .await()

            if (activity.actorUid != currentUid) {
                notificationRepository.createFeedCommentNotification(
                    targetUid = activity.actorUid,
                    actorUid = currentUid,
                    actorName = actorName,
                    trackTitle = activity.trackTitle,
                    commentText = text
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleCommentLike(
        activity: ProfileRankingActivity,
        comment: FeedComment,
        currentUid: String
    ): Result<Boolean> {
        return try {
            val commentRef = firestore.collection("users")
                .document(activity.actorUid)
                .collection("rankingLists")
                .document(activity.listId)
                .collection("entries")
                .document(activity.entryId)
                .collection("comments")
                .document(comment.id)
            val likeRef = commentRef.collection("likes").document(currentUid)
            val existing = likeRef.get().await()
            if (existing.exists()) {
                firestore.runBatch { batch ->
                    batch.delete(likeRef)
                    batch.update(commentRef, "likeCount", (comment.likeCount - 1).coerceAtLeast(0))
                }.await()
                Result.success(false)
            } else {
                firestore.runBatch { batch ->
                    batch.set(
                        likeRef,
                        mapOf(
                            "uid" to currentUid,
                            "createdAt" to Timestamp.now()
                        ),
                        SetOptions.merge()
                    )
                    batch.set(
                        commentRef,
                        mapOf("likeCount" to comment.likeCount + 1),
                        SetOptions.merge()
                    )
                }.await()
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.isLegacyMockEntry(): Boolean {
        val title = getString("trackTitle").orEmpty().trim().lowercase()
        val artistText = extractArtistText(get("artistNames")).trim().lowercase()
        val songKey = "$title|$artistText"
        val hasNotes = !getString("comment").isNullOrBlank() || !getString("notes").isNullOrBlank()
        val comparisonCount = (getLong("comparisonCount") ?: 0L).toInt()
        val winCount = (getLong("winCount") ?: 0L).toInt()
        val lossCount = (getLong("lossCount") ?: 0L).toInt()

        return songKey in LEGACY_MOCK_SONG_KEYS &&
            !hasNotes &&
            comparisonCount == 0 &&
            winCount == 0 &&
            lossCount == 0
    }
}

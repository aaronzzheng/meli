package com.example.meli.data.repository

import com.example.meli.model.ProfileRankingActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ProfileRankingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    data class SeedResult(
        val created: Boolean,
        val count: Int
    )

    suspend fun loadUserRankings(uid: String): Result<List<ProfileRankingActivity>> {
        return try {
            Result.success(loadRankingsForUser(uid))
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
                activities += loadRankingsForUser(feedUid)
            }

            Result.success(activities.sortedByDescending { it.updatedAtMillis })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun seedMockRankingsIfEmpty(uid: String): Result<SeedResult> {
        return try {
            val rankingListsRef = firestore.collection("users")
                .document(uid)
                .collection("rankingLists")

            val existingListsSnapshot = rankingListsRef.get().await()
            var hasEntries = false
            for (listDoc in existingListsSnapshot.documents) {
                val count = listDoc.reference.collection("entries").limit(1).get().await().size()
                if (count > 0) {
                    hasEntries = true
                    break
                }
            }

            if (hasEntries) {
                return Result.success(SeedResult(created = false, count = 0))
            }

            val mockListId = "mock_list_profile"
            val mockListRef = rankingListsRef.document(mockListId)
            mockListRef.set(
                mapOf(
                    "name" to "Mock Rankings",
                    "createdAt" to Timestamp.now()
                )
            ).await()

            val now = System.currentTimeMillis()
            val seedSongs = listOf(
                mapOf(
                    "trackId" to "mock_track_1",
                    "trackTitle" to "Blinding Lights",
                    "artistNames" to listOf("The Weeknd"),
                    "score" to 8.1,
                    "updatedAt" to Timestamp(java.util.Date(now - 60_000L))
                ),
                mapOf(
                    "trackId" to "mock_track_2",
                    "trackTitle" to "Levitating",
                    "artistNames" to listOf("Dua Lipa"),
                    "score" to 7.4,
                    "updatedAt" to Timestamp(java.util.Date(now - 120_000L))
                ),
                mapOf(
                    "trackId" to "mock_track_3",
                    "trackTitle" to "As It Was",
                    "artistNames" to listOf("Harry Styles"),
                    "score" to 6.8,
                    "updatedAt" to Timestamp(java.util.Date(now - 180_000L))
                ),
                mapOf(
                    "trackId" to "mock_track_4",
                    "trackTitle" to "Cruel Summer",
                    "artistNames" to listOf("Taylor Swift"),
                    "score" to 9.2,
                    "updatedAt" to Timestamp(java.util.Date(now - 240_000L))
                ),
                mapOf(
                    "trackId" to "mock_track_5",
                    "trackTitle" to "Good 4 U",
                    "artistNames" to listOf("Olivia Rodrigo"),
                    "score" to 5.9,
                    "updatedAt" to Timestamp(java.util.Date(now - 300_000L))
                )
            )

            for (song in seedSongs) {
                val trackId = song["trackId"] as String
                mockListRef.collection("entries").document(trackId).set(
                    mapOf(
                        "trackTitle" to (song["trackTitle"] as String),
                        "artistNames" to (song["artistNames"] as List<*>),
                        "score" to (song["score"] as Double),
                        "updatedAt" to (song["updatedAt"] as Timestamp)
                    )
                ).await()
            }

            Result.success(SeedResult(created = true, count = seedSongs.size))
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

    private suspend fun loadRankingsForUser(uid: String): List<ProfileRankingActivity> {
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
                activities += buildActivity(uid, actorName, listDoc.id, listName, entryDoc)
            }
        }

        return activities.sortedByDescending { it.updatedAtMillis }
    }

    private fun buildActivity(
        uid: String,
        actorName: String,
        listId: String,
        listName: String?,
        entryDoc: com.google.firebase.firestore.DocumentSnapshot
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
            trackTitle = title,
            artistText = artistText,
            rankingScore = rankingScore,
            updatedAtMillis = updatedAtMillis,
            listName = listName
        )
    }

    private fun resolveActorName(uid: String, userDoc: com.google.firebase.firestore.DocumentSnapshot): String {
        return userDoc.getString("displayName")?.takeIf { it.isNotBlank() }
            ?: userDoc.getString("username")?.takeIf { it.isNotBlank() }
            ?: userDoc.getString("email")?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: uid.take(8)
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
}

package com.example.meli.data.repository

import com.example.meli.model.ComparisonChoice
import com.example.meli.model.FriendTrackRating
import com.example.meli.model.RankingSortOption
import com.example.meli.model.RatingSentiment
import com.example.meli.model.TrackRating
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.math.pow

data class EloComparisonResult(
    val updatedCurrent: TrackRating,
    val updatedOpponent: TrackRating?
)

class TrackRatingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    companion object {
        const val DEFAULT_LIST_ID = "songs"
        const val DEFAULT_LIST_NAME = "Your Songs"
        const val DEFAULT_SCORE = 5.0
        const val K_FACTOR = 0.75
        const val TARGET_COMPARISONS = 3
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

    private fun currentUid(): String? = auth.currentUser?.uid

    private fun ratingCollection(uid: String) = firestore.collection("users")
        .document(uid)
        .collection("ratings")

    suspend fun getRating(trackId: String): Result<TrackRating?> {
        val uid = currentUid()
            ?: return Result.failure(IllegalStateException("Please sign in to rank songs."))

        return try {
            pruneLegacyMockRatings(uid)
            val snapshot = ratingCollection(uid).document(trackId).get().await()
            Result.success(snapshot.takeIf { it.exists() }?.toTrackRating())
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun getNextComparisonCandidate(
        excludedTrackIds: Set<String>,
        sentiment: RatingSentiment,
        targetScore: Double
    ): Result<TrackRating?> {
        val uid = currentUid()
            ?: return Result.failure(IllegalStateException("Please sign in to rank songs."))

        return try {
            val candidate = ratingCollection(uid).get().await().documents
                .mapNotNull { it.toTrackRating() }
                .filter { it.trackId !in excludedTrackIds && it.sentiment == sentiment }
                .sortedWith(
                    compareBy<TrackRating> { it.scoreDistanceFrom(targetScore) }
                        .thenBy { it.comparisonCount }
                        .thenByDescending { it.createdAtMillis }
                )
                .firstOrNull()

            Result.success(candidate)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun loadRatings(sortOption: RankingSortOption): Result<List<TrackRating>> {
        val uid = currentUid()
            ?: return Result.failure(IllegalStateException("Please sign in to view your list."))

        return try {
            pruneLegacyMockRatings(uid)
            val ratings = ratingCollection(uid).get().await().documents
                .mapNotNull { it.toTrackRating() }

            val sorted = when (sortOption) {
                RankingSortOption.HIGHEST_RATED -> ratings.sortedWith(
                    compareByDescending<TrackRating> { it.score }
                        .thenByDescending { it.updatedAtMillis }
                )

                RankingSortOption.LOWEST_RATED -> ratings.sortedWith(
                    compareBy<TrackRating> { it.score }
                        .thenByDescending { it.updatedAtMillis }
                )

                RankingSortOption.RECENT -> ratings.sortedByDescending { it.updatedAtMillis }
            }

            Result.success(sorted)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun loadRatedTrackIds(): Result<Set<String>> {
        val uid = currentUid()
            ?: return Result.failure(IllegalStateException("Please sign in to view rated songs."))

        return try {
            pruneLegacyMockRatings(uid)
            Result.success(
                ratingCollection(uid).get().await().documents.map { it.id }.toSet()
            )
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun loadFriendRatingsForTrack(trackId: String): Result<List<FriendTrackRating>> {
        val uid = currentUid()
            ?: return Result.failure(IllegalStateException("Please sign in to view friend rankings."))

        return try {
            val friendIds = firestore.collection("users")
                .document(uid)
                .collection("friends")
                .whereEqualTo("status", "ACCEPTED")
                .get()
                .await()
                .documents
                .map { it.id }

            val friendRatings = mutableListOf<FriendTrackRating>()
            for (friendId in friendIds) {
                val userDoc = firestore.collection("users").document(friendId).get().await()
                val ratingDoc = firestore.collection("users")
                    .document(friendId)
                    .collection("ratings")
                    .document(trackId)
                    .get()
                    .await()
                val rating = ratingDoc.takeIf { it.exists() }?.toTrackRating() ?: continue
                val userName = userDoc.getString("displayName")
                    ?.takeIf { it.isNotBlank() }
                    ?: userDoc.getString("username")
                    ?.takeIf { it.isNotBlank() }
                    ?: "Friend"
                friendRatings += FriendTrackRating(
                    userName = userName,
                    score = rating.score,
                    sentiment = rating.sentiment
                )
            }

            Result.success(friendRatings.sortedByDescending { it.score })
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun saveSession(
        current: TrackRating,
        updatedOpponents: Collection<TrackRating>
    ): Result<TrackRating> {
        val uid = currentUid()
            ?: return Result.failure(IllegalStateException("Please sign in to rank songs."))

        return try {
            val allRatings = (updatedOpponents + current).distinctBy { it.trackId }
            val listRef = firestore.collection("users")
                .document(uid)
                .collection("rankingLists")
                .document(DEFAULT_LIST_ID)

            firestore.runBatch { batch ->
                batch.set(
                    listRef,
                    mapOf(
                        "name" to DEFAULT_LIST_NAME,
                        "createdAt" to current.createdAtMillis
                    )
                )

                allRatings.forEach { rating ->
                    batch.set(
                        ratingCollection(uid).document(rating.trackId),
                        rating.toFirestoreMap()
                    )
                    batch.set(
                        listRef.collection("entries").document(rating.trackId),
                        mapOf(
                            "trackTitle" to rating.trackTitle,
                            "artistNames" to rating.artistNames,
                            "albumTitle" to rating.albumTitle,
                            "imageUrl" to rating.imageUrl,
                            "score" to rating.score,
                            "comment" to rating.notes,
                            "updatedAt" to rating.updatedAtMillis
                        )
                    )
                }
            }.await()

            Result.success(current)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun deleteRating(trackId: String): Result<Unit> {
        val uid = currentUid()
            ?: return Result.failure(IllegalStateException("Please sign in to edit your list."))

        return try {
            ratingCollection(uid).document(trackId).delete().await()
            firestore.collection("users")
                .document(uid)
                .collection("rankingLists")
                .document(DEFAULT_LIST_ID)
                .collection("entries")
                .document(trackId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    fun buildWorkingRating(
        trackId: String,
        trackTitle: String,
        artistNames: List<String>,
        albumTitle: String?,
        imageUrl: String?,
        sentiment: RatingSentiment,
        notes: String,
        existing: TrackRating?
    ): TrackRating {
        val now = System.currentTimeMillis()
        val reuseExisting = existing?.sentiment == sentiment

        return TrackRating(
            trackId = trackId,
            trackTitle = trackTitle,
            artistNames = artistNames,
            albumTitle = albumTitle,
            imageUrl = imageUrl,
            sentiment = sentiment,
            score = if (reuseExisting) existing.score else defaultScoreFor(sentiment),
            notes = notes,
            createdAtMillis = existing?.createdAtMillis ?: now,
            updatedAtMillis = now,
            comparisonCount = if (reuseExisting) existing.comparisonCount else 0,
            winCount = if (reuseExisting) existing.winCount else 0,
            lossCount = if (reuseExisting) existing.lossCount else 0,
            comparisonTrackId = null,
            comparisonTrackTitle = null,
            comparisonChoice = null
        )
    }

    fun applyComparison(
        current: TrackRating,
        opponent: TrackRating?,
        choice: ComparisonChoice
    ): EloComparisonResult {
        val now = System.currentTimeMillis()
        if (opponent == null) {
            return EloComparisonResult(
                updatedCurrent = current.copy(
                    updatedAtMillis = now,
                    comparisonChoice = choice
                ),
                updatedOpponent = null
            )
        }

        if (choice == ComparisonChoice.TIE) {
            val sharedScore = opponent.score
            return EloComparisonResult(
                updatedCurrent = current.copy(
                    score = sharedScore,
                    updatedAtMillis = now,
                    comparisonCount = current.comparisonCount + 1,
                    comparisonTrackId = opponent.trackId,
                    comparisonTrackTitle = opponent.trackTitle,
                    comparisonChoice = choice
                ),
                updatedOpponent = opponent.copy(
                    updatedAtMillis = now,
                    comparisonCount = opponent.comparisonCount + 1
                )
            )
        }

        val expectedCurrent = expectedScore(current.score, opponent.score)
        val expectedOpponent = expectedScore(opponent.score, current.score)
        val currentScore = if (choice == ComparisonChoice.CURRENT) 1.0 else 0.0
        val opponentScore = if (choice == ComparisonChoice.OTHER) 1.0 else 0.0

        val updatedCurrent = current.copy(
            score = normalizeScore(current.score + K_FACTOR * (currentScore - expectedCurrent)),
            updatedAtMillis = now,
            comparisonCount = current.comparisonCount + 1,
            winCount = current.winCount + if (choice == ComparisonChoice.CURRENT) 1 else 0,
            lossCount = current.lossCount + if (choice == ComparisonChoice.OTHER) 1 else 0,
            comparisonTrackId = opponent.trackId,
            comparisonTrackTitle = opponent.trackTitle,
            comparisonChoice = choice
        )
        val updatedOpponent = opponent.copy(
            score = normalizeScore(opponent.score + K_FACTOR * (opponentScore - expectedOpponent)),
            updatedAtMillis = now,
            comparisonCount = opponent.comparisonCount + 1,
            winCount = opponent.winCount + if (choice == ComparisonChoice.OTHER) 1 else 0,
            lossCount = opponent.lossCount + if (choice == ComparisonChoice.CURRENT) 1 else 0
        )

        return EloComparisonResult(updatedCurrent = updatedCurrent, updatedOpponent = updatedOpponent)
    }

    private fun expectedScore(playerRating: Double, opponentRating: Double): Double {
        return 1.0 / (1.0 + 10.0.pow((opponentRating - playerRating) / 2.5))
    }

    private fun normalizeScore(rawScore: Double): Double {
        return (rawScore.coerceIn(0.0, 10.0) * 10.0).toInt() / 10.0
    }

    private fun defaultScoreFor(sentiment: RatingSentiment): Double {
        return when (sentiment) {
            RatingSentiment.LOVE -> 8.0
            RatingSentiment.FINE -> 5.5
            RatingSentiment.DISLIKE -> 2.5
        }
    }

    private fun TrackRating.toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "trackId" to trackId,
            "trackTitle" to trackTitle,
            "artistNames" to artistNames,
            "albumTitle" to albumTitle,
            "imageUrl" to imageUrl,
            "sentiment" to sentiment.storageValue,
            "score" to score,
            "comment" to notes,
            "comparisonCount" to comparisonCount,
            "winCount" to winCount,
            "lossCount" to lossCount,
            "comparisonTrackId" to comparisonTrackId,
            "comparisonTrackTitle" to comparisonTrackTitle,
            "comparisonChoice" to comparisonChoice?.storageValue,
            "createdAt" to createdAtMillis,
            "updatedAt" to updatedAtMillis
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toTrackRating(): TrackRating? {
        val sentiment = RatingSentiment.fromStorage(getString("sentiment")) ?: return null
        val score = getDouble("score") ?: getLong("score")?.toDouble() ?: DEFAULT_SCORE
        val trackId = getString("trackId") ?: id

        return TrackRating(
            trackId = trackId,
            trackTitle = getString("trackTitle").orEmpty().ifBlank { "Unknown Song" },
            artistNames = when (val artists = get("artistNames")) {
                is List<*> -> artists.filterIsInstance<String>()
                is String -> listOf(artists)
                else -> emptyList()
            },
            albumTitle = getString("albumTitle"),
            imageUrl = getString("imageUrl"),
            sentiment = sentiment,
            score = score,
            notes = getString("comment").orEmpty(),
            createdAtMillis = getLong("createdAt") ?: System.currentTimeMillis(),
            updatedAtMillis = getLong("updatedAt") ?: System.currentTimeMillis(),
            comparisonCount = (getLong("comparisonCount") ?: 0L).toInt(),
            winCount = (getLong("winCount") ?: 0L).toInt(),
            lossCount = (getLong("lossCount") ?: 0L).toInt(),
            comparisonTrackId = getString("comparisonTrackId"),
            comparisonTrackTitle = getString("comparisonTrackTitle"),
            comparisonChoice = ComparisonChoice.fromStorage(getString("comparisonChoice"))
        )
    }

    private suspend fun pruneLegacyMockRatings(uid: String) {
        val snapshot = ratingCollection(uid).get().await()
        val staleMockDocs = snapshot.documents.filter { document ->
            val rating = document.toTrackRating() ?: return@filter false
            rating.isLegacyMockRating()
        }
        if (staleMockDocs.isEmpty()) return

        val listEntries = firestore.collection("users")
            .document(uid)
            .collection("rankingLists")
            .document(DEFAULT_LIST_ID)
            .collection("entries")

        firestore.runBatch { batch ->
            staleMockDocs.forEach { document ->
                batch.delete(document.reference)
                batch.delete(listEntries.document(document.id))
            }
        }.await()
    }

    private fun TrackRating.isLegacyMockRating(): Boolean {
        val songKey = "${trackTitle.trim().lowercase()}|${artistText.trim().lowercase()}"
        return songKey in LEGACY_MOCK_SONG_KEYS &&
            notes.isBlank() &&
            comparisonCount == 0 &&
            winCount == 0 &&
            lossCount == 0
    }
}

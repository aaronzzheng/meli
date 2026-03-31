package com.example.meli.model

import java.util.Locale
import kotlin.math.abs

enum class RatingSentiment(
    val storageValue: String,
    val label: String
) {
    LOVE("LOVE", "I liked it"),
    FINE("FINE", "It was fine"),
    DISLIKE("DISLIKE", "I didn't like it");

    companion object {
        fun fromStorage(value: String?): RatingSentiment? {
            return entries.firstOrNull { it.storageValue == value }
        }
    }
}

enum class ComparisonChoice(val storageValue: String) {
    CURRENT("CURRENT"),
    OTHER("OTHER"),
    TIE("TIE");

    companion object {
        fun fromStorage(value: String?): ComparisonChoice? {
            return entries.firstOrNull { it.storageValue == value }
        }
    }
}

enum class RankingSortOption {
    HIGHEST_RATED,
    LOWEST_RATED,
    RECENT
}

data class TrackRating(
    val trackId: String,
    val trackTitle: String,
    val artistNames: List<String>,
    val albumTitle: String?,
    val imageUrl: String?,
    val sentiment: RatingSentiment,
    val score: Double,
    val notes: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val comparisonCount: Int,
    val winCount: Int,
    val lossCount: Int,
    val comparisonTrackId: String?,
    val comparisonTrackTitle: String?,
    val comparisonChoice: ComparisonChoice?
) {
    val artistText: String
        get() = artistNames.joinToString(", ").ifBlank { "Unknown artist" }

    val formattedScore: String
        get() = String.format(Locale.US, "%.1f", score)

    val confidenceLabel: String
        get() = when {
            comparisonCount >= 10 -> "Stable"
            comparisonCount >= 5 -> "Learning"
            else -> "New"
        }

    fun scoreDistanceFrom(target: Double): Double = abs(score - target)
}

data class FriendTrackRating(
    val userUid: String,
    val userName: String,
    val score: Double,
    val sentiment: RatingSentiment,
    val notes: String
) {
    val formattedScore: String
        get() = String.format(Locale.US, "%.1f", score)
}

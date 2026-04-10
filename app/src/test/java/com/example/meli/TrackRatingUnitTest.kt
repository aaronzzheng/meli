package com.example.meli

import com.example.meli.model.TrackRating
import com.example.meli.model.RatingSentiment
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackRatingUnitTest {

    @Test
    // Keeps score display consistent so rankings feel clear and reliable to users.
    fun formattedScore_usesSingleDecimalPlace() {
        val rating = baseRating(score = 8.26)

        assertEquals("8.3", rating.formattedScore)
    }

    @Test
    // Keeps ranking confidence messaging aligned with how much user input the app has collected.
    fun confidenceLabel_changesWithComparisonCount() {
        val newRating = baseRating(comparisonCount = 2)
        val learningRating = baseRating(comparisonCount = 6)
        val stableRating = baseRating(comparisonCount = 10)

        assertEquals("New", newRating.confidenceLabel)
        assertEquals("Learning", learningRating.confidenceLabel)
        assertEquals("Stable", stableRating.confidenceLabel)
    }

    @Test
    // Keeps ranking comparisons fair by treating equally close songs the same on either side.
    fun scoreDistanceFrom_returnsAbsoluteDifference() {
        val rating = baseRating(score = 7.5)

        assertEquals(1.25, rating.scoreDistanceFrom(8.75), 0.0001)
        assertEquals(1.25, rating.scoreDistanceFrom(6.25), 0.0001)
    }

    private fun baseRating(
        score: Double = 5.0,
        comparisonCount: Int = 0
    ): TrackRating {
        return TrackRating(
            trackId = "track-1",
            trackTitle = "Song",
            artistNames = listOf("Artist"),
            albumTitle = "Album",
            imageUrl = null,
            sentiment = RatingSentiment.FINE,
            score = score,
            notes = "",
            createdAtMillis = 1L,
            updatedAtMillis = 1L,
            comparisonCount = comparisonCount,
            winCount = 0,
            lossCount = 0,
            comparisonTrackId = null,
            comparisonTrackTitle = null,
            comparisonChoice = null
        )
    }
}

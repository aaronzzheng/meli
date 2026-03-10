package com.example.meli

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FirestoreSchemaSeedTest {

    @Test
    fun seedFirestoreSchema() {
        val db = FirebaseFirestore.getInstance()
        val now = System.currentTimeMillis()

        val uidA = "seed_user_1"
        val uidB = "seed_user_2"
        val usernameA = "seeduser1"
        val usernameB = "seeduser2"
        val emailA = "seeduser1@example.com"
        val emailB = "seeduser2@example.com"

        val artistId = "spotify_artist_seed_1"
        val albumId = "spotify_album_seed_1"
        val trackId = "spotify_track_seed_1"
        val listId = "favorites"

        val batch = db.batch()

        val userARef = db.collection("users").document(uidA)
        val userBRef = db.collection("users").document(uidB)

        // /users/{uid}
        batch.set(
            userARef,
            mapOf(
                "displayName" to "Seed User One",
                "username" to usernameA,
                "email" to emailA,
                "createdAt" to now,
                "spotify" to mapOf(
                    "spotifyUserId" to "sp_seed_user_1",
                    "tokenExpiresAt" to (now + 3_600_000)
                )
            )
        )
        batch.set(
            userBRef,
            mapOf(
                "displayName" to "Seed User Two",
                "username" to usernameB,
                "email" to emailB,
                "createdAt" to now,
                "spotify" to mapOf(
                    "spotifyUserId" to "sp_seed_user_2",
                    "tokenExpiresAt" to (now + 3_600_000)
                )
            )
        )

        // /usernames/{username}
        batch.set(db.collection("usernames").document(usernameA), mapOf("uid" to uidA))
        batch.set(db.collection("usernames").document(usernameB), mapOf("uid" to uidB))

        // /emails/{emailHashOrKey}
        batch.set(db.collection("emails").document(emailA), mapOf("uid" to uidA))
        batch.set(db.collection("emails").document(emailB), mapOf("uid" to uidB))

        // /users/{uid}/friends/{otherUid}
        batch.set(
            userARef.collection("friends").document(uidB),
            mapOf(
                "status" to "ACCEPTED",
                "requestedAt" to now,
                "acceptedAt" to now,
                "direction" to "OUTGOING"
            )
        )
        batch.set(
            userBRef.collection("friends").document(uidA),
            mapOf(
                "status" to "ACCEPTED",
                "requestedAt" to now,
                "acceptedAt" to now,
                "direction" to "INCOMING"
            )
        )

        // /artists/{spotifyArtistId}
        batch.set(db.collection("artists").document(artistId), mapOf("name" to "Seed Artist"))

        // /albums/{spotifyAlbumId}
        batch.set(
            db.collection("albums").document(albumId),
            mapOf(
                "title" to "Seed Album",
                "releaseDate" to "2025-01-01",
                "artistIds" to listOf(artistId),
                "artistNames" to listOf("Seed Artist")
            )
        )

        // /tracks/{spotifyTrackId}
        batch.set(
            db.collection("tracks").document(trackId),
            mapOf(
                "title" to "Seed Track",
                "durationMs" to 210_000L,
                "explicit" to false,
                "albumId" to albumId,
                "albumTitle" to "Seed Album",
                "artistIds" to listOf(artistId),
                "artistNames" to listOf("Seed Artist")
            )
        )

        // /users/{uid}/ratings/{trackId}
        batch.set(
            userARef.collection("ratings").document(trackId),
            mapOf(
                "score" to 9L,
                "comment" to "Great track",
                "createdAt" to now,
                "updatedAt" to now,
                "trackTitle" to "Seed Track",
                "artistNames" to listOf("Seed Artist"),
                "albumTitle" to "Seed Album"
            )
        )

        // Optional mirror: /tracks/{trackId}/ratings/{uid}
        batch.set(
            db.collection("tracks").document(trackId).collection("ratings").document(uidA),
            mapOf(
                "score" to 9L,
                "comment" to "Great track",
                "createdAt" to now,
                "updatedAt" to now,
                "userId" to uidA,
                "username" to usernameA
            )
        )

        // /users/{uid}/rankingLists/{listId}
        val listRef = userARef.collection("rankingLists").document(listId)
        batch.set(
            listRef,
            mapOf(
                "name" to "Favorites",
                "createdAt" to now
            )
        )

        // /users/{uid}/rankingLists/{listId}/entries/{trackId}
        batch.set(
            listRef.collection("entries").document(trackId),
            mapOf(
                "rankPosition" to 1L,
                "updatedAt" to now,
                "trackTitle" to "Seed Track",
                "artistNames" to listOf("Seed Artist")
            )
        )

        // /comparisonSessions/{pairId_timestamp}
        val pairId = "${uidA}_${uidB}"
        val comparisonId = "${pairId}_$now"
        batch.set(
            db.collection("comparisonSessions").document(comparisonId),
            mapOf(
                "userAId" to uidA,
                "userBId" to uidB,
                "pairId" to pairId,
                "createdAt" to now,
                "overlapCount" to 12L,
                "similarityScore" to 0.78
            )
        )

        Tasks.await(batch.commit(), 20, TimeUnit.SECONDS)
    }
}

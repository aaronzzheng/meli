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
        val friendshipId = "${uidA}_${uidB}"
        val rankingListSqlId = "${uidA}_${listId}"
        val rankingEntrySqlId = "${rankingListSqlId}_${trackId}"
        val comparisonSqlId = "${friendshipId}_$now"

        val batch = db.batch()

        val userARef = db.collection("users").document(uidA)
        val userBRef = db.collection("users").document(uidB)

        // /users/{uid}
        batch.set(
            userARef,
            mapOf(
                // Friendly name shown in profile UIs.
                "displayName" to "Seed User One",
                // Human-readable handle used for mentions/login-like lookup.
                "username" to usernameA,
                // Contact/login email.
                "email" to emailA,
                // Account creation timestamp.
                "createdAt" to now,
                // Spotify linkage metadata nested under the user profile.
                "spotify" to mapOf(
                    // Spotify account id tied to this app user.
                    "spotifyUserId" to "sp_seed_user_1",
                    // When Spotify access should be refreshed.
                    "tokenExpiresAt" to (now + 3_600_000)
                )
            )
        )
        batch.set(
            userBRef,
            mapOf(
                // Friendly name shown in profile UIs.
                "displayName" to "Seed User Two",
                // Human-readable handle used for mentions/login-like lookup.
                "username" to usernameB,
                // Contact/login email.
                "email" to emailB,
                // Account creation timestamp.
                "createdAt" to now,
                // Spotify linkage metadata nested under the user profile.
                "spotify" to mapOf(
                    // Spotify account id tied to this app user.
                    "spotifyUserId" to "sp_seed_user_2",
                    // When Spotify access should be refreshed.
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
                // Current friendship state.
                "status" to "ACCEPTED",
                // When the friend request was initiated.
                "requestedAt" to now,
                // When the request was accepted (null if pending).
                "acceptedAt" to now,
                // Outgoing/incoming marker from this user's perspective.
                "direction" to "OUTGOING"
            )
        )
        batch.set(
            userBRef.collection("friends").document(uidA),
            mapOf(
                // Current friendship state.
                "status" to "ACCEPTED",
                // When the friend request was initiated.
                "requestedAt" to now,
                // When the request was accepted (null if pending).
                "acceptedAt" to now,
                // Outgoing/incoming marker from this user's perspective.
                "direction" to "INCOMING"
            )
        )

        // /artists/{spotifyArtistId}
        batch.set(db.collection("artists").document(artistId), mapOf("name" to "Seed Artist"))

        // /albums/{spotifyAlbumId}
        batch.set(
            db.collection("albums").document(albumId),
            mapOf(
                // Album name displayed to users.
                "title" to "Seed Album",
                // Album release date from Spotify metadata.
                "releaseDate" to "2025-01-01",
                // Related artist ids for join-free reads.
                "artistIds" to listOf(artistId),
                // Denormalized artist names for quick rendering.
                "artistNames" to listOf("Seed Artist")
            )
        )

        // /tracks/{spotifyTrackId}
        batch.set(
            db.collection("tracks").document(trackId),
            mapOf(
                // Track name displayed in lists and detail views.
                "title" to "Seed Track",
                // Track length for playback/ranking UI.
                "durationMs" to 210_000L,
                // Explicit-content flag for filtering/badges.
                "explicit" to false,
                // Parent album document id.
                "albumId" to albumId,
                // Denormalized album title for quick UI.
                "albumTitle" to "Seed Album",
                // Related artist ids for join-free reads.
                "artistIds" to listOf(artistId),
                // Denormalized artist names for quick rendering.
                "artistNames" to listOf("Seed Artist")
            )
        )

        // /users/{uid}/ratings/{trackId}
        batch.set(
            userARef.collection("ratings").document(trackId),
            mapOf(
                // User's rating value for this track.
                "score" to 9L,
                // Optional text feedback.
                "comment" to "Great track",
                // First creation timestamp.
                "createdAt" to now,
                // Last update timestamp.
                "updatedAt" to now,
                // Denormalized display data for history pages.
                "trackTitle" to "Seed Track",
                // Denormalized display data for history pages.
                "artistNames" to listOf("Seed Artist"),
                // Denormalized display data for history pages.
                "albumTitle" to "Seed Album"
            )
        )

        // Optional mirror: /tracks/{trackId}/ratings/{uid}
        batch.set(
            db.collection("tracks").document(trackId).collection("ratings").document(uidA),
            mapOf(
                // Same rating value mirrored under track.
                "score" to 9L,
                // Same optional text feedback mirrored under track.
                "comment" to "Great track",
                // First creation timestamp.
                "createdAt" to now,
                // Last update timestamp.
                "updatedAt" to now,
                // Who authored this rating.
                "userId" to uidA,
                // Denormalized username for fast track-centric views.
                "username" to usernameA
            )
        )

        // /users/{uid}/rankingLists/{listId}
        val listRef = userARef.collection("rankingLists").document(listId)
        batch.set(
            listRef,
            mapOf(
                // User-defined list name.
                "name" to "Favorites",
                // List creation timestamp.
                "createdAt" to now
            )
        )

        // /users/{uid}/rankingLists/{listId}/entries/{trackId}
        batch.set(
            listRef.collection("entries").document(trackId),
            mapOf(
                // Rank position inside this list.
                "rankPosition" to 1L,
                // Last rank change timestamp.
                "updatedAt" to now,
                // Denormalized display data for quick rendering.
                "trackTitle" to "Seed Track",
                // Denormalized display data for quick rendering.
                "artistNames" to listOf("Seed Artist")
            )
        )

        // /comparisonSessions/{pairId_timestamp}
        val pairId = "${uidA}_${uidB}"
        val comparisonId = "${pairId}_$now"
        batch.set(
            db.collection("comparisonSessions").document(comparisonId),
            mapOf(
                // First user in compared pair.
                "userAId" to uidA,
                // Second user in compared pair.
                "userBId" to uidB,
                // Stable pair key for querying comparison history.
                "pairId" to pairId,
                // When this comparison was computed.
                "createdAt" to now,
                // Number of overlapping tracks/artists considered.
                "overlapCount" to 12L,
                // Computed similarity metric for the pair.
                "similarityScore" to 0.78
            )
        )

        // SQL-style compatibility collections requested by schema checks.
        batch.set(
            db.collection("credentials").document(uidA),
            mapOf(
                // SQL compatibility: synthetic primary key value.
                "credential_id" to 1L,
                // Owning user id.
                "user_id" to uidA,
                // Username used at credential layer.
                "username" to usernameA,
                // Placeholder password hash for dev schema checks.
                "password_hash" to "seed_hash_value_only_for_dev",
                // Hash algorithm name for verification logic.
                "hash_algorithm" to "bcrypt",
                // Credential creation timestamp.
                "created_at" to now
            )
        )
        batch.set(
            db.collection("credentials").document(uidB),
            mapOf(
                // SQL compatibility: synthetic primary key value.
                "credential_id" to 2L,
                // Owning user id.
                "user_id" to uidB,
                // Username used at credential layer.
                "username" to usernameB,
                // Placeholder password hash for dev schema checks.
                "password_hash" to "seed_hash_value_only_for_dev",
                // Hash algorithm name for verification logic.
                "hash_algorithm" to "bcrypt",
                // Credential creation timestamp.
                "created_at" to now
            )
        )

        batch.set(
            db.collection("spotify_links").document(uidA),
            mapOf(
                // SQL compatibility: synthetic primary key value.
                "spotify_link_id" to 1L,
                // Owning user id.
                "user_id" to uidA,
                // Linked Spotify account id.
                "spotify_user_id" to "sp_seed_user_1",
                // Placeholder OAuth access token for dev checks.
                "access_token" to "seed_access_token_dev_only",
                // Placeholder OAuth refresh token for dev checks.
                "refresh_token" to "seed_refresh_token_dev_only",
                // Access token expiration timestamp.
                "token_expires_at" to (now + 3_600_000)
            )
        )
        batch.set(
            db.collection("spotify_links").document(uidB),
            mapOf(
                // SQL compatibility: synthetic primary key value.
                "spotify_link_id" to 2L,
                // Owning user id.
                "user_id" to uidB,
                // Linked Spotify account id.
                "spotify_user_id" to "sp_seed_user_2",
                // Placeholder OAuth access token for dev checks.
                "access_token" to "seed_access_token_dev_only",
                // Placeholder OAuth refresh token for dev checks.
                "refresh_token" to "seed_refresh_token_dev_only",
                // Access token expiration timestamp.
                "token_expires_at" to (now + 3_600_000)
            )
        )

        batch.set(
            db.collection("friendships").document(friendshipId),
            mapOf(
                // SQL compatibility: synthetic primary key value.
                "friendship_id" to 1L,
                // User who sent the request.
                "requester_user_id" to uidA,
                // User who received the request.
                "addressee_user_id" to uidB,
                // Request state.
                "status" to "ACCEPTED",
                // Request creation timestamp.
                "requested_at" to now,
                // Request acceptance timestamp.
                "accepted_at" to now
            )
        )

        batch.set(
            db.collection("track_artists").document("${trackId}_${artistId}"),
            mapOf(
                // Track side of many-to-many relation.
                "track_id" to trackId,
                // Artist side of many-to-many relation.
                "artist_id" to artistId
            )
        )

        batch.set(
            db.collection("ranking_lists").document(rankingListSqlId),
            mapOf(
                // SQL compatibility: synthetic primary key value.
                "ranking_list_id" to 1L,
                // Owner user id.
                "user_id" to uidA,
                // User-provided list name.
                "name" to "Favorites",
                // List creation timestamp.
                "created_at" to now
            )
        )

        batch.set(
            db.collection("ranking_entries").document(rankingEntrySqlId),
            mapOf(
                // SQL compatibility: synthetic primary key value.
                "ranking_entry_id" to 1L,
                // Parent list foreign-key equivalent.
                "ranking_list_id" to rankingListSqlId,
                // Ranked track foreign-key equivalent.
                "track_id" to trackId,
                // Position within the ranking list.
                "rank_position" to 1L,
                // Last edit timestamp for this rank.
                "updated_at" to now
            )
        )

        batch.set(
            db.collection("comparison_sessions").document(comparisonSqlId),
            mapOf(
                // SQL compatibility: synthetic primary key value.
                "comparison_id" to 1L,
                // First compared user id.
                "user_a_id" to uidA,
                // Second compared user id.
                "user_b_id" to uidB,
                // Session creation timestamp.
                "created_at" to now,
                // Number of overlapping entities analyzed.
                "overlap_count" to 12L,
                // Similarity metric for the compared users.
                "similarity_score" to 0.78
            )
        )

        Tasks.await(batch.commit(), 20, TimeUnit.SECONDS)
    }
}

# Firestore Schema (Simple)

This app uses **Cloud Firestore**.
Database: **(default)**

## Main Collections

### `/users/{uid}`
- `displayName`
- `username`
- `email`
- `createdAt`
- `spotify`: `{ spotifyUserId, tokenExpiresAt }`

### `/usernames/{username}`
- `uid`

### `/emails/{email}`
- `uid`

### `/artists/{spotifyArtistId}`
- `name`

### `/albums/{spotifyAlbumId}`
- `title`
- `releaseDate`
- `artistIds`
- `artistNames`

### `/tracks/{spotifyTrackId}`
- `title`
- `durationMs`
- `explicit`
- `albumId`
- `albumTitle`
- `artistIds`
- `artistNames`

### `/comparisonSessions/{pairId_timestamp}`
- `userAId`
- `userBId`
- `pairId`
- `createdAt`
- `overlapCount`
- `similarityScore`

## User Subcollections

### `/users/{uid}/friends/{otherUid}`
- `status` (`PENDING | ACCEPTED | BLOCKED`)
- `requestedAt`
- `acceptedAt`
- `direction` (`OUTGOING | INCOMING`)

### `/users/{uid}/ratings/{trackId}`
- `score`
- `comment`
- `createdAt`
- `updatedAt`
- `trackTitle`
- `artistNames`
- `albumTitle`

### `/users/{uid}/rankingLists/{listId}`
- `name`
- `createdAt`

### `/users/{uid}/rankingLists/{listId}/entries/{trackId}`
- `rankPosition`
- `updatedAt`
- `trackTitle`
- `artistNames`

## List Screen CRUD Collection

### `/tests_manual/{docId}`
- `text`
- `createdAt`
- `updatedAt`

Used by List screen buttons:
- Add
- Update Latest
- Delete Latest

## SQL-Style Compatibility Collections (for checkpoint checks)

### `/credentials/{userId}`
- `credential_id`
- `user_id`
- `username`
- `password_hash`
- `hash_algorithm`
- `created_at`

### `/spotify_links/{userId}`
- `spotify_link_id`
- `user_id`
- `spotify_user_id`
- `access_token`
- `refresh_token`
- `token_expires_at`

### `/friendships/{requester_addressee}`
- `friendship_id`
- `requester_user_id`
- `addressee_user_id`
- `status`
- `requested_at`
- `accepted_at`

### `/track_artists/{track_artist}`
- `track_id`
- `artist_id`

### `/ranking_lists/{user_list}`
- `ranking_list_id`
- `user_id`
- `name`
- `created_at`

### `/ranking_entries/{list_track}`
- `ranking_entry_id`
- `ranking_list_id`
- `track_id`
- `rank_position`
- `updated_at`

### `/comparison_sessions/{pair_timestamp}`
- `comparison_id`
- `user_a_id`
- `user_b_id`
- `created_at`
- `overlap_count`
- `similarity_score`

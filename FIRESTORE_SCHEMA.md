# Firestore Data Model (Denormalized)

Based on the SQL schema, this is a Firestore-friendly denormalized structure.

## Core Collections

### `/users/{uid}`
- `displayName`
- `username`
- `email`
- `createdAt`
- `spotify`: `{ spotifyUserId, tokenExpiresAt }`

Note: Keep sensitive OAuth tokens in a private subcollection if needed.

### `/usernames/{username}`
- `uid`

Used to enforce unique usernames via transaction.

### `/emails/{emailHashOrKey}`
- `uid`

Used to enforce unique emails via transaction.

### `/artists/{spotifyArtistId}`
- `name`

### `/albums/{spotifyAlbumId}`
- `title`
- `releaseDate`
- `artistIds: []`
- `artistNames: []` (denormalized)

### `/tracks/{spotifyTrackId}`
- `title`
- `durationMs`
- `explicit`
- `albumId`
- `albumTitle`
- `artistIds: []`
- `artistNames: []`

## User-Scoped Subcollections

### `/users/{uid}/friends/{otherUid}`
- `status` (`PENDING | ACCEPTED | BLOCKED`)
- `requestedAt`
- `acceptedAt`
- `direction` (`OUTGOING | INCOMING`)

Store friendship docs on both users for fast reads.

### `/users/{uid}/ratings/{trackId}`
- `score`
- `comment`
- `createdAt`
- `updatedAt`
- `trackTitle`
- `artistNames`
- `albumTitle`

Optional mirror for track-centric reads:
- `/tracks/{trackId}/ratings/{uid}`

### `/users/{uid}/rankingLists/{listId}`
- `name`
- `createdAt`

### `/users/{uid}/rankingLists/{listId}/entries/{trackId}`
- `rankPosition`
- `updatedAt`
- `trackTitle`
- `artistNames`

## Comparison Sessions

### `/comparisonSessions/{pairId_timestamp}`
- `userAId`
- `userBId`
- `createdAt`
- `overlapCount`
- `similarityScore`
- `pairId`

Recommended `pairId` format:
- `min(uidA, uidB) + "_" + max(uidA, uidB)`

## Firestore-Specific Notes

- No joins: duplicate small, read-heavy fields (names/titles) where helpful.
- No built-in unique constraints: enforce with transactions + lock docs (`/usernames/...`, `/emails/...`).
- Many-to-many (`track_artists`) maps well to arrays on track/album docs.
- Create composite indexes for common query patterns (for example: `status + requestedAt`, `user + createdAt`, `pairId + createdAt`).

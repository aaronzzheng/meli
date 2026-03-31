package com.example.meli.model

data class ProfileRankingActivity(
    val id: String,
    val actorUid: String,
    val actorName: String,
    val actorImageBase64: String?,
    val listId: String,
    val entryId: String,
    val trackTitle: String,
    val artistText: String,
    val rankingScore: Double?,
    val updatedAtMillis: Long,
    val listName: String?,
    val imageUrl: String?,
    val notes: String,
    val likeCount: Int,
    val commentCount: Int,
    val likedByCurrentUser: Boolean
)

data class FeedComment(
    val id: String,
    val actorUid: String,
    val actorName: String,
    val actorImageBase64: String?,
    val text: String,
    val createdAtMillis: Long,
    val likeCount: Int,
    val likedByCurrentUser: Boolean,
    val replyToName: String?
)

package com.example.meli.model

data class UserProfileSummary(
    val uid: String,
    val displayName: String,
    val username: String,
    val email: String,
    val friendCount: Int,
    val friendshipStatus: FriendshipStatus
)

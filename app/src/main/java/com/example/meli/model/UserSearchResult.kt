package com.example.meli.model

data class UserSearchResult(
    val uid: String,
    val displayName: String,
    val username: String,
    val email: String,
    val friendshipStatus: FriendshipStatus
)

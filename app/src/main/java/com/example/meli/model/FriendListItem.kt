package com.example.meli.model

data class FriendListItem(
    val uid: String,
    val displayName: String,
    val username: String,
    val email: String,
    val friendshipStatus: FriendshipStatus = FriendshipStatus.FRIENDS
)

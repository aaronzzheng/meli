package com.example.meli.model

data class AppNotification(
    val id: String,
    val type: String,
    val actorUid: String,
    val actorName: String,
    val trackId: String = "",
    val message: String,
    val createdAtMillis: Long,
    val status: String,
    val isRead: Boolean
)

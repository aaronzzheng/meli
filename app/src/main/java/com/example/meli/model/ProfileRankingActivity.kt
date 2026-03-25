package com.example.meli.model

data class ProfileRankingActivity(
    val id: String,
    val actorUid: String,
    val actorName: String,
    val trackTitle: String,
    val artistText: String,
    val rankingScore: Double?,
    val updatedAtMillis: Long,
    val listName: String?
)

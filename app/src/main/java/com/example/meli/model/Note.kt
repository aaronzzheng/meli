package com.example.meli.model

import com.google.firebase.Timestamp

data class Note(
    val id: String = "",
    val ownerUid: String = "",
    var title: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
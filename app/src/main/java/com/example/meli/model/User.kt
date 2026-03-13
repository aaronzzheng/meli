package com.example.meli.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val email: String = "",
    val createdAt: Timestamp? = null
)
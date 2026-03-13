package com.example.meli.data.repository

import com.example.meli.model.ListStatus

interface ListStatusRepository {
    fun loadStatus(onResult: (ListStatus) -> Unit)
    fun addListItem(text: String, onComplete: (Result<Unit>) -> Unit)
    fun updateLatestListItem(text: String, onComplete: (Result<Unit>) -> Unit)
    fun deleteLatestListItem(onComplete: (Result<Unit>) -> Unit)
}

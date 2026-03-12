package com.example.meli.data.repository

import com.example.meli.model.ListStatus

interface ListStatusRepository {
    fun loadStatus(onResult: (ListStatus) -> Unit)
}

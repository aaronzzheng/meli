package com.example.meli.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.meli.data.repository.FirestoreListStatusRepository
import com.example.meli.data.repository.ListStatusRepository

class ListViewModel : ViewModel() {

    private val repository: ListStatusRepository = FirestoreListStatusRepository()

    private val _text = MutableLiveData("Loading data-store status...")
    val text: LiveData<String> = _text

    init {
        refresh()
    }

    fun refresh() {
        repository.loadStatus { status ->
            _text.postValue("${status.message}\nLayer source: ${status.source}")
        }
    }
}

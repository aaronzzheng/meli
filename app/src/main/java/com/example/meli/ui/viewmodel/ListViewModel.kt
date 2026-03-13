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
    private val _addResult = MutableLiveData<String>()
    val addResult: LiveData<String> = _addResult

    init {
        refresh()
    }

    fun refresh() {
        repository.loadStatus { status ->
            _text.postValue("${status.message}\nLayer source: ${status.source}")
        }
    }

    fun addItem(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            _addResult.value = "Please enter something first."
            return
        }

        _addResult.value = "Adding..."
        repository.addListItem(trimmed) { result ->
            result.onSuccess {
                _addResult.postValue("Added to Firestore.")
                refresh()
            }.onFailure { error ->
                _addResult.postValue("Add failed: ${error.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun updateLatestItem(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            _addResult.value = "Enter text to update the latest item."
            return
        }

        _addResult.value = "Updating latest item..."
        repository.updateLatestListItem(trimmed) { result ->
            result.onSuccess {
                _addResult.postValue("Latest item updated.")
                refresh()
            }.onFailure { error ->
                _addResult.postValue("Update failed: ${error.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun deleteLatestItem() {
        _addResult.value = "Deleting latest item..."
        repository.deleteLatestListItem { result ->
            result.onSuccess {
                _addResult.postValue("Latest item deleted.")
                refresh()
            }.onFailure { error ->
                _addResult.postValue("Delete failed: ${error.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun onAddResultConsumed() {
        _addResult.value = ""
    }
}

package com.example.meli.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.meli.data.repository.NoteRepository
import kotlinx.coroutines.launch

class NotesViewModel : ViewModel() {
    private val repo = NoteRepository()
    val notes = repo.getNotes().asLiveData()

    fun addNote(title: String) = viewModelScope.launch { repo.createNote(title) }
    fun updateNote(id: String, title: String) = viewModelScope.launch { repo.updateNote(id, title) }
    fun deleteNote(id: String) = viewModelScope.launch { repo.deleteNote(id) }
}
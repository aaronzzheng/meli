package com.example.meli.data.repository

import com.example.meli.model.Note
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NoteRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getNotes(): Flow<List<Note>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: return@callbackFlow
        val subscription = firestore.collection("notes")
            .whereEqualTo("ownerUid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    trySend(it.toObjects(Note::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun createNote(title: String) {
        val uid = auth.currentUser?.uid ?: return
        val doc = firestore.collection("notes").document()
        val note = Note(doc.id, uid, title, Timestamp.now(), Timestamp.now())
        doc.set(note).await()
    }

    suspend fun updateNote(noteId: String, newTitle: String) {
        firestore.collection("notes").document(noteId)
            .update("title", newTitle, "updatedAt", Timestamp.now()).await()
    }

    suspend fun deleteNote(noteId: String) {
        firestore.collection("notes").document(noteId).delete().await()
    }
}
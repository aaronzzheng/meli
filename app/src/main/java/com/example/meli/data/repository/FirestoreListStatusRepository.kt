package com.example.meli.data.repository

import com.example.meli.model.ListStatus
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreListStatusRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ListStatusRepository {

    override fun loadStatus(onResult: (ListStatus) -> Unit) {
        firestore.collection("codex_connection_tests_manual")
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val message = if (snapshot.isEmpty) {
                    "Connected to Firestore. No manual-check docs yet."
                } else {
                    "Connected to Firestore. Found ${snapshot.size()} manual-check document(s)."
                }
                onResult(ListStatus(message = message, source = "Firestore"))
            }
            .addOnFailureListener { error ->
                onResult(
                    ListStatus(
                        message = "Firestore read failed: ${error.localizedMessage ?: "Unknown error"}",
                        source = "Firestore"
                    )
                )
            }
    }
}

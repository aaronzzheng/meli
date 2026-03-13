package com.example.meli.data.repository

import com.example.meli.model.ListStatus
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class FirestoreListStatusRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ListStatusRepository {

    override fun loadStatus(onResult: (ListStatus) -> Unit) {
        firestore.collection("tests_manual")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { snapshot ->
                val message = if (snapshot.isEmpty) {
                    "Connected to Firestore. No manual-check docs yet."
                } else {
                    val items = snapshot.documents.joinToString(separator = "\n") { doc ->
                        val text = doc.getString("text")
                            ?: doc.getString("note")
                            ?: "(no text field)"
                        "- $text"
                    }
                    "Connected to Firestore. Latest ${snapshot.size()} item(s):\n$items"
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

    override fun addListItem(text: String, onComplete: (Result<Unit>) -> Unit) {
        val docId = "item-${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val payload = mapOf(
            "text" to text,
            "createdAt" to now,
            "updatedAt" to now
        )

        firestore.collection("tests_manual")
            .document(docId)
            .set(payload)
            .addOnSuccessListener {
                onComplete(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                onComplete(Result.failure(error))
            }
    }

    override fun updateLatestListItem(text: String, onComplete: (Result<Unit>) -> Unit) {
        val now = System.currentTimeMillis()
        firestore.collection("tests_manual")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val latestDoc = snapshot.documents.firstOrNull()
                if (latestDoc == null) {
                    onComplete(Result.failure(IllegalStateException("No items found to update.")))
                    return@addOnSuccessListener
                }

                latestDoc.reference
                    .update(
                        mapOf(
                            "text" to text,
                            "updatedAt" to now
                        )
                    )
                    .addOnSuccessListener { onComplete(Result.success(Unit)) }
                    .addOnFailureListener { error -> onComplete(Result.failure(error)) }
            }
            .addOnFailureListener { error ->
                onComplete(Result.failure(error))
            }
    }

    override fun deleteLatestListItem(onComplete: (Result<Unit>) -> Unit) {
        firestore.collection("tests_manual")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val latestDoc = snapshot.documents.firstOrNull()
                if (latestDoc == null) {
                    onComplete(Result.failure(IllegalStateException("No items found to delete.")))
                    return@addOnSuccessListener
                }

                latestDoc.reference
                    .delete()
                    .addOnSuccessListener { onComplete(Result.success(Unit)) }
                    .addOnFailureListener { error -> onComplete(Result.failure(error)) }
            }
            .addOnFailureListener { error ->
                onComplete(Result.failure(error))
            }
    }
}

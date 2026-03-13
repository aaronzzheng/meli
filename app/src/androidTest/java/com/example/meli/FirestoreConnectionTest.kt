package com.example.meli

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FirestoreConnectionTest {

    @Test
    fun firestoreCrud_succeeds() {
        val firestore = FirebaseFirestore.getInstance()
        val docId = "connectivity-${UUID.randomUUID()}"
        val docRef = firestore.collection("connection_tests").document(docId)
        val createdAt = System.currentTimeMillis()
        val initialPayload = mapOf(
            "status" to "created",
            "counter" to 1L,
            "createdAt" to createdAt
        )

        // Create
        Tasks.await(docRef.set(initialPayload), 15, TimeUnit.SECONDS)
        val createdSnapshot = Tasks.await(docRef.get(Source.SERVER), 15, TimeUnit.SECONDS)
        assertTrue("Expected document to exist after create", createdSnapshot.exists())
        assertEquals("created", createdSnapshot.getString("status"))
        assertEquals(1L, createdSnapshot.getLong("counter"))
        assertNotNull(createdSnapshot.getLong("createdAt"))

        // Update
        Tasks.await(docRef.update(mapOf("status" to "updated", "counter" to 2L)), 15, TimeUnit.SECONDS)
        val updatedSnapshot = Tasks.await(docRef.get(Source.SERVER), 15, TimeUnit.SECONDS)
        assertTrue("Expected document to exist after update", updatedSnapshot.exists())
        assertEquals("updated", updatedSnapshot.getString("status"))
        assertEquals(2L, updatedSnapshot.getLong("counter"))
        assertEquals(createdAt, updatedSnapshot.getLong("createdAt"))

        // Delete
        Tasks.await(docRef.delete(), 15, TimeUnit.SECONDS)
        var deleted = false
        repeat(5) {
            val deletedSnapshot = Tasks.await(docRef.get(Source.SERVER), 15, TimeUnit.SECONDS)
            if (!deletedSnapshot.exists()) {
                deleted = true
                return@repeat
            }
            Thread.sleep(300)
        }
        assertTrue("Expected document to be deleted", deleted)
    }

    @Test
    fun firestoreCreate_persistsForManualCheck() {
        val firestore = FirebaseFirestore.getInstance()
        val docId = "manual-check-${UUID.randomUUID()}"
        val docRef = firestore.collection("tests_manual").document(docId)
        val payload = mapOf(
            "status" to "created_for_manual_check",
            "createdAt" to System.currentTimeMillis(),
            "note" to "This document is intentionally not deleted by test."
        )

        Tasks.await(docRef.set(payload), 15, TimeUnit.SECONDS)
        val snapshot = Tasks.await(docRef.get(Source.SERVER), 15, TimeUnit.SECONDS)

        assertTrue("Expected manual-check document to exist", snapshot.exists())
        assertEquals("created_for_manual_check", snapshot.getString("status"))
        assertNotNull(snapshot.getLong("createdAt"))
    }
}

package com.example.meli

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FirestoreSchemaAuditTest {

    @Test
    fun auditSchemaCollections_presenceReport() {
        val db = FirebaseFirestore.getInstance()
        val timeoutSeconds = 20L

        val report = mutableListOf<String>()
        val topLevelCollections = listOf(
            "users",
            "usernames",
            "emails",
            "artists",
            "albums",
            "tracks",
            "comparisonSessions",
            // SQL-style names from the checkpoint schema for exact-name checks.
            "credentials",
            "spotify_links",
            "friendships",
            "track_artists",
            "ranking_lists",
            "ranking_entries",
            "comparison_sessions"
        )

        topLevelCollections.forEach { name ->
            val query = db.collection(name).limit(1)
            val snapshot = Tasks.await(query.get(Source.SERVER), timeoutSeconds, TimeUnit.SECONDS)
            report += "collection=$name hasData=${!snapshot.isEmpty}"
        }

        val usersSnapshot = Tasks.await(
            db.collection("users").limit(1).get(Source.SERVER),
            timeoutSeconds,
            TimeUnit.SECONDS
        )

        if (!usersSnapshot.isEmpty) {
            val uid = usersSnapshot.documents.first().id
            val userRef = db.collection("users").document(uid)

            val friends = Tasks.await(
                userRef.collection("friends").limit(1).get(Source.SERVER),
                timeoutSeconds,
                TimeUnit.SECONDS
            )
            val ratings = Tasks.await(
                userRef.collection("ratings").limit(1).get(Source.SERVER),
                timeoutSeconds,
                TimeUnit.SECONDS
            )
            val rankingLists = Tasks.await(
                userRef.collection("rankingLists").limit(1).get(Source.SERVER),
                timeoutSeconds,
                TimeUnit.SECONDS
            )

            report += "subcollection=users/$uid/friends hasData=${!friends.isEmpty}"
            report += "subcollection=users/$uid/ratings hasData=${!ratings.isEmpty}"
            report += "subcollection=users/$uid/rankingLists hasData=${!rankingLists.isEmpty}"

            if (!rankingLists.isEmpty) {
                val rankingListId = rankingLists.documents.first().id
                val entries = Tasks.await(
                    userRef.collection("rankingLists")
                        .document(rankingListId)
                        .collection("entries")
                        .limit(1)
                        .get(Source.SERVER),
                    timeoutSeconds,
                    TimeUnit.SECONDS
                )
                report += "subcollection=users/$uid/rankingLists/$rankingListId/entries hasData=${!entries.isEmpty}"
            } else {
                report += "subcollection=users/<uid>/rankingLists/<listId>/entries hasData=false (no ranking list found)"
            }
        } else {
            report += "subcollections=users/* skipped (no users found)"
        }

        val trackRatingsSnapshot = Tasks.await(
            db.collectionGroup("ratings").limit(1).get(Source.SERVER),
            timeoutSeconds,
            TimeUnit.SECONDS
        )
        report += "collectionGroup=ratings hasData=${!trackRatingsSnapshot.isEmpty}"

        val output = report.joinToString(separator = "\n")
        Log.i("FirestoreSchemaAudit", "\n$output")
        println("Firestore schema audit report:\n$output")

        assertNotNull(output)
    }
}

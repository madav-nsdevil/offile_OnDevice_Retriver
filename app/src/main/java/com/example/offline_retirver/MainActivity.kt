    package com.example.offline_retirver

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.example.offline_retirver.app.data.db.KnowledgeBaseDao
import com.example.offline_retirver.app.data.domain.model.SearchResult
import com.example.offline_retirver.app.data.retrieval.VectorSearchEngine
import com.example.offline_retirver.app.data.sync.DeltaSyncWorker
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Schedule background delta sync (runs only when network is available)
        scheduleDeltaSync()

        // ✅ Open prebuilt local DB from assets (offline database)
        val db = openLocalDbFromAssets("knowledge_base.db")
        val dao = KnowledgeBaseDao(db)
        val engine = VectorSearchEngine(dao, embeddingDim = 384)

        setContent {
            var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
            var status by remember { mutableStateOf("Running offline retriever...") }

            LaunchedEffect(Unit) {
                lifecycleScope.launch {
                    val fakeQueryEmbedding = FloatArray(384) { Random.nextFloat() }
                    results = engine.search(queryEmbedding = fakeQueryEmbedding)
                    status = "Got ${results.size} results"
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(status)
                Spacer(Modifier.height(12.dp))

                LazyColumn {
                    items(results) { r ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Title: ${r.documentTitle}")
                                Text("Score: ${r.score}")
                                Spacer(Modifier.height(6.dp))
                                Text(r.text.take(220))
                            }
                        }
                    }
                }
            }
        }
    }

    // ✅ Must be INSIDE the Activity so Context is available
    private fun scheduleDeltaSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request =
            PeriodicWorkRequestBuilder<DeltaSyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

        // ✅ Use explicit Activity context
        WorkManager.getInstance(this@MainActivity)
            .enqueueUniquePeriodicWork(
                "kb_delta_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )

        // Optional: run once immediately for testing (uncomment if needed)
        // WorkManager.getInstance(this@MainActivity)
        //     .enqueue(OneTimeWorkRequestBuilder<DeltaSyncWorker>().build())
    }

    private fun openLocalDbFromAssets(assetName: String): SQLiteDatabase {
        val dbDir = File(applicationInfo.dataDir, "databases")
        if (!dbDir.exists()) dbDir.mkdirs()

        val outFile = File(dbDir, assetName)
        if (!outFile.exists() || outFile.length() == 0L) {
            assets.open(assetName).use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return SQLiteDatabase.openDatabase(
            outFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )
    }
}

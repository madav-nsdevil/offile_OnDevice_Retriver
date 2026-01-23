package com.example.offline_retirver.app.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.offline_retirver.app.data.db.DbBootstrap3Tables
import com.example.offline_retirver.app.data.db.KnowledgeUpsertDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import android.database.sqlite.SQLiteDatabase

class DeltaSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val client = OkHttpClient()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefs = applicationContext.getSharedPreferences("kb_sync", Context.MODE_PRIVATE)
            val since = prefs.getLong("version", 0L)

            val baseUrl = "https://YOUR_DOMAIN" // TODO change
            val delta = getJson("$baseUrl/api/kb/delta?since=$since")

            val newVersion = delta.getLong("version")
            if (newVersion <= since) return@withContext Result.success()

            val courses = parseCourses(delta.optJSONArray("courses"))
            val documents = parseDocuments(delta.optJSONArray("documents"))
            val chunks = parseChunks(delta.optJSONArray("chunks"))

            val deleted = delta.optJSONObject("deleted")
            val deletedDocs = parseLongList(deleted?.optJSONArray("documents"))
            val deletedChunks = parseLongList(deleted?.optJSONArray("chunks"))

            val db = openLocalDb("knowledge_base.db")

            // Ensure schema exists (safe no-op if already exists)
            DbBootstrap3Tables.ensureSchema(db)

            val upsertDao = KnowledgeUpsertDao(db)
            upsertDao.applyDelta(
                courses = courses,
                documents = documents,
                chunks = chunks,
                deletedDocumentIds = deletedDocs,
                deletedChunkIds = deletedChunks
            )

            prefs.edit().putLong("version", newVersion).apply()
            db.close()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun getJson(url: String): JSONObject {
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            val body = resp.body?.string() ?: error("Empty body")
            return JSONObject(body)
        }
    }

    private fun openLocalDb(fileName: String): SQLiteDatabase {
        val dbDir = File(applicationContext.applicationInfo.dataDir, "databases")
        if (!dbDir.exists()) dbDir.mkdirs()
        val f = File(dbDir, fileName)
        return SQLiteDatabase.openDatabase(f.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
    }

    private fun parseCourses(arr: JSONArray?): List<KnowledgeUpsertDao.CourseRow> {
        if (arr == null) return emptyList()
        val out = ArrayList<KnowledgeUpsertDao.CourseRow>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                KnowledgeUpsertDao.CourseRow(
                    id = o.getLong("id"),
                    code = o.getString("code"),
                    subjectName = o.getString("subject_name")
                )
            )
        }
        return out
    }

    private fun parseDocuments(arr: JSONArray?): List<KnowledgeUpsertDao.DocumentRow> {
        if (arr == null) return emptyList()
        val out = ArrayList<KnowledgeUpsertDao.DocumentRow>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                KnowledgeUpsertDao.DocumentRow(
                    id = o.getLong("id"),
                    courseId = o.getLong("course_id"),
                    title = o.getString("title"),
                    fileType = o.optString("file_type", null)
                )
            )
        }
        return out
    }

    private fun parseChunks(arr: JSONArray?): List<KnowledgeUpsertDao.ChunkRow> {
        if (arr == null) return emptyList()
        val out = ArrayList<KnowledgeUpsertDao.ChunkRow>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)

            val vectorJson = o.getJSONArray("vector").toString()

            out.add(
                KnowledgeUpsertDao.ChunkRow(
                    id = o.getLong("id"),
                    documentId = o.getLong("document_id"),
                    chunkIndex = o.getInt("chunk_index"),
                    text = o.getString("text"),
                    vectorJson = vectorJson,
                    embedding = o.optString("embedding", null)
                )
            )
        }
        return out
    }

    private fun parseLongList(arr: JSONArray?): List<Long> {
        if (arr == null) return emptyList()
        val out = ArrayList<Long>(arr.length())
        for (i in 0 until arr.length()) out.add(arr.getLong(i))
        return out
    }
}

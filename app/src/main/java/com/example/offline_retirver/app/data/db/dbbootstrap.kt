package com.example.vector_retrieval.app.data.db

import android.database.sqlite.SQLiteDatabase
import kotlin.random.Random

object DbBootstrap3Tables {

    fun ensureSchema(db: SQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys = ON;")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS courses (
              id INTEGER PRIMARY KEY,
              code TEXT NOT NULL UNIQUE,
              subject_name TEXT NOT NULL
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS documents (
              id INTEGER PRIMARY KEY,
              course_id INTEGER NOT NULL,
              title TEXT NOT NULL,
              file_type TEXT,
              FOREIGN KEY(course_id) REFERENCES courses(id) ON DELETE CASCADE
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS chunks (
              id INTEGER PRIMARY KEY,
              document_id INTEGER NOT NULL,
              chunk_index INTEGER NOT NULL,
              text TEXT NOT NULL,
              vector TEXT NOT NULL,          -- JSON array string: [0.1, 0.2, ...]
              embedding TEXT,                -- model name (optional)
              FOREIGN KEY(document_id) REFERENCES documents(id) ON DELETE CASCADE
            );
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_docs_course ON documents(course_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chunks_doc ON chunks(document_id)")
    }

    /** Only seeds if chunks is empty */
    fun seedFakeIfEmpty(db: SQLiteDatabase, dims: Int = 384) {
        val count = db.rawQuery("SELECT COUNT(*) FROM chunks", null).use {
            it.moveToFirst()
            it.getInt(0)
        }
        if (count > 0) return

        db.beginTransaction()
        try {
            db.execSQL(
                "INSERT INTO courses(id, code, subject_name) VALUES(?,?,?)",
                arrayOf(1, "CS101", "Intro to CS")
            )

            db.execSQL(
                "INSERT INTO documents(id, course_id, title, file_type) VALUES(?,?,?,?)",
                arrayOf(1, 1, "Lecture 1", "txt")
            )

            val texts = listOf(
                "Vectors represent meaning of text.",
                "Cosine similarity compares embeddings.",
                "Offline RAG retrieves top chunks."
            )

            texts.forEachIndexed { idx, t ->
                val vecJson = randomVectorJson(dims)
                db.execSQL(
                    "INSERT INTO chunks(id, document_id, chunk_index, text, vector, embedding) VALUES(?,?,?,?,?,?)",
                    arrayOf(idx + 1, 1, idx, t, vecJson, "fake-$dims")
                )
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun randomVectorJson(dims: Int): String {
        val v = FloatArray(dims) { Random.nextFloat() }
        return buildString {
            append('[')
            for (i in 0 until dims) {
                if (i > 0) append(',')
                append(v[i])
            }
            append(']')
        }
    }
}

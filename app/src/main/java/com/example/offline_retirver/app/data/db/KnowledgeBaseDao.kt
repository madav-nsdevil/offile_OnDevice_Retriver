package com.example.offline_retirver.app.data.db

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

class KnowledgeBaseDao(private val db: SQLiteDatabase) {

    fun streamAllChunks(): Cursor {
        return db.rawQuery(
            """
            SELECT 
                ch.id AS id,
                ch.document_id AS document_id,
                ch.text AS text,
                ch.chunk_index AS chunk_index,
                ch.vector AS vector,
                d.title AS title
            FROM chunks ch
            JOIN documents d ON d.id = ch.document_id
            """.trimIndent(),
            null
        )
    }
}

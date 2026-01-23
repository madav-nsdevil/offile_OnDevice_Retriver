package com.example.offline_retirver.app.data.db

import android.database.sqlite.SQLiteDatabase

class KnowledgeUpsertDao(private val db: SQLiteDatabase) {

    data class CourseRow(val id: Long, val code: String, val subjectName: String)
    data class DocumentRow(val id: Long, val courseId: Long, val title: String, val fileType: String?)
    data class ChunkRow(
        val id: Long,
        val documentId: Long,
        val chunkIndex: Int,
        val text: String,
        val vectorJson: String,
        val embedding: String?
    )

    fun applyDelta(
        courses: List<CourseRow>,
        documents: List<DocumentRow>,
        chunks: List<ChunkRow>,
        deletedDocumentIds: List<Long> = emptyList(),
        deletedChunkIds: List<Long> = emptyList()
    ) {
        // IMPORTANT: single transaction so you never end up half-synced
        db.beginTransaction()
        try {
            upsertCourses(courses)
            upsertDocuments(documents)
            upsertChunks(chunks)

            if (deletedChunkIds.isNotEmpty()) deleteByIds("chunks", deletedChunkIds)
            if (deletedDocumentIds.isNotEmpty()) deleteByIds("documents", deletedDocumentIds)

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun upsertCourses(rows: List<CourseRow>) {
        if (rows.isEmpty()) return
        val stmt = db.compileStatement("""
            INSERT INTO courses(id, code, subject_name)
            VALUES(?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
              code=excluded.code,
              subject_name=excluded.subject_name
        """.trimIndent())

        try {
            for (r in rows) {
                stmt.bindLong(1, r.id)
                stmt.bindString(2, r.code)
                stmt.bindString(3, r.subjectName)
                stmt.executeInsert()
                stmt.clearBindings()
            }
        } finally {
            stmt.close()
        }
    }

    private fun upsertDocuments(rows: List<DocumentRow>) {
        if (rows.isEmpty()) return
        val stmt = db.compileStatement("""
            INSERT INTO documents(id, course_id, title, file_type)
            VALUES(?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
              course_id=excluded.course_id,
              title=excluded.title,
              file_type=excluded.file_type
        """.trimIndent())

        try {
            for (r in rows) {
                stmt.bindLong(1, r.id)
                stmt.bindLong(2, r.courseId)
                stmt.bindString(3, r.title)
                if (r.fileType == null) stmt.bindNull(4) else stmt.bindString(4, r.fileType)
                stmt.executeInsert()
                stmt.clearBindings()
            }
        } finally {
            stmt.close()
        }
    }

    private fun upsertChunks(rows: List<ChunkRow>) {
        if (rows.isEmpty()) return
        val stmt = db.compileStatement("""
            INSERT INTO chunks(id, document_id, chunk_index, text, vector, embedding)
            VALUES(?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
              document_id=excluded.document_id,
              chunk_index=excluded.chunk_index,
              text=excluded.text,
              vector=excluded.vector,
              embedding=excluded.embedding
        """.trimIndent())

        try {
            for (r in rows) {
                stmt.bindLong(1, r.id)
                stmt.bindLong(2, r.documentId)
                stmt.bindLong(3, r.chunkIndex.toLong())
                stmt.bindString(4, r.text)
                stmt.bindString(5, r.vectorJson)
                if (r.embedding == null) stmt.bindNull(6) else stmt.bindString(6, r.embedding)
                stmt.executeInsert()
                stmt.clearBindings()
            }
        } finally {
            stmt.close()
        }
    }

    private fun deleteByIds(table: String, ids: List<Long>) {
        val placeholders = ids.joinToString(",") { "?" }
        val args = ids.map { it.toString() }.toTypedArray()
        db.execSQL("DELETE FROM $table WHERE id IN ($placeholders)", args)
    }
}

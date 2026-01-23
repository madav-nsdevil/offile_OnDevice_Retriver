package com.example.offline_retirver.app.data.db

import android.database.sqlite.SQLiteDatabase
import java.io.File

class KnowledgeBaseDb private constructor(
    val dbFile: File,
    val db: SQLiteDatabase
) : AutoCloseable {

    override fun close() {
        db.close()
    }

    companion object {
        fun openReadOnly(dbFile: File): KnowledgeBaseDb {
            val db = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

            // Conservative performance tweaks (safe for read-only DB)
            db.execSQL("PRAGMA temp_store = MEMORY;")
            db.execSQL("PRAGMA cache_size = -20000;") // ~20MB page cache (negative => KB)
            db.execSQL("PRAGMA query_only = ON;")
            db.execSQL("PRAGMA mmap_size = 268435456;") // 256MB mmap if supported

            return KnowledgeBaseDb(dbFile, db)
        }
    }
}

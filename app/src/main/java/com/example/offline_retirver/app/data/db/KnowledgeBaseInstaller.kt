package com.example.offline_retirver.app.data.db

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object KnowledgeBaseInstaller {

    /**
     * Installs a knowledge base DB into app-internal storage:
     * - from assets (assetsPath) OR
     * - from a provided File (sourceFile)
     *
     * Returns the installed DB file.
     */
    fun installOrGet(
        context: Context,
        dbName: String,
        assetsPath: String? = null,
        sourceFile: File? = null
    ): File {
        require((assetsPath != null) xor (sourceFile != null)) {
            "Provide exactly one of assetsPath or sourceFile"
        }

        val dbDir = File(context.noBackupFilesDir, "knowledge_dbs").apply { mkdirs() }
        val target = File(dbDir, dbName)

        // If already installed and non-empty, reuse.
        if (target.exists() && target.length() > 1024) return target

        // Copy atomically via temp file.
        val tmp = File(dbDir, "$dbName.tmp")
        if (tmp.exists()) tmp.delete()

        FileOutputStream(tmp).use { out ->
            if (assetsPath != null) {
                context.assets.open(assetsPath).use { input -> input.copyTo(out) }
            } else {
                FileInputStream(sourceFile!!).use { input -> input.copyTo(out) }
            }
        }

        // Replace target
        if (target.exists()) target.delete()
        check(tmp.renameTo(target)) { "Failed to move temp DB into place" }

        return target
    }
}

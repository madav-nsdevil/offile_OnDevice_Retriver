package com.example.offline_retirver.app.data.domain.repo

import android.content.Context
import com.example.offline_retirver.app.data.db.KnowledgeBaseDao
import com.example.offline_retirver.app.data.db.KnowledgeBaseDb
import com.example.offline_retirver.app.data.db.KnowledgeBaseInstaller
import com.example.offline_retirver.app.data.retrieval.VectorSearchEngine
import com.example.offline_retirver.app.data.domain.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable

class KnowledgeRepository private constructor(
    private val kbDb: KnowledgeBaseDb,
    private val dao: KnowledgeBaseDao,
    private val searchEngine: VectorSearchEngine
) : Closeable {

    override fun close() = kbDb.close()

    suspend fun ensureIndexes():Unit=
        withContext(Dispatchers.IO) {
//            dao.ensureFts()
        }


    suspend fun search(
        queryEmbedding: FloatArray,
        topK: Int = 450
    ): List<SearchResult> {
        return searchEngine.search(
            queryEmbedding = queryEmbedding,
            params = VectorSearchEngine.Params(topK = topK)
        )
    }

    companion object {
        fun fromAssets(
            context: Context,
            assetDbPath: String,
            installedDbName: String = "knowledge_base.db"
        ): KnowledgeRepository {
            val dbFile = KnowledgeBaseInstaller.installOrGet(
                context = context,
                dbName = installedDbName,
                assetsPath = assetDbPath
            )
            val kbDb = KnowledgeBaseDb.openReadOnly(dbFile)
            val dao = KnowledgeBaseDao(kbDb.db)
            val engine = VectorSearchEngine(dao, embeddingDim = 384)
            return KnowledgeRepository(kbDb, dao, engine)
        }
    }
}

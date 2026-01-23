package com.example.offline_retirver.app.data.retrieval

import android.database.Cursor
import com.example.offline_retirver.app.data.db.KnowledgeBaseDao
import com.example.offline_retirver.app.data.domain.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VectorSearchEngine(
    private val dao: KnowledgeBaseDao,
    private val embeddingDim: Int = 384
) {
    data class Params(
        val topK: Int = 50,
        val minScore: Float? = null
    )

    suspend fun search(
        queryEmbedding: FloatArray,
        params: Params = Params()
    ): List<SearchResult> = withContext(Dispatchers.IO) {

        require(queryEmbedding.size == embeddingDim) { "Query embedding dim mismatch" }

        val cursor: Cursor = dao.streamAllChunks()

        cursor.use { c ->
            val heap = TopKHeap<SearchResult>(params.topK) { it.score }

            val colId = c.getColumnIndexOrThrow("id")
            val colDocId = c.getColumnIndexOrThrow("document_id")
            val colText = c.getColumnIndexOrThrow("text")
            val colChunkIndex = c.getColumnIndexOrThrow("chunk_index")
            val colVector = c.getColumnIndexOrThrow("vector")
            val colTitle = c.getColumnIndexOrThrow("title")

            while (c.moveToNext()) {
                val chunkId = c.getLong(colId)
                val docId = c.getLong(colDocId)
                val text = c.getString(colText)
                val chunkIndex = c.getInt(colChunkIndex)
                val title = c.getString(colTitle)
                val vectorJson = c.getString(colVector)

                val chunkVec = try {
                    JsonVectorParser.parseToFloatArray(vectorJson, embeddingDim)
                } catch (_: Exception) {
                    continue
                }

                val score = Cosine.cosineSimilarity(queryEmbedding, chunkVec)
                if (params.minScore != null && score < params.minScore) continue

                heap.offer(
                    SearchResult(
                        chunkId = chunkId,
                        documentId = docId,
                        documentTitle = title,
                        chunkIndex = chunkIndex,
                        text = text,
                        score = score
                    )
                )
            }

            heap.toSortedListDesc()
        }
    }
}

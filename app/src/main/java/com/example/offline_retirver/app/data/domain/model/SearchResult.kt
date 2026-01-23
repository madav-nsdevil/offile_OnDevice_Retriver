package com.example.offline_retirver.app.data.domain.model

data class SearchResult(
    val chunkId: Long,
    val documentId: Long,
    val documentTitle: String,
    val chunkIndex: Int,
    val text: String,
    val score: Float
)


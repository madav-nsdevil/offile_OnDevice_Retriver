package com.example.offline_retirver.app.data.db


data class ChunkRow(
    val id: Long,
    val documentId: Long,
    val text: String,
    val chunkIndex: Int,
    val vectorJson: String,
    val documentTitle: String
)

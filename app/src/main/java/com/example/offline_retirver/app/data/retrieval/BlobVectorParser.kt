package com.example.vector_retrieval.app.data.retrieval

import java.nio.ByteBuffer
import java.nio.ByteOrder

object BlobVectorParser {
    fun parseFloatArray(blob: ByteArray, expectedDim: Int): FloatArray {
        if (blob.size != expectedDim * 4) {
            throw IllegalArgumentException("Blob size mismatch: ${blob.size} bytes, expected ${expectedDim * 4}")
        }
        val bb = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN)
        val out = FloatArray(expectedDim)
        for (i in 0 until expectedDim) out[i] = bb.getFloat()
        return out
    }
}

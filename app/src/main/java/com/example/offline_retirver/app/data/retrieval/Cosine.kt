package com.example.offline_retirver.app.data.retrieval

import kotlin.math.sqrt

object Cosine {
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var na = 0f
        var nb = 0f
        for (i in a.indices) {
            val x = a[i]
            val y = b[i]
            dot += x * y
            na += x * x
            nb += y * y
        }
        val denom = sqrt(na) * sqrt(nb)
        return if (denom == 0f) 0f else dot / denom
    }
}

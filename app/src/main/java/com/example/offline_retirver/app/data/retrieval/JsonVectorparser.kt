package com.example.offline_retirver.app.data.retrieval

import org.json.JSONArray

object JsonVectorParser {
    fun parseToFloatArray(json: String, expectedDim: Int): FloatArray {
        val arr = JSONArray(json)
        if (arr.length() != expectedDim) {
            throw IllegalArgumentException("Vector dim mismatch: expected $expectedDim got ${arr.length()}")
        }
        return FloatArray(expectedDim) { i -> arr.getDouble(i).toFloat() }
    }
}

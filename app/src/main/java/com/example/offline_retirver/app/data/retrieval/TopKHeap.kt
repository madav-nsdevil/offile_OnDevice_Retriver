package com.example.offline_retirver.app.data.retrieval

import java.util.PriorityQueue

class TopKHeap<T>(
    private val k: Int,
    private val score: (T) -> Float
) {
    private val pq = PriorityQueue<T>(compareBy(score)) // min-heap

    fun offer(item: T) {
        if (k <= 0) return
        if (pq.size < k) {
            pq.add(item)
        } else {
            val min = pq.peek()
            if (score(item) > score(min)) {
                pq.poll()
                pq.add(item)
            }
        }
    }

    fun toSortedListDesc(): List<T> = pq.toList().sortedByDescending(score)
}

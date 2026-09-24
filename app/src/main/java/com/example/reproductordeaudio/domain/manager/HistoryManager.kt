package com.example.reproductordeaudio.domain.manager

import com.example.reproductordeaudio.data.repository.RoomRepository

class HistoryManager(private val roomRepository: RoomRepository) {

    fun shouldRecordHistory(playedDurationMs: Long, totalDurationMs: Long): Boolean {
        if (totalDurationMs <= 0) return false
        val threshold = if (totalDurationMs < 60_000) totalDurationMs / 2 else 30_000
        return playedDurationMs >= threshold
    }

    suspend fun recordPlayback(mediaStoreId: Long) {
        roomRepository.recordPlayback(mediaStoreId)
    }
}

package com.example.reproductordeaudio.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert
    suspend fun insertHistory(history: HistoryEntity)

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN (
            SELECT mediaStoreId, MAX(playedAt) as lastPlayed 
            FROM history 
            GROUP BY mediaStoreId
        ) h ON s.mediaStoreId = h.mediaStoreId
        ORDER BY h.lastPlayed DESC
        LIMIT 10
    """)
    fun getRecentSongs(): Flow<List<SongEntity>>

    @Query("DELETE FROM history WHERE mediaStoreId = :mediaStoreId")
    suspend fun deleteHistoryForSong(mediaStoreId: Long)
}

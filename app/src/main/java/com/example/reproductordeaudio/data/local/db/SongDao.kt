package com.example.reproductordeaudio.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE mediaStoreId = :id")
    suspend fun getSongById(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE mediaStoreId = :id")
    fun getSongFlowById(id: Long): Flow<SongEntity?>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE mediaStoreId = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedAt = :playedAt WHERE mediaStoreId = :id")
    suspend fun incrementPlayCount(id: Long, playedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM songs WHERE mediaStoreId = :id")
    suspend fun deleteSongById(id: Long)

    @Query("DELETE FROM songs WHERE mediaStoreId NOT IN (:ids)")
    suspend fun deleteOrphanSongs(ids: List<Long>)
}

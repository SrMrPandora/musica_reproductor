package com.example.reproductordeaudio.data.repository

import com.example.reproductordeaudio.data.local.db.AppDatabase
import com.example.reproductordeaudio.data.local.db.CardIdentityEntity
import com.example.reproductordeaudio.data.local.db.HistoryEntity
import com.example.reproductordeaudio.data.local.db.PlaylistEntity
import com.example.reproductordeaudio.data.local.db.PlaylistSongCrossRef
import com.example.reproductordeaudio.data.local.db.SongEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomRepository(private val db: AppDatabase) {

    val allSongs: Flow<List<SongEntity>> = db.songDao().getAllSongs()
    val favoriteSongs: Flow<List<SongEntity>> = db.songDao().getFavoriteSongs()
    val recentSongs: Flow<List<SongEntity>> = db.historyDao().getRecentSongs()
    val allPlaylists: Flow<List<PlaylistEntity>> = db.playlistDao().getAllPlaylists()
    val allCardIdentities: Flow<Map<Long, CardIdentityEntity>> = db.cardIdentityDao()
        .getAllCardIdentities()
        .map { list -> list.associateBy { it.mediaStoreId } }

    suspend fun getSongById(id: Long): SongEntity? = db.songDao().getSongById(id)

    fun getSongFlowById(id: Long): Flow<SongEntity?> = db.songDao().getSongFlowById(id)

    fun getCardIdentity(mediaStoreId: Long): Flow<CardIdentityEntity?> {
        return db.cardIdentityDao().getById(mediaStoreId)
    }

    suspend fun saveSongs(songs: List<SongEntity>) = db.songDao().insertSongs(songs)

    suspend fun toggleFavorite(mediaStoreId: Long, isFavorite: Boolean) {
        db.songDao().setFavorite(mediaStoreId, isFavorite)
    }

    suspend fun recordPlayback(mediaStoreId: Long) {
        val now = System.currentTimeMillis()
        db.songDao().incrementPlayCount(mediaStoreId, now)
        db.historyDao().insertHistory(HistoryEntity(mediaStoreId = mediaStoreId, playedAt = now))
    }

    suspend fun deleteSong(mediaStoreId: Long) {
        db.songDao().deleteSongById(mediaStoreId)
        db.historyDao().deleteHistoryForSong(mediaStoreId)
    }

    suspend fun deleteOrphans(existingIds: List<Long>) {
        db.songDao().deleteOrphanSongs(existingIds)
    }

    suspend fun createPlaylist(name: String): Long {
        return db.playlistDao().insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        db.playlistDao().deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, mediaStoreId: Long) {
        db.playlistDao().addSongToPlaylist(
            PlaylistSongCrossRef(playlistId = playlistId, mediaStoreId = mediaStoreId)
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, mediaStoreId: Long) {
        db.playlistDao().removeSongFromPlaylist(playlistId, mediaStoreId)
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<SongEntity>> {
        return db.playlistDao().getSongsForPlaylist(playlistId)
    }
}

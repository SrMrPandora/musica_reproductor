package com.example.reproductordeaudio.data.manager

import com.example.reproductordeaudio.data.repository.RoomRepository
import com.example.reproductordeaudio.domain.model.Song
import com.example.reproductordeaudio.domain.model.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaylistManager(private val roomRepository: RoomRepository) {

    val allPlaylists = roomRepository.allPlaylists

    suspend fun createPlaylist(name: String): Long {
        return roomRepository.createPlaylist(name.trim())
    }

    suspend fun deletePlaylist(playlistId: Long) {
        roomRepository.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, mediaStoreId: Long) {
        roomRepository.addSongToPlaylist(playlistId, mediaStoreId)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, mediaStoreId: Long) {
        roomRepository.removeSongFromPlaylist(playlistId, mediaStoreId)
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> {
        return roomRepository.getSongsForPlaylist(playlistId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}

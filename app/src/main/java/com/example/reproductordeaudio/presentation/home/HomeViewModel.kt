package com.example.reproductordeaudio.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.reproductordeaudio.data.local.db.AppDatabase
import com.example.reproductordeaudio.data.local.db.PlaylistEntity
import com.example.reproductordeaudio.data.local.db.SongEntity
import com.example.reproductordeaudio.data.manager.PlaylistManager
import com.example.reproductordeaudio.data.repository.MediaStoreRepository
import com.example.reproductordeaudio.data.repository.RoomRepository
import com.example.reproductordeaudio.domain.analyzer.ArtworkAnalyzer
import com.example.reproductordeaudio.domain.manager.HistoryManager
import com.example.reproductordeaudio.domain.manager.PlaybackManager
import com.example.reproductordeaudio.domain.model.Artist
import com.example.reproductordeaudio.domain.model.Playlist
import com.example.reproductordeaudio.domain.model.Song
import com.example.reproductordeaudio.domain.model.normalizeArtistName
import com.example.reproductordeaudio.domain.model.toDomain
import com.example.reproductordeaudio.domain.sync.LibrarySyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption {
    A_Z, Z_A, MOST_PLAYED, LEAST_PLAYED
}

data class HomeUiState(
    val songs: List<Song> = emptyList(),
    val favoriteSongs: List<Song> = emptyList(),
    val recentSongs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val isLoading: Boolean = false,
    val sortOption: SortOption = SortOption.A_Z,
    val selectedTab: Int = 0,
    val isSphereEffectEnabled: Boolean = true
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val roomRepository = RoomRepository(db)
    val mediaStoreRepository = MediaStoreRepository(application)
    val syncManager = LibrarySyncManager(mediaStoreRepository, roomRepository)
    val historyManager = HistoryManager(roomRepository)
    val playlistManager = PlaylistManager(roomRepository)
    val artworkAnalyzer = ArtworkAnalyzer(application)

    val playbackManager = PlaybackManager(application, roomRepository, historyManager, artworkAnalyzer)

    private val _sortOption = MutableStateFlow(SortOption.A_Z)
    private val _isLoading = MutableStateFlow(false)
    private val _selectedTab = MutableStateFlow(0)
    private val _isSphereEffectEnabled = MutableStateFlow(true)

    val uiState: StateFlow<HomeUiState> = combine(
        roomRepository.allSongs,
        roomRepository.favoriteSongs,
        roomRepository.recentSongs,
        roomRepository.allPlaylists,
        _sortOption,
        _isLoading,
        _selectedTab,
        _isSphereEffectEnabled
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val allSongsEntities = array[0] as List<SongEntity>
        @Suppress("UNCHECKED_CAST")
        val favEntities = array[1] as List<SongEntity>
        @Suppress("UNCHECKED_CAST")
        val recentEntities = array[2] as List<SongEntity>
        @Suppress("UNCHECKED_CAST")
        val playlistsEntities = array[3] as List<PlaylistEntity>
        val sort = array[4] as SortOption
        val loading = array[5] as Boolean
        val tab = array[6] as Int
        val sphereEffect = array[7] as Boolean

        val allSongs = allSongsEntities.map { it.toDomain() }
        val favSongs = favEntities.map { it.toDomain() }
        val recentSongs = recentEntities.map { it.toDomain() }

        val sortedSongs = applySort(allSongs, sort)
        val artists = groupArtists(allSongs)
        val playlists = playlistsEntities.map { Playlist(it.id, it.name) }

        HomeUiState(
            songs = sortedSongs,
            favoriteSongs = favSongs,
            recentSongs = recentSongs,
            playlists = playlists,
            artists = artists,
            isLoading = loading,
            sortOption = sort,
            selectedTab = tab,
            isSphereEffectEnabled = sphereEffect
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    init {
        syncLibrary()
    }

    fun syncLibrary() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                syncManager.syncLibrary()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun toggleSphereEffect() {
        _isSphereEffectEnabled.value = !_isSphereEffectEnabled.value
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            roomRepository.toggleFavorite(song.id, !song.isFavorite)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistManager.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistManager.deletePlaylist(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            playlistManager.addSongToPlaylist(playlistId, songId)
        }
    }

    fun deleteSongFromDevice(songId: Long) {
        viewModelScope.launch {
            val song = roomRepository.getSongById(songId)
            if (song != null) {
                mediaStoreRepository.deleteAudioFilePhysical(song.uri)
                roomRepository.deleteSong(songId)
            }
        }
    }

    private fun applySort(songs: List<Song>, sort: SortOption): List<Song> {
        return when (sort) {
            SortOption.A_Z -> songs.sortedBy { it.title.lowercase() }
            SortOption.Z_A -> songs.sortedByDescending { it.title.lowercase() }
            SortOption.MOST_PLAYED -> songs.sortedByDescending { it.playCount }
            SortOption.LEAST_PLAYED -> songs.sortedBy { it.playCount }
        }
    }

    private fun groupArtists(songs: List<Song>): List<Artist> {
        val grouped = songs.groupBy { it.artist.normalizeArtistName() }
        return grouped.map { (normalized, songGroup) ->
            val displayName = songGroup.firstOrNull()?.artist ?: normalized
            Artist(
                name = displayName,
                normalizedName = normalized,
                songCount = songGroup.size,
                songs = songGroup
            )
        }.sortedBy { it.name.lowercase() }
    }
}

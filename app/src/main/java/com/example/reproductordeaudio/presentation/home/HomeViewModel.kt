package com.example.reproductordeaudio.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.reproductordeaudio.data.local.db.AppDatabase
import com.example.reproductordeaudio.data.local.db.CardIdentityEntity
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Normalizer

enum class SortOption {
    A_Z, Z_A, MOST_PLAYED, LEAST_PLAYED
}

data class HomeUiState(
    val songs: List<Song> = emptyList(),
    val favoriteSongs: List<Song> = emptyList(),
    val recentSongs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val sortOption: SortOption = SortOption.A_Z,
    val isLoading: Boolean = false,
    val selectedTab: Int = 0,
    val isSphereEffectEnabled: Boolean = true,
    val searchQuery: String = ""
)

private data class DatabaseData(
    val all: List<SongEntity>,
    val favs: List<SongEntity>,
    val recents: List<SongEntity>,
    val playlists: List<PlaylistEntity>
)

private data class UiControls(
    val sort: SortOption,
    val loading: Boolean,
    val tab: Int,
    val sphere: Boolean,
    val debouncedQuery: String
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val roomRepository = RoomRepository(db)
    val mediaStoreRepository = MediaStoreRepository(application)
    val artworkAnalyzer = ArtworkAnalyzer(application)
    val syncManager = LibrarySyncManager(mediaStoreRepository, roomRepository, db.cardIdentityDao(), artworkAnalyzer)
    val historyManager = HistoryManager(roomRepository)
    val playlistManager = PlaylistManager(roomRepository)

    val playbackManager = PlaybackManager(application, roomRepository, historyManager, artworkAnalyzer)

    val cardIdentities: StateFlow<Map<Long, CardIdentityEntity>> = roomRepository.allCardIdentities
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private val _sortOption = MutableStateFlow(SortOption.A_Z)
    private val _isLoading = MutableStateFlow(false)
    private val _selectedTab = MutableStateFlow(0)
    private val _isSphereEffectEnabled = MutableStateFlow(true)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(FlowPreview::class)
    private val _debouncedSearchQuery = _searchQuery
        .debounce(200L)
        .distinctUntilChanged()

    private val _databaseData = combine(
        roomRepository.allSongs,
        roomRepository.favoriteSongs,
        roomRepository.recentSongs,
        roomRepository.allPlaylists
    ) { all, favs, recents, playlists ->
        DatabaseData(all, favs, recents, playlists)
    }

    private val _uiControls = combine(
        _sortOption,
        _isLoading,
        _selectedTab,
        _isSphereEffectEnabled,
        _debouncedSearchQuery
    ) { sort, loading, tab, sphere, debouncedQuery ->
        UiControls(sort, loading, tab, sphere, debouncedQuery)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _databaseData,
        _uiControls,
        _searchQuery
    ) { dbData, controls, immediateQuery ->
        val sortedSongs = filterAndSortSongs(dbData.all.map { it.toDomain() }, controls.debouncedQuery, controls.sort)
        val sortedFavs = filterAndSortSongs(dbData.favs.map { it.toDomain() }, controls.debouncedQuery, controls.sort)
        val sortedRecents = filterAndSortSongs(dbData.recents.map { it.toDomain() }, controls.debouncedQuery, controls.sort)
        val artists = groupArtists(sortedSongs)

        HomeUiState(
            songs = sortedSongs,
            favoriteSongs = sortedFavs,
            recentSongs = sortedRecents,
            playlists = dbData.playlists.map { it.toDomain() },
            artists = artists,
            sortOption = controls.sort,
            isLoading = controls.loading,
            selectedTab = controls.tab,
            isSphereEffectEnabled = controls.sphere,
            searchQuery = immediateQuery
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

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getCardIdentity(mediaStoreId: Long): Flow<CardIdentityEntity?> {
        return roomRepository.getCardIdentity(mediaStoreId)
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

    private fun filterAndSortSongs(songs: List<Song>, query: String, sort: SortOption): List<Song> {
        val sorted = applySort(songs, sort)
        if (query.isBlank()) return sorted

        val normalizedQuery = query.stripAccents()

        val prefixMatches = mutableListOf<Song>()
        val containsMatches = mutableListOf<Song>()

        for (song in sorted) {
            val normTitle = song.title.stripAccents()
            val normArtist = song.artist.stripAccents()

            if (normTitle.startsWith(normalizedQuery)) {
                prefixMatches.add(song)
            } else if (normTitle.contains(normalizedQuery) || normArtist.contains(normalizedQuery)) {
                containsMatches.add(song)
            }
        }

        return prefixMatches + containsMatches
    }

    private fun String.stripAccents(): String {
        val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
        return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").lowercase()
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
        return grouped.map { (_, artistSongs) ->
            val firstSong = artistSongs.first()
            Artist(
                name = firstSong.artist,
                normalizedName = firstSong.artist.normalizeArtistName(),
                songCount = artistSongs.size,
                songs = artistSongs
            )
        }.sortedBy { it.name.lowercase() }
    }
}

package com.example.reproductordeaudio.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.reproductordeaudio.domain.model.Artist
import com.example.reproductordeaudio.domain.model.Playlist
import com.example.reproductordeaudio.domain.model.Song
import com.example.reproductordeaudio.presentation.components.DefaultArtwork
import com.example.reproductordeaudio.presentation.components.SongCard
import com.example.reproductordeaudio.presentation.components.SongContextMenu
import com.example.reproductordeaudio.presentation.components.TopBar
import kotlin.math.abs

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenPlayer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackManager.playbackState.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var selectedSongForMenu by remember { mutableStateOf<Song?>(null) }
    var selectedArtistDetail by remember { mutableStateOf<Artist?>(null) }

    val tabs = listOf("Canciones", "Favoritos", "Recientes", "Playlists", "Artistas")

    Scaffold(
        topBar = {
            Column {
                TopBar(
                    title = "Biblioteca",
                    onSyncClick = { viewModel.syncLibrary() },
                    onSortClick = if (uiState.selectedTab == 0) { { showSortMenu = true } } else null,
                    onShuffleClick = if (uiState.selectedTab == 1 && uiState.favoriteSongs.isNotEmpty()) {
                        { viewModel.playbackManager.setQueue(uiState.favoriteSongs.shuffled()) }
                    } else null,
                    onSphereToggleClick = { viewModel.toggleSphereEffect() },
                    isSphereEffectEnabled = uiState.isSphereEffectEnabled
                )

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("A → Z") },
                        onClick = {
                            viewModel.setSortOption(SortOption.A_Z)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Z → A") },
                        onClick = {
                            viewModel.setSortOption(SortOption.Z_A)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Más reproducidas") },
                        onClick = {
                            viewModel.setSortOption(SortOption.MOST_PLAYED)
                            showSortMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Menos reproducidas") },
                        onClick = {
                            viewModel.setSortOption(SortOption.LEAST_PLAYED)
                            showSortMenu = false
                        }
                    )
                }

                ScrollableTabRow(selectedTabIndex = uiState.selectedTab, edgePadding = 16.dp) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = uiState.selectedTab == index,
                            onClick = {
                                selectedArtistDetail = null
                                viewModel.setSelectedTab(index)
                            },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (uiState.selectedTab == 3) {
                FloatingActionButton(onClick = { showCreatePlaylistDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Crear Playlist")
                }
            }
        },
        bottomBar = {
            playbackState.currentSong?.let { song ->
                MiniPlayer(
                    song = song,
                    isPlaying = playbackState.isPlaying,
                    onPlayPause = { viewModel.playbackManager.playOrPause() },
                    onClick = onOpenPlayer
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading && uiState.songs.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                when (uiState.selectedTab) {
                    0 -> SongListTab(
                        songs = uiState.songs,
                        isSphereEffectEnabled = uiState.isSphereEffectEnabled,
                        onSongClick = { song, index ->
                            viewModel.playbackManager.setQueue(uiState.songs, index)
                        },
                        onMenuClick = { selectedSongForMenu = it }
                    )
                    1 -> SongListTab(
                        songs = uiState.favoriteSongs,
                        isSphereEffectEnabled = uiState.isSphereEffectEnabled,
                        emptyMessage = "No tienes canciones en favoritos",
                        onSongClick = { song, index ->
                            viewModel.playbackManager.setQueue(uiState.favoriteSongs, index)
                        },
                        onMenuClick = { selectedSongForMenu = it }
                    )
                    2 -> SongListTab(
                        songs = uiState.recentSongs,
                        isSphereEffectEnabled = uiState.isSphereEffectEnabled,
                        emptyMessage = "Sin reproducciones recientes",
                        onSongClick = { song, index ->
                            viewModel.playbackManager.setQueue(uiState.recentSongs, index)
                        },
                        onMenuClick = { selectedSongForMenu = it }
                    )
                    3 -> PlaylistTab(
                        playlists = uiState.playlists,
                        onDelete = { viewModel.deletePlaylist(it.id) }
                    )
                    4 -> {
                        if (selectedArtistDetail != null) {
                            ArtistDetailTab(
                                artist = selectedArtistDetail!!,
                                isSphereEffectEnabled = uiState.isSphereEffectEnabled,
                                onBack = { selectedArtistDetail = null },
                                onSongClick = { song, index ->
                                    viewModel.playbackManager.setQueue(selectedArtistDetail!!.songs, index)
                                },
                                onMenuClick = { selectedSongForMenu = it }
                            )
                        } else {
                            ArtistListTab(
                                artists = uiState.artists,
                                onArtistClick = { selectedArtistDetail = it }
                            )
                        }
                    }
                }
            }

            SongContextMenu(
                expanded = selectedSongForMenu != null,
                song = selectedSongForMenu,
                onDismissRequest = { selectedSongForMenu = null },
                onPlay = {
                    selectedSongForMenu?.let { song ->
                        viewModel.playbackManager.setQueue(listOf(song))
                    }
                },
                onToggleFavorite = {
                    selectedSongForMenu?.let { viewModel.toggleFavorite(it) }
                },
                onAddToPlaylist = {
                    selectedSongForMenu?.let { song ->
                        uiState.playlists.firstOrNull()?.let { pl ->
                            viewModel.addSongToPlaylist(pl.id, song.id)
                        }
                    }
                },
                onDelete = {
                    selectedSongForMenu?.let { viewModel.deleteSongFromDevice(it.id) }
                }
            )

            if (showCreatePlaylistDialog) {
                CreatePlaylistDialog(
                    onDismiss = { showCreatePlaylistDialog = false },
                    onCreate = { name ->
                        viewModel.createPlaylist(name)
                        showCreatePlaylistDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun SongListTab(
    songs: List<Song>,
    isSphereEffectEnabled: Boolean = true,
    emptyMessage: String = "No hay canciones disponibles",
    onSongClick: (Song, Int) -> Unit,
    onMenuClick: (Song) -> Unit
) {
    if (songs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = emptyMessage, style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        val listState = rememberLazyListState()
        val density = LocalDensity.current

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(songs.size, key = { songs[it].id }) { index ->
                val song = songs[index]

                val itemModifier = if (isSphereEffectEnabled) {
                    Modifier.graphicsLayer {
                        val layoutInfo = listState.layoutInfo
                        val visibleItem = layoutInfo.visibleItemsInfo.find { it.index == index }
                        if (visibleItem != null && layoutInfo.viewportEndOffset > 0) {
                            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                            val itemCenter = visibleItem.offset + visibleItem.size / 2f
                            val distanceFromCenter = abs(viewportCenter - itemCenter)
                            val maxDistance = layoutInfo.viewportEndOffset / 2f

                            val fraction = (distanceFromCenter / maxDistance).coerceIn(0f, 1f)

                            val scale = 1.0f - fraction * 0.20f
                            val alphaValue = 1.0f - fraction * 0.45f
                            val rotX = ((itemCenter - viewportCenter) / maxDistance) * 32f

                            scaleX = scale
                            scaleY = scale
                            alpha = alphaValue
                            rotationX = rotX
                            cameraDistance = 12f * density.density
                        }
                    }
                } else Modifier

                SongCard(
                    song = song,
                    onClick = { onSongClick(song, index) },
                    onMenuClick = { onMenuClick(song) },
                    modifier = itemModifier
                )
            }
        }
    }
}

@Composable
fun PlaylistTab(
    playlists: List<Playlist>,
    onDelete: (Playlist) -> Unit
) {
    if (playlists.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No has creado listas de reproducción", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(playlists, key = { it.id }) { playlist ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDelete(playlist) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar Playlist")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistListTab(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit
) {
    if (artists.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No se encontraron artistas", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(artists, key = { it.normalizedName }) { artist ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable { onArtistClick(artist) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = artist.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${artist.songCount} canciones",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistDetailTab(
    artist: Artist,
    isSphereEffectEnabled: Boolean = true,
    onBack: () -> Unit,
    onSongClick: (Song, Int) -> Unit,
    onMenuClick: (Song) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Text("← Volver a Artistas")
        }
        Text(
            text = artist.name,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        SongListTab(
            songs = artist.songs,
            isSphereEffectEnabled = isSphereEffectEnabled,
            onSongClick = onSongClick,
            onMenuClick = onMenuClick
        )
    }
}

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!song.artworkUri.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.artworkUri)
                        .memoryCacheKey(song.id.toString())
                        .diskCacheKey(song.id.toString())
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                DefaultArtwork(size = 48.dp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir"
                )
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre de la playlist") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) onCreate(name)
                }
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

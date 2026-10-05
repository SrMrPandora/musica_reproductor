package com.example.reproductordeaudio.presentation.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.reproductordeaudio.data.local.db.CardIdentityEntity
import com.example.reproductordeaudio.domain.model.Artist
import com.example.reproductordeaudio.domain.model.Playlist
import com.example.reproductordeaudio.domain.model.Song
import com.example.reproductordeaudio.presentation.components.DefaultArtwork
import com.example.reproductordeaudio.presentation.components.SongCard
import com.example.reproductordeaudio.presentation.components.SongContextMenu
import com.example.reproductordeaudio.presentation.components.TopBar
import com.example.reproductordeaudio.ui.theme.DynamicColorScheme
import com.example.reproductordeaudio.ui.theme.LocalDynamicColors
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.example.reproductordeaudio.ui.theme.ProvideDynamicColors
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSongClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackManager.playbackState.collectAsStateWithLifecycle()
    val cardIdentities by viewModel.cardIdentities.collectAsStateWithLifecycle()
    val shouldScrollToCurrentSong by viewModel.shouldScrollToCurrentSong.collectAsStateWithLifecycle()

    var selectedSongForMenu by remember { mutableStateOf<Song?>(null) }
    var selectedArtistDetail by remember { mutableStateOf<Artist?>(null) }
    var selectedPlaylistDetail by remember { mutableStateOf<Playlist?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var songForAddToPlaylist by remember { mutableStateOf<Song?>(null) }

    val pagerState = rememberPagerState(initialPage = uiState.selectedTab) { 5 }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.setSelectedTab(page)
        }
    }

    LaunchedEffect(uiState.selectedTab) {
        if (pagerState.currentPage != uiState.selectedTab) {
            pagerState.animateScrollToPage(uiState.selectedTab)
        }
    }

    val currentTitle = remember(pagerState.targetPage) {
        when (pagerState.targetPage) {
            0 -> "Canciones"
            1 -> "Favoritos"
            2 -> "Recientes"
            3 -> "Playlists"
            4 -> "Artistas"
            else -> "Biblioteca"
        }
    }

    ProvideDynamicColors(
        currentSong = playbackState.currentSong,
        cardIdentities = cardIdentities
    ) {
        val dynamicColors = LocalDynamicColors.current

        Scaffold(
            containerColor = dynamicColors.background,
            topBar = {
                Box {
                    TopBar(
                        title = currentTitle,
                        onSyncClick = { viewModel.syncLibrary() },
                        onSortClick = { showSortMenu = true },
                        onShuffleClick = {
                            val randomSong = viewModel.getRandomWeightedSong()
                            if (randomSong != null) {
                                val songs = uiState.songs
                                val index = songs.indexOfFirst { it.id == randomSong.id }
                                viewModel.playbackManager.setQueue(songs, if (index != -1) index else 0)
                                onSongClick()
                            }
                        },
                        onSphereToggleClick = { viewModel.toggleSphereEffect() },
                        isSphereEffectEnabled = uiState.isSphereEffectEnabled,
                        isSyncing = uiState.isLoading
                    )

                    HomeSortDropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        onSortOptionSelected = { option ->
                            viewModel.setSortOption(option)
                            showSortMenu = false
                        },
                        containerColor = dynamicColors.surface,
                        textColor = dynamicColors.textColor
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = playbackState.currentSong != null,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    playbackState.currentSong?.let { song ->
                        MiniPlayer(
                            song = song,
                            isPlaying = playbackState.isPlaying,
                            cardIdentity = cardIdentities[song.id],
                            onPlayPause = { viewModel.playbackManager.playOrPause() },
                            onClick = onSongClick
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                HomeSearchBar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    dynamicColors = dynamicColors
                )

                HomeTabRow(
                    currentPage = pagerState.currentPage,
                    onTabSelected = { index ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    dynamicColors = dynamicColors
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (uiState.isLoading && uiState.songs.isEmpty()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = dynamicColors.accent)
                    } else {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (page) {
                                in 0..2 -> {
                                    val (pageSongs, emptyMsg) = when (page) {
                                        1 -> uiState.favoriteSongs to "No tienes canciones en favoritos"
                                        2 -> uiState.recentSongs to "Sin reproducciones recientes"
                                        else -> uiState.songs to "No hay canciones disponibles"
                                    }
                                    SongListTab(
                                        songs = pageSongs,
                                        isSphereEffectEnabled = uiState.isSphereEffectEnabled,
                                        emptyMessage = emptyMsg,
                                        searchQuery = uiState.searchQuery,
                                        cardIdentities = cardIdentities,
                                        currentSongId = playbackState.currentSong?.id,
                                        shouldScrollToCurrentSong = shouldScrollToCurrentSong,
                                        onScrollHandled = { viewModel.onScrollToCurrentSongHandled() },
                                        onClearSearch = { viewModel.updateSearchQuery("") },
                                        onSongClick = { song, index ->
                                            viewModel.playbackManager.setQueue(pageSongs, index)
                                        },
                                        onMenuClick = { selectedSongForMenu = it }
                                    )
                                }
                                3 -> {
                                    if (selectedPlaylistDetail != null) {
                                        val playlistSongs by viewModel.getSongsForPlaylist(selectedPlaylistDetail!!.id)
                                            .collectAsStateWithLifecycle(initialValue = emptyList())
                                        PlaylistDetailTab(
                                            playlist = selectedPlaylistDetail!!,
                                            songs = playlistSongs,
                                            isSphereEffectEnabled = uiState.isSphereEffectEnabled,
                                            searchQuery = uiState.searchQuery,
                                            cardIdentities = cardIdentities,
                                            onClearSearch = { viewModel.updateSearchQuery("") },
                                            onBack = { selectedPlaylistDetail = null },
                                            onSongClick = { song, index ->
                                                viewModel.playbackManager.setQueue(playlistSongs, index)
                                            },
                                            onMenuClick = { selectedSongForMenu = it }
                                        )
                                    } else {
                                        PlaylistTab(
                                            playlists = uiState.playlists,
                                            onCreatePlaylist = { viewModel.createPlaylist(it) },
                                            onPlaylistClick = { selectedPlaylistDetail = it },
                                            onDelete = { viewModel.deletePlaylist(it.id) }
                                        )
                                    }
                                }
                                4 -> {
                                    if (selectedArtistDetail != null) {
                                        ArtistDetailTab(
                                            artist = selectedArtistDetail!!,
                                            isSphereEffectEnabled = uiState.isSphereEffectEnabled,
                                            searchQuery = uiState.searchQuery,
                                            cardIdentities = cardIdentities,
                                            onClearSearch = { viewModel.updateSearchQuery("") },
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
                            songForAddToPlaylist = selectedSongForMenu
                        },
                        onDelete = {
                            selectedSongForMenu?.let { song ->
                                viewModel.deleteSongFromDevice(song.id)
                            }
                        }
                    )

                    if (songForAddToPlaylist != null) {
                        val context = LocalContext.current
                        AddToPlaylistDialog(
                            song = songForAddToPlaylist!!,
                            playlists = uiState.playlists,
                            onDismiss = { songForAddToPlaylist = null },
                            onSelectPlaylist = { playlistId, playlistName ->
                                viewModel.addSongToPlaylist(playlistId, songForAddToPlaylist!!.id)
                                Toast.makeText(context, "Agregada a $playlistName", Toast.LENGTH_SHORT).show()
                                songForAddToPlaylist = null
                            },
                            onCreateNewPlaylist = { name ->
                                viewModel.createPlaylist(name)
                                Toast.makeText(context, "Playlist \"$name\" creada", Toast.LENGTH_SHORT).show()
                                songForAddToPlaylist = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSortDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    containerColor: Color,
    textColor: Color
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        containerColor = containerColor
    ) {
        DropdownMenuItem(
            text = { Text("A - Z", color = textColor) },
            onClick = { onSortOptionSelected(SortOption.A_Z) }
        )
        DropdownMenuItem(
            text = { Text("Z - A", color = textColor) },
            onClick = { onSortOptionSelected(SortOption.Z_A) }
        )
        DropdownMenuItem(
            text = { Text("Más escuchadas", color = textColor) },
            onClick = { onSortOptionSelected(SortOption.MOST_PLAYED) }
        )
        DropdownMenuItem(
            text = { Text("Menos escuchadas", color = textColor) },
            onClick = { onSortOptionSelected(SortOption.LEAST_PLAYED) }
        )
    }
}

@Composable
private fun HomeSearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    dynamicColors: DynamicColorScheme = LocalDynamicColors.current
) {
    TextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        placeholder = { Text("Buscar canción o artista...", color = dynamicColors.textColor.copy(alpha = 0.6f)) },
        leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = dynamicColors.iconTint)
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = searchQuery.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Limpiar búsqueda",
                        tint = dynamicColors.iconTint
                    )
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = dynamicColors.surface.copy(alpha = 0.5f),
            unfocusedContainerColor = dynamicColors.surface.copy(alpha = 0.3f),
            disabledContainerColor = dynamicColors.surface.copy(alpha = 0.2f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = dynamicColors.textColor,
            unfocusedTextColor = dynamicColors.textColor
        )
    )
}

@Composable
private fun HomeTabRow(
    currentPage: Int,
    onTabSelected: (Int) -> Unit,
    dynamicColors: DynamicColorScheme = LocalDynamicColors.current
) {
    val tabTitles = listOf("Canciones", "Favoritos", "Recientes", "Playlists", "Artistas")

    ScrollableTabRow(
        selectedTabIndex = currentPage,
        containerColor = Color.Transparent,
        contentColor = dynamicColors.textColor,
        edgePadding = 16.dp,
        indicator = { tabPositions ->
            if (currentPage < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[currentPage]),
                    color = dynamicColors.accent
                )
            }
        },
        divider = {}
    ) {
        tabTitles.forEachIndexed { index, title ->
            Tab(
                selected = currentPage == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        color = if (currentPage == index) dynamicColors.textColor else dynamicColors.textColor.copy(alpha = 0.6f)
                    )
                }
            )
        }
    }
}

@Composable
fun SongListTab(
    songs: List<Song>,
    isSphereEffectEnabled: Boolean = true,
    emptyMessage: String = "No hay canciones disponibles",
    searchQuery: String = "",
    cardIdentities: Map<Long, CardIdentityEntity> = emptyMap(),
    currentSongId: Long? = null,
    shouldScrollToCurrentSong: Boolean = false,
    onScrollHandled: () -> Unit = {},
    onClearSearch: (() -> Unit)? = null,
    onSongClick: (Song, Int) -> Unit,
    onMenuClick: (Song) -> Unit
) {
    val dynamicColors = LocalDynamicColors.current

    if (songs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (searchQuery.isNotBlank()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = dynamicColors.iconTint.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No se encontraron resultados para \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = dynamicColors.textColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    if (onClearSearch != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = onClearSearch) {
                            Text("Limpiar búsqueda", color = dynamicColors.accent)
                        }
                    }
                }
            } else {
                Text(text = emptyMessage, style = MaterialTheme.typography.bodyLarge, color = dynamicColors.textColor.copy(alpha = 0.7f))
            }
        }
    } else {
        val listState = rememberLazyListState()
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(shouldScrollToCurrentSong, currentSongId) {
            if (shouldScrollToCurrentSong && currentSongId != null) {
                val targetIndex = songs.indexOfFirst { it.id == currentSongId }
                if (targetIndex != -1) {
                    val currentIndex = listState.firstVisibleItemIndex
                    if (abs(targetIndex - currentIndex) > 8) {
                        val intermediateIndex = if (targetIndex > currentIndex) targetIndex - 2 else targetIndex + 2
                        listState.scrollToItem(intermediateIndex)
                    }
                    val currentLayoutInfo = listState.layoutInfo
                    val viewportHeight = currentLayoutInfo.viewportEndOffset - currentLayoutInfo.viewportStartOffset
                    val itemHeight = currentLayoutInfo.visibleItemsInfo.find { it.index == targetIndex }?.size
                        ?: currentLayoutInfo.visibleItemsInfo.firstOrNull()?.size
                        ?: 0
                    val centerOffset = (viewportHeight - itemHeight) / 2
                    listState.animateScrollToItem(targetIndex, scrollOffset = -centerOffset)
                }
                onScrollHandled()
            }
        }

        val centeredIndex by remember {
            derivedStateOf {
                val layoutInfo = listState.layoutInfo
                if (listState.isScrollInProgress || layoutInfo.viewportEndOffset <= 0 || layoutInfo.visibleItemsInfo.isEmpty()) {
                    -1
                } else {
                    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                    layoutInfo.visibleItemsInfo.minByOrNull { item ->
                        val itemCenter = item.offset + item.size / 2f
                        abs(viewportCenter - itemCenter)
                    }?.index ?: -1
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            if (searchQuery.isNotBlank()) {
                Text(
                    text = "${songs.size} ${if (songs.size == 1) "resultado" else "resultados"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = dynamicColors.accent,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    count = songs.size,
                    key = { songs[it].id },
                    contentType = { "song_card" }
                ) { index ->
                    val song = songs[index]
                    val isCentered = (index == centeredIndex)

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

                                val baseScale = if (isCentered) 1.10f else (1.0f - fraction * 0.35f)
                                val alphaValue = if (isCentered) 1.0f else (1.0f - fraction * 0.60f).coerceAtLeast(0.3f)
                                val rotX = ((itemCenter - viewportCenter) / maxDistance) * 32f

                                scaleX = baseScale
                                scaleY = baseScale
                                alpha = alphaValue
                                rotationX = rotX
                                cameraDistance = 12f * density.density
                                shadowElevation = if (isCentered) 20.dp.toPx() else 2.dp.toPx()
                            }
                        }
                    } else Modifier

                    SongCard(
                        song = song,
                        onClick = {
                            coroutineScope.launch {
                                val currentLayoutInfo = listState.layoutInfo
                                val viewportHeight = currentLayoutInfo.viewportEndOffset - currentLayoutInfo.viewportStartOffset
                                val currentVisibleItem = currentLayoutInfo.visibleItemsInfo.find { it.index == index }
                                val itemHeight = currentVisibleItem?.size ?: 0
                                val centerOffset = (viewportHeight - itemHeight) / 2
                                listState.animateScrollToItem(index, scrollOffset = -centerOffset)
                                onSongClick(song, index)
                            }
                        },
                        onMenuClick = { onMenuClick(song) },
                        modifier = itemModifier,
                        cardIdentity = cardIdentities[song.id],
                        isCentered = isCentered
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistTab(
    playlists: List<Playlist>,
    onCreatePlaylist: (String) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onDelete: (Playlist) -> Unit
) {
    val dynamicColors = LocalDynamicColors.current
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (playlists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No has creado listas de reproducción", style = MaterialTheme.typography.bodyLarge, color = dynamicColors.textColor.copy(alpha = 0.7f))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(playlists, key = { it.id }) { playlist ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .clickable { onPlaylistClick(playlist) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = dynamicColors.surface.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, modifier = Modifier.size(32.dp), tint = dynamicColors.iconTint)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                                color = dynamicColors.textColor
                            )
                            IconButton(onClick = { onDelete(playlist) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar Playlist", tint = dynamicColors.iconTint)
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = dynamicColors.accent,
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Crear Playlist")
        }

        if (showCreateDialog) {
            CreatePlaylistDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name ->
                    onCreatePlaylist(name)
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
fun PlaylistDetailTab(
    playlist: Playlist,
    songs: List<Song>,
    isSphereEffectEnabled: Boolean = true,
    searchQuery: String = "",
    cardIdentities: Map<Long, CardIdentityEntity> = emptyMap(),
    onClearSearch: (() -> Unit)? = null,
    onBack: () -> Unit,
    onSongClick: (Song, Int) -> Unit,
    onMenuClick: (Song) -> Unit
) {
    val dynamicColors = LocalDynamicColors.current
    Column(modifier = Modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Text("← Volver a Playlists", color = dynamicColors.accent)
        }
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.headlineMedium,
            color = dynamicColors.textColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        SongListTab(
            songs = songs,
            isSphereEffectEnabled = isSphereEffectEnabled,
            emptyMessage = "Esta lista de reproducción no tiene canciones",
            searchQuery = searchQuery,
            cardIdentities = cardIdentities,
            onClearSearch = onClearSearch,
            onSongClick = onSongClick,
            onMenuClick = onMenuClick
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val dynamicColors = LocalDynamicColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dynamicColors.surface,
        title = {
            Text("Nueva Playlist", color = dynamicColors.textColor, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre de la playlist", color = dynamicColors.textColor.copy(alpha = 0.7f)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = dynamicColors.accent,
                    unfocusedBorderColor = dynamicColors.textColor.copy(alpha = 0.3f),
                    focusedTextColor = dynamicColors.textColor,
                    unfocusedTextColor = dynamicColors.textColor
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) onCreate(name.trim())
                },
                enabled = name.isNotBlank()
            ) {
                Text("Crear", color = if (name.isNotBlank()) dynamicColors.accent else dynamicColors.textColor.copy(alpha = 0.3f))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = dynamicColors.textColor.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun AddToPlaylistDialog(
    song: Song,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (Long, String) -> Unit,
    onCreateNewPlaylist: (String) -> Unit
) {
    val dynamicColors = LocalDynamicColors.current
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                onCreateNewPlaylist(name)
                showCreateDialog = false
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = dynamicColors.surface,
            title = {
                Column {
                    Text("Agregar a playlist", color = dynamicColors.textColor, style = MaterialTheme.typography.titleLarge)
                    Text(
                        song.title,
                        color = dynamicColors.textColor.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            },
            text = {
                if (playlists.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "No tienes listas de reproducción.",
                            color = dynamicColors.textColor.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = dynamicColors.accent)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Crear nueva playlist", color = dynamicColors.accent)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            Card(
                                onClick = { onSelectPlaylist(playlist.id, playlist.name) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = dynamicColors.background.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.PlaylistPlay,
                                        contentDescription = null,
                                        tint = dynamicColors.iconTint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        playlist.name,
                                        color = dynamicColors.textColor,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        item {
                            TextButton(
                                onClick = { showCreateDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = dynamicColors.accent)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Crear nueva playlist", color = dynamicColors.accent)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = dynamicColors.textColor.copy(alpha = 0.7f))
                }
            }
        )
    }
}

@Composable
fun ArtistListTab(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit
) {
    val dynamicColors = LocalDynamicColors.current
    if (artists.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No se encontraron artistas", style = MaterialTheme.typography.bodyLarge, color = dynamicColors.textColor.copy(alpha = 0.7f))
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(artists, key = { it.normalizedName }) { artist ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable { onArtistClick(artist) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = dynamicColors.surface.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(32.dp), tint = dynamicColors.iconTint)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = artist.name, style = MaterialTheme.typography.titleMedium, color = dynamicColors.textColor)
                            Text(
                                text = "${artist.songCount} canciones",
                                style = MaterialTheme.typography.bodySmall,
                                color = dynamicColors.textColor.copy(alpha = 0.7f)
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
    searchQuery: String = "",
    cardIdentities: Map<Long, CardIdentityEntity> = emptyMap(),
    onClearSearch: (() -> Unit)? = null,
    onBack: () -> Unit,
    onSongClick: (Song, Int) -> Unit,
    onMenuClick: (Song) -> Unit
) {
    val dynamicColors = LocalDynamicColors.current
    Column(modifier = Modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
            Text("← Volver a Artistas", color = dynamicColors.accent)
        }
        Text(
            text = artist.name,
            style = MaterialTheme.typography.headlineMedium,
            color = dynamicColors.textColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        SongListTab(
            songs = artist.songs,
            isSphereEffectEnabled = isSphereEffectEnabled,
            searchQuery = searchQuery,
            cardIdentities = cardIdentities,
            onClearSearch = onClearSearch,
            onSongClick = onSongClick,
            onMenuClick = onMenuClick
        )
    }
}

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    cardIdentity: CardIdentityEntity? = null,
    onPlayPause: () -> Unit,
    onClick: () -> Unit
) {
    val gradientColors = remember(cardIdentity?.colorsJson) {
        cardIdentity?.colorsJson?.split(",")?.mapNotNull { str ->
            str.trim().toIntOrNull()?.let { Color(it) }
        }
    }

    val textColor = cardIdentity?.textColorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.onPrimaryContainer
    val subTextColor = cardIdentity?.textColorArgb?.let { Color(it).copy(alpha = 0.7f) } ?: MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)

    val backgroundModifier = if (gradientColors != null && gradientColors.isNotEmpty()) {
        Modifier.background(Brush.linearGradient(colors = gradientColors))
    } else {
        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(backgroundModifier)
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
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
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
                    overflow = TextOverflow.Ellipsis,
                    color = textColor
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = subTextColor
                )
            }

            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = textColor
                )
            }
        }
    }
}

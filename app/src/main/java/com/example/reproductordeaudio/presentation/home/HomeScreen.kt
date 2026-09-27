package com.example.reproductordeaudio.presentation.home

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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.reproductordeaudio.ui.theme.LocalDynamicColors
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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

    var selectedSongForMenu by remember { mutableStateOf<Song?>(null) }
    var selectedArtistDetail by remember { mutableStateOf<Artist?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

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
                        onSphereToggleClick = { viewModel.toggleSphereEffect() },
                        isSphereEffectEnabled = uiState.isSphereEffectEnabled
                    )

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        containerColor = dynamicColors.surface
                    ) {
                        DropdownMenuItem(
                            text = { Text("A - Z", color = dynamicColors.textColor) },
                            onClick = {
                                viewModel.setSortOption(SortOption.A_Z)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Z - A", color = dynamicColors.textColor) },
                            onClick = {
                                viewModel.setSortOption(SortOption.Z_A)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Más escuchadas", color = dynamicColors.textColor) },
                            onClick = {
                                viewModel.setSortOption(SortOption.MOST_PLAYED)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Menos escuchadas", color = dynamicColors.textColor) },
                            onClick = {
                                viewModel.setSortOption(SortOption.LEAST_PLAYED)
                                showSortMenu = false
                            }
                        )
                    }
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
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    placeholder = { Text("Buscar canción o artista...", color = dynamicColors.textColor.copy(alpha = 0.6f)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = dynamicColors.iconTint)
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = uiState.searchQuery.isNotEmpty(),
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            IconButton(
                                onClick = { viewModel.updateSearchQuery("") },
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

                val tabTitles = listOf("Canciones", "Favoritos", "Recientes", "Playlists", "Artistas")

                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.Transparent,
                    contentColor = dynamicColors.textColor,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                color = dynamicColors.accent
                            )
                        }
                    },
                    divider = {}
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = title,
                                    color = if (pagerState.currentPage == index) dynamicColors.textColor else dynamicColors.textColor.copy(alpha = 0.6f)
                                )
                            }
                        )
                    }
                }

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
                                0 -> SongListTab(
                                    songs = uiState.songs,
                                    isSphereEffectEnabled = uiState.isSphereEffectEnabled,
                                    searchQuery = uiState.searchQuery,
                                    cardIdentities = cardIdentities,
                                    onClearSearch = { viewModel.updateSearchQuery("") },
                                    onSongClick = { song, index ->
                                        viewModel.playbackManager.setQueue(uiState.songs, index)
                                    },
                                    onMenuClick = { selectedSongForMenu = it }
                                )
                                1 -> SongListTab(
                                    songs = uiState.favoriteSongs,
                                    isSphereEffectEnabled = uiState.isSphereEffectEnabled,
                                    emptyMessage = "No tienes canciones en favoritos",
                                    searchQuery = uiState.searchQuery,
                                    cardIdentities = cardIdentities,
                                    onClearSearch = { viewModel.updateSearchQuery("") },
                                    onSongClick = { song, index ->
                                        viewModel.playbackManager.setQueue(uiState.favoriteSongs, index)
                                    },
                                    onMenuClick = { selectedSongForMenu = it }
                                )
                                2 -> SongListTab(
                                    songs = uiState.recentSongs,
                                    isSphereEffectEnabled = uiState.isSphereEffectEnabled,
                                    emptyMessage = "Sin reproducciones recientes",
                                    searchQuery = uiState.searchQuery,
                                    cardIdentities = cardIdentities,
                                    onClearSearch = { viewModel.updateSearchQuery("") },
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
                            selectedSongForMenu?.let { song ->
                                uiState.playlists.firstOrNull()?.let { pl ->
                                    viewModel.addSongToPlaylist(pl.id, song.id)
                                }
                            }
                        },
                        onDelete = {
                            selectedSongForMenu?.let { song ->
                                viewModel.deleteSongFromDevice(song.id)
                            }
                        }
                    )
                }
            }
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

        val layoutInfo = listState.layoutInfo
        val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f

        var previousScrollOffset by remember { mutableStateOf(0) }
        var isFastScrolling by remember { mutableStateOf(false) }

        val currentScrollOffset = listState.firstVisibleItemIndex * 1000 + listState.firstVisibleItemScrollOffset
        val scrollDelta = abs(currentScrollOffset - previousScrollOffset)
        previousScrollOffset = currentScrollOffset

        isFastScrolling = listState.isScrollInProgress && scrollDelta > 120

        val closestIndex = if (!isFastScrolling && layoutInfo.viewportEndOffset > 0 && layoutInfo.visibleItemsInfo.isNotEmpty()) {
            layoutInfo.visibleItemsInfo.minWithOrNull(
                compareBy<LazyListItemInfo> { item ->
                    val itemCenter = item.offset + item.size / 2f
                    abs(viewportCenter - itemCenter)
                }.thenBy { item -> item.index }
            )?.index ?: -1
        } else -1

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
                    val isCentered = (index == closestIndex)

                    val itemModifier = if (isSphereEffectEnabled) {
                        Modifier.graphicsLayer {
                            val visibleItem = layoutInfo.visibleItemsInfo.find { it.index == index }
                            if (visibleItem != null && layoutInfo.viewportEndOffset > 0) {
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
    onDelete: (Playlist) -> Unit
) {
    val dynamicColors = LocalDynamicColors.current
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
                        .padding(horizontal = 12.dp, vertical = 6.dp),
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

package com.example.reproductordeaudio.presentation.player

import android.os.Trace
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.reproductordeaudio.domain.model.RepeatMode
import com.example.reproductordeaudio.presentation.home.HomeViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SONG_TRANSITION_DURATION = 600

@Composable
fun PlayerScreen(
    viewModel: HomeViewModel,
    onBackClick: () -> Unit
) {
    BackHandler {
        viewModel.triggerScrollToCurrentSong()
        onBackClick()
    }

    val playbackState by viewModel.playbackManager.playbackState.collectAsState()
    val palette by viewModel.playbackManager.currentPalette.collectAsState()
    val amplitudes by viewModel.playbackManager.visualizerAmplitudes.collectAsState()

    val song = playbackState.currentSong ?: return

    var showVolumeHud by remember { mutableStateOf(false) }
    var volumeHudJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val hazeState = remember { HazeState() }

    val heartColor by animateColorAsState(
        targetValue = palette?.heartColor ?: Color(0xFFFF4081),
        animationSpec = tween(SONG_TRANSITION_DURATION),
        label = "heart_color"
    )

    val backgroundColor by animateColorAsState(
        targetValue = palette?.backgroundColor ?: Color(0xFF121212),
        animationSpec = tween(SONG_TRANSITION_DURATION),
        label = "background_color"
    )

    val visualizerColor by animateColorAsState(
        targetValue = palette?.secondaryColor ?: Color(0xFF03DAC6),
        animationSpec = tween(SONG_TRANSITION_DURATION),
        label = "visualizer_color"
    )

    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Fondo con Haze Effect en tiempo real sobre la carátula original
        Trace.beginSection("blur_render")
        Crossfade(
            targetState = song.id,
            animationSpec = tween(SONG_TRANSITION_DURATION),
            label = "background_crossfade"
        ) { targetSongId ->
            val targetSong = playbackState.currentSong ?: song
            val prepared = viewModel.playbackManager.preloadManager.getPreparedArtwork(targetSongId)
            Box(modifier = Modifier.fillMaxSize()) {
                if (prepared?.bitmap != null) {
                    Image(
                        bitmap = prepared.bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeSource(hazeState)
                    )
                } else if (!targetSong.artworkUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(targetSong.artworkUri)
                            .memoryCacheKey(targetSongId.toString())
                            .diskCacheKey(targetSongId.toString())
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeSource(hazeState)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeEffect(
                            state = hazeState,
                            style = HazeStyle(
                                backgroundColor = backgroundColor,
                                blurRadius = 18.dp,
                                tint = null
                            )
                        )
                )
            }
        }
        Trace.endSection()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            backgroundColor.copy(alpha = 0.85f),
                            backgroundColor
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Barra Superior (Atrás, Contador, Corazón)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${song.playCount} veces",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                IconButton(onClick = { viewModel.toggleFavorite(song) }) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = heartColor
                    )
                }
            }

            // Área adaptativa responsiva para Disco y Visualizador Radial
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val availableWidth = maxWidth
                val availableHeight = maxHeight

                val containerSize = minOf(availableWidth * 0.92f, availableHeight * 0.45f, 380.dp)
                val discSize = containerSize * (280f / 380f)

                val containerSizePx = with(density) { containerSize.toPx() }
                val discSizePx = with(density) { discSize.toPx() }
                val innerRadiusPx = discSizePx / 2f
                val maxBarLengthPx = (containerSizePx - discSizePx) / 2f

                Box(
                    modifier = Modifier.size(containerSize),
                    contentAlignment = Alignment.Center
                ) {
                    Trace.beginSection("visualizer_compose")
                    RadialVisualizer(
                        amplitudesProvider = { amplitudes },
                        barColor = visualizerColor,
                        size = containerSize,
                        innerRadiusPx = innerRadiusPx,
                        maxBarLengthPx = maxBarLengthPx
                    )
                    Trace.endSection()

                    // Transición animada de 600 ms para cambio de disco
                    AnimatedContent(
                        targetState = song.id,
                        transitionSpec = {
                            val queueSize = viewModel.playbackManager.getQueueSize()
                            val initialIndex = viewModel.playbackManager.getSongIndex(initialState)
                            val targetIndex = viewModel.playbackManager.getSongIndex(targetState)

                            val isForward = when {
                                initialIndex == -1 || targetIndex == -1 -> true
                                queueSize > 1 && initialIndex == queueSize - 1 && targetIndex == 0 -> true
                                queueSize > 1 && initialIndex == 0 && targetIndex == queueSize - 1 -> false
                                else -> targetIndex >= initialIndex
                            }

                            val slideInFrom = if (isForward) { fullWidth: Int -> fullWidth } else { fullWidth: Int -> -fullWidth }
                            val slideOutTo = if (isForward) { fullWidth: Int -> -fullWidth } else { fullWidth: Int -> fullWidth }

                            (slideInHorizontally(
                                animationSpec = tween(SONG_TRANSITION_DURATION, easing = FastOutSlowInEasing),
                                initialOffsetX = slideInFrom
                            ) + fadeIn(animationSpec = tween(SONG_TRANSITION_DURATION)) + scaleIn(
                                initialScale = 0.8f,
                                animationSpec = tween(SONG_TRANSITION_DURATION)
                            )).togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(SONG_TRANSITION_DURATION, easing = FastOutSlowInEasing),
                                    targetOffsetX = slideOutTo
                                ) + fadeOut(animationSpec = tween(SONG_TRANSITION_DURATION)) + scaleOut(
                                    targetScale = 0.8f,
                                    animationSpec = tween(SONG_TRANSITION_DURATION)
                                )
                            )
                        },
                        label = "disc_transition"
                    ) { targetSongId ->
                        val targetSong = playbackState.currentSong ?: song
                        RotatingArtworkDisc(
                            artworkUri = targetSong.artworkUri,
                            songId = targetSongId,
                            isPlaying = playbackState.isPlaying,
                            onVolumeDrag = { delta ->
                                val newVol = (playbackState.volume + delta).coerceIn(0f, 1f)
                                viewModel.playbackManager.setVolume(newVol)
                                showVolumeHud = true
                                volumeHudJob?.cancel()
                                volumeHudJob = coroutineScope.launch {
                                    delay(2000)
                                    showVolumeHud = false
                                }
                            },
                            size = discSize
                        )
                    }
                }
            }

            // Título, Artista y Control de Volumen HUD
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                    color = Color.White,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1
                )

                // HUD de Volumen flotante
                AnimatedVisibility(
                    visible = showVolumeHud,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 20 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { 20 })
                ) {
                    Row(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when {
                            playbackState.volume == 0f -> Icons.AutoMirrored.Filled.VolumeOff
                            playbackState.volume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                            else -> Icons.AutoMirrored.Filled.VolumeUp
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Volumen",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = playbackState.volume,
                            onValueChange = { newVol ->
                                viewModel.playbackManager.setVolume(newVol)
                                showVolumeHud = true
                                volumeHudJob?.cancel()
                                volumeHudJob = coroutineScope.launch {
                                    delay(2000)
                                    showVolumeHud = false
                                }
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = visualizerColor,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.width(140.dp)
                        )
                    }
                }
            }

            // Barra de Progreso y Tiempos
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = if (playbackState.duration > 0) {
                        (playbackState.currentPosition.toFloat() / playbackState.duration.toFloat()).coerceIn(0f, 1f)
                    } else 0f,
                    onValueChange = { fraction ->
                        val targetMs = (fraction * playbackState.duration).toLong()
                        viewModel.playbackManager.seekTo(targetMs)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = visualizerColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(playbackState.currentPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatTime(playbackState.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Controles Principales de Reproducción
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.playbackManager.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Aleatorio",
                        tint = if (playbackState.isShuffleEnabled) visualizerColor else Color.White.copy(alpha = 0.5f)
                    )
                }

                IconButton(onClick = { viewModel.playbackManager.previous() }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Card(
                    onClick = { viewModel.playbackManager.playOrPause() },
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = visualizerColor),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pausar" else "Reproducir",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                IconButton(onClick = { viewModel.playbackManager.next() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Siguiente",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = { viewModel.playbackManager.toggleRepeat() }) {
                    val repeatIcon = if (playbackState.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Repetir",
                        tint = if (playbackState.repeatMode != RepeatMode.OFF) visualizerColor else Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

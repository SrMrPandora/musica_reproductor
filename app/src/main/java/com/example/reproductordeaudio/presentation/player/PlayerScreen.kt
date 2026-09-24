package com.example.reproductordeaudio.presentation.player

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
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tracing.Trace
import androidx.tracing.trace
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.reproductordeaudio.domain.model.RepeatMode
import com.example.reproductordeaudio.presentation.home.HomeViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SONG_TRANSITION_DURATION = 600

@Composable
fun PlayerScreen(
    viewModel: HomeViewModel,
    onBackClick: () -> Unit
) {
    val playbackState by viewModel.playbackManager.playbackState.collectAsState()
    val palette by viewModel.playbackManager.currentPalette.collectAsState()
    val amplitudes by viewModel.playbackManager.visualizerAmplitudes.collectAsState()

    val song = playbackState.currentSong ?: return

    var showVolumeHud by remember { mutableStateOf(false) }
    var volumeHudJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

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
        // Fondo desenfocado pre-renderizado offscreen
        Trace.beginSection("blur_render")
        Crossfade(
            targetState = song,
            animationSpec = tween(SONG_TRANSITION_DURATION),
            label = "background_crossfade"
        ) { targetSong ->
            val prepared = viewModel.playbackManager.preloadManager.getPreparedArtwork(targetSong.id)
            if (prepared?.blurredBitmap != null) {
                Image(
                    bitmap = prepared.blurredBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (!targetSong.artworkUri.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(targetSong.artworkUri)
                        .memoryCacheKey(targetSong.id.toString())
                        .diskCacheKey(targetSong.id.toString())
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
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

                val outerRadiusPx = containerSizePx / 2f
                val innerRadiusPx = discSizePx / 2f
                val maxBarLengthPx = outerRadiusPx - innerRadiusPx

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

                    // Transición "Cambio de Disco" (slide, scale, fade)
                    AnimatedContent(
                        targetState = song,
                        transitionSpec = {
                            (slideInHorizontally(
                                animationSpec = tween(SONG_TRANSITION_DURATION, easing = FastOutSlowInEasing)
                            ) { fullWidth -> fullWidth / 2 } +
                                    fadeIn(tween(SONG_TRANSITION_DURATION)) +
                                    scaleIn(initialScale = 0.85f, animationSpec = tween(SONG_TRANSITION_DURATION))) togetherWith
                                    (slideOutHorizontally(
                                        animationSpec = tween(SONG_TRANSITION_DURATION, easing = FastOutSlowInEasing)
                                    ) { fullWidth -> -fullWidth / 2 } +
                                            fadeOut(tween(SONG_TRANSITION_DURATION)) +
                                            scaleOut(targetScale = 0.85f, animationSpec = tween(SONG_TRANSITION_DURATION)))
                        },
                        label = "artwork_disc_transition"
                    ) { targetSong ->
                        RotatingArtworkDisc(
                            artworkUri = targetSong.artworkUri,
                            songId = targetSong.id,
                            isPlaying = playbackState.isPlaying,
                            onVolumeDrag = { delta ->
                                val newVol = (playbackState.volume + delta).coerceIn(0f, 1f)
                                viewModel.playbackManager.setVolume(newVol)
                                showVolumeHud = true
                                volumeHudJob?.cancel()
                                volumeHudJob = coroutineScope.launch {
                                    delay(1500L)
                                    showVolumeHud = false
                                }
                            },
                            size = discSize
                        )
                    }

                    // Indicator HUD de Volumen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedVisibility(
                            visible = showVolumeHud,
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(500))
                        ) {
                            val volPercent = (playbackState.volume * 100).toInt()
                            val icon = when {
                                volPercent == 0 -> Icons.AutoMirrored.Filled.VolumeOff
                                volPercent < 50 -> Icons.AutoMirrored.Filled.VolumeDown
                                else -> Icons.AutoMirrored.Filled.VolumeUp
                            }
                            Card(
                                shape = CircleShape,
                                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = "Volumen",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$volPercent%",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Información de la canción con animación suave
            AnimatedContent(
                targetState = song,
                transitionSpec = {
                    (slideInVertically(animationSpec = tween(SONG_TRANSITION_DURATION)) { height -> height / 2 } +
                            fadeIn(tween(SONG_TRANSITION_DURATION))) togetherWith
                            (slideOutVertically(animationSpec = tween(SONG_TRANSITION_DURATION)) { height -> -height / 2 } +
                                    fadeOut(tween(SONG_TRANSITION_DURATION)))
                },
                label = "song_info_transition"
            ) { targetSong ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = targetSong.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = targetSong.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Barra de progreso
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = playbackState.currentPosition.toFloat(),
                    onValueChange = { viewModel.playbackManager.seekTo(it.toLong()) },
                    valueRange = 0f..(playbackState.duration.coerceAtLeast(1L).toFloat()),
                    colors = SliderDefaults.colors(
                        thumbColor = heartColor,
                        activeTrackColor = heartColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(playbackState.currentPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatTime(playbackState.duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Controles de Reproducción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.playbackManager.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.isShuffleEnabled) heartColor else Color.White.copy(alpha = 0.5f)
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

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(heartColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { viewModel.playbackManager.playOrPause() }) {
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
                    Icon(
                        imageVector = when (playbackState.repeatMode) {
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repetir",
                        tint = if (playbackState.repeatMode != RepeatMode.OFF) heartColor else Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

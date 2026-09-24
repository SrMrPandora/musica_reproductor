package com.example.reproductordeaudio.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.reproductordeaudio.presentation.components.DefaultArtwork

@Composable
fun RotatingArtworkDisc(
    artworkUri: String?,
    songId: Long? = null,
    isPlaying: Boolean,
    onVolumeDrag: (Float) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 280.dp
) {
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(songId) {
        rotationAngle = 0f
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            var lastTime = withFrameNanos { it }
            while (isPlaying) {
                withFrameNanos { currentTime ->
                    val deltaSeconds = (currentTime - lastTime) / 1_000_000_000f
                    lastTime = currentTime
                    rotationAngle = (rotationAngle + deltaSeconds * 30f) % 360f
                }
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .shadow(24.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(0xFF1E1F28))
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    val change = -dragAmount / 300f
                    onVolumeDrag(change)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotationAngle),
            contentAlignment = Alignment.Center
        ) {
            if (!artworkUri.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(artworkUri)
                        .apply {
                            if (songId != null) {
                                memoryCacheKey(songId.toString())
                                diskCacheKey(songId.toString())
                            }
                        }
                        .crossfade(true)
                        .build(),
                    contentDescription = "Cover Artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                DefaultArtwork(
                    size = size,
                    iconSize = 80.dp
                )
            }
        }
    }
}

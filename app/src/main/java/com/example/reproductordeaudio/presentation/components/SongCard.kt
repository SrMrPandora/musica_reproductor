package com.example.reproductordeaudio.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.reproductordeaudio.data.local.db.CardIdentityEntity
import com.example.reproductordeaudio.domain.model.Song

@Composable
fun SongCard(
    song: Song,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardIdentity: CardIdentityEntity? = null,
    isCentered: Boolean = false
) {
    val gradientColors = remember(cardIdentity?.colorsJson) {
        cardIdentity?.colorsJson?.split(",")?.mapNotNull { str ->
            str.trim().toIntOrNull()?.let { Color(it) }
        }
    }

    val textColor = cardIdentity?.textColorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface
    val subTextColor = cardIdentity?.textColorArgb?.let { Color(it).copy(alpha = 0.7f) } ?: MaterialTheme.colorScheme.onSurfaceVariant

    val glowColor = remember(gradientColors) {
        gradientColors?.firstOrNull() ?: Color(0xFF6200EE)
    }

    val glowModifier = if (isCentered) {
        Modifier.drawBehind {
            val maxDim = size.maxDimension
            val radius = maxDim / 1.3f
            val strokeWidth = 2.dp.toPx()
            val cornerRadius = 12.dp.toPx()

            // Capa 1: Resplandor radial exterior desbordante
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.65f),
                        glowColor.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Capa 2: Anillo / Borde definido brillante
            drawRoundRect(
                color = glowColor.copy(alpha = 0.8f),
                style = Stroke(width = strokeWidth),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        }
    } else Modifier

    val cardBackground = if (gradientColors != null && gradientColors.isNotEmpty()) {
        Modifier.background(Brush.linearGradient(colors = gradientColors))
    } else {
        Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("song_item")
            .semantics { contentDescription = "song_item" }
            .then(glowModifier)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(cardBackground)
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
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                DefaultArtwork(size = 56.dp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = subTextColor
                )
            }

            if (song.isFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorito",
                    tint = Color(0xFFFF4081),
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 4.dp)
                )
            }

            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opciones",
                    tint = subTextColor
                )
            }
        }
    }
}

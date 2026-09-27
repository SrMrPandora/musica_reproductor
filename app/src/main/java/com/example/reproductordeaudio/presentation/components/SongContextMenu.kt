package com.example.reproductordeaudio.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.reproductordeaudio.domain.model.Song
import com.example.reproductordeaudio.ui.theme.LocalDynamicColors

@Composable
fun SongContextMenu(
    expanded: Boolean,
    song: Song?,
    onDismissRequest: () -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit
) {
    if (song == null) return

    val dynamicColors = LocalDynamicColors.current

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        containerColor = dynamicColors.surface
    ) {
        DropdownMenuItem(
            text = { Text("Reproducir", color = dynamicColors.textColor) },
            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = dynamicColors.iconTint) },
            onClick = {
                onPlay()
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text(if (song.isFavorite) "Quitar de favoritos" else "Agregar a favoritos", color = dynamicColors.textColor) },
            leadingIcon = {
                Icon(
                    if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = dynamicColors.iconTint
                )
            },
            onClick = {
                onToggleFavorite()
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text("Agregar a playlist", color = dynamicColors.textColor) },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, tint = dynamicColors.iconTint) },
            onClick = {
                onAddToPlaylist()
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text("Eliminar del dispositivo", color = dynamicColors.textColor) },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = dynamicColors.iconTint) },
            onClick = {
                onDelete()
                onDismissRequest()
            }
        )
    }
}

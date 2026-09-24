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

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = { Text("Reproducir") },
            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
            onClick = {
                onPlay()
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text(if (song.isFavorite) "Quitar de favoritos" else "Agregar a favoritos") },
            leadingIcon = {
                Icon(
                    if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null
                )
            },
            onClick = {
                onToggleFavorite()
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text("Agregar a playlist") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
            onClick = {
                onAddToPlaylist()
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            text = { Text("Eliminar del dispositivo") },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
            onClick = {
                onDelete()
                onDismissRequest()
            }
        )
    }
}

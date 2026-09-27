package com.example.reproductordeaudio.domain.model

import androidx.compose.runtime.Immutable
import com.example.reproductordeaudio.data.local.db.SongEntity

@Immutable
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String?,
    val uri: String,
    val artworkUri: String?,
    val duration: Long,
    val isFavorite: Boolean,
    val playCount: Int,
    val lastPlayedAt: Long?
)

fun SongEntity.toDomain(): Song = Song(
    id = mediaStoreId,
    title = title,
    artist = artist,
    album = album,
    uri = uri,
    artworkUri = artworkUri,
    duration = duration,
    isFavorite = isFavorite,
    playCount = playCount,
    lastPlayedAt = lastPlayedAt
)

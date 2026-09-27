package com.example.reproductordeaudio.domain.model

import com.example.reproductordeaudio.data.local.db.PlaylistEntity

data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int = 0
)

fun PlaylistEntity.toDomain(): Playlist = Playlist(
    id = id,
    name = name
)

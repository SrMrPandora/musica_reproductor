package com.example.reproductordeaudio.domain.model

data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int = 0
)

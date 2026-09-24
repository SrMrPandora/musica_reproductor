package com.example.reproductordeaudio.domain.model

data class Artist(
    val name: String,
    val normalizedName: String,
    val songCount: Int,
    val songs: List<Song>
)

fun String.normalizeArtistName(): String {
    return this.trim().lowercase()
}

package com.example.reproductordeaudio.domain.model

enum class PlayerState {
    IDLE, LOADING, READY, PLAYING, PAUSED, BUFFERING, ERROR
}

enum class RepeatMode {
    OFF, ALL, ONE
}

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val playerState: PlayerState = PlayerState.IDLE,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val volume: Float = 1.0f,
    val errorMessage: String? = null
)

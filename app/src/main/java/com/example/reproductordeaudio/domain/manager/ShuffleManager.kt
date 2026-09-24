package com.example.reproductordeaudio.domain.manager

import com.example.reproductordeaudio.domain.model.Song

class ShuffleManager {

    fun createShuffledQueue(songs: List<Song>, currentSong: Song?): List<Song> {
        if (songs.isEmpty()) return emptyList()
        val shuffled = songs.filter { it.id != currentSong?.id }.shuffled().toMutableList()
        currentSong?.let { shuffled.add(0, it) }
        return shuffled
    }
}

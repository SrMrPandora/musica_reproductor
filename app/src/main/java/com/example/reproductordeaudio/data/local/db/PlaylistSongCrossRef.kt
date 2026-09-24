package com.example.reproductordeaudio.data.local.db

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "mediaStoreId"],
    indices = [Index("mediaStoreId")]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val mediaStoreId: Long,
    val position: Int = 0
)

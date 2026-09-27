package com.example.reproductordeaudio.domain.analyzer

import android.graphics.Bitmap

data class PreparedArtwork(
    val songId: Long,
    val bitmap: Bitmap?,
    val palette: ArtworkPalette
)

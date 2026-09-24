package com.example.reproductordeaudio.domain.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ArtworkPalette(
    val primaryColor: Color,
    val secondaryColor: Color,
    val heartColor: Color,
    val backgroundColor: Color
)

class ArtworkAnalyzer(private val context: Context) {

    private val paletteCache = mutableMapOf<String, ArtworkPalette>()

    suspend fun analyzeArtwork(artworkUri: String?): ArtworkPalette = withContext(Dispatchers.IO) {
        if (artworkUri == null) return@withContext getDefaultPalette()
        paletteCache[artworkUri]?.let { return@withContext it }

        val bitmap = loadBitmapFromUri(artworkUri) ?: return@withContext getDefaultPalette()
        val palette = Palette.from(bitmap).generate()

        val defaultColor = 0xFF212121.toInt()
        val defaultAccent = 0xFFFF4081.toInt()

        val dominantColorInt = palette.getDominantColor(defaultColor)
        val vibrantColorInt = palette.getVibrantColor(palette.getMutedColor(defaultAccent))
        val darkMutedColorInt = palette.getDarkMutedColor(palette.getDarkVibrantColor(defaultColor))

        val primaryColor = Color(dominantColorInt)
        val secondaryColor = Color(vibrantColorInt)
        val backgroundColor = Color(darkMutedColorInt)

        val heartColor = ensureLegibleColor(Color(vibrantColorInt), backgroundColor)

        val artworkPalette = ArtworkPalette(
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            heartColor = heartColor,
            backgroundColor = backgroundColor
        )

        paletteCache[artworkUri] = artworkPalette
        artworkPalette
    }

    private fun loadBitmapFromUri(uriString: String): Bitmap? {
        val uri = Uri.parse(uriString)
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val pictureBytes = retriever.embeddedPicture
            if (pictureBytes != null) {
                return BitmapFactory.decodeByteArray(pictureBytes, 0, pictureBytes.size)
            }
        } catch (e: Exception) {
            // Fallback
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun ensureLegibleColor(accentColor: Color, backgroundColor: Color): Color {
        val accentArgb = accentColor.toArgb()
        val bgArgb = backgroundColor.toArgb()

        val contrastRatio = ColorUtils.calculateContrast(accentArgb, bgArgb)
        if (contrastRatio >= 3.0) return accentColor

        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(accentArgb, hsl)

        val bgLuminance = ColorUtils.calculateLuminance(bgArgb)
        if (bgLuminance < 0.5) {
            hsl[2] = (hsl[2] + 0.3f).coerceAtMost(0.9f)
            hsl[1] = (hsl[1] + 0.2f).coerceAtMost(1.0f)
        } else {
            hsl[2] = (hsl[2] - 0.3f).coerceAtLeast(0.1f)
        }

        val adjustedArgb = ColorUtils.HSLToColor(hsl)
        return Color(adjustedArgb)
    }

    private fun getDefaultPalette(): ArtworkPalette {
        return ArtworkPalette(
            primaryColor = Color(0xFF6200EE),
            secondaryColor = Color(0xFF03DAC6),
            heartColor = Color(0xFFFF4081),
            backgroundColor = Color(0xFF121212)
        )
    }
}

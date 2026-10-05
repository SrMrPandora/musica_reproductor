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

data class CardIdentity(
    val gradientColors: List<Color>,
    val textColor: Color
)

class ArtworkAnalyzer(private val context: Context) {

    private val paletteCache = mutableMapOf<String, ArtworkPalette>()

    suspend fun analyzeArtwork(artworkUri: String?): ArtworkPalette = withContext(Dispatchers.IO) {
        if (artworkUri == null) return@withContext getDefaultPalette()
        paletteCache[artworkUri]?.let { return@withContext it }

        val bitmap = loadBitmapFromUri(artworkUri) ?: return@withContext getDefaultPalette()
        val palette = Palette.from(bitmap).generate()

        val dominantColorInt = palette.getDominantColor(DEFAULT_DARK_COLOR)
        val vibrantColorInt = palette.getVibrantColor(palette.getMutedColor(DEFAULT_ACCENT_COLOR))
        val darkMutedColorInt = palette.getDarkMutedColor(palette.getDarkVibrantColor(DEFAULT_DARK_COLOR))

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

    suspend fun extractCardIdentity(bitmap: Bitmap): CardIdentity = withContext(Dispatchers.Default) {
        val palette = Palette.from(bitmap).generate()
        val topSwatches = palette.swatches
            .sortedByDescending { it.population }
            .take(5)

        val swatches = topSwatches
            .map { Color(it.rgb) }
            .sortedBy { ColorUtils.calculateLuminance(it.toArgb()) }

        val gradientColors = when {
            swatches.isEmpty() -> DEFAULT_CARD_GRADIENT
            swatches.size == 1 -> listOf(swatches.first(), swatches.first())
            else -> swatches
        }

        val lastColorArgb = gradientColors.last().toArgb()
        val whiteContrast = ColorUtils.calculateContrast(Color.White.toArgb(), lastColorArgb)
        val textColor = if (whiteContrast >= 3.0) Color.White else Color.Black

        CardIdentity(
            gradientColors = gradientColors,
            textColor = textColor
        )
    }

    fun loadBitmapFromUri(uriString: String): Bitmap? {
        val uri = Uri.parse(uriString)
        return loadEmbeddedPicture(uri) ?: loadStreamBitmap(uri)
    }

    private fun loadEmbeddedPicture(uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture?.let { bytes ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignored
            }
        }
    }

    private fun loadStreamBitmap(uri: Uri): Bitmap? {
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

        if (ColorUtils.calculateContrast(accentArgb, bgArgb) >= 3.0) {
            return accentColor
        }

        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(accentArgb, hsl)

        val bgLuminance = ColorUtils.calculateLuminance(bgArgb)
        if (bgLuminance < 0.5) {
            hsl[2] = (hsl[2] + 0.3f).coerceAtMost(0.9f)
            hsl[1] = (hsl[1] + 0.2f).coerceAtMost(1.0f)
        } else {
            hsl[2] = (hsl[2] - 0.3f).coerceAtLeast(0.1f)
        }

        return Color(ColorUtils.HSLToColor(hsl))
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

private const val DEFAULT_DARK_COLOR = 0xFF212121.toInt()
private const val DEFAULT_ACCENT_COLOR = 0xFFFF4081.toInt()
private val DEFAULT_CARD_GRADIENT = listOf(Color(0xFF212121), Color(0xFF121212))

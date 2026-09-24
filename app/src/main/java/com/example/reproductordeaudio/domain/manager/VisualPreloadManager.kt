package com.example.reproductordeaudio.domain.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.tracing.Trace
import com.example.reproductordeaudio.domain.analyzer.ArtworkAnalyzer
import com.example.reproductordeaudio.domain.analyzer.PreparedArtwork
import com.example.reproductordeaudio.domain.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class VisualPreloadManager(
    private val context: Context,
    private val artworkAnalyzer: ArtworkAnalyzer
) {

    private val cache = LruCache<Long, PreparedArtwork>(12)
    private val activePreloadJobs = ConcurrentHashMap<Long, Job>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun updateWindow(currentSong: Song?, queue: List<Song>) {
        if (currentSong == null || queue.isEmpty()) return

        val currentIndex = queue.indexOfFirst { it.id == currentSong.id }
        if (currentIndex < 0) return

        val nextSongs = mutableListOf<Pair<Int, Song>>()
        for (i in 1..5) {
            val idx = currentIndex + i
            if (idx < queue.size) {
                nextSongs.add(Pair(i, queue[idx]))
            }
        }

        val windowSongIds = nextSongs.map { it.second.id }.toSet() + currentSong.id

        activePreloadJobs.keys.filter { it !in windowSongIds }.forEach { id ->
            activePreloadJobs[id]?.cancel()
            activePreloadJobs.remove(id)
        }

        scope.launch(Dispatchers.IO) {
            if (cache.get(currentSong.id) == null) {
                preloadSongVisuals(currentSong)
            }

            for ((priority, song) in nextSongs) {
                if (cache.get(song.id) == null && !activePreloadJobs.containsKey(song.id)) {
                    val job = launch(Dispatchers.IO) {
                        preloadSongVisuals(song)
                    }
                    activePreloadJobs[song.id] = job
                    if (priority == 1) {
                        job.join()
                    }
                }
            }
        }
    }

    fun getPreparedArtwork(songId: Long): PreparedArtwork? {
        return cache.get(songId)
    }

    private suspend fun preloadSongVisuals(song: Song) = withContext(Dispatchers.IO) {
        try {
            Trace.beginSection("cover_decode")
            val palette = artworkAnalyzer.analyzeArtwork(song.artworkUri)
            val bitmap = loadBitmapFromUri(song.uri)
            val blurredBitmap = bitmap?.let { createFastBlurredBitmap(it) }

            val prepared = PreparedArtwork(
                songId = song.id,
                bitmap = bitmap,
                blurredBitmap = blurredBitmap,
                palette = palette
            )

            cache.put(song.id, prepared)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            Trace.endSection()
            activePreloadJobs.remove(song.id)
        }
    }

    private fun loadBitmapFromUri(uriString: String): Bitmap? {
        val uri = Uri.parse(uriString)
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val pictureBytes = retriever.embeddedPicture
            if (pictureBytes != null && pictureBytes.isNotEmpty()) {
                BitmapFactory.decodeByteArray(pictureBytes, 0, pictureBytes.size)
            } else null
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createFastBlurredBitmap(src: Bitmap): Bitmap? {
        return try {
            val scale = 0.15f
            val width = (src.width * scale).toInt().coerceAtLeast(16)
            val height = (src.height * scale).toInt().coerceAtLeast(16)

            val smallBitmap = Bitmap.createScaledBitmap(src, width, height, true)
            boxBlur(smallBitmap, 6)
        } catch (e: Exception) {
            null
        }
    }

    private fun boxBlur(bitmap: Bitmap, radius: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int; var gsum: Int; var bsum: Int
        var p: Int; var yp: Int; var yi: Int; var yw: Int

        yw = 0
        yi = 0

        for (y in 0 until h) {
            bsum = 0; gsum = 0; rsum = 0
            for (i in -radius..radius) {
                p = pix[yi + maxOf(0, minOf(wm, i))]
                rsum += (p and 0xff0000) shr 16
                gsum += (p and 0x00ff00) shr 8
                bsum += p and 0x0000ff
            }
            for (x in 0 until w) {
                r[yi] = rsum / div
                g[yi] = gsum / div
                b[yi] = bsum / div

                p = pix[yw + minOf(x + radius + 1, wm)]
                rsum += (p and 0xff0000) shr 16 - ((pix[yw + maxOf(x - radius, 0)] and 0xff0000) shr 16)
                gsum += (p and 0x00ff00) shr 8 - ((pix[yw + maxOf(x - radius, 0)] and 0x00ff00) shr 8)
                bsum += (p and 0x0000ff) - (pix[yw + maxOf(x - radius, 0)] and 0x0000ff)

                yi++
            }
            yw += w
        }

        for (x in 0 until w) {
            bsum = 0; gsum = 0; rsum = 0
            yp = -radius * w
            for (i in -radius..radius) {
                yi = maxOf(0, yp) + x
                rsum += r[yi]
                gsum += g[yi]
                bsum += b[yi]
                yp += w
            }
            yi = x
            for (y in 0 until h) {
                pix[yi] = (0xff000000.toInt() and pix[yi]) or
                        ((rsum / div) shl 16) or
                        ((gsum / div) shl 8) or
                        (bsum / div)

                p = x + minOf(y + radius + 1, hm) * w
                rsum += r[p] - r[maxOf(0, x + (y - radius) * w)]
                gsum += g[p] - g[maxOf(0, x + (y - radius) * w)]
                bsum += b[p] - b[maxOf(0, x + (y - radius) * w)]

                yi += w
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }
}

package com.example.reproductordeaudio.domain.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Trace
import android.util.LruCache
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

            val prepared = PreparedArtwork(
                songId = song.id,
                bitmap = bitmap,
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
}

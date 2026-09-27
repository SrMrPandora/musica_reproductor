package com.example.reproductordeaudio.domain.sync

import androidx.compose.ui.graphics.toArgb
import com.example.reproductordeaudio.data.local.db.CardIdentityDao
import com.example.reproductordeaudio.data.local.db.CardIdentityEntity
import com.example.reproductordeaudio.data.repository.MediaStoreRepository
import com.example.reproductordeaudio.data.repository.RoomRepository
import com.example.reproductordeaudio.domain.analyzer.ArtworkAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LibrarySyncManager(
    private val mediaStoreRepository: MediaStoreRepository,
    private val roomRepository: RoomRepository,
    private val cardIdentityDao: CardIdentityDao,
    private val artworkAnalyzer: ArtworkAnalyzer
) {
    suspend fun syncLibrary() = withContext(Dispatchers.IO) {
        val mediaStoreSongs = mediaStoreRepository.fetchAudioFiles()
        val mediaStoreIds = mediaStoreSongs.map { it.mediaStoreId }

        for (msSong in mediaStoreSongs) {
            val existingSong = roomRepository.getSongById(msSong.mediaStoreId)
            if (existingSong == null) {
                roomRepository.saveSongs(listOf(msSong))
            } else {
                if (existingSong.title != msSong.title ||
                    existingSong.artist != msSong.artist ||
                    existingSong.album != msSong.album ||
                    existingSong.dateModified != msSong.dateModified
                ) {
                    val updated = existingSong.copy(
                        title = msSong.title,
                        artist = msSong.artist,
                        album = msSong.album,
                        uri = msSong.uri,
                        artworkUri = msSong.artworkUri,
                        duration = msSong.duration,
                        dateModified = msSong.dateModified
                    )
                    roomRepository.saveSongs(listOf(updated))
                }
            }

            if (!cardIdentityDao.exists(msSong.mediaStoreId)) {
                val bitmap = artworkAnalyzer.loadBitmapFromUri(msSong.uri)
                if (bitmap != null) {
                    val identity = artworkAnalyzer.extractCardIdentity(bitmap)
                    val colorsJson = identity.gradientColors.joinToString(",") { it.toArgb().toString() }
                    cardIdentityDao.upsert(
                        CardIdentityEntity(
                            mediaStoreId = msSong.mediaStoreId,
                            colorsJson = colorsJson,
                            textColorArgb = identity.textColor.toArgb()
                        )
                    )
                }
            }
        }

        if (mediaStoreIds.isNotEmpty()) {
            roomRepository.deleteOrphans(mediaStoreIds)
        }
    }
}

package com.example.reproductordeaudio.domain.manager

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.reproductordeaudio.data.repository.RoomRepository
import com.example.reproductordeaudio.domain.analyzer.ArtworkAnalyzer
import com.example.reproductordeaudio.domain.analyzer.ArtworkPalette
import com.example.reproductordeaudio.domain.analyzer.PreparedArtwork
import com.example.reproductordeaudio.domain.model.PlaybackState
import com.example.reproductordeaudio.domain.model.PlayerState
import com.example.reproductordeaudio.domain.model.RepeatMode
import com.example.reproductordeaudio.domain.model.Song
import com.example.reproductordeaudio.domain.model.toDomain
import com.example.reproductordeaudio.service.AudioVisualizerProcessor
import com.example.reproductordeaudio.service.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlaybackManager(
    private val context: Context,
    private val roomRepository: RoomRepository,
    private val historyManager: HistoryManager,
    private val artworkAnalyzer: ArtworkAnalyzer
) {

    private var player: Player? = null
    private var mediaControllerFuture = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, PlaybackService::class.java))
    ).buildAsync()

    private val visualizerProcessor = AudioVisualizerProcessor.getInstance()
    val preloadManager = VisualPreloadManager(context, artworkAnalyzer)

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPalette = MutableStateFlow<ArtworkPalette?>(null)
    val currentPalette: StateFlow<ArtworkPalette?> = _currentPalette.asStateFlow()

    private val _currentPreparedArtwork = MutableStateFlow<PreparedArtwork?>(null)
    val currentPreparedArtwork: StateFlow<PreparedArtwork?> = _currentPreparedArtwork.asStateFlow()

    val visualizerAmplitudes: StateFlow<FloatArray> = visualizerProcessor.amplitudesFlow

    private var currentQueue = mutableListOf<Song>()
    private var originalQueue = mutableListOf<Song>()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressUpdateJob: Job? = null
    private var visualizerTickerJob: Job? = null
    private var songObserverJob: Job? = null
    private var playedTimeMs: Long = 0L
    private var hasRecordedHistory: Boolean = false

    init {
        mediaControllerFuture.addListener({
            try {
                val controller = mediaControllerFuture.get()
                player = controller
                setupPlayerListener(controller)
                visualizerProcessor.start(0)
                updatePlaybackState()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupPlayerListener(p: Player) {
        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                updatePlaybackState()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState()
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val mediaId = mediaItem?.mediaId?.toLongOrNull()
                val activeSong = findSongInQueue(mediaId, p.currentMediaItemIndex)

                if (activeSong != null) {
                    observeCurrentSongInRoom(activeSong.id)
                    resetHistoryTracker()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _playbackState.update {
                    it.copy(
                        playerState = PlayerState.ERROR,
                        errorMessage = error.localizedMessage ?: "Error de reproducción"
                    )
                }
                scope.launch {
                    delay(1500L)
                    next()
                }
            }
        })
    }

    private fun findSongInQueue(mediaId: Long?, fallbackIndex: Int): Song? {
        if (mediaId != null) {
            return originalQueue.find { it.id == mediaId } ?: currentQueue.find { it.id == mediaId }
        }
        return currentQueue.getOrNull(fallbackIndex)
    }

    private fun observeCurrentSongInRoom(songId: Long) {
        songObserverJob?.cancel()
        songObserverJob = scope.launch {
            roomRepository.getSongFlowById(songId).collect { entity ->
                val updatedSong = entity?.toDomain() ?: return@collect
                _playbackState.update { currentState ->
                    currentState.copy(currentSong = updatedSong)
                }
                analyzeCurrentArtwork(updatedSong)
            }
        }
    }

    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        val p = player ?: return
        if (songs.isEmpty()) return

        originalQueue = songs.toMutableList()
        currentQueue = songs.toMutableList()

        val validIndex = startIndex.coerceIn(0, songs.lastIndex)
        val mediaItems = songs.map { it.toMediaItem() }

        p.setMediaItems(mediaItems, validIndex, 0L)
        p.shuffleModeEnabled = _playbackState.value.isShuffleEnabled
        p.prepare()
        p.playWhenReady = true

        val song = songs[validIndex]
        observeCurrentSongInRoom(song.id)
        resetHistoryTracker()
    }

    fun playOrPause() {
        val p = player ?: return
        if (p.isPlaying) {
            p.pause()
        } else {
            p.play()
        }
    }

    fun next() {
        val p = player ?: return
        if (p.hasNextMediaItem()) {
            p.seekToNextMediaItem()
        } else if (_playbackState.value.repeatMode == RepeatMode.ALL) {
            p.seekTo(0, 0L)
        }
    }

    fun previous() {
        val p = player ?: return
        if (p.currentPosition > 3000L) {
            p.seekTo(0L)
        } else if (p.hasPreviousMediaItem()) {
            p.seekToPreviousMediaItem()
        }
    }

    fun seekTo(positionMs: Long) {
        val p = player ?: return
        p.seekTo(positionMs)
        _playbackState.update { it.copy(currentPosition = positionMs) }
    }

    fun toggleShuffle() {
        val p = player ?: return
        val newShuffle = !_playbackState.value.isShuffleEnabled
        _playbackState.update { it.copy(isShuffleEnabled = newShuffle) }
        p.shuffleModeEnabled = newShuffle
    }

    fun toggleRepeat() {
        val p = player ?: return
        val nextMode = _playbackState.value.repeatMode.next()
        _playbackState.update { it.copy(repeatMode = nextMode) }
        p.repeatMode = nextMode.toPlayerRepeatMode()
    }

    fun setVolume(volume: Float) {
        val p = player ?: return
        val clamped = volume.coerceIn(0f, 1f)
        p.volume = clamped
        _playbackState.update { it.copy(volume = clamped) }
    }

    private fun analyzeCurrentArtwork(song: Song) {
        preloadManager.updateWindow(song, currentQueue)
        val cachedPrepared = preloadManager.getPreparedArtwork(song.id)
        if (cachedPrepared != null) {
            _currentPalette.value = cachedPrepared.palette
            _currentPreparedArtwork.value = cachedPrepared
        } else {
            scope.launch {
                val palette = artworkAnalyzer.analyzeArtwork(song.artworkUri)
                _currentPalette.value = palette
            }
        }
    }

    private fun startProgressTracker() {
        progressUpdateJob?.cancel()
        progressUpdateJob = scope.launch {
            val p = player
            while (p != null && p.isPlaying) {
                val pos = p.currentPosition
                val dur = p.duration.coerceAtLeast(0L)
                _playbackState.update {
                    it.copy(
                        currentPosition = pos,
                        duration = dur
                    )
                }

                playedTimeMs += 1000L
                val currentSong = _playbackState.value.currentSong
                if (currentSong != null && !hasRecordedHistory && historyManager.shouldRecordHistory(playedTimeMs, dur)) {
                    hasRecordedHistory = true
                    historyManager.recordPlayback(currentSong.id)
                }

                delay(1000L)
            }
        }

        visualizerTickerJob?.cancel()
        visualizerTickerJob = scope.launch {
            while (true) {
                val isPlaying = player?.isPlaying == true
                visualizerProcessor.onTick(isPlaying)
                if (!isPlaying) break
                delay(33L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressUpdateJob?.cancel()
        visualizerTickerJob?.cancel()
        visualizerProcessor.onTick(false)
    }

    private fun resetHistoryTracker() {
        playedTimeMs = 0L
        hasRecordedHistory = false
    }

    private fun updatePlaybackState() {
        val p = player ?: return
        val pState = when (p.playbackState) {
            Player.STATE_IDLE -> PlayerState.IDLE
            Player.STATE_BUFFERING -> PlayerState.BUFFERING
            Player.STATE_READY -> if (p.isPlaying) PlayerState.PLAYING else PlayerState.PAUSED
            Player.STATE_ENDED -> PlayerState.PAUSED
            else -> PlayerState.IDLE
        }

        _playbackState.update {
            it.copy(
                isPlaying = p.isPlaying,
                playerState = pState,
                currentPosition = p.currentPosition,
                duration = p.duration.coerceAtLeast(0L)
            )
        }
    }

    fun getSongIndex(songId: Long): Int {
        val indexInCurrent = currentQueue.indexOfFirst { it.id == songId }
        if (indexInCurrent != -1) return indexInCurrent
        return originalQueue.indexOfFirst { it.id == songId }
    }

    fun getQueueSize(): Int = currentQueue.size.coerceAtLeast(originalQueue.size)

    fun release() {
        stopProgressTracker()
        songObserverJob?.cancel()
        visualizerProcessor.release()
        MediaController.releaseFuture(mediaControllerFuture)
    }
}

private fun Song.toMediaItem(): MediaItem {
    return MediaItem.Builder()
        .setUri(Uri.parse(uri))
        .setMediaId(id.toString())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(artworkUri?.let { Uri.parse(it) })
                .build()
        ).build()
}

private fun RepeatMode.next(): RepeatMode = when (this) {
    RepeatMode.OFF -> RepeatMode.ALL
    RepeatMode.ALL -> RepeatMode.ONE
    RepeatMode.ONE -> RepeatMode.OFF
}

private fun RepeatMode.toPlayerRepeatMode(): Int = when (this) {
    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
}

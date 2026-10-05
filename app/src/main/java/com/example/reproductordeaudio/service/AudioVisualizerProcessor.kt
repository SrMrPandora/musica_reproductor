package com.example.reproductordeaudio.service

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow

class AudioVisualizerProcessor private constructor() {

    private var visualizer: Visualizer? = null
    private val bandCount = 32
    private val rawAmplitudes = FloatArray(bandCount)
    private val smoothedAmplitudes = FloatArray(bandCount)

    private val _amplitudesFlow = MutableStateFlow(FloatArray(bandCount))
    val amplitudesFlow: StateFlow<FloatArray> = _amplitudesFlow.asStateFlow()

    private var lastFftTimestamp = 0L

    companion object {
        @Volatile
        private var INSTANCE: AudioVisualizerProcessor? = null

        fun getInstance(): AudioVisualizerProcessor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioVisualizerProcessor().also { INSTANCE = it }
            }
        }
    }

    fun start(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        release()

        try {
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            v: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (waveform != null) processWaveform(waveform)
                        }

                        override fun onFftDataCapture(
                            v: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (fft != null) {
                                lastFftTimestamp = System.currentTimeMillis()
                                processFft(fft)
                            }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false,
                    true
                )
                enabled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onTick(isPlaying: Boolean) {
        if (!isPlaying) {
            var hasSignal = false
            for (i in 0 until bandCount) {
                smoothedAmplitudes[i] *= 0.8f
                if (smoothedAmplitudes[i] > 0.005f) hasSignal = true else smoothedAmplitudes[i] = 0f
            }
            if (hasSignal) {
                _amplitudesFlow.value = smoothedAmplitudes.copyOf()
            }
            return
        }

        applySmoothingAndPublish()
    }

    private fun processWaveform(waveform: ByteArray) {
        val step = waveform.size / bandCount
        for (i in 0 until bandCount) {
            val idx = (i * step).coerceIn(0, waveform.size - 1)
            val sample = (waveform[idx].toInt() and 0xFF) - 128
            val norm = abs(sample) / 128f
            rawAmplitudes[i] = norm
        }
        applySmoothingAndPublish()
    }

    private fun processFft(fft: ByteArray) {
        val n = fft.size
        if (n <= 2) return

        val maxIndex = n / 2
        val usefulMaxIndex = (maxIndex * 0.45).toInt().coerceAtLeast(1)
        val magnitudes = FloatArray(maxIndex)

        // Magnitudes FFT
        magnitudes[0] = abs(fft[0].toFloat())
        for (k in 1 until maxIndex) {
            val r = fft[2 * k].toFloat()
            val i = fft[2 * k + 1].toFloat()
            magnitudes[k] = hypot(r, i)
        }

        // Mapeo logarítmico para distribución musical en rango activo (0 Hz - ~10 kHz)
        for (i in 0 until bandCount) {
            val minBin = (usefulMaxIndex * (2.0.pow(i.toDouble() / bandCount) - 1.0)).toInt().coerceIn(0, usefulMaxIndex - 1)
            val maxBin = (usefulMaxIndex * (2.0.pow((i + 1).toDouble() / bandCount) - 1.0)).toInt().coerceIn(minBin + 1, usefulMaxIndex)

            var sum = 0f
            var count = 0
            for (bin in minBin until maxBin) {
                sum += magnitudes[bin]
                count++
            }
            val avg = if (count > 0) sum / count else 0f

            val gainFactor = 1.0f + (i.toFloat() / bandCount) * 1.5f
            val norm = (avg / 40f * gainFactor).coerceIn(0.05f, 1f)
            rawAmplitudes[i] = norm
        }

        applySmoothingAndPublish()
    }

    private fun applySmoothingAndPublish() {
        val decay = 0.82f
        val attack = 0.45f
        val result = FloatArray(bandCount)

        for (i in 0 until bandCount) {
            val raw = rawAmplitudes[i]
            val current = smoothedAmplitudes[i]
            smoothedAmplitudes[i] = if (raw > current) {
                current + (raw - current) * attack
            } else {
                current * decay
            }
            result[i] = smoothedAmplitudes[i]
        }
        _amplitudesFlow.value = result
    }

    fun release() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

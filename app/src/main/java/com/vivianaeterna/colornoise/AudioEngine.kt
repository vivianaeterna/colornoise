package com.vivianaeterna.colornoise // <-- CHANGE THIS

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.exp
import kotlin.math.PI

class AudioEngine {

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private var audioTrack: AudioTrack? = null
    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var playJob: Job? = null

    private var currentVolume: Float = 0.5f
    private var currentBands = FloatArray(10) { 1.0f }

    // Crossover filter state
    private val filterState = DoubleArray(10) { 0.0 }

    // The 10 frequency cutoffs (Left/Deep Bass to Right/High Treble)
    private val cutoffFreqs = doubleArrayOf(
        60.0, 150.0, 350.0, 800.0, 1500.0, 3000.0, 5500.0, 9000.0, 14000.0, 18000.0
    )

    private val filterCoeffs = cutoffFreqs.map { 1.0 - exp(-2.0 * PI * it / sampleRate) }

    fun play() {
        if (playJob?.isActive == true) return
        resetFilters()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(audioFormat).setSampleRate(sampleRate).setChannelMask(channelConfig).build())
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        playJob = engineScope.launch {
            val buffer = ShortArray(bufferSize)
            while (isActive) {
                generateEqualizedNoise(buffer)
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }
    }

    fun stop() {
        playJob?.cancel()
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    fun setVolume(volume: Float) {
        currentVolume = volume
        audioTrack?.setVolume(volume)
    }

    fun updateBands(bands: List<Float>) {
        if (bands.size == 10) {
            currentBands = bands.toFloatArray()
        }
    }

    private fun generateEqualizedNoise(buffer: ShortArray) {
        val maxAmp = Short.MAX_VALUE.toDouble()

        for (i in buffer.indices) {
            val white = (Math.random() * 2.0 - 1.0)
            var output = 0.0
            var previous_lp = 0.0

            // --- NEW CORRECTED CROSSOVER LOGIC ---
            for (band in 0 until 9) {
                // Apply low-pass filter to the raw white noise
                filterState[band] += filterCoeffs[band] * (white - filterState[band])
                val current_lp = filterState[band]

                // The slice for this band is the difference between the current low-pass and the previous one
                // Band 0: 0-60Hz. Band 1: 60-150Hz, etc.
                val bandSlice = current_lp - previous_lp

                // Scale by the slider value and add to output
                output += bandSlice * currentBands[band]

                // Pass the current low-pass to the next band
                previous_lp = current_lp
            }

            // The 10th band (highest frequencies) is whatever is left over from the raw white noise
            val bandSlice9 = white - previous_lp
            output += bandSlice9 * currentBands[9]

            buffer[i] = (output * maxAmp).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun resetFilters() {
        filterState.fill(0.0)
    }

    fun release() {
        stop()
        engineScope.cancel()
    }
}
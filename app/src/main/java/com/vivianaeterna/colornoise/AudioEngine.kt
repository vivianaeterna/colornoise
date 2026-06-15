package com.vivianaeterna.colornoise

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*

/**
 * This class handles the actual audio generation.
 * It runs on a background thread (Coroutine) so it doesn't freeze the app.
 */
class AudioEngine {

    // --- Audio Configuration ---
    private val sampleRate = 44100 // Standard CD quality sample rate
    // Mono sound is enough for noise, saves CPU
    private val channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    // Calculate how much data the audio card needs per chunk to avoid stuttering
    private val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    // The Android object that actually sends sound to the speakers
    private var audioTrack: AudioTrack? = null

    // Coroutine scope for running the infinite audio loop in the background
    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var playJob: Job? = null

    // --- State Variables for Noise Generation ---
    // These must survive between buffer writes, especially for Pink and Brown noise!
    private var currentNoiseType: NoiseType = NoiseType.WHITE
    private var currentVolume: Float = 0.5f

    // Brown noise uses a "random walk" - it remembers the last sample to generate the next
    private var brownLastOut: Double = 0.0
    // Pink noise uses the Voss-McCartney algorithm - it remembers previous random values
    private var pinkB0: Double = 0.0
    private var pinkB1: Double = 0.0
    private var pinkB2: Double = 0.0
    private var pinkB3: Double = 0.0

    // Enum to define the types of noise
    enum class NoiseType { WHITE, PINK, BROWN }

    /**
     * Starts playing the audio. Creates the AudioTrack and launches the generation loop.
     */
    fun play() {
        // If already playing, don't start again
        if (playJob?.isActive == true) return

        // Reset algorithm states when starting fresh
        resetAlgorithmState()

        // Build the AudioTrack
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA) // Treat as music/media
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION) // Sound effect/ambient
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM) // We stream data continuously
            .build()

        audioTrack?.play()

        // Launch the infinite loop that generates noise chunks
        playJob = engineScope.launch {
            // A buffer to hold the generated samples. Short = 16-bit audio.
            val buffer = ShortArray(bufferSize)

            while (isActive) { // Keeps running until the Job is cancelled
                generateNoiseBuffer(buffer) // Fill buffer with math
                audioTrack?.write(buffer, 0, buffer.size) // Send buffer to speakers
            }
        }
    }

    /**
     * Stops the audio and releases resources.
     */
    fun stop() {
        playJob?.cancel() // Cancel the infinite loop
        audioTrack?.stop()
        audioTrack?.release() // Free up the hardware audio resources
        audioTrack = null
    }

    /**
     * Updates the volume. Called from the UI slider.
     */
    fun setVolume(volume: Float) {
        currentVolume = volume
        // AudioTrack has its own volume control (0.0f to 1.0f)
        audioTrack?.setVolume(volume)
    }

    /**
     * Updates the noise color. Called from the UI buttons.
     */
    fun setNoiseType(type: NoiseType) {
        currentNoiseType = type
        resetAlgorithmState() // Crucial! Otherwise transitioning makes a loud pop/click
    }

    /**
     * The math that fills the buffer based on the current noise type.
     */
    private fun generateNoiseBuffer(buffer: ShortArray) {
        val maxAmp = Short.MAX_VALUE.toDouble() // 32767.0

        for (i in buffer.indices) {
            // A random number between -1.0 and 1.0 (White noise base)
            val white = (Math.random() * 2.0 - 1.0)

            val sample: Double = when (currentNoiseType) {
                NoiseType.WHITE -> white // Pure random = White noise

                // Brown noise: Low-pass filtered white noise (random walk)
                NoiseType.BROWN -> {
                    // 0.02 controls how fast it moves. Lower = deeper/rumblier.
                    brownLastOut = (brownLastOut + (0.02 * white)) / 1.02
                    brownLastOut * 20.0 // Boost volume of brown noise
                }

                // Pink noise: Filtered to have equal energy per octave.
                // Simplified Voss-McCartney algorithm
                NoiseType.PINK -> {
                    pinkB0 = 0.99886 * pinkB0 + white * 0.0555179
                    pinkB1 = 0.99332 * pinkB1 + white * 0.0750759
                    pinkB2 = 0.96900 * pinkB2 + white * 0.1538520
                    pinkB3 = 0.86650 * pinkB3 + white * 0.3104856
                    // Combine them, plus a bit of direct white noise
                    (pinkB0 + pinkB1 + pinkB2 + pinkB3 + white * 0.5362) * 0.11 // 0.11 prevents clipping
                }
            }

            // Convert the -1.0 to 1.0 Double into a 16-bit Short for the audio card
            buffer[i] = (sample * maxAmp).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun resetAlgorithmState() {
        brownLastOut = 0.0
        pinkB0 = 0.0
        pinkB1 = 0.0
        pinkB2 = 0.0
        pinkB3 = 0.0
    }

    // Clean up when the ViewModel dies
    fun release() {
        stop()
        engineScope.cancel()
    }
}
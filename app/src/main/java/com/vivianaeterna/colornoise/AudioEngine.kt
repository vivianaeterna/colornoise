package com.vivianaeterna.colornoise // <-- CHANGE THIS

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*

class AudioEngine {

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private var audioTrack: AudioTrack? = null
    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var playJob: Job? = null

    private var currentNoiseType: NoiseType = NoiseType.WHITE
    private var currentVolume: Float = 0.5f
    private var currentTone: Float = 0.8f // 0.0 = Rumbly/Filtered, 1.0 = Bright/Raw

    // Algorithm states
    private var brownLastOut: Double = 0.0
    private var pinkB0: Double = 0.0
    private var pinkB1: Double = 0.0
    private var pinkB2: Double = 0.0
    private var pinkB3: Double = 0.0

    // For Blue/Violet (high-pass), we need the previous raw white noise samples
    private var prevWhite: Double = 0.0
    private var prevPrevWhite: Double = 0.0

    // For the Tone slider (simple low-pass filter state)
    private var toneLastOut: Double = 0.0

    enum class NoiseType { WHITE, PINK, BROWN, BLUE, VIOLET, GREEN, GREY }

    fun play() {
        if (playJob?.isActive == true) return
        resetAlgorithmState()

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
                generateNoiseBuffer(buffer)
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

    fun setNoiseType(type: NoiseType) {
        currentNoiseType = type
        resetAlgorithmState()
    }

    // Tone: 0.0f (Deep) to 1.0f (Bright)
    fun setTone(tone: Float) {
        currentTone = tone
    }

    private fun generateNoiseBuffer(buffer: ShortArray) {
        val maxAmp = Short.MAX_VALUE.toDouble()

        for (i in buffer.indices) {
            val white = (Math.random() * 2.0 - 1.0)

            // 1. Generate the raw base noise color
            val baseSample: Double = when (currentNoiseType) {
                NoiseType.WHITE -> white

                NoiseType.BROWN -> {
                    brownLastOut = (brownLastOut + (0.02 * white)) / 1.02
                    brownLastOut * 20.0
                }

                NoiseType.PINK -> {
                    pinkB0 = 0.99886 * pinkB0 + white * 0.0555179
                    pinkB1 = 0.99332 * pinkB1 + white * 0.0750759
                    pinkB2 = 0.96900 * pinkB2 + white * 0.1538520
                    pinkB3 = 0.86650 * pinkB3 + white * 0.3104856
                    (pinkB0 + pinkB1 + pinkB2 + pinkB3 + white * 0.5362) * 0.11
                }

                // Blue: High-pass filtered white (differentiate white)
                NoiseType.BLUE -> {
                    val blue = white - prevWhite
                    prevWhite = white
                    blue * 2.0 // Boost volume
                }

                // Violet: Even heavier high-pass (differentiate twice)
                NoiseType.VIOLET -> {
                    val violet = white - 2 * prevWhite + prevPrevWhite
                    prevPrevWhite = prevWhite
                    prevWhite = white
                    violet * 4.0 // Boost volume heavily
                }

                // Green: Mid-frequency heavy (simplified bandpass approximation)
                NoiseType.GREEN -> {
                    val pinkish = (pinkB0 + pinkB1 + pinkB2 + pinkB3 + white * 0.5362) * 0.11
                    pinkB0 = 0.99886 * pinkB0 + white * 0.0555179
                    pinkB1 = 0.99332 * pinkB1 + white * 0.0750759
                    pinkB2 = 0.96900 * pinkB2 + white * 0.1538520
                    pinkB3 = 0.86650 * pinkB3 + white * 0.3104856
                    pinkish * 1.5
                }

                // Grey: Psychoacoustic equal loudness (approximation: slightly filtered white)
                NoiseType.GREY -> {
                    brownLastOut = (brownLastOut + (0.005 * white)) / 1.005
                    (white * 0.7) + (brownLastOut * 0.3)
                }
            }

            // 2. Apply the Tone Slider (Low-Pass Filter)
            // toneCutoff goes from 0.01 (muffled) to near 1.0 (passes everything)
            val toneCutoff = 0.01 + (currentTone * 0.98)

            // Simple 1-pole low-pass filter math
            toneLastOut = toneLastOut + (toneCutoff * (baseSample - toneLastOut))

            val finalSample = toneLastOut

            // 3. Convert to 16-bit audio
            buffer[i] = (finalSample * maxAmp).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    private fun resetAlgorithmState() {
        brownLastOut = 0.0; pinkB0 = 0.0; pinkB1 = 0.0; pinkB2 = 0.0; pinkB3 = 0.0
        prevWhite = 0.0; prevPrevWhite = 0.0; toneLastOut = 0.0
    }

    fun release() {
        stop()
        engineScope.cancel()
    }
}
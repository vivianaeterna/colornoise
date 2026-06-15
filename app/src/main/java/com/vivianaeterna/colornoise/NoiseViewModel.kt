package com.vivianaeterna.colornoise

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The ViewModel survives screen rotations and holds the "Truth" of your app's state.
 * The UI reads from this, and user interactions send events to this.
 */
class NoiseViewModel : ViewModel() {

    // The AudioEngine does the heavy lifting
    private val audioEngine = AudioEngine()

    // --- UI State ---
    // We use StateFlow. It's like a live radio station. The UI listens to it.

    // Backing state (private) - only ViewModel can change it
    private val _isPlaying = MutableStateFlow(false)
    private val _volume = MutableStateFlow(0.5f) // 0.0 to 1.0
    private val _noiseType = MutableStateFlow(AudioEngine.NoiseType.WHITE)

    // Public state (read-only) - UI observes these
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    val volume: StateFlow<Float> = _volume.asStateFlow()
    val noiseType: StateFlow<AudioEngine.NoiseType> = _noiseType.asStateFlow()

    /**
     * Called by the Play/Pause button
     */
    fun togglePlayback() {
        if (_isPlaying.value) {
            audioEngine.stop()
            _isPlaying.value = false
        } else {
            audioEngine.play()
            audioEngine.setVolume(_volume.value)
            _isPlaying.value = true
        }
    }

    /**
     * Called by the Volume slider
     */
    fun updateVolume(newVolume: Float) {
        _volume.value = newVolume
        audioEngine.setVolume(newVolume)
    }

    /**
     * Called by the Color selection buttons
     */
    fun updateNoiseType(newType: AudioEngine.NoiseType) {
        _noiseType.value = newType
        audioEngine.setNoiseType(newType)
    }

    /**
     * Clean up when the ViewModel is destroyed (app closed)
     */
    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
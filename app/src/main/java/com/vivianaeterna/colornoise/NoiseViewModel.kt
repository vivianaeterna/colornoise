package com.vivianaeterna.colornoise

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

// Data class for a preset
data class Preset(
    val name: String,
    val noiseType: AudioEngine.NoiseType,
    val volume: Float,
    val tone: Float
)

class NoiseViewModel(application: Application) : AndroidViewModel(application) {

    private val audioEngine = AudioEngine()

    // --- UI State ---
    private val _isPlaying = MutableStateFlow(false)
    private val _volume = MutableStateFlow(0.5f)
    private val _noiseType = MutableStateFlow(AudioEngine.NoiseType.WHITE)
    private val _tone = MutableStateFlow(0.8f) // Default to bright
    private val _savedPresets = MutableStateFlow<List<Preset>>(emptyList())
    private val _showSaveDialog = MutableStateFlow(false)

    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    val volume: StateFlow<Float> = _volume.asStateFlow()
    val noiseType: StateFlow<AudioEngine.NoiseType> = _noiseType.asStateFlow()
    val tone: StateFlow<Float> = _tone.asStateFlow()
    val savedPresets: StateFlow<List<Preset>> = _savedPresets.asStateFlow()
    val showSaveDialog: StateFlow<Boolean> = _showSaveDialog.asStateFlow()

    // SharedPreferences for saving presets
    private val prefs = application.getSharedPreferences("NoisePresets", 0)

    init {
        loadPresets() // Load saved presets when app starts
    }

    fun togglePlayback() {
        if (_isPlaying.value) {
            audioEngine.stop()
            _isPlaying.value = false
        } else {
            audioEngine.play()
            audioEngine.setVolume(_volume.value)
            audioEngine.setTone(_tone.value)
            _isPlaying.value = true
        }
    }

    fun updateVolume(newVolume: Float) {
        _volume.value = newVolume
        audioEngine.setVolume(newVolume)
    }

    fun updateNoiseType(newType: AudioEngine.NoiseType) {
        _noiseType.value = newType
        audioEngine.setNoiseType(newType)
    }

    fun updateTone(newTone: Float) {
        _tone.value = newTone
        audioEngine.setTone(newTone)
    }

    // --- Preset Logic ---

    fun showSaveDialog() { _showSaveDialog.value = true }
    fun hideSaveDialog() { _showSaveDialog.value = false }

    fun saveCurrentPreset(name: String) {
        val newPreset = Preset(name, _noiseType.value, _volume.value, _tone.value)
        val currentList = _savedPresets.value.toMutableList()
        currentList.add(newPreset)
        _savedPresets.value = currentList
        savePresetsToDisk()
        hideSaveDialog()
    }

    fun loadPreset(preset: Preset) {
        updateNoiseType(preset.noiseType)
        updateVolume(preset.volume)
        updateTone(preset.tone)
    }

    fun deletePreset(preset: Preset) {
        val currentList = _savedPresets.value.toMutableList()
        currentList.remove(preset)
        _savedPresets.value = currentList
        savePresetsToDisk()
    }

    // Simple JSON serialization to save lists without a database
    private fun savePresetsToDisk() {
        val jsonArray = JSONArray()
        for (p in _savedPresets.value) {
            val json = JSONArray().put(p.name).put(p.noiseType.name).put(p.volume).put(p.tone)
            jsonArray.put(json)
        }
        prefs.edit().putString("presets_json", jsonArray.toString()).apply()
    }

    private fun loadPresets() {
        val jsonStr = prefs.getString("presets_json", null) ?: return
        val jsonArray = JSONArray(jsonStr)
        val list = mutableListOf<Preset>()
        for (i in 0 until jsonArray.length()) {
            val inner = jsonArray.getJSONArray(i)
            list.add(Preset(
                inner.getString(0),
                AudioEngine.NoiseType.valueOf(inner.getString(1)),
                inner.getDouble(2).toFloat(),
                inner.getDouble(3).toFloat()
            ))
        }
        _savedPresets.value = list
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
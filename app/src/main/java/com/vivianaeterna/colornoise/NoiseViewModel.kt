package com.vivianaeterna.colornoise // <-- CHANGE THIS

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

// Preset now holds a list of 10 band values instead of a NoiseType enum
data class Preset(
    val name: String,
    val bands: List<Float>
)

class NoiseViewModel(application: Application) : AndroidViewModel(application) {

    private val audioEngine = AudioEngine()

    // --- UI State ---
    private val _isPlaying = MutableStateFlow(false)
    private val _volume = MutableStateFlow(0.5f)
    private val _bands = MutableStateFlow(List(10) { 1.0f }) // Default to 1.0 (White Noise flat line)
    private val _savedPresets = MutableStateFlow<List<Preset>>(emptyList())
    private val _showSaveDialog = MutableStateFlow(false)

    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    val volume: StateFlow<Float> = _volume.asStateFlow()
    val bands: StateFlow<List<Float>> = _bands.asStateFlow()
    val savedPresets: StateFlow<List<Preset>> = _savedPresets.asStateFlow()
    val showSaveDialog: StateFlow<Boolean> = _showSaveDialog.asStateFlow()

    private val prefs = application.getSharedPreferences("NoisePresets", 0)

    init {
        loadPresets()
    }

    fun togglePlayback() {
        if (_isPlaying.value) {
            audioEngine.stop()
            _isPlaying.value = false
        } else {
            audioEngine.play()
            audioEngine.setVolume(_volume.value)
            audioEngine.updateBands(_bands.value) // Push current bands to engine
            _isPlaying.value = true
        }
    }

    fun updateVolume(newVolume: Float) {
        _volume.value = newVolume
        audioEngine.setVolume(newVolume)
    }

    // Called when a single slider is moved
    fun updateBand(index: Int, value: Float) {
        val currentBands = _bands.value.toMutableList()
        currentBands[index] = value
        _bands.value = currentBands
        audioEngine.updateBands(_bands.value)
    }

    // Called when a Color Preset Chip is clicked
    fun applyColorPreset(presetBands: List<Float>) {
        _bands.value = presetBands
        audioEngine.updateBands(_bands.value)
    }

    // --- Preset Logic ---
    fun showSaveDialog() { _showSaveDialog.value = true }
    fun hideSaveDialog() { _showSaveDialog.value = false }

    fun saveCurrentPreset(name: String) {
        val newPreset = Preset(name, _bands.value)
        val currentList = _savedPresets.value.toMutableList()
        currentList.add(newPreset)
        _savedPresets.value = currentList
        savePresetsToDisk()
        hideSaveDialog()
    }

    fun loadPreset(preset: Preset) {
        applyColorPreset(preset.bands)
        // Approximate volume based on average of bands (optional, or just keep current volume)
    }

    fun deletePreset(preset: Preset) {
        val currentList = _savedPresets.value.toMutableList()
        currentList.remove(preset)
        _savedPresets.value = currentList
        savePresetsToDisk()
    }

    private fun savePresetsToDisk() {
        val jsonArray = JSONArray()
        for (p in _savedPresets.value) {
            val jsonBands = JSONArray()
            p.bands.forEach { jsonBands.put(it.toDouble()) } // JSON puts doubles
            val json = JSONArray().put(p.name).put(jsonBands)
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
            val name = inner.getString(0)
            val jsonBands = inner.getJSONArray(1)
            val bands = mutableListOf<Float>()
            for (j in 0 until jsonBands.length()) {
                bands.add(jsonBands.getDouble(j).toFloat())
            }
            list.add(Preset(name, bands))
        }
        _savedPresets.value = list
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
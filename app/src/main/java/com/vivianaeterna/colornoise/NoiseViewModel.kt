package com.vivianaeterna.colornoise // <-- CHANGE THIS

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class NoiseViewModel(application: Application) : AndroidViewModel(application) {

    private val _volume = MutableStateFlow(0.5f)
    private val _bands = MutableStateFlow(List(10) { 0.5f })
    private val _savedPresets = MutableStateFlow<List<Preset>>(emptyList())
    private val _showSaveDialog = MutableStateFlow(false)

    val volume: StateFlow<Float> = _volume.asStateFlow()
    val bands: StateFlow<List<Float>> = _bands.asStateFlow()
    val savedPresets: StateFlow<List<Preset>> = _savedPresets.asStateFlow()
    val showSaveDialog: StateFlow<Boolean> = _showSaveDialog.asStateFlow()

    private val prefs = application.getSharedPreferences("NoisePresets", 0)

    init { loadPresets() }

    fun togglePlayback(context: Context, permissionLauncher: ManagedActivityResultLauncher<String, Boolean>) {
        if (NoiseAppState.isPlaying.value) {
            AudioEngine.stop()
            context.stopService(Intent(context, NoiseService::class.java))
        } else {
            // Check for Notification Permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionCheck = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    startServiceAndPlay(context)
                } else {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                startServiceAndPlay(context)
            }
        }
    }

    private fun startServiceAndPlay(context: Context) {
        AudioEngine.play()
        AudioEngine.setVolume(_volume.value)
        AudioEngine.updateBands(_bands.value)
        context.startService(Intent(context, NoiseService::class.java))
    }

    fun updateVolume(newVolume: Float) {
        _volume.value = newVolume
        AudioEngine.setVolume(newVolume)
    }

    fun updateBand(index: Int, value: Float) {
        val currentBands = _bands.value.toMutableList()
        currentBands[index] = value
        _bands.value = currentBands
        AudioEngine.updateBands(_bands.value)
    }

    fun applyColorPreset(name: String, presetBands: List<Float>) {
        _bands.value = presetBands
        NoiseAppState.setPresetName(name)
        AudioEngine.updateBands(_bands.value)
    }

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
        applyColorPreset(preset.name, preset.bands)
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
            p.bands.forEach { jsonBands.put(it.toDouble()) }
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
            for (j in 0 until jsonBands.length()) { bands.add(jsonBands.getDouble(j).toFloat()) }
            list.add(Preset(name, bands))
        }
        _savedPresets.value = list
    }

    override fun onCleared() {
        super.onCleared()
        // We don't release AudioEngine here anymore, the Service handles it!
    }
}
package com.vivianaeterna.colornoise // <-- CHANGE THIS

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NoiseApp()
                }
            }
        }
    }
}

@Composable
fun NoiseApp(viewModel: NoiseViewModel = viewModel()) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val currentNoiseType by viewModel.noiseType.collectAsState()
    val tone by viewModel.tone.collectAsState()
    val savedPresets by viewModel.savedPresets.collectAsState()
    val showSaveDialog by viewModel.showSaveDialog.collectAsState()

    // Save Dialog State
    if (showSaveDialog) {
        SavePresetDialog(
            onDismiss = { viewModel.hideSaveDialog() },
            onSave = { name -> viewModel.saveCurrentPreset(name) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Color Noise", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 16.dp, bottom = 16.dp))

        // --- Color Selection Grid ---
        // FlowRow automatically wraps items to the next line if they don't fit
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AudioEngine.NoiseType.entries.forEach { type ->
                FilterChip(
                    selected = (type == currentNoiseType),
                    onClick = { viewModel.updateNoiseType(type) },
                    label = { Text(type.name) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- Frequency / Tone Slider ---
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Frequency Tone", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Low", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = tone,
                    onValueChange = { viewModel.updateTone(it) },
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Text("High", style = MaterialTheme.typography.bodySmall)
            }
        }

        // --- Volume Slider ---
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Volume: ${(volume * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = volume,
                onValueChange = { viewModel.updateVolume(it) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))

        // --- Controls Row (Play & Save) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { viewModel.togglePlayback() },
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isPlaying) "STOP" else "PLAY")
            }
            Spacer(Modifier.width(16.dp))
            OutlinedButton(
                onClick = { viewModel.showSaveDialog() },
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text("SAVE")
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        // --- Saved Presets List ---
        Text("Custom Presets", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)

        // LazyColumn is like RecyclerView - it efficiently renders lists
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (savedPresets.isEmpty()) {
                item { Text("No saved presets yet.", modifier = Modifier.padding(top = 16.dp)) }
            }
            items(savedPresets) { preset ->
                SavedPresetRow(
                    preset = preset,
                    onLoad = { viewModel.loadPreset(preset) },
                    onDelete = { viewModel.deletePreset(preset) }
                )
            }
        }
    }
}

@Composable
fun SavePresetDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Preset") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Preset Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { if(text.isNotBlank()) onSave(text) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SavedPresetRow(preset: Preset, onLoad: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(preset.name, style = MaterialTheme.typography.titleSmall)
                Text("${preset.noiseType.name} • Vol ${(preset.volume*100).toInt()}% • Tone ${(preset.tone*100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onLoad) { Text("Load") }
            IconButton(onClick = onDelete) {
                Text("✕", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
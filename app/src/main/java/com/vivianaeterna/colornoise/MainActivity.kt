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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
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

object ColorPresets {
    // Flat: Target energy sum = 5.0 (0.5 * 10 bands)
    val White = List(10) { 0.5f }

    // -3dB / octave: Gradual steady drop (Sum ≈ 5.5)
    val Pink = listOf(1.0f, 0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f, 0.2f, 0.1f)

    // -6dB / octave: Sharp drop, but boosted in the lows to maintain volume (Sum ≈ 5.6)
    val Brown = listOf(1.0f, 1.0f, 0.8f, 0.6f, 0.4f, 0.3f, 0.2f, 0.15f, 0.1f, 0.05f)

    // +3dB / octave: Inverse of Pink (Sum ≈ 5.5)
    val Blue = listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f)

    // +6dB / octave: Inverse of Brown (Sum ≈ 5.6)
    val Violet = listOf(0.05f, 0.1f, 0.15f, 0.2f, 0.3f, 0.4f, 0.6f, 0.8f, 1.0f, 1.0f)

    // Mid-frequency bell curve (Sum ≈ 4.85)
    val Green = listOf(0.15f, 0.4f, 0.8f, 1.0f, 1.0f, 0.8f, 0.4f, 0.15f, 0.1f, 0.05f)

    // Equal loudness contour: Dips where the ear is sensitive (mids), boosts lows/highs (Sum ≈ 4.95)
    val Grey = listOf(0.85f, 0.65f, 0.35f, 0.15f, 0.1f, 0.15f, 0.35f, 0.65f, 0.85f, 0.85f)
}

@Composable
fun NoiseApp(viewModel: NoiseViewModel = viewModel()) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val bands by viewModel.bands.collectAsState()
    val savedPresets by viewModel.savedPresets.collectAsState()
    val showSaveDialog by viewModel.showSaveDialog.collectAsState()

    if (showSaveDialog) {
        SavePresetDialog(
            onDismiss = { viewModel.hideSaveDialog() },
            onSave = { name -> viewModel.saveCurrentPreset(name) }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Color Noise EQ", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top=16.dp, bottom=16.dp))

        // --- Color Preset Buttons ---
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val colorMap = mapOf(
                "White" to ColorPresets.White, "Pink" to ColorPresets.Pink,
                "Brown" to ColorPresets.Brown, "Blue" to ColorPresets.Blue,
                "Violet" to ColorPresets.Violet, "Green" to ColorPresets.Green,
                "Grey" to ColorPresets.Grey
            )
            colorMap.forEach { (name, preset) ->
                OutlinedButton(
                    onClick = { viewModel.applyColorPreset(preset) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(name, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- 10-Band Vertical Equalizer ---
        val bandColors = listOf(
            Color(0xFF8B4513), Color.Red, Color(0xFFFF8C00), Color.Yellow,
            Color.Green, Color(0xFF90EE90), Color.Cyan, Color(0xFF87CEEB),
            Color.Blue, Color(0xFF8A2BE2)
        )

        // 0.3f takes up 30% of the screen height
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.3f), // <--- Your 0.3f request
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            bands.forEachIndexed { index, value ->
                VerticalSlider(
                    value = value,
                    onValueChange = { viewModel.updateBand(index, it) },
                    color = bandColors[index],
                    // weight(1f) spaces them evenly, fillMaxHeight() stretches them to the Row
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- Volume Slider ---
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Master Volume: ${(volume * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
            Slider(value = volume, onValueChange = { viewModel.updateVolume(it) }, modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(16.dp))

        // --- Controls Row ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { viewModel.togglePlayback() },
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            ) { Text(if (isPlaying) "STOP" else "PLAY") }

            Spacer(Modifier.width(16.dp))
            OutlinedButton(onClick = { viewModel.showSaveDialog() }, modifier = Modifier.weight(1f).height(50.dp)) { Text("SAVE EQ") }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        // --- Saved Presets List ---
        Text("Custom Presets", style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (savedPresets.isEmpty()) {
                item { Text("No saved presets yet.", modifier = Modifier.padding(top = 16.dp)) }
            }
            items(savedPresets) { preset ->
                SavedPresetRow(preset = preset, onLoad = { viewModel.loadPreset(preset) }, onDelete = { viewModel.deletePreset(preset) })
            }
        }
    }
}

/**
 * A properly constrained Vertical Slider.
 * Uses Modifier.layout to swap width/height constraints so the slider
 * actually stretches to fill its parent's height.
 */
@Composable
fun VerticalSlider(value: Float, onValueChange: (Float) -> Unit, color: Color, modifier: Modifier = Modifier) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .layout { measurable, constraints ->
                // 1. Swap the width and height constraints
                val swappedConstraints = Constraints(
                    minWidth = constraints.minHeight,
                    maxWidth = constraints.maxHeight, // Row's height becomes Slider's max width!
                    minHeight = constraints.minWidth,
                    maxHeight = constraints.maxWidth
                )

                // 2. Measure the slider with the swapped constraints
                val placeable = measurable.measure(swappedConstraints)

                // 3. Report the swapped dimensions back to the parent layout
                layout(placeable.height, placeable.width) {
                    // 4. Center the horizontal slider inside the vertical box
                    placeable.place(
                        x = -(placeable.width - placeable.height) / 2,
                        y = (placeable.width - placeable.height) / 2
                    )
                }
            }
            .graphicsLayer {
                // 5. Finally, rotate the visual drawing
                rotationZ = -90f
            },
        colors = SliderDefaults.colors(
            thumbColor = color,
            activeTrackColor = color.copy(alpha = 0.8f),
            inactiveTrackColor = color.copy(alpha = 0.2f)
        )
    )
}

@Composable
fun SavePresetDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Preset") },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Preset Name") }, singleLine = true) },
        confirmButton = { Button(onClick = { if(text.isNotBlank()) onSave(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SavedPresetRow(preset: Preset, onLoad: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(preset.name, style = MaterialTheme.typography.titleSmall)
                Text("Custom EQ Profile", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onLoad) { Text("Load") }
            IconButton(onClick = onDelete) { Text("✕", color = MaterialTheme.colorScheme.error) }
        }
    }
}
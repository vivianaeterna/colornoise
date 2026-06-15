package com.vivianaeterna.colornoise // <-- CHANGE THIS to match your project's package!

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // MaterialTheme defines the look and feel (colors, fonts)
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Call our main app composable
                    NoiseApp()
                }
            }
        }
    }
}

@Composable
fun NoiseApp(viewModel: NoiseViewModel = viewModel()) { // viewModel() automatically creates/survives rotation

    // Collect the state from the ViewModel.
    // Whenever these change in the ViewModel, this UI function will re-run automatically.
    val isPlaying by viewModel.isPlaying.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val currentNoiseType by viewModel.noiseType.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly // Spread items top to bottom
    ) {
        // --- Title ---
        Text(
            text = "Color Noise",
            style = MaterialTheme.typography.headlineLarge
        )

        // --- Noise Color Buttons ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NoiseButton("White", AudioEngine.NoiseType.WHITE, currentNoiseType) { viewModel.updateNoiseType(it) }
            NoiseButton("Pink", AudioEngine.NoiseType.PINK, currentNoiseType) { viewModel.updateNoiseType(it) }
            NoiseButton("Brown", AudioEngine.NoiseType.BROWN, currentNoiseType) { viewModel.updateNoiseType(it) }
        }

        // --- Volume Slider ---
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Volume: ${(volume * 100).toInt()}%")
            Slider(
                value = volume,
                onValueChange = { viewModel.updateVolume(it) },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        // --- Play/Pause Button ---
        Button(
            onClick = { viewModel.togglePlayback() },
            modifier = Modifier.size(width = 200.dp, height = 60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (isPlaying) "STOP" else "PLAY", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

/**
 * A reusable composable for the noise type buttons.
 * It highlights if it is the currently selected noise type.
 */
@Composable
fun NoiseButton(
    label: String,
    type: AudioEngine.NoiseType,
    currentType: AudioEngine.NoiseType,
    onClick: (AudioEngine.NoiseType) -> Unit
) {
    val isSelected = (type == currentType)

    OutlinedButton(
        onClick = { onClick(type) },
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        )
    ) {
        Text(text = label)
    }
}
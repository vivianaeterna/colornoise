# 🎧 ColorNoise

A modern, native Android color noise generator designed for relaxation, focus, and sleep. Inspired by apps like ChromaDoze, but rebuilt from the ground up with a sleek UI, dynamic 10-band equalizer, and robust background playback.

## ✨ Features
- 10-Band Graphic Equalizer: Fine-tune your noise with logarithmically spaced vertical sliders, ranging from deep 60Hz rumbles to crisp 18kHz highs.
- Accoustic Presets: Mathematically accurate color noise profiles:
  - White: Equal energy per Hz (Flat)
  - Pink: -3dB per octave (Steady drop)
  - Brown: -6dB per octave (Deep rumble)
  - Blue & Violet: +3dB/+6dB per octave (High hiss)
  - Green: Mid-frequency bell curve
  - Grey: Psychoacoustic equal loudness contour (Dips where the ear is sensitive)
- Custom Presets: Save and load your own custom EQ profiles.
- Persistent Notification: Keep the noise playing while using other apps, with full play/pause and preset-skipping controls right from the notification or lock screen.
- Modern UI: Built with Jetpack Compose, featuring dynamic theming that automatically matches your device's Light/Dark mode.
- Volume Normalization: Presets are balanced so switching between White and Brown noise doesn't cause jarring volume jumps.

## 📲 Download

1.Go to the Releases page (Update this link!).
1.Download the latest .apk file.
1.On your Android phone, open the downloaded file. You may need to allow "Install from unknown sources" for your browser or file manager.

## 🛠️ Tech Stack

- Language: Kotlin
- UI: Jetpack Compose & Material 3
- Audio Engine: Android AudioTrack with real-time crossover filter network (1-pole low-pass math).
- Architecture: MVVM (ViewModel + StateFlow)
- Concurrency: Kotlin Coroutines for buffer streaming and state observation.
- Background Audio: Foreground Service with MediaSession for notification controls.

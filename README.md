# SojaKothy Music 🎵

A YouTube music player for Android - like NewPipe but audio-only edition.

## Features
- 🔍 Search YouTube music (no API key needed)
- ▶️ Background audio playback
- 📥 Download songs for offline use
- ❤️ Favorite songs
- 🔄 Repeat / Shuffle modes
- 🎨 Dark theme with mini player

## Build via GitHub Actions (Recommended)

1. Create a new GitHub repository
2. Push all files to the `main` branch
3. Go to **Actions** tab → the workflow runs automatically
4. After ~5-10 minutes, download the APK from **Artifacts**

## Local Build Requirements
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34

```bash
chmod +x gradlew
./gradlew assembleDebug
```
The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

## Architecture
- **Language:** Kotlin
- **DI:** Hilt
- **Database:** Room
- **Player:** ExoPlayer / Media3
- **YouTube:** NewPipe Extractor (no API key)
- **Downloads:** WorkManager
- **Architecture:** MVVM + StateFlow

## No API Key Needed
Uses [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor) to
fetch YouTube content directly — no Google API key required.

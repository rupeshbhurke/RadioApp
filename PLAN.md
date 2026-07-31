# Implementation Plan: Android Internet Radio App

## 1. Environment & Prerequisites
Based on the initial inspection, the following are required to compile the app:
- A working JDK 17 (Java is currently not in PATH or broken).
- Android SDK Command-Line Tools (sdkmanager).
- Properly set `JAVA_HOME` and `ANDROID_HOME` environment variables.

## 2. Architecture
- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose (Material 3)
- **Architecture**: MVVM with Unidirectional Data Flow, Repository Pattern for data access.
- **Media Playback**: Jetpack Media3 ExoPlayer with `MediaSessionService`.
- **Local Storage**: Room (Favourites, Recent), DataStore (Preferences).
- **Minimum SDK**: API 26 (Android 8.0)

## 3. Milestones

### Milestone 1: Project Initialization & Basic UI
- Setup base Android project with Compose and Gradle Kotlin DSL.
- Create Navigation scaffolding (Home, Browse, Favourites, Settings).
- Create a sample JSON catalogue in `assets/` and a `StationRepository` to read it.

### Milestone 2: Media Playback & Foreground Service
- Add Jetpack Media3 dependencies.
- Implement `MediaSessionService` for background playback.
- Implement `PlayerController` and update the UI with a persistent mini-player.
- Verify playback continues in the background and lock screen.

### Milestone 3: Local Persistence
- Integrate Room database for Favourites and Recently Played.
- Integrate DataStore for user preferences.

### Milestone 4: Browse, Search & Refinements
- Implement Browse filtering and local search.
- Handle error states gracefully (stream timeout, invalid URL).
- Add settings page and refine UI.

## 4. Verification
Run `./gradlew lint test assembleDebug` to verify compilation, tests, and to generate the debug APK.

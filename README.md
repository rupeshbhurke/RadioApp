# RadioApp

A native Android internet radio application built with Jetpack Compose. 
RadioApp allows users to search, favorite, and stream thousands of internet radio stations globally using the [Radio Browser API](https://www.radio-browser.info/).

## Features
- **Global Radio Search:** Browse and search tens of thousands of radio stations dynamically fetched from the Radio Browser API.
- **Smart Search State:** Search queries and filters are preserved seamlessly while navigating between tabs.
- **Background Sync:** Seamlessly fetches thousands of stations in the background without locking up the UI.
- **Favorites:** Save your favorite stations for quick access. 
- **Cloud Backup:** Fully integrated with Android Auto Backup to persist your favorites across app reinstalls and device switches.
- **Auto-Play:** Automatically starts playing the last played station on launch.
- **Dynamic Theming:** Supports System Default, Light, and Dark modes.
- **Customizable Layout:** Adjust grid layout from 1-column list views to 5-column grid views.
- **Mini Player:** Sticky bottom player controls across all tabs, featuring marquee scrolling for long station names and a quick-action favorite toggle.
- **Media Session Integration:** Fully integrates with Android's MediaSession for lock-screen controls, Bluetooth metadata, and headset button support.
- **Sleep Timer:** Automatically stops playback and exits the app after a set duration.

## Tech Stack
- Kotlin
- Jetpack Compose
- Room Database (Offline Caching)
- Retrofit & Gson (Networking)
- ExoPlayer (Media3)
- MediaSessionService
- DataStore (Preferences)
- Coil (Image Loading)

## Build & Run
1. Clone the repository.
2. Open the project in Android Studio or run via command line:
   ```bash
   ./gradlew assembleDebug
   ```
3. Install the APK on your device or emulator.

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

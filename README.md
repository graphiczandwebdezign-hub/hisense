# Hisense Vidaa TV Remote — Android App

A native Android (Kotlin/Jetpack Compose) remote control app for Hisense Vidaa Smart TVs. Communicates over WiFi using the TV's built-in MQTT broker on **port 36669**.

## Features

- **Full remote control** — D-Pad, number pad, volume/channel rockers, transport controls
- **QWERTY Keyboard** — On-screen keyboard for typing searches on your TV
- **Voice Search** — Speak your search query and it auto-types on the TV
- **TV Discovery** — Automatically find Hisense TVs on your network
- **App Shortcuts** — One-tap Netflix, YouTube, Prime Video, Disney+
- **Color Buttons** — Red, Green, Yellow, Blue
- **Quick Text Bar** — Type and send text instantly
- **Pairing Flow** — Enter the 4-digit code shown on your TV
- **Dark Theme** — Beautiful dark glass UI

## How to Open in Android Studio

### 1. Open the project

```bash
# Launch Android Studio
# File → Open → Navigate to HisenseRemote/ → OK
```

### 2. Let Gradle sync

Android Studio will automatically download dependencies and sync the project. Wait for the "Build Sync" to finish.

### 3. Build the APK

```bash
# In Android Studio menu:
Build → Build Bundle(s) / APK(s) → Build APK(s)

# Or via command line:
cd HisenseRemote
./gradlew assembleDebug
```

### 4. Find your APK

```
app/build/outputs/apk/debug/app-debug.apk
```

### 5. Install

- Transfer the APK to your Android phone
- Enable "Install from unknown sources" in Settings
- Tap the APK to install

## Usage

1. Make sure your phone is **on the same WiFi** as your Hisense TV
2. Open the **Hisense Remote** app
3. Tap **Discover** to auto-find your TV, or enter the IP manually
4. Tap **Connect**
5. If a 4-digit code appears on your TV, enter it and tap **Pair**
6. Enjoy the full remote!

## Project Structure

```
HisenseRemote/
├── app/
│   ├── build.gradle.kts          # Dependencies (MQTT, Compose, etc.)
│   ├── src/main/
│   │   ├── AndroidManifest.xml    # Permissions & activity
│   │   ├── java/com/hisense/remote/
│   │   │   ├── MainActivity.kt    # Entry point
│   │   │   ├── model/
│   │   │   │   └── TvModels.kt    # Data models & key codes
│   │   │   ├── service/
│   │   │   │   ├── MqttService.kt          # MQTT communication
│   │   │   │   └── TvDiscoveryService.kt   # Network discovery
│   │   │   └── ui/
│   │   │       ├── theme/
│   │   │       │   └── Theme.kt            # Dark theme
│   │   │       ├── screens/
│   │   │       │   ├── MainScreen.kt       # All UI composables
│   │   │       │   └── RemoteViewModel.kt  # State management
│   │   └── res/
│   │       ├── values/themes.xml
│   │       └── xml/network_security_config.xml
├── build.gradle.kts              # Root build config
├── settings.gradle.kts           # Project settings
├── gradle.properties             # Gradle properties
└── gradlew                       # Gradle wrapper
```

## Permissions

| Permission | Why |
|---|---|
| `INTERNET` | Connect to TV's MQTT broker on port 36669 |
| `ACCESS_WIFI_STATE` | Get local IP for TV discovery |
| `RECORD_AUDIO` | Voice search |
| `WAKE_LOCK` | Keep MQTT connection alive |

## Technical Details

- **Protocol**: MQTT over TLS on port 36669
- **Authentication**: `hisenseservice` / `multimqttservice`
- **TV Discovery**: Scans subnet for open port 36669
- **Text Input**: Sends characters as individual key-press MQTT messages
- **Voice**: Uses Android's built-in `RecognizerIntent` for speech-to-text

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- Gradle 8.5
- JDK 17
- Hisense Vidaa Smart TV on the same WiFi network

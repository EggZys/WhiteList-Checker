# WhiteList Checker

<p align="center">
  <img src="https://img.shields.io/badge/Android-14%2B-green?style=for-the-badge&logo=android" alt="Android 14+">
  <img src="https://img.shields.io/badge/Kotlin-1.9-blue?style=for-the-badge&logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/License-MIT-purple?style=for-the-badge" alt="License">
</p>

<p align="center">
  <b>Cyberpunk-styled Android app for monitoring internet connectivity and censorship restrictions</b>
</p>

---

## Overview

WhiteList Checker is a real-time network monitoring application that detects the level of internet censorship in your region. It continuously scans multiple URL groups to determine if you have full access, Russia-only access, or are limited to the RKN whitelist.

## Features

- **Real-time monitoring** - background service checks connectivity every 90 seconds
- **Smart detection** - identifies 4 network states:
  - `FULL ACCESS` - all global and local resources available
  - `RU ONLY` - only Russian websites accessible
  - `RKN LOCKDOWN` - only government whitelist sites available
  - `NO SIGNAL` - no internet connection
- **Persistent notification** - status bar shows current connectivity state
- **Alert notifications** - instant alerts when network state changes
- **Custom URL configuration** - add your own URLs to each category
- **Cyberpunk UI** - dark neon-themed interface with smooth animations

## Screenshots

<p align="center">
  <i><img width="576" height="1280" alt="screenshot" src="https://github.com/user-attachments/assets/df8f0308-e2e1-43f5-b5d2-bbc86f1afcc5"/>
</i>
</p>

## Architecture

```
app/src/main/java/com/eggzys/internetmonitor/
├── MainActivity.kt          # UI with cyberpunk theme and animations
├── MonitorService.kt        # Foreground service for background monitoring
├── InternetStateChecker.kt  # Network state detection logic
├── NotificationHelper.kt    # Notification management
├── InternetState.kt         # State enum with colors and descriptions
└── UrlGroups.kt             # URL group configuration
```

## Tech Stack

- **Language**: Kotlin
- **UI**: Material3 + Custom drawables
- **Networking**: OkHttp 4.12
- **Coroutines**: Kotlin Coroutines for async operations
- **Architecture**: Service-based background monitoring

## URL Groups

The app checks three categories of URLs:

| Category | Default URLs | Purpose |
|----------|-------------|---------|
| Global | google.com, github.com, cloudflare.com | Tests global internet access |
| Russia | kp40.ru, rbc.ru, 1tv.ru | Tests Russian website access |
| RKN Whitelist | dzen.ru, gosuslugi.ru, vk.com | Tests government whitelist |

## Build

```bash
# Clone repository
git clone https://github.com/EggZys/WhiteList-Checker.git
cd WhiteList-Checker

# Build debug APK
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- Minimum device: Android 8.0 (API 26)

## Permissions

| Permission | Purpose |
|------------|---------|
| `INTERNET` | Network connectivity checks |
| `ACCESS_NETWORK_STATE` | Network state detection |
| `FOREGROUND_SERVICE` | Background monitoring |
| `POST_NOTIFICATIONS` | Status notifications |

## How It Works

1. User presses **ENGAGE** to start monitoring
2. Foreground service launches with persistent notification
3. Every 90 seconds, the app checks all URL groups in parallel
4. Based on which groups respond, it determines the network state
5. If state changes, an alert notification is sent
6. UI updates with color-coded status and animations

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  <sub>Built with Kotlin and Material3</sub>
</p>

# MediaDock — Native Android Client

MediaDock is an all-in-one media downloader for Android built with modern Kotlin, Jetpack Compose, Material 3, and Clean Architecture.

---

## Technical Specifications

| Category | Specification |
|---|---|
| **Language** | Kotlin 1.9.22 |
| **Minimum SDK** | Android 8.0 (API 26) |
| **Target & Compile SDK** | Android 14 (API 34) |
| **UI Framework** | Jetpack Compose (BOM 2024.02.01) |
| **Design System** | Material 3 (Teal/Cyan Dynamic Theme, Light & Dark) |
| **Dependency Injection** | Hilt 2.50 |
| **Architecture** | Clean Architecture + MVVM + Unidirectional Data Flow |
| **Local Database** | Room 2.6.1 (`DownloadDao`, `HistoryDao`) |
| **Preferences** | Jetpack DataStore Preferences |
| **Networking** | Retrofit 2.9.0 + OkHttp 4.12.0 + SSE (Server-Sent Events) |
| **Image Loading** | Coil Compose 2.5.0 |
| **Background Execution** | Foreground Service (`dataSync`) + Android Notifications |
| **File Management** | MediaStore API + Storage Access Framework (SAF) |
| **Security** | Android Keystore + EncryptedSharedPreferences (Zero hardcoded secrets) |

---

## Directory Structure

```
android/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/mediadock/app/
│       │   │   ├── MediaDockApp.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── core/
│       │   │   │   ├── common/         # Resource, Dispatchers, FormattingUtils
│       │   │   │   ├── designsystem/   # Material 3 Theme, Typography, Reusable Components
│       │   │   │   ├── network/        # Retrofit, OkHttp, AuthInterceptor, SSE EventSource
│       │   │   │   ├── database/       # Room Database, DAOs, Entities
│       │   │   │   ├── datastore/      # Preferences DataStore
│       │   │   │   ├── security/       # SecureTokenManager (EncryptedSharedPreferences)
│       │   │   │   ├── storage/        # StorageManager (MediaStore + SAF)
│       │   │   │   └── downloads/      # DownloadTransferEngine, ForegroundTransferService
│       │   │   ├── di/                 # NetworkModule, DatabaseModule, RepositoryModule
│       │   │   ├── domain/             # Models, Repositories, Use Cases
│       │   │   ├── data/               # DTOs, Repository Implementations
│       │   │   └── feature/            # Home, MediaInspect, Downloads, History, Settings
│       │   └── res/
│       │       ├── values/             # English strings, colors, styles
│       │       └── values-bn/          # Bengali (বাংলা) localization
│       └── test/                       # Unit tests (JUnit 4, MockK, Coroutines Test)
├── gradle/wrapper/                     # Gradle wrapper
├── build.gradle.kts                    # Root build script
├── settings.gradle.kts                 # Project settings
└── local.properties                    # SDK location
```

---

## Building from Command Line

To build the project locally using Gradle:

```bash
# Set environment variables
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot"
set "ANDROID_HOME=D:\Android\Sdk"

# Run unit tests
./gradlew test

# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease

# Build Google Play App Bundle (AAB)
./gradlew bundleRelease
```

### Build Artifact Locations
* **Debug APK:** `android/app/build/outputs/apk/debug/app-debug.apk`
* **Release APK:** `android/app/build/outputs/apk/release/app-release.apk`
* **Release AAB:** `android/app/build/outputs/bundle/release/app-release.aab`

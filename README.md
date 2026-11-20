# MediaDock — All-in-One Android Media Downloader

[![Download Latest APK](https://img.shields.io/badge/Download-MediaDock%20v1.4.0%20APK-FE5223?style=for-the-badge&logo=android&logoColor=white)](https://github.com/sync-jisan-dev/media-dock/releases/download/v1.4.0/MediaDock-release.apk)
[![Latest Release](https://img.shields.io/github/v/release/sync-jisan-dev/media-dock?color=FE5223&label=Release)](https://github.com/sync-jisan-dev/media-dock/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Target: Android 14](https://img.shields.io/badge/Android%20SDK-API%2034-green.svg)](android/)

MediaDock is an enterprise-grade, open-source universal media downloader and Chrome-grade browser for Android. Built with Jetpack Compose, Kotlin Coroutines, and Clean Architecture, MediaDock enables users to paste or share URLs from major video platforms, social networks, and websites to analyze, select, convert, and download media directly into local storage.

---

## 🚀 Quick Download

Direct production builds are available under [GitHub Releases](https://github.com/sync-jisan-dev/media-dock/releases/latest):

* **[Download MediaDock v1.4.0 Production APK (14.0 MB)](https://github.com/sync-jisan-dev/media-dock/releases/download/v1.4.0/MediaDock-release.apk)**
* **[Download MediaDock v1.4.0 Debug APK (18.6 MB)](https://github.com/sync-jisan-dev/media-dock/releases/download/v1.4.0/MediaDock-debug.apk)**

---

---

## Key Features

1. **Video Downloading:** Supports 144p up to 4K Ultra HD (2160p) across MP4, MKV, and WEBM containers with automatic audio merging via FFmpeg.
2. **Audio Extraction:** High-bitrate audio extraction (MP3 320 kbps, M4A, Opus, WAV, FLAC) with automatic ID3 tag & album artwork embedding.
3. **Professional Multi-Engine Extraction:** Uses yt-dlp as the primary quality engine, gallery-dl for specialist galleries and collections, a TikTok embed recovery path, and an optional operator-owned Cobalt instance as a final provider fallback.
4. **Webpage Image Extraction:** Extracts high-resolution images through gallery-dl first, then falls back to Open Graph metadata, Twitter Cards, JSON-LD, `<img>` tags, and responsive `srcset` sources with multi-selection and batch download.
5. **Video-to-GIF Conversion:** Interactive start/end time trimming to transcode video segments into animated GIFs.
6. **Real-Time Progress:** Live Server-Sent Events (SSE) tracking backend transcoding speed and ETA, coupled with streaming chunk download directly to device storage.
7. **Native Android Integration:** Deep integration with Android's Share Sheet (`ACTION_SEND`), Scoped Storage (`MediaStore` API), and Storage Access Framework (SAF).
8. **Zero-Secret Mobile Security:** Zero embedded credentials in the APK; secure, server-signed guest sessions backed by Android Keystore.
9. **Offline-First Management:** Persistent download history via Room Database with fast search, filtering, and direct file open/share actions.
10. **MediaDock 2.0 Design:** A purpose-built indigo/aqua design system with adaptive light/dark themes, floating navigation, accessible contrast, and focused task hierarchy.

---

## System Architecture

```
[ Android Client ]
  ├─ Jetpack Compose (Material 3 Theme)
  ├─ MVVM ViewModels + Coroutines Flow
  ├─ Clean Architecture Domain Use Cases
  ├─ Retrofit 2 + OkHttp (SSE Events)
  ├─ Foreground Transfer Service + Scoped Storage
  └─ Room 2.6.1 Local Database (Offline History)
            │
      HTTPS / TLS 1.3
            │
[ Backend Services (Multi-Engine Orchestrator) ]
  ├─ NestJS 11 Gateway (/app/v1)
  ├─ Guest Session Engine (HMAC-SHA256 Token)
  ├─ yt-dlp Primary Quality Engine + FFmpeg Worker
  ├─ gallery-dl Gallery / Collection Extractor
  ├─ Cobalt Client (Optional Self-Hosted Fallback)
  ├─ TikTok Embed + SSRF-Guarded Cheerio Fallbacks
  ├─ Redis 7 (Queue & State Event Bus)
  └─ PostgreSQL 16 (Prisma ORM)
```

### Optional Cobalt provider

Cobalt's hosted API is not intended for use by third-party applications. MediaDock therefore never calls `api.cobalt.tools`. To enable the fallback, run your own protected Cobalt instance and configure:

```env
COBALT_API_URL=https://your-cobalt-instance.example
COBALT_API_KEY=your-instance-api-key
```

The production MediaDock image includes gallery-dl as a separate command-line engine. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for its license and upstream source.

---

## Project Structure

```
project-root/
│
├── android/                        # Complete Android Studio project
│   ├── app/                        # App module with Jetpack Compose & Hilt
│   │   ├── src/main/java/          # Clean Architecture layers (core, domain, data, feature)
│   │   ├── src/main/res/           # Material 3 strings, themes, adaptive icons
│   │   └── src/test/               # Unit tests (JUnit 4, MockK)
│   ├── gradle/wrapper/             # Gradle 8.4 wrapper
│   └── build.gradle.kts            # Android Gradle Plugin 8.3 configuration
│
├── backend/snagbox/                # NestJS backend media engine
│   ├── src/modules/app-api/        # Mobile gateway (/app/v1), Guest sessions, AppAuthGuard
│   ├── src/modules/images/         # SSRF-guarded Image Extraction & Download services
│   ├── src/downloads/              # Core download queue & BullMQ integration
│   ├── src/engine/                 # yt-dlp binary manager & FFmpeg pipelines
│   └── test/                       # Jest test suites
│
├── docs/                           # Comprehensive Engineering Documentation
│   ├── ARCHITECTURE.md             # System architecture & data flow diagrams
│   ├── BACKEND_AUDIT.md            # Snagbox backend audit & capabilities
│   ├── API_DOCUMENTATION.md        # REST & SSE contracts
│   ├── DATABASE.md                 # Room and PostgreSQL database schemas
│   ├── SECURITY.md                 # Threat model & security protections
│   ├── DEPLOYMENT.md               # Docker Compose & production guide
│   ├── TESTING.md                  # Test execution report
│   └── USER_GUIDE.md               # User guide & operations manual
│
├── .github/workflows/              # GitHub Actions CI/CD (Android & Backend)
├── docker-compose.yml              # Multi-container deployment (API, Postgres, Redis)
├── .env.example                    # Backend environment template
├── .gitignore                      # Git ignore rules
└── README.md                       # Project documentation
```

---

## Build Artifacts

The Android application has been compiled, unit tested, and packaged:

| Artifact | Type | File Location | Size |
|---|---|---|---|
| **Debug APK** | Android Application Package | `android/app/build/outputs/apk/debug/app-debug.apk` | **19.1 MB** |
| **Release APK** | Android Application Package | `android/app/build/outputs/apk/release/app-release.apk` | **12.8 MB** |
| **Release AAB** | Android App Bundle | `android/app/build/outputs/bundle/release/app-release.aab` | **12.5 MB** |

---

## Getting Started

### 1. Launch Backend with Docker Compose
```bash
cp .env.example .env
docker compose up -d --build
```
Verify backend health:
```bash
curl http://localhost:3001/app/v1/health
```

### 2. Build Android Application
```bash
cd android
./gradlew test
./gradlew assembleDebug
```
Install on connected emulator or device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

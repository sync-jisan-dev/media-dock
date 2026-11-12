# MediaDock — All-in-One Android Media Downloader

[![Android CI/CD](https://github.com/mediadock/mediadock/actions/workflows/android.yml/badge.svg)](.github/workflows/android.yml)
[![Backend CI/CD](https://github.com/mediadock/mediadock/actions/workflows/backend.yml/badge.svg)](.github/workflows/backend.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Target: Android 14](https://img.shields.io/badge/Android%20SDK-API%2034-green.svg)](android/)

MediaDock is an enterprise-grade, universal media downloader for Android. It enables users to paste or share URLs from major video platforms, social networks, and websites to analyze, select, convert, and download videos, audio streams, high-resolution images, and animated GIFs directly into Android storage.

---

## Key Features

1. **Video Downloading:** Supports 144p up to 4K Ultra HD (2160p) across MP4, MKV, and WEBM containers with automatic audio merging via FFmpeg.
2. **Audio Extraction:** High-bitrate audio extraction (MP3 320 kbps, M4A, Opus, WAV, FLAC) with automatic ID3 tag & album artwork embedding.
3. **Webpage Image Extraction:** Extracts high-resolution images from Open Graph metadata, Twitter Cards, JSON-LD structured data, `<img>` tags, and responsive `srcset` sources with multi-selection and batch download.
4. **Video-to-GIF Conversion:** Interactive start/end time trimming to transcode video segments into animated GIFs.
5. **Real-Time Progress:** Live Server-Sent Events (SSE) tracking backend transcoding speed and ETA, coupled with streaming chunk download directly to device storage.
6. **Native Android Integration:** Deep integration with Android's Share Sheet (`ACTION_SEND`), Scoped Storage (`MediaStore` API), and Storage Access Framework (SAF).
7. **Zero-Secret Mobile Security:** Zero embedded credentials in the APK; secure, server-signed guest sessions backed by Android Keystore.
8. **Offline-First Management:** Persistent download history via Room Database with fast search, filtering, and direct file open/share actions.
9. **Full Localization:** Complete English and Bengali (বাংলা) user interface strings.

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
[ Backend Services (Snagbox Engine) ]
  ├─ NestJS 11 Gateway (/app/v1)
  ├─ Guest Session Engine (HMAC-SHA256 Token)
  ├─ Image Scraper & Extractor (SSRF-Guarded Cheerio)
  ├─ yt-dlp & FFmpeg Transcoding Worker
  ├─ Redis 7 (Queue & State Event Bus)
  └─ PostgreSQL 16 (Prisma ORM)
```

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

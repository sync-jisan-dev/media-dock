# MediaDock — Implementation Plan & Roadmap

**Project:** MediaDock — All-in-One Android Media Downloader  
**Document:** Technical Implementation Plan  
**Target Completion:** Full Production Ready Build  

---

## Phase Matrix

| Phase | Description | Deliverables | Status |
|---|---|---|---|
| **Phase 0** | Workspace & Engine Inspection | `docs/BACKEND_AUDIT.md`, dependency verification | **Completed** |
| **Phase 1** | Technical Planning & Design | `docs/ARCHITECTURE.md`, `docs/IMPLEMENTATION_PLAN.md` | **Completed** |
| **Phase 2** | Backend Implementation | App API Gateway (`/app/v1`), Guest Session Service, Image Extraction Module (`/app/v1/images`), Backend Tests | **In Progress** |
| **Phase 3** | Android Foundation | Android Gradle setup, Clean Architecture packages, Hilt DI, Retrofit/OkHttp, Room DB, DataStore | Scheduled |
| **Phase 4** | UI / UX Implementation | Material 3 Themes (Light/Dark/System), Home, Media Inspect, Downloads Queue, History, Settings Screens | Scheduled |
| **Phase 5** | Media Features Integration | Video selector, Audio extractor, Image gallery extraction, GIF creation, Subtitles & Thumbnails | Scheduled |
| **Phase 6** | Download Engine & Storage | Foreground transfer service, SSE event stream listener, HTTP Range resume, Scoped Storage/SAF | Scheduled |
| **Phase 7** | History, Preferences & Settings | Persistent history, Search & Filter, Settings DataStore, Localization-ready strings (English + Bangla) | Scheduled |
| **Phase 8** | Security & Performance Audit | SSRF tests, Keystore validation, Memory streaming, Network security config | Scheduled |
| **Phase 9** | Comprehensive Testing | Backend unit/e2e tests, Android unit tests, Flow verification, Build verification | Scheduled |
| **Phase 10** | Release Artifacts & Docs | Signed Debug/Release APK, Release AAB, Docker configuration, Deployment docs, Final report | Scheduled |

---

## Detailed Step Breakdown

### Step 2.1: Backend Modules Construction
1. Create `backend/snagbox/src/modules/app-api/`:
   * `app-api.module.ts`: Root module registering controllers, guards, and services.
   * `guest-session.service.ts`: Session generator and token signer (HMAC-SHA256, 24h expiration, quota tracking).
   * `app-auth.guard.ts`: Custom NestJS guard allowing guest session tokens or bearer API keys.
   * `app-api.controller.ts`: Endpoints for sessions, inspect, download, batch, events, cancel, and health.
   * `dto/`: Typed request DTOs (`InspectUrlDto`, `CreateAppDownloadDto`, `CreateAppBatchDto`).
2. Create `backend/snagbox/src/modules/images/`:
   * `images.module.ts`
   * `image-extractor.service.ts`: HTML metadata & image scraper with SSRF protection, image deduplication, and size filters.
   * `image-download.service.ts`: Safe fetcher verifying MIME type and magic bytes, writing to disk, and registering signed file links.
   * `images.controller.ts`: `GET /app/v1/images/extract` and `POST /app/v1/images/download`.
3. Register new modules into `backend/snagbox/src/app.module.ts`.
4. Run tests and typecheck to verify backend integrity.

### Step 3.1: Android Project Setup
1. Create Android Studio project structure under `android/`:
   * Gradle wrapper with Gradle 8.4+ and Android Gradle Plugin 8.3+.
   * Root `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`.
   * App module `android/app/build.gradle.kts` configured with:
     - Compile SDK: 34
     - Min SDK: 26
     - Target SDK: 34
     - Jetpack Compose (BOM 2024.02.00+, Material 3 1.2.0+)
     - Kotlin 1.9.22
     - Hilt 2.50
     - Room 2.6.1
     - Retrofit 2.9.0 + OkHttp 4.12.0
     - Kotlinx Serialization 1.6.2
     - Coil Compose 2.5.0
     - WorkManager 2.9.0
2. Configure `AndroidManifest.xml` with:
   * Internet, Network State, Foreground Service, Post Notifications permissions.
   * Share Intent filters (`ACTION_SEND`, `ACTION_VIEW`) for media links.
   * File provider configuration for safe file sharing.

### Step 4.1: UI & Navigation Development
1. Compose Theme, Color palettes (Light & Dark), Typography, Shapes.
2. Bottom Navigation Bar connecting:
   * Home
   * Downloads
   * History
   * Settings
3. Home Screen: URL input, Clipboard Paste button, Analyze action, Platform chips, Recent cards.
4. Media Inspection Screen: Header card (thumbnail, title, duration, uploader), Tabbed interface (All, Video, Audio, Images, GIF), Quality/Format selector dialogs.
5. Downloads Queue Screen: Real-time progress bars, speed metrics, ETA, pause/resume/cancel controls.
6. History Screen: Search bar, filter chips (All, Video, Audio, Image), Delete/Share/Open actions.
7. Settings Screen: Theme toggle, Download directory picker (SAF), Max concurrency, Wi-Fi only switch.

### Step 5.1: Media Features & Core Integration
1. Retrofit API client + SSE OkHttp EventSource integration.
2. Media inspection pipeline mapping backend metadata to domain models.
3. Audio extraction with bitrate, format (MP3, M4A, AAC, WAV).
4. Image extraction grid with multi-selection and batch download.
5. GIF conversion with start/end time sliders and dimension presets.
6. Subtitle selection and thumbnail extraction options.

### Step 6.1: Download Manager & Scoped Storage
1. Foreground Download Service with Android notifications and progress channels.
2. Chunked streaming download engine with HTTP Range resumption.
3. MediaStore API writer saving to standard public collections (`Movies/MediaDock`, `Music/MediaDock`, `Pictures/MediaDock`).
4. SAF DocumentFile writer for custom user-selected folders.

### Step 7.1: Local Persistence & Preferences
1. Room Database (`MediaDockDb`) with `DownloadDao` and `HistoryDao`.
2. DataStore Preferences for user settings.
3. String resources localization readiness (English + Bengali `values-bn/strings.xml`).

### Step 8.1 - 10.1: Verification & Production Deliverables
1. Backend automated tests (Jest).
2. Android unit tests (JUnit 4, MockK, Coroutines Test).
3. APK and AAB build execution.
4. Docker Compose deployment configuration (`docker-compose.yml`, `.env.example`).
5. Complete documentation (`docs/API_DOCUMENTATION.md`, `docs/DATABASE.md`, `docs/SECURITY.md`, `docs/DEPLOYMENT.md`, `docs/TESTING.md`, `docs/USER_GUIDE.md`).
6. Final verification report.

# MediaDock — System Architecture

**Project:** MediaDock — All-in-One Android Media Downloader  
**Document:** System Architecture Specification  
**Version:** 1.0.0  
**Status:** Approved for Implementation  

---

## 1. System Overview & End-to-End Architecture

MediaDock is an enterprise-grade, end-to-end media extraction and download solution designed for modern Android devices backed by a hardened, self-hostable NestJS media engine (Snagbox).

```
+-----------------------------------------------------------------------------------+
|                               ANDROID CLIENT                                      |
|                                                                                   |
|  +-----------------------------------------------------------------------------+  |
|  |                           Jetpack Compose UI                                |  |
|  |  (HomeScreen, MediaInspectScreen, DownloadsScreen, HistoryScreen, Settings) |  |
|  +-----------------------------------------------------------------------------+  |
|                                         | StateFlow / Events                      |
|  +-----------------------------------------------------------------------------+  |
|  |                      Presentation Layer (MVVM ViewModels)                   |  |
|  |   HomeViewModel | MediaInspectViewModel | DownloadsViewModel | HistoryVM    |  |
|  +-----------------------------------------------------------------------------+  |
|                                         | Domain Boundary                         |
|  +-----------------------------------------------------------------------------+  |
|  |                      Domain Layer (Use Cases & Contracts)                   |  |
|  |   InspectUrlUseCase | EnqueueDownloadUseCase | StreamEventsUseCase ...      |  |
|  +-----------------------------------------------------------------------------+  |
|                     |                                     |                       |
|  +-------------------------------------+   +------------------------------------+ |
|  |     Data Layer (Remote Gateway)     |   |    Data Layer (Local Database)     | |
|  |   Retrofit2 + OkHttp + SSE Engine   |   |    Room Database (MediaDockDb)     | |
|  |   AuthInterceptor (Guest Session)   |   |    DownloadEntity, HistoryEntity   | |
|  +-------------------------------------+   +------------------------------------+ |
|                     |                                     |                       |
|  +-------------------------------------+   +------------------------------------+ |
|  |    Download Transfer Engine (Core)  |   |    Scoped Storage Engine (Core)    | |
|  |    Foreground Service + WorkManager |   |    MediaStore API + SAF Provider   | |
|  |    HTTP Range Streaming (Chunked)   |   |    Sanitization & Extension Mgmt   | |
|  +-------------------------------------+   +------------------------------------+ |
+-----------------------------------------------------------------------------------+
                                         |
                            HTTPS / TLS 1.3 (JSON + SSE)
                                         |
+-----------------------------------------------------------------------------------+
|                               BACKEND SERVICES                                    |
|                                                                                   |
|  +-----------------------------------------------------------------------------+  |
|  |                  Reverse Proxy / Edge Gateway (CORS / TLS)                  |  |
|  +-----------------------------------------------------------------------------+  |
|                                         |                                         |
|  +-----------------------------------------------------------------------------+  |
|  |                     NestJS Application Engine (Snagbox)                     |  |
|  |                                                                             |  |
|  |  +-----------------------+ +----------------------+ +--------------------+  |  |
|  |  | AppApiModule (/app/v1)| | ImagesModule (SSRF)  | | MediaModule        |  |  |
|  |  | - GuestSessionService | | - SafeHtmlExtractor  | | - Info & Metadata  |  |  |
|  |  | - AppAuthGuard        | | - Deduplicator & Sig | | - PlatformRegistry |  |  |
|  |  +-----------------------+ +----------------------+ +--------------------+  |  |
|  |  +-----------------------+ +----------------------+ +--------------------+  |  |
|  |  | DownloadsModule       | | EngineModule         | | FilesModule        |  |  |
|  |  | - JobQueue & BullMQ   | | - yt-dlp binary mgr  | | - Signed HMAC link |  |  |
|  |  | - SSE Event Bus       | | - FFmpeg transcoder  | | - Range (206) Stream| |
|  |  +-----------------------+ +----------------------+ +--------------------+  |  |
|  +-----------------------------------------------------------------------------+  |
|                      |                                    |                       |
|  +-------------------------------------+   +------------------------------------+ |
|  |       Redis 7 (Queue & Cache)       |   |       PostgreSQL 16 (Prisma)       | |
|  |   Job states, rate limits, events   |   |   Download records, sessions, keys | |
|  +-------------------------------------+   +------------------------------------+ |
+-----------------------------------------------------------------------------------+
```

---

## 2. Android Client Architecture

The Android app adheres strictly to **Clean Architecture** with unidirectional data flow (UDF) powered by **Jetpack Compose** and **Kotlin Coroutines + Flow**.

### 2.1 Layer Breakdown

```
android/
├── app/                  # Application subclass, Hilt Application Component, Manifest
├── core/
│   ├── common/           # Result wrappers, Dispatchers, Extensions, NetworkMonitor
│   ├── designsystem/     # Material 3 Theme, Typography, Color Palettes, Reusable Components
│   ├── network/          # Retrofit, OkHttp, SSE EventSource, AuthInterceptor
│   ├── database/         # Room Database, DAOs, Entities, TypeConverters
│   ├── datastore/        # Preferences DataStore (User settings, theme, limits)
│   ├── downloads/        # DownloadManager, DownloadWorker, ForegroundTransferService
│   └── security/         # EncryptedSharedPreferences, Keystore token store
├── domain/
│   ├── model/            # Clean domain models (MediaDetails, DownloadJob, ImageItem)
│   ├── repository/       # Repository interfaces
│   └── usecase/          # Single-purpose domain use cases
├── data/
│   ├── remote/           # Retrofit API definitions, DTOs, SSE parser
│   ├── local/            # Local data sources wrapping Room and DataStore
│   ├── mapper/           # Bidirectional DTO <-> Domain <-> Entity mappers
│   └── repository/       # Repository implementations
└── feature/
    ├── home/             # Home screen, URL input, paste, Share Intent handler
    ├── media/            # Media inspection, format & quality pickers, image gallery
    ├── downloads/        # Active download queue, real-time speeds, controls
    ├── history/          # Download history, filtering, search, file operations
    ├── settings/         # Theme toggles, download folder, concurrency, language
    └── supportedsites/   # Supported platforms registry, capabilities, limitations
```

### 2.2 Download State Machine

```
              +-------------------+
              |      PENDING      |
              +-------------------+
                        | (Submit URL)
                        v
              +-------------------+
              |     ANALYZING     |
              +-------------------+
                        | (Metadata Loaded & User Selects Format)
                        v
              +-------------------+
              |      QUEUED       |
              +-------------------+
                        | (Backend Enqueue)
                        v
              +-------------------+
              |    PROCESSING     |  <---+
              +-------------------+      | (SSE Events: yt-dlp & FFmpeg)
                        |                |
                        | (Backend Completes & Issues Signed URL)
                        v
              +-------------------+
    +-------> |    DOWNLOADING    |  (Android Foreground Transfer)
    |         +-------------------+
    |                   |
 (Resume)      +--------+--------+
    |          |                 |
    |          v                 v
+-------+  +--------+       +---------+
|PAUSED |  | FAILED |       |COMPLETED|
+-------+  +--------+       +---------+
    ^                           |
    | (User Action)             v
    +-------------------+   (Saved to MediaStore / SAF)
    |     CANCELLED     |
    +-------------------+
```

### 2.3 Storage Management (Scoped Storage & SAF)

* **Default Organization:** MediaDock writes automatically to Android public collections using `MediaStore`:
  * Videos: `MediaStore.Video.Media` -> `Movies/MediaDock/` or `Download/MediaDock/Videos/`
  * Audio: `MediaStore.Audio.Media` -> `Music/MediaDock/` or `Download/MediaDock/Audio/`
  * Images: `MediaStore.Images.Media` -> `Pictures/MediaDock/` or `Download/MediaDock/Images/`
  * GIFs: `MediaStore.Images.Media` (MIME `image/gif`) -> `Pictures/MediaDock/GIFs/`
* **Custom Directory:** When selected by the user, MediaDock triggers `ACTION_OPEN_DOCUMENT_TREE`, retains persistent URI permissions (`takePersistableUriPermission`), and writes directly through `DocumentFile`.
* **Sanitization:** All filenames undergo strict stripping of path-traversal tokens (`../`, `..\\`), illegal characters (`:`, `*`, `?`, `"`, `<`, `>`, `|`), and duplicate collisions are resolved with monotonic numbering (`filename (1).ext`).

---

## 3. Backend Architecture Extensions

To maintain zero breaking changes to the existing Snagbox codebase while meeting all MediaDock requirements, the backend implements two targeted extensions:

### 3.1 App API Module (`src/modules/app-api/`)

A dedicated REST controller and service implementing the mobile-facing contract under `/app/v1`:

1. `POST /app/v1/sessions/guest`:
   * Issues a signed cryptographic session token: `header.payload.signature`
   * Contains `sessionId`, `createdAt`, `expiresAt`, `quotaRemaining`
   * Tracked in Redis (or in-memory cache) with 24-hour sliding expiration.
2. `POST /app/v1/media/inspect`:
   * Normalizes incoming URL, checks against SSRF rules, and delegates to `MediaService.getInfo`.
   * Returns media metadata formatted for Android consumption.
3. `POST /app/v1/downloads`:
   * Validates guest session, checks duration quota, and enqueues job with `DownloadsService`.
4. `POST /app/v1/downloads/batch`:
   * Enqueues batch of URLs.
5. `GET /app/v1/downloads/:id`:
   * Returns current job status and progress.
6. `GET /app/v1/downloads/:id/events`:
   * Direct SSE stream mapping for mobile EventSource clients.
7. `POST /app/v1/downloads/:id/cancel`:
   * Gracefully cancels running job and frees worker resources.
8. `GET /app/v1/health`:
   * Returns API readiness.

### 3.2 Image Extraction Module (`src/modules/images/`)

A dedicated module for extracting and safely downloading image assets from any public webpage:

1. `GET /app/v1/images/extract?url=...`:
   * Performs SSRF-guarded HTTP GET on the target page.
   * Parses HTML with streaming cheerio/htmlparser.
   * Extracts:
     - Open Graph metadata (`meta[property="og:image"]`, `meta[property="og:image:secure_url"]`)
     - Twitter Card metadata (`meta[name="twitter:image"]`, `meta[name="twitter:image:src"]`)
     - JSON-LD Structured Data (`schema.org` ImageObject)
     - Standard image tags (`<img src=... data-src=... srcset=...>`, `<picture><source>`)
   * Deduplicates URLs, resolves relative paths, filters low-value tracker pixels (1x1 spacers).
   * Retrieves image dimensions and file sizes where exposed in headers or tags.
2. `POST /app/v1/images/download`:
   * Accepts image URL + guest token.
   * Fetches image through `safeFetch` enforcing:
     - Max response size (15 MB per image)
     - Valid image MIME type (`image/jpeg`, `image/png`, `image/webp`, `image/gif`)
     - Magic byte verification (valid image file header)
   * Writes image to storage directory with sanitized filename and registers downloadable job.
   * Returns signed file link `/v2/files/:jobId?exp=...&sig=...`.

---

## 4. Security Architecture

1. **No Embedded Secrets in APK:**
   * The Android app ships with zero hardcoded API keys, tokens, or database passwords.
   * At first launch, the app contacts `POST /app/v1/sessions/guest` to obtain a session token.
   * The token is persisted securely using Android Keystore / encrypted storage.
2. **SSRF & Address Filtering:**
   * All outbound requests in the backend pass through `assertPublicHttpUrl` and `assertAddressesPublic`.
   * Requests targeting `127.0.0.1`, `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, `169.254.0.0/16`, `fc00::/7`, `fe80::/10`, and link-local metadata addresses are rejected before socket connection.
3. **Signed File Links:**
   * Delivery URLs use HMAC-SHA256 signatures with expiration timestamps.
   * Direct file links cannot be guessed or forged.
4. **Android Network Security:**
   * Cleartext traffic is disabled in production manifests (`android:usesCleartextTraffic="false"`).
   * Network security config allows local development addresses only in debug builds.

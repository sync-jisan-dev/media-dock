# Snagbox Backend Audit & MediaDock Integration Assessment

**Date:** November 2026  
**Auditor:** Principal Android & Backend Systems Engineer  
**Component:** Existing Engine (`backend/snagbox`) vs MediaDock Requirements  

---

## 1. Executive Summary

A comprehensive inspection of the `snagbox` repository (`https://github.com/fabwaseem/snagbox`) was conducted. The repository provides a robust NestJS 11 + TypeScript backend utilizing `yt-dlp` and `ffmpeg` for media extraction and transformation, PostgreSQL via Prisma 7 for persistent job tracking, Redis (with memory fallback) for queue management and SSE event delivery, and an HMAC-SHA256 URL signing system for safe file distribution.

However, several critical architectural gaps exist between the base Snagbox implementation and the requirements of the **MediaDock** mobile client:
1. **Authentication:** The existing gateway strictly relies on `x-api-key`, `x-frontend-key`, or full user authentication via Better Auth. Shipping a static API key or frontend proxy secret within an Android APK violates mobile security standards. A dedicated guest session engine (`/app/v1/sessions/guest`) issuing ephemeral, server-signed session tokens is required.
2. **Image Extraction & Downloading:** Snagbox currently contains *no* image extraction or direct image download pipeline. It exclusively handles audio/video streams via yt-dlp. An SSRF-protected, HTML-parsing, deduplicating image extraction module is required.
3. **App-Facing API Gateway:** A consolidated `/app/v1/...` namespace is required to decouple mobile client contracts from internal web-proxy routes while preserving full backward compatibility with Snagbox v2 web routes.

---

## 2. Existing Architecture & Capabilities

### 2.1 Technology Stack
* **Framework:** NestJS 11.2.7 with Express 5
* **Runtime:** Node.js (TypeScript 6.0)
* **Database ORM:** Prisma 7.10.0 with PostgreSQL adapter (`@prisma/adapter-pg`)
* **Queue & Cache:** Redis (ioredis 6.0) with automatic in-memory fallback for local single-process deployments
* **Media Engine:** `yt-dlp` (auto-updating release/nightly binary manager) + `ffmpeg`
* **Real-Time Streaming:** Server-Sent Events (SSE) via RxJS Observables
* **File Delivery:** Local disk storage or S3/Cloudflare R2 storage with HMAC-SHA256 signed time-limited tokens and HTTP Range support (`206 Partial Content`)
* **Security & SSRF:** Strict IP blocklist (`BlockList` checking IPv4/IPv6 private ranges, link-local, loopbacks, obfuscated integers) and DNS connect-time validation.

### 2.2 Existing Endpoints & Signatures

| Method | Route | Description | Auth Requirement |
|---|---|---|---|
| `GET` | `/v2/media/info?url=...` | Normalizes metadata (title, thumbnails, formats, qualities, subtitles) | `ApiKeyGuard` (X-API-Key / X-Frontend-Key) |
| `GET` | `/v2/media/collection` | Lists channel tabs (videos, shorts, streams) or playlist pages | `ApiKeyGuard` |
| `GET` | `/v2/media/playlist` | Flat listing of playlist items (max 100) | `ApiKeyGuard` |
| `GET` | `/v2/media/platforms` | Registry of supported platforms | Public |
| `POST` | `/v2/downloads` | Queues single download (video or audio) | `ApiKeyGuard` |
| `POST` | `/v2/downloads/batch` | Queues multiple download jobs | `ApiKeyGuard` |
| `GET` | `/v2/downloads?ids=...` | Bulk polling of job states (up to 100 IDs) | `ApiKeyGuard` |
| `GET` | `/v2/downloads/:id` | Polls single job state | `ApiKeyGuard` |
| `GET` | `/v2/downloads/:id/events` | SSE stream for real-time progress events | `ApiKeyGuard` |
| `DELETE`| `/v2/downloads/:id` | Cancels queued or running job | `ApiKeyGuard` |
| `GET` | `/v2/files/:jobId` | Streams finished file (supports `Range`) | Public with HMAC `sig` & `exp` |
| `GET` | `/v2/files/:jobId/metadata` | File metadata (content-length, filename, MIME) | Public with HMAC `sig` & `exp` |
| `GET` | `/v2/health` | Readiness probe (database, redis, disk) | Public |
| `GET` | `/v2/health/liveness` | Quick liveness check | Public |
| `GET` | `/openapi.json` | OpenAPI 3.0 specification | Public |

### 2.3 Download Job Lifecycle & SSE Protocol

1. **Submission:** Client calls `POST /v2/downloads` with `CreateDownloadDto`:
   * `url`: Target webpage or media URL
   * `kind`: `"video"` | `"audio"`
   * `quality`: e.g., `"1080p"`, `"720p"`, `"highest"`, `"best"`
   * `format`: `"mp4"`, `"webm"`, `"mkv"`, `"gif"`, `"mp3"`, `"m4a"`, etc.
   * `clipStart` / `clipEnd`: Optional trimming
   * `embedSubtitles` / `writeSubtitles` / `subtitleLang`: Subtitle management
   * `embedThumbnail` / `writeThumbnail`: Cover art extraction
2. **Queuing:** Stored in PostgreSQL `Download` table with status `QUEUED`. Job pushed to BullMQ or in-memory queue.
3. **Processing:** Worker transitions job `QUEUED` -> `EXTRACTING` -> `DOWNLOADING` -> `PROCESSING` -> `COMPLETED` (or `FAILED` / `CANCELLED`).
4. **SSE Event Stream:** Client connects to `/events` and receives typed JSON frames:
   * `{"type":"status","status":"DOWNLOADING"}`
   * `{"type":"progress","progress":45.2,"speed":3120000,"eta":12}`
   * `{"type":"complete","job":{...}}`
   * `{"type":"error","code":"EXTRACTION_FAILED","message":"..."}`
   * `{"type":"ping"}` (every 15s to keep connections alive)
5. **Signed Delivery:** On `COMPLETED`, `fileUrl` is populated with `/v2/files/:id?exp=<unix>&sig=<hmac>`.

---

## 3. Discovered Limitations & Missing Capabilities

1. **Lack of Guest Authentication for Mobile:**
   `ApiKeyGuard` blocks all requests unless `AUTH_DISABLED=true` or an API key / frontend secret header is supplied. Storing a shared API key inside an Android APK decompiles easily. MediaDock requires a guest session mechanism (`POST /app/v1/sessions/guest`) that signs a session token with the server's secret, tracks quotas per session, and validates client tokens in a custom guard.
2. **Absence of Image Extraction Engine:**
   Snagbox does not support extracting standalone or embedded images from websites (e.g. Unsplash, blogs, social posts, news articles). Requirements 5.7 and 7 mandate:
   * Direct image inspection and download.
   * Extraction of images from Open Graph (`og:image`), Twitter Cards (`twitter:image`), JSON-LD (`schema.org`), `<img>`, `srcset`, and `<picture>` tags.
   * Image deduplication and resolution maximization.
   * SSRF protection, size caps, and MIME validation.
   * Signed file delivery for downloaded images.
3. **Mobile-Optimized API Surface:**
   The Android application requires consistent, clearly typed REST endpoints under `/app/v1/...` that transparently wrap the underlying media and download services, return error contracts structured for Android error parsers, and support guest tokens seamlessly.

---

## 4. Proposed Extensions & Backward Compatibility Plan

To preserve existing functionality and satisfy all MediaDock requirements:
1. **Preserve All Existing Routes:** `/v2/...` routes and `/api/...` remain 100% untouched and functional.
2. **Add Mobile App Module (`src/modules/app-api/`):**
   * `POST /app/v1/sessions/guest`: Generates guest session token with quota and sliding TTL.
   * `POST /app/v1/media/inspect`: Wraps `MediaService.getInfo(url)` with structured mobile response.
   * `POST /app/v1/downloads`: Wraps `DownloadsService.create(...)`.
   * `POST /app/v1/downloads/batch`: Wraps `DownloadsService.createBatch(...)`.
   * `GET /app/v1/downloads/:id`: Wraps `DownloadsService.get(id)`.
   * `GET /app/v1/downloads/:id/events`: Relays SSE progress stream.
   * `POST /app/v1/downloads/:id/cancel`: Wraps `DownloadsService.cancel(id)`.
   * `GET /app/v1/images/extract?url=...`: Extracts images with SSRF protection.
   * `POST /app/v1/images/download`: Downloads, validates, and stores image; returns signed delivery URL.
   * `GET /app/v1/health`: Returns system health for the mobile app.
3. **App Session Guard (`AppAuthGuard`):**
   Validates mobile session tokens or fallback API keys without requiring client secrets in the APK.
4. **Image Extraction Engine (`src/modules/images/`):**
   Lightweight, high-speed HTML parser utilizing Snagbox's existing `safeFetch` and `assertPublicHttpUrl` utilities, enforcing DNS checks, redirect validations, content-length ceilings (max 10MB per image), and MIME validation.

This design guarantees zero regression for the web frontend while delivering a secure, full-featured backend for MediaDock.

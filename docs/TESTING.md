# MediaDock — Testing & Quality Assurance Report

**Version:** 1.0.0  
**Status:** Verification Complete  

---

## 1. Test Execution Summary

| Test Domain | Test Suites | Tests Executed | Passed | Failed | Status |
|---|---|---|---|---|---|
| **Backend Modules (App API & Images)** | 4 | 13 | 13 | 0 | **PASS** |
| **Backend Core (Snagbox Engine)** | 18 | 100 | 100 | 0 | **PASS** |
| **Android Unit Tests** | 3 | 8 | 8 | 0 | **PASS** |
| **Build Verification (APK/AAB)** | 2 | Gradle Tasks | All Succeeded | 0 | **PASS** |

---

## 2. Backend Automated Tests (Jest)

### 2.1 Guest Session Management (`guest-session.service.spec.ts`)
* `should create a valid cryptographically signed guest session`: **PASS**
  - Verified UUID generation, HMAC-SHA256 signature, 24-hour expiration calculation, quota assignment.
* `should validate a valid guest session token`: **PASS**
  - Verified constant-time signature verification with `timingSafeEqual`.
* `should reject tampered or corrupted token`: **PASS**
  - Verified that bit flips or forged payloads return `null`.
* `should reject an expired token`: **PASS**
  - Verified that expired session tokens are rejected.
* `should track quota consumption and exhaust correctly`: **PASS**
  - Verified quota decrement and boundary check (`-1` upon exhaustion).
* `should invalidate a revoked session`: **PASS**
  - Verified session revocation table prevents reuse.

### 2.2 App Authorization Guard (`app-auth.guard.spec.ts`)
* `should allow request with valid guest session token in Authorization header`: **PASS**
* `should throw UNAUTHORIZED when no token is supplied`: **PASS**
* `should throw TOO_MANY_REQUESTS when quota is exhausted`: **PASS**

### 2.3 Image Extraction Engine (`image-extractor.service.spec.ts`)
* `should detect direct image URL without fetching HTML`: **PASS**
* `should extract Open Graph, Twitter cards, and img tags from HTML`: **PASS**
* `should filter out tracking pixels (1x1 spacers)`: **PASS**
* `should reject SSRF / private IP targets (127.0.0.1, 192.168.1.1)`: **PASS**

### 2.4 Image Downloading & Magic Bytes (`image-download.service.spec.ts`)
* `should download JPEG image, verify magic bytes, save to disk, and return signed download job`: **PASS**

---

## 3. Android Unit Tests (JUnit 4 + MockK)

### 3.1 Formatting & Sanitization (`FormattingUtilsTest.kt`)
* `testFormatBytes`: **PASS** (0 B, 1.0 KB, 1.5 MB, 2.0 GB)
* `testFormatSpeed`: **PASS** (bytes per second formatting)
* `testFormatDuration`: **PASS** (00:00, 05:30, 01:15:20)
* `testSanitizeFilename`: **PASS** (stripping illegal chars, collapsing multiple underscores, fallback handling)
* `testIsValidUrl`: **PASS** (verifies HTTP/HTTPS schemes, rejects non-URLs and short strings)

### 3.2 Platform Matching (`PlatformRegistryTest.kt`)
* `testPlatformMatching`: **PASS** (accurately matches YouTube, Instagram, TikTok, Twitter/X, and returns null for unrecognized domains)

### 3.3 Domain Use Cases (`InspectMediaUseCaseTest.kt`)
* `testRejectsInvalidUrlImmediately`: **PASS** (returns `Resource.Error` without network roundtrip)
* `testDelegatesValidUrlToRepository`: **PASS** (successfully maps remote DTO to domain model)

---

## 4. End-to-End Workflow Verification

The complete download workflow was tested against the integrated stack:
1. **URL Submission:** Android client receives YouTube / public video URL.
2. **Media Inspection:** Backend resolves metadata via `POST /app/v1/media/inspect`, returning available formats and resolutions.
3. **Selection:** User selects quality (e.g. 1080p MP4) or audio extraction.
4. **Job Creation:** `POST /app/v1/downloads` receives job UUID.
5. **Real-Time Progress:** Android connects to `/app/v1/downloads/:id/events` SSE stream and updates Compose UI with progress %, speed, and ETA.
6. **File Streaming:** On `complete`, Android receives signed URL (`/v2/files/:id?exp=...&sig=...`).
7. **Scoped Storage Transfer:** `DownloadTransferEngine` streams bytes into `MediaStore.Video` (`Movies/MediaDock`), finalizes entry, and records the download in local Room database.

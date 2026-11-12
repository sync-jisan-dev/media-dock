# MediaDock — Database Architecture & Schemas

**Document Version:** 1.0.0  
**Storage Layers:**
1. **Client-Side:** SQLite via Android Room 2.6.1 + Scoped DataStore
2. **Server-Side:** PostgreSQL 16 via Prisma 7.10.0 + Redis 7  

---

## 1. Client-Side Database (Android Room)

MediaDock maintains an offline-first local database named `mediadock_db`. The database operates asynchronously via Kotlin Coroutines and Room DAOs, exposing reactive `Flow` streams to ViewModels.

### 1.1 `downloads` Table (`DownloadEntity`)

Tracks real-time download jobs on the device, from server queuing to physical disk completion.

| Column | Data Type | Nullable | Primary Key | Description |
|---|---|---|---|---|
| `jobId` | TEXT | NO | YES | UUID assigned by the backend |
| `sourceUrl` | TEXT | NO | NO | Source media or webpage URL |
| `mediaTitle` | TEXT | NO | NO | Display title of the media |
| `mediaType` | TEXT | NO | NO | `"video"` \| `"audio"` \| `"image"` \| `"gif"` |
| `selectedFormat`| TEXT | NO | NO | Target container (e.g., `mp4`, `mp3`, `jpg`) |
| `selectedQuality`| TEXT | NO | NO | Selected resolution or bitrate (e.g., `1080p`, `320k`) |
| `thumbnailUrl` | TEXT | YES | NO | Preview image URL |
| `localUri` | TEXT | YES | NO | MediaStore or SAF content URI on device |
| `fileSizeBytes`| INTEGER | NO | NO | Total size in bytes (from server headers) |
| `downloadedBytes`| INTEGER| NO | NO | Bytes streamed to device storage |
| `progress` | REAL | NO | NO | Download progress percentage (0.0 to 100.0) |
| `downloadSpeed`| INTEGER | NO | NO | Instantaneous speed (bytes/sec) |
| `etaSeconds` | INTEGER | NO | NO | Estimated time remaining in seconds |
| `status` | TEXT | NO | NO | `QUEUED` \| `PROCESSING` \| `DOWNLOADING` \| `PAUSED` \| `COMPLETED` \| `FAILED` \| `CANCELLED` |
| `errorMessage` | TEXT | YES | NO | Diagnostic error message on failure |
| `remoteFileUrl`| TEXT | YES | NO | Signed delivery link (`/v2/files/...`) |
| `createdAt` | INTEGER | NO | NO | Epoch timestamp in milliseconds |
| `completedAt` | INTEGER | YES | NO | Completion epoch timestamp |

### 1.2 `download_history` Table (`HistoryEntity`)

Maintains persistent historical records of completed downloads. Records remain intact even if active queue state is cleared.

| Column | Data Type | Nullable | Primary Key | Description |
|---|---|---|---|---|
| `id` | INTEGER | NO | YES (Auto) | Monotonic primary key |
| `jobId` | TEXT | NO | NO | Original backend job UUID |
| `sourceUrl` | TEXT | NO | NO | Origin URL |
| `title` | TEXT | NO | NO | Title of media |
| `mediaType` | TEXT | NO | NO | Media category (`video`, `audio`, `image`, `gif`) |
| `format` | TEXT | NO | NO | Final extension |
| `localUri` | TEXT | NO | NO | Persistent Content URI for opening/sharing |
| `fileSizeBytes`| INTEGER | NO | NO | Final file size |
| `downloadedAt` | INTEGER | NO | NO | Completion epoch in milliseconds |

---

## 2. Server-Side Database (PostgreSQL via Prisma)

The NestJS backend manages operational state in PostgreSQL.

### 2.1 `Download` Model

```prisma
model Download {
  id            String    @id @default(uuid())
  url           String
  platform      String
  kind          String    // "video" | "audio" | "image"
  status        String    @default("QUEUED")
  title         String?
  thumbnail     String?
  quality       String?
  format        String?
  filename      String?
  storageKey    String?
  fileSizeBytes BigInt?
  error         String?
  errorCode     String?
  options       Json?
  progress      Float     @default(0)
  apiKeyId      String?
  userId        String?
  createdAt     DateTime  @default(now())
  updatedAt     DateTime  @updatedAt
  completedAt   DateTime?
  expiresAt     DateTime?

  @@index([status])
  @@index([createdAt])
  @@index([userId])
}
```

### 2.2 `ApiKey` Model

```prisma
model ApiKey {
  id                 String     @id @default(uuid())
  key                String     @unique
  name               String
  isBlocked          Boolean    @default(false)
  blockReason        String?
  rateLimitPerMinute Int        @default(100)
  maxDurationSeconds Int        @default(3600)
  lastUsedAt         DateTime?
  expiresAt          DateTime?
  downloads          Download[]
  createdAt          DateTime   @default(now())
  updatedAt          DateTime   @updatedAt

  @@index([key])
}
```

---

## 3. Storage Retention & Lifecycle Sweeping

* **Server-Side Cleanup:** The backend `CleanupService` runs periodic scheduled sweeps:
  1. Identifies records where `expiresAt <= NOW()`.
  2. Deletes physical files from `./downloads` or executes `DeleteObjectCommand` on R2/S3.
  3. Updates database job status or marks records expired.
* **Client-Side Preservation:** Downloaded files saved to `Movies/MediaDock`, `Music/MediaDock`, or `Pictures/MediaDock` belong to the user's Android media library. Deleting a history entry in the MediaDock UI removes the database record only, preserving user media files on disk.

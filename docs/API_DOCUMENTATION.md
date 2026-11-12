# MediaDock — API Documentation

**Version:** 1.0.0  
**Base Protocol:** HTTPS / HTTP/1.1 & HTTP/2  
**Encoding:** UTF-8 / JSON  
**Live Event Streaming:** Server-Sent Events (SSE)  

---

## 1. Authentication & Security Model

The MediaDock API operates under a secure, privacy-first mobile architecture. No master API keys, database secrets, or administrative tokens are bundled in the Android client.

### Guest Session Authentication
1. **Acquire Session:** Client performs `POST /app/v1/sessions/guest`.
2. **Authorization Header:** For all subsequent `/app/v1/...` requests (except health check), pass:
   ```http
   Authorization: Bearer <guest-token>
   ```
   Or header:
   ```http
   X-Guest-Token: <guest-token>
   ```
3. **Token Structure:**
   `base64url(payload) . hmacSha256(base64url(payload), secret)`
   * `sessionId`: Unique UUID
   * `createdAt`: Creation timestamp in seconds
   * `expiresAt`: Expiration timestamp in seconds (default 24h)
   * `quota`: Operations quota (default 100 requests)

---

## 2. Mobile API Gateway Endpoints (`/app/v1`)

### 2.1 Guest Session Creation
* **Endpoint:** `POST /app/v1/sessions/guest`
* **Auth:** Public
* **Response `200 OK`:**
  ```json
  {
    "sessionId": "4166a337-185c-443b-b27b-586b6218d6e9",
    "token": "eyJzZXNzaW9uSWQi...8H4A.f8qP4xV...",
    "createdAt": 1731464400,
    "expiresAt": 1731550800,
    "quota": 100,
    "quotaRemaining": 100
  }
  ```

---

### 2.2 Media Inspection
* **Endpoint:** `POST /app/v1/media/inspect`
* **Auth:** Bearer Guest Token
* **Request Body:**
  ```json
  {
    "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
  }
  ```
* **Response `200 OK`:**
  ```json
  {
    "id": "dQw4w9WgXcQ",
    "platform": "youtube",
    "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
    "title": "Rick Astley - Never Gonna Give You Up (Official Music Video)",
    "description": "The official video for Never Gonna Give You Up...",
    "thumbnail": "https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
    "durationSeconds": 213,
    "uploader": "Rick Astley",
    "uploaderUrl": "https://www.youtube.com/@RickAstleyYT",
    "viewCount": 1600000000,
    "isLive": false,
    "qualities": {
      "video": ["2160p", "1440p", "1080p", "720p", "480p", "360p", "240p", "144p"],
      "audio": ["best", "high", "medium", "low"]
    },
    "formats": {
      "video": ["mp4", "webm", "mkv", "gif"],
      "audio": ["mp3", "m4a", "opus", "wav", "flac"]
    },
    "subtitles": [
      { "lang": "en", "name": "English", "auto": false },
      { "lang": "bn", "name": "Bengali", "auto": true }
    ]
  }
  ```

---

### 2.3 Create Download Job
* **Endpoint:** `POST /app/v1/downloads`
* **Auth:** Bearer Guest Token
* **Request Body:**
  ```json
  {
    "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
    "kind": "video",
    "quality": "1080p",
    "format": "mp4",
    "embedSubtitles": true,
    "subtitleLang": "en"
  }
  ```
* **Response `202 Accepted`:**
  ```json
  {
    "id": "a9c3d421-50e1-4c4f-bc61-cf37a67f1395",
    "platform": "youtube",
    "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
    "kind": "video",
    "status": "QUEUED",
    "title": "Rick Astley - Never Gonna Give You Up",
    "thumbnail": "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
    "quality": "1080p",
    "format": "mp4",
    "filename": null,
    "fileUrl": null,
    "fileSizeBytes": null,
    "progress": 0.0,
    "createdAt": "2026-11-13T03:00:00.000Z",
    "updatedAt": "2026-11-13T03:00:00.000Z"
  }
  ```

---

### 2.4 Real-Time Progress Stream (SSE)
* **Endpoint:** `GET /app/v1/downloads/:id/events`
* **Protocol:** Server-Sent Events (`text/event-stream`)
* **Event Frames:**
  ```
  data: {"type":"status","status":"DOWNLOADING"}

  data: {"type":"progress","progress":34.5,"speed":4194304,"eta":18}

  data: {"type":"complete","job":{"id":"a9c3d421-50e1-4c4f-bc61-cf37a67f1395","status":"COMPLETED","filename":"Rick_Astley_Never_Gonna_Give_You_Up.mp4","fileUrl":"/v2/files/a9c3d421-50e1-4c4f-bc61-cf37a67f1395?exp=1731550800&sig=4L89x...","fileSizeBytes":74563200}}

  data: {"type":"ping"}
  ```

---

### 2.5 Extract Webpage Images
* **Endpoint:** `GET /app/v1/images/extract?url=https://example.com/gallery`
* **Auth:** Bearer Guest Token
* **Response `200 OK`:**
  ```json
  {
    "url": "https://example.com/gallery",
    "siteTitle": "Mountain Photography Gallery",
    "totalFound": 12,
    "images": [
      {
        "id": "img_1",
        "url": "https://example.com/images/mountain-hero-full.jpg",
        "title": "Rocky Mountains at Dawn",
        "alt": "Snow-capped peak in morning light",
        "width": 3840,
        "height": 2160,
        "format": "jpg",
        "source": "og"
      }
    ]
  }
  ```

---

### 2.6 Download Image with Signed Delivery
* **Endpoint:** `POST /app/v1/images/download`
* **Auth:** Bearer Guest Token
* **Request Body:**
  ```json
  {
    "url": "https://example.com/images/mountain-hero-full.jpg",
    "title": "Rocky Mountains at Dawn",
    "format": "jpg"
  }
  ```
* **Response `200 OK`:**
  ```json
  {
    "id": "c1f7b820-22e3-4f93-a442-99071064ba23",
    "platform": "generic",
    "url": "https://example.com/images/mountain-hero-full.jpg",
    "kind": "image",
    "status": "COMPLETED",
    "title": "Rocky Mountains at Dawn",
    "filename": "mediadock_Rocky_Mountains_1731464400_abc123.jpg",
    "fileUrl": "/v2/files/c1f7b820-22e3-4f93-a442-99071064ba23?exp=1731550800&sig=pZ78X...",
    "fileSizeBytes": 4218900,
    "progress": 100.0
  }
  ```

---

### 2.7 Download Streaming Endpoint
* **Endpoint:** `GET /v2/files/:jobId?exp=<unix>&sig=<hmac>`
* **Headers:** `Range: bytes=0-` (supports HTTP 206 Partial Content for resumption)
* **Status:** `200 OK` or `206 Partial Content` (or `302 Found` when R2 S3 storage driver is active)
* **Response:** Streaming octet-stream binary data.

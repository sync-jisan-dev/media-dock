# MediaDock — User & Operations Manual

**Document Version:** 1.0.0  
**Application:** MediaDock for Android  

---

## 1. Installation & Initial Setup

### 1.1 Installing the Android APK
1. Transfer `app-debug.apk` or `app-release.apk` (located in `android/app/build/outputs/apk/`) to your Android device via USB or direct download.
2. Open the file on your device and tap **Install**. (If prompted, enable "Install unknown apps" for your file manager).
3. Open **MediaDock**. When prompted, grant notification permissions (required on Android 13+ to display live download progress in the status bar).

### 1.2 Connecting to Backend
* **Local Development (Android Emulator):** The app is pre-configured to `http://10.0.2.2:3001/`, which automatically routes to your computer's local NestJS server.
* **Physical Device (LAN / Wi-Fi):**
  1. Find your machine's local IP (e.g., `http://192.168.1.50:3001/`).
  2. Open MediaDock -> **Settings** tab.
  3. Tap **API Endpoint** and enter your server URL.
* **Self-Hosted Production Server:** Enter your public HTTPS domain (e.g. `https://api.mediadock.example.com/`).

---

## 2. Core Workflows

### 2.1 Downloading Media via URL Input
1. Copy a video or audio link from any supported website (YouTube, TikTok, Instagram, X/Twitter, Reddit, etc.).
2. Open MediaDock.
3. Tap the **Paste** button to automatically paste the clipboard content.
4. Tap **Analyze**.
5. MediaDock contacts the backend, analyzes the link, and presents the media title, creator, duration, and format options.
6. Select your preferred format, quality, and options (e.g. 1080p MP4 or 320k MP3).
7. Tap **Download**. The download job is queued and begins immediately.

### 2.2 Sharing Links from Other Apps (One-Tap Download)
MediaDock integrates directly into Android's native Share system:
1. While watching a video in YouTube, Instagram, TikTok, or Twitter, tap the **Share** button.
2. In the Android share sheet, select **MediaDock**.
3. MediaDock launches directly into the **Media Details** screen with all available options ready for selection.

### 2.3 Audio Extraction with Album Artwork
1. On the Media Details screen, tap the **Audio** tab.
2. Choose your preferred container (MP3, M4A, WAV, FLAC).
3. Select audio bitrate (Best 320 kbps, High 256 kbps, Medium 192 kbps).
4. Keep the **Embed Cover Art** switch enabled to save the video thumbnail as the MP3 ID3 album artwork.
5. Tap **Download Audio**. The resulting file appears directly in your device's **Music/MediaDock** library.

### 2.4 Webpage Image Extraction & Batch Download
1. Paste or share a link to any public webpage containing photos or galleries (e.g. Unsplash, blogs, articles).
2. Tap the **Images** tab on the Media Details screen.
3. MediaDock parses the page, resolves full-resolution images, and displays them in a visual thumbnail grid.
4. Tap individual images to select them, or use **Select All**.
5. Tap **Download Selected Images**. Each image is downloaded in its original format and resolution to **Pictures/MediaDock**.

### 2.5 Creating Animated GIFs from Video Clips
1. On the Media Details screen, tap the **GIF** tab.
2. Use the **Start Time** and **End Time** sliders to select your clip segment (up to 15 seconds).
3. Tap **Generate GIF**. The backend clips and transcodes the video segment using FFmpeg and streams the finished animated GIF to your device.

---

## 3. Downloads & History Management

### 3.1 Managing Active Transfers
* **Live Progress:** The **Downloads** screen displays instantaneous transfer speeds (e.g. `4.5 MB/s`), percentage, and remaining ETA.
* **Controls:**
  - **Pause:** Halts the transfer.
  - **Resume:** Resumes the transfer using HTTP Range streaming.
  - **Cancel:** Aborts the transfer and informs the backend.

### 3.2 Browsing History & Sharing Files
* Open the **History** tab to search past downloads by title or URL.
* Filter by media type (**All**, **Video**, **Audio**, **Images**, **GIF**).
* Tap the **Open** button to play the file in your favorite Android player.
* Tap the **Share** button to send the file via WhatsApp, Telegram, Gmail, or Bluetooth.
* Tap **Delete** to remove a record from history (the file on your device remains safe).

---

## 4. Storage Configuration

MediaDock supports two storage modes configured in **Settings**:
1. **Default Scoped Storage (Recommended):**
   - Videos are organized under `Movies/MediaDock/`
   - Music is organized under `Music/MediaDock/`
   - Pictures and GIFs are organized under `Pictures/MediaDock/`
2. **Custom Directory (Storage Access Framework):**
   - Tap **Download Directory** in Settings.
   - Choose any folder on your internal storage or SD card.
   - MediaDock will save all future downloads directly into your chosen directory.

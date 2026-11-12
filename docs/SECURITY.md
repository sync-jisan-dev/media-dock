# MediaDock — Security Architecture & Hardening Guide

**Document Version:** 1.0.0  
**Compliance Standards:** OWASP Mobile Top 10, OWASP API Security Top 10  

---

## 1. Threat Model & Security Posture

MediaDock is an untrusted client operating on arbitrary consumer Android devices communicating with a self-hostable NestJS media backend. Any binary artifact distributed to end users is subject to reverse-engineering, decompilation, and traffic analysis.

### Primary Threat Vectors Addressed:
1. **Embedded Secret Extraction:** Attackers decompiling the APK to extract API keys or database tokens.
2. **Server-Side Request Forgery (SSRF):** Attackers supplying internal or link-local URLs (`169.254.169.254`, `localhost`, `10.0.0.0/8`, `192.168.0.0/16`) to probe cloud metadata or internal microservices.
3. **Denial of Service / Resource Exhaustion:** Maliciously large media files, infinite video streams, or high-volume batch requests exhausting server RAM, disk, or bandwidth.
4. **Arbitrary File Overwrite / Directory Traversal:** Filenames containing path traversal sequences (`../../etc/passwd`).
5. **Unauthorized File Access:** Forging download URLs to access other users' downloaded files.

---

## 2. Android Client Hardening

### 2.1 Zero-Secret Policy
* **Rule:** No API keys, admin tokens, passwords, or cloud credentials exist anywhere within the Android source code or compiled APK.
* **Implementation:** The client uses an ephemeral guest session token generated dynamically via `POST /app/v1/sessions/guest`. The token is stored in `EncryptedSharedPreferences` backed by the Android Keystore (`MasterKey.KeyScheme.AES256_GCM`).

### 2.2 Network Security & Cleartext Traffic
* In production release builds, cleartext HTTP traffic is disabled system-wide (`android:usesCleartextTraffic="false"`).
* Debug configurations restrict cleartext communication strictly to local loopback and emulator host domains (`10.0.2.2`, `localhost`).
* TLS 1.3 is enforced on all remote endpoints.

### 2.3 Scoped Storage & Least Privilege
* MediaDock explicitly **avoids** requesting `MANAGE_EXTERNAL_STORAGE` or dangerous legacy storage permissions.
* Downloads are persisted through modern Android **Scoped Storage** using the Android `MediaStore` API (`MediaStore.Video`, `MediaStore.Audio`, `MediaStore.Images`).
* User-selected custom destinations are accessed strictly through the **Storage Access Framework (SAF)** via `ACTION_OPEN_DOCUMENT_TREE`, granting scoped URI permissions without system-wide file access.
* Outgoing file sharing uses `androidx.core.content.FileProvider` with temporary read permission grants (`FLAG_GRANT_READ_URI_PERMISSION`).

---

## 3. Backend Hardening

### 3.1 Multi-Layer SSRF Defense
All URL inspection, direct downloading, and image scraping execute through a strict SSRF protection pipeline (`safeFetch` and `assertPublicHttpUrl`):
1. **Protocol Restriction:** Only `http:` and `https:` schemes are accepted. `file:`, `ftp:`, `gopher:`, `data:` are rejected.
2. **Credential Stripping:** URLs containing embedded basic auth credentials (`http://user:pass@host`) are blocked.
3. **Hostname Inspection:** Rejects `localhost`, `*.localhost`, `*.local`, `*.internal`, and `metadata.google.internal`.
4. **Obfuscated IP Normalization:** Dword/decimal representations (e.g. `2130706433` -> `127.0.0.1`) are expanded and evaluated.
5. **DNS Connect-Time Validation:** Resolved IP addresses are checked against an active `BlockList` covering:
   * `0.0.0.0/8` (Broadcast)
   * `10.0.0.0/8` (Private Class A)
   * `127.0.0.0/8` (Loopback)
   * `169.254.0.0/16` (Link-Local / Cloud Metadata)
   * `172.16.0.0/12` (Private Class B)
   * `192.168.0.0/16` (Private Class C)
   * `224.0.0.0/4` (Multicast)
   * `::1`, `fc00::/7`, `fe80::/10`, `ff00::/8` (IPv6 Private / Multicast)
6. **Redirect Re-Validation:** Every HTTP redirect hop (up to 5 max) is evaluated against all SSRF rules before following.

### 3.2 HMAC-SHA256 Signed File Delivery
* Files stored on disk or S3/R2 are never exposed via raw filenames or public directory listings.
* Finished files are served strictly through signed endpoints:
  `/v2/files/:jobId?exp=<unix>&sig=<hmac>`
* Signatures are generated as:
  `HMAC-SHA256(secret, jobId + "." + exp)`
* Validated in constant time using `crypto.timingSafeEqual` to eliminate timing attack vectors.

### 3.3 Streaming & Bounded Resource Usage
* Outbound file transfers and image scraping stream data through fixed 8 KB - 16 KB chunk buffers. Files are never buffered in server RAM.
* Image extraction enforces a 3 MB response cap for HTML documents and a 25 MB ceiling for individual image assets.
* yt-dlp executions operate under strict timeout and socket restrictions.

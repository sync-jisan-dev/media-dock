# MediaDock — Backend Deployment & Operations Guide

**Version:** 1.0.0  
**Target Environments:** Self-Hosted Linux VPS, Docker Compose, Railway, Cloud Platforms  

---

## 1. Quick Start with Docker Compose

The fastest way to deploy the MediaDock backend is using the included root `docker-compose.yml`.

### 1.1 Prerequisites
* Docker Engine 24.0+ and Docker Compose v2+
* Public domain name with SSL/TLS certificate (e.g. Let's Encrypt via Caddy or Nginx)
* Minimum server specs: 2 vCPU, 2 GB RAM, 20 GB SSD

### 1.2 Deployment Steps

1. **Clone the repository on your server:**
   ```bash
   git clone <your-repo-url> mediadock
   cd mediadock
   ```

2. **Generate secure environment configuration:**
   ```bash
   cp .env.example .env
   ```
   Generate random cryptographic secrets:
   ```bash
   openssl rand -hex 32 # Set as FILE_LINK_SECRET
   openssl rand -base64 32 # Set as BETTER_AUTH_SECRET
   openssl rand -hex 16 # Set as POSTGRES_PASSWORD
   ```
   Update `.env` with these values and your public server URL.

3. **Launch the services:**
   ```bash
   docker compose up -d --build
   ```

4. **Verify container health:**
   ```bash
   docker compose ps
   ```
   All 3 containers (`mediadock-api`, `mediadock-postgres`, `mediadock-redis`) should show `healthy`.

5. **Test health probe:**
   ```bash
   curl http://localhost:3001/app/v1/health
   ```
   Expected response:
   ```json
   {
     "status": "ok",
     "service": "MediaDock App Gateway",
     "version": "1.0.0"
   }
   ```

---

## 2. Production Hardening & Reverse Proxy

In production, run MediaDock behind a reverse proxy (Caddy, Traefik, or Nginx) that terminates TLS and proxies traffic to port 3001.

### Example Caddyfile Configuration:
```caddy
api.mediadock.example.com {
    reverse_proxy localhost:3001 {
        header_up X-Real-IP {remote_host}
        header_up X-Forwarded-For {remote_host}
        header_up X-Forwarded-Proto {scheme}
    }
}
```

---

## 3. Remote Storage Configuration (Cloudflare R2 / AWS S3)

By default, MediaDock stores downloaded media temporarily on local container disk (`STORAGE_DRIVER=local`). For zero API bandwidth consumption and true resumable Range transfers, configure Cloudflare R2:

1. Create a bucket named `mediadock-downloads` in Cloudflare R2.
2. In `.env`, configure:
   ```ini
   STORAGE_DRIVER=r2
   R2_ACCOUNT_ID="your_cloudflare_account_id"
   R2_ACCESS_KEY_ID="your_r2_access_key"
   R2_SECRET_ACCESS_KEY="your_r2_secret_key"
   R2_BUCKET="mediadock-downloads"
   SIGNED_URL_TTL_SECONDS=21600
   STORAGE_QUOTA_BYTES=10737418240
   ```
3. Restart containers: `docker compose up -d`

When R2 is enabled:
* Completed media is automatically uploaded to R2.
* Clients receive direct presigned download URLs.
* Transfers go directly from cloud object storage to the Android device, bypassing the application server.
* The automatic retention rule purges files older than `FILE_TTL_MINUTES` (default 3 days).

---

## 4. Upgrades & Database Migrations

When updating the backend code:
```bash
git pull origin main
cd backend/snagbox
pnpm install
pnpm prisma:deploy
cd ../..
docker compose up -d --build api
```

# syntax=docker/dockerfile:1

FROM node:22-bookworm-slim AS build
RUN apt-get update \
  && apt-get install -y --no-install-recommends ca-certificates openssl \
  && rm -rf /var/lib/apt/lists/*
ENV COREPACK_ENABLE_DOWNLOAD_PROMPT=0
RUN corepack enable && corepack prepare pnpm@10.33.4 --activate
WORKDIR /app
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY packages/shared/package.json packages/shared/package.json
RUN pnpm install --frozen-lockfile
COPY . .
ENV DATABASE_URL=postgresql://postgres:postgres@localhost:5432/downloader
RUN pnpm --filter @snagbox/shared build \
  && pnpm exec prisma generate \
  && pnpm exec nest build

FROM node:22-bookworm-slim AS runtime
RUN apt-get update \
  && apt-get install -y --no-install-recommends ca-certificates curl ffmpeg python3 python3-pip \
  && python3 -m pip install --no-cache-dir --break-system-packages gallery-dl==1.32.20 \
  && rm -rf /var/lib/apt/lists/*
# Pinned yt-dlp. Node 22 (this image) is the JS runtime for YouTube's n challenge.
ARG YTDLP_VERSION=2026.10.26
ARG TARGETARCH
RUN set -eu; \
  arch="${TARGETARCH:-}"; \
  if [ -z "$arch" ]; then \
    case "$(uname -m)" in \
      x86_64) arch=amd64 ;; \
      aarch64|arm64) arch=arm64 ;; \
      *) echo "unsupported architecture: $(uname -m)" >&2; exit 1 ;; \
    esac; \
  fi; \
  case "$arch" in \
    amd64) asset=yt-dlp_linux; sum=767ba9dce7052aa196f8b12652f433d76de128ba168966fa84b6394d5e6c6795 ;; \
    arm64) asset=yt-dlp_linux_aarch64; sum=e87e9533e89fe5889abaa21f5114dfe7756d6aa6219b83551add04d48b18e483 ;; \
    *) echo "unsupported architecture: $arch" >&2; exit 1 ;; \
  esac; \
  curl -fsSL -o /usr/local/bin/yt-dlp \
    "https://github.com/yt-dlp/yt-dlp/releases/download/${YTDLP_VERSION}/${asset}"; \
  echo "${sum}  /usr/local/bin/yt-dlp" | sha256sum -c -; \
  chmod 755 /usr/local/bin/yt-dlp; \
  yt-dlp --version
# The official Node image already provides the unprivileged `node` user at UID
# 1000, which also satisfies Hugging Face Spaces' runtime requirement.
ENV HOME=/home/node
WORKDIR /home/node/app

COPY --chown=node:node --from=build /app/package.json /app/pnpm-workspace.yaml /app/pnpm-lock.yaml /app/prisma.config.ts ./
COPY --chown=node:node --from=build /app/dist ./dist
COPY --chown=node:node --from=build /app/prisma ./prisma
COPY --chown=node:node --from=build /app/packages ./packages
COPY --chown=node:node --from=build /app/node_modules ./node_modules
COPY --chown=node:node --from=build /app/assets ./assets

RUN mkdir -p /home/node/app/downloads /home/node/app/temp && chown -R node:node /home/node

USER node

LABEL org.opencontainers.image.title="Snagbox" \
      org.opencontainers.image.description="Paste a link, snag the media: a self-hostable downloader API." \
      org.opencontainers.image.url="https://github.com/fabwaseem/snagbox" \
      org.opencontainers.image.source="https://github.com/fabwaseem/snagbox" \
      org.opencontainers.image.licenses="MIT" \
      org.opencontainers.image.version="2.0.0"
ENV NODE_ENV=production \
  PORT=7860 \
  YTDLP_PATH=/usr/local/bin/yt-dlp \
  FFMPEG_PATH=/usr/bin/ffmpeg \
  GALLERY_DL_PATH=/usr/local/bin/gallery-dl \
  YTDLP_VERSION=2026.10.26
EXPOSE 7860
# exec replaces the shell so node is PID 1 and receives SIGTERM on rollout.
CMD ["sh", "-c", "./node_modules/.bin/prisma migrate deploy && exec node dist/main"]

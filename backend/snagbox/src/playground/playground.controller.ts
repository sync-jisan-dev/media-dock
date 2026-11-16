import { Controller, Get, Header, VERSION_NEUTRAL } from "@nestjs/common";
import { SkipThrottle } from "@nestjs/throttler";
import { Public } from "../common/decorators/public.decorator";

const PAGE = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>MediaDock — Download your media</title>
<link rel="icon" href="/assets/brand/mediadock-mark.svg" type="image/svg+xml">
<link rel="stylesheet" href="/playground.css">
</head>
<body>
<header class="top">
  <a class="wordmark" href="/"><svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M16.9504 12.1817C17.1981 12.814 16.5076 13.5726 15.1267 15.0899C13.6702 16.6902 12.9201 17.4904 12 17.5C11.0799 17.4904 10.3298 16.6902 8.87331 15.0899C7.49239 13.5726 6.80193 12.814 7.04964 12.1817C7.05868 12.1586 7.06851 12.1359 7.0791 12.1135C7.34928 11.542 8.24477 11.5029 10 11.5002V4.99998C10 4.53501 10 4.30253 10.0511 4.11179C10.1898 3.59414 10.5941 3.1898 11.1118 3.05111C11.3025 3 11.535 3 12 3C12.4649 3 12.6974 3 12.8882 3.05111C13.4058 3.1898 13.8102 3.59414 13.9489 4.11179C14 4.30253 14 4.53501 14 4.99998V11.5002C15.7552 11.5029 16.6507 11.542 16.9209 12.1135C16.9315 12.1359 16.9413 12.1586 16.9504 12.1817Z"/><path d="M5.00006 21H19.0001"/></svg><span>MediaDock</span></a>
  <p class="tag">Your media. On your terms.</p>
</header>
<main>
  <h1>Save something<br>worth keeping.</h1>
  <p>Paste a public link and choose your format. Your download will appear here when it’s ready.</p>
  <form id="form">
    <label for="url">URL</label>
    <input id="url" name="url" type="url" required placeholder="https://www.youtube.com/watch?v=...">
    <label for="kind">Kind</label>
    <select id="kind" name="kind">
      <option value="video">Video</option>
      <option value="audio">Audio</option>
    </select>
    <label for="format">Format</label>
    <select id="format" name="format">
      <option>mp4</option><option>webm</option><option>mkv</option><option>gif</option>
      <option>mp3</option><option>m4a</option><option>opus</option>
    </select>
    <label for="key">API key (optional)</label>
    <input id="key" name="key" type="password" autocomplete="off" placeholder="sb_...">
    <button type="submit"><svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M16.9504 12.1817C17.1981 12.814 16.5076 13.5726 15.1267 15.0899C13.6702 16.6902 12.9201 17.4904 12 17.5C11.0799 17.4904 10.3298 16.6902 8.87331 15.0899C7.49239 13.5726 6.80193 12.814 7.04964 12.1817C7.05868 12.1586 7.06851 12.1359 7.0791 12.1135C7.34928 11.542 8.24477 11.5029 10 11.5002V4.99998C10 4.53501 10 4.30253 10.0511 4.11179C10.1898 3.59414 10.5941 3.1898 11.1118 3.05111C11.3025 3 11.535 3 12 3C12.4649 3 12.6974 3 12.8882 3.05111C13.4058 3.1898 13.8102 3.59414 13.9489 4.11179C14 4.30253 14 4.53501 14 4.99998V11.5002C15.7552 11.5029 16.6507 11.542 16.9209 12.1135C16.9315 12.1359 16.9413 12.1586 16.9504 12.1817Z"/><path d="M5.00006 21H19.0001"/></svg><span>Download media</span></button>
  </form>
  <progress id="bar" value="0" max="100" hidden></progress>
  <p id="log" role="status" aria-live="polite"></p>
  <p id="file"></p>
</main>
<script src="/playground.js"></script>
</body>
`;

const CSS = `@font-face { font-family: Inter; src: url('/assets/fonts/inter-variable.ttf') format('truetype'); font-weight: 100 900; font-display: swap; }
:root { color-scheme: light dark; --ink: #111; --paper: #f7f7f7; --mist: #666; --line: #ddd; --card: #fff; --button: #111; --button-ink: #fff; font-family: Inter, sans-serif; }
* { box-sizing: border-box; }
body { margin: 0; min-height: 100vh; background: var(--paper); color: var(--ink); }
.top { max-width: 1120px; margin: auto; display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding: 28px 32px; border-bottom: 1px solid var(--line); }
.wordmark { display: inline-flex; align-items: center; gap: 12px; text-decoration: none; color: var(--ink); font-size: 22px; font-weight: 700; letter-spacing: -.7px; }
.wordmark svg { width: 30px; height: 30px; }
.tag { margin: 0; color: var(--mist); font-size: 13px; }
main { max-width: 560px; margin: 56px auto; padding: 0 24px 40px; }
h1 { font-size: clamp(34px, 7vw, 48px); line-height: 1.12; letter-spacing: -.055em; font-weight: 650; margin: 0 0 16px; }
main > p { color: var(--mist); font-size: 14px; line-height: 1.65; margin-bottom: 24px; }
form { padding: 24px; border: 1px solid var(--line); border-radius: 24px; background: var(--card); }
label { display: block; margin: 18px 0 8px; font-size: 12px; font-weight: 600; }
label:first-child { margin-top: 0; }
input, select, button { font: inherit; width: 100%; min-height: 48px; padding: 12px 14px; border-radius: 12px; border: 1px solid var(--line); background: var(--card); color: inherit; font-size: 14px; }
input:focus-visible, select:focus-visible, button:focus-visible, a:focus-visible { outline: 2px solid var(--ink); outline-offset: 3px; }
button { display: flex; gap: 10px; align-items: center; justify-content: center; margin-top: 24px; cursor: pointer; background: var(--button); color: var(--button-ink); border: 0; font-weight: 600; transition: transform 160ms ease, opacity 160ms ease; }
button:active { transform: scale(.98); }
button:disabled { opacity: .6; cursor: progress; }
progress { width: 100%; height: 8px; margin-top: 24px; accent-color: var(--ink); }
#log { white-space: pre-wrap; }
#file a { display: block; border-radius: 14px; background: var(--button); color: var(--button-ink); padding: 16px; text-decoration: none; text-align: center; font-weight: 600; }
@media (prefers-color-scheme: dark) { :root { --paper: #080808; --ink: #f8f8f8; --card: #141414; --line: #393939; --mist: #b8b8b8; --button: #fff; --button-ink: #111; } }
@media (max-width: 480px) { .top { padding: 22px 24px; } .tag { display: none; } main { margin-top: 36px; } form { padding: 20px; } }
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { transition: none !important; animation: none !important; } }
`;

const SCRIPT = `const form = document.querySelector("#form");
const log = document.querySelector("#log");
const bar = document.querySelector("#bar");
const file = document.querySelector("#file");
const keyInput = document.querySelector("#key");
keyInput.value = sessionStorage.getItem("apiKey") || "";
form.addEventListener("submit", async (event) => {
  event.preventDefault();
  const button = form.querySelector("button");
  if (button.disabled) return;
  button.disabled = true;
  button.querySelector("span").textContent = "Adding to downloads…";
  try {
  file.textContent = "";
  bar.hidden = false;
  bar.value = 0;
  log.textContent = "Queuing…";
  const key = keyInput.value.trim();
  if (key) sessionStorage.setItem("apiKey", key);
  const headers = { "content-type": "application/json" };
  if (key) headers["x-api-key"] = key;
  const body = {
    url: document.querySelector("#url").value.trim(),
    kind: document.querySelector("#kind").value,
    format: document.querySelector("#format").value,
  };
  const created = await fetch("/v2/downloads", { method: "POST", headers, body: JSON.stringify(body) });
  const job = await created.json();
  if (!created.ok) {
    log.textContent = job.message || "Request failed";
    return;
  }
  const stream = await fetch("/v2/downloads/" + job.id + "/events", { headers });
  const reader = stream.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  const handle = (data) => {
    if (data.type === "progress") {
      bar.value = data.percentage || 0;
      log.textContent = "Downloading " + Math.round(data.percentage || 0) + "%";
    } else if (data.type === "status") {
      log.textContent = data.status;
    } else if (data.type === "complete") {
      bar.value = 100;
      log.textContent = "Ready";
      const link = document.createElement("a");
      link.href = data.job.fileUrl;
      link.textContent = data.job.filename || "Download file";
      file.replaceChildren(link);
      return true;
    } else if (data.type === "error") {
      log.textContent = data.message || "Failed";
      return true;
    }
    return false;
  };
  for (;;) {
    const chunk = await reader.read();
    if (chunk.done) break;
    buffer += decoder.decode(chunk.value, { stream: true });
    const parts = buffer.split("\\n\\n");
    buffer = parts.pop() || "";
    let stop = false;
    for (const part of parts) {
      const line = part.split("\\n").find((item) => item.startsWith("data:"));
      if (!line) continue;
      if (handle(JSON.parse(line.slice(5).trim()))) stop = true;
    }
    if (stop) break;
  }
  await reader.cancel().catch(() => {});
  } catch (error) {
    log.textContent = error instanceof Error ? error.message : "The download could not be started. Try again.";
  } finally {
    button.disabled = false;
    button.querySelector("span").textContent = "Download media";
  }
});
`;

@Controller({ version: VERSION_NEUTRAL })
export class PlaygroundController {
  @Public()
  @SkipThrottle()
  @Get()
  @Header("Content-Type", "text/html; charset=utf-8")
  page(): string {
    return PAGE;
  }

  @Public()
  @SkipThrottle()
  @Get("playground.css")
  @Header("Content-Type", "text/css; charset=utf-8")
  styles(): string {
    return CSS;
  }

  @Public()
  @SkipThrottle()
  @Get("playground.js")
  @Header("Content-Type", "application/javascript; charset=utf-8")
  script(): string {
    return SCRIPT;
  }
}

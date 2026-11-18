# MediaDock 1.2 — Home and browser

## Home

Home is a centered search-and-paste landing page with favorite platforms immediately
below the search field. The promotional hero, counters and collection feed have been
removed. Saved links and My collection remain available as small shortcuts.

When Home gains window focus it checks for a web link in the clipboard. The suggestion
can be dismissed, and merely displaying it does not inspect or download the link.
Tapping **Download this link** opens media preparation and queues an available video or
audio format using supported download defaults. Image-only pages still offer image
selection. A quick request is recorded as attempted before submission, preventing a
screen recreation or metadata retry from starting the same request again.

Quick video/audio requests skip the unrelated image-extraction request. Manual inspection
retains the video/audio/image format chooser.

## Waiting experience

Preparation displays an indeterminate signal animation, source hostname, elapsed time,
rotating tips and an optional Catch the signal game. The game never changes transfer
progress. Longer waits show an explanation and retain the normal Back action. Preparing
metadata and submitting a quick download have different labels reflecting actual state.
The queue also offers **Pass the time while we prepare**, reopening the waiting experience
while a job runs; it closes when that job completes or fails.

## Browser

- Up to 12 persistent tabs, with a tab picker, new-tab and close-tab controls.
- Active tab, page title, URL, desktop mode, text size and scroll position are stored locally.
- In-memory WebView back/forward bundles restore when switching tabs or leaving Browse.
  After process death, saved tabs reopen their URLs; full navigation history is not claimed.
- First- and third-party cookies are enabled and flushed on page completion and pause/disposal.
- DOM storage and the standard WebView disk cache remain enabled. Leaving Browse does not
  clear cookies, local storage or cached website data.
- Address/search bar, site information, navigation, reload/stop, browser start page,
  bookmarks, recent pages, find-in-page, desktop mode and text-size controls.
- Native document picker for website file inputs and expanded web-video custom views.
- A shared activity-scoped browser session keeps Browse and Home-opened platform pages
  in the same tab workspace.

Cookies are kept inside the app's own WebView data. They are not copied into backend
extraction providers. Websites determine login expiry and may restrict embedded-browser
authentication. There is no claim to implement Chrome password sync or its full extension
and account ecosystem.

## Android references

- [Focused clipboard access](https://developer.android.com/about/versions/10/privacy/changes)
- [CookieManager persistence](https://developer.android.com/reference/android/webkit/CookieManager)
- [WebView state and storage](https://developer.android.com/reference/android/webkit/WebView)

Browser persistence tests use an ephemeral loopback HTTP fixture inside the emulator,
not a local-PC production backend or a paid media provider.

## Validation

- 46 unit tests and 12 Pixel 10 Pro emulator tests passed.
- Emulator tests cover clipboard confirmation UI, the waiting game, cookie/localStorage
  retention between WebViews, durable tab metadata and restored back/forward history.
- A live test copied the supplied YouTube video URL in Browse, confirmed Home's suggestion,
  and automatically queued a default download. Detection took 28.8 seconds and queue
  submission 697 ms in this run; these are observations rather than latency guarantees.
- The resulting 32,362,436-byte MP4 was saved to Android MediaStore at 640×360 with H.264
  video/AAC audio and a duration of 872.77 seconds. Opening/ending decode checks passed,
  and playback was observed in the Pixel emulator.
- The final release build and release lint checks passed. The updated 1.2.0 APK was
  installed over the existing release app.

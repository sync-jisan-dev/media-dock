# MediaDock 1.1 — 50 additions

Native Android implementation with Inter typography, HugeIcons, monochrome surfaces,
clear selection states, and shorter tab transitions. Existing navigation fixes are retained.

## Saved links

1. **Saved-link inbox** — Home → Your saved links; keep links without starting a download.
2. **Bulk link capture** — Add links extracts HTTP/HTTPS URLs, validates and deduplicates them.
3. **Pinned links** — Pin important items to the top of the inbox.
4. **Editable link titles** — Link actions → Edit title.
5. **Link search** — Search saved titles and URLs.
6. **Link sharing** — Copy or share an inbox link using Android's clipboard/share sheet.
7. **JSON link backup** — Export JSON to a document location chosen by the user.
8. **JSON link restore** — Import a backup with validation, a 512 KB limit and a 500-link import limit.
9. **Download from saved links** — Open the existing media inspection flow directly from an inbox item.
10. **Recent browsing history** — Recent tab records up to 100 visited pages; users can save or clear them.

## Browser

11. **Continue browsing** — Reopen the most recent page from the Browse dashboard.
12. **Page bookmarks** — Browser tools → Bookmark page saves the current URL and title.
13. **Platform search** — Filter the supported-platform directory by platform name or domain.
14. **Find in page** — Search text, see match counts, and move between matches.
15. **Desktop-site mode** — Switch the current page between mobile and desktop rendering.
16. **Copy current page URL** — Browser tools → Copy page link.
17. **Share current page** — Browser tools → Share page.
18. **External browser handoff** — Open the current HTTP/HTTPS page in another installed app.
19. **Stop loading** — The reload control becomes Stop while a page is loading.
20. **Page error recovery** — Main-frame load failures display an explanation and Retry page action.

## Library

21. **Favorite files** — Favorite/unfavorite media without moving the saved file.
22. **Favorites view** — Combine a favorites-only filter with search and media types.
23. **Date sorting** — Switch between newest and oldest downloads.
24. **Title sorting** — Sort alphabetically using edited display titles.
25. **Size sorting** — Order media by largest or smallest file size.
26. **Grid view** — Switch between a two-column grid and a list; the preference persists.
27. **Compact density** — Use compact or comfortable library rows; the preference persists.
28. **Multi-selection** — Select mode, long press, select all, clear selection and Back-to-exit.
29. **Share multiple files** — Send selected content URIs through Android's multi-file share sheet.
30. **Bulk favorites** — Favorite or unfavorite the selected files together.
31. **Editable media titles** — Change a library display title while retaining the actual filename.
32. **File notes** — Add/edit/remove searchable notes, up to 2,000 characters per file.
33. **File details** — View type, size, date, original URL, content URI and note.
34. **Copy original source** — Copy the original source URL from file actions/details.
35. **Reopen original source** — Open a library item's original page in the in-app browser.
36. **Library manifest export** — Export displayed or selected file metadata as JSON using Android's document picker.
37. **Named collections** — Create and rename persistent collections.
38. **Collection membership** — Add selected files to a collection or remove them from it.
39. **Collection filtering** — Combine collection, type, favorite and search filters.
40. **Delete collection only** — Remove a collection after confirmation while retaining its media files.
41. **Undo library removal** — Removing library entries keeps files on disk and offers a Snackbar Undo.

## Queue

42. **Queue search** — Search current/failed downloads by title or source URL.
43. **Queue status filters** — All, Running, Paused and Failed views; failures stay visible.
44. **Pause all transfers** — Pause phone file transfers; server-side processors are not falsely marked paused.
45. **Resume paused transfers** — Restart paused file transfers together; the UI explains the restart behavior.
46. **Retry failed jobs** — Requeue supported video/audio/image jobs; retain a failed record if enqueue fails and prevent duplicate taps. GIF retries request a new clip range.
47. **Queue summary** — Aggregate current transfer speed and downloaded bytes.

## Settings

48. **Connection diagnostics** — Test backend health, display latency/timeouts, and share a report containing app/Android versions, backend hostname, connection result and storage counts. Credentials and full backend URLs are excluded.
49. **Storage dashboard** — Display device free/total capacity and library file size with manual refresh.
50. **Working download defaults** — Persist preferred video quality/audio format and apply them only when the source supports them.

## Additional fixes and behavior

- Wi-Fi-only and transfer concurrency preferences are enforced by the transfer engine.
- Paused transfers restart from the beginning; byte-range resume is not claimed.
- Truncated/empty transfers fail instead of being recorded as completed.
- Incomplete outputs are cleaned up, and transfer responses/streams are closed.
- Library records now retain the original source URL rather than a temporary server file URL.
- Custom SAF folders are not incorrectly finalized as MediaStore entries.
- Settings can be reset without removing saved media, links or collections.
- Shared URLs are consumed once and lead to Home instead of reopening on every Home visit.
- About displays the actual build version. Cloud hosting is not labeled as guaranteed 24/7.

## Persistence and scope

Links, notes, favorites, collections and library display preferences use a separate local
DataStore. The existing media database schema remains unchanged. Exported library manifests
contain metadata, not media binaries. Deleting a collection/library entry does not delete
the downloaded file.

Download extraction still depends on the existing backend, source website and provider
availability. This update adds no paid subscriptions, hosting or provider calls on app launch.
Connection diagnostics make only a health request. Automated tests use isolated debug data
and transfer fixtures without calling paid extraction providers.

## Validation on November 18, 2026

- 38 unit tests passed, covering URL handling/backups, composed library filters/sorts,
  queue retries, preferred formats, transfer limits and existing download behavior.
- 8 emulator tests passed on Pixel 10 Pro API 37, covering navigation, saved-link UI,
  collection UI, byte-for-byte file transfer and truncated-transfer rejection.
- Release build and release lint checks passed.
- Home, saved links, library and settings were inspected in the emulator. Appearance
  switching was checked in light/dark themes; original dark preference was restored.
- In-app cloud health check succeeded with a measured 1,346 ms response.
- External share targets/document providers depend on installed Android apps. The
  automated fixture tests do not constitute live extraction tests for every website.

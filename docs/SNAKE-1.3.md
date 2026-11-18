# MediaDock 1.3 — Snake while waiting

Snake replaces the dot-catching activity in media preparation and download waiting screens.
The board is visible immediately; gameplay begins only when Start Snake is tapped.

- Swipe controls and accessible directional buttons.
- Score, locally saved best score, pause, restart and play again.
- Wall/self collisions end a round; food grows the snake and increases the speed gradually.
- Backgrounding the app pauses the game. Game logic does not use network requests or
  alter download speed/progress.
- The queue opens the waiting screen for new active jobs. Closing it keeps downloads
  running; the queue shortcut can reopen it. Completed, failed, cancelled or paused jobs
  close that job's waiting screen.
- Actual phone-transfer percentage is shown only once bytes are being received. Server
  preparation remains indeterminate; the game score never represents download progress.

Validation: 50 unit tests and 12 Pixel 10 Pro emulator tests passed, including Snake
growth, direction reversal, wall/self collision, vacated-tail movement and game controls.

# Third-party engine notices

MediaDock interoperates with the following independent projects. Their names do not imply endorsement.

## gallery-dl

- Project: https://codeberg.org/mikf/gallery-dl
- Documentation: https://gdl-org.github.io/
- Version pinned in the production image: `1.32.20`
- License: GNU General Public License v2.0 only

gallery-dl is installed as a separate Python command-line program inside the backend container and invoked as a subprocess. Its upstream source and license are available from the project link above.

## Cobalt

- Project: https://github.com/imputnet/cobalt
- API documentation: https://github.com/imputnet/cobalt/blob/main/docs/api.md
- License: GNU Affero General Public License v3.0

MediaDock does not bundle Cobalt or use the public hosted Cobalt API. It provides an optional HTTP client for an operator-owned, separately deployed Cobalt instance. Operators are responsible for securing that instance and complying with its license and service documentation.

## yt-dlp and FFmpeg

MediaDock also invokes separately distributed yt-dlp and FFmpeg executables. Consult their upstream projects for the license terms applicable to the exact builds distributed with your deployment.

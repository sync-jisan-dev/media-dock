# MediaDock identity

The geometric hub is an original adaptation of the reference's pixel-symbol/wordmark
direction. A negative-space play triangle connects it to media, and an lime north
pixel identifies the primary lockup.

- `mediadock-wordmark-primary.svg/png`: black wordmark with lime accent; transparent.
- `mediadock-wordmark-reversed.svg/png`: white wordmark with lime accent; transparent.
- `mediadock-wordmark-black.svg/png`: black wordmark with lime accent; transparent.
- `mediadock-mark.svg/png`: icon-only black symbol with lime accent; transparent.
- `mediadock-mark-reversed.svg/png`: lime symbol with white accent; transparent.
- `mediadock-app-icon.svg/png`: black app tile with an lime/white symbol.
- `mediadock-brand-preview.svg/png`: presentation with both lockups and brand palette.

Wordmark letters are outlined from the project's Inter Medium font, so exported SVGs
do not need an installed font. SVGs are the master files; PNGs are convenient exports.
The full wordmark belongs in headers. Use only the symbol for launcher icons and other
small square placements. Keep at least one satellite-square width of clear space.

Palette: black `#000000`, lime `#D0FF94`, black `#000000`, pastel pink `#FBC5EB`, hot pink `#F34D91`, white `#FFFFFF`.

To regenerate vectors, install `fonttools` and run `python scripts/generate-brand-assets.py`.
For PNGs, install `@resvg/resvg-js` in an external tool directory and expose it through
`NODE_PATH`, then run `node scripts/render-brand-assets.cjs`.

Android uses native vector resources for the wordmark, symbols and adaptive launcher.
The prior launcher artwork is retained as unused source; the launcher references the
new `mediadock_launcher_*` assets.

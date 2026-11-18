# MediaDock Cosmic Sci-Fi & Sage Mint Visual System — 1.5.0

Inspired by retro-futuristic space exploration, scientific research, and the "Useful Features" editorial story card reference.

## 1. Color Palette

| Color Name | Hex Code | Primary Purpose in UI |
|---|---|---|
| **Sage Mint** | `#D1F1D9` | Application canvas, root background, soft borders |
| **Mint Tint** | `#E2F7E9` | Surface panels, illustration container backgrounds |
| **Forest Dark** | `#0D3328` | Primary ink, dark capsules, primary action buttons, dark mode surface |
| **Forest Muted** | `#234B3D` | Secondary text, subtle captions, borders |
| **Vibrant Mint** | `#34D399` | Navigation active indicator, holographic rings, badge accents |
| **Cosmic Violet** | `#7C5CFC` | Secondary cosmic accent, tech console accents, food orb in Snake |
| **White** | `#FFFFFF` | Card surfaces, reversed text on dark capsules, icons |

## 2. Typography

* **Headlines & Display:** Expressive, high-contrast **Editorial Serif** (`FontFamily.Serif` / Noto Serif) applied to `displayLarge` through `titleMedium`, matching the literary character of *"Useful Features"*, *"Embark on your Ultimate Space Journey"*, and *"CosmicLinks"*.
* **Body, Controls & Labels:** Clean geometric sans-serif (**Inter**) with crisp tracking, medium weights for labels, and high legibility across Android density buckets.

## 3. UI Components & Layout Language

1. **Feature Capsules:** Dark green pill buttons (`#0D3328`) with circular icon badges and two-line descriptions (*Premium Quality*, *Easily Customizable*, *High-Res Png's*, *Multiple Formats*).
2. **Organic Blob Illustrations:** Transparent cutouts of astronauts, lunar rovers, extraterrestrial researchers, and quantum stations.
3. **Animated GIFs:**
   * `illustration_cosmic_pulse.gif`: Smoothly expanding holographic rings and audio frequency waves for the media preparation waiting experience.
   * `illustration_rover_stream.gif`: Space rover streaming data packets across alien dunes for active downloads.
4. **Coil GIF Engine:** Integrated `io.coil-kt:coil-gif:2.5.0` with `ImageDecoderDecoder` (API 28+) and `GifDecoder` fallback.

## 4. Verification

* 50 of 50 Android unit tests pass cleanly.
* Debug APK assembled: 18.8 MB with all assets and animated drawables.

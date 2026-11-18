# Editorial illustration set

Four original transparent PNG illustrations generated using built-in `image_gen`.
The uploaded story-card image was used as style/color inspiration, not reproduced.
Exact prompts are recorded in `generation-prompts.json`.

| Asset | App placement |
| --- | --- |
| downloads.png | Empty Queue and preparation tips |
| library.png | Empty My files and saved links |
| browse.png | Browser start page |
| connection.png | Connection/analysis failure states |

The production copies are in Android `res/drawable-nodpi/illustration_*.png`.
Coil downsamples the artwork for display and shares the app image cache. The
`AppIllustration` component adds a subtle native floating animation; these are
PNG assets, not animated GIFs.

UI anchors are pastel pink #FBC5EB, lime #D0FF94, hot pink #F34D91, black and white.
Warm peach is used for illustrated skin. Inter is retained with bolder/larger
headlines, inspired by the reference's typographic hierarchy.

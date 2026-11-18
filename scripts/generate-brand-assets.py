"""Generate outlined MediaDock SVGs and Android vectors from the project's Inter font.
Requires fonttools. PNG exports are rendered by render-brand-assets.cjs.
"""
from pathlib import Path
import json
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "assets/brand"
RES = ROOT / "android/app/src/main/res/drawable"
OUT.mkdir(parents=True, exist_ok=True)
FOREST_DARK, VIBRANT_MINT, WHITE = "#0D3328", "#34D399", "#FFFFFF"
SAGE_MINT, COSMIC_VIOLET = "#D1F1D9", "#7C5CFC"

MARK = "M12 12H28V20H36V12H52V28H44V36H52V52H36V44H28V52H12V36H20V28H12Z M28 25L39 32L28 39Z"
DOTS = "M0 28H8V36H0Z M28 56H36V64H28Z M56 28H64V36H56Z"
NORTH = "M28 0H36V8H28Z"
font = TTFont(ROOT / "android/app/src/main/res/font/inter_variable.ttf")
axes = {a.axisTag: a.defaultValue for a in font["fvar"].axes}
axes["wght"] = 500
font = instantiateVariableFont(font, axes, inplace=False)
glyphs = font.getGlyphSet()
cmap = font.getBestCmap()
scale = 104 / font["head"].unitsPerEm
x = 192
parts = []
for character in "MediaDock":
    glyph = glyphs[cmap[ord(character)]]
    pen = SVGPathPen(glyphs)
    glyph.draw(TransformPen(pen, (scale, 0, 0, -scale, x, 124)))
    parts.append(pen.getCommands())
    x += (glyph.width - 22) * scale
WIDTH, HEIGHT = round(x + 28), 176
WORD = " ".join(parts)

def mark_svg(ink, accent):
    return f'<path fill="{ink}" fill-rule="evenodd" d="{MARK}"/><path fill="{ink}" d="{DOTS}"/><path fill="{accent}" d="{NORTH}"/>'

def svg(body, w, h):
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">{body}</svg>'

def lockup(ink, accent):
    return f'<g transform="translate(24 24) scale(2)">{mark_svg(ink, accent)}</g><path fill="{ink}" d="{WORD}"/>'

for name, ink, accent in [("primary",FOREST_DARK,VIBRANT_MINT),("reversed",WHITE,VIBRANT_MINT),("black",FOREST_DARK,VIBRANT_MINT)]:
    (OUT / f"mediadock-wordmark-{name}.svg").write_text(svg(lockup(ink,accent), WIDTH,HEIGHT), encoding="utf-8")
(OUT / "mediadock-mark.svg").write_text(svg(mark_svg(FOREST_DARK,VIBRANT_MINT),64,64),encoding="utf-8")
(OUT / "mediadock-mark-reversed.svg").write_text(svg(mark_svg(VIBRANT_MINT,WHITE),64,64),encoding="utf-8")
(OUT / "mediadock-app-icon.svg").write_text(svg(f'<rect width="1024" height="1024" rx="224" fill="{FOREST_DARK}"/><g transform="translate(208 208) scale(9.5)">{mark_svg(VIBRANT_MINT,WHITE)}</g>',1024,1024),encoding="utf-8")

def android_vector(body, w, h):
    return f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="{w}dp" android:height="{h}dp" android:viewportWidth="{w}" android:viewportHeight="{h}">{body}</vector>\n'

def mark_android(ink, accent):
    return f'<path android:fillColor="{ink}" android:fillType="evenOdd" android:pathData="{MARK}"/><path android:fillColor="{ink}" android:pathData="{DOTS}"/><path android:fillColor="{accent}" android:pathData="{NORTH}"/>'

for suffix,ink,accent in [("dark",FOREST_DARK,VIBRANT_MINT),("light",VIBRANT_MINT,WHITE)]:
    (RES / f"mediadock_mark_{suffix}.xml").write_text(android_vector(mark_android(ink,accent),64,64),encoding="utf-8")
for suffix,ink in [("dark",FOREST_DARK),("light",WHITE)]:
    body=f'<group android:translateX="24" android:translateY="24" android:scaleX="2" android:scaleY="2">{mark_android(ink,VIBRANT_MINT)}</group><path android:fillColor="{ink}" android:pathData="{WORD}"/>'
    (RES / f"mediadock_wordmark_{suffix}.xml").write_text(android_vector(body,WIDTH,HEIGHT),encoding="utf-8")
(RES / "mediadock_launcher_foreground.xml").write_text(android_vector(f'<group android:translateX="22" android:translateY="22">{mark_android(VIBRANT_MINT,WHITE)}</group>',108,108),encoding="utf-8")
(RES / "mediadock_launcher_background.xml").write_text(android_vector(f'<path android:fillColor="{FOREST_DARK}" android:pathData="M0 0H108V108H0Z"/>',108,108),encoding="utf-8")

board = f'<rect width="1600" height="1040" fill="{SAGE_MINT}"/>'
board += f'<text x="96" y="72" font-family="Arial" font-size="20" letter-spacing="4" fill="{FOREST_DARK}">MEDIADOCK / COSMIC IDENTITY</text>'
board += f'<g transform="translate(90 160) scale(1.7)">{lockup(FOREST_DARK,VIBRANT_MINT)}</g>'
board += f'<rect x="64" y="510" width="1472" height="270" rx="32" fill="{FOREST_DARK}"/>'
board += f'<g transform="translate(90 530) scale(1.25)">{lockup(WHITE,VIBRANT_MINT)}</g>'
board += f'<g transform="translate(1280 574) scale(2.1)">{mark_svg(VIBRANT_MINT,WHITE)}</g>'
for index,(label,color) in enumerate([("SAGE MINT",SAGE_MINT),("VIBRANT MINT",VIBRANT_MINT),("COSMIC VIOLET",COSMIC_VIOLET),("FOREST DARK",FOREST_DARK),("WHITE",WHITE)]):
    left=96+index*288
    board += f'<rect x="{left}" y="858" width="248" height="54" rx="8" fill="{color}" stroke="#B5DAC5"/><text x="{left}" y="946" font-family="Arial" font-size="16" letter-spacing="1" fill="{FOREST_DARK}">{label}</text><text x="{left}" y="974" font-family="Arial" font-size="16" fill="{FOREST_DARK}">{color}</text>'
(OUT / "mediadock-brand-preview.svg").write_text(svg(board,1600,1040),encoding="utf-8")
(OUT / "brand-metadata.json").write_text(json.dumps({"name":"MediaDock","wordmark_viewbox":[WIDTH,HEIGHT],"font":"Inter Medium, outlined","palette":{"sage_mint":SAGE_MINT,"vibrant_mint":VIBRANT_MINT,"cosmic_violet":COSMIC_VIOLET,"forest_dark":FOREST_DARK,"white":WHITE}},indent=2),encoding="utf-8")
print(f"Generated cosmic wordmark {WIDTH}x{HEIGHT}, symbols, icon and Android vectors")

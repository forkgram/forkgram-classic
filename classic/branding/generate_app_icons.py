#!/usr/bin/env python3
"""Regenerate the Forkgram app-icon assets from forkgram-app-icon.svg.

This is the single source of truth for the launcher icon set. Edit the SVG (or
the size constants below), re-run this script, and rebuild.

What it produces, and the principle behind each sprite:
  * icon_01_background_sa          - background ONLY (the blue gradient square).
  * icon_01_foreground[_round]     - foreground ONLY (the fork on transparent),
                                     used as the adaptive <foreground> over a
                                     background that carries icon_background_clip
                                     (a white matte with a ~0.53R transparent
                                     disc). FORK_FG_HOME is kept small so the
                                     fork sits inside that disc instead of
                                     spilling past the ring on launchers that
                                     render the adaptive icon.
  * icon_01_foreground_sa          - foreground for the _sa / _adaptive adaptive
                                     XML, which sits over the full-bleed gradient
                                     (no clip matte) and is also the in-app icon
                                     picker preview, so it can be larger.
  * icon_01_launcher[_round]       - legacy composite PNG: full-bleed gradient
                                     circle + fork + white outline ring.
  * icon_01_launcher_sa/_adaptive  - same composite without the ring.
  * icon_fork.xml                  - monochrome (themed-icon) vector, built from
                                     the SVG fork polygons (exact, no tracing).
  * splash_fork_320.xml            - splash vector: solid disc + fork.
  * notification / menu_fork       - white fork silhouettes (menu_fork is tinted
                                     SRC_IN in code, so only its alpha matters).
  * book_logo                      - rounded-square gradient + fork.

Rendering: ImageMagick (built against librsvg) rasterises the gradient and the
fork to high-res masters; Pillow does the compositing, masks and rings; the two
Android vectors are emitted directly from the source polygons.

Requirements: `magick` (ImageMagick with the RSVG delegate) and Python Pillow.

Usage:
    python3 generate_app_icons.py            # write in place into the res tree
    python3 generate_app_icons.py /tmp/out   # write into a staging dir instead
"""
import math
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
SRC = HERE / "forkgram-app-icon.svg"
RES = (HERE / ".." / ".." / "TMessagesProj" / "src" / "main" / "res").resolve()
OUT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else RES
TMP = Path(tempfile.mkdtemp(prefix="forkgram-icons-"))
SS = 4  # supersample factor for raster compositing

# Fork size as a fraction of the sprite's long side, per context (see module
# docstring for why these differ).
FORK_FG_HOME = 0.36   # adaptive foreground (must fit the icon_background_clip disc)
FORK_FG_SA = 0.52     # adaptive foreground over full-bleed background
FORK_COMPOSITE = 0.56 # legacy circular launcher PNGs
FORK_MONO = 0.36      # icon_fork.xml monochrome (masked like the home fg)
FORK_SPLASH = 0.74    # splash disc (diameter 200 in a 320 viewport)
FORK_NOTIF = 0.86     # status-bar silhouette
FORK_MENU = 0.66      # device-list silhouette
FORK_BOOK = 0.46      # book_logo on a rounded square
RING_WIDTH = 0.062    # white outline ring thickness (matches the original icon)
RING_INSET = 0.02     # ring distance from the rim

# Density buckets -> pixel size of each sprite family.
ADAPT = dict(mdpi=108, hdpi=162, xhdpi=216, xxhdpi=324, xxxhdpi=432)  # 108dp
LEG = dict(mdpi=48, hdpi=72, xhdpi=96, xxhdpi=144, xxxhdpi=192)       # 48dp
NOTIF = dict(mdpi=24, hdpi=36, xhdpi=48, xxhdpi=72)
MENU = dict(mdpi=26, hdpi=39, xhdpi=52, xxhdpi=78)
BOOK = dict(mdpi=15, hdpi=22, xhdpi=30, xxhdpi=45, xxxhdpi=60)

# ---- parse source ---------------------------------------------------------
svg = SRC.read_text()
polys = re.findall(r'<polygon fill="url\(#(\w+)\)" points="([^"]*)"', svg)
assert len(polys) == 3, polys
POLY = {}
for name, pts in polys:
    nums = [float(v) for v in re.findall(r'-?\d+\.?\d*', pts)]
    POLY[name] = list(zip(nums[0::2], nums[1::2]))

# source transform: translate(256 256) rotate(-40) scale(1.14 1.34) translate(-256 -256)
PIV, ROT, SCX, SCY = 256.0, -40.0, 1.14, 1.34


def g1(x, y):
    x -= PIV
    y -= PIV
    x *= SCX
    y *= SCY
    r = math.radians(ROT)
    c, s = math.cos(r), math.sin(r)
    return (x * c - y * s) + PIV, (x * s + y * c) + PIV


allpts = [g1(x, y) for poly in POLY.values() for (x, y) in poly]
xs = [p[0] for p in allpts]
ys = [p[1] for p in allpts]
SB = (min(xs), min(ys), max(xs), max(ys))         # transformed fork bbox
SBc = ((SB[0] + SB[2]) / 2, (SB[1] + SB[3]) / 2)  # transformed fork centre
SBlong = max(SB[2] - SB[0], SB[3] - SB[1])

# ---- derived SVGs ---------------------------------------------------------
BG_DEF = ('<linearGradient id="bg" gradientUnits="userSpaceOnUse" x1="0" y1="0" '
          'x2="0" y2="512"><stop offset="0" stop-color="#3a5e83"/>'
          '<stop offset="1" stop-color="#274566"/></linearGradient>')
(TMP / "bg.svg").write_text(
    f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" '
    f'height="512"><defs>{BG_DEF}</defs><rect width="512" height="512" '
    f'fill="url(#bg)"/></svg>')

# fork with shadow + facets (drop the bg rect, keep everything else)
(TMP / "fork_shadow.svg").write_text(
    re.sub(r'<rect width="512" height="512" fill="url\(#bg\)">\s*</rect>', '', svg))

# fork solid white silhouette: 3 polygons only, source transform, no shadow/strokes
poly_xml = "".join(
    '<polygon fill="#ffffff" points="%s"/>' % " ".join(f"{x},{y}" for x, y in POLY[n])
    for n in ("paperMid", "paperLight", "paperDark"))
(TMP / "fork_solid.svg").write_text(
    f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" '
    f'height="512"><g transform="translate(256 256) rotate(-40) scale(1.14 1.34) '
    f'translate(-256 -256)">{poly_xml}</g></svg>')

# ---- render masters -------------------------------------------------------
def render(svg_path, px, out_png):
    subprocess.run(["magick", "-background", "none", str(svg_path),
                    "-resize", f"{px}x{px}", str(out_png)], check=True)


render(TMP / "bg.svg", 1024, TMP / "bg_master.png")
render(TMP / "fork_shadow.svg", 1024, TMP / "fork_shadow_master.png")
render(TMP / "fork_solid.svg", 1024, TMP / "fork_solid_master.png")

BG = Image.open(TMP / "bg_master.png").convert("RGBA")
FORK_S = Image.open(TMP / "fork_shadow_master.png").convert("RGBA")  # with shadow
FORK_F = Image.open(TMP / "fork_solid_master.png").convert("RGBA")   # silhouette

# solid fork bbox in master px (true fork bounds, no shadow)
fb = FORK_F.split()[3].getbbox()
fb_long = max(fb[2] - fb[0], fb[3] - fb[1])
fb_c = ((fb[0] + fb[2]) / 2, (fb[1] + fb[3]) / 2)


def place_fork(canvas, src, target_long, center=None):
    """Scale `src` so the *solid* fork's long side == target_long, paste so the
    solid-fork centre lands on `center` (canvas centre by default). Keeps the
    shadow in the correct relative position when src is FORK_S."""
    W, H = canvas.size
    if center is None:
        center = (W / 2, H / 2)
    k = target_long / fb_long
    r = src.resize((max(1, round(src.size[0] * k)), max(1, round(src.size[1] * k))),
                   Image.LANCZOS)
    cx, cy = fb_c[0] * k, fb_c[1] * k
    canvas.alpha_composite(r, (round(center[0] - cx), round(center[1] - cy)))
    return canvas


def save(img, resdir, name, size):
    d = OUT / resdir
    d.mkdir(parents=True, exist_ok=True)
    if img.size != (size, size):
        img = img.resize((size, size), Image.LANCZOS)
    img.save(str(d / f"{name}.png"))


def white_sil(target_long, size):
    """White fork silhouette centred on a `size` transparent canvas."""
    cv = Image.new("RGBA", (size * SS, size * SS), (0, 0, 0, 0))
    white = Image.new("RGBA", FORK_F.size, (255, 255, 255, 0))
    white.putalpha(FORK_F.split()[3])
    place_fork(cv, white, target_long * SS)
    return cv.resize((size, size), Image.LANCZOS)


# ---- adaptive layers ------------------------------------------------------
for d, A in ADAPT.items():
    save(BG.resize((A, A), Image.LANCZOS), f"drawable-{d}", "icon_01_background_sa", A)
    fg = place_fork(Image.new("RGBA", (A * SS, A * SS), (0, 0, 0, 0)),
                    FORK_S, FORK_FG_HOME * A * SS).resize((A, A), Image.LANCZOS)
    save(fg, f"mipmap-{d}", "icon_01_foreground", A)
    save(fg, f"mipmap-{d}", "icon_01_foreground_round", A)
    fgsa = place_fork(Image.new("RGBA", (A * SS, A * SS), (0, 0, 0, 0)),
                      FORK_S, FORK_FG_SA * A * SS).resize((A, A), Image.LANCZOS)
    save(fgsa, f"mipmap-{d}", "icon_01_foreground_sa", A)

# ---- legacy circular launchers -------------------------------------------
def circle_icon(L, ring):
    S = L * SS
    base = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, S - 1, S - 1], fill=255)
    disc = BG.resize((S, S), Image.LANCZOS).copy()
    disc.putalpha(mask)
    base.alpha_composite(disc)
    place_fork(base, FORK_S, FORK_COMPOSITE * S)
    if ring:
        w = round(RING_WIDTH * S)
        inset = round(RING_INSET * S)
        ImageDraw.Draw(base).ellipse([inset, inset, S - 1 - inset, S - 1 - inset],
                                     outline=(255, 255, 255, 255), width=w)
    return base.resize((L, L), Image.LANCZOS)


for d, L in LEG.items():
    ring = circle_icon(L, True)
    save(ring, f"mipmap-{d}", "icon_01_launcher", L)
    save(ring, f"mipmap-{d}", "icon_01_launcher_round", L)
    plain = circle_icon(L, False)
    save(plain, f"mipmap-{d}", "icon_01_launcher_sa", L)
    save(plain, f"mipmap-{d}", "icon_01_launcher_adaptive", L)

# ---- notification (white silhouette) -------------------------------------
for d, N in NOTIF.items():
    save(white_sil(FORK_NOTIF * N, N), f"drawable-{d}", "notification", N)

# ---- menu_fork (alpha silhouette; tinted SRC_IN in code) -----------------
for d, N in MENU.items():
    save(white_sil(FORK_MENU * N, N), f"drawable-{d}", "menu_fork", N)

# ---- book_logo (rounded square + white fork) -----------------------------
for d, N in BOOK.items():
    S = N * SS
    bgsq = BG.resize((S, S), Image.LANCZOS).copy()
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, S - 1, S - 1], radius=round(0.22 * S), fill=255)
    bgsq.putalpha(mask)
    white = Image.new("RGBA", FORK_F.size, (255, 255, 255, 0))
    white.putalpha(FORK_F.split()[3])
    place_fork(bgsq, white, FORK_BOOK * S)
    save(bgsq.resize((N, N), Image.LANCZOS), f"drawable-{d}", "book_logo", N)

# ---- vectors (monochrome + splash) ---------------------------------------
def path_data():
    parts = []
    for n in ("paperMid", "paperLight", "paperDark"):
        p = POLY[n]
        d = "M%s" % ",".join(f"{x:g} {y:g}" for x, y in p[:1])
        d += "".join(f"L{x:g} {y:g}" for x, y in p[1:]) + "Z"
        parts.append(d)
    return "".join(parts)


def g0_for(viewport, target_long):
    """Outer group translate+scale mapping the transformed fork bbox to a
    centred sprite of `target_long` long side in a `viewport` canvas."""
    s = target_long / SBlong
    return s, viewport / 2 - s * SBc[0], viewport / 2 - s * SBc[1]


PD = path_data()
# inner group replays the SVG's transform; outer group (below) places/scales it.
INNER = ('<group android:pivotX="256" android:pivotY="256" android:rotation="-40" '
         'android:scaleX="1.14" android:scaleY="1.34">'
         '<path android:fillColor="%s" android:pathData="%s"/></group>')
(OUT / "drawable").mkdir(parents=True, exist_ok=True)

# icon_fork.xml - monochrome layer (viewport 108)
s, tx, ty = g0_for(108, FORK_MONO * 108)
(OUT / "drawable" / "icon_fork.xml").write_text(
    '<vector xmlns:android="http://schemas.android.com/apk/res/android" '
    'android:width="108dp" android:height="108dp" android:viewportWidth="108" '
    'android:viewportHeight="108">'
    f'<group android:translateX="{tx:.3f}" android:translateY="{ty:.3f}" '
    f'android:scaleX="{s:.5f}" android:scaleY="{s:.5f}">'
    f'{INNER % ("#FFFFFFFF", PD)}</group></vector>\n')

# splash_fork_320.xml - solid disc + fork (viewport 320, disc diameter 200)
s, tx, ty = g0_for(320, FORK_SPLASH * 200)
(OUT / "drawable" / "splash_fork_320.xml").write_text(
    '<?xml version="1.0" encoding="utf-8"?>\n'
    '<vector xmlns:android="http://schemas.android.com/apk/res/android" '
    'android:width="320dp" android:height="320dp" android:viewportWidth="320" '
    'android:viewportHeight="320">'
    '<path android:fillColor="#3A5E83" android:pathData="M160,60 '
    'C133.49,60 108.04,70.54 89.29,89.29 C70.54,108.04 60,133.49 60,160 '
    'C60,186.51 70.54,211.96 89.29,230.71 C108.04,249.46 133.49,260 160,260 '
    'C186.51,260 211.96,249.46 230.71,230.71 C249.46,211.96 260,186.51 260,160 '
    'C260,133.49 249.46,108.04 230.71,89.29 C211.96,70.54 186.51,60 160,60 Z"/>'
    f'<group android:translateX="{tx:.3f}" android:translateY="{ty:.3f}" '
    f'android:scaleX="{s:.5f}" android:scaleY="{s:.5f}">'
    f'{INNER % ("#FFFFFFFF", PD)}</group></vector>\n')

shutil.rmtree(TMP, ignore_errors=True)
print(f"Wrote icon assets to {OUT}")

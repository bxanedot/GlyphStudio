"""Render the GlyphStudio logo to every icon file the app and installers need.

The logo is pure geometry on a 512x512 grid: an Apple-style squircle with a
navy gradient and three capsules, each with a soft shadow below it. This file
defines it once and writes docs/logo.svg plus crisp rasters for packaging:
app_icon.png (Linux), app_icon.ico (Windows) and app_icon.icns (macOS, inset
to Apple's 824/1024 icon grid with a drop shadow). The in-app copy is
app/src/jvmMain/kotlin/com/sfsymbols/ui/AppLogo.kt; keep the two in sync.

Usage: python scripts/render_icons.py   (needs Pillow: pip install pillow)
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

GRID = 512
SQUIRCLE_N = 5  # superellipse exponent; about Apple's continuous-corner icon shape
BG_TOP, BG_BOTTOM = "#4A6385", "#1E2A3C"
PILL_TOP, PILL_BOTTOM = "#FFFFFF", "#D3DCE7"
SHADOW = (8, 14, 24, 90)  # RGBA of the offset shadow under each capsule
SHADOW_DY = 10
# Three equal capsules: 88 wide, 16 apart, centred in a 296x296 box.
PILLS = [(108 + i * 104, 108, 88, 296) for i in range(3)]

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "app/src/jvmMain/resources"


def squircle(size: float, steps: int = 256) -> list[tuple[float, float]]:
    """Superellipse |x|^n + |y|^n = 1 filling a size x size box."""
    r = size / 2
    pts = []
    for i in range(steps):
        t = 2 * math.pi * i / steps
        c, s = math.cos(t), math.sin(t)
        pts.append((r + r * math.copysign(abs(c) ** (2 / SQUIRCLE_N), c),
                    r + r * math.copysign(abs(s) ** (2 / SQUIRCLE_N), s)))
    return pts


def svg() -> str:
    path = "M" + " L".join(f"{x:.2f},{y:.2f}" for x, y in squircle(GRID)) + " Z"
    caps = lambda dy, fill: "\n".join(
        f'  <rect x="{x}" y="{y + dy}" width="{w}" height="{h}" rx="{w // 2}" fill="{fill}"/>' for x, y, w, h in PILLS)
    r, g, b, a = SHADOW
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {GRID} {GRID}" width="{GRID}" height="{GRID}">\n'
        f'  <title>GlyphStudio</title>\n  <defs>\n'
        f'    <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{BG_TOP}"/><stop offset="1" stop-color="{BG_BOTTOM}"/></linearGradient>\n'
        f'    <linearGradient id="pill" gradientUnits="userSpaceOnUse" x1="0" y1="108" x2="0" y2="404"><stop offset="0" stop-color="{PILL_TOP}"/><stop offset="1" stop-color="{PILL_BOTTOM}"/></linearGradient>\n'
        f'  </defs>\n  <path d="{path}" fill="url(#bg)"/>\n'
        f'{caps(SHADOW_DY, f"rgba({r},{g},{b},{a / 255:.2f})")}\n{caps(0, "url(#pill)")}\n</svg>\n'
    )


def gradient(size: int, top: str, bottom: str, y0: float = 0, y1: float | None = None) -> Image.Image:
    """Vertical gradient from y0 to y1 (pixels), clamped outside that band."""
    y1 = size if y1 is None else y1
    t0, b0 = Image.new("RGB", (1, 1), top).getpixel((0, 0)), Image.new("RGB", (1, 1), bottom).getpixel((0, 0))
    col = Image.new("RGBA", (1, size))
    for y in range(size):
        f = min(max((y - y0) / (y1 - y0), 0), 1)
        col.putpixel((0, y), tuple(round(a + (b - a) * f) for a, b in zip(t0, b0)) + (255,))
    return col.resize((size, size))


def render(size: int) -> Image.Image:
    """Draw at 8x and downsample, so edges are anti-aliased at every size."""
    big = size * 8
    k = big / GRID
    im = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    mask = Image.new("L", (big, big), 0)
    ImageDraw.Draw(mask).polygon(squircle(big), fill=255)
    im.paste(gradient(big, BG_TOP, BG_BOTTOM), (0, 0), mask)
    shadow, pills = Image.new("L", (big, big), 0), Image.new("L", (big, big), 0)
    for x, y, w, h in PILLS:
        box = (x * k, y * k, (x + w) * k - 1, (y + h) * k - 1)
        ImageDraw.Draw(pills).rounded_rectangle(box, radius=w / 2 * k, fill=255)
        ImageDraw.Draw(shadow).rounded_rectangle((box[0], box[1] + SHADOW_DY * k, box[2], box[3] + SHADOW_DY * k),
                                                 radius=w / 2 * k, fill=255)
    im.paste(Image.new("RGBA", (big, big), SHADOW[:3] + (255,)), (0, 0),
             shadow.point(lambda v: v * SHADOW[3] // 255))
    im.paste(gradient(big, PILL_TOP, PILL_BOTTOM, 108 * k, 404 * k), (0, 0), pills)
    return im.resize((size, size), Image.LANCZOS)


def mac_icon(size: int) -> Image.Image:
    """Apple's macOS grid: the shape fills 824 of 1024, with a soft drop shadow."""
    body = round(size * 824 / 1024)
    off = (size - body) // 2
    logo = render(body)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    shadow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    shadow.paste((0, 0, 0, 110), (off, off + size // 80), logo.getchannel("A"))
    canvas.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(size / 90)))
    canvas.alpha_composite(logo, (off, off))
    return canvas


def main() -> None:
    (ROOT / "docs/logo.svg").write_text(svg(), encoding="utf-8", newline="\n")
    render(1024).save(RESOURCES / "app_icon.png", optimize=True)
    ico_sizes = [16, 20, 24, 32, 40, 48, 64, 128, 256]
    render(256).save(RESOURCES / "app_icon.ico", sizes=[(s, s) for s in ico_sizes],
                     append_images=[render(s) for s in ico_sizes])
    mac_icon(1024).save(RESOURCES / "app_icon.icns")
    print("Wrote docs/logo.svg, app_icon.png, app_icon.ico and app_icon.icns")


if __name__ == "__main__":
    main()

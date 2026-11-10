"""Render the GlyphStudio logo to every icon file the app and installers need.

The logo is pure geometry on a 512x512 grid, so it is defined here once and
written out as an SVG (docs/logo.svg) plus crisp rasters for packaging:
app_icon.png (Linux), app_icon.ico (Windows) and app_icon.icns (macOS).
The in-app copy lives in app/src/jvmMain/kotlin/com/sfsymbols/ui/AppLogo.kt;
keep the two in sync if the shape changes.

Usage: python scripts/render_icons.py   (needs Pillow: pip install pillow)
"""
from pathlib import Path

from PIL import Image, ImageDraw

GRID = 512
CORNER = 112
BACKGROUND = "#2E3D52"
FOREGROUND = "#E8EDF2"
# Three equal pills: 88 wide, 16 apart, centred in a 296x296 box.
PILLS = [(108 + i * 104, 108, 88, 296) for i in range(3)]

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "app/src/jvmMain/resources"


def svg() -> str:
    pills = "\n".join(
        f'    <rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{w // 2}"/>' for x, y, w, h in PILLS
    )
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {GRID} {GRID}" width="{GRID}" height="{GRID}">\n'
        f'  <title>GlyphStudio</title>\n'
        f'  <rect width="{GRID}" height="{GRID}" rx="{CORNER}" fill="{BACKGROUND}"/>\n'
        f'  <g fill="{FOREGROUND}">\n{pills}\n  </g>\n</svg>\n'
    )


def render(size: int) -> Image.Image:
    """Draw at 8x and downsample, so edges are anti-aliased at every size."""
    big = size * 8
    k = big / GRID
    im = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((0, 0, big - 1, big - 1), radius=CORNER * k, fill=BACKGROUND)
    for x, y, w, h in PILLS:
        d.rounded_rectangle((x * k, y * k, (x + w) * k - 1, (y + h) * k - 1), radius=w / 2 * k, fill=FOREGROUND)
    return im.resize((size, size), Image.LANCZOS)


def main() -> None:
    (ROOT / "docs/logo.svg").write_text(svg(), encoding="utf-8", newline="\n")
    render(1024).save(RESOURCES / "app_icon.png", optimize=True)
    ico_sizes = [16, 20, 24, 32, 40, 48, 64, 128, 256]
    render(256).save(RESOURCES / "app_icon.ico", sizes=[(s, s) for s in ico_sizes],
                     append_images=[render(s) for s in ico_sizes])
    render(1024).save(RESOURCES / "app_icon.icns")
    print("Wrote docs/logo.svg, app_icon.png, app_icon.ico and app_icon.icns")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Rebuild block_atlas.png with biome-tinted grass/leaves and chest cube faces."""
from PIL import Image
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"
BLOCKS = ROOT / "textures/blocks"
OUT = ROOT / "block_atlas.png"


def load16(path):
    im = Image.open(path).convert("RGBA")
    if im.size != (16, 16):
        im = im.resize((16, 16), Image.NEAREST)
    return im


def tint_multiply(im, rgb):
    r, g, b = rgb
    out = im.copy()
    px = out.load()
    for y in range(out.size[1]):
        for x in range(out.size[0]):
            pr, pg, pb, pa = px[x, y]
            if pa == 0:
                continue
            px[x, y] = ((pr * r) // 255, (pg * g) // 255, (pb * b) // 255, pa)
    return out


def main():
    grass_cm = Image.open(ROOT / "textures/colormap/grass.png").convert("RGB")
    fol_cm = Image.open(ROOT / "textures/colormap/foliage.png").convert("RGB")
    grass_tint = grass_cm.getpixel((127, 127))
    fol_tint = fol_cm.getpixel((127, 127))

    grass_top = tint_multiply(load16(BLOCKS / "grass_top.png"), grass_tint)
    leaves = tint_multiply(load16(BLOCKS / "leaves_oak.png"), fol_tint)

    grass_side = load16(BLOCKS / "grass_side.png")
    overlay = BLOCKS / "grass_side_overlay.png"
    if overlay.exists():
        ov = tint_multiply(load16(overlay), grass_tint)
        base_px = grass_side.load()
        ov_px = ov.load()
        for y in range(16):
            for x in range(16):
                r, g, b, a = ov_px[x, y]
                if a > 10:
                    base_px[x, y] = (r, g, b, 255)

    ch = Image.open(ROOT / "textures/entity/chest/normal.png").convert("RGBA")
    chest_top = ch.crop((16, 0, 32, 16))
    chest_side = ch.crop((0, 16, 16, 32))
    chest_front = ch.crop((16, 16, 32, 32))

    tiles = [
        grass_top,
        grass_side,
        load16(BLOCKS / "dirt.png"),
        load16(BLOCKS / "stone.png"),
        load16(BLOCKS / "sand.png"),
        load16(BLOCKS / "log_oak.png"),
        load16(BLOCKS / "log_oak_top.png"),
        leaves,
        load16(BLOCKS / "planks_oak.png"),
        load16(BLOCKS / "crafting_table_top.png"),
        load16(BLOCKS / "crafting_table_side.png"),
        load16(BLOCKS / "crafting_table_front.png"),
        chest_top,
        chest_side,
        chest_front,
        load16(BLOCKS / "dirt.png"),
    ]

    atlas = Image.new("RGBA", (16 * 16, 16), (0, 0, 0, 0))
    for i, t in enumerate(tiles):
        if t.size != (16, 16):
            t = t.resize((16, 16), Image.NEAREST)
        atlas.paste(t, (i * 16, 0))
    atlas.save(OUT)
    print("Wrote", OUT)


if __name__ == "__main__":
    main()

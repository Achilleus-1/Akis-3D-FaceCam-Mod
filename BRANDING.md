# Aki's 3D FaceCam Mod branding

**[Product of Achilleus](https://linktr.ee/achilleus_)**.

Artwork supplied by Achilleus as `Akis-3D-FaceCam-Mod.png`. The original purple
face-cube composition and its transparency are preserved throughout branding.

| Asset | Use |
| --- | --- |
| [Cover](docs/cover.png) | 1024 × 1024 transparent cover used in the README |
| [Mod icon](src/main/resources/icon.png) | 512 × 512 transparent NeoForge Mods listing icon |
| [Pack icon](src/main/resources/pack.png) | Matching packaged resource icon |
| [Social preview](docs/social-preview.png) | 1280 × 640 GitHub social preview export |
| [Artwork exporter](tools/branding.py) | Reproducible transparent icon and preview exports from the original cover |

The cover is a byte-identical copy of the supplied 1024 × 1024 PNG. Icons fit the
full composition into 512 × 512 RGBA images; the 1280 × 640 social preview centers
the full artwork on a transparent canvas. No background or extra text is added.

Regenerate with `python tools/branding.py` after installing Pillow. To import
replacement artwork, use `python tools/branding.py --source path/to/original.png`.
This is promotional artwork, not gameplay evidence. Social previews
can be uploaded through GitHub repository settings; committing the PNG does not
configure that setting automatically. See [LEGAL.md](LEGAL.md) for attribution.

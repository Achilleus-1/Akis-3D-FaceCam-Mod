"""Export the supplied transparent cover to packaged icons. Requires Pillow."""
import argparse
from pathlib import Path
import shutil
from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, help='Optional replacement original PNG')
    args = parser.parse_args()
    cover = ROOT / 'docs/cover.png'
    source = args.source or cover
    with Image.open(source) as opened:
        if opened.format != 'PNG':
            raise SystemExit('Use the original PNG to preserve its transparency.')
        image = opened.convert('RGBA')
    if image.getchannel('A').getextrema() != (0, 255):
        raise SystemExit('Expected the supplied artwork to include transparent and opaque pixels.')
    if source.resolve() != cover.resolve():
        shutil.copyfile(source, cover)
    # Fit the entire artwork; transparent padding preserves proportions without cropping.
    icon = Image.new('RGBA', (512, 512))
    fitted = ImageOps.contain(image, (512, 512), Image.Resampling.LANCZOS)
    icon.alpha_composite(fitted, ((512-fitted.width)//2, (512-fitted.height)//2))
    resources = ROOT / 'src/main/resources'
    for name in ['icon.png', 'pack.png']:
        icon.save(resources / name, optimize=True)
    social = Image.new('RGBA', (1280, 640))
    fitted = ImageOps.contain(image, social.size, Image.Resampling.LANCZOS)
    social.alpha_composite(fitted, ((1280-fitted.width)//2, (640-fitted.height)//2))
    social.save(ROOT / 'docs/social-preview.png', optimize=True)
    for name in [resources/'icon.png', resources/'pack.png', ROOT/'docs/social-preview.png']:
        with Image.open(name) as exported:
            assert exported.mode == 'RGBA' and exported.getchannel('A').getextrema() == (0, 255), name
    print('Preserved original cover and exported transparent mod/pack icons and social preview.')


if __name__ == '__main__':
    main()

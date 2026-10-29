"""Draw the original block-head / raised-mesh identity. Requires Pillow only."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
SCALE = 3
INK = '#132b3a'
TEAL = '#38dfcc'


def font(size):
    for name in ['C:/Windows/Fonts/bahnschrift.ttf', 'C:/Windows/Fonts/segoeuib.ttf',
                 '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf']:
        if Path(name).exists():
            return ImageFont.truetype(name, round(size * SCALE))
    return ImageFont.load_default(size=round(size * SCALE))


def polygon(draw, points, color, outline=INK, width=5):
    xy = [(round(x * SCALE), round(y * SCALE)) for x, y in points]
    draw.polygon(xy, fill=color)
    if outline:
        draw.line(xy + [xy[0]], fill=outline, width=width * SCALE, joint='curve')


def emblem():
    image = Image.new('RGBA', (1024 * SCALE, 1024 * SCALE))
    d = ImageDraw.Draw(image)
    polygon(d, [(270,240),(590,240),(750,145),(430,145)], '#70a6af', width=9)
    polygon(d, [(590,240),(750,145),(750,480),(590,575)], '#203c50', width=9)
    polygon(d, [(270,240),(590,240),(590,575),(270,575)], '#3d6474', width=9)
    # The faceted surface sits within the front face, with an asymmetric raised nose.
    points = [(430,267),(500,280),(550,330),(557,397),(530,467),(483,525),
              (430,542),(377,525),(330,467),(305,397),(312,330),(360,280),
              (362,342),(430,320),(498,342),(352,407),(508,407),
              (420,365),(392,419),(449,425),(430,469),(390,465),(471,465)]
    triangles = [(0,1,13),(1,14,13),(1,2,14),(2,3,14),(3,16,14),
                 (3,4,16),(4,22,16),(4,5,22),(5,20,22),(5,6,20),
                 (6,7,20),(7,21,20),(7,8,21),(8,15,21),(8,9,15),
                 (9,10,12),(9,12,15),(10,11,12),(11,0,13),(11,13,12),
                 (12,13,17),(13,14,17),(14,16,19),(14,19,17),
                 (17,18,12),(12,18,15),(17,19,18),(15,18,21),
                 (18,19,20),(18,20,21),(19,22,20),(16,22,19)]
    colors = ['#3cceb8','#5ee5ce','#8df4da','#2aabac','#6cdecc','#41bbbf']
    for index, triangle in enumerate(triangles):
        polygon(d, [points[i] for i in triangle], colors[index % len(colors)], '#187c83', 2)
    polygon(d, [points[i] for i in (17,18,19)], '#c4ffe0', '#187c83', 3)
    # Brow, eye, and mouth strokes help the topology read as a face at icon sizes.
    for line in [[(342,354),(371,347),(390,355)],[(466,355),(496,347),(522,355)],
                 [(389,474),(429,481),(471,474)]]:
        d.line([(x*SCALE,y*SCALE) for x,y in line], fill=INK, width=7*SCALE, joint='curve')
    # Four capture corners indicate the webcam source without adding a screen feed.
    for corner in [[(230,288),(230,210),(308,210)],[(697,115),(778,115),(778,195)],
                   [(230,520),(230,605),(308,605)],[(697,540),(778,540),(778,460)]]:
        d.line([(x*SCALE,y*SCALE) for x,y in corner], fill=TEAL, width=11*SCALE, joint='curve')
    return image


def centered(draw, y, text, size, color):
    draw.text((512*SCALE,y*SCALE), text, font=font(size), fill=color,
              anchor='mt', stroke_width=2*SCALE, stroke_fill=INK)


def main():
    docs = ROOT/'docs'; docs.mkdir(exist_ok=True)
    resource = ROOT/'src/main/resources'
    source = emblem()
    icon = source.crop((180*SCALE,70*SCALE,825*SCALE,715*SCALE)).resize((512,512),Image.Resampling.LANCZOS)
    for name in ['icon.png','pack.png']:
        icon.save(resource/name, optimize=True)
    cover = source.copy(); d=ImageDraw.Draw(cover)
    centered(d,675,"AKI'S",62,'#8be9d9')
    centered(d,759,'3D FACECAM',105,'#e1fff5')
    centered(d,886,'MOD',62,'#8be9d9')
    cover.resize((1024,1024),Image.Resampling.LANCZOS).save(docs/'cover.png',optimize=True)
    social=Image.new('RGBA',(1280,640))
    social.alpha_composite(icon,(48,64))
    label=Image.new('RGBA',(1024*SCALE,640*SCALE)); d=ImageDraw.Draw(label)
    for y,text,size in [(160,"AKI'S",45),(232,'3D FACECAM',76),(330,'MOD',45),(423,'PRODUCT OF ACHILLEUS',23)]:
        d.text((10*SCALE,y*SCALE),text,font=font(size),fill='#a4f5e6',stroke_width=SCALE,stroke_fill=INK)
    label=label.resize((1024,640),Image.Resampling.LANCZOS)
    social.alpha_composite(label,(575,0))
    social.save(docs/'social-preview.png',optimize=True)
    print('Wrote transparent cover, 512px mod/pack icons, and 1280x640 social preview.')


if __name__=='__main__':
    main()

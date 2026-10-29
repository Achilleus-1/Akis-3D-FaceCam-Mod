import zipfile,pathlib
from project import ROOT, JAR_NAME
with zipfile.ZipFile(ROOT/'build/libs'/JAR_NAME) as z:
    for n in z.namelist():
        if n.endswith('.dll') and any(x in n.lower() for x in ['ffmpeg','avcodec','opencv_videoio','vcruntime','msvcp','avformat']): print(n)

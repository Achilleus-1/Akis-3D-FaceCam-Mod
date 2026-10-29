"""Preserve dependency-provided third-party notices from the resolved native jars."""
import pathlib, zipfile
root=pathlib.Path(__file__).resolve().parents[1]
cache=root/'.gradle-user/caches/modules-2/files-2.1'
for group in ['org.bytedeco','com.microsoft.onnxruntime']:
    for jar in (cache/group).rglob('*.jar'):
        with zipfile.ZipFile(jar) as z:
            names=[n for n in z.namelist() if not n.endswith('/') and any(t in n.lower() for t in ['license','copying','notice']) and not n.endswith('.class')]
            if not names:continue
            text='\n\n'.join('===== '+n+' =====\n'+z.read(n).decode('utf-8',errors='replace') for n in names)
            dest=root/'licenses'/(jar.stem+'-bundled-notices.txt');dest.write_text(text,encoding='utf-8')
            print(jar.name, 'notices=',len(names))

"""Create deliverables without developer caches, worlds, personal camera images, or Minecraft binaries."""
import pathlib, zipfile, hashlib, json, shutil
from project import ROOT as root, VERSION, STEM, JAR_NAME, SOURCE_NAME
dist=root/'dist';dist.mkdir(exist_ok=True)
jar=root/'build/libs'/JAR_NAME
shutil.copy2(jar,dist/jar.name)
with zipfile.ZipFile(jar) as z:
    names=z.namelist()
    required=['META-INF/neoforge.mods.toml','icon.png','pack.png','facecam/models/face_mesh_Nx3x192x192.onnx','facecam/models/face_detection_short_range.onnx','dev/facecam/FaceCam.class','ai/onnxruntime/native/win-x64/onnxruntime.dll']
    for name in required:
        if name not in names:raise RuntimeError('Missing packaged component: '+name)
    metadata=z.read('META-INF/neoforge.mods.toml').decode('utf-8')
    if 'version="'+VERSION+'"' not in metadata or 'displayName="Aki\'s 3D FaceCam Mod"' not in metadata:
        raise RuntimeError('Packaged beta metadata is inconsistent')
    print('Packaged Windows DLLs:',len([n for n in names if n.endswith('.dll')]))
    print('VC runtime DLLs:',[n for n in names if 'vcruntime' in n.lower() or 'msvcp' in n.lower()])
paths=[]
for folder in ['src','gradle','licenses','tools','docs','.github']:
    p=root/folder
    if p.exists():paths.extend(f for f in p.rglob('*') if f.is_file() and '__pycache__' not in f.parts and f.suffix!='.log')
for name in ['README.md','VERIFICATION.md','BRANDING.md','CHANGELOG.md','CONTRIBUTING.md','DEVELOPMENT.md','LEGAL.md','SECURITY.md','LICENSE','NOTICE','THIRD_PARTY_NOTICES.md','build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat','.gitignore','.gitattributes','.editorconfig']:
    p=root/name
    if p.exists():paths.append(p)
archive=dist/SOURCE_NAME
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
    for p in sorted(paths):z.write(p,pathlib.Path('Akis-3D-FaceCam-Mod')/p.relative_to(root))
manifest={p.name:{'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in [dist/jar.name,archive]}
(dist/'SHA256.json').write_text(json.dumps(manifest,indent=2)+'\n')
(dist/'SHA256SUMS.txt').write_text(''.join(data['sha256']+'  '+name+'\n' for name,data in manifest.items()),encoding='utf-8')
print(json.dumps(manifest,indent=2))

"""Developer bootstrap only. Never used by the installed mod."""
import pathlib, urllib.request, zipfile, hashlib, json
ROOT = pathlib.Path(__file__).resolve().parents[1]
def fetch(url, destination):
    p = ROOT / destination
    p.parent.mkdir(parents=True, exist_ok=True)
    if not p.exists():
        print('Downloading', url, flush=True)
        urllib.request.urlretrieve(url, p)
    return p
for name in ['gradlew', 'gradlew.bat', 'gradle/wrapper/gradle-wrapper.jar']:
    fetch('https://raw.githubusercontent.com/gradle/gradle/v8.14.3/' + name, name)
wrapper = ROOT / 'gradle/wrapper/gradle-wrapper.properties'
gradle_sha=urllib.request.urlopen('https://services.gradle.org/distributions/gradle-8.14.3-bin.zip.sha256').read().decode().strip()
wrapper.write_text('distributionBase=GRADLE_USER_HOME\ndistributionPath=wrapper/dists\ndistributionUrl=https\\://services.gradle.org/distributions/gradle-8.14.3-bin.zip\nnetworkTimeout=60000\nvalidateDistributionUrl=true\nzipStoreBase=GRADLE_USER_HOME\nzipStorePath=wrapper/dists\ndistributionSha256Sum='+gradle_sha+'\n')
assets = {}
for name in ['face_mesh_Nx3x192x192.onnx','face_detection_short_range.onnx']:
    p = fetch('https://github.com/yakhyo/mediapipe-face-mesh-onnx/releases/download/weights/'+name+'?facecam=1', 'src/main/resources/facecam/models/'+name)
    assets[name] = hashlib.sha256(p.read_bytes()).hexdigest()
canonical=fetch('https://raw.githubusercontent.com/google-ai-edge/mediapipe/master/mediapipe/modules/face_geometry/data/canonical_face_model.obj', 'src/main/resources/facecam/models/canonical_face_model.obj')
assets['canonical_face_model.obj']=hashlib.sha256(canonical.read_bytes()).hexdigest()
fetch('https://raw.githubusercontent.com/yakhyo/mediapipe-face-mesh-onnx/main/LICENSE', 'licenses/face-mesh-Apache-2.0.txt')
fetch('https://raw.githubusercontent.com/microsoft/onnxruntime/v1.20.0/LICENSE', 'licenses/onnxruntime-MIT.txt')
fetch('https://raw.githubusercontent.com/bytedeco/javacpp/1.5.11/LICENSE.txt', 'licenses/javacpp-Apache-2.0.txt')
fetch('https://raw.githubusercontent.com/opencv/opencv/4.10.0/LICENSE', 'licenses/opencv-Apache-2.0.txt')
fetch('https://raw.githubusercontent.com/OpenMathLib/OpenBLAS/v0.3.28/LICENSE', 'licenses/openblas-BSD-3-Clause.txt')
(ROOT/'src/main/resources/facecam/models/SHA256.json').write_text(json.dumps(assets, indent=2)+'\n')
jdk = ROOT / '.tools/jdk21'
installed = pathlib.Path('C:/Program Files/Java/jdk-21')
if not jdk.exists() and not installed.exists():
    archive = fetch('https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse', '.tools/jdk21.zip')
    with zipfile.ZipFile(archive) as z:
        z.extractall(ROOT/'.tools/jdk-extract')
    next((ROOT/'.tools/jdk-extract').iterdir()).rename(jdk)
print('Bootstrap complete', flush=True)

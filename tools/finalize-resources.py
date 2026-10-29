import pathlib, json, hashlib, urllib.request, zipfile, io, xml.etree.ElementTree as ET
root=pathlib.Path(__file__).resolve().parents[1]
models=root/'src/main/resources/facecam/models'
hashes={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in models.iterdir() if p.suffix in ['.onnx','.obj']}
(models/'SHA256.json').write_text(json.dumps(hashes,indent=2)+'\n')
url='https://visualstudio.microsoft.com/wp-content/uploads/2021/09/Visual-C-Runtime-2015-2022-License-1.docx'
raw=urllib.request.urlopen(url).read()
with zipfile.ZipFile(io.BytesIO(raw)) as z:
    tree=ET.fromstring(z.read('word/document.xml'))
    ns={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
    text='\n'.join(''.join(p.itertext()) for p in tree.findall('.//w:p',ns))
    (root/'licenses/microsoft-vc-runtime-2015-2022.txt').write_text('Source: '+url+'\n\n'+text,encoding='utf-8')
properties=root/'gradle/wrapper/gradle-wrapper.properties'
sha=urllib.request.urlopen('https://services.gradle.org/distributions/gradle-8.14.3-bin.zip.sha256').read().decode().strip()
text=properties.read_text();text='\n'.join(line for line in text.splitlines() if not line.startswith('distributionSha256Sum='))+'\ndistributionSha256Sum='+sha+'\n';properties.write_text(text)
print('Canonical/model hashes, Microsoft runtime license and Gradle distribution checksum recorded')

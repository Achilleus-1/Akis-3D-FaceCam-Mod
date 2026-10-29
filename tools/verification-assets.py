"""Fetch a public upstream test photo, for local verification only (not packaged)."""
import urllib.request,pathlib
p=pathlib.Path('build/verification/man_smiling.jpg');p.parent.mkdir(parents=True,exist_ok=True)
urllib.request.urlretrieve('https://raw.githubusercontent.com/yakhyo/mediapipe-face-mesh-onnx/main/assets/inputs/man_smiling.jpg',p)
print(p)

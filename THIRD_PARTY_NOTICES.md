# FaceCam third-party notices

FaceCam source is Apache-2.0. Distributed dependencies retain their own licenses.

* MediaPipe canonical geometry and face models: Google, Apache-2.0.
  https://github.com/google-ai-edge/mediapipe
* ONNX face model exports: Yakhyokhuja Valikhujaev, Apache-2.0.
  https://github.com/yakhyo/mediapipe-face-mesh-onnx
  Face Mesh weights trace to Google's MediaPipe through PINTO0309's Apache-2.0 conversion.
  Detection decoding follows MediaPipe's SSD anchors and weighted NMS.
* ONNX Runtime 1.20.0: Microsoft, MIT.
* JavaCPP 1.5.11 and JavaCPP presets: Samuel Audet and contributors, Apache-2.0 / GPLv2 with Classpath exception (Apache option used).
* OpenCV 4.10.0: OpenCV contributors, Apache-2.0. Native build includes third-party notices in its META-INF resources.
* OpenBLAS 0.3.28: OpenBLAS contributors, BSD-3-Clause.
* Microsoft Visual C++ Runtime 2015–2022: Microsoft proprietary redistributable runtime. Unmodified MSVCP/VCRUNTIME DLLs are included through JavaCPP's Windows artifact. See licenses/microsoft-vc-runtime-2015-2022.txt and https://learn.microsoft.com/en-us/visualstudio/releases/2022/redistribution .
* Gradle wrapper: Gradle contributors, Apache-2.0.
* NeoForge and Minecraft are build/runtime dependencies, not bundled in the mod.

Full license texts are in licenses/ and inside the mod jar. Model SHA256 hashes are in facecam/models/SHA256.json. Models and native binaries are included in the jar; the installed mod downloads nothing and never uses cloud inference.

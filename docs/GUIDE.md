> Public beta 1.1.0-beta.1. See the [project overview](../README.md) and [release notes](RELEASE_NOTES.md). Commands below run from the project root.

# Aki's 3D FaceCam Mod — setup, controls, and architecture

A Minecraft Java Edition mod for **Minecraft 1.21.1 / NeoForge 21.1.259 / Java 21 / Windows x64**. An ordinary webcam supplies a live, textured, shallow triangle mesh attached to the front of the player's existing head. Capture and inference stay local; face updates travel through the connected Minecraft server. No audio is captured.

## Install

1. Install 64-bit Java 21 and the **NeoForge 21.1.259** client for Minecraft **1.21.1** using the official installer: https://neoforged.net/ . Select that profile in your launcher.
2. Put **akis-3d-facecam-mod-1.21.1-1.1.0-beta.1.jar** into that profile's `mods` folder. Use the jar without `-sources` in its name.
3. Launch Minecraft. The camera and sharing start **off on every launch**. There is no Python installation, script, companion app, or model download to perform.
4. For multiplayer, install the same jar in the NeoForge 21.1.259 server's `mods` folder and on participating clients. Players can receive faces without enabling their own camera. The mod is required on both sides of the connection.
5. Singleplayer uses the integrated server automatically; no separate server install is needed.

The jar is about 129 MiB because Windows x64 OpenCV/ONNX native libraries and both trained models are bundled. Native loaders extract local copies as needed. Server hosts can run the jar without a webcam or Windows natives being initialized; capture is supported only on Windows x64 clients.

For a new dedicated server, use the official NeoForge installer with `--installServer`, follow its Java 21/EULA setup instructions, then place FaceCam in `mods` before starting it. Use normal authenticated (`online-mode=true`) production servers. The development verification server uses offline accounts on localhost only.

## Use and controls

* **Comma ( , )** opens FaceCam settings. Also available through Mods → Aki's 3D FaceCam Mod → Config.
* **Period ( . )** starts/stops sharing while in a world. Rebind both keys in Options → Controls → Key Binds → Aki's 3D FaceCam Mod.
* Choose a camera index before starting. Windows DirectShow camera indices 0–7 are offered; index order depends on the installed devices. Try the next index if the selected device cannot open.
* **Local preview** explicitly opens the camera and shows the masked atlas without sending frames. Closing settings stops a preview-only camera.
* **Start sharing** explicitly enables transmission to the connected Minecraft server. A persistent `Aki's 3D FaceCam Mod · SHARING` status appears while enabled.
* **Stop and release camera** stops transmission, tells viewers to restore the skin, and releases the camera on the capture worker. Closing settings while sharing keeps sharing active. Leaving the world stops sharing and closes the camera.
* Use Minecraft's front third-person view to see your own face. The primary effect is on the character's head, not a HUD video feed.
* Face depth ranges from **0 to 6 model units**, default **1.75**. Zero flattens the triangle surface. Fully transparent outer-head fronts allow full depth even with the outer layer enabled. Opaque outer-front pixels or head equipment conservatively cap protrusion to 0.40 units to keep geometry beneath coverings.
* **Face size** ranges from **10% to 400%** of the original proportions. The entire surface scales about the head-face center, including depth, so small faces keep proportionate noses. The depth slider sets protrusion at 100% size. **Reset size** restores **100%**, exactly the previous version's face footprint. The sender's size is transmitted to nearby clients. Large sizes deliberately extend outside the normal 8×8 head footprint; coverings still cap actual protrusion.
* Smoothing ranges from 0–85%, default 45%. Higher values reduce jitter and slow expression changes.
* Video choices: 64², **128² default**, or 192². Frame rates: 5, 10, **15 default**, 20, or 30 FPS. Server limits may lower effective FPS.
* **Received faces: hidden** restores other players' ordinary skins locally. It does not disable sending your own face.

Upgrading from the old 1.0.0 local build migrates the old F7/F8 defaults to comma/period and the untouched 0.85 depth default to 1.75. Other custom keybindings and depth values are preserved. Update the mod on the server and all clients together: networking protocol 2 carries face size and is incompatible with 1.0.0.

Client preferences are stored in `config/facecam-client.json`; capture/sharing state is never persisted. Camera error details appear in the settings status; hover the status to read long messages.

## Server settings

After the server first starts, edit `config/facecam-server.toml` in its game directory. This path was verified with NeoForge 21.1.259. Restart to apply changes consistently.

| Setting | Default | Purpose |
| --- | ---: | --- |
| `sharingDistance` | 48 blocks | Relay only to nearby players currently tracking the sender |
| `maxFramesPerSecond` | 15 | Per-sender frame rate cap |
| `maxImageBytes` | 23000 | Maximum compressed image size; geometry adds 2808 bytes |
| `maxBytesPerSecondPerSender` | 180000 | Per-sender ingress token bucket and client pacing target |
| `maxViewersPerSender` | 16 | Bounds relay fanout; sender also receives their own stream |

A single serverbound payload is at most approximately 25.9 KiB, below NeoForge's 32 KiB serverbound ceiling. No chunking or reassembly is needed. Actual bandwidth depends on resolution, image detail, and effective FPS. A server with many sharing players should lower distance/viewer limits. The relay does not record face data to disk.

## Architecture

1. An opt-in daemon worker opens a Windows DirectShow webcam at approximately 640×480. No camera initialization occurs while capture is off.
2. Bundled ONNX Runtime runs Google's BlazeFace detector, then a 192×192 face crop through a MediaPipe-derived Face Mesh network producing **468 xyz landmarks**. Java implements letterboxing, SSD anchor decoding, weighted suppression and rotated square cropping. This is a verified Java desktop integration, not a dependency on MediaPipe's mobile Java API.
3. A rigid similarity fit against stable canonical landmarks removes translation, rotation and scale from the inferred face. Expression residuals remain. Temporal smoothing applies to the fitted mesh. At 100% size the frontal footprint fits inside the head's **8×8 model-unit face**; its oval boundary has zero depth. The configurable size scales this whole surface around the face center, and sizes above 100% intentionally exceed the head footprint.
4. Triangle rasterization warps current webcam face pixels into a fixed canonical UV atlas. Pixels outside the eroded face boundary are black before JPEG compression; raw room/camera crops are never sent. Receivers reconstruct a smooth alpha mask from the same topology. Texture alignment remains fixed while expression geometry animates.
5. Each frame contains one JPEG plus 468 quantized xyz vertices under one frame ID. The server obtains identity from `IPayloadContext.player()`, validates bounds, rate/bandwidth limits, distance and entity tracking, then relays eligible updates. It never invokes capture, inference or graphics.
6. Receivers maintain one pending frame and one decoded result per player, with one bounded decoding executor. JPEG dimensions are checked before full decode. GPU texture creation/upload/release runs on Minecraft's client thread; textures are reused. Adjacent geometry updates interpolate over 65 ms.
7. A real `TRIANGLES` render layer uses the vanilla entity shader, block/sky light, generated geometric normals, ordinary depth testing, and the exact player head transform. Standard and slim models receive the same head layer. The rest of the skin, hat layer and equipment render normally.
8. All vertices are limited to 0.40 protrusion when head equipment is present or the enabled outer-head front contains visible pixels. A bounded render-thread cache inspects skin alpha so completely transparent fronts do not flatten the face. This keeps them behind the 0.5-unit skin shell and vanilla helmet shell, including from oblique angles. Transparent hat texels reveal the surface through the normal depth test; opaque texels hide it. The surface is offset 0.015 units from the base face to avoid z-fighting.

Poor tracking briefly holds the last frame, then receivers fade after about 450 ms and release it by 1.5 seconds. A stop message releases the texture immediately. Camera errors stop sharing; leaving/disconnecting clears all streams. The server also stops streams after 1.5 seconds without valid updates. Outdated frames are overwritten, never accumulated in an unbounded queue.

## Build from source

The source archive includes Gradle wrapper, editable Java, models and license texts. Install a **JDK 21** and run:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
.\gradlew.bat build
```

Outputs are `build/libs/akis-3d-facecam-mod-1.21.1-1.1.0-beta.1.jar` and `build/libs/akis-3d-facecam-mod-1.21.1-1.1.0-beta.1-sources.jar`. The first build needs network access for pinned Gradle/NeoForge/Maven dependencies. Normal mod use needs no model/dependency downloads. Maven versions and NeoForge are pinned in `build.gradle`; model SHA256 hashes are recorded with the models. Use Java 21 to run Gradle as well as Minecraft.

`tools/bootstrap.py` is an optional developer re-download utility, not a runtime requirement. The project already contains its model files and wrapper. `tools/verification-assets.py` downloads an upstream public test photograph into `build/verification` only. `gradlew nativeSmoke` explicitly tests native loading and inference against that photograph. See [VERIFICATION.md](../VERIFICATION.md) for observed results and repeatable two-client checks.

## Troubleshooting and limitations

* **Camera unavailable / access denied / already in use:** enable Windows Settings → Privacy & security → Camera → desktop app access, close conferencing/camera software, reconnect the device, and select a different index. DirectShow does not reliably distinguish these causes; the status explicitly lists them rather than claiming a precise diagnosis.
* **Missing native components:** use Windows **x64 Java 21** and ensure antivirus permits local DLL extraction. The jar includes OpenCV, OpenBLAS, ONNX and JavaCPP's app-local MSVCP/VCRUNTIME DLLs; it does not install system software. Native loading was tested on the delivery machine, not on a clean Windows installation. The official Microsoft Visual C++ 2015–2022 x64 redistributable can repair a damaged system runtime if needed.
* **No tracked face:** use even lighting, keep the whole face inside the frame, and face the webcam. Heavy occlusion/profile views can invalidate tracking. Recovery is automatic while the camera remains active.
* **Lag:** reduce FPS/resolution; close CPU-intensive apps. Local inference uses two CPU threads. Effective FPS is bounded by inference time and server byte limits.
* **Nose seems shallow with a hat:** intentional shell depth limiting. A completely transparent outer-front layer allows full depth already. If the front contains opaque pixels, disable the outer head layer in Minecraft Skin Customization to see full configured depth when no head equipment is worn. Helmets are never disabled by FaceCam.
* **Other players show normal skins:** check server/client installation, receiver hide setting, distance, entity tracking, and whether the sender has enabled sharing.

This is an approximate monocular facial surface, not a scan. Occluded facial regions cannot be reconstructed from unseen image pixels; strong yaw, low light, hair/hands across the face and extreme expressions can degrade tracking. The face-oval mask removes surrounding room and shoulders; it is not a semantic per-pixel occlusion/hair segmentation model, so occluders inside the facial region may remain visible. Model confidence can also remain high through some occlusions. Arbitrary translucent outer-skin pixels follow Minecraft's depth-writing behavior; fully transparent and opaque regions are the main supported cases. Custom modded helmets/shaders are not exhaustively verified.

Face images are visible to the connected server and eligible modded players. Minecraft's connection is the transport; FaceCam adds no cloud endpoint, encryption layer or recording system. Never enable sharing on a server you do not trust with your face.

The exact tested acceptance coverage and remaining hardware/manual limitations are recorded in [VERIFICATION.md](../VERIFICATION.md). Synthetic tests are not evidence of real webcam behavior.

## Licenses

Aki's 3D FaceCam Mod source: Apache-2.0. See `LICENSE`, `NOTICE`, `THIRD_PARTY_NOTICES.md`, and `licenses/` for Google MediaPipe, model exports, Microsoft ONNX Runtime, JavaCPP, OpenCV and OpenBLAS notices. Minecraft and NeoForge are not included in the source archive or mod jar.

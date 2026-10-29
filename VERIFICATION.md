# FaceCam verification

## Artwork beta 1.1.0-beta.2 — 2026-10-28

The supplied `Akis-3D-FaceCam-Mod.png` replaces the generated artwork. The README
cover is byte-identical to the original. Both 512 × 512 packaged icons and the
1280 × 640 social preview retain RGBA transparency and the full composition.
The packaged icon bytes match the verified project assets. All compiled classes
are byte-identical to beta.1; this release changes branding and version metadata.

`gradlew build` succeeded and all ten automated tests passed. Repository checks
and direct alpha-channel checks passed. Existing live webcam and clean Windows
installation limitations below remain; no new camera test is claimed.

## Public beta 1.1.0-beta.1 — 2026-10-28

The renamed **Aki's 3D FaceCam Mod** beta was rebuilt with Java 21 for Minecraft
1.21.1 / NeoForge 21.1.259. All **10 automated tests passed**; model checksum
validation passed. The release jar metadata reports the new name and beta
version and includes its icon, models, native DLLs, and dependency notices.

Windows native/model smoke testing passed again: `NATIVE_OPENCV_OK`,
`ONNX_SESSIONS_OK`, and **468 landmarks**, approximately **1.714 nose-depth
units**, and **56 ms** inference on the existing public test photograph.
This timing is a single-photo observation, not sustained webcam performance.

The renamed installable jar was loaded by an isolated dedicated development
server with loose project classes and Maven native dependencies removed from
the launch classpath. It listed **Aki's 3D FaceCam Mod 1.1.0-beta.1**, reached
`Done`, and did not report client-only class loading failures. The local test
world reported its expected upgrade from the earlier 1.1.0 build. Production
servers should use online authentication; diagnostic offline accounts stay on
localhost. Earlier two-client appearance/network checks are recorded below;
they are not claims of repeated live-camera testing for this publication.

Repository checks cover syntax, local documentation links, release filenames,
large-file exclusion, and common private-path/credential patterns. Generated
jars, worlds, IDE state, caches, and unredacted runtime logs are excluded from
Git and the editable source deliverable. Historical screenshots and test reports
are preserved, with the synthetic/photo source disclosed in the README.

The physical webcam and clean Windows installation limitations below remain.

The packaged beta client also started, joined that dedicated server, and opened
the renamed settings screen with capture/sharing off. Its full title and the
comma/period hints were checked in an unmodified framebuffer capture under
`docs/verification/public-beta/`. Both diagnostic processes then exited cleanly.

## FaceCam 1.1.0 appearance update

The updated build passes **10 automated tests**. New checks cover 10%–400% size bounds, scaling around the head-face center, proportional nose depth at small sizes, stronger default depth, the six-unit unscaled depth limit, and zero-depth oval boundaries.

Default keys are now **comma for settings** and **period for sharing**; the in-game verification reports `configKey=,` and `sharingKey=.`. Old untouched F7/F8 bindings migrate while other custom bindings are preserved. Default depth rises from 0.85 to **1.75 model units**, and the slider now allows 0–6 units. Static-photo inference produced approximately **1.714 units of nose depth** at the new default.

Two updated jar-based clients connected to the updated dedicated server. The receiver displayed a 400% enlarged mesh, a 10% tiny mesh, and the restored 100% original footprint after the reset function. Size is transmitted as bounded frame metadata under networking protocol 2. All server and client installations must update together from 1.0.0.

The renderer now inspects the outer-front skin alpha on the render thread through a bounded cache. A fully transparent outer front does not impose the previous 0.40-unit cap, so the outer layer may stay enabled while full facial depth is visible. Opaque outer-front regions and head equipment retain conservative shell depth limiting. The updated settings screen, larger depth with the outer layer enabled, and synchronized size extremes/reset were inspected using framebuffer captures under `docs/verification/appearance-1.1/`.

The updated renderer was also inspected with an opaque outer-front skin region and a vanilla diamond helmet. Both covered the deeper face without visible protrusion through the covering; transparent skin regions revealed the underlying face. The dedicated server and both clients shut down cleanly after verification.

The original hardware limitation remains: a physical webcam stream was not available for verification. These appearance checks used the repeatable photograph/landmark source.

## Original 1.0.0 verification baseline

Verification date: **2026-10-13 (America/Chicago)**. Environment: Windows 11 x64, Oracle JDK **21.0.2**, Minecraft **1.21.1**, NeoForge **21.1.259**, NVIDIA RTX 5070 Ti / OpenGL 4.6.

This project contains the implemented capture/inference, geometry fitting, canonical texture rasterization, networking, server relay, client decoding, mesh renderer and configuration UI. It is not an implementation plan or a flat-video-plane substitute.

## Automated checks

`gradlew build nativeSmoke writeVerificationClasspath` completed successfully. **Eight JUnit tests passed**:

1. Canonical topology is valid; the nose, cheeks, brow and lips have nonzero geometric depth; the oval boundary has zero depth; the depth-zero setting flattens all z offsets.
2. A known 3D rotation, translation and scale are removed by the rigid pose fit (maximum residual below 0.03 canonical units).
3. Synthetic mouth and eyelid expression displacements survive rigid pose removal.
4. Synthetic magenta room pixels are excluded from the transmitted face atlas; its reconstructed alpha mask excludes the outer area.
5. Frame and byte token buckets enforce their budgets.
6. Invalid resolutions, image sizes, geometry lengths and coordinates are rejected.
7. A noisy 192² image compresses to a complete payload below 32 KiB.
8. A JPEG whose dimensions disagree with its advertised face size is rejected before pixel decoding.

The Gradle build also validates SHA256 checksums of both ONNX models and the canonical geometry. Wrapper distribution checksum verification is enabled. The jar embeds Windows native dependencies, models, notices and licenses.

## Local inference and camera probe

The bundled OpenCV Windows native libraries and both ONNX sessions loaded successfully. Inference on the upstream `man_smiling.jpg` test photograph produced **468 landmarks**, a canonical masked 128² atlas, and approximately **0.832 model units of nose depth** at the default depth setting. Inference took approximately **59–61 ms** on this machine; this measures detection/landmark inference on one photograph, not sustained capture/encoding/network FPS.

The actual installable jar was also used for an explicit DirectShow camera probe. **Camera index 0 could not open.** The jar-based Minecraft client's local-preview initialization loaded its native/model stack and showed a clear `Camera/inference error: IOException: Camera 0 unavailable` status without crashing. This does not distinguish absent hardware, privacy denial, or a device already in use. Therefore, **live ordinary-webcam capture, live face-motion stabilization, live blinking/smiling/mouth tracking, camera unplug/recovery, and sustained real-webcam performance have not been validated**. Static-photo inference and synthetic tests are not substitutes for those tests.

## Minecraft integration

A dedicated NeoForge server reached `Done` with FaceCam loaded, without client-only class-loading failure or camera/inference/graphics initialization. A second isolation run loaded **the compiled jar from `mods`**, with loose FaceCam classes/resources and Maven OpenCV/JavaCPP/ONNX dependencies removed from the launch classpaths. This run also reached `Done`, and two similarly isolated jar-based clients connected and rendered the stream. The NeoForge development launcher supplies the Minecraft runtime; this was not an installation test through a commercial launcher or a clean Windows system.

Two Minecraft clients connected to the dedicated server under distinct local offline development identities. The explicit diagnostic harness sent a face atlas inferred from the test photograph and synthetic lip displacements at 10 FPS through the normal NeoForge payloads. The receiving client reconstructed and rendered the actual triangle mesh. This is real Minecraft networking/rendering with a repeatable test source; it is **not a live webcam demonstration**.

Observed integration results:

* Face imagery rendered on the original head's front; the original head, body and skin remained intact.
* Mesh placement and a restrained protruding surface were inspected from frontal and oblique views. Geometric depth is also verified numerically by the tests.
* A solid block between the jar-based viewer and the player's head occluded the face while the stream and texture remained active, confirming ordinary world depth behavior.
* The default slim renderer and a diagnostic standard/WIDE renderer both displayed the stream.
* With the vanilla outer layer enabled, a controlled opaque blue half-face region covered the webcam surface; its transparent half revealed the face. Disabling that layer removed the covering and allowed full configured depth.
* Leather, chainmail, iron, gold, diamond, netherite and turtle helmets were inspected. Their normal visible regions covered the corresponding face regions. Diamond was also inspected from an oblique angle. The face did not render through the helmet nose/brow regions.
* The receiver decoded thousands of updates while rendering the actual surface. Images and geometry are transmitted in one atomic frame payload; the diagnostic expression motion is synthetic.
* An explicit sharing stop restored the original skin and changed the receiving client from one stream/texture to **zero streams/textures**.
* Temporarily lowering the server image limit to reject incoming updates triggered timeout cleanup and restored the original skin without a stop message. Restart is the documented way to apply server settings consistently.
* Leaving and reconnecting clients was exercised. Server logout removed their stream/tracking state; clients could reconnect and display new updates.
* Resource snapshots during continuous receipt showed **one stream slot, one FaceCam texture, zero pending frames and zero decoder backlog**. Two GC histograms several minutes apart retained **three `FaceFrame` objects** (fixture/current/previous) and one slot in each snapshot. Total `DynamicTexture` objects stayed at five, including vanilla textures. Texture registration also uses fixed explicit resource IDs, avoiding Minecraft's historical per-prefix dynamic-registration counters. This is a short repeatable bounded-retention check, not an all-day load test.
* The in-game settings screen was rendered and inspected. Visual QA found and fixed double background blur caused by the pinned NeoForge `Screen.render` behavior.
* The isolated dedicated server's heap contained no `OrtSession`, `VideoCapture`, `DynamicTexture` or `NativeImage` instances while two clients were streaming/rendering.

Evidence images and test reports are included under `docs/verification/`. These are framebuffer captures, not generated mockups. Unredacted runtime logs remain local and are excluded from publication.

## Remaining acceptance coverage

| Requirement | Coverage |
| --- | --- |
| Ordinary live webcam, background-free animated face | Implemented; native/model/photo checks passed; live webcam unavailable in the probe |
| True triangle geometry, 8×8 footprint, geometric features | Automated checks and actual Minecraft renders |
| Removal of real rigid pose while preserving expressions | Known-transform and synthetic-expression tests; actual human motion untested |
| Head translation/rotation and normal animation | Uses the exact head transform; frontal/oblique rendering inspected; exhaustive sneaking/swimming/pose testing unperformed |
| Standard/slim models | Both renderers exercised with the test source |
| Outer skin and vanilla helmets | Opaque/transparent mask, layer off, all seven helmet types exercised |
| Multiplayer synchronization | Two real clients and server with atomic test-source image/geometry frames; live video sync untested |
| Stop, timeout, reconnect | Exercised with the test source; physical camera failure recovery untested |
| Bounded memory/bandwidth | Limits/compression tests, overwrite queues, backpressure checks and sampled live-client retention; large-population/long-duration load untested |
| Clean Windows install and modded shader/helmet compatibility | Not tested |

## Repeat verification

Normal installation must never start the camera automatically. Diagnostic commands below are explicitly opt-in development tools:

```powershell
python tools/verification-assets.py
.\gradlew.bat build nativeSmoke
java -cp build/libs/facecam-1.0.0.jar dev.facecam.diagnostic.NativeSmoke --camera 0
```

Python is used only for developer fixture/download/packaging tools, never by the installed mod. To repeat the two-client diagnostic, set the development server's `server-port=25585` and `server-ip=127.0.0.1`, run `gradlew runServer`, then launch `runVerifySender` and `runVerifyViewer` from separate terminals **after compiling once**. Avoid concurrent compilation into the same build output.

The diagnostic JVM properties `facecam.verify.role` and `facecam.verify.server` enable developer-only mailboxes. They are absent during normal installation. `verification-commands.txt` in the test server's game directory submits ordinary console commands; `verification-shot.txt` requests a named framebuffer capture, `verification-control.txt` controls test scenarios, and `verification-stop.txt` closes a test client. They do not enable a physical webcam unless the explicit `preview` command is supplied. The test sender's synthetic payload source is separate from normal capture/sharing.

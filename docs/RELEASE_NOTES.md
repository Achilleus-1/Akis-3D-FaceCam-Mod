# Aki's 3D FaceCam Mod — 1.1.0-beta.2

Artwork update for the public beta on **Minecraft 1.21.1**, **NeoForge 21.1.259**, **Java 21**,
and **Windows x64 clients**. Product of [Achilleus](https://linktr.ee/achilleus_).

An ordinary RGB webcam supplies a masked, approximately reconstructed 3D face
attached to the front of the existing player head. Tracking and inference run
locally. Participating clients share synchronized imagery and geometry through
the connected NeoForge server, with distance, rate, and bandwidth limits.

- Camera and sharing start off; no audio or cloud inference.
- **Comma** opens settings; **period** toggles sharing. Both are rebindable.
- Default facial depth is **1.75 units**, adjustable **0–6**.
- Face size is **10%–400%**; **Reset size** restores the original **100%** proportions.
- Standard/slim heads, normal skin, outer layers, and vanilla helmets are preserved.
- Bundled Windows native libraries and models; no Python or companion setup for players.

## Changes in beta.2

- Use Achilleus's supplied purple face-cube artwork throughout the mod and repository.
- Preserve original PNG transparency and the entire composition in every export.
- Webcam, geometry, protocol, and settings behavior is unchanged from beta.1.

## Install

Download `akis-3d-facecam-mod-1.21.1-1.1.0-beta.2.jar` and replace older FaceCam
jars in the client's `mods` folder. Multiplayer requires the same beta on the
server and participating clients. The `source-project.zip` is the complete
editable Gradle project; `SHA256SUMS.txt` verifies both attachments.

Internal ID `facecam`, saved settings, and protocol 2 are preserved from the
local 1.1.0 build. Protocol 2 is incompatible with 1.0.0; update all sides together.

## Beta verification and limitations

The previous implementation passed ten automated tests, Windows native/model
photo inference, dedicated server startup, and two-client synthetic rendering,
size/reset, covering, networking, timeout, and bounded-resource checks.
The renamed beta is rebuilt and rechecked for publication; see
[VERIFICATION.md](../VERIFICATION.md) for the exact coverage.

**Live webcam operation remains unverified:** camera index 0 was unavailable in
the test environment. Synthetic/photo results do not prove live expression,
head-motion stabilization, camera recovery, or sustained webcam performance.
Clean Windows installation and arbitrary shader/modded helmet compatibility
also remain untested. Opaque outer-front skin pixels or head equipment impose a
conservative depth cap. Sizes over 100% intentionally exceed the head footprint.

Only enable sharing on servers you trust with your face imagery. Report
reproducible beta issues without posting personal camera images or private logs.

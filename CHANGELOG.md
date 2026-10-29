# Changelog

## 1.1.0-beta.1 — 2026-10-28

- Publish the existing 1.1.0 implementation as **Aki's 3D FaceCam Mod**.
- Update the Mods listing, settings title, controls category, sharing status,
  author credit, repository links, and packaged icon.
- Add the public repository layout, cover and social preview assets,
  installation/usage guides, contributor and security guidance, and Windows CI.
- Use a beta version and consistent Minecraft-versioned release filenames.
- Preserve the `facecam` internal ID, configuration paths, comma/period controls,
  and protocol 2. Update earlier 1.0.0 installations on server and clients together.
- Clearly distinguish previous synthetic multiplayer checks from the remaining
  live webcam and clean Windows installation tests.

## 1.1.0 — 2026-10-13 (local build)

- Comma opens settings and period toggles sharing; old untouched F7/F8 defaults migrate.
- Increase default face depth to 1.75 units; allow 0–6 units.
- Add synchronized face sizing from 10% to 400% and reset to the original 100% proportions.
- Allow full configured depth under completely transparent outer-front skin pixels.
- Add appearance checks; ten automated tests pass.

## 1.0.0 — 2026-10-13 (local build)

- Implement opt-in Windows RGB webcam capture, local ONNX detection/tracking,
  stabilized 468-vertex geometry, masked imagery, and genuine triangle rendering.
- Implement authenticated nearby server relay, bounded queues and bandwidth,
  stream cleanup, settings, vanilla covering depth limits, and native packaging.
- Verify native photo inference and two-client synthetic rendering/networking.

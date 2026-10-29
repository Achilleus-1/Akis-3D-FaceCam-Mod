# Development

Use a **JDK 21** and the committed Gradle wrapper. Minecraft is **1.21.1**;
NeoForge is **21.1.259**. Versions and release names live in `gradle.properties`.

```powershell
.\gradlew.bat build --console=plain
python tools/check_repository.py
python tools/package.py
```

On Linux/macOS use `./gradlew build`; webcam capture and native inference are
supported on Windows x64 clients. The first build downloads pinned dependencies.
Python 3.12 is only for developer utilities; normal players need no Python.
`tools/package.py` uses the standard library. Regenerating branding requires Pillow.

The installable mod is `build/libs/akis-3d-facecam-mod-1.21.1-1.1.0-beta.1.jar`.
The `-sources.jar` is Java source only, not an installable mod. Packaging produces
the installable jar, a complete editable source project zip, and SHA256 manifests
under `dist/`. Generated artifacts are release attachments, not Git source files.
Pinned model hashes and the Gradle distribution hash are checked during the build.

## Layout

| Path | Contents |
| --- | --- |
| `src/main/java/dev/facecam/client/` | Opt-in capture/inference, settings, decoding, render layer |
| `src/main/java/dev/facecam/core/` | Shared geometry, pose fitting, image masking, frame validation |
| `src/main/java/dev/facecam/network/` | Payload codecs and authenticated nearby server relay |
| `src/main/java/dev/facecam/diagnostic/` | Explicitly enabled developer test sources and mailboxes |
| `src/main/resources/` | Metadata, translations, icons, bundled models |
| `src/test/` | Geometry, masking, validation, compression, rate and size tests |
| `docs/` | User guide, branding, and historical verification screenshots |
| `.github/` | Windows build/check workflow, issue and pull request templates |
| `licenses/` | Third-party redistribution licenses |
| `tools/` | Packaging, repository checks, and opt-in verification helpers |

The `facecam` internal ID, Java packages, config paths, resource names, and
protocol 2 remain stable across the public rename. This is a Windows-native fat
jar; the dedicated server must not initialize any webcam, inference, or GPU code.

## Run and verify

Run `gradlew runClient` or `gradlew runServer` (Windows: `.\gradlew.bat`). Accept
Minecraft's EULA yourself if you agree. Normal startup never opens a camera.
The development server enables a local diagnostic command mailbox; production
installations do not set that JVM property. Keep development test servers local.

```powershell
python tools/verification-assets.py
.\gradlew.bat nativeSmoke
java -cp build/libs/akis-3d-facecam-mod-1.21.1-1.1.0-beta.1.jar dev.facecam.diagnostic.NativeSmoke --camera 0
```

The first command downloads an upstream public test photo into ignored build
output. `nativeSmoke` checks Windows native loading and real model inference on
that photo; the explicit `--camera` command probes hardware. Read
[VERIFICATION.md](VERIFICATION.md) before interpreting these results.

For two-client diagnostics, compile once, set the dev server to localhost port
25585, start it, then run `runVerifySender` and `runVerifyViewer` in separate
terminals. Do not compile concurrently into shared output. The sender uses a
synthetic fixture and does not prove real webcam behavior. The packaged launcher
is an advanced Windows helper requiring captured Gradle development launch data;
it is not part of user installation.

## Release

Update `mod_version`, changelog, user-facing download names, and release notes.
Run the checks above before creating a tag. `tools/github_release.py` requires
explicit `--publish`, GitHub CLI, and existing GitHub authentication. It publishes
only a clean prepared checkout with matching release assets. It never writes
credentials to disk or prints them. Public beta releases are marked prerelease.

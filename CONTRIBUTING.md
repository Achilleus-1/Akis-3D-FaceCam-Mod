# Contributing

Use a focused branch and pull request. Explain the problem, resulting behavior,
and verification. Run `python tools/check_repository.py` and `./gradlew build`
with JDK 21 before submitting. See [DEVELOPMENT.md](DEVELOPMENT.md).

Keep camera access opt-in, inference local, server code independent of client
classes, and frame queues and network budgets bounded. Never add audio capture.
Preserve the `facecam` mod ID, configuration paths, and dependency notices.
Document protocol changes and update both server and client requirements.

Do not commit webcam imagery, credentials, local IDE files, worlds, generated
builds, or unredacted machine-specific logs. Use [SECURITY.md](SECURITY.md) for
private vulnerability reports. Describe synthetic and hardware tests separately.

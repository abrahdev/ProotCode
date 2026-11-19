---
sidebar_position: 4
---

# Roadmap

ProotCode is developed in small, testable phases.

## P0 — Repository and scaffold

- [x] Kotlin and Jetpack Compose project.
- [x] CI for formatting, lint, tests, and debug builds.
- [x] Signed APK releases from GitHub Actions.
- [x] Documentation site.

## P1 — Linux runtime

- [x] Ubuntu 26.04 LTS rootfs download and verification.
- [ ] Debian rootfs in the catalog.
- [x] proot installation and first boot.
- [ ] Non-root user creation with a persistent home.
- [ ] Bind mounts for Downloads and user-selected folders.
- [x] Installation progress screen.

## P2 — Terminal and packages

- [x] PTY-backed terminal sessions.
- [x] Multiple terminal windows.
- [ ] Mouse and pointer integration.
- [ ] apt command runner in the app interface.
- [ ] Essential presets: Git, build-essential, curl, OpenCode.
- [x] Tool selection grouped by category.

## P3 — Code Server

- [x] Code Server installation inside the environment.
- [ ] Local network access with an explicit toggle.
- [ ] Mobile CSS with rounded corners.
- [ ] Project access through the Linux home.

## P4 — Quality of life

- [ ] Backup and restore for projects and configuration.
- [ ] Battery and CPU usage monitoring.
- [ ] Resource limits for long-running sessions.
- [ ] Localization (English and Spanish).

## Out of scope

- Root access or custom ROM requirements.
- Cloud accounts, telemetry, or tracking.
- Closed-source components.

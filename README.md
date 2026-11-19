# ProotCode

<img src="docs/static/img/logo.svg" width="96" alt="ProotCode logo" />

**A portable Linux development workspace for Android.**

[![CI](https://github.com/abrahdev/ProotCode/actions/workflows/ci.yml/badge.svg)](https://github.com/abrahdev/ProotCode/actions/workflows/ci.yml)
[![Release](https://github.com/abrahdev/ProotCode/actions/workflows/release.yml/badge.svg)](https://github.com/abrahdev/ProotCode/actions/workflows/release.yml)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL_v3-blue.svg)](LICENSE)
![Version](https://img.shields.io/github/v/release/abrahdev/ProotCode?filter=v*&label=version&color=4ade80)
[![Docs](https://img.shields.io/badge/docs-abrahdev.github.io%2FProotCode-0f172a)](https://abrahdev.github.io/ProotCode/)

ProotCode is an open-source Kotlin app for Android that deploys rootless Debian and Ubuntu
distributions with proot. It is designed to provide a portable development environment with
multiple terminals, apt package management, and a mobile-optimized Code Server.

No account, no telemetry, no tracking. Everything runs on your device.

> **Status:** early development. The rootless proot runtime, environment installs, and
> interactive PTY terminals work today. The features below split what is implemented
> from what is still roadmap.

## Features

### Implemented

- **Rootless Linux** — proot-based runtime with no root access required.
- **Environment wizard** — pick a distribution from the catalog (Ubuntu 26.04 LTS today,
  for arm64-v8a and x86_64), an optional Code Server artifact, and tool presets, with
  staged install progress.
- **Code Server install** — `code-server` installed into each environment, with
  per-environment version switching.
- **Projects bridge** — a shared projects folder visible inside the environment, with an
  in-app verification test.
- **Multiple terminals** — interactive PTY shells with tabs and background sessions,
  in-app or in separate windows.

### Roadmap

- **Debian support** — add Debian to the distribution catalog alongside Ubuntu.
- **Non-root user** — a regular user with a persistent home directory.
- **Storage bridge** — bind selected Android folders (such as Downloads) into the Linux home.
- **Package manager** — run apt commands from the app interface.
- **Preset installation** — apply the selected tool presets to the rootfs during setup.
- **Code Server serving** — launch it in the browser, bound to the local network, with mobile CSS.
- **Mouse support** — pointer integration for proot and the Linux environment.
- **Resource controls** — monitor and limit battery and CPU usage.
- **Backups** — export and restore projects and configuration.

## Requirements

- Android 8.0 (API 26) or newer.
- For development: JDK 17+, Android SDK 37, and an internet connection for the first build.

## Installation

Download the latest APK from [GitHub Releases](https://github.com/abrahdev/ProotCode/releases).

You can also add `https://github.com/abrahdev/ProotCode` as a source in
[Obtainium](https://obtainium.org) to receive updates from GitHub Releases.

## Build from source

```bash
git clone https://github.com/abrahdev/ProotCode.git
cd ProotCode/mobile
./gradlew assembleDebug
```

The APK is generated at `mobile/app/build/outputs/apk/debug/app-debug.apk`.

For a signed release build, copy `mobile/key.properties.example` to
`mobile/key.properties`, fill in the keystore values, and run:

```bash
cd mobile
./gradlew assembleRelease
```

## Project structure

```
mobile/               Android build root (Kotlin + Compose)
mobile/app/           Android application module
mobile/gradle/        Version catalog and Gradle wrapper
mobile/CHANGELOG.md   Release notes
web/                  Astro landing page
assets/               Brand artwork, favicon, and landing mockup
catalog/              Artifact catalog fetched by the Android app
docs/                 Docusaurus documentation
.github/workflows/    CI, release, and documentation deployment
VERSION               Calendar version used by the release workflow
```

## Documentation

The documentation is published at [abrahdev.github.io/ProotCode](https://abrahdev.github.io/ProotCode/).

- [Installation](https://abrahdev.github.io/ProotCode/installation)
- [Architecture](https://abrahdev.github.io/ProotCode/architecture)
- [Roadmap](https://abrahdev.github.io/ProotCode/roadmap)
- [Contributing](https://abrahdev.github.io/ProotCode/contributing)

## Contributing

Issues and pull requests are welcome. Read the
[contributing guide](https://abrahdev.github.io/ProotCode/contributing) before opening a PR.

## License

[GNU Affero General Public License v3](LICENSE)

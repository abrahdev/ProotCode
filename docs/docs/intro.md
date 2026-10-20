---
slug: /
sidebar_position: 1
---

# ProotCode

**A portable Linux development workspace for Android.**

ProotCode is an open-source Kotlin app that runs Debian and Ubuntu environments without root
using [proot](https://proot-me.github.io/). It is built for developers who want a real Linux
toolchain on a phone or tablet: apt, Git, build tools, multiple terminals, and Code Server in
the browser.

No account, no telemetry, no tracking. Everything runs on your device.

:::warning

ProotCode is at an early stage. This documentation describes the architecture and roadmap; the
Linux runtime is not implemented yet.

:::

## Why ProotCode

- **Rootless** — no bootloader unlock, no root, no custom ROM.
- **Isolated** — only the Android folders you bind are visible inside Linux.
- **Portable** — projects and configuration live in a persistent home directory.
- **Open source** — AGPL-3.0, no closed components required.

## Next steps

- [Install ProotCode](/installation)
- [Understand the architecture](/architecture)
- [Follow the roadmap](/roadmap)

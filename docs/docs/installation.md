---
sidebar_position: 2
---

# Installation

## Requirements

- Android 8.0 (API 26) or newer.
- At least 4 GB of free storage for a distribution and development tools.
- An internet connection for the initial download and package installation.

## Install the APK

1. Download the latest APK from [GitHub Releases](https://github.com/abrahdev/ProotCode/releases).
2. Allow installation from unknown sources for your browser or file manager.
3. Open the APK and install it.
4. Launch ProotCode and choose Debian or Ubuntu.

## Updates with Obtainium

Add `https://github.com/abrahdev/ProotCode` as a source in
[Obtainium](https://obtainium.org) to receive updates from GitHub Releases automatically.

## Build from source

### Requirements

- JDK 17 or newer.
- Android SDK 37 with build tools.
- Git.

### Debug build

```bash
git clone https://github.com/abrahdev/ProotCode.git
cd ProotCode/mobile
./gradlew assembleDebug
```

The APK is generated at `mobile/app/build/outputs/apk/debug/app-debug.apk`.

### Release build

Create a keystore and a `key.properties` file inside `mobile/`:

```properties
storeFile=/absolute/path/to/prootcode-release.jks
storePassword=your-password
keyAlias=prootcode
keyPassword=your-password
```

Then run:

```bash
cd mobile
./gradlew assembleRelease
```

The signed APK is generated at `mobile/app/build/outputs/apk/release/app-release.apk`.

:::danger

Never commit `key.properties` or the keystore. Both are ignored by Git.

:::

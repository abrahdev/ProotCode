---
sidebar_position: 3
---

# Architecture

This page describes the intended architecture. The initial repository contains the Android
scaffold, CI, release pipeline, and documentation.

## Layers

```
mobile/                    Android build root
  app/                     Android UI, navigation, and lifecycle
    ui/                    Jetpack Compose screens and theme
    core/                  Shared utilities and result types
    data/                  Settings, storage, and environment metadata
    runtime/               proot process management
    terminal/              Terminal sessions and input handling
    codeserver/            Code Server lifecycle and networking
docs/                      Docusaurus documentation
```

The package root is `dev.prootcode.app`. Layers will be introduced as features are implemented;
the scaffold only contains `ui`.

## Runtime layers

The diagram groups the runtime into five layers. Component names such as `GuestExec`,
`RootfsInstaller` and `SysdataProvider` are proposed names for future modules, not existing code.

```mermaid
flowchart TB
    subgraph APP["Layer 1: Android app (Kotlin, targetSdk 37)"]
        UI["UI / Terminal"]
        SVC["Foreground Service"]
        GE["GuestExec<br/>(builds argv + env)"]
        RI["RootfsInstaller<br/>(extracts tar, verifies SHA-256)"]
        SD["SysdataProvider<br/>(fake /proc when needed)"]
    end

    subgraph NATIVE["Layer 2: nativeLibraryDir (read-only, executable)"]
        PROOT["libproot.so"]
        LOADER["proot loader"]
        TALLOC["libtalloc.so"]
    end

    subgraph DATA["Layer 3: filesDir (writable, NO execve)"]
        ROOTFS["rootfs/ (Ubuntu or Debian)"]
        FAKE["sysdata/ (stat, uptime, ...)"]
        HOME["home/ (persistent bind)"]
    end

    subgraph EXT["Layer 4: external storage"]
        PROJ["getExternalFilesDir()/projects"]
    end

    subgraph OS["Layer 5: Android / kernel"]
        SEL["SELinux untrusted_app<br/>execve denied, mmap-exec allowed"]
        KERN["Kernel (>= 4.11 for apt)"]
    end

    UI --> SVC --> GE
    RI --> ROOTFS
    SD --> FAKE
    GE --> PROOT
    PROOT --> LOADER
    PROOT --> TALLOC
    LOADER -->|"mmap guest ELF"| ROOTFS
    PROOT -.->|"bind"| FAKE
    PROOT -.->|"bind"| HOME
    PROOT -.->|"bind ~/projects_bridge"| PROJ
    PROOT -->|"ptrace + syscalls"| KERN
    SEL -.->|"policy"| PROOT
```

## Rootless execution

ProotCode uses proot to intercept system calls and present a Linux root filesystem without
root privileges. Each distribution is installed from a rootfs tarball into app-private storage.

- The Linux root filesystem is isolated from Android.
- Only explicitly bind-mounted Android folders are visible inside Linux.
- A non-root user owns the home directory.
- The home directory is persistent and can be backed up.

### Kernel requirements

Running the distribution does not depend on executing files from app storage: proot maps the
guest binaries instead, which works with `targetSdk 37` on Android 8 through 16.

Package management has an additional requirement. The proot spike showed that apt fails on the
Android 8 image (kernel 3.18) while it works from API 29 (kernel 4.14), so:

- proot, the terminal and bind mounts work on API 26 and later.
- apt, presets and tool installation require kernel 4.11 or newer, with API 29 and later as the
  validated baseline.

Details and raw results are documented in #4.

### Running a guest binary

The app data directory cannot be executed, but it can be mapped. proot relies on that
difference: the loader lives in `nativeLibraryDir`, maps the guest ELF and jumps to its entry
point instead of calling `execve` on it.

```mermaid
sequenceDiagram
    participant App as App (GuestExec)
    participant P as libproot.so
    participant L as Loader
    participant G as Guest binary (e.g. dpkg)
    participant K as Kernel / SELinux

    App->>P: ProcessBuilder(nativeLibraryDir/libproot.so, args)
    Note over App,P: execve allowed: file lives in nativeLibraryDir
    P->>K: fork + ptrace
    G->>K: execve("/usr/bin/dpkg")
    K-->>P: ptrace stop (sysenter)
    P->>P: translate guest path to real path in filesDir
    P->>L: replace execve with the loader
    L->>K: mmap(PROT_EXEC) of the guest ELF
    Note over L,K: SELinux grants execute on the mapping
    K-->>L: ok
    L->>G: jump to entry point
    Note over K: A direct execve would hit execute_no_trans denied
```

## Bind mounts

proot does not mount anything. It rewrites the paths of the traced processes, so every bind is
a mapping between a guest path and an Android path.

```mermaid
flowchart LR
    subgraph HOST["Android host"]
        H1["/dev"]
        H2["/proc, /sys"]
        H3["filesDir/sysdata/*"]
        H4["filesDir/home"]
        H5["externalFilesDir/projects"]
        H6["filesDir/rootfs"]
    end

    subgraph GUEST["Guest (root = rootfs)"]
        G0["/"]
        G1["/dev"]
        G2["/proc, /sys"]
        G3["/proc/stat, uptime, loadavg, ..."]
        G4["/root"]
        G5["/root/projects_bridge"]
    end

    H6 -->|"-r"| G0
    H1 -->|"-b"| G1
    H2 -->|"-b"| G2
    H3 -->|"-b, only if the real file is unreadable"| G3
    H4 -->|"-b"| G4
    H5 -->|"-b"| G5
```

## Environment lifecycle

```mermaid
flowchart TD
    A([First launch]) --> B{"Install marker<br/>matches current version?"}
    B -->|"yes"| M
    B -->|"no"| C["Read tarball from assets"]
    C --> D["Verify SHA-256"]
    D -->|"fails"| E([Error: abort])
    D -->|"ok"| F["Extract tarball into filesDir/rootfs<br/>(hardlinks as symlinks)"]
    F --> G["Write resolv.conf"]
    G --> H["Write version marker"]
    H --> M["Detect kernel: uname -r"]
    M --> N{"Kernel >= 4.11?"}
    N -->|"yes"| O["Full mode (apt enabled)"]
    N -->|"no"| P["Limited mode (terminal and binds)"]
    O --> Q["Check /proc/stat is readable<br/>if not, bind fake sysdata"]
    P --> Q
    Q --> R["Resolve nativeLibraryDir at runtime<br/>(changes on every app update)"]
    R --> S["Launch proot with PROOT_LOADER"]
    S --> T([Session active])
```

## Distributions

Two products are built from the same code:

- `standard` downloads the rootfs and code-server from the catalog on first use.
- `-Raccoon` bundles Ubuntu 26.04 LTS and code-server for its ABI and works offline.

Raccoon flavors carry only their own architecture because assets are not split by ABI. The
build details and trade-offs are in [ADR 0005](/adr).

## Environment lifecycle

1. The wizard loads the catalog (remote, cached or bundled).
2. The rootfs and optional code-server are downloaded or read from the bundled assets, always
   verified by SHA-256.
3. The rootfs is extracted to `filesDir/environments/<id>/rootfs`; code-server goes to
   `opt/code-server`.
4. `resolv.conf` is written from the active network and `environments.json` records the
   environment, its presets and its size.
5. The projects bridge is verified by writing a probe file through proot.

## Storage model

| Location | Purpose |
|---|---|
| App-private storage | Rootfs, binaries, and runtime state |
| User-selected Android folder | Projects and shared files |
| Linux home | Shell configuration, tools, and project data |
| Downloads bridge | Optional bind mount for the Android Downloads folder |

## Code Server

Code Server runs inside the Linux environment and listens on a local port. The app can expose it
on the device's network interface so another device on the same network can open the editor.

- The server is bound to the Linux home directory.
- VS Code extensions are installed inside the environment.
- Custom CSS adapts the web interface to mobile screens.
- No telemetry or external account is required.

## Security boundaries

- No root access on Android.
- No access to Android app data outside the bind mounts.
- The local network exposure is explicit and can be disabled.
- Secrets and SSH keys stay inside the Linux home unless the user exports them.

## Next

- [Roadmap](/roadmap)
- [Contributing](/contributing)

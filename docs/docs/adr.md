# ADR 0005: environments MVP and distributions

## Context

Issue #5 needs a first usable product: install an Ubuntu environment, optionally with
code-server, list and delete environments, and work offline on a bundled distribution.

## Decisions

### Two distributions instead of a build flag

The original plan used `-PbundleArtifacts`. Two product flavors replace it because the
bundled distribution has a different promise and size:

| Flavor | Contents | versionName |
| --- | --- | --- |
| `standard` | proot for both ABIs; rootfs and code-server downloaded | `2026.10.0` |
| `raccoonArm64` | proot, Ubuntu 26.04 LTS arm64 and code-server arm64 | `2026.10.0-Raccoon` |
| `raccoonX64` | same for x86_64 | `2026.10.0-Raccoon` |

Assets are not split by ABI automatically, so each Raccoon flavor carries only its own rootfs
and code-server. The shared `applicationId` means both distributions cannot be installed at the
same time.

### Artifacts

`artifacts.toml` pins the URL, SHA-256 and size of every artifact. `tools/bundle_artifacts.py`
downloads to a local cache, verifies the checksum and copies the rootfs and code-server into the
flavor assets. Raccoon APKs are about 287 MB per ABI; the standard APK is about 13 MB.

### Downloads

Downloads use the system `DownloadManager` instead of a custom foreground service. It provides
background progress, notifications, cancellation and resume without a service of our own. The
app verifies the SHA-256 after the download and aborts on mismatch, so integrity does not depend
on the system.

The trade-off is that the notification is owned by the system and the artifact lands in the
app-specific external storage before it is used.

### Storage layout

- Rootfs: `filesDir/environments/<id>/rootfs`, extracted with hardlinks converted to symlinks.
- code-server: `filesDir/environments/<id>/rootfs/opt/code-server`, versioned by re-extracting
  the directory.
- Projects bridge: `getExternalFilesDir()/projects`, bind-mounted at `~/projects_bridge` by
  proot path translation.
- Registry: `filesDir/environments.json` with the distro, arch, code-server version, presets,
  size and creation time.

### Catalog

`catalog.json` is versioned, validated and served from `raw.githubusercontent.com` with a cached
copy and a bundled asset as fallback. The catalog refresh is triggered when the environment
wizard opens and from the manual refresh action; failures fall back silently.

### Kernel requirement

apt needs kernel 4.11 or newer (see the proot spike), while proot itself works from API 26. The
wizard does not block old kernels, but package management will fail there.

## Consequences

- Releases need three APKs or a matrix build; the release workflow will be updated when the
  distributions are published.
- The Raccoon flavor has a large APK and duplicates the rootfs and code-server per ABI.
- 16 KB page validation remains open (#16).
- proot is GPL-2.0 and code-server is MIT; the `NOTICE` file covers attribution and source
  locations.

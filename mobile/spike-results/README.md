# proot spike results (issue #4)

Validation of running an Ubuntu 26.04 rootfs with `targetSdk 37` across Android 8 to 16, on
emulators and on a physical arm64 device. Every run installs the debug APK, extracts the rootfs
from assets and executes the checks inside the app; the raw output is in the `api-<level>/` and
`arm64-duchamp/` folders.

## Method

- proot 5.1.107.101 (Termux `.deb`), libtalloc 2.5.0 and libandroid-shmem 0.7, patched with
  `patchelf` (`RUNPATH=$ORIGIN`, `DT_NEEDED=libtalloc.so`) and packaged as
  `jniLibs/<abi>/*.so` with `useLegacyPackaging = true`.
- Ubuntu Base 26.04.1 rootfs (`amd64` and `arm64`), SHA-256 verified against the official
  `SHA256SUMS`.
- Emulators: `system-images;android-<level>;default;x86_64`, headless, `-gpu angle_indirect`.
- Physical device: Xiaomi 2311DRK48G, Android 16, `arm64-v8a`, kernel 6.1.138, 4 KB pages.
- The app runs 12 checks and writes `report.json` / `report.txt` to its private and external
  storage; AVC denials are collected from `logcat -b all`.

Reproduce with:

```bash
cd mobile
./gradlew assembleDebug
PATCHELF=patchelf ./tools/fetch-proot-spike-artifacts.sh x86_64 arm64-v8a
./tools/run-proot-spike.sh 26 29 33 35 36

# Physical device: install without -g (MIUI rejects it) and read the report via run-as.
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <serial> shell am start -W -n dev.prootcode.app/.MainActivity --ez autorun true
adb -s <serial> shell run-as dev.prootcode.app cat files/diagnostics/report.txt
```

## Matrix

| API | Android | exec from `filesDir` | proot `/bin/true` | bridge read/write | `apt update` + `install` | Result |
| --- | --- | --- | --- | --- | --- | --- |
| 26 | 8.0.0 | allowed (expected) | pass | pass | **fail** (`realpath` ENOSYS) | 9 pass, 3 fail |
| 29 | 10 | denied (expected) | pass | pass | pass | 12 pass |
| 33 | 13 | denied (expected) | pass | pass | pass | 12 pass |
| 35 | 15 | denied (expected) | pass | pass | pass | 12 pass |
| 36 | 16 | denied (expected) | pass | pass | pass | 12 pass |
| arm64 device | 16 | denied (expected) | pass | pass | pass | 12 pass |

The physical device runs Android 16 with kernel 6.1.138 and SELinux enforcing, so the result
also covers a vendor policy and a real arm64 userspace, not only AOSP emulators.

## Key evidence

The `targetSdk 37` restriction is active: executing a binary from the app data directory is
denied at the SELinux level, with `execute` still granted for mappings.

```
avc: granted { execute } for comm="DefaultDispatch" name="control"
     scontext=u:r:untrusted_app tcontext=u:object_r:app_data_file tclass=file
avc: denied { execute_no_trans } for comm="DefaultDispatch"
     path="/data/data/dev.prootcode.app/files/spike/control/control"
     scontext=u:r:untrusted_app tcontext=u:object_r:app_data_file tclass=file permissive=0
```

The rootfs binaries are mapped by the proot loader instead of being executed, so only the
`execute` check applies:

```
avc: granted { execute } for comm="libproot.so" name="dpkg"
     scontext=u:r:untrusted_app tcontext=u:object_r:app_data_file tclass=file
```

## API 26 limitation

`/bin/true`, `id -u`, the bind mount bridge and both exec controls work on API 26, but every
apt command fails:

```
E: flAbsPath on /var/lib/dpkg/status failed - realpath (38: Function not implemented)
E: The package lists or status file could not be parsed or opened.
```

Findings:

- The API 26 emulator runs kernel `3.18.91`; API 29 runs `4.14.175` and API 33 `5.15.41`.
- With the default seccomp path, apt aborts proot on API 26 with
  `handle_tracee_event: assertion "IS_IN_SYSENTER(tracee)" failed`. Disabling seccomp
  (`PROOT_NO_SECCOMP=1`) avoids the abort and lets apt download the indexes, but the
  `realpath` failure above remains.
- The same rootfs resolves `readlink -f`, `stat` and `perl`'s `realpath` correctly, so the
  failure is specific to apt's `realpath` call on this kernel + glibc 2.43 + proot combination.
- The API 26 image kernel is older than most physical Android 8 devices (usually 4.4), so a
  physical device may behave differently.

Decision: apt-based features require kernel >= 4.11, with API 29 and later as the validated
baseline. API 26 keeps proot, the terminal and bind mounts, but package management is
unsupported there. The arm64 device (kernel 6.1) confirms the apt path on a real vendor
policy; the follow-up for old kernels is to build `termux/proot` at a pinned commit and patch
its `realpath`/`statx` handling.

## Limitations

- `arm64-v8a` is validated on a single Android 16 device (kernel 6.1). Older arm64 devices with
  4.x kernels still depend on the kernel requirement above.
- 16 KB page alignment is verified statically (every `LOAD` segment is a multiple of `0x4000`),
  but neither the emulators nor the test device use 16 KB pages.
- No performance, battery or long-running session measurements.

# proot spike results (issue #4)

Validation of running an Ubuntu 26.04 rootfs with `targetSdk 37` across Android 8 to 16.
Every run installs the debug APK, extracts the rootfs from assets and executes the checks
inside the app; the raw output of each API is in the `api-<level>/` folders.

## Method

- proot 5.1.107.101 (Termux `.deb`), libtalloc 2.5.0 and libandroid-shmem 0.7, patched with
  `patchelf` (`RUNPATH=$ORIGIN`, `DT_NEEDED=libtalloc.so`) and packaged as
  `jniLibs/x86_64/*.so` with `useLegacyPackaging = true`.
- Ubuntu Base 26.04.1 (amd64) rootfs, SHA-256 verified against the official `SHA256SUMS`.
- Emulators: `system-images;android-<level>;default;x86_64`, headless, `-gpu angle_indirect`.
- The app runs 12 checks and writes `report.json` / `report.txt` to its private and external
  storage; AVC denials are collected from `logcat -b all`.

Reproduce with:

```bash
cd mobile
./gradlew assembleDebug
PATCHELF=patchelf ./tools/fetch-proot-spike-artifacts.sh x86_64
./tools/run-proot-spike.sh 26 29 33 35 36
```

## Matrix

| API | Android | exec from `filesDir` | proot `/bin/true` | bridge read/write | `apt update` + `install` | Result |
| --- | --- | --- | --- | --- | --- | --- |
| 26 | 8.0.0 | allowed (expected) | pass | pass | **fail** (`realpath` ENOSYS) | 9 pass, 3 fail |
| 29 | 10 | denied (expected) | pass | pass | pass | 12 pass |
| 33 | 13 | denied (expected) | pass | pass | pass | 12 pass |
| 35 | 15 | denied (expected) | pass | pass | pass | 12 pass |
| 36 | 16 | denied (expected) | pass | pass | pass | 12 pass |

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
unsupported there. Physical `arm64-v8a` devices still need validation; if they reproduce the
failure on old kernels, the follow-up is to build `termux/proot` at a pinned commit and patch
its `realpath`/`statx` handling.

## Limitations

- Only `x86_64` emulators; `arm64-v8a` and physical devices are not tested.
- 16 KB page alignment is verified statically (`LOAD` segments with `p_align 0x4000`), but the
  emulators use 4 KB pages.
- No performance, battery or long-running session measurements.

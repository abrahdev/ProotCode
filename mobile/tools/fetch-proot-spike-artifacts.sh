#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ARTIFACTS_DIR="$ROOT_DIR/mobile/spike-artifacts"
CACHE_DIR="$ARTIFACTS_DIR/.cache"

TERMUX_REPO="https://packages.termux.dev/apt/termux-main"
UBUNTU_BASE="https://cdimage.ubuntu.com/ubuntu-base/releases/26.04/release"
UBUNTU_ROOTFS="ubuntu-base-26.04.1-base"

PROOT_VERSION="5.1.107.101"
TALLOC_VERSION="2.5.0"
SHMEM_VERSION="0.7"

PATCHELF="${PATCHELF:-$(command -v patchelf || true)}"

ABIS=("$@")
if [ ${#ABIS[@]} -eq 0 ]; then
    ABIS=(x86_64)
fi

termux_arch() {
    case "$1" in
        arm64-v8a) echo "aarch64" ;;
        x86_64) echo "x86_64" ;;
        *) echo "unsupported abi: $1" >&2; exit 1 ;;
    esac
}

ubuntu_arch() {
    case "$1" in
        arm64-v8a) echo "arm64" ;;
        x86_64) echo "amd64" ;;
        *) echo "unsupported abi: $1" >&2; exit 1 ;;
    esac
}

package_sha256() {
    local arch="$1" name="$2" version="$3"
    local index="$CACHE_DIR/Packages-$arch.gz"
    if [ ! -f "$index" ]; then
        curl -fsSL --retry 3 -o "$index" \
            "$TERMUX_REPO/dists/stable/main/binary-$arch/Packages.gz"
    fi
    gzip -dc "$index" | awk -v name="$name" -v version="$version" '
        /^Package: / { pkg = $2; ver = ""; sha = "" }
        /^Version: / { ver = $2 }
        /^SHA256: / { sha = $2 }
        /^$/ {
            if (pkg == name && ver == version && sha != "") { print sha; found = 1; exit }
        }
        END {
            if (!found && pkg == name && ver == version && sha != "") { print sha }
        }
    '
}

pool_dir() {
    case "$1" in
        proot) echo "p" ;;
        *) echo "${1:0:4}" ;;
    esac
}

fetch_deb() {
    local arch="$1" name="$2" version="$3"
    local file="${name}_${version}_${arch}.deb"
    local path="$CACHE_DIR/$file"
    if [ ! -f "$path" ]; then
        curl -fsSL --retry 3 -o "$path" "$TERMUX_REPO/pool/main/$(pool_dir "$name")/$name/$file"
    fi
    local expected
    expected="$(package_sha256 "$arch" "$name" "$version")"
    if [ -z "$expected" ]; then
        echo "no checksum found for $file" >&2
        exit 1
    fi
    local actual
    actual="$(sha256sum "$path" | cut -d' ' -f1)"
    if [ "$expected" != "$actual" ]; then
        echo "checksum mismatch for $file" >&2
        exit 1
    fi
    echo "$path"
}

mkdir -p "$CACHE_DIR"

for abi in "${ABIS[@]}"; do
    tarch="$(termux_arch "$abi")"
    uarch="$(ubuntu_arch "$abi")"

    proot_deb="$(fetch_deb "$tarch" proot "$PROOT_VERSION")"
    talloc_deb="$(fetch_deb "$tarch" libtalloc "$TALLOC_VERSION")"
    shmem_deb="$(fetch_deb "$tarch" libandroid-shmem "$SHMEM_VERSION")"

    work="$CACHE_DIR/unpacked-$abi"
    rm -rf "$work"
    mkdir -p "$work"
    for deb in "$proot_deb" "$talloc_deb" "$shmem_deb"; do
        dpkg-deb -x "$deb" "$work"
    done

    prefix="$work/data/data/com.termux/files/usr"
    lib_dir="$ARTIFACTS_DIR/jniLibs/$abi"
    mkdir -p "$lib_dir"

    cp "$prefix/bin/proot" "$lib_dir/libproot.so"
    cp "$prefix/libexec/proot/loader" "$lib_dir/libproot_loader.so"
    cp "$prefix/lib/libtalloc.so.2.5.0" "$lib_dir/libtalloc.so"
    cp "$prefix/lib/libandroid-shmem.so" "$lib_dir/libandroid-shmem.so"

    if [ -n "$PATCHELF" ]; then
        "$PATCHELF" --replace-needed libtalloc.so.2 libtalloc.so \
            --set-rpath '$ORIGIN' "$lib_dir/libproot.so"
        "$PATCHELF" --set-rpath '$ORIGIN' "$lib_dir/libtalloc.so"
        "$PATCHELF" --set-rpath '$ORIGIN' "$lib_dir/libandroid-shmem.so"
    else
        echo "patchelf not found: libproot.so still links against libtalloc.so.2" >&2
        exit 1
    fi

    chmod 755 "$lib_dir"/*.so

    for elf in "$lib_dir"/*.so; do
        while read -r align; do
            if [ $((align % 0x4000)) -ne 0 ]; then
                echo "16 KB alignment check failed: $elf ($align)" >&2
                exit 1
            fi
        done < <(readelf -lW "$elf" | awk '$1 == "LOAD" { print $NF }')
    done

    rootfs_dir="$ARTIFACTS_DIR/assets/rootfs/$abi"
    mkdir -p "$rootfs_dir"
    rootfs_file="$UBUNTU_ROOTFS-$uarch.tar.gz"
    if [ ! -f "$CACHE_DIR/$rootfs_file" ]; then
        curl -fsSL --retry 3 -o "$CACHE_DIR/$rootfs_file" "$UBUNTU_BASE/$rootfs_file"
    fi
    if [ ! -f "$CACHE_DIR/SHA256SUMS" ]; then
        curl -fsSL --retry 3 -o "$CACHE_DIR/SHA256SUMS" "$UBUNTU_BASE/SHA256SUMS"
    fi
    (cd "$CACHE_DIR" && grep " \*\?$rootfs_file\$" SHA256SUMS | sha256sum -c -)
    cp "$CACHE_DIR/$rootfs_file" "$rootfs_dir/ubuntu-26.04-base.bin"

    echo "prepared $abi"
done

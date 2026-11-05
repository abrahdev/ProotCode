#!/usr/bin/env python3
"""Bundle the artifacts of a Raccoon flavor into its assets directory."""

import hashlib
import pathlib
import shutil
import sys
import tomllib
import urllib.request

FLAVOR_ARCH = {
    "raccoonArm64": "arm64-v8a",
    "raccoonX64": "x86_64",
}

ROOT = pathlib.Path(__file__).resolve().parents[1]
ARTIFACTS = ROOT / "artifacts.toml"
CACHE = ROOT / ".artifacts-cache"


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def download(url: str, destination: pathlib.Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = destination.with_suffix(destination.suffix + ".part")
    with urllib.request.urlopen(url) as response, temporary.open("wb") as output:
        shutil.copyfileobj(response, output, 1024 * 1024)
    temporary.replace(destination)


def cached(entry: dict) -> pathlib.Path:
    name = entry["url"].rsplit("/", 1)[-1]
    path = CACHE / name
    if path.is_file() and sha256(path) == entry["sha256"]:
        return path
    download(entry["url"], path)
    actual = sha256(path)
    if actual != entry["sha256"]:
        path.unlink(missing_ok=True)
        raise SystemExit(f"checksum mismatch for {name}: {actual}")
    return path


def main(flavors: list[str]) -> None:
    with ARTIFACTS.open("rb") as stream:
        manifest = tomllib.load(stream)
    for flavor in flavors:
        arch = FLAVOR_ARCH.get(flavor)
        if arch is None:
            raise SystemExit(f"unknown flavor: {flavor}")
        distro = next(item for item in manifest["distros"] if item["arch"] == arch)
        code_server = next(item for item in manifest["code_server"] if item["arch"] == arch)
        output = ROOT / "app" / "src" / flavor / "assets" / "bundled"
        output.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(cached(distro), output / "rootfs.bin")
        shutil.copyfile(cached(code_server), output / "code-server.bin")
        print(f"{flavor}: rootfs and code-server bundled in {output}")


if __name__ == "__main__":
    main(sys.argv[1:] or list(FLAVOR_ARCH))

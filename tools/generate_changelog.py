#!/usr/bin/env python3
"""Generate mobile/CHANGELOG.md and the in-app changelog resource from git history."""

import argparse
import datetime as dt
import pathlib
import re
import subprocess

ROOT = pathlib.Path(__file__).resolve().parents[1]
VERSION_FILE = ROOT / "VERSION"
CHANGELOG = ROOT / "mobile" / "CHANGELOG.md"
RAW_CHANGELOG = ROOT / "mobile" / "app" / "src" / "main" / "res" / "raw" / "changelog.txt"

HEADER = """# Changelog

All notable changes to this project are documented in this file.
"""

SECTION_ORDER = (
    ("Added", ("feat",)),
    ("Changed", ("refactor", "style", "ui", "perf")),
    ("Fixed", ("fix",)),
    ("Documentation", ("docs",)),
    ("Maintenance", ("chore", "ci", "build", "test", "revert")),
    ("Other", ()),
)

SUBJECT_RE = re.compile(r"^(?P<type>[a-z]+)(?:\((?P<scope>[^)]+)\))?(?P<breaking>!)?: (?P<desc>.+)$")
SECTION_RE = re.compile(r"^## \[(?P<version>[^\]]+)\](?: - (?P<date>\S+))?\s*$", re.MULTILINE)


def git_output(args):
    result = subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True)
    if result.returncode != 0:
        raise SystemExit("git {} failed: {}".format(" ".join(args), result.stderr.strip()))
    return result.stdout.strip()


def read_version():
    version = VERSION_FILE.read_text(encoding="utf-8").strip()
    if not version:
        raise SystemExit("VERSION is empty")
    return version


def previous_ref(version, explicit):
    if explicit is not None:
        return explicit
    current_tag = "v" + version
    tags = git_output(["tag", "--list", "v*", "--sort=-v:refname"]).splitlines()
    for tag in tags:
        if not tag or tag == current_tag:
            continue
        ancestor = subprocess.run(["git", "merge-base", "--is-ancestor", tag, "HEAD"], cwd=ROOT)
        if ancestor.returncode == 0:
            return tag
    roots = git_output(["rev-list", "--max-parents=0", "HEAD"]).splitlines()
    return roots[0] if roots else None


def collect_subjects(ref):
    log = ["log", "--no-merges", "--pretty=format:%s%x1e"]
    if ref:
        log[2:2] = [ref + "..HEAD"]
    return [part.strip() for part in git_output(log).split("\x1e") if part.strip()]


def categorize(subject):
    match = SUBJECT_RE.match(subject)
    if match is None:
        return "Other", subject
    description = match.group("desc").strip()
    scope = match.group("scope")
    if scope:
        description = "{}: {}".format(scope, description)
    if match.group("breaking"):
        description = "Breaking: " + description
    for title, types in SECTION_ORDER:
        if match.group("type") in types:
            return title, description
    return "Other", description


def group_subjects(subjects):
    grouped = {title: [] for title, _ in SECTION_ORDER}
    for subject in subjects:
        title, description = categorize(subject)
        entries = grouped[title]
        if description and description not in entries:
            entries.append(description)
    return grouped


def render_markdown(version, date, grouped):
    lines = ["## [{}] - {}".format(version, date), ""]
    for title, _ in SECTION_ORDER:
        entries = grouped[title]
        if not entries:
            continue
        lines.extend(["### " + title, ""])
        lines.extend("- " + entry for entry in entries)
        lines.append("")
    return "\n".join(lines).rstrip() + "\n"


def render_plain(sections):
    lines = []
    for version, date, body in sections:
        lines.append("{} ({})".format(version, date))
        for line in body.splitlines():
            if line.startswith("## "):
                continue
            if line.startswith("### "):
                lines.append("  " + line[4:] + ":")
            elif line.startswith("- "):
                lines.append("    " + line)
            elif line.strip():
                lines.append(line)
        lines.append("")
    return "\n".join(lines).rstrip() + "\n"


def parse_existing():
    if not CHANGELOG.exists():
        return HEADER, []
    text = CHANGELOG.read_text(encoding="utf-8")
    matches = list(SECTION_RE.finditer(text))
    if not matches:
        return text.rstrip() + "\n\n", []
    header = text[: matches[0].start()].rstrip() + "\n"
    sections = []
    for index, match in enumerate(matches):
        end = matches[index + 1].start() if index + 1 < len(matches) else len(text)
        body = text[match.end() : end].strip("\n")
        sections.append((match.group("version"), match.group("date") or "", body + "\n"))
    return header, sections


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", help="version to document (defaults to VERSION)")
    parser.add_argument("--date", help="release date as YYYY-MM-DD (defaults to today)")
    parser.add_argument("--since", help="git ref to collect commits from (defaults to the latest v* tag)")
    parser.add_argument("--dry-run", action="store_true", help="print the output without writing files")
    args = parser.parse_args()

    version = args.version or read_version()
    date = args.date or dt.date.today().isoformat()
    ref = previous_ref(version, args.since)
    subjects = collect_subjects(ref)
    if not subjects:
        raise SystemExit("No commits found since {}".format(ref or "the beginning"))

    grouped = group_subjects(subjects)
    new_section = render_markdown(version, date, grouped)

    header, existing = parse_existing()
    sections = [(version, date, new_section)] + [
        (old_version, old_date, body) for old_version, old_date, body in existing if old_version != version
    ]
    markdown = header + "\n" + "\n\n".join(body.rstrip() for _, _, body in sections) + "\n"
    plain = render_plain(sections)

    if args.dry_run:
        print(markdown)
        print("--- res/raw/changelog.txt ---")
        print(plain)
        return

    CHANGELOG.write_text(markdown, encoding="utf-8")
    RAW_CHANGELOG.parent.mkdir(parents=True, exist_ok=True)
    RAW_CHANGELOG.write_text(plain, encoding="utf-8")
    print(
        "Wrote {} and {} for {} ({} commits since {}).".format(
            CHANGELOG.relative_to(ROOT),
            RAW_CHANGELOG.relative_to(ROOT),
            version,
            len(subjects),
            ref or "the beginning",
        )
    )


if __name__ == "__main__":
    main()

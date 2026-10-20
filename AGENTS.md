# ProotCode — Agent Instructions

## Project Overview

ProotCode is an open-source Kotlin Android app that runs Debian and Ubuntu environments
without root using proot, with terminals, apt management, and Code Server.

## General Rules

- Never commit without explicit user permission.
- Never build a release bundle unless explicitly asked.
- Always run `./gradlew ktlintCheck lintDebug testDebugUnitTest` after code changes and fix issues before reporting done.
- Check existing code conventions before changing anything (imports, naming, package structure).
- Follow existing patterns and reuse existing dependencies.
- Do not add comments to code unless explicitly requested.
- Keep responses concise unless the user asks for detail.

## Git & GitHub

- Branch from `main` using `feat/<name>`, `fix/<name>`, or `chore/<name>`.
- Open a pull request against `main`; do not push directly to `main`.
- Commit messages follow Conventional Commits: `type(scope): description`.
- Types: `feat`, `fix`, `refactor`, `chore`, `ui`, `style`, `docs`, `test`, `ci`.
- Before committing, inspect `git status`, `git diff`, and `git log --oneline -10`.
- Stage only intended files; never commit keystores, `key.properties`, or secrets.
- Use the `gh` CLI for issues, pull requests, and checks.
- Create an issue before a PR when resolving a feature or a bug.

## Android & Kotlin

- Kotlin official code style, 4-space indentation, 120-column limit.
- UI with Jetpack Compose and Material 3. No XML layouts.
- Package root: `dev.prootcode.app`.
- Keep UI, domain, and data concerns separated as the app grows.
- Add strings to `app/src/main/res/values/strings.xml`; never hardcode user-facing text.
- Minimum SDK 26, compile/target SDK 37, Java 17 bytecode.
- Do not change AGP, Kotlin, or Compose versions without asking.

## Build

- Debug APK: `./gradlew assembleDebug`
- Unit tests: `./gradlew testDebugUnitTest`
- Lint: `./gradlew lintDebug`
- Formatting: `./gradlew ktlintCheck` (use `ktlintFormat` to fix)
- Release bundle: `./gradlew bundleRelease` only when explicitly requested.

## Versioning & Releases

- Calendar version in `VERSION`: `YYYY.MM.PATCH` (for example `2026.10.0`).
- `versionCode` is derived automatically from `VERSION`.
- Update `VERSION` and `CHANGELOG.md` together.
- Push the `VERSION` change to `main` to trigger the release workflow.
- Release tags use `v<version>`; versions with a suffix such as `-alpha` are marked as pre-releases.

## Signing

- Local signing: `key.properties` plus a keystore outside the repository.
- CI signing: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- Never print, commit, or paste keystore contents or passwords.

## Prohibited

- Do not refactor working code unless asked.
- Do not add dependencies without checking whether existing ones cover the need.
- Do not change theme colors, fonts, or branding without asking.
- Do not generate or guess URLs for the user.
- Do not create documentation files unless explicitly requested.

---
sidebar_position: 5
---

# Contributing

## Development setup

1. Install JDK 17 or newer and Android SDK 37.
2. Clone the repository.
3. Run `cd mobile && ./gradlew assembleDebug` to verify the toolchain.
4. Open the `mobile/` directory in Android Studio or IntelliJ IDEA.

## Checks

Run the same checks as CI before opening a pull request:

```bash
cd mobile
./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug
```

Use `./gradlew ktlintFormat` to fix formatting issues automatically.

## Branches and commits

- Branch from `main`: `feat/<name>`, `fix/<name>`, or `chore/<name>`.
- Use Conventional Commits: `type(scope): description`.
- Keep pull requests focused.
- Create an issue before a large feature or refactor.

## Code style

- Kotlin official style, 4-space indentation, 120-column limit.
- Jetpack Compose and Material 3 for UI.
- User-facing strings belong in `mobile/app/src/main/res/values/strings.xml`.
- Do not add comments unless they explain non-obvious behavior.

## Releases

- Update `VERSION` and `mobile/CHANGELOG.md`.
- Merge the change into `main`.
- The release workflow builds and publishes a signed APK.

## License

By contributing, you agree that your contributions are licensed under the AGPL-3.0.

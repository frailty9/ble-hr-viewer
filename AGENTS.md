# Repository Guidelines

## Investigate Before Editing

Treat questions, bug reports, and observations as requests to investigate unless the maintainer explicitly asks for a change. Inspect the code, explain the cause or remaining uncertainty, and propose a fix when useful. Wait for confirmation before editing, even for a one-line fix. An explicit implementation request or approval of a specific proposal authorizes work within that scope. Discuss architecture, dependency, compatibility, and release workflow changes before implementing them.

## Project Structure

BLE Heart Rate Viewer is a single-module Kotlin Android app using Jetpack Compose. The source root is `app/src/main/java/io/github/frailty9/blehr/`. Keep page UI in `feature/home`, `feature/settings`, or the relevant feature. Use `navigation/` for destinations, `data/` for repositories and settings, `ble/` for GATT and parsing, and `overlay/` for the floating readout and service. Keep `MainActivity` focused on hosting the app. Resources live in `app/src/main/res/`; bundled license assets live in `app/src/main/assets/`. Edit `PRIVACY.md` and `AGREEMENT.md` for generated legal text.

## Build and Test

Use Android SDK 35 and any JDK compatible with the project's Gradle and Android Gradle Plugin versions. Android Studio can run the `app` configuration. Use the Gradle wrapper from the repository root (`./gradlew` on macOS/Linux):

```powershell
.\gradlew.bat :app:assembleDebug             # Build and export a Debug APK to dist/
.\gradlew.bat :app:testDebugUnitTest         # Run JVM unit tests
.\gradlew.bat :app:lintDebug                 # Run Android lint
.\gradlew.bat :app:connectedDebugAndroidTest # Run device or emulator tests
.\gradlew.bat :app:assembleRelease           # Build the Release APK
```

Mirror production packages in `app/src/test/` (JUnit 4) and `app/src/androidTest/` (AndroidX and Compose UI). Use `*Test.kt` and descriptive test names. Check affected behavior during development; follow `docs/compliance/RELEASE_CHECKLIST.md` before release. An emulator can verify UI and navigation. Verify actual BLE readings, disconnects, and reconnects with physical hardware.

## Style and Localization

Use four-space indentation, `PascalCase` for types and composables, and `camelCase` for other functions and properties. Keep English `values/strings.xml` and Chinese `values-zh/strings.xml` in sync. Use the term "heart rate device" in user-facing copy without assuming a wristband.

## Commits and Pull Requests

For plans or successive edits involving several concerns, make focused local commits as each concern is ready so the work stays easy to review. Local commits need no separate approval; ask before rewriting history or pushing. Follow existing prefixes, such as `fix(ui):` and `feat(overlay):`. For `x.y.z`, increment `y` for features and `z` for fixes. Describe behavior and verification in PRs; link relevant issues and attach screenshots for visual changes.

## Local Development

Read `AGENTS.local.md` in the repository root if present; skip it otherwise. Keep machine-specific paths, device details, and debug conventions in that ignored file. Keep signing credentials in local signing configuration and out of Git.

# road-guide

Local-first Android app for background route tracking, daily timelines, estimated environmental exposure, route comparison, and local place/privacy management.

## Prerequisites
- **JDK 17** (required by project toolchain: AGP 8.7.3 / Gradle 8.10.2 / Kotlin 2.0.21)
- Android SDK installed; `local.properties` must point to it (e.g. `sdk.dir=/opt/android-sdk`)
- An emulator or device for instrumented tests

## Build
Use the project Gradle wrapper:

```bash
JAVA_HOME=<path-to-jdk17> ./gradlew :app:assembleDebug
```

Outputs:
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Lint report: `app/build/reports/lint-results-debug.html`

## Tests

```bash
./gradlew test
./gradlew lint
./gradlew :app:connectedDebugAndroidTest
```

Only `connectedDebugAndroidTest` requires a connected Android device or emulator.

## Project knowledge
Start with [`docs/project-knowledge/00_PROJECT_INDEX.md`](docs/project-knowledge/00_PROJECT_INDEX.md), then use `docs/project-knowledge/13_CURRENT_IMPLEMENTATION_STATUS.md` for the verified implementation snapshot. `ROAD GUIDE.txt` is the original product/spec reference and is not an implementation manifest.

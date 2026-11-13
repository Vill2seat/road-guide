# road-guide

Local-first Android app (greenfield) for day tracking, timeline, and estimated environmental exposure.

## Prerequisites
- **JDK 17** (required by project toolchain: AGP 8.7.3 / Gradle 8.10.2 / Kotlin 2.0.21)
- Android SDK installed; `local.properties` must point to it (e.g. `sdk.dir=/opt/android-sdk`)

## Build (constrained environment)
Use the project Gradle wrapper and disable the daemon/build cache with limited heap:

```bash
JAVA_HOME=<path-to-jdk17> ./gradlew :app:assembleDebug -Dorg.gradle.jvmargs="-Xmx1536m -XX:MaxMetaspaceSize=512m" -Dorg.gradle.daemon=false --no-build-cache
JAVA_HOME=<path-to-jdk17> ./gradlew :app:lint -Dorg.gradle.jvmargs="-Xmx1536m -XX:MaxMetaspaceSize=512m" -Dorg.gradle.daemon=false --no-build-cache
```

Outputs:
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Lint report: `app/build/reports/lint-results-debug.html`

## Project knowledge
See `docs/project-knowledge/` (index: `00_PROJECT_INDEX.md`) and `ROAD GUIDE.txt` for the product/spec source of truth.

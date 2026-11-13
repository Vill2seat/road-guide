# Current Implementation Status — Road Guide

## Overview
Fact-based status of major components.

**Last Updated**: 2026-11-13  
**Based On**: `ROAD GUIDE.txt`, `docs/project-knowledge/*`, repository inspection, and local build verification.

## Status Legend
| Status | Meaning |
|--------|---------|
| ✅ Implemented | Code exists, compiles, basic function verified |
| 🟡 Partially Implemented | Some code exists, gaps documented |
| 📋 Planned | Specified, no code yet |
| 🚫 Blocked | Dependency missing / external blocker |
| ❌ Not Started | Not yet addressed |

## Build / Verification (verified)
- **Toolchain:** Gradle wrapper 8.10.2, AGP 8.7.3, Kotlin 2.0.21 (+ compose plugin), KSP 2.0.21-1.0.28, Compose BOM 2024.10.01, compileSdk/targetSdk 35, minSdk 26, Java 17.
- **JDK:** build verified with `JAVA_HOME=/usr/local/sdkman/candidates/java/17.0.21.0+2-ms` (JDK 17 installed in env). Default system JDK is 25 and **not compatible** with this toolchain.
- **Constrained flags:** `-Dorg.gradle.jvmargs="-Xmx1536m -XX:MaxMetaspaceSize=512m" -Dorg.gradle.daemon=false --no-build-cache` (persisted in `gradle.properties` for daemon/jvmargs).
- **Results (2026-11-13):**
  - `:app:assembleDebug` → **BUILD SUCCESSFUL** (APK: `app/build/outputs/apk/debug/app-debug.apk`)
  - `:app:lint` → **BUILD SUCCESSFUL** (report: `app/build/reports/lint-results-debug.html`)

## Component Status Matrix

### Core Infrastructure
| Component | Status | Notes |
|-----------|--------|-------|
| Gradle / Build Config | ✅ Implemented | Wrapper 8.10.2, root/app scripts + `libs.versions.toml` aligned (Room/KSP/DataStore, Compose BOM). |
| Room Database (v1 skeleton) | 🟡 Partially Implemented | `RoadGuideDatabase` (v1, `exportSchema=true`, `Converters`), entities and DAOs present/compiling; schema aligned to spec baseline, needs migration strategy hardening and integration/use. |
| DataStore (tracking prefs) | 🟡 Partially Implemented | `TrackingPrefs` skeleton (enabled/interval/units/aqi threshold) compiles. |
| DI (Hilt/Koin) | 📋 Planned | Not set up. |
| Navigation (Compose) | 🟡 Partially Implemented | M3 `AppNavHost`/bottom nav + routes/screens scaffolded. |
| Theme / Design System | 🟡 Partially Implemented | Tokens/theme/placeholders from Figma export; components incomplete. |

### Data Layer (TASK-011)
| Item | Status | Notes |
|------|--------|-------|
| Entities | 🟡 Partially Implemented | `LocationPoint`, `Route`, `RouteSegment`, `Stay`, `Place`, `EnvironmentalSample`, `ExposureResult`, `DailyReport`, `PrivacyZone`, `DataRetentionPolicy` (Room annotations/indices/relations per spec baseline). |
| DAOs | 🟡 Partially Implemented | `LocationPointDao`, `RouteDao`, `RouteSegmentDao`, `StayDao`, `PlaceDao`, `EnvironmentalSampleDao`, `ExposureResultDao`, `DailyReportDao`, `PrivacyZoneDao` (basic suspend/Flow ops). |
| Type converters | 🟡 Partially Implemented | Enum converters for status/type/activity/transport/privacy/category/data source/quality. |
| Constants | ✅ Implemented | `domain/model/ExposureConstants` and `data/local/ValidatorConstants` set to exact spec values (E factors/refs incl. `TIME_REFERENCE_SECONDS=1800.0`, `EXPOSURE_T_MAX=2.0`, `EXPOSURE_ROAD_SOURCE_FACTOR=1.0`, `EXPOSURE_CALIBRATION_K=5.0` separate, `DEFAULT_SKIN_FACTOR=1.0`, `S` default 1.0, stationary 0.7; env validation 3h/30km/15min). |

### Tracking / Domain / Features
| Component | Status | Notes |
|-----------|--------|-------|
| Background tracking, permissions, geofencing | 📋 Planned | Stubs only (`service/TrackingServiceStub`). |
| Movement/segmentation/exposure/routing/map integrations | 📋 Planned | Not implemented (`map/MapViewStub`, domain/usecase stubs). |
| Feature screens content | 🟡 Partially Implemented | Placeholder screens (tracker/timeline/planner/history/places/profile). |

### Assets
| Component | Status | Notes |
|-----------|--------|-------|
| Figma export | 🟡 Partially Implemented | Screens/tokens/icons/vectors present; ~56 SVG vectors pending re-export due to prior API 429. |

## Notes / Known Gaps
- Room schema is an initial compiling baseline; verify field-level parity with `03_DATA_MODEL_AND_DATABASE.md` as features integrate and add migrations before production.
- `fallbackToDestructiveMigration()` is enabled in DB builder for dev; replace with proper migrations for persisted data.
- Lint/build may emit deprecation warnings; no blocking errors observed in verified run.
- No unit/instrumentation coverage yet for data layer or domain logic.

## Immediate Next Steps
1. Validate Room schema details against spec during repository/usecase integration; add schema tests/migrations policy.
2. Wire repositories/use cases to Room/DataStore and implement tracking/segmentation/exposure pipelines per specs.
3. Expand UI from placeholders using Figma tokens/components and complete remaining asset exports.

# Current Implementation Status — Road Guide

## Overview
Fact-based status of major components.

**Last Updated**: 2026-11-24
**Based On**: `ROAD GUIDE.txt`, `docs/project-knowledge/*`, repository inspection, build/test configuration, and the prior local build verification.

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
- **JDK:** verification re-run on 2026-11-24 with `JAVA_HOME=/usr/local/sdkman/candidates/java/17.0.21.1-ms`. Default system JDK is 25 and **not compatible** with this toolchain.
- **Constrained flags:** `-Dorg.gradle.jvmargs="-Xmx1536m -XX:MaxMetaspaceSize=512m" -Dorg.gradle.daemon=false --no-build-cache` (persisted in `gradle.properties` for daemon/jvmargs).
- **Prior build results (2026-11-13):**
  - `:app:assembleDebug` → **BUILD SUCCESSFUL** (APK: `app/build/outputs/apk/debug/app-debug.apk`)
  - `:app:lint` → **BUILD SUCCESSFUL** (report: `app/build/reports/lint-results-debug.html`)
- **Documentation verification (2026-11-24):**
  - `:app:testDebugUnitTest` → **BUILD SUCCESSFUL**
  - `:app:lint` → **BUILD SUCCESSFUL**
- This documentation update does not replace the need to rerun verification after subsequent code changes.

## Component Status Matrix

### Core Infrastructure
| Component | Status | Notes |
|-----------|--------|-------|
| Gradle / Build Config | ✅ Implemented | Wrapper 8.10.2, root/app scripts + `libs.versions.toml` aligned (Room/KSP/DataStore, Compose BOM). |
| Room Database | 🟡 Partially Implemented | `RoadGuideDatabase` is version 4 with exported schemas, entities/DAOs, and migrations 1→2, 2→3, and 3→4. Migration coverage and production strategy should continue to be verified. |
| DataStore (tracking prefs) | 🟡 Implemented | Persists enabled/paused state, interval, units, and AQI threshold; tracking defaults off. |
| DI (Hilt) | ✅ Implemented | `RoadGuideApplication` enables Hilt; `AppModule` provides database, DAOs, repositories, sources, and use cases. |
| Navigation (Compose) | 🟡 Partially Implemented | M3 `AppNavHost`/bottom nav + routes/screens scaffolded. |
| Theme / Design System | 🟡 Partially Implemented | Tokens/theme/placeholders from Figma export; components incomplete. |

### Data Layer (TASK-011)
| Item | Status | Notes |
|------|--------|-------|
| Entities | ✅ Implemented | `LocationPoint`, `Route`, `RouteSegment`, `Stay`, `Place`, `EnvironmentalSample`, `ExposureResult`, `DailyReport`, `PrivacyZone`, `DataRetentionPolicy` (Room annotations/indices/relations per spec baseline). |
| DAOs | ✅ Implemented | `LocationPointDao`, `RouteDao`, `RouteSegmentDao`, `StayDao`, `PlaceDao`, `EnvironmentalSampleDao`, `ExposureResultDao`, `DailyReportDao`, `PrivacyZoneDao` (basic suspend/Flow ops). |
| Type converters | ✅ Implemented | Enum converters for status/type/activity/transport/privacy/category/data source/quality. |
| Constants | ✅ Implemented | `domain/model/ExposureConstants` and `data/local/ValidatorConstants` set to exact spec values (E factors/refs incl. `TIME_REFERENCE_SECONDS=1800.0`, `EXPOSURE_T_MAX=2.0`, `EXPOSURE_ROAD_SOURCE_FACTOR=1.0`, `EXPOSURE_CALIBRATION_K=5.0` separate, `DEFAULT_SKIN_FACTOR=1.0`, `S` default 1.0, stationary 0.7; env validation 3h/30km/15min). |

### Tracking / Domain / Features
| Component | Status | Notes |
|-----------|--------|-------|
| Background tracking, permissions, geofencing | 🟡 Partially Implemented | Foreground service start/pause/resume/stop, staged runtime permissions, privacy-zone checks, route ID persistence, and boot recovery are wired. Device/OEM recovery behavior still needs instrumentation testing. |
| Movement/segmentation | 🟡 Partially Implemented | On route completion, speed-based activity classification produces persisted route segments and stable stays; GPS gaps over five minutes split segments and are excluded from route distance. Incremental processing, accuracy filtering, and richer movement signals remain. |
| Environmental data | 🟡 Partially Implemented | Open-Meteo weather and hourly Air Quality clients are connected through DI and sample persistence. Invalid/missing AQ values do not produce exposure scores; time-aligned AQ is matched to route points and recent samples are cached. Data are modeled estimates, not station observations; historical API availability and live-network behavior still need verification. |
| Exposure/routing/map integrations | 🟡 Partially Implemented | Exposure aggregation and routing/map components exist but are not fully connected to the recorded activity timeline. |
| Places and privacy zones | 🟡 Partially Implemented | Places and zones have add/edit/delete/search controls. Active circular rules filter recorded history, Timeline, and reports; export has a filtering contract but no file-export flow. Existing private coordinates remain in Room, and `Place.isPrivate` is list metadata only. |
| Feature screens content | 🟡 Partially Implemented | Tracker/timeline/planner/history/profile still have incomplete flows; Places has CRUD and privacy-zone controls. |

### Assets
| Component | Status | Notes |
|-----------|--------|-------|
| Figma export | 🟡 Partially Implemented | Screens/tokens/icons/vectors present; ~56 SVG vectors pending re-export due to prior API 429. |

## Notes / Known Gaps
- Room schema is an initial compiling baseline; verify field-level parity with `03_DATA_MODEL_AND_DATABASE.md` as features integrate and add migrations before production.
- Database builders use the explicit 1→2, 2→3, and 3→4 migrations; do not remove or weaken migration coverage.
- Lint/build may emit deprecation warnings; no blocking errors observed in verified run.
- Focused unit and instrumentation tests now cover database migrations, privacy-zone decisions, exposure aggregation/calculation, and movement segmentation; the latest interface changes have not been built or run through tests.
- Privacy zones are evaluated locally from stored circles; there is no Android geofence registration. Adding a zone does not scrub pre-existing raw coordinates from Room. The export policy is not yet connected to a JSON/file exporter.

## Immediate Next Steps
1. Validate Room schema details against spec during repository/usecase integration; add schema tests/migrations policy.
2. Verify environmental API parsing, time matching, and AQ-data handling on device; then continue connecting route exposure to the recorded activity timeline.
3. Expand UI from placeholders using Figma tokens/components and complete remaining asset exports.

### Domain Layer (TASK-012)

| Item | Status | Notes |
|------|--------|-------|
| AppModel.kt | ✅ Implemented | TrackingSettingsState, RouteSummary, LocationPointSummary domain models v1 - no Room annotations, pure Kotlin data classes. |
| RouteTab.kt | ✅ Implemented | RouteTab enum with all navigation tabs (TRACKER, TIMELINE, PLANNER, HISTORY, PLACES) + TopLevelDestination. |
| AppUseCase.kt | ✅ Implemented | Use case interfaces v1: GetTrackingSettings, GetLocationPoints, GetRouteSummaries, ToggleTracking, SaveRoute - read/observe data, no business logic. |
| No ping()-placeholder | ✅ Implemented | AppRepository wired to DataProvider, no placeholder logic as main logic. |

### Use Case Implementations (TASK-013)

| Item | Status | Notes |
|------|--------|-------|
| GetTrackingSettingsImpl | ✅ Implemented | Reads from TrackingSettingsRepository.preferencesFlow |
| GetLocationPointsImpl | ✅ Implemented | Wires to LocationPointRepository.observeAll() with mapping |
| GetRouteSummariesImpl | ✅ Implemented | Wires to RouteRepository.observeAll() with mapping |
| ToggleTrackingImpl | ✅ Implemented | Calls setTrackingEnabled and returns updated state |
| SaveRouteImpl | ✅ Implemented | Wires to RouteRepository.insert() returning Long |
| RouteMapper | ✅ Implemented | Room entity to domain model mapping (no Room annotations in domain) |
| LocationPointMapper | ✅ Implemented | Room entity to domain model mapping |

### Tracking Pipeline (TASK-013)

| Component | Status | Notes |
|-----------|--------|-------|
| TrackingPipeline | ✅ Skeleton | Location collection, segmentation, exposure calculation, daily report skeleton |
| Exposure calculation | 🟡 Partially Implemented | Uses valid AQ values and millisecond segment timestamps converted to seconds; stores raw and normalized exposure separately. |
| Stay detection | 🟡 Implemented | Requires at least five minutes of low-speed points within a 50 m radius; saved on route completion. |
| DailyReport creation | 🟡 Partially Implemented | Aggregates duration-weighted valid segment results; confidence and AQ coverage are stored as 0–100 percentages. No report is emitted when valid exposure data are absent. |

### UI Layer Integration (TASK-014)

| Item | Status | Notes |
|------|--------|-------|
| TrackerViewModel | 🟡 Partially Implemented | Reads persisted route points and daily report, observes latest environmental sample, filters private points, and drives start/pause/resume/stop. |
| TimelineViewModel | 🟡 Partially Implemented | Emits privacy-filtered chronological activity/stay events and supports local-day selection. |
| HistoryViewModel | ✅ Implemented | StateFlow for historical reports |
| PlacesViewModel | ✅ Implemented | StateFlow for places list |
| ProfileViewModel | ✅ Implemented | StateFlow for tracking settings; toggle starts/stops the service. |
| TrackerScreen | 🟡 Partially Implemented | Live map, recorded route summary, environmental-data fallback, report summary, and Timeline/Report navigation; visual bottom sheet and device recovery checks remain. |
| TimelineScreen | 🟡 Partially Implemented | Daily date navigation, chronological route/activity/stay events, privacy filtering, and empty states; corrections and exposure details remain. |
| DailyReportScreen | 🟡 Partially Implemented | Loads today's report by default, observes updates, lets users select recent dates, and regenerates on refresh; pollutant breakdown and explainability remain. |
| PlannerScreen | 🟡 Partially Implemented | Resolves origin/destination, offers driving/walking/cycling, uses routed geometry, compares available alternatives, and surfaces insufficient AQ data; waypoints and reliable alternatives remain. |
| HistoryScreen | ✅ Updated | Displays reports with Loading/Empty/Error states |
| PlacesScreen | ✅ Updated | CRUD for places and privacy zones with Loading/Empty/Error states |
| ProfileScreen | ✅ Updated | Tracking settings toggle with Loading/Empty/Error states |

### TASK-011: Map Integration & Route Recording

| Item | Status | Notes |
|------|--------|-------|
| MapLibre dependency | ✅ Added | `org.maplibre.maplibre-android:maplibre:11.1.0` |
| Android permissions | ✅ Added | ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, FOREGROUND_SERVICE |
| MapView composable | ✅ Created | `map/MapView.kt` - MapLibre map wrapper |
| RouteLine composable | ✅ Created | `map/RouteLine.kt` - Polylines with optional gradient |
| LocationMarker model | ✅ Created | `map/model/LocationMarker.kt` - Location data wrapper |
| TrackerViewModel | ✅ Updated | Added recording state, route points |
| TrackerScreen | ✅ Updated | Map display + bottom controls |
| TrackingRecordingService | 🟡 Implemented | Location foreground service; persists real route ID, reuses active route, and restores after process death. |
| AppNavHost | ✅ Verified | Tracker route wired correctly |

### Instrumentation Tests (TASK-016)

| Item | Status | Notes |
|------|--------|-------|
| RouteDatabaseTest | ✅ Implemented | Route CRUD with in-memory Room DB (androidTest) |
| LocationPointDatabaseTest | ✅ Implemented | LocationPoint CRUD (androidTest) |
| TrackingSettingsRepositoryTest | ✅ Implemented | DataStore preferences read/write (androidTest) |
| PlaceRepositoryTest | ✅ Implemented | Place CRUD (androidTest) |
| PrivacyZoneRepositoryTest | ✅ Implemented | PrivacyZone CRUD (androidTest) |
| TrackingPipelineIntegrationTest | ✅ Implemented | Full pipeline skeleton (androidTest) |
| ExposureCalculationIntegrationTest | ✅ Implemented | Exposure formula verification (androidTest) |

**Note:** These tests require a connected Android device or emulator to run (`./gradlew :app:connectedDebugAndroidTest`). 

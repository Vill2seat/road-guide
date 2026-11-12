# Current Implementation Status — Road Guide

## Overview

Fact-based status of every major component. Updated after each implementation milestone.

**Last Updated**: 2024-11-12
**Based On**: `ROAD GUIDE.txt` specification + repository inspection

---

## Status Legend

| Status | Meaning |
|--------|---------|
| ✅ **Implemented** | Code exists, compiles, basic function verified |
| 🟡 **Partially Implemented** | Some code exists, gaps documented |
| 📋 **Planned** | Specified, no code yet |
| 🚫 **Blocked** | Dependency missing, decision needed |
| ❌ **Not Started** | Not yet addressed |

---

## Component Status Matrix

### Core Infrastructure

| Component | Status | Notes |
|-----------|--------|-------|
| **Gradle / Build Config** | 📋 Planned | No `build.gradle.kts`, no `settings.gradle.kts` |
| **Room Database** | 📋 Planned | No entities, DAOs, or Database class |
| **Dependency Injection (Hilt/Koin)** | 📋 Planned | No DI setup |
| **Navigation (Compose)** | 📋 Planned | No NavHost, no bottom nav |
| **Theme / Design System** | 📋 Planned | No `Color.kt`, `Typography.kt`, `Theme.kt` |

### Background Tracking

| Component | Status | Notes |
|-----------|--------|-------|
| **LocationForegroundService** | 📋 Planned | No service class |
| **Adaptive LocationRequest** | 📋 Planned | No interval logic |
| **Tracking Notification** | 📋 Planned | No notification channel/builder |
| **BootReceiver** | 📋 Planned | No receiver |
| **Permission Flow (Runtime)** | 📋 Planned | No rationale screens |
| **Geofencing (Private Zones)** | 📋 Planned | No GeofencingClient usage |

### Movement & Segmentation

| Component | Status | Notes |
|-----------|--------|-------|
| **MovementDetectionEngine** | 📋 Planned | No classification logic |
| **Stay Detection** | 📋 Planned | No radius/duration logic |
| **Transport Detection** | 📋 Planned | No vehicle vs transit logic |
| **RouteSegmentationEngine** | 📋 Planned | No segment/stay builder |
| **Route Completion Logic** | 📋 Planned | No day boundary handling |

### Environmental Data

| Component | Status | Notes |
|-----------|--------|-------|
| **Open-Meteo Weather Client** | 📋 Planned | No Retrofit interface |
| **Open-Meteo AQ Client** | 📋 Planned | No Retrofit interface |
| **Overpass API Client** | 📋 Planned | No query builder |
| **EnvironmentalRepository** | 📋 Planned | No cache/validation |
| **Data Quality Validator** | 📋 Planned | No 3h/30km/15min checks |

### Exposure Engine

| Component | Status | Notes |
|-----------|--------|-------|
| **ExposureCalculationEngine** | 📋 Planned | No factor formulas |
| **Pollution Factor (P)** | 📋 Planned | No normalization |
| **Time Factor (T)** | 📋 Planned | No `T_raw` / `EXPOSURE_T_MAX` |
| **Activity Factor (A)** | 📋 Planned | No lookup table |
| **Road Factor (R)** | 📋 Planned | No OSM context integration |
| **Weather Factor (W)** | 📋 Planned | No wind/rain/temp formulas |
| **Skin Factor (S)** | 📋 Planned | Default 1.0 only |
| **Calibration (K=5.0)** | 📋 Planned | Constant defined, not used |
| **DailyExposureAggregator** | 📋 Planned | No sum/normalize |
| **Confidence Calculation** | 📋 Planned | No weighted formula |
| **Coverage Calculation** | 📋 Planned | No duration-weighted formula |

### Routing & Maps

| Component | Status | Notes |
|-----------|--------|-------|
| **MapLibre Integration** | 📋 Planned | No MapView, no style |
| **OpenFreeMap Tiles** | 📋 Planned | No tile source config |
| **OSRM Client** | 📋 Planned | No routing request |
| **Valhalla Client** | 📋 Planned | No fallback |
| **Route Enrichment Pipeline** | 📋 Planned | No env/geo sampling |
| **Eco Route Planner UI** | 📋 Planned | No screen |

### UI / Screens

| Screen | Status | Notes |
|--------|--------|-------|
| **Splash** | 📋 Planned | No composable |
| **Onboarding (3 pages)** | 📋 Planned | No pager |
| **Permission Screens (4)** | 📋 Planned | No rationale UI |
| **Privacy Setup** | 📋 Planned | No toggles |
| **Main Tracker** | 📋 Planned | No map + bottom sheet |
| **Bottom Sheet Cards (6)** | 📋 Planned | No card composables |
| **Full Timeline** | 📋 Planned | No vertical timeline |
| **Daily Report** | 📋 Planned | No gauge, no sections |
| **Exposure Details** | 📋 Planned | No breakdown |
| **Eco Route Planner** | 📋 Planned | No form |
| **Route Alternatives** | 📋 Planned | No map + cards |
| **Route Details** | 📋 Planned | No screen |
| **Places List** | 📋 Planned | No screen |
| **Place Details** | 📋 Planned | No screen |
| **History** | 📋 Planned | No list + filters |
| **Settings (8 sections)** | 📋 Planned | No preference UI |
| **Profile / Auth** | 📋 Planned | No screens |

### Design System

| Component | Status | Notes |
|-----------|--------|-------|
| **Color Tokens** | 📋 Planned | No `Color.kt` |
| **Typography** | 📋 Planned | No `Typography.kt` |
| **Spacing** | 📋 Planned | No `Spacing.kt` |
| **Icon Set** | 📋 Planned | No XML/Vector drawables |
| **Card Components** | 📋 Planned | No `RGCard` |
| **Button Variants** | 📋 Planned | No `PrimaryButton` etc |
| **Bottom Sheet** | 📋 Planned | No `TrackerBottomSheet` |
| **Timeline Item** | 📋 Planned | No `TimelineItem` |
| **Exposure Gauge** | 📋 Planned | No `ExposureScoreDisplay` |
| **Chips/Pills** | 📋 Planned | No chip composables |
| **Map Markers** | 📋 Planned | No marker composables |
| **Form Fields** | 📋 Planned | No `RGTextField` |
| **Dialogs** | 📋 Planned | No dialog composables |
| **Empty/Error States** | 📋 Planned | No `ScreenState` sealed class |

### Testing

| Area | Status | Notes |
|------|--------|-------|
| **Unit Tests** | 📋 Planned | No test directory |
| **Integration Tests** | 📋 Planned | No `androidTest` |
| **Robolectric Setup** | 📋 Planned | No test runner config |
| **MockWebServer** | 📋 Planned | No API mocks |
| **Device Test Scripts** | 📋 Planned | No manual procedures |
| **CI Pipeline** | 📋 Planned | No GitHub Actions |

---

## Repository State (Actual)

```
road-guide/
├── .git/
├── README.md          # 1 line: "# road-guide"
├── ROAD GUIDE.txt     # 1823 lines - full specification
└── docs/
    └── project-knowledge/  # 12 documents created this session
        ├── 00_PROJECT_INDEX.md
        ├── 01_PRODUCT_VISION.md
        ├── 02_SYSTEM_ARCHITECTURE.md
        ├── 03_DATA_MODEL_AND_DATABASE.md
        ├── 04_GPS_AND_TRACKING.md
        ├── 05_EXPOSURE_ENGINE_AND_FORMULAS.md
        ├── 06_ENVIRONMENTAL_DATA_AND_INTEGRATIONS.md
        ├── 07_ROUTING_AND_MAPS.md
        ├── 08_UI_UX_AND_DESIGN_SYSTEM.md
        ├── 09_API_AND_DATA_CONTRACTS.md
        ├── 10_BACKGROUND_SERVICES_AND_PERMISSIONS.md
        ├── 11_TESTING_AND_QUALITY.md
        └── 12_TECHNICAL_DECISIONS.md
```

**No source code exists** — this is a greenfield project with only the specification document.

---

## Known Gaps vs Specification

| Spec Section | Gap |
|--------------|-----|
| Part 4 (Tech Stack) | No Gradle, no dependencies declared |
| Part 5 (Background Location) | No Foreground Service, no permissions |
| Part 6 (Movement Detection) | No engine, no classification |
| Part 7 (Timeline) | No segmentation, no timeline builder |
| Part 8 (Geographic Context) | No Overpass client, no OSM parsing |
| Part 9 (Environmental Data) | No API clients, no validator |
| Part 10 (Exposure Engine) | No formulas, no constants |
| Part 11 (Daily Aggregator) | No normalization, no report builder |
| Part 12 (Eco Route Planner) | No routing, no planner UI |
| Part 13 (Database) | No Room entities |
| Part 14 (Privacy) | No private zones, no export/delete |
| Part 15-16 (UI/Screens) | Zero Compose code |
| Part 17 (Navigation) | No NavHost |
| Part 21-22 (Design System) | No tokens, no Figma export yet |
| Part 25 (Integrations) | No HTTP clients |
| Part 27-30 (UX States) | No state handling |
| Part 31-33 (Testing) | No tests |

---

## Immediate Next Steps (Priority Order)

1. **Initialize Android Project**
   - `build.gradle.kts` with Compose, Room, Retrofit, MapLibre, Hilt
   - `settings.gradle.kts`
   - Minimum SDK 24, Target 34, Compile 34
   - Kotlin 1.9+, AGP 8.3+

2. **Design System Foundation**
   - `Color.kt`, `Typography.kt`, `Spacing.kt`, `Theme.kt`
   - Basic `RGCard`, `PrimaryButton`, `RGTextField`

3. **Room Database Layer**
   - All 9 entities + DAOs + Database class
   - Migrations stub

4. **Foreground Service Skeleton**
   - Service + Notification + BootReceiver
   - Permission request flow

5. **Environmental API Clients**
   - Open-Meteo Weather + AQ interfaces
   - MockWebServer tests

6. **Exposure Engine Core**
   - Factor functions + unit tests
   - Constants object

7. **Movement/Segmentation Engine**
   - Basic classification (speed-based)
   - Stay detection

8. **MapLibre Integration**
   - MapView + OpenFreeMap style
   - Tracker screen skeleton

---

## Verification Checklist

Before claiming any component "Implemented":

- [ ] Code compiles (`./gradlew assembleDebug`)
- [ ] Unit tests pass (`./gradlew test`)
- [ ] Lint clean (`./gradlew lint`)
- [ ] Ktlint clean (`./gradlew ktlintCheck`)
- [ ] Documented in relevant `.md` file
- [ ] Constants match `ROAD GUIDE.txt` exactly
- [ ] No hardcoded values that should be configurable
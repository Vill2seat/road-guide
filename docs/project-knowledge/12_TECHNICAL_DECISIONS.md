# Technical Decisions (ADR Log) — Road Guide

## Overview

Architecture Decision Records documenting significant technical choices, trade-offs, and rationale.

**Status**: 📋 Planned — Decisions recorded from specification analysis.

---

## ADR Format

| Field | Description |
|-------|-------------|
| **ID** | ADR-YYYYMMDD-XXX |
| **Status** | Proposed / Accepted / Superseded / Deprecated |
| **Date** | Decision date |
| **Context** | Problem statement |
| **Decision** | What was chosen |
| **Consequences** | Trade-offs, follow-up work |
| **Related** | Files, docs, issues |

---

## ADR-20241112-001: Local-First Architecture

**Status**: Accepted
**Date**: 2024-11-12

### Context
Road Guide handles sensitive location data. Users must trust that their daily movements aren't uploaded without consent.

### Decision
- All location data (points, routes, stays) stored locally in Room database
- No automatic cloud sync or analytics
- Environmental API calls use coordinates only (no user identity)
- Optional account/sync as future opt-in feature

### Consequences
- ✅ Privacy by default
- ✅ Works offline
- ❌ No cross-device history without sync
- ❌ Data loss if phone lost (mitigated by export)

### Related
- `03_DATA_MODEL_AND_DATABASE.md` — Room entities
- `10_BACKGROUND_SERVICES_AND_PERMISSIONS.md` — Privacy controls

---

## ADR-20241112-002: Foreground Service for Background Tracking

**Status**: Accepted
**Date**: 2024-11-12

### Context
Android restricts background location access. App must track continuously (screen off, app closed).

### Decision
- Use `ForegroundService` with `FOREGROUND_SERVICE_TYPE_LOCATION`
- Persistent notification with pause/resume actions
- `START_STICKY` + `BootReceiver` for process death recovery
- Adaptive intervals to balance battery vs accuracy

### Consequences
- ✅ Meets Android 10+ background location requirements
- ✅ User awareness via notification
- ✅ Survives process death
- ⚠️ Notification always visible (cannot hide on Android 14+)
- ⚠️ Battery impact (mitigated by adaptive intervals)

### Related
- `04_GPS_AND_TRACKING.md` — Service architecture
- `10_BACKGROUND_SERVICES_AND_PERMISSIONS.md` — Permission flow

---

## ADR-20241112-003: Exposure Model — Relative Score (0–100), Not Medical Dose

**Status**: Accepted
**Date**: 2024-11-12

### Context
Spec explicitly forbids medical claims. Exposure must be "relative ecological load estimation."

### Decision
- Formula: `E = K × P × T × A × R × W × S` → normalized 0–100
- No μg/m³ dose output
- No health risk language
- UI labels: "Estimated Exposure", "Environmental Exposure Score"
- Confidence & Coverage always shown separately

### Consequences
- ✅ Avoids regulatory/liability issues
- ✅ Honest about model limitations
- ⚠️ Users may misinterpret score as absolute
- ⚠️ Requires clear disclaimer in UI (Settings → Environmental Preferences)

### Related
- `05_EXPOSURE_ENGINE_AND_FORMULAS.md` — Full formula documentation
- `08_UI_UX_AND_DESIGN_SYSTEM.md` — Exposure display components

---

## ADR-20241112-004: Open-Meteo + CAMS for Environmental Data

**Status**: Accepted
**Date**: 2024-11-12

### Context
Need global air quality + weather data. No personal sensors.

### Decision
- Primary: Open-Meteo Weather API + Air Quality API (CAMS model)
- No API key required (free tier: 10k/day)
- Hourly resolution, ~11km (EU) / ~45km (global)
- Cache 1 hour, validate: max age 3h, max distance 30km

### Consequences
- ✅ Free, no auth, global coverage
- ✅ Consistent weather + AQ from same provider
- ⚠️ Resolution too coarse for street-level (mitigated by OSM context)
- ⚠️ Modeled data, not measured (documented in Confidence)
- ⚠️ Rate limits (cache aggressively)

### Related
- `06_ENVIRONMENTAL_DATA_AND_INTEGRATIONS.md` — API contracts

---

## ADR-20241112-005: OSM + Overpass for Geographic Context

**Status**: Accepted
**Date**: 2024-11-12

### Context
Need road types, green areas, industrial zones for exposure modifiers.

### Decision
- OpenStreetMap via Overpass API
- Query radius: 200m (segments), 500m (stays)
- Cache 24 hours
- Tags: highway, landuse, leisure, natural, industrial, amenity, railway

### Consequences
- ✅ Rich, global, free geographic data
- ✅ Community maintained
- ⚠️ Overpass public instance rate-limited (self-host for production)
- ⚠️ Data quality varies by region
- ⚠️ Not real-time (OSM updates propagate)

### Related
- `06_ENVIRONMENTAL_DATA_AND_INTEGRATIONS.md` — Overpass queries

---

## ADR-20241112-006: OSRM for Routing (Self-Hosted)

**Status**: Accepted
**Date**: 2024-11-12

### Context
Eco Route Planner needs multiple route alternatives with geometry.

### Decision
- OSRM as primary routing engine
- Self-hosted (public demo unreliable)
- Profiles: driving, walking, cycling
- `alternatives=true` for multiple routes
- Valhalla as fallback/alternative (supports transit)

### Consequences
- ✅ Fast, free, supports alternatives
- ✅ Customizable profiles
- ⚠️ Self-hosting requires infrastructure
- ⚠️ No public transit in OSRM (Valhalla needed)
- ⚠️ Alternative routes not guaranteed

### Related
- `07_ROUTING_AND_MAPS.md` — Routing architecture

---

## ADR-20241112-007: MapLibre + OpenFreeMap for Map Display

**Status**: Accepted
**Date**: 2024-11-12

### Context
Need vector map rendering with custom styling for exposure overlays.

### Decision
- MapLibre Native Android (FOSS fork of Mapbox)
- OpenFreeMap tiles (Liberty style, free, no key)
- Custom style JSON for pastel palette + exposure layers
- Offline caching via MapLibre offline manager

### Consequences
- ✅ No Mapbox token/pricing
- ✅ Full style control
- ✅ GPU accelerated
- ⚠️ Tile provider has no SLA
- ⚠️ Style maintenance burden

### Related
- `07_ROUTING_AND_MAPS.md` — Map stack
- `08_UI_UX_AND_DESIGN_SYSTEM.md` — Map colors

---

## ADR-20241112-008: Jetpack Compose + MVVM + Repository

**Status**: Accepted
**Date**: 2024-11-12

### Context
Modern Android UI, testable architecture, reactive state.

### Decision
- UI: Jetpack Compose (Material 3)
- Architecture: MVVM with Use Cases (Clean Architecture)
- State: `StateFlow` / `MutableStateFlow`
- DI: Hilt (or existing project solution)
- Navigation: Compose Navigation + Bottom Nav

### Consequences
- ✅ Declarative UI, less boilerplate
- ✅ Testable domain layer
- ✅ Reactive updates
- ⚠️ Compose learning curve
- ⚠️ MapLibre Compose integration less mature

### Related
- `02_SYSTEM_ARCHITECTURE.md` — Package structure
- `08_UI_UX_AND_DESIGN_SYSTEM.md` — Compose components

---

## ADR-20241112-009: Room Database with Incremental Migrations

**Status**: Accepted
**Date**: 2024-11-12

### Context
Complex relational data (points, routes, segments, stays, places, env samples).

### Decision
- Room with `@Entity` classes
- Foreign keys with CASCADE/SET_NULL
- Versioned migrations (no destructive in prod)
- Indices on timestamp, routeId, coordinate for query performance
- DataStore for simple preferences

### Consequences
- ✅ Type-safe, compile-time verified SQL
- ✅ Reactive `Flow` queries
- ✅ Migration safety
- ⚠️ Schema changes require migration code
- ⚠️ Large LocationPoint table needs cleanup policy

### Related
- `03_DATA_MODEL_AND_DATABASE.md` — Full schema

---

## ADR-20241112-010: No Dark Mode (v1)

**Status**: Accepted
**Date**: 2024-11-12

### Context
Spec defines light pastel palette. Dark mode adds significant design/testing effort.

### Decision
- v1: Light theme only (pastel greens/blues)
- Design tokens support dark mode structurally (separate color scheme)
- Dark mode as post-v1 feature

### Consequences
- ✅ Faster v1 delivery
- ✅ Consistent with spec visuals
- ⚠️ User expectation for dark mode
- ⚠️ Battery on OLED (minor)

### Related
- `08_UI_UX_AND_DESIGN_SYSTEM.md` — Color tokens

---

## ADR-20241112-011: Activity Recognition API Optional

**Status**: Accepted
**Date**: 2024-11-12

### Context
`ACTIVITY_RECOGNITION` permission improves transport classification but requires runtime prompt.

### Decision
- Make Activity Recognition optional
- Core classification uses GPS speed/acceleration/OSM
- If granted → enhance confidence
- If denied → degrade gracefully (show lower confidence)

### Consequences
- ✅ Fewer permission prompts
- ✅ Works without extra permission
- ⚠️ Lower classification accuracy without it
- ⚠️ Must handle both paths in MovementEngine

### Related
- `04_GPS_AND_TRACKING.md` — Movement detection
- `10_BACKGROUND_SERVICES_AND_PERMISSIONS.md` — Permission flow

---

## ADR-20241112-012: Exposure Normalization — Sigmoid Function

**Status**: Proposed
**Date**: 2024-11-12

### Context
Daily raw exposure sum must map to 0–100. Simple capping loses differentiation.

### Decision
- Use logistic/sigmoid normalization: `100 / (1 + exp(-k × (raw - x0)))`
- Calibrate `k` (steepness) and `x0` (midpoint) from test data
- Alternative: percentile mapping from population simulation

### Consequences
- ✅ Smooth, monotonic, bounded
- ✅ Differentiates high-exposure days
- ⚠️ Requires calibration data
- ⚠️ Parameters need validation

### Related
- `05_EXPOSURE_ENGINE_AND_FORMULAS.md` — Daily aggregation

---

## ADR-20241112-013: Private Zones via Geofencing API

**Status**: Accepted
**Date**: 2024-11-12

### Context
Users need to exclude sensitive locations (home, doctor) from detailed history.

### Decision
- Android Geofencing API for enter/exit detection
- Points in zone flagged with `PRIVATE_ZONE` quality bit
- Stays in zone: `privacyStatus = EXCLUDED`
- Timeline shows placeholder; exposure optionally excluded

### Consequences
- ✅ System-managed, battery-efficient
- ✅ Real-time zone awareness
- ⚠️ Geofence limit: 100 per app
- ⚠️ Requires location permission (already have)

### Related
- `10_BACKGROUND_SERVICES_AND_PERMISSIONS.md` — Private zones
- `03_DATA_MODEL_AND_DATABASE.md` — PrivacyZone entity

---

## ADR-20241112-014: Kotlinx Serialization for Export/Import

**Status**: Accepted
**Date**: 2024-11-12

### Context
User data export (GDPR) and potential future sync.

### Decision
- `@Serializable` on export DTOs
- JSON format with version field
- ISO-8601 timestamps, epoch ms for compactness
- Separate export schema from Room entities

### Consequences
- ✅ Multi-platform, Kotlin-native
- ✅ Versioning support
- ⚠️ Custom serializers for Instant/LocalDate

### Related
- `09_API_AND_DATA_CONTRACTS.md` — Export format

---

## ADR-20241112-015: No Crash Reporting SDK (v1)

**Status**: Accepted
**Date**: 2024-11-12

### Context
Privacy-first: no third-party data collection.

### Decision
- v1: No Firebase Crashlytics, Sentry, etc.
- Use Play Console Vitals + local crash logs
- User-initiated bug report via email intent

### Consequences
- ✅ Zero third-party data
- ✅ Simpler build
- ⚠️ Slower crash detection
- ⚠️ Manual reproduction needed

### Related
- `10_BACKGROUND_SERVICES_AND_PERMISSIONS.md` — Privacy controls

---

## Decision Index

| ID | Topic | Status |
|----|-------|--------|
| ADR-001 | Local-first architecture | Accepted |
| ADR-002 | Foreground Service tracking | Accepted |
| ADR-003 | Relative exposure score (0–100) | Accepted |
| ADR-004 | Open-Meteo + CAMS | Accepted |
| ADR-005 | OSM + Overpass | Accepted |
| ADR-006 | OSRM self-hosted routing | Accepted |
| ADR-007 | MapLibre + OpenFreeMap | Accepted |
| ADR-008 | Compose + MVVM + Repository | Accepted |
| ADR-009 | Room with migrations | Accepted |
| ADR-010 | No dark mode v1 | Accepted |
| ADR-011 | Activity Recognition optional | Accepted |
| ADR-012 | Sigmoid normalization | Proposed |
| ADR-013 | Geofencing for private zones | Accepted |
| ADR-014 | Kotlinx Serialization | Accepted |
| ADR-015 | No crash reporting SDK | Accepted |

---

## Process for New Decisions

1. Create ADR file in `docs/architecture/decisions/ADR-YYYYMMDD-XXX.md`
2. Use format above
3. Link from relevant technical document
4. Update this index
5. Never delete — mark Superseded if overturned
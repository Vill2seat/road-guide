# Project Context — Road Guide

## For AI Agents & Developers

**Read this first.** Then follow `docs/project-knowledge/14_AI_WORK_RULES.md`.

---

## What Is Road Guide?

**Personal Daily Environmental Exposure Tracker & Eco Route Planner** for Android.

- Automatically tracks movements in background (Foreground Service)
- Builds daily Timeline: walks, rides, stays, places
- Enriches with air quality (Open-Meteo/CAMS), weather, OSM geographic context
- Calculates **Exposure Score 0–100** (relative estimate, not medical dose)
- Shows Daily Report with contributors, recommendations
- Eco Route Planner: compares route alternatives by exposure

**Core Principle**: "Know your route. Understand your exposure. Choose better."

---

## Project Knowledge Base

**Location**: `docs/project-knowledge/`

**Start Here**: `docs/project-knowledge/00_PROJECT_INDEX.md`

**Key Documents**:
| Need | Document |
|------|----------|
| Product vision & user scenarios | `01_PRODUCT_VISION.md` |
| System architecture & data flows | `02_SYSTEM_ARCHITECTURE.md` |
| Database schema (Room) | `03_DATA_MODEL_AND_DATABASE.md` |
| GPS, tracking, movement detection | `04_GPS_AND_TRACKING.md` |
| Exposure formulas & constants | `05_EXPOSURE_ENGINE_AND_FORMULAS.md` |
| External APIs (Open-Meteo, OSM, Routing) | `06_ENVIRONMENTAL_DATA_AND_INTEGRATIONS.md` |
| Routing & MapLibre | `07_ROUTING_AND_MAPS.md` |
| UI/UX, Design System, Screens | `08_UI_UX_AND_DESIGN_SYSTEM.md` |
| API contracts, DTOs, mappers | `09_API_AND_DATA_CONTRACTS.md` |
| Background service, permissions | `10_BACKGROUND_SERVICES_AND_PERMISSIONS.md` |
| Testing strategy | `11_TESTING_AND_QUALITY.md` |
| Technical decisions (ADR log) | `12_TECHNICAL_DECISIONS.md` |
| Current implementation status | `13_CURRENT_IMPLEMENTATION_STATUS.md` |
| **AI Work Rules (mandatory)** | `14_AI_WORK_RULES.md` |

---

## Figma Design Export

**Location**: `design/figma-export/`

**Inventory**: `design/figma-export/FIGMA_EXPORT_INDEX.md`

**Figma Source**: https://www.figma.com/design/wsVbXcY4p9BfsyywSE0a6Q/road-guide

| Folder | Contents |
|--------|----------|
| `screens/` | All app screens by flow |
| `components/` | Reusable UI components |
| `icons/` | Icon set (SVG/PNG) |
| `illustrations/` | Onboarding, empty states |
| `logos/` | App logo variants |
| `assets/` | Other graphics |
| `styles/` | Color tokens, typography, spacing |

---

## Original Specification

**File**: `ROAD GUIDE.txt` (root)

**Purpose**: Read-only reference. 1823 lines covering product, architecture, APIs, UI, formulas, testing.

**Do Not Modify**. Use knowledge base for daily work.

---

## Current State (2024-11-12)

> **Greenfield project** — No source code exists yet.

| Area | Status |
|------|--------|
| Build config (Gradle) | 📋 Planned |
| Room database | 📋 Planned |
| Foreground Service | 📋 Planned |
| Movement/Segmentation engines | 📋 Planned |
| Environmental API clients | 📋 Planned |
| Exposure Engine | 📋 Planned |
| Routing clients | 📋 Planned |
| MapLibre integration | 📋 Planned |
| Jetpack Compose UI | 📋 Planned |
| Design System | 📋 Planned |
| Tests | 📋 Planned |

See `docs/project-knowledge/13_CURRENT_IMPLEMENTATION_STATUS.md` for full matrix.

---

## Mandatory Rules Summary

1. **Read knowledge base first** — `00_PROJECT_INDEX.md` → task-relevant docs
2. **Verify in code** — docs describe intent, code is truth
3. **Three states** — ✅ Implemented / 🟡 Partial / 📋 Planned — never conflate
4. **Constants are sacred** — `K=5.0`, `T_MAX=2.0`, `STATIONARY=0.7`, etc. — no changes without audit
5. **No medical claims** — Exposure Score = relative 0–100 estimate
6. **Missing env data ≠ zero** — show incomplete, reduce confidence
7. **Privacy first** — local storage, no mandatory auth, private zones
8. **Architecture changes** → dependency analysis + ADR
9. **Update knowledge base** after significant changes
10. **Test before done** — `./gradlew test lint ktlintCheck`

Full rules: `docs/project-knowledge/14_AI_WORK_RULES.md`

---

## Quick Commands (When Project Initialized)

```bash
# Build
./gradlew assembleDebug

# Test
./gradlew test          # Unit
./gradlew connectedCheck # Instrumented

# Quality
./gradlew lint
./gradlew ktlintCheck
./gradlew detekt

# Format
./gradlew ktlintFormat
```

---

## For Human Developers

- **Spec**: `ROAD GUIDE.txt`
- **Architecture**: `docs/project-knowledge/02_SYSTEM_ARCHITECTURE.md`
- **Database**: `docs/project-knowledge/03_DATA_MODEL_AND_DATABASE.md`
- **Formulas**: `docs/project-knowledge/05_EXPOSURE_ENGINE_AND_FORMULAS.md`
- **Design**: `design/figma-export/` + `docs/project-knowledge/08_UI_UX_AND_DESIGN_SYSTEM.md`
- **Decisions**: `docs/project-knowledge/12_TECHNICAL_DECISIONS.md`

---

## Contact / References

- **Figma**: https://www.figma.com/design/wsVbXcY4p9BfsyywSE0a6Q/road-guide
- **Open-Meteo**: https://open-meteo.com/en/docs
- **CAMS**: https://atmosphere.copernicus.eu/
- **OSM/Overpass**: https://overpass-api.de/
- **OSRM**: https://project-osrm.org/
- **MapLibre**: https://maplibre.org/
- **Valhalla**: https://valhalla.github.io/valhalla/
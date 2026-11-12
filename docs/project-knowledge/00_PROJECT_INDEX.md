# Project Knowledge Base Index — Road Guide

## Overview

**Road Guide** — Personal Daily Environmental Exposure Tracker & Eco Route Planner for Android.

**Primary Purpose**: Automatic monitoring of user movements throughout the day and estimation of environmental exposure with ability to build alternative routes with lower pollution impact.

**Core Concept**: Not just a GPS tracker — a personal monitor of daily environmental exposure.

---

## Document Priority & Reading Order

### Required First Read (Entry Points)
1. **00_PROJECT_INDEX.md** — This file
2. **01_PRODUCT_VISION.md** — Product concept, user scenarios, core value
3. **13_CURRENT_IMPLEMENTATION_STATUS.md** — What actually exists vs planned

### Core Architecture & Data
4. **02_SYSTEM_ARCHITECTURE.md** — System architecture, data flows, component interactions
5. **03_DATA_MODEL_AND_DATABASE.md** — Room entities, relationships, migrations
6. **04_GPS_AND_TRACKING.md** — Location tracking, movement detection, background services

### Exposure Engine (Critical)
7. **05_EXPOSURE_ENGINE_AND_FORMULAS.md** — All mathematical formulas, coefficients, calibration constants

### Integrations & External Services
8. **06_ENVIRONMENTAL_DATA_AND_INTEGRATIONS.md** — Open-Meteo, OSM, Overpass, routing APIs
9. **07_ROUTING_AND_MAPS.md** — MapLibre, OSRM/Valhalla, route planning

### UI/UX & Design
10. **08_UI_UX_AND_DESIGN_SYSTEM.md** — Screens, navigation, components, design tokens
11. **design/figma-export/FIGMA_EXPORT_INDEX.md** — Exported Figma assets reference

### Contracts & Technical Details
12. **09_API_AND_DATA_CONTRACTS.md** — DTOs, API requests/responses, data formats
13. **10_BACKGROUND_SERVICES_AND_PERMISSIONS.md** — Foreground services, permissions, battery
14. **11_TESTING_AND_QUALITY.md** — Test structure, quality criteria

### Governance & Decisions
15. **12_TECHNICAL_DECISIONS.md** — ADR log, architectural decisions
16. **14_AI_WORK_RULES.md** — Mandatory rules for AI agents working on this project

---

## Key Principles

| Principle | Description |
|-----------|-------------|
| **Local-First** | All location data stored locally by default |
| **No Medical Claims** | Exposure Score is relative estimate (0–100), not measured dose |
| **Transparency** | Confidence & Coverage shown separately from Score |
| **User Control** | Pause tracking, private zones, data export/delete |
| **Background Operation** | Works when app closed, screen off, via Foreground Service |
| **Graceful Degradation** | No environmental data ≠ zero pollution; show warnings |

---

## Three States of Information

Every document **must** distinguish:

- ✅ **Implemented** — Confirmed in source code
- 🟡 **Partially Implemented** — Some parts exist, gaps documented
- 📋 **Planned** — Specified in spec, not yet coded

> **Current State**: This is a greenfield project. **Everything is Planned**. No source code exists yet.

---

## Figma Design Export

Location: `design/figma-export/`

- `screens/` — All app screens by flow
- `components/` — Reusable UI components
- `icons/` — Icon set (SVG/PNG)
- `illustrations/` — Illustrations, onboarding graphics
- `logos/` — App logo variants
- `assets/` — Other graphical assets
- `styles/` — Color tokens, typography, spacing
- `FIGMA_EXPORT_INDEX.md` — Inventory of exported assets

Figma URL: https://www.figma.com/design/wsVbXcY4p9BfsyywSE0a6Q/road-guide

---

## Quick Reference: Core Constants (from Spec)

| Constant | Value | Location |
|----------|-------|----------|
| `T_raw` | `durationSeconds / 1800.0` | Exposure Engine |
| `EXPOSURE_T_MAX` | `2.0` | Exposure Engine |
| `EXPOSURE_ROAD_SOURCE_FACTOR` | `1.0` | Exposure Engine |
| `EXPOSURE_CALIBRATION_K` | `5.0` | Exposure Engine |
| `Skin Factor (S)` | `1.0` (default) | Exposure Engine |
| Activity Factor: Stationary | `0.7` | Exposure Engine |
| Activity Factor: Running | Up to `1.5` | Exposure Engine |
| Max Environmental Data Age | `3 hours` | Data Validator |
| Max Coordinate Distance | `30 km` | Data Validator |
| Max Future Timestamp Offset | `15 minutes` | Data Validator |

---

## Update Rules

1. After any architectural change → update affected documents
2. After implementing a Planned feature → change status to Implemented
3. After discovering discrepancy → document in `TECHNICAL_DECISIONS.md`
4. Never delete or overwrite original spec (`ROAD GUIDE.txt`)
5. Keep `CURRENT_IMPLEMENTATION_STATUS.md` synchronized with reality

---

## For New AI Agents

**Start Here**: Read this index → `PRODUCT_VISION.md` → `CURRENT_IMPLEMENTATION_STATUS.md`

Then read task-relevant documents. **Do not re-read the full 1800-line spec** unless resolving a specific contradiction.
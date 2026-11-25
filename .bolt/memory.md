# Project memory

## Product intent & audience

- **Road Guide** — Android app for personal environmental exposure tracking + eco-route planning.
- Automatically tracks daily movements in background, enriches with environmental data (air quality, weather, geographic context), calculates relative **Environmental Exposure Score (0–100)**.
- Eco Route Planner compares routes by environmental impact rather than speed/distance.
- Audience: health- and environment-conscious individuals.
- Explicitly **not medical** — score is a relative estimate, not a measured dose.

## Brand personality & voice

- Core principle: "Know your route. Understand your exposure. Choose better."
- Honest, non-medical, privacy-first tone.
- No medical claims permitted; missing data never reported as zero.

## Visual design & UX direction

- Jetpack Compose UI; MapLibre maps with OpenFreeMap tiles.
- Figma designs exist but ~56 SVG vectors pending re-export; screens not yet Figma-faithful.
- Feature screens (Tracker, Timeline, Planner) are scaffolds with real data binding; many flows incomplete.
- Bottom sheet design and full user flows still pending.

## Decisions, constraints & rationale

- **Stack**: Kotlin + Jetpack Compose, MVVM + Repository + Clean Architecture, Hilt DI, Room (v4, migrations 1→4, no destructive fallback), Coroutines/Flow/StateFlow.
- **Background tracking**: foreground service (location type) + persistent notification; boot recovery wired but not device-tested.
- **External APIs**: Open-Meteo (weather + air quality), Overpass/OSM (roads, land use), OSRM (route alternatives).
- **Exposure formula**: `E_segment = K × (P × T × A × R × W × S)`; K=5.0 (sacred constant — no changes without audit).
  - P=pollution (WHO 2021 thresholds), T=time (cap 2.0), A=activity (0.7–1.5+), R=road/geographic (0.5–2.0+), W=weather (0.5–1.5), S=skin (1.0 placeholder).
  - Daily aggregation normalizes via sigmoid/logistic to 0–100; confidence & coverage tracked separately from score.
- **Data validation**: max age 3h, max coord distance 30km, max future 15min; failures degrade confidence/coverage and show warnings, never zero pollution.
- **Settings**: DataStore for simple key-values; Room for complex objects (privacy zones, retention policy).
- **10 Room entities**: LocationPoint, Route, RouteSegment, Stay, Place, EnvironmentalSample, ExposureResult, DailyReport, PrivacyZone, DataRetentionPolicy.
- **AI work rules**: knowledge-base-first, verify-in-code-not-docs, sacred constants, no medical claims, missing-data≠zero, privacy-first, two-attempt rule, test-before-claiming-done.
- `.bolt/prompt` is authoritative for global instructions; not restated here.

## Open questions & assumptions

- Exposure engine not fully connected end-to-end with activity timeline.
- Privacy zones are local-only — adding a zone does not scrub pre-existing raw coordinates from Room; `Place.isPrivate` is metadata only.
- No data export flow implemented (contract defined, no JSON/file exporter).
- Foreground service + boot recovery not instrumented-tested on real devices/OEMs.
- Open-Meteo historical data availability and live network behavior unverified on-device.
- UI screens incomplete; Figma fidelity and bottom sheet design pending.
- Skin factor S=1.0 reserved for future extension.
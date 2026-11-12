# Product Vision — Road Guide

## Purpose

Road Guide is a **Personal Daily Environmental Exposure Tracker & Eco Route Planner** for Android.

It automatically monitors user movements throughout the day, enriches location/time data with environmental context (air quality, weather, geographic factors), calculates a relative **Exposure Score (0–100)**, and presents a chronological **Daily Timeline** with an end-of-day **Daily Report**.

Additionally, it provides an **Eco Route Planner** for manual route comparison by environmental impact.

---

## Core User Scenarios

### 1. Automatic Daily Monitoring (Primary)
```
07:30  Home → Walking to bus stop (1.1 km, 18 min)
07:48  Bus → Public transport to work (6.4 km, 24 min)
08:12  Walking → Office (450 m, 7 min)
08:20  Work → Stay (8h 45m)
17:05  Driving → Home (5.8 km, 19 min)
17:24  Park → Stay (42 min)
19:00  Home → End of day
```

**User does nothing** — app runs in background, builds timeline automatically.

### 2. Daily Report Consumption
- Overall Exposure Score (0–100) with Confidence & Coverage
- Breakdown by pollutant (PM2.5, PM10, NO₂, O₃, etc.)
- Top exposure contributors (time, place, reason)
- Best/worst environmental periods
- Actionable recommendations (route shifts, timing changes)

### 3. Eco Route Planner (Secondary)
User enters Origin → Destination → Mode → Gets alternatives:
| Route | Distance | Time | Exposure Score | Characteristic |
|-------|----------|------|----------------|----------------|
| A | 4.2 km | 52 min | 68 | Fastest |
| B | 4.8 km | 58 min | 39 | Balanced |
| C | 5.1 km | 64 min | 27 | Lowest Exposure |

User sees trade-offs: cleaner route may be longer.

### 4. History & Places
- Day-by-day history with filter (date, exposure level, transport, places)
- Place profiles: Home, Work, Frequent, Recent, Saved
- Visit frequency, duration, environmental stats per place

---

## Key Product Principles

| Principle | Implication |
|-----------|-------------|
| **Background-First** | No manual start/stop per trip; Foreground Service with persistent notification |
| **Local-First Privacy** | Location history stays on device; no mandatory account |
| **Estimation, Not Measurement** | Exposure Score = relative model (0–100), not μg/m³ dose |
| **Transparency** | Confidence & Coverage always shown alongside Score |
| **User Control** | Pause tracking, Private Zones, data export/delete, correction of misclassified events |
| **Graceful Degradation** | Missing env data → warning + incomplete flag, never "zero pollution" |
| **Explainability** | Every score shows contributing factors in plain language |

---

## Target User Value

> "Know your route. Understand your exposure. Choose better."

**Not**: "Here's your GPS track."
**But**: "Here's how the environment accompanied you today, and what you could change tomorrow."

---

## Primary Screens (from Spec)

| Screen | Purpose |
|--------|---------|
| **Tracker (Main)** | Map background + bottom sheet: Today's Overview, Daily Exposure, Current Environment, Recent Activity, Timeline access, Quick Actions |
| **Full Timeline** | Chronological day view with editable events |
| **Daily Report** | End-of-day summary with breakdown, contributors, recommendations |
| **Exposure Details** | Formula explanation, factor breakdown, day chart |
| **Eco Route Planner** | Origin/Destination/Waypoints + mode → route alternatives |
| **Route Alternatives** | Map with multiple routes, comparison cards |
| **Places** | Saved/frequent places with visit history & env stats |
| **History** | Day cards with score, coverage, distance, event count |
| **Settings** | Tracking, Privacy, Environmental, Notifications, Map, Units, Account |

---

## Non-Goals (Explicit)

- ❌ Medical diagnosis or health risk assessment
- ❌ Real-time pollutant measurement (uses modeled data, ~11–45km resolution)
- ❌ Indoor air quality measurement (GPS/CAMS cannot measure indoors)
- ❌ Social features, sharing, community
- ❌ Mandatory cloud sync or account
- ❌ Turn-by-turn navigation (route selection only)

---

## Success Criteria (from Spec)

- No point lost without recorded reason
- Data survives process death/reboot
- Long stays don't disappear from Timeline
- Stops don't become hundreds of fake routes
- Unknown activity ≠ invented activity
- Missing env data ≠ zero pollution
- Exposure Score is explainable
- Confidence & Coverage shown separately
- All screens have error/empty/offline states
- Core features work without registration
- Private zones work predictably
- Map + scrollable panel UX remains smooth
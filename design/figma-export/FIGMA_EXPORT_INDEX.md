# Figma Export Index — Road Guide

## Overview

This directory contains exported design assets from the Road Guide Figma file.

**Figma URL**: https://www.figma.com/design/wsVbXcY4p9BfsyywSE0a6Q/road-guide

**Export Date**: 2024-11-12
**Export Method**: Manual via Figma UI (Dev Mode) — no API token in repo

**Repository status (2026-11-25):** The four style-token JSON files are present.
The screen inventory contains 31 default frames exported at @2x and @3x (62 PNGs).
The export is incomplete: component sheets, alternate screen states, several listed
frames, illustrations, and logos are not present. Icons are partial; see
[`icons/ICON_INDEX.md`](./icons/ICON_INDEX.md) for the vector inventory.

---

## Export Checklist

### ✅ Design System (Styles)

| Category | Items | Format | Status |
|----------|-------|--------|--------|
| **Color Styles** | Primary, Secondary, Background, Surface, Text, Exposure (4 levels), State (Error/Warning/Success) | JSON | ✅ Exported |
| **Typography Styles** | Display, Headline, Title, Body, Label (all sizes/weights) | JSON | ✅ Exported |
| **Spacing System** | space1–space16, radius, elevation | JSON | ✅ Exported |
| **Shadow/Elevation** | elevation1–elevation4 | JSON | ✅ Exported |

### ✅ Components

| Component | Variants | States | Format | Status |
|-----------|----------|--------|--------|--------|
| **Buttons** | Primary, Secondary, Tertiary, Icon, FAB | Enabled, Pressed, Disabled, Loading | SVG/PNG | ❌ Not exported |
| **Cards** | Surface, Elevated, Outlined, Exposure (4 border colors) | Default, Pressed, Disabled | SVG/PNG | ❌ Not exported |
| **Bottom Navigation** | 5 tabs | Active, Inactive, Badge | SVG/PNG | ❌ Not exported as a component sheet |
| **Top App Bar** | With title, with actions | Default, Scrolled | SVG/PNG | ❌ Not exported as a component sheet |
| **Bottom Sheet** | Half, Expanded, Drag handle | Default, Dragging | SVG/PNG | ❌ Not exported as a component sheet |
| **Timeline Item** | Walking, Cycling, Vehicle, Transit, Stay | Default, Active, Corrected | SVG/PNG | ❌ Not exported as a component sheet |
| **Exposure Gauge** | Small, Medium, Large | All score ranges | SVG/PNG | ❌ Not exported as a component sheet |
| **Chips/Pills** | Filter, Status, Exposure, Activity | Selected, Unselected | SVG/PNG | ❌ Not exported as a component sheet |
| **Form Fields** | Text, Search, Multiline | Empty, Filled, Error, Disabled | SVG/PNG | ❌ Not exported as a component sheet |
| **Dialogs** | Confirmation, Permission, Error, Bottom Sheet | Default | SVG/PNG | ❌ Not exported as a component sheet |
| **Map Markers** | Current, Place (5 categories), Stop, Waypoint | Default, Selected | SVG/PNG | ❌ Not exported as a component sheet |
| **Map Layers** | Green areas, Major roads, Exposure heatmap, Private zones | On, Off | SVG/PNG | ❌ Not exported as a component sheet |

### ✅ Screens (by Flow)

#### Onboarding Flow
| Screen | Frame Name | Format | Status |
|--------|------------|--------|--------|
| Splash | `Splash` | PNG (2x, 3x) | ✅ Exported |
| Onboarding Page 1 | `Onboarding/TrackYourDay` | PNG (2x, 3x) | ❌ Not exported as a separate frame |
| Onboarding Page 2 | `Onboarding/UnderstandExposure` | PNG (2x, 3x) | ❌ Not exported as a separate frame |
| Onboarding Page 3 | `Onboarding/ChooseBetterRoutes` | PNG (2x, 3x) | ❌ Not exported as a separate frame |
| Location Permission | `Permissions/Location` | PNG (2x, 3x) | ✅ Exported |
| Background Location | `Permissions/BackgroundLocation` | PNG (2x, 3x) | ✅ Exported |
| Activity Recognition | `Permissions/ActivityRecognition` | PNG (2x, 3x) | ✅ Exported |
| Notifications | `Permissions/Notifications` | PNG (2x, 3x) | ✅ Exported |
| Privacy Setup | `Onboarding/PrivacySetup` | PNG (2x, 3x) | ✅ Exported |

#### Tracker (Main) Flow
| Screen | Frame Name | Format | Status |
|--------|------------|--------|--------|
| Main Tracker (collapsed) | `Tracker/Collapsed` | PNG (2x, 3x) | ✅ Exported |
| Main Tracker (half-expanded) | `Tracker/HalfExpanded` | PNG (2x, 3x) | ❌ Not exported |
| Main Tracker (fully expanded) | `Tracker/FullyExpanded` | PNG (2x, 3x) | ✅ Exported (named `09-tracker-expanded`) |
| Tracker — Map Layers Open | `Tracker/MapLayers` | PNG (2x, 3x) | ❌ Not exported |
| Tracker — Tracking Paused | `Tracker/Paused` | PNG (2x, 3x) | ❌ Not exported |

#### Timeline Flow
| Screen | Frame Name | Format | Status |
|--------|------------|--------|--------|
| Full Daily Timeline | `Timeline/FullDay` | PNG (2x, 3x) | ✅ Exported |
| Timeline — Event Details | `Timeline/EventDetails` | PNG (2x, 3x) | ✅ Exported |
| Timeline — Edit Activity | `Timeline/EditActivity` | PNG (2x, 3x) | ❌ Not exported |
| Timeline — Edit Place | `Timeline/EditPlace` | PNG (2x, 3x) | ❌ Not exported |

#### Daily Report Flow
| Screen | Frame Name | Format | Status |
|--------|------------|--------|--------|
| Daily Report (main) | `Report/DailyReport` | PNG (2x, 3x) | ✅ Exported |
| Exposure Details | `Report/ExposureDetails` | PNG (2x, 3x) | ✅ Exported |
| Report — View on Map | `Report/ViewOnMap` | PNG (2x, 3x) | ❌ Not exported |

#### Eco Route Planner Flow
| Screen | Frame Name | Format | Status |
|--------|------------|--------|--------|
| Planner — Input | `Planner/Input` | PNG (2x, 3x) | ✅ Exported |
| Route Alternatives | `Planner/Alternatives` | PNG (2x, 3x) | ✅ Exported |
| Route Details | `Planner/RouteDetails` | PNG (2x, 3x) | ✅ Exported |
| Route Comparison | `Planner/Comparison` | PNG (2x, 3x) | ❌ Not exported |

#### Places & History
| Screen | Frame Name | Format | Status |
|--------|------------|--------|--------|
| Places List | `Places/List` | PNG (2x, 3x) | ✅ Exported |
| Place Details | `Places/Details` | PNG (2x, 3x) | ✅ Exported |
| History List | `History/List` | PNG (2x, 3x) | ✅ Exported |
| History Filters | `History/Filters` | PNG (2x, 3x) | ❌ Not exported |

#### Settings & Profile
| Screen | Frame Name | Format | Status |
|--------|------------|--------|--------|
| Settings Main | `Settings/Main` | PNG (2x, 3x) | ✅ Exported |
| Tracking Settings | `Settings/Tracking` | PNG (2x, 3x) | ✅ Exported |
| Privacy & Data | `Settings/Privacy` | PNG (2x, 3x) | ✅ Exported |
| Environmental Preferences | `Settings/Environmental` | PNG (2x, 3x) | ✅ Exported |
| Notifications Settings | `Settings/Notifications` | PNG (2x, 3x) | ✅ Exported |
| Map Settings | `Settings/Map` | PNG (2x, 3x) | ✅ Exported |
| Units & Language | `Settings/UnitsLanguage` | PNG (2x, 3x) | ✅ Exported |
| Account | `Settings/Account` | PNG (2x, 3x) | ❌ Not exported as a separate frame |
| About | `Settings/About` | PNG (2x, 3x) | ✅ Exported |
| Notifications Center | `Notifications/Main` | PNG (2x, 3x) | ✅ Exported |
| Profile | `Profile/Main` | PNG (2x, 3x) | ✅ Exported |
| Sign In | `Auth/SignIn` | PNG (2x, 3x) | ✅ Exported |
| Create Account | `Auth/CreateAccount` | PNG (2x, 3x) | ✅ Exported |

### ✅ Screen States (Each Screen)

| State | Required | Format |
|-------|----------|--------|
| **Default** | ✅ Yes for 31 frames | PNG @2x/@3x |
| **Loading** | ❌ Not exported as screen states | PNG |
| **Empty** | ❌ Not exported as screen states | PNG |
| **Error** | ❌ Not exported as screen states | PNG |
| **Offline** | ❌ Not exported as screen states | PNG |
| **Partial/Stale Data** | ❌ Not exported as screen states | PNG |
| **Permission Denied** | ❌ Not exported as screen states | PNG |
| **Disabled** | ❌ Not exported as screen states | PNG |

### ✅ Icons

| Category | Icons | Format | Status |
|----------|-------|--------|--------|
| **Navigation** | map, timeline, route, calendar, map-pin, user, settings, bell, search, plus, layers, locate, chevron-down | SVG (24×24) | 🟡 Partial; see `icons/ICON_INDEX.md` |
| **Transport** | footprints, car, bus, bicycle, train, tram, motorcycle, ferry | SVG (24×24) | 🟡 Partial; see `icons/ICON_INDEX.md` |
| **Environment** | tree, wind, cloud-sun, droplets, sun, cloud, gauge, leaf | SVG (24×24) | 🟡 Partial; see `icons/ICON_INDEX.md` |
| **Actions** | play, pause, check, pencil, trash, edit, filter, crosshair, shield, alert, info, battery, refresh, wifi-off | SVG (24×24) | 🟡 Partial; see `icons/ICON_INDEX.md` |
| **Status** | check-circle, close-circle, help-circle, warning, success, error | SVG (24×24) | 🟡 Partial; see `icons/ICON_INDEX.md` |

### ✅ Illustrations

| Illustration | Use Case | Format | Status |
|--------------|----------|--------|--------|
| Onboarding 1 — Track | Page 1 | SVG/PNG | ❌ Not exported |
| Onboarding 2 — Exposure | Page 2 | SVG/PNG | ❌ Not exported |
| Onboarding 3 — Routes | Page 3 | SVG/PNG | ❌ Not exported |
| Empty Timeline | No data | SVG/PNG | ❌ Not exported |
| Empty History | No days | SVG/PNG | ❌ Not exported |
| Empty Places | No places | SVG/PNG | ❌ Not exported |
| Error — GPS | GPS unavailable | SVG/PNG | ❌ Not exported |
| Error — Network | API unreachable | SVG/PNG | ❌ Not exported |
| Error — Permission | Location denied | SVG/PNG | ❌ Not exported |
| Private Zone | Zone visualization | SVG/PNG | ❌ Not exported |

### ✅ Logos

| Variant | Format | Status |
|---------|--------|--------|
| Full color (icon + wordmark) | SVG, PNG (512×512) | ❌ Not exported |
| Icon only | SVG, PNG (512×512) | ❌ Not exported |
| Monochrome (light) | SVG, PNG | ❌ Not exported |
| Monochrome (dark) | SVG, PNG | ❌ Not exported |
| Splash variant | PNG (2x, 3x) | ❌ Not exported |

---

## Directory Structure

```
design/figma-export/
├── screens/
│   ├── onboarding/
│   ├── tracker/
│   ├── timeline/
│   ├── planner/
│   ├── history/
│   ├── places/
│   ├── profile/
│   └── settings/
├── icons/
├── styles/
│   ├── colors.json
│   ├── typography.json
│   ├── spacing.json
│   └── shadows.json
└── FIGMA_EXPORT_INDEX.md          # This file
```

---

## Export Instructions (Manual)

### Via Figma Dev Mode (Recommended)

1. Open Figma file: https://www.figma.com/design/wsVbXcY4p9BfsyywSE0a6Q/road-guide
2. Enable **Dev Mode** (Shift+D)
3. Select frame/component
4. Right-click → **Export** → Choose format:
   - **Screens**: PNG @2x, @3x
   - **Icons/Components**: SVG (preferred) + PNG @2x
   - **Illustrations/Logos**: SVG + PNG @2x
5. Save to corresponding folder under `design/figma-export/`

### Via Figma UI (Alternative)

1. Select frames/components in Layers panel
2. Bottom right: **Export** section
3. Add export settings: PNG @2x, @3x / SVG
3. Click **Export [Name]**
4. Save to correct subfolder

### Color & Typography Tokens

1. In Dev Mode, select **Styles** panel
2. Copy color values, typography specs to JSON files in `styles/`
3. Or use Figma Tokens plugin to export as JSON

---

## Naming Convention

```
<flow>/<screen>-<state>@<scale>.<ext>
```

Examples:
- `tracker/main-collapsed@2x.png`
- `tracker/main-half-expanded@3x.png`
- `components/button-primary-enabled.svg`
- `icons/navigation-map.svg`
- `illustrations/empty-timeline.svg`
- `logos/full-color.svg`

---

## Quality Checks

Before committing exports:

- [ ] All screens have Default, Loading, Empty, Error states
- [ ] Icons are 24×24dp baseline, stroke 2dp, consistent style
- [ ] Colors match design tokens exactly (no drift)
- [ ] PNG exports at @2x and @3x for all screens
- [ ] SVG exports for all vector assets (icons, logos, illustrations)
- [ ] No duplicate/near-duplicate assets
- [ ] File sizes reasonable (PNG < 500KB, SVG < 50KB)

---

## Missing / Unable to Export

| Asset | Reason | Alternative |
|-------|--------|-------------|
| 56 Figma vectors | Figma API returned HTTP 429; see `icons/ICON_INDEX.md` | Use available vectors or outlined Compose icons |
| Component sheets, illustrations, and logos | Not present in repository | Export from Figma Dev Mode |
| Loading/empty/error/offline screen states | Not present in repository | Export after defining state frames |

**Screen export location:** Report screenshots currently live in `screens/timeline/`;
the `screens/report/` directory shown in the original plan has not been created.

---

## Integration with Code

| Code Location | References |
|---------------|------------|
| `docs/project-knowledge/08_UI_UX_AND_DESIGN_SYSTEM.md` | Color tokens, typography, component specs |
| `docs/project-knowledge/02_SYSTEM_ARCHITECTURE.md` | Map style JSON references tile colors |
| Compose Theme | `Color.kt`, `Typography.kt` generated from `styles/` |
| Icon composables | `Icons.kt` uses outlined Compose vectors; the Figma SVG set is incomplete and is not fully integrated |
| Illustrations | No exported illustration assets or `Illustrations.kt` wrapper yet |

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2024-11-12 | Initial index created from spec |
| 1.1 | 2026-11-25 | Updated inventory from repository: 31 default screen frames, style-token JSONs, partial icons; marked missing frames and asset categories |

---

## Next Steps

1. Export remaining screen frames and state variants from Figma.
2. Export component sheets, illustrations, and logos.
3. Re-export the 56 missing vectors when the Figma API limit allows.
4. Verify the added exports against this inventory and update the status tables.
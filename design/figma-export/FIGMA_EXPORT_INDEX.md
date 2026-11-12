# Figma Export Index — Road Guide

## Overview

This directory contains exported design assets from the Road Guide Figma file.

**Figma URL**: https://www.figma.com/design/wsVbXcY4p9BfsyywSE0a6Q/road-guide

**Export Date**: 2024-11-12
**Export Method**: Manual via Figma UI (Dev Mode) — no API token in repo

---

## Export Checklist

### ✅ Design System (Styles)

| Category | Items | Format | Status |
|----------|-------|--------|--------|
| **Color Styles** | Primary, Secondary, Background, Surface, Text, Exposure (4 levels), State (Error/Warning/Success) | JSON + PNG swatches | 📋 Pending |
| **Typography Styles** | Display, Headline, Title, Body, Label (all sizes/weights) | JSON | 📋 Pending |
| **Spacing System** | space1–space16, radius, elevation | JSON | 📋 Pending |
| **Shadow/Elevation** | elevation1–elevation4 | JSON | 📋 Pending |

### ✅ Components

| Component | Variants | States | Format | Status |
|-----------|----------|--------|--------|--------|
| **Buttons** | Primary, Secondary, Tertiary, Icon, FAB | Enabled, Pressed, Disabled, Loading | SVG/PNG | 📋 Pending |
| **Cards** | Surface, Elevated, Outlined, Exposure (4 border colors) | Default, Pressed, Disabled | SVG/PNG | 📋 Pending |
| **Bottom Navigation** | 5 tabs | Active, Inactive, Badge | SVG/PNG | 📋 Pending |
| **Top App Bar** | With title, with actions | Default, Scrolled | SVG/PNG | 📋 Pending |
| **Bottom Sheet** | Half, Expanded, Drag handle | Default, Dragging | SVG/PNG | 📋 Pending |
| **Timeline Item** | Walking, Cycling, Vehicle, Transit, Stay | Default, Active, Corrected | SVG/PNG | 📋 Pending |
| **Exposure Gauge** | Small, Medium, Large | All score ranges | SVG/PNG | 📋 Pending |
| **Chips/Pills** | Filter, Status, Exposure, Activity | Selected, Unselected | SVG/PNG | 📋 Pending |
| **Form Fields** | Text, Search, Multiline | Empty, Filled, Error, Disabled | SVG/PNG | 📋 Pending |
| **Dialogs** | Confirmation, Permission, Error, Bottom Sheet | Default | SVG/PNG | 📋 Pending |
| **Map Markers** | Current, Place (5 categories), Stop, Waypoint | Default, Selected | SVG/PNG | 📋 Pending |
| **Map Layers** | Green areas, Major roads, Exposure heatmap, Private zones | On, Off | SVG/PNG | 📋 Pending |

### ✅ Screens (by Flow)

#### Onboarding Flow
| Screen | Frame Name | Format | Status |
|--------|------------|--------|--------|
| Splash | `Splash` | PNG (2x, 3x) | 📋 Pending |
| Onboarding Page 1 | `Onboarding/TrackYourDay` | PNG (2x, 3x) | 📋 Pending |
| Onboarding Page 2 | `Onboarding/UnderstandExposure` | PNG (2x, 3x) | 📋 Pending |
| Onboarding Page 3 | `Onboarding/ChooseBetterRoutes` | PNG (2x, 3x) | 📋 Pending |
| Location Permission | `Permissions/Location` | PNG (2x, 3x) | 📋 Pending |
| Background Location | `Permissions/BackgroundLocation` | PNG (2x, 3x) | 📋 Pending |
| Activity Recognition | `Permissions/ActivityRecognition` | PNG (2x, 3x) | 📋 Pending |
| Notifications | `Permissions/Notifications` | PNG (2x, 3x) | 📋 Pending |
| Privacy Setup | `Onboarding/PrivacySetup` | PNG (2x, 3x) | 📋 Pending |

#### Tracker (Main) Flow
| Screen | Frame Name | Format | Status |
|--------|------------|--------|--------|
| Main Tracker (collapsed) | `Tracker/Collapsed` | PNG (2x, 3x) | 📋 Pending |
| Main Tracker (half-expanded) | `Tracker/HalfExpanded` | PNG (2x, 3x) | 📋 Pending |
| Main Tracker (fully expanded) | `Tracker/FullyExpanded` | PNG (2x, 3x) | 📋 Pending |
| Tracker — Map Layers Open | `Tracker/MapLayers` | PNG (2x, 3x) | 📋 Pending |
| Tracker — Tracking Paused | `Tracker/Paused` | PNG (2x, 3x) | 📋 Pending |

#### Timeline Flow
| Screen | Frame Name | Format | Status |
|--------|------------|--------|--------|
| Full Daily Timeline | `Timeline/FullDay` | PNG (2x, 3x) | 📋 Pending |
| Timeline — Event Details | `Timeline/EventDetails` | PNG (2x, 3x) | 📋 Pending |
| Timeline — Edit Activity | `Timeline/EditActivity` | PNG (2x, 3x) | 📋 Pending |
| Timeline — Edit Place | `Timeline/EditPlace` | PNG (2x, 3x) | 📋 Pending |

#### Daily Report Flow
| Screen | Frame Name | Format | Status |
|--------|------------|--------|--------|
| Daily Report (main) | `Report/DailyReport` | PNG (2x, 3x) | 📋 Pending |
| Exposure Details | `Report/ExposureDetails` | PNG (2x, 3x) | 📋 Pending |
| Report — View on Map | `Report/ViewOnMap` | PNG (2x, 3x) | 📋 Pending |

#### Eco Route Planner Flow
| Screen | Frame Name | Format | Status |
|--------|------------|--------|--------|
| Planner — Input | `Planner/Input` | PNG (2x, 3x) | 📋 Pending |
| Route Alternatives | `Planner/Alternatives` | PNG (2x, 3x) | 📋 Pending |
| Route Details | `Planner/RouteDetails` | PNG (2x, 3x) | 📋 Pending |
| Route Comparison | `Planner/Comparison` | PNG (2x, 3x) | 📋 Pending |

#### Places & History
| Screen | Frame Name | Format | Status |
|--------|------------|--------|--------|
| Places List | `Places/List` | PNG (2x, 3x) | 📋 Pending |
| Place Details | `Places/Details` | PNG (2x, 3x) | 📋 Pending |
| History List | `History/List` | PNG (2x, 3x) | 📋 Pending |
| History Filters | `History/Filters` | PNG (2x, 3x) | 📋 Pending |

#### Settings & Profile
| Screen | Frame Name | Format | Status |
|--------|------------|--------|--------|
| Settings Main | `Settings/Main` | PNG (2x, 3x) | 📋 Pending |
| Tracking Settings | `Settings/Tracking` | PNG (2x, 3x) | 📋 Pending |
| Privacy & Data | `Settings/Privacy` | PNG (2x, 3x) | 📋 Pending |
| Environmental Preferences | `Settings/Environmental` | PNG (2x, 3x) | 📋 Pending |
| Notifications Settings | `Settings/Notifications` | PNG (2x, 3x) | 📋 Pending |
| Map Settings | `Settings/Map` | PNG (2x, 3x) | 📋 Pending |
| Units & Language | `Settings/UnitsLanguage` | PNG (2x, 3x) | 📋 Pending |
| Account | `Settings/Account` | PNG (2x, 3x) | 📋 Pending |
| About | `Settings/About` | PNG (2x, 3x) | 📋 Pending |
| Profile | `Profile/Main` | PNG (2x, 3x) | 📋 Pending |
| Sign In | `Auth/SignIn` | PNG (2x, 3x) | 📋 Pending |
| Create Account | `Auth/CreateAccount` | PNG (2x, 3x) | 📋 Pending |

### ✅ Screen States (Each Screen)

| State | Required | Format |
|-------|----------|--------|
| **Default** | ✅ Yes | PNG |
| **Loading** | ✅ Yes | PNG |
| **Empty** | ✅ Yes | PNG |
| **Error** | ✅ Yes | PNG |
| **Offline** | ✅ Yes | PNG |
| **Partial/Stale Data** | ✅ Yes | PNG |
| **Permission Denied** | ✅ Yes | PNG |
| **Disabled** | Where applicable | PNG |

### ✅ Icons

| Category | Icons | Format | Status |
|----------|-------|--------|--------|
| **Navigation** | map, timeline, route, calendar, map-pin, user, settings, bell, search, plus, layers, locate, chevron-down | SVG (24×24) | 📋 Pending |
| **Transport** | footprints, car, bus, bicycle, train, tram, motorcycle, ferry | SVG (24×24) | 📋 Pending |
| **Environment** | tree, wind, cloud-sun, droplets, sun, cloud, gauge, leaf | SVG (24×24) | 📋 Pending |
| **Actions** | play, pause, check, pencil, trash, edit, filter, crosshair, shield, alert, info, battery, refresh, wifi-off | SVG (24×24) | 📋 Pending |
| **Status** | check-circle, close-circle, help-circle, warning, success, error | SVG (24×24) | 📋 Pending |

### ✅ Illustrations

| Illustration | Use Case | Format | Status |
|--------------|----------|--------|--------|
| Onboarding 1 — Track | Page 1 | SVG/PNG | 📋 Pending |
| Onboarding 2 — Exposure | Page 2 | SVG/PNG | 📋 Pending |
| Onboarding 3 — Routes | Page 3 | SVG/PNG | 📋 Pending |
| Empty Timeline | No data | SVG/PNG | 📋 Pending |
| Empty History | No days | SVG/PNG | 📋 Pending |
| Empty Places | No places | SVG/PNG | 📋 Pending |
| Error — GPS | GPS unavailable | SVG/PNG | 📋 Pending |
| Error — Network | API unreachable | SVG/PNG | 📋 Pending |
| Error — Permission | Location denied | SVG/PNG | 📋 Pending |
| Private Zone | Zone visualization | SVG/PNG | 📋 Pending |

### ✅ Logos

| Variant | Format | Status |
|---------|--------|--------|
| Full color (icon + wordmark) | SVG, PNG (512×512) | 📋 Pending |
| Icon only | SVG, PNG (512×512) | 📋 Pending |
| Monochrome (light) | SVG, PNG | 📋 Pending |
| Monochrome (dark) | SVG, PNG | 📋 Pending |
| Splash variant | PNG (2x, 3x) | 📋 Pending |

---

## Directory Structure

```
design/figma-export/
├── screens/
│   ├── onboarding/
│   ├── tracker/
│   ├── timeline/
│   ├── planner/
│   ├── report/
│   ├── history/
│   ├── places/
│   ├── profile/
│   └── settings/
├── components/
├── icons/
├── illustrations/
├── logos/
├── assets/
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
| *None yet* | Export not performed | — |

**Update this section** after export attempt with any assets that couldn't be exported and why.

---

## Integration with Code

| Code Location | References |
|---------------|------------|
| `docs/project-knowledge/08_UI_UX_AND_DESIGN_SYSTEM.md` | Color tokens, typography, component specs |
| `docs/project-knowledge/02_SYSTEM_ARCHITECTURE.md` | Map style JSON references tile colors |
| Compose Theme | `Color.kt`, `Typography.kt` generated from `styles/` |
| Icon composables | `Icons.kt` references `icons/` SVG |
| Illustrations | `Illustrations.kt` references `illustrations/` |

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2024-11-12 | Initial index created from spec |
| *TBD* | *After export* | Actual export inventory |

---

## Next Steps

1. **Export all assets** from Figma using Dev Mode
2. **Verify** against this checklist
3. **Update** this file with actual exported files
4. **Generate** Compose theme files from `styles/` JSON
5. **Create** icon/illustration Compose wrappers
# UI/UX & Design System — Road Guide

## Overview

Complete UI specification for Road Guide: screens, navigation, components, design tokens, and Figma handoff.

**Status**: 🟡 Partially Implemented — Compose navigation and core screens exist; compare their current behavior with the acceptance scenarios below before extending secondary screens.

## Priority Screen Acceptance Scenarios

| Screen | User scenario / acceptance criteria | Remaining gap |
|--------|-------------------------------------|---------------|
| **Tracker (P0)** | Start, pause, resume, and stop recording; show the persisted route and live position; honor privacy zones on map/history; show missing environmental data as unavailable; provide direct Timeline and Daily Report actions. | Verify permission-denial/recovery and process-death behavior on device; the visual bottom-sheet design and place markers are incomplete. |
| **Timeline (P0)** | Browse by local day; show chronological route, activity, and stay events; show an actionable empty state; exclude private history. | Event correction/editing and richer environmental detail are not implemented. |
| **Daily Report (P0)** | Open today's report by default; display score, confidence, and coverage separately; allow selection of recent report dates; regenerate on refresh; show an insufficient-data state rather than a zero score. | Pollutant breakdown, best/worst period detail, charts, and contributor explanations remain incomplete. |
| **Eco Route Planner (P0)** | Resolve current location/address/coordinates; select driving, walking, or cycling; request real OSRM geometry; compare available alternatives; clearly indicate missing AQ data; never draw a fabricated route. | Waypoints/saved places, route mode availability by region, and reliable alternative-route supply remain incomplete. |
| **History / Places / Profile (P1)** | Extend only after the P0 scenarios above remain usable. | Filtering, export, and several documented profile flows are still partial. |

These are behavior-level acceptance checks, not a claim that the screens have been device-tested.

---

## Design Principles (Per Spec)

| Principle | Application |
|-----------|-------------|
| **Light & Pastel** | Light backgrounds, soft green/blue accents, no dark mode (v1) |
| **Map as Background** | Tracker screen: map = full-screen canvas; cards overlay |
| **Minimalist** | Clean cards, line icons, generous whitespace, no visual clutter |
| **Color = Meaning** | Exposure colors (Good→Very High) only for data, not decoration |
| **Accessible** | Color not sole info carrier; text labels always present |
| **States for Everything** | Loading, Empty, Error, Offline, Permission, Partial, Stale |

---

## Color Palette (Design Tokens)

### Semantic Colors
| Token | Hex | Usage |
|-------|-----|-------|
| `colorPrimary` | #6FAF8E | Primary actions, selected states, route A |
| `colorPrimaryContainer` | #E7F2EB | Primary card backgrounds, chips |
| `colorSecondary` | #5A8FCF | Secondary actions, route B, water |
| `colorSecondaryContainer` | #DCECF3 | Secondary card backgrounds |
| `colorBackground` | #F7F9F7 | Screen background |
| `colorSurface` | #FFFFFF | Card background, sheets |
| `colorSurfaceVariant` | #F0F2F0 | Elevated surfaces, dividers |
| `colorOnBackground` | #26352D | Primary text |
| `colorOnSurface` | #26352D | Text on cards |
| `colorOnSurfaceVariant` | #7B8981 | Secondary text, hints |
| `colorOutline` | #D0D8D3 | Dividers, borders |

### Exposure Status Colors (Data-Driven Only)
| Level | Token | Hex | Label |
|-------|-------|-----|-------|
| Good | `colorExposureGood` | #8FC9A8 | "Good" |
| Moderate | `colorExposureModerate` | #E7D18B | "Moderate" |
| High | `colorExposureHigh` | #E8A77F | "High" |
| Very High | `colorExposureVeryHigh` | #D98787 | "Very High" |

### State Colors
| State | Token | Hex |
|-------|-------|-----|
| Error | `colorError` | #D98787 |
| Warning | `colorWarning` | #E8A77F |
| Success | `colorSuccess` | #8FC9A8 |
| Info | `colorInfo` | #5A8FCF |

---

## Typography

### Font Family
- **Primary**: System font (`FontFamily.Default`) — Roboto / Google Sans on Android
- **Monospace**: `FontFamily.Monospace` — for timestamps, coordinates

### Type Scale
| Style | Font Size | Line Height | Weight | Letter Spacing | Usage |
|-------|-----------|-------------|--------|----------------|-------|
| `displayLarge` | 57 sp | 64 sp | Regular | -0.25 sp | Splash logo |
| `displayMedium` | 45 sp | 52 sp | Regular | 0 sp | — |
| `displaySmall` | 36 sp | 44 sp | Regular | 0 sp | Daily Report score |
| `headlineLarge` | 32 sp | 40 sp | Regular | 0 sp | Screen titles |
| `headlineMedium` | 28 sp | 36 sp | Regular | 0 sp | Card titles |
| `headlineSmall` | 24 sp | 32 sp | Regular | 0 sp | Section headers |
| `titleLarge` | 22 sp | 28 sp | Medium | 0 sp | Card subtitles |
| `titleMedium` | 16 sp | 24 sp | Medium | 0.15 sp | List items, buttons |
| `titleSmall` | 14 sp | 20 sp | Medium | 0.1 sp | Chips, labels |
| `bodyLarge` | 16 sp | 24 sp | Regular | 0.5 sp | Body text |
| `bodyMedium` | 14 sp | 20 sp | Regular | 0.25 sp | Secondary body |
| `bodySmall` | 12 sp | 16 sp | Regular | 0.4 sp | Captions, timestamps |
| `labelLarge` | 14 sp | 20 sp | Medium | 0.1 sp | Button text |
| `labelMedium` | 12 sp | 16 sp | Medium | 0.5 sp | Small buttons |
| `labelSmall` | 11 sp | 16 sp | Medium | 0.5 sp | Tags, badges |

---

## Spacing System

| Token | Value | Usage |
|-------|-------|-------|
| `space0` | 0 dp | — |
| `space1` | 4 dp | Icon gaps, tight groups |
| `space2` | 8 dp | Internal card padding, item spacing |
| `space3` | 12 dp | Medium gaps |
| `space4` | 16 dp | **Standard screen padding**, card margins |
| `space5` | 20 dp | Large gaps |
| `space6` | 24 dp | Section separation |
| `space8` | 32 dp | Major section breaks |
| `space10` | 40 dp | Screen-level margins |
| `space12` | 48 dp | — |
| `space16` | 64 dp | — |

**Border Radius**:
| Token | Value |
|-------|-------|
| `radiusSmall` | 8 dp |
| `radiusMedium` | 12 dp |
| `radiusLarge` | 16 dp |
| `radiusFull` | 999 dp |

**Elevation** (Compose `Shadow`):
| Level | dp | Usage |
|-------|-----|-------|
| `elevation0` | 0 | Flat |
| `elevation1` | 1 | Cards on surface |
| `elevation2` | 3 | Raised cards |
| `elevation3` | 6 | Bottom sheet, dialogs |
| `elevation4` | 12 | Modal bottom sheets |

---

## Icon System

### Style
- **Line icons**, 24×24 dp baseline
- **Stroke width**: 2 dp (consistent)
- **Rounded caps & joins**
- **Source**: Material Symbols (outlined) + custom for domain-specific

### Required Icons (Per Spec)

| Category | Icons |
|----------|-------|
| **Navigation** | `map`, `clock`/`timeline`, `route`, `calendar`, `map-pin`, `user-round`, `settings`, `bell`, `search`, `plus`, `layers`, `locate`, `chevron-down` |
| **Transport** | `footprints` (walking), `car`, `bus`, `bicycle`, `train`, `tram` |
| **Environment** | `tree` (green), `wind`, `cloud-sun`, `droplets`, `sun`, `cloud`, `gauge`/`activity` (exposure) |
| **Actions** | `play`, `pause`, `check`, `pencil`, `trash-2`, `edit`, `filter`, `crosshair`, `shield-check` (privacy), `alert-circle`, `info`, `battery` |
| **Status** | `check-circle`, `close-circle`, `help-circle`, `refresh-cw`, `wifi-off` |

### Icon Usage Rules
- Always pair with text label (accessibility)
- Use `colorOnSurfaceVariant` for inactive, `colorPrimary` for active
- Exposure icons use semantic exposure colors only in data context

---

## Component Library

### 1. Cards
```kotlin
// Base Card
@Composable
fun RGCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    elevation: Dp = 1.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    content: @Composable () -> Unit
)

// Variants
// - SurfaceCard: default, elevation1
// - ElevatedCard: elevation3, for bottom sheet sections
// - OutlinedCard: border 1dp colorOutline, no elevation
// - ExposureCard: colored left border (4dp) by exposure level
```

### 2. Buttons
| Variant | Style | Usage |
|---------|-------|-------|
| `PrimaryButton` | Filled, `colorPrimary`, `onPrimary` text | Main CTAs: "Get Started", "Find Routes" |
| `SecondaryButton` | Outlined, `colorPrimary` border/text | Secondary actions |
| `TertiaryButton` | Text only, `colorPrimary` | "Skip", "Later" |
| `IconButton` | Circular, `colorSurface` bg, icon `colorOnSurface` | Map controls, sheet handles |
| `Fab` | `colorPrimary`, `onPrimary` | "Locate me" on map |

**States**: Enabled, Hover (desktop), Pressed, Disabled, Loading (spinner)

### 3. Bottom Sheet (Tracker Screen)
```kotlin
@Composable
fun TrackerBottomSheet(
    sheetState: BottomSheetState,
    content: @Composable () -> Unit
) {
    ModalBottomSheetLayout(sheetState) {
        // Map fills screen
        MapView()
        
        // Sheet content
        Column(Modifier.fillMaxWidth()) {
            DragHandle()
            TrackerSheetContent()
        }
    }
}
```
**Behavior**: Half-expanded → expanded; map stays behind; drag handle at top.

### 4. Timeline Item (Vertical)
```kotlin
@Composable
fun TimelineItem(
    icon: IconData,
    timeRange: String,        // "07:30–07:48"
    label: String,            // "Walking"
    distance: String?,        // "1.1 km"
    exposureScore: Int?,      // 42
    confidence: ConfidenceLevel,
    isActive: Boolean = false,
    onClick: () -> Unit
)
```
- Vertical connector line (color: `colorOutline`)
- Icon in circle (color by activity type)
- Exposure badge (color by level)

### 5. Exposure Gauge / Score Display
```kotlin
@Composable
fun ExposureScoreDisplay(
    score: Int,               // 0-100
    confidence: ConfidenceLevel,
    coverage: Float,          // 0.0-1.0
    size: GaugeSize = Medium,
    showBreakdown: Boolean = false
)
```
- Circular progress (stroke 8dp) colored by exposure level
- Center: large score number
- Below: "Confidence: Medium • Coverage: 86%"

### 6. Chips / Pills
| Variant | Usage |
|---------|-------|
| `FilterChip` | Map layers, history filters |
| `StatusChip` | Tracking status (Active/Paused), Privacy status |
| `ExposureChip` | Score label with color dot |
| `ActivityChip` | Transport type in timeline |

### 7. Map Marker
```kotlin
@Composable
fun MapMarker(
    type: MarkerType,         // CURRENT_LOCATION, PLACE, STOP, WAYPOINT
    label: String?,
    exposureLevel: ExposureLevel?,
    isSelected: Boolean
)
```
- Current location: blue pulse dot
- Place: colored pin by category
- Stop: small diamond
- Waypoint: numbered circle

### 8. Form Fields
```kotlin
@Composable
fun RGTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    leadingIcon: IconData?,
    trailingIcon: IconData?,
    error: String?,
    keyboardOptions: KeyboardOptions,
    visualTransformation: VisualTransformation?
)
```

### 9. Dialogs
- `ConfirmationDialog` (Delete, Discard changes)
- `PermissionRationaleDialog` (Location, Background, Activity, Notifications)
- `ErrorDialog` (Network, API, GPS)
- `BottomSheetDialog` (Place picker, Mode selector)

### 10. Empty / Error / Loading States
**Every data-driven screen must have**:
```kotlin
sealed interface ScreenState<out T> {
    data class Loading<T>(val previousData: T?) : ScreenState<T>
    data class Success<T>(val data: T) : ScreenState<T>
    data class Empty<T>(val message: String, val illustration: Illustration) : ScreenState<T>
    data class Error<T>(val message: String, val retry: () -> Unit, val isOffline: Boolean) : ScreenState<T>
    data class Partial<T>(val data: T, val warning: String) : ScreenState<T>  // stale/partial data
    data class PermissionDenied<T>(val permission: String, val onRequest: () -> Unit) : ScreenState<T>
}
```

---

## Screen Specifications (Per Spec Part 16)

### 1. Splash Screen
- Logo + "Road Guide" + slogan "Understand your daily environmental exposure"
- Pastel background (#F7F9F7)
- Duration: min 1.5s, max 3s (init completes)

### 2. Onboarding (3 Pages + Buttons)
| Page | Title | Content |
|------|-------|---------|
| 1 | Track Your Day | Auto movement tracking, Daily timeline |
| 2 | Understand Exposure | Air quality + weather, Daily report |
| 3 | Choose Better Routes | Compare alternatives, Lower exposure |
| Buttons | [Get Started] [Sign In] | Sign In optional |

### 3. Permission Screens (Sequential)
| Screen | Permission | Key Message |
|--------|------------|-------------|
| Location | `ACCESS_FINE_LOCATION` | "Record daily movements, recognize places, estimate environmental conditions" |
| Background Location | `ACCESS_BACKGROUND_LOCATION` | "Continue tracking when app not open" + show notification preview + timeline example |
| Activity Recognition | `ACTIVITY_RECOGNITION` | "Distinguish walking, cycling, driving, stationary" |
| Notifications | `POST_NOTIFICATIONS` | "Monitoring status, daily report ready, data warnings" |

### 4. Privacy Setup
- Toggle: Enable Automatic Tracking / Start Manually / Configure Later
- Info cards: Local storage, Pause anytime, Private zones, Delete history

### 5. Main Tracker (Detailed in `02_SYSTEM_ARCHITECTURE.md`)
**Critical Rule**: "Do not place separate 'Activity Type' block on main Tracker screen. Activity type shown in History and Timeline only."

### 6. Full Daily Timeline
- Vertical time axis
- Event cards with: icon, start/end, duration, distance, place, exposure, confidence
- Actions: Edit Activity, Change Place, Correct Transport, Mark Private, Delete

### 7. Daily Report
- Score gauge + Confidence + Coverage
- Sections: Daily Summary, Exposure Breakdown, Main Contributors, Best Periods, Recommendations, Data Quality
- Actions: View on Map, View Timeline, Compare Routes, Share (user-initiated)

### 8. Eco Route Planner
- From / To / +Add Stop
- Mode chips: Walking, Cycling, Driving, Public Transport*
- [Find Routes] → Route Alternatives screen

### 9. Route Alternatives
- Map with all routes (different styles)
- Comparison cards: Distance, Time, Exposure Score, Confidence, Coverage
- Badges: Fastest, Balanced, Lowest Exposure
- Select → Route Details

### 10. Places / Place Details
- Categories: Home, Work, Frequent, Recent, Saved
- Stats: Visits, Duration, Env stats
- Actions: Rename, Category, Private, Delete

### 11. History
- Day cards: Date, Score, Coverage, Confidence, Distance, Event count
- Filters: Date, Exposure Level, Transport, Places

### 12. Settings (Sections)
1. Tracking Settings (Auto, Schedule, Accuracy, Battery, Activity, Background, Pause, Diagnostics)
2. Privacy & Data (Zones, Excluded, Retention, Export, Delete Daily, Delete All, Policy)
3. Environmental Preferences (Sources, AQI Standard, Pollutant Display, Explanation, Disclaimer)
4. Notifications (Daily Report, Tracking Status, Data Warnings, Route Suggestions, Env Alerts)
5. Map Settings (Style, Green Areas, Major Roads, Exposure Overlay, Timeline Points, Private Zones)
6. Units & Language (km/mi, °C/°F, Language, 12h/24h)
7. Account (Sign In/Out, Profile)
8. About (Version, Data Sources, Licenses, Policy, Terms, Contact, Disclaimer)

---

## Navigation Structure

### Bottom Navigation (Primary)
| Tab | Icon | Screen | Deep Links |
|-----|------|--------|------------|
| Map | `map` | Tracker | `tracker` |
| Timeline | `clock`/`timeline` | Full Timeline | `timeline/{date}` |
| Planner | `route` | Eco Route Planner | `planner` |
| History | `calendar` | History | `history` |
| Places | `map-pin` | Places | `places` |

### Top-Level (via User Avatar 👤)
- Profile → Settings, Sign In/Out, Privacy
- Notifications → `bell` → Notifications Center

### Deep Link Scheme
```
roadguide://tracker
roadguide://timeline?date=2024-11-12
roadguide://planner?from=current&to=place_id_123
roadguide://report?date=2024-11-12
roadguide://places
roadguide://settings?section=tracking
```

---

## Adaptive Layout

| Screen Size | Tracker Bottom Sheet | Planner | Timeline |
|-------------|---------------------|---------|----------|
| Compact (< 600dp) | Modal bottom sheet (half/expanded) | Full screen | Full screen |
| Medium (600-840dp) | Side sheet (360dp) or modal | Two-pane (map + list) | Two-pane |
| Expanded (> 840dp) | Permanent side panel | Three-pane | Three-pane |

**Tablet / Foldable**: Map remains primary; secondary content in side panels.

---

## Accessibility

- **TalkBack**: All interactive elements have content descriptions
- **Color Blind**: Exposure colors + text labels; no color-only info
- **Font Scale**: Support up to 200% system font size (Compose handles)
- **Touch Targets**: Minimum 48×48 dp
- **Contrast**: WCAG AA minimum (4.5:1 text, 3:1 UI)

---

## Figma Handoff Reference

**Figma File**: https://www.figma.com/design/wsVbXcY4p9BfsyywSE0a6Q/road-guide

**Pages in Figma** (per spec Part 22):
1. Color Styles
2. Typography Styles
3. Spacing System
4. Buttons (all variants + states)
5. Cards (all variants + states)
6. Bottom Navigation
7. Top Navigation
8. Map Overlays
9. Timeline Components
10. Exposure Indicators
11. Charts
12. Forms
13. Dialogs
14. Permission Screens
15. Settings Components

**Each Screen Has**:
- Default state
- Loading state
- Empty state
- Error state
- Disabled state (where applicable)

**Export Path**: `design/figma-export/` (see `FIGMA_EXPORT_INDEX.md`)

---

## Planned vs Implemented

> **All UI components and screens are 📋 Planned** — no Jetpack Compose code exists.
> This document defines target design per `ROAD GUIDE.txt` (Parts 15, 16, 17, 21, 22, 27).
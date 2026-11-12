# Environmental Data & Integrations — Road Guide

## Overview

Catalog of all external API integrations for environmental, geographic, and routing data.

**Status**: 📋 Planned — Integration specs defined, no implementation exists.

---

## Integration Summary

| Service | Purpose | Auth | Rate Limits | Cache TTL |
|---------|---------|------|-------------|-----------|
| Open-Meteo Weather | Temperature, wind, precipitation, humidity | None | 10,000/day | 1 hour |
| Open-Meteo Air Quality | PM2.5, PM10, NO₂, O₃, SO₂, CO, AQI | None | 10,000/day | 1 hour |
| Overpass API (OSM) | Roads, green areas, industrial, POIs | None | ~1 req/sec (public) | 24 hours |
| OSRM / Valhalla | Route alternatives, geometry, duration | None/Optional | Varies | 1 hour |
| OpenFreeMap / MapLibre | Map tiles, styles | Attribution | Tile CDN limits | Browser cache |

---

## 1. Open-Meteo Weather API

### Endpoint
```
https://api.open-meteo.com/v1/forecast
```

### Parameters (Per Spec)
| Param | Value | Description |
|-------|-------|-------------|
| `latitude` | Decimal | WGS84 lat |
| `longitude` | Decimal | WGS84 lon |
| `hourly` | `temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,rain,showers,wind_speed_10m,wind_direction_10m,cloud_cover,uv_index` | Comma-separated |
| `timezone` | `auto` | Returns local time |
| `timeformat` | `unixtime` | Unix seconds |
| `wind_speed_unit` | `ms` | m/s |
| `forecast_hours` | `48` | Minimum needed |
| `past_hours` | `24` | For historical matching |

### Response Format (Hourly Arrays)
```json
{
  "hourly": {
    "time": [1704067200, 1704070800, ...],
    "temperature_2m": [12.3, 11.8, ...],
    "relative_humidity_2m": [78, 82, ...],
    "apparent_temperature": [11.5, 11.0, ...],
    "precipitation": [0.0, 0.2, ...],
    "rain": [0.0, 0.2, ...],
    "showers": [0.0, 0.0, ...],
    "wind_speed_10m": [3.2, 2.8, ...],
    "wind_direction_10m": [240, 235, ...],
    "cloud_cover": [45, 50, ...],
    "uv_index": [1.2, 0.8, ...]
  },
  "hourly_units": {
    "temperature_2m": "°C",
    "wind_speed_10m": "m/s",
    "precipitation": "mm",
    ...
  }
}
```

### Used In Exposure Engine
| Variable | Role |
|----------|------|
| `temperature_2m` | Weather Factor (temp modulation) |
| `wind_speed_10m` | Weather Factor (dispersion) |
| `wind_direction_10m` | Future: source-receptor alignment |
| `precipitation` / `rain` | Weather Factor (washout) |
| `relative_humidity_2m` | Weather Factor (PM hygroscopic growth) |
| `cloud_cover` | Minor: mixing height proxy |
| `uv_index` | Future: skin exposure context |

### Data Quality Notes
- **Spatial Resolution**: ~11 km (EU domain), ~45 km (global)
- **Temporal Resolution**: Hourly
- **Model**: ECMWF IFS / GFS / ICON (ensemble)
- **Not Measured**: Modeled forecast/analysis, not station observation
- **Latency**: Near real-time (analysis) to forecast

---

## 2. Open-Meteo Air Quality API / CAMS

### Endpoint
```
https://air-quality-api.open-meteo.com/v1/air-quality
```

### Parameters (Per Spec)
| Param | Value |
|-------|-------|
| `latitude` | Decimal |
| `longitude` | Decimal |
| `hourly` | `pm10,pm2_5,nitrogen_dioxide,ozone,sulphur_dioxide,carbon_monoxide,us_aqi,european_aqi` |
| `timezone` | `auto` |
| `timeformat` | `unixtime` |
| `forecast_hours` | `48` |
| `past_hours` | `24` |
| `domains` | `cams_europe` or `cams_global` (auto by coords) |

### Response Format
```json
{
  "hourly": {
    "time": [1704067200, 1704070800, ...],
    "pm10": [18.5, 19.2, ...],
    "pm2_5": [8.2, 8.5, ...],
    "nitrogen_dioxide": [22.1, 23.5, ...],
    "ozone": [45.0, 42.3, ...],
    "sulphur_dioxide": [2.1, 2.0, ...],
    "carbon_monoxide": [0.3, 0.3, ...],
    "us_aqi": [38, 42, ...],
    "european_aqi": [28, 31, ...]
  },
  "hourly_units": {
    "pm10": "μg/m³",
    "pm2_5": "μg/m³",
    "nitrogen_dioxide": "μg/m³",
    "ozone": "μg/m³",
    "sulphur_dioxide": "μg/m³",
    "carbon_monoxide": "mg/m³",
    "us_aqi": "index",
    "european_aqi": "index"
  }
}
```

### CAMS Model Details
| Domain | Resolution | Coverage | Notes |
|--------|------------|----------|-------|
| `cams_europe` | ~11 km | Europe + margins | Higher accuracy, more species |
| `cams_global` | ~45 km | Worldwide | Coarser, fewer validation stations |

**Critical**: "Global and European sets have different characteristics and must not be considered equal in accuracy." (Spec)

### Pollutant Units & Thresholds (For Normalization)

| Pollutant | Unit | WHO 2021 24h Guideline | EU Limit | US AQI Moderate (50) |
|-----------|------|------------------------|----------|---------------------|
| PM2.5 | μg/m³ | 15 | 25 | 12.0 |
| PM10 | μg/m³ | 45 | 50 | 54 |
| NO₂ | μg/m³ | 25 | 200 (1h) | ~100 |
| O₃ | μg/m³ | 100 (8h) | 120 (8h) | ~137 |
| SO₂ | μg/m³ | 40 | 125 | ~91 |
| CO | mg/m³ | 4 | 10 (8h) | ~5 |

### AQI Standards — **Do Not Mix**

| Standard | Range | Categories | Source |
|----------|-------|------------|--------|
| **US AQI** | 0–500+ | Good (0–50), Moderate (51–100), Unhealthy for Sensitive (101–150), Unhealthy (151–200), Very Unhealthy (201–300), Hazardous (301+) | EPA |
| **European AQI** | 0–100+ | Good (0–20), Fair (21–40), Moderate (41–60), Poor (61–80), Very Poor (81–100), Extremely Poor (100+) | EEA |

**App Setting**: User selects `AQI Standard: US / EU` → determines which index displayed and used for `P` factor if AQI-based normalization chosen.

---

## 3. Overpass API (OpenStreetMap)

### Endpoint
```
https://overpass-api.de/api/interpreter
```
(Mirrors: `overpass.kumi.systems`, `overpass.openstreetmap.ru`)

### Query Pattern (Per Spec)
```
[out:json][timeout:25];
(
  way(around:500,<lat>,<lon>)["highway"];
  way(around:500,<lat>,<lon>)["landuse"];
  way(around:500,<lat>,<lon>)["leisure"];
  way(around:500,<lat>,<lon>)["natural"];
  way(around:500,<lat>,<lon>)["industrial"];
  way(around:500,<lat>,<lon>)["amenity"];
  way(around:500,<lat>,<lon>)["building"];
  way(around:500,<lat>,<lon>)["railway"];
  way(around:500,<lat>,<lon>)["public_transport"];
);
out tags center;
```

### Radius Strategy
| Context | Radius | Use Case |
|---------|--------|----------|
| **Segment midpoint** | 200 m | Road type, immediate surroundings |
| **Stay center** | 500 m | Place context, green/industrial areas |
| **Route corridor** | 100 m buffer | Continuous road type along path |

### Key Tags for Classification

#### Road Types (`highway=`)
| Tag | Category | Exposure Implication |
|-----|----------|---------------------|
| `motorway`, `trunk`, `primary` | Major Arterial | ×1.3–1.5 |
| `secondary`, `tertiary` | Collector | ×1.1–1.2 |
| `residential`, `service`, `living_street` | Local | ×1.0 (baseline) |
| `cycleway`, `footway`, `path` | Non-motorized | ×0.7–0.8 |

#### Green Areas (`leisure=`, `landuse=`, `natural=`)
| Tag | Category | Exposure Implication |
|-----|----------|---------------------|
| `park`, `forest`, `grass`, `meadow`, `recreation_ground`, `garden` | Green | ×0.6–0.8 |
| `nature_reserve`, `protected_area` | Protected Green | ×0.5–0.7 |

#### Pollution Sources
| Tag | Category | Exposure Implication |
|-----|----------|---------------------|
| `landuse=industrial` | Industrial | ×1.3–1.8 |
| `railway=rail` + `usage=main/freight` | Railway | ×1.1–1.3 |
| `man_made=chimney`, `power=plant` | Point Source | ×1.5–2.0 (proximity-based) |
| `highway=bus_stop` + `public_transport=station` | Transit Hub | ×1.1 (idling traffic) |

### Response Format
```json
{
  "elements": [
    {
      "type": "way",
      "id": 123456,
      "tags": {
        "highway": "primary",
        "name": "Main Street",
        "lanes": "2",
        "maxspeed": "50"
      },
      "center": { "lat": 60.1699, "lon": 24.9384 },
      "geometry": [ { "lat": ..., "lon": ... }, ... ]
    }
  ]
}
```

### Requirements (Per Spec)
- ✅ **Caching**: 24h for static OSM features
- ✅ **Rate Limit**: ≤ 1 req/sec to public instance; use mirror or self-hosted for production
- ✅ **Timeout**: 25s max; fallback to cached on timeout
- ✅ **Deduplication**: Don't re-query same lat/lon/radius within cache TTL
- ✅ **Error Handling**: On failure → skip geographic context, reduce Confidence, continue

---

## 4. Routing APIs

### OSRM (Primary Candidate)

**Endpoint** (Public Demo / Self-hosted):
```
https://router.project-osrm.org/route/v1/{profile}/{coordinates}
```

**Profiles**: `driving`, `walking`, `cycling` (car, foot, bike)

**Parameters**:
| Param | Value |
|-------|-------|
| `alternatives` | `true` |
| `geometries` | `polyline` or `geojson` |
| `overview` | `full` |
| `steps` | `true` |
| `annotations` | `duration,distance,speed` |

**Request Example**:
```
https://router.project-osrm.org/route/v1/driving/24.9384,60.1699;24.9450,60.1720?alternatives=true&geometries=geojson&overview=full
```

**Response**:
```json
{
  "routes": [
    {
      "geometry": { "type": "LineString", "coordinates": [[...]] },
      "distance": 4200.5,
      "duration": 3120.0,
      "legs": [{ "steps": [...], "summary": "", "distance": ..., "duration": ... }],
      "weight": 3120.0,
      "weight_name": "duration"
    }
  ],
  "waypoints": [{ "location": [...], "name": "", "hint": "" }]
}
```

**Limitations** (Per Spec):
- "OSRM supports alternative routes but does not guarantee alternatives for every request"
- Public demo has rate limits; production needs self-hosted

### Valhalla (Alternative)

**Endpoint**:
```
https://valhalla1.openstreetmap.de/route
```

**Request** (POST JSON):
```json
{
  "locations": [{ "lat": 60.1699, "lon": 24.9384 }, { "lat": 60.1720, "lon": 24.9450 }],
  "costing": "auto",
  "directions_options": { "units": "kilometers" },
  "alternates": 2
}
```

**Costing Models**: `auto`, `pedestrian`, `bicycle`, `transit` (if GTFS available)

**Advantages**: Richer costing, turn-by-turn, elevation, multimodal

### Route Enrichment Pipeline

```
Raw Route Geometry (LineString)
        │
        ▼
Sample points every 100-200m along route
        │
        ▼
For each sample point:
  ├─▶ EnvironmentalRepository.getEnvironmentalData(lat, lon, departureTime + offset)
  └─▶ GeographicContextEngine.analyzePoint(lat, lon, radius=100m)
        │
        ▼
Aggregate per-route:
  ├─▶ Average/Max Pollution Factor
  ├─▶ Road Type Distribution
  ├─▶ Green Area Proximity
  └─▶ Industrial Proximity
        │
        ▼
ExposureCalculationEngine.calculateRouteExposure(enrichedRoute)
        │
        ▼
RouteAlternative { geometry, distance, duration, exposureScore, confidence, coverage }
```

---

## 5. Map Tiles & Display

### OpenFreeMap (Tile Provider)
- **Style**: `openfreemap` (OSM-based, free, no key required)
- **Tile URL**: `https://tiles.openfreemap.org/styles/liberty/{z}/{x}/{y}.png`
- **Attribution**: © OpenStreetMap contributors, © OpenFreeMap

### MapLibre Native (Renderer)
- **Integration**: `org.maplibre.maplibre-android:maplibre:11+`
- **Style**: JSON style referencing OpenFreeMap tiles
- **Offline**: Support tile caching for tracked areas

### Map Overlays (Planned)
| Layer | Source | Update Frequency |
|-------|--------|------------------|
| User Location | GPS | Real-time |
| Current Route | Segmentation Engine | Live |
| Timeline Points | Route/Stay entities | On screen |
| Green Areas | Overpass (cached) | Daily refresh |
| Major Roads | Overpass (cached) | Daily refresh |
| Exposure Heatmap | Exposure Engine | Per segment |
| Private Zones | User settings | Real-time |

---

## Data Quality Validator (Per Spec)

Every environmental sample validated before use:

```kotlin
data class ValidationResult(
    val isValid: Boolean,
    val quality: DataQuality,
    val issues: List<ValidationIssue>
)

enum class ValidationIssue {
    DATA_TOO_OLD,           // > 3 hours
    COORDINATE_TOO_FAR,     // > 30 km
    TIMESTAMP_IN_FUTURE,    // > 15 min ahead
    MISSING_REQUIRED_FIELDS,
    AQI_STANDARD_MISMATCH,  // US vs EU confusion
    MODEL_RESOLUTION_WARNING // > 20 km for local decision
}

fun validate(sample: EnvironmentalSample, requestLat: Double, requestLon: Double, requestTime: Long): ValidationResult {
    val issues = mutableListOf<ValidationIssue>()
    
    // Age check
    val ageHours = (System.currentTimeMillis() - sample.fetchedAt) / 3_600_000
    if (ageHours > 3) issues.add(DATA_TOO_OLD)
    
    // Distance check
    val distanceKm = haversine(requestLat, requestLon, sample.latitude, sample.longitude)
    if (distanceKm > 30) issues.add(COORDINATE_TOO_FAR)
    
    // Future timestamp
    val timeDiffMin = (sample.timestamp - requestTime) / 60_000
    if (timeDiffMin > 15) issues.add(TIMESTAMP_IN_FUTURE)
    
    // Required fields
    if (sample.pm2_5 == null && sample.usAqi == null && sample.europeanAqi == null) {
        issues.add(MISSING_REQUIRED_FIELDS)
    }
    
    val quality = when {
        issues.contains(MISSING_REQUIRED_FIELDS) -> DataQuality.OUT_OF_RANGE
        issues.any { it == DATA_TOO_OLD || it == COORDINATE_TOO_FAR } -> DataQuality.STALE
        issues.isEmpty() -> DataQuality.HIGH
        else -> DataQuality.MEDIUM
    }
    
    return ValidationResult(issues.none { it == MISSING_REQUIRED_FIELDS }, quality, issues)
}
```

**On Validation Failure**:
- ❌ Do NOT use as authoritative data
- ⚠️ Reduce Confidence
- 📋 Show "Data Unavailable" / "Incomplete" in UI
- ❌ Never substitute 0 (implies clean air)

---

## Offline / Degraded Mode

| Scenario | Behavior |
|----------|----------|
| No network | Use cached env data (if < 3h old); mark Coverage ↓ |
| API timeout | Retry 2× with backoff; then use cache; mark Confidence ↓ |
| API error (5xx) | Exponential backoff; cache fallback |
| Overpass down | Skip geographic context; R = 1.0; Confidence ↓ |
| Routing API down | Show cached routes / error state; allow manual retry |
| All env data missing | Exposure Score = "Incomplete"; show warning; Timeline still works |

---

## Attribution Requirements

| Service | Attribution |
|---------|-------------|
| Open-Meteo | "Weather data by Open-Meteo.com" + link |
| CAMS | "Air quality data by Copernicus Atmosphere Monitoring Service" |
| OpenStreetMap | "© OpenStreetMap contributors" |
| Overpass API | "Data from Overpass API" |
| OpenFreeMap | "Tiles by OpenFreeMap" |
| OSRM | "Routing by OSRM" (if used) |
| Valhalla | "Routing by Valhalla" (if used) |
| MapLibre | "Map by MapLibre" |

Display in Settings → About → Data Sources.

---

## Planned vs Implemented

> **All integrations are 📋 Planned** — no HTTP clients, parsers, or cache layer exists.
> This document defines target contracts per `ROAD GUIDE.txt` (Parts 8, 9, 12, 25).
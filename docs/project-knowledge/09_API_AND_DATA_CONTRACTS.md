# API & Data Contracts — Road Guide

## Overview

Data Transfer Objects (DTOs), API request/response contracts, and data transformation rules between layers.

**Status**: 📋 Planned — Contracts defined per spec, no implementation exists.

---

## Domain Models (Kotlin Data Classes)

### Core Domain Entities (Used in Domain Layer)

```kotlin
// Location
data class LocationPoint(
    val id: Long,
    val coordinate: LatLng,
    val timestamp: Instant,
    val accuracyMeters: Float,
    val speedMps: Float?,
    val bearing: Float?,
    val altitude: Double?,
    val provider: String,
    val isMock: Boolean,
    val qualityFlags: Int,
    val routeId: Long?
)

data class LatLng(val latitude: Double, val longitude: Double) {
    fun distanceTo(other: LatLng): Double = haversine(this, other)
}

// Routes & Segments
data class Route(
    val id: Long,
    val startTime: Instant,
    val endTime: Instant?,
    val distanceMeters: Double,
    val status: RouteStatus,
    val routeType: RouteType,
    val confidence: Double,
    val date: LocalDate,
    val segments: List<RouteSegment> = emptyList(),
    val stays: List<Stay> = emptyList()
)

enum class RouteStatus { IN_PROGRESS, COMPLETED, PAUSED, CORRUPTED }
enum class RouteType { DAILY_AUTO, MANUAL_TRACKING, PLANNED_ROUTE }

data class RouteSegment(
    val id: Long,
    val routeId: Long,
    val startTime: Instant,
    val endTime: Instant,
    val activityType: ActivityType,
    val transportType: TransportType?,
    val distanceMeters: Double,
    val startCoordinate: LatLng,
    val endCoordinate: LatLng,
    val geometry: List<LatLng>,
    val confidence: Double,
    val exposureResult: ExposureResult?,
    val isUserCorrected: Boolean,
    val correctedActivityType: ActivityType?,
    val correctedTransportType: TransportType?
)

enum class ActivityType { UNKNOWN, STATIONARY, WALKING, RUNNING, CYCLING, IN_VEHICLE, PUBLIC_TRANSPORT }
enum class TransportType { NONE, WALKING, CYCLING, CAR, MOTORCYCLE, BUS, TRAM, TRAIN, SUBWAY, FERRY, OTHER }

data class Stay(
    val id: Long,
    val routeId: Long,
    val startTime: Instant,
    val endTime: Instant,
    val center: LatLng,
    val radiusMeters: Double,
    val place: Place?,
    val confidence: Double,
    val label: String?,
    val privacyStatus: PrivacyStatus,
    val isIndoor: Boolean,
    val indoorConfidence: Double,
    val exposureResult: ExposureResult?
)

enum class PrivacyStatus { PUBLIC, PRIVATE, EXCLUDED }

// Places
data class Place(
    val id: Long,
    val name: String,
    val category: PlaceCategory,
    val coordinate: LatLng,
    val radiusMeters: Double,
    val address: String?,
    val isPrivate: Boolean,
    val userLabel: String?,
    val visitCount: Int,
    val totalDurationSeconds: Long,
    val firstVisitTime: Instant,
    val lastVisitTime: Instant,
    val environmentalStats: EnvironmentalStats?
)

enum class PlaceCategory { HOME, WORK, SCHOOL, GYM, PARK, SHOP, RESTAURANT, TRANSIT, OTHER }

data class EnvironmentalStats(
    val avgAqi: Double?,
    val avgPm25: Double?,
    val exposureScoreAvg: Double?
)

// Environmental Data
data class EnvironmentalSample(
    val id: Long,
    val coordinate: LatLng,
    val timestamp: Instant,
    val pollutants: PollutantReadings,
    val weather: WeatherReadings,
    val source: DataSource,
    val dataQuality: DataQuality,
    val modelResolutionKm: Double?,
    val fetchedAt: Instant
)

data class PollutantReadings(
    val pm2_5: Double?,       // μg/m³
    val pm10: Double?,        // μg/m³
    val no2: Double?,         // μg/m³
    val o3: Double?,          // μg/m³
    val so2: Double?,         // μg/m³
    val co: Double?,          // mg/m³
    val usAqi: Int?,          // 0-500+
    val europeanAqi: Int?     // 0-100+
)

data class WeatherReadings(
    val temperatureC: Double?,
    val apparentTemperatureC: Double?,
    val humidityPercent: Double?,
    val windSpeedMps: Double?,
    val windDirectionDeg: Double?,
    val precipitationMm: Double?,
    val rainMm: Double?,
    val cloudCoverPercent: Double?,
    val uvIndex: Double?
)

enum class DataSource { OPEN_METEO, OPEN_METEO_AQ, OVERPASS, MANUAL }
enum class DataQuality { HIGH, MEDIUM, LOW, UNKNOWN, STALE, OUT_OF_RANGE }

// Exposure
data class ExposureResult(
    val id: Long,
    val routeId: Long,
    val segmentId: Long?,
    val stayId: Long?,
    val rawScore: Double,
    val normalizedScore: Double,      // 0-100
    val confidence: Double,           // 0.0-1.0
    val coverage: Double,             // 0.0-1.0
    val factors: ExposureFactors,
    val calculationVersion: Int,
    val createdAt: Instant
)

data class ExposureFactors(
    val pollutionFactor: Double,
    val timeFactor: Double,
    val activityFactor: Double,
    val roadFactor: Double,
    val weatherFactor: Double,
    val skinFactor: Double
)

// Daily Report
data class DailyReport(
    val date: LocalDate,
    val totalScore: Double,           // 0-100
    val confidence: Double,
    val coverage: Double,
    val totalDistanceMeters: Double,
    val totalDurationSeconds: Long,
    val walkingDurationSeconds: Long,
    val transportDurationSeconds: Long,
    val stationaryDurationSeconds: Long,
    val visitedPlacesCount: Int,
    val topContributors: List<TopContributor>,
    val bestPeriod: TimePeriod?,
    val worstPeriod: TimePeriod?,
    val recommendations: List<Recommendation>,
    val reportVersion: Int,
    val generatedAt: Instant,
    val isRegenerated: Boolean
)

data class TopContributor(
    val startTime: Instant,
    val endTime: Instant,
    val label: String,
    val level: ContributionLevel,
    val segmentId: Long?,
    val stayId: Long?
)

enum class ContributionLevel { HIGH, MEDIUM, LOW }

data class TimePeriod(
    val startTime: Instant,
    val endTime: Instant,
    val label: String,
    val score: Double
)

data class Recommendation(
    val type: RecommendationType,
    val title: String,
    val description: String,
    val relatedSegmentId: Long?
)

enum class RecommendationType { ROUTE_ALTERNATIVE, TIMING_SHIFT, MODE_SHIFT, INDOOR_ACTIVITY, GENERAL }

// Geographic Context
data class GeographicContext(
    val roadType: RoadType?,
    val nearestRoadDistanceM: Double?,
    val greenAreaProximity: ProximityLevel,
    val industrialProximity: ProximityLevel,
    val transitHubProximity: ProximityLevel,
    val osmFeatures: List<OsmFeature>
)

enum class RoadType { MOTORWAY, TRUNK, PRIMARY, SECONDARY, TERTIARY, RESIDENTIAL, SERVICE, LIVING_STREET, CYCLEWAY, FOOTWAY, PATH, UNKNOWN }

enum class ProximityLevel { NONE, NEAR, ADJACENT, INSIDE }

data class OsmFeature(
    val type: OsmFeatureType,
    val tags: Map<String, String>,
    val coordinate: LatLng,
    val distanceMeters: Double
)

enum class OsmFeatureType { ROAD, GREEN_AREA, INDUSTRIAL, RAILWAY, TRANSIT_HUB, BUILDING, WATER, OTHER }

// Routing
data class RouteAlternative(
    val id: String,
    val geometry: List<LatLng>,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val exposureScore: Double,        // 0-100
    val confidence: Double,
    val coverage: Double,
    val label: RouteLabel,
    val segments: List<RouteExposureSegment>
)

enum class RouteLabel { FASTEST, BALANCED, LOWEST_EXPOSURE, ALTERNATIVE }

data class RouteExposureSegment(
    val startIndex: Int,              // Point index in geometry
    val endIndex: Int,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val exposureScore: Double,
    val factors: ExposureFactors
)

// Settings
data class TrackingSettings(
    val automaticTrackingEnabled: Boolean,
    val trackingSchedule: TimeRange?,
    val locationAccuracy: LocationAccuracy,
    val batteryOptimization: Boolean,
    val activityDetectionEnabled: Boolean,
    val backgroundMonitoringEnabled: Boolean,
    val isPaused: Boolean
)

enum class LocationAccuracy { HIGH, BALANCED, LOW }

data class TimeRange(val startMinutesFromMidnight: Int, val endMinutesFromMidnight: Int)

data class PrivacyZone(
    val id: Long,
    val name: String,
    val center: LatLng,
    val radiusMeters: Double,
    val isActive: Boolean,
    val excludeFromHistory: Boolean,
    val excludeFromExposure: Boolean
)

data class DataRetentionPolicy(
    val locationPointsDays: Int,
    val routesDays: Int,
    val environmentalSamplesHours: Int,
    val exposureResultsDays: Int,
    val dailyReportsDays: Int,        // -1 = forever
    val autoDeleteEnabled: Boolean
)
```

---

## API Contracts (External Services)

### Open-Meteo Weather API

**Request**:
```
GET https://api.open-meteo.com/v1/forecast
Query Parameters:
  latitude: Double
  longitude: Double
  hourly: "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,rain,showers,wind_speed_10m,wind_direction_10m,cloud_cover,uv_index"
  timezone: "auto"
  timeformat: "unixtime"
  wind_speed_unit: "ms"
  forecast_hours: 48
  past_hours: 24
```

**Response DTO**:
```kotlin
data class OpenMeteoWeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val generationtime_ms: Double,
    val utc_offset_seconds: Int,
    val timezone: String,
    val timezone_abbreviation: String,
    val elevation: Double,
    val hourly: HourlyWeatherData,
    val hourly_units: HourlyWeatherUnits
)

data class HourlyWeatherData(
    val time: List<Long>,                    // Unix seconds
    val temperature_2m: List<Double?>,
    val relative_humidity_2m: List<Int?>,
    val apparent_temperature: List<Double?>,
    val precipitation: List<Double?>,
    val rain: List<Double?>,
    val showers: List<Double?>,
    val wind_speed_10m: List<Double?>,
    val wind_direction_10m: List<Int?>,
    val cloud_cover: List<Int?>,
    val uv_index: List<Double?>
)

data class HourlyWeatherUnits(
    val temperature_2m: String,      // "°C"
    val relative_humidity_2m: String, // "%"
    val apparent_temperature: String, // "°C"
    val precipitation: String,       // "mm"
    val rain: String,                // "mm"
    val showers: String,             // "mm"
    val wind_speed_10m: String,      // "m/s"
    val wind_direction_10m: String,  // "°"
    val cloud_cover: String,         // "%"
    val uv_index: String             // ""
)
```

### Open-Meteo Air Quality API

**Request**:
```
GET https://air-quality-api.open-meteo.com/v1/air-quality
Query Parameters:
  latitude: Double
  longitude: Double
  hourly: "pm10,pm2_5,nitrogen_dioxide,ozone,sulphur_dioxide,carbon_monoxide,us_aqi,european_aqi"
  timezone: "auto"
  timeformat: "unixtime"
  forecast_hours: 48
  past_hours: 24
  domains: "cams_europe" | "cams_global" (optional, auto by coords)
```

**Response DTO**:
```kotlin
data class OpenMeteoAQResponse(
    val latitude: Double,
    val longitude: Double,
    val generationtime_ms: Double,
    val utc_offset_seconds: Int,
    val timezone: String,
    val timezone_abbreviation: String,
    val elevation: Double,
    val hourly: HourlyAQData,
    val hourly_units: HourlyAQUnits
)

data class HourlyAQData(
    val time: List<Long>,
    val pm10: List<Double?>,
    val pm2_5: List<Double?>,
    val nitrogen_dioxide: List<Double?>,
    val ozone: List<Double?>,
    val sulphur_dioxide: List<Double?>,
    val carbon_monoxide: List<Double?>,
    val us_aqi: List<Int?>,
    val european_aqi: List<Int?>
)

data class HourlyAQUnits(
    val pm10: String,              // "μg/m³"
    val pm2_5: String,             // "μg/m³"
    val nitrogen_dioxide: String,  // "μg/m³"
    val ozone: String,             // "μg/m³"
    val sulphur_dioxide: String,   // "μg/m³"
    val carbon_monoxide: String,   // "mg/m³"
    val us_aqi: String,            // ""
    val european_aqi: String       // ""
)
```

### Overpass API

**Request** (POST body):
```
[out:json][timeout:25];
(
  way(around:{radius},{lat},{lon})["highway"];
  way(around:{radius},{lat},{lon})["landuse"];
  way(around:{radius},{lat},{lon})["leisure"];
  way(around:{radius},{lat},{lon})["natural"];
  way(around:{radius},{lat},{lon})["industrial"];
  way(around:{radius},{lat},{lon})["amenity"];
  way(around:{radius},{lat},{lon})["building"];
  way(around:{radius},{lat},{lon})["railway"];
  way(around:{radius},{lat},{lon})["public_transport"];
);
out tags center geom;
```

**Response DTO**:
```kotlin
data class OverpassResponse(
    val version: Double,
    val generator: String,
    val osm3s: Osm3sInfo,
    val elements: List<OsmElement>
)

data class Osm3sInfo(
    val timestamp_osm_base: String,
    val timestamp_areas_base: String,
    val copyright: String
)

data class OsmElement(
    val type: String,           // "way", "node", "relation"
    val id: Long,
    val tags: Map<String, String>?,
    val center: LatLng?,        // For ways: centroid
    val geometry: List<LatLng>? // For ways: full geometry
)
```

### OSRM Routing API

**Request**:
```
GET https://router.project-osrm.org/route/v1/{profile}/{coordinates}
Query Parameters:
  alternatives: "true"
  geometries: "geojson"
  overview: "full"
  steps: "true"
  annotations: "duration,distance,speed"
```

**Coordinates Format**: `lon1,lat1;lon2,lat2;...` (longitude first!)

**Response DTO**:
```kotlin
data class OsrmResponse(
    val code: String,           // "Ok"
    val routes: List<OsrmRoute>,
    val waypoints: List<OsrmWaypoint>
)

data class OsrmRoute(
    val geometry: GeoJsonLineString,
    val legs: List<OsrmLeg>,
    val distance: Double,       // meters
    val duration: Double,       // seconds
    val weight: Double,
    val weight_name: String
)

data class OsrmLeg(
    val steps: List<OsrmStep>,
    val summary: String,
    val distance: Double,
    val duration: Double
)

data class OsrmStep(
    val distance: Double,
    val duration: Double,
    val geometry: GeoJsonLineString,
    val name: String,
    val mode: String,
    val maneuver: OsrmManeuver
)

data class OsrmManeuver(
    val type: String,           // "turn", "depart", "arrive", etc.
    val instruction: String,
    val bearing_before: Double,
    val bearing_after: Double,
    val location: List<Double>  // [lon, lat]
)

data class OsrmWaypoint(
    val location: List<Double>, // [lon, lat]
    val name: String,
    val hint: String
)

data class GeoJsonLineString(
    val type: String = "LineString",
    val coordinates: List<List<Double>>  // [[lon, lat], ...]
)
```

### Valhalla Routing API

**Request** (POST JSON):
```json
{
  "locations": [{"lat": 60.1699, "lon": 24.9384}, {"lat": 60.1720, "lon": 24.9450}],
  "costing": "pedestrian",
  "directions_options": {"units": "kilometers"},
  "alternates": 2,
  "shape_match": "map_snap"
}
```

**Response DTO** (simplified):
```kotlin
data class ValhallaResponse(
    val trip: ValhallaTrip,
    val alternatives: List<ValhallaTrip>?
)

data class ValhallaTrip(
    val summary: ValhallaSummary,
    val legs: List<ValhallaLeg>,
    val shape: String  // Encoded polyline
)

data class ValhallaSummary(
    val length: Double,      // km
    val time: Double,        // seconds
    val cost: Double
)

data class ValhallaLeg(
    val summary: ValhallaSummary,
    val shape: String,
    val maneuvers: List<ValhallaManeuver>
)

data class ValhallaManeuver(
    val type: Int,
    val instruction: String,
    val length: Double,
    val time: Double,
    val begin_shape_index: Int,
    val end_shape_index: Int
)
```

---

## Internal Data Transformations

### Layer Boundaries

```
┌─────────────────────────────────────────────────────────────┐
│                      UI Layer                               │
│  ViewModels expose: UiState<T> (data classes with formatting)│
├─────────────────────────────────────────────────────────────┤
│                      Domain Layer                           │
│  UseCases consume/produce: Domain Models (above)            │
├─────────────────────────────────────────────────────────────┤
│                       Data Layer                            │
│  Repositories: Entity <-> Domain Model mappers              │
│  Room Entities: @Entity classes (03_DATA_MODEL_AND_DATABASE)│
│  Remote DTOs: API Response classes (above)                  │
└─────────────────────────────────────────────────────────────┘
```

### Mapper Pattern

```kotlin
// Example: EnvironmentalRepository
class EnvironmentalRepositoryImpl(
    private val api: OpenMeteoApi,
    private val dao: EnvironmentalSampleDao,
    private val cache: EnvironmentalCache
) : EnvironmentalRepository {
    
    override suspend fun getEnvironmentalData(lat: Double, lon: Double, time: Instant): EnvironmentalSample {
        // 1. Check cache (memory + Room)
        // 2. If miss/stale → fetch from API
        // 3. Transform DTO → Domain Model
        // 4. Validate → assign DataQuality
        // 5. Store in cache + Room
        // 6. Return Domain Model
    }
}

// Mapper: OpenMeteoAQResponse + OpenMeteoWeatherResponse → EnvironmentalSample
fun mapToEnvironmentalSample(
    aqResponse: OpenMeteoAQResponse,
    weatherResponse: OpenMeteoWeatherResponse,
    requestLat: Double,
    requestLon: Double,
    requestTime: Instant
): EnvironmentalSample {
    // Find matching hourly index for requestTime
    // Extract readings
    // Validate
    // Return domain model
}
```

---

## Time & Unit Conventions

| Aspect | Convention | Implementation |
|--------|------------|----------------|
| **Timestamps** | UTC `Instant` (ISO-8601) everywhere | `Instant.now()`, `Instant.ofEpochMilli(ms)` |
| **Local Dates** | `LocalDate` in user's timezone | `Instant.atZone(ZoneId.systemDefault()).toLocalDate()` |
| **Durations** | `Duration` or seconds `Long` | `Duration.ofSeconds(secs)`, `secs.toLong()` |
| **Coordinates** | WGS84 decimal degrees (lat, lon) | `LatLng(lat, lon)` |
| **Distances** | Meters (Double) | `distanceMeters` |
| **Speeds** | m/s (Double/Float) | `speedMps` |
| **Areas** | Square meters | — |
| **Pollutants** | μg/m³ (PM, NO₂, O₃, SO₂), mg/m³ (CO) | Per API units |
| **AQI** | Dimensionless index (US 0-500, EU 0-100+) | `usAqi`, `europeanAqi` |
| **Temperature** | Celsius (Double) | Store °C; convert to °F for display if needed |
| **Wind** | m/s, degrees (0-360, from north) | `windSpeedMps`, `windDirectionDeg` |
| **Precipitation** | mm (Double) | `precipitationMm` |

---

## Serialization (JSON)

**Library**: Kotlinx Serialization (`@Serializable`)

```kotlin
@Serializable
data class ExportFormat(
    val version: Int = 1,
    val exportedAt: Instant,
    val routes: List<RouteExport>,
    val places: List<PlaceExport>,
    val dailyReports: List<DailyReportExport>
)

@Serializable
data class RouteExport(
    val id: Long,
    val date: LocalDate,
    val startTime: Instant,
    val endTime: Instant?,
    val distanceMeters: Double,
    val segments: List<SegmentExport>,
    val stays: List<StayExport>
)
```

**Date Format**: ISO-8601 with timezone (`yyyy-MM-dd'T'HH:mm:ss.SSSZ`)
**Instant Serialization**: Custom serializer → epoch milliseconds (Long) for compactness

**Export privacy requirements (implementation contract)**:
- Omit route points, segments, and stays excluded by any active `excludeFromHistory` zone.
- Retain points in exposure-only zones only if all exposure/sample fields are omitted for them.
- Recompute exported route distance/duration from visible intervals; do not export stored totals that include hidden intervals.
- The app currently has no JSON serializer/exporter or file-sharing flow. `PrivacyZonePolicy.prepareForExport` only supplies a location-point filtering/eligibility contract and is not yet connected to this schema.
- Zone changes do not physically remove earlier raw coordinates from Room; a future exporter must apply current zone rules before serialization.

---

## Versioning & Migration

| Contract | Versioning Strategy |
|----------|---------------------|
| Room Entities | Schema version + Migration classes |
| API DTOs | `@SerialName` for field renames; optional fields with defaults |
| Export Format | `version` field in root; readers handle multiple versions |
| Exposure Calculation | `calculationVersion` in `ExposureResult` & `DailyReport` |

---

## Planned vs Implemented

> **Contract status is mixed:** weather, air-quality, and routing clients exist; these API examples and the JSON export schema remain target contracts. In particular, JSON serialization and file export are not implemented. See `13_CURRENT_IMPLEMENTATION_STATUS.md` for current component status.
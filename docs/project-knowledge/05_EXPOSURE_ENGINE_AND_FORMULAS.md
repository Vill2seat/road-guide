# Exposure Engine & Formulas — Road Guide

## Overview

The Exposure Engine calculates a **relative Environmental Exposure Score (0–100)** for each segment/stay of the day, then aggregates into a Daily Exposure Score.

**Critical**: This is an **estimation model**, not a measured dose. No medical claims. Units are dimensionless (0–100 scale).

**Status**: 🟡 Partially implemented — segment factors, normalization, aggregation, confidence, and coverage are coded and tested; configurable user preferences and parts of the original target model remain incomplete.

---

## Core Formula: Segment Exposure

### Base Equation

```
E_segment = P × T × A × R × W × S
```

| Factor | Symbol | Description | Range | Source |
|--------|--------|-------------|-------|--------|
| Pollution Factor | P | Normalized pollutant concentrations | 0.0–∞ | Open-Meteo AQ + normalization |
| Time Factor | T | Duration weighting (30-min reference) | 0.0–2.0 | `T_raw` capped at `EXPOSURE_T_MAX` |
| Activity Factor | A | Breathing rate / exertion multiplier | 0.7–1.5+ | ActivityType lookup |
| Road/Geographic Factor | R | Road proximity, green areas, industrial | 0.5–2.0+ | OSM context + `EXPOSURE_ROAD_SOURCE_FACTOR` |
| Weather Factor | W | Wind, rain, temperature modulation | 0.5–1.5 | Open-Meteo Weather |
| Skin/Exposure Factor | S | Skin coverage adjustment | 1.0 (default) | User input (future) |

**Calibration Constant**: `EXPOSURE_CALIBRATION_K = 5.0`

**Final Segment Score** (pre-normalization):
```
E_segment_raw = EXPOSURE_CALIBRATION_K × (P × T × A × R × W × S)
```

---

## Factor Definitions & Constants

### 1. Time Factor (T)

**Formula**:
```
T_raw = durationSeconds / 1800.0          // Reference: 30 minutes = 1800 seconds
T = min(T_raw, EXPOSURE_T_MAX)
```

**Constants**:
| Constant | Value | Description |
|----------|-------|-------------|
| Reference Duration | 1800 sec (30 min) | Baseline exposure unit |
| `EXPOSURE_T_MAX` | 2.0 | Cap: 60+ minutes = 2× reference |

**Examples**:
| Duration | T_raw | T (capped) |
|----------|-------|------------|
| 15 min (900s) | 0.5 | 0.5 |
| 30 min (1800s) | 1.0 | 1.0 |
| 45 min (2700s) | 1.5 | 1.5 |
| 60 min (3600s) | 2.0 | 2.0 |
| 120 min (7200s) | 4.0 | 2.0 |

> **Implemented in**: `ExposureCalculationEngine.calculateTimeFactor(durationSeconds)`

---

### 2. Activity Factor (A)

**Per Spec (Existing Configuration)**:

| ActivityType | Factor | Notes |
|--------------|--------|-------|
| `STATIONARY` | **0.7** | Reduced breathing rate indoors/outdoors |
| `WALKING` | *Use current model config* | ~1.0 baseline |
| `CYCLING` | *Use current model config* | Elevated ventilation |
| `RUNNING` | **Up to 1.5** | High exertion, mouth breathing |
| `IN_VEHICLE` | *Use current model config* | Depends on ventilation, cabin filter |
| `PUBLIC_TRANSPORT` | *Use current model config* | Often crowded, doors open |
| `UNKNOWN` | **1.0** | Conservative default |

> ⚠️ **Exact values for WALKING, CYCLING, IN_VEHICLE, PUBLIC_TRANSPORT** are defined in the existing model configuration (to be extracted from current implementation when available). **Do not invent values.**

> **Implemented in**: `ExposureCalculationEngine.getActivityFactor(activityType)`

---

### 3. Road/Geographic Factor (R)

**Base Constant**:
```
EXPOSURE_ROAD_SOURCE_FACTOR = 1.0
```

**Modifiers** (applied multiplicatively to base 1.0):

| Context | Modifier | Condition |
|---------|----------|-----------|
| Major road (motorway, trunk, primary) | × 1.2–1.5 | Within 50m of highway |
| Residential road | × 1.0 | Baseline |
| Living street / pedestrian | × 0.8 | Traffic calmed |
| Cycleway / footway / path | × 0.7 | Non-motorized |
| Green area (park, forest, grass) | × 0.6–0.8 | Inside or adjacent (≤100m) |
| Industrial area | × 1.3–1.8 | `landuse=industrial` within 200m |
| Railway infrastructure | × 1.1–1.3 | Within 100m |

**Calculation**:
```
R = EXPOSURE_ROAD_SOURCE_FACTOR × Π(modifiers for detected contexts)
```

**Confidence Weighting**: If OSM data coverage < 50% for segment → reduce modifier magnitude by coverage ratio.

> **Implemented in**: `GeographicContextEngine.analyzeSegment(segment)` → `ExposureCalculationEngine.calculateRoadFactor(context)`

---

### 4. Weather Factor (W)

**Inputs** (from Open-Meteo Weather, hourly):

| Variable | Effect on Exposure | Formula Concept |
|----------|-------------------|-----------------|
| Wind Speed (m/s) | Disperses pollutants → ↓ exposure | `W_wind = max(0.5, 1.0 - 0.05 × windSpeed)` capped |
| Wind Direction | Matters for source-receptor | Future: correlate with road/industrial bearing |
| Precipitation (mm) | Washes particulates → ↓ PM | `W_rain = max(0.6, 1.0 - 0.1 × precipitation)` |
| Temperature (°C) | Affects chemistry, inversion | `W_temp = 1.0 + 0.01 × (temp - 20)` (simplified) |
| Humidity (%) | Affects PM hygroscopic growth | `W_humidity = 1.0 + 0.002 × (humidity - 60)` |
| Cloud Cover | Affects mixing height | Minor, future refinement |

**Combined** (multiplicative, each capped 0.5–1.5):
```
W = W_wind × W_rain × W_temp × W_humidity
```

> **Default**: `W = 1.0` if weather data unavailable (reduces Confidence, not Score).
> **Implemented in**: `ExposureCalculationEngine.calculateWeatherFactor(weatherSample)`

---

### 5. Skin/Exposure Factor (S)

**Default**: `S = 1.0`

**Future Extension** (if user enables):
```
S = 1.0 + (skinExposurePercent / 100.0) × 0.5
```
- 0% skin exposed (fully covered) → 1.0
- 100% skin exposed → 1.5

> ⚠️ **Not implemented**. Only for future "contact exposure" model if scientifically validated.
> **Current**: Always `S = 1.0`.

---

### 6. Pollution Factor (P) — Most Complex

**Principle**: Normalize each pollutant to dimensionless 0–1+ scale, then combine.

#### Step 1: Per-Pollutant Normalization

For each pollutant, define:
- **Reference Threshold** (e.g., WHO guideline, EU limit, AQI breakpoint)
- **Normalization Function**: `f(conc) = conc / threshold` (linear) or piecewise

| Pollutant | Unit | WHO 2021 Guideline (24h) | EU Limit (24h) | US AQI Breakpoint (Moderate) |
|-----------|------|--------------------------|----------------|------------------------------|
| PM2.5 | μg/m³ | 15 | 25 | 12.0 (AQI 50) |
| PM10 | μg/m³ | 45 | 50 | 54 (AQI 50) |
| NO₂ | μg/m³ | 25 | 200 (1h) | 53 ppb ≈ 100 μg/m³ (AQI 50) |
| O₃ | μg/m³ | 100 (8h) | 120 (8h) | 70 ppb ≈ 137 μg/m³ (AQI 50) |
| SO₂ | μg/m³ | 40 (24h) | 125 (24h) | 35 ppb ≈ 91 μg/m³ (AQI 50) |
| CO | mg/m³ | 4 (24h) | 10 (8h) | 4.4 ppm ≈ 5 mg/m³ (AQI 50) |

**Normalization Approach** (per spec: "own scale, own thresholds, own normalization"):

```
P_pollutant = concentration / reference_threshold
```

**Reference Threshold Selection** (configurable, default = WHO 2021 24h):
- PM2.5: 15 μg/m³
- PM10: 45 μg/m³
- NO₂: 25 μg/m³
- O₃: 100 μg/m³ (8h avg)
- SO₂: 40 μg/m³
- CO: 4 mg/m³

#### Step 2: Handling Missing Pollutants

| Scenario | Handling |
|----------|----------|
| Single pollutant missing | Exclude from combination; reduce Coverage |
| All pollutants missing | P = 1.0 (neutral); Coverage = 0; Confidence ↓ |
| Data failed validation (stale, distant) | Treat as missing; flag in Coverage |

**Never** substitute 0 for missing data.

#### Step 3: Combining Pollutants

**Option A: Maximum** (conservative, worst pollutant drives)
```
P = max(P_PM2.5, P_PM10, P_NO2, P_O3, P_SO2, P_CO)
```

**Option B: Weighted Sum** (additive burden)
```
P = Σ(w_i × P_i) / Σ(w_i)
Weights: PM2.5=1.0, PM10=0.5, NO2=0.8, O3=0.6, SO2=0.4, CO=0.3
```

**Option C: AQI-Based** (if AQI available)
```
P = usAqi / 50.0          // US AQI: 50 = "Moderate" threshold
   or europeanAqi / 50.0  // EU AQI: 50 = "Moderate" threshold
```

> ⚠️ **Spec Requirement**: "If AQI used, specify which: US AQI or European AQI. Do not mix."
> **Default**: Option A (Maximum) with WHO thresholds. Configurable per user preference (Settings → AQI Standard).

> **Implemented in**: `ExposureCalculationEngine.calculatePollutionFactor(envSample)`

---

## Daily Exposure Aggregation

### Conceptual Formula

```
DailyExposureRaw = Σ(WeightedExposureSegment_i)
```

Where:
```
WeightedExposureSegment_i = E_segment_raw_i × (segmentDuration_i / totalDayDuration)
```

**Then Normalize to 0–100**:
```
DailyExposureScore = Normalize(DailyExposureRaw, 0, 100)
```

### Normalization Function

**Requirements** (per spec):
- NOT simple sum capped at 100
- Must be monotonic
- Must handle wide range of daily raw values
- Must be tested independently

**Candidate: Sigmoid / Logistic**
```
DailyExposureScore = 100 / (1 + exp(-k × (DailyExposureRaw - x0)))
```
- `k` = steepness (calibrated)
- `x0` = midpoint (calibrated to map "typical bad day" → ~70, "typical good day" → ~30)

**Alternative: Percentile Mapping**
- Collect distribution of `DailyExposureRaw` from user population (or simulated)
- Map percentiles → 0–100

> **Implemented in**: `DailyExposureAggregator.normalize(rawScore)`

---

### Daily Report Components

| Component | Calculation |
|-----------|-------------|
| **Total Score** | `DailyExposureScore` (0–100) |
| **Coverage** | `Σ(segmentDuration × segmentCoverage) / totalTrackedDuration` |
| **Confidence** | Weighted avg of segment confidences × coverage factor |
| **Total Distance** | Σ segment distances |
| **Walking Time** | Σ duration where activity=WALKING |
| **Transport Time** | Σ duration where transport≠NONE |
| **Stationary Time** | Σ stay durations |
| **Visited Places** | Count distinct Places (non-private) |
| **Top Contributors** | Segments/Stays with highest `normalizedScore × durationWeight` |
| **Best Period** | Lowest scoring continuous period ≥ 30 min |
| **Worst Period** | Highest scoring continuous period ≥ 30 min |
| **Recommendations** | Rule-based from contributors (see below) |

---

## Confidence & Coverage (Independent Metrics)

### Coverage
**Definition**: Fraction of tracked time with *sufficient* environmental data.

```
Coverage = (Σ duration_i where envDataQuality ∈ {HIGH, MEDIUM}) / totalTrackedDuration
```
- `0.0` = no env data for any segment
- `1.0` = all segments have valid env data
- **Not** a proxy for accuracy

### Confidence
**Definition**: Reliability of the exposure estimate.

```
Confidence = (Σ duration_i × segmentConfidence_i) / totalTrackedDuration × CoverageFactor
```

**Segment Confidence** factors:
- GPS quality (accuracy, jump flags) → 0.0–1.0
- Movement classification confidence → 0.0–1.0
- Environmental data quality → 0.0–1.0
- Geographic context coverage → 0.0–1.0
- Weather data availability → 0.0–1.0

**CoverageFactor**: `min(1.0, Coverage × 1.5)` — low coverage drags down confidence.

**Display Labels**:
| Confidence Value | Label |
|------------------|-------|
| ≥ 0.8 | High |
| 0.5–0.8 | Medium |
| < 0.5 | Low |

---

## Indoor / Outdoor Handling

| Period Type | Exposure Calculation |
|-------------|---------------------|
| **Outdoor** (GPS + OSM confirms outside) | Full formula: P×T×A×R×W×S |
| **Indoor** (dwelling, workplace, transit vehicle) | **Limited**: P = indoor proxy (if user provides), else `Insufficient Indoor Data` |
| **Vehicle Cabin** | R reduced (cabin filter), A = IN_VEHICLE, P = ambient at location |

**Spec Rule**: "External data cannot represent measured indoor air quality. Show 'Insufficient Indoor Data' status."

---

## Model Limitations (Per Spec — Must Display)

1. **Resolution**: CAMS model ~11km (EU) / 45km (global) — cannot resolve street-level differences
2. **Not Measured**: No personal sensor; modeled background + geographic proxies
3. **No Indoor**: Indoor exposure unknown unless user provides data
4. **No Personal Physiology**: Breathing rate, lung capacity, health status not modeled
5. **Relative Only**: Score 0–100 compares days/routes, not absolute dose
6. **Calibration**: `EXPOSURE_CALIBRATION_K = 5.0` is arbitrary scaling for UI range

---

## Constants Summary (From Spec — Do Not Change)

| Constant | Value | Location |
|----------|-------|----------|
| `T_raw` denominator | 1800.0 | Time Factor |
| `EXPOSURE_T_MAX` | 2.0 | Time Factor |
| `EXPOSURE_ROAD_SOURCE_FACTOR` | 1.0 | Road Factor |
| `EXPOSURE_CALIBRATION_K` | 5.0 | Final Scaling |
| `Skin Factor (S)` default | 1.0 | Skin Factor |
| Activity Factor: Stationary | 0.7 | Activity Factor |
| Activity Factor: Running | Up to 1.5 | Activity Factor |
| Max Env Data Age | 3 hours | Validator |
| Max Coord Distance | 30 km | Validator |
| Max Future Offset | 15 minutes | Validator |

---

## Implementation Checklist

| Component | Status | File |
|-----------|--------|------|
| `calculateTimeFactor(duration)` | ✅ Implemented | `ExposureCalculationEngine.kt` |
| `getActivityFactor(activityType)` | ✅ Implemented | `ExposureCalculationEngine.kt` |
| `calculateRoadFactor(context)` | ✅ Implemented | `ExposureCalculationEngine.kt` |
| `calculateWeatherFactor(weather)` | ✅ Implemented | `ExposureCalculationEngine.kt` |
| `calculatePollutionFactor(envSample)` | ✅ Implemented | `ExposureCalculationEngine.kt` |
| `calculateSegmentExposure(...)` | ✅ Implemented | `ExposureCalculationEngine.kt` |
| `DailyExposureAggregator.aggregate()` | ✅ Implemented | `DailyExposureAggregator.kt` |
| `normalizeExposure(rawScore)` | ✅ Implemented | `ExposureCalculationEngine.kt` |
| `calculateDailyExposureMetrics()` | ✅ Implemented | `DailyExposureAggregator.kt` |
| Confidence and coverage | 🟡 Partial | Daily aggregation and segment result fields |
| Unit tests for factors | ✅ Implemented | `ExposureCalculationEngineTest.kt` |
| Daily aggregation tests | ✅ Implemented | `DailyExposureAggregatorTest.kt` |
| Instrumentation exposure test | ✅ Implemented | `ExposureCalculationIntegrationTest.kt` |

---

## Example Calculation (Illustrative)

**Scenario**: 30-min walk on residential road near park, moderate air quality.

| Factor | Value | Calculation |
|--------|-------|-------------|
| P (PM2.5=12, threshold=15) | 0.8 | 12/15 = 0.8 (max of pollutants) |
| T (30 min) | 1.0 | 1800/1800 = 1.0 |
| A (WALKING) | 1.0 | Baseline |
| R (residential + park) | 0.85 | 1.0 × 0.85 (park modifier) |
| W (light wind, no rain) | 0.95 | 0.95 × 1.0 × 1.0 × 1.0 |
| S | 1.0 | Default |
| **E_raw** | **3.25** | 5.0 × 0.8 × 1.0 × 1.0 × 0.85 × 0.95 × 1.0 |
| **Normalized (example)** | **~35/100** | Depends on normalization curve |

> Actual normalized score depends on `DailyExposureAggregator.normalize()` calibration.
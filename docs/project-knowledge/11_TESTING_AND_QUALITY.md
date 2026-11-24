# Testing & Quality — Road Guide

## Overview

Test strategy, quality criteria, and validation procedures for Road Guide.

**Status**: 🟡 Partially implemented — Unit and instrumentation suites exist; CI and some end-to-end/device coverage remain incomplete.

---

## Test Pyramid

```
                    ┌─────────────────┐
                    │  E2E / Device   │  ← 10%  (Critical user journeys)
                    │  (Real device)  │
              ┌─────┴─────────────────┴─────┐
              │    Integration Tests        │  ← 20%  (Repository + Engine + API)
              │  (Repository, Engine, API)  │
        ┌─────┴─────────────────────────────┴─────┐
        │           Unit Tests                    │  ← 70%  (Pure logic, mappers, formulas)
        │  (UseCases, Engines, Mappers, Utils)    │
        └─────────────────────────────────────────┘
```

---

## Unit Tests (70%)

### Target: Pure Kotlin Logic (No Android Dependencies)

| Module | Test Focus | Example Tests |
|--------|------------|---------------|
| **ExposureCalculationEngine** | All factor formulas | `calculateTimeFactor(1800) == 1.0`, `calculateTimeFactor(7200) == 2.0`, `getActivityFactor(STATIONARY) == 0.7` |
| **DailyExposureAggregator** | Aggregation + normalization | `normalize(rawScore)` monotonic, known inputs → expected outputs |
| **MovementDetectionEngine** | Classification logic | Speed/acceleration patterns → ActivityType |
| **RouteSegmentationEngine** | Segment/stay boundaries | Point sequences → correct segments/stays |
| **GeographicContextEngine** | OSM tag classification | `highway=primary` → RoadType.PRIMARY, `leisure=park` → GreenArea |
| **EnvironmentalRepository** | Validation logic | `validate(sample)` → correct DataQuality |
| **Mappers** | DTO ↔ Domain | Round-trip serialization, null handling |
| **Utils** | Haversine, time, units | `haversine(lat1,lon1,lat2,lon2)`, `formatDuration(secs)` |

### Test Patterns

```kotlin
// Example: Exposure Engine Factor Tests
class ExposureCalculationEngineTest {
    
    @Test
    fun `time factor: 30 minutes = 1.0`() {
        val engine = ExposureCalculationEngine()
        assertEquals(1.0, engine.calculateTimeFactor(1800), 0.001)
    }
    
    @Test
    fun `time factor: capped at EXPOSURE_T_MAX (2.0)`() {
        val engine = ExposureCalculationEngine()
        assertEquals(2.0, engine.calculateTimeFactor(7200), 0.001)  // 2 hours
        assertEquals(2.0, engine.calculateTimeFactor(18000), 0.001) // 5 hours
    }
    
    @Test
    fun `activity factor: stationary = 0.7`() {
        val engine = ExposureCalculationEngine()
        assertEquals(0.7, engine.getActivityFactor(ActivityType.STATIONARY), 0.001)
    }
    
    @Test
    fun `pollution factor: max of normalized pollutants`() {
        val engine = ExposureCalculationEngine()
        val sample = EnvironmentalSample(
            pollutants = PollutantReadings(pm2_5 = 30.0, pm10 = 40.0, no2 = 20.0, ...),
            // PM2.5: 30/15 = 2.0, PM10: 40/45 = 0.89, NO2: 20/25 = 0.8
        )
        assertEquals(2.0, engine.calculatePollutionFactor(sample), 0.001)
    }
    
    @Test
    fun `daily normalization: monotonic increasing`() {
        val aggregator = DailyExposureAggregator()
        val score1 = aggregator.normalize(10.0)
        val score2 = aggregator.normalize(20.0)
        val score3 = aggregator.normalize(50.0)
        assertTrue(score1 < score2 && score2 < score3)
    }
}
```

---

## Integration Tests (20%)

### Repository Tests (Room + Test DB)

```kotlin
@RunWith(AndroidJUnit4::class)
class LocationRepositoryTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var db: RoadGuideDatabase
    private lateinit var dao: LocationPointDao
    private lateinit var repo: LocationRepositoryImpl
    
    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RoadGuideDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.locationPointDao()
        repo = LocationRepositoryImpl(dao, ...)
    }
    
    @Test
    fun `saveAndObservePoints_flowEmitsCorrectOrder`() = runTest {
        val points = listOf(
            LocationPoint(lat=60.1, lon=24.9, time=1000, ...),
            LocationPoint(lat=60.2, lon=24.9, time=2000, ...)
        )
        repo.savePoints(points)
        
        val observed = repo.observeRecentPoints(10).first()
        assertEquals(2, observed.size)
        assertEquals(1000, observed[0].timestamp)
        assertEquals(2000, observed[1].timestamp)
    }
    
    @Test
    fun `assignPointsToRoute_updatesForeignKey`() = runTest {
        val points = repo.savePoints(testPoints())
        val routeId = 42L
        repo.assignPointsToRoute(points.map { it.id }, routeId)
        
        val updated = dao.getByRouteId(routeId).first()
        assertTrue(updated.all { it.routeId == routeId })
    }
}
```

### Engine Integration Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class ExposureEngineIntegrationTest {
    
    @Test
    fun `full segment exposure: known scenario matches expected`() {
        // Given: 30-min walk on residential road, moderate AQ
        val segment = RouteSegment(
            activityType = WALKING,
            transportType = WALKING,
            duration = 1800,
            distance = 2500,
            ...
        )
        val env = EnvironmentalSample(
            pollutants = PollutantReadings(pm2_5 = 12.0, ...),  // Moderate
            weather = WeatherReadings(windSpeed = 2.0, ...),
            dataQuality = HIGH
        )
        val context = GeographicContext(
            roadType = RESIDENTIAL,
            greenAreaProximity = NEAR
        )
        
        // When
        val result = exposureEngine.calculateSegmentExposure(segment, env, context)
        
        // Then: Verify factor breakdown
        assertEquals(1.0, result.factors.timeFactor, 0.01)
        assertEquals(1.0, result.factors.activityFactor, 0.01)  // Walking baseline
        assertEquals(1.0, result.factors.roadFactor, 0.01)      // Residential baseline
        assertTrue(result.normalizedScore in 20.0..50.0)        // Plausible range
    }
}
```

### API Integration Tests (MockWebServer)

```kotlin
@RunWith(AndroidJUnit4::class)
class OpenMeteoApiTest {
    
    @get:Rule
    val mockServer = MockWebServer()
    
    private lateinit var api: OpenMeteoApi
    
    @Before
    fun setup() {
        val retrofit = Retrofit.Builder()
            .baseUrl(mockServer.url("/"))
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        api = retrofit.create(OpenMeteoApi::class.java)
    }
    
    @Test
    fun `weather response parsed correctly`() = runTest {
        mockServer.enqueue(MockResponse()
            .setBody(jsonFromFile("weather_response.json"))
            .addHeader("Content-Type", "application/json"))
        
        val response = api.getWeather(60.1699, 24.9384)
        
        assertEquals(24, response.hourly.time.size)
        assertEquals(12.3, response.hourly.temperature_2m[0], 0.1)
    }
    
    @Test
    fun `timeout handled gracefully`() = runTest {
        mockServer.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        
        val result = api.getWeather(60.1699, 24.9384)
        // Should throw or return Result.Failure
    }
}
```

---

## E2E / Device Tests (10%)

### Required Scenarios (Per Spec Part 32)

| # | Scenario | Validation |
|---|----------|------------|
| 1 | User moves between multiple places all day | Timeline complete, all segments classified |
| 2 | User stays home all day | Single Stay, correct duration, indoor handling |
| 3 | User stays at work all day | Work Stay recognized, place labeled |
| 4 | Bus ride | PUBLIC_TRANSPORT detected, stops not separate Stays |
| 5 | Car drive | IN_VEHICLE detected, route geometry saved |
| 6 | 3-hour park walk | Green area proximity → lower exposure |
| 7 | Short stops (traffic lights) | Not separate Stays, merged into segment |
| 8 | Walk near major road | Road proximity → higher exposure |
| 9 | GPS temporarily lost | Gap flagged, no fabricated points |
| 10 | Internet off → on | Env data backfilled, Coverage updated |
| 11 | App killed by system | Tracking resumes via Foreground Service |
| 12 | User pauses tracking | No points collected, Timeline shows pause |
| 13 | Private zone enabled | Zone data excluded from Timeline/Exposure |
| 14 | Build alternative routes | 3 options with scores, map renders |
| 15 | User corrects misclassified transport | Correction saved, exposure recalculated |

### Device Test Script (Manual)

```bash
# Run on physical device (Pixel 9 per spec)
# 1. Install debug APK
# 2. Grant all permissions
# 3. Enable Auto Tracking
# 4. Execute Scenario 1 (commute day)
# 5. Wait for Daily Report
# 6. Verify:
#    - Timeline matches actual movements
#    - Exposure Score plausible
#    - Confidence/Coverage shown
#    - No missing periods > 5 min unexplained
# 7. Check battery: Settings → Battery → Road Guide < 5%/hour
# 8. Test Private Zone: add home zone → verify exclusion
# 9. Test Planner: create route → verify 3 alternatives
```

---

## Quality Criteria (Per Spec Part 33)

| Criterion | Definition of Done |
|-----------|-------------------|
| **No point lost without reason** | Every gap in LocationPoint has logged cause (permission, GPS off, interval) |
| **Data survives restart** | Kill app → reopen → Timeline intact |
| **Long stays don't disappear** | 8h Stay at home → visible in Timeline |
| **Stops don't become fake routes** | Traffic light stops → merged, not separate routes |
| **Unknown ≠ invented** | Low confidence → UNKNOWN, not random type |
| **Missing env data ≠ zero pollution** | Score = "Incomplete", warning shown |
| **Exposure Score explainable** | Tap score → see factor breakdown |
| **Confidence & Coverage separate** | Both displayed independently |
| **All screens have error states** | Loading, Empty, Error, Offline, Partial, Permission |
| **Core features work without registration** | No login required for tracking/report |
| **Private zones predictable** | Enter zone → data excluded; exit → resumes |
| **Map + sheet UX smooth** | 60fps scroll, no jank, map stays fixed |

---

## Test Infrastructure

### Dependencies (build.gradle.kts)

```kotlin
dependencies {
    // Unit tests
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("com.google.truth:truth:1.1.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    
    // Android tests
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("org.robolectric:robolectric:4.11")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    
    // Room testing
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    
    // Compose testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.0")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest:1.6.0")
}
```

### CI Pipeline (GitHub Actions)

```yaml
# .github/workflows/test.yml
name: Test
on: [push, pull_request]
jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '17' }
      - name: Run unit tests
        run: ./gradlew test --info
  
  instrumented-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '17' }
      - name: Run instrumented tests (Robolectric)
        run: ./gradlew connectedCheck --info
  
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '17' }
      - name: Run lint
        run: ./gradlew lint
  
  ktlint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run ktlint
        run: ./gradlew ktlintCheck
```

---

## Exposure Engine Validation

### Golden Test Cases

| Test Case | Input | Expected Score Range | Notes |
|-----------|-------|---------------------|-------|
| Clean park walk | 30min, PM2.5=5, green area | 10-25 | Low pollution + green bonus |
| Busy road walk | 30min, PM2.5=35, motorway adjacent | 55-75 | High pollution + road penalty |
| Indoor office | 8h, no env data | "Incomplete" | Insufficient Indoor Data |
| Mixed commute | Walk 15 + Bus 30 + Walk 10 | 30-55 | Segment aggregation |
| Highway drive | 30min, PM2.5=20, in vehicle | 35-50 | Vehicle factor + road |

### Regression Test: Formula Constants

```kotlin
@Test
fun `constants match spec exactly`() {
    assertEquals(1800.0, ExposureConstants.TIME_REFERENCE_SECONDS)
    assertEquals(2.0, ExposureConstants.EXPOSURE_T_MAX)
    assertEquals(1.0, ExposureConstants.EXPOSURE_ROAD_SOURCE_FACTOR)
    assertEquals(5.0, ExposureConstants.EXPOSURE_CALIBRATION_K)
    assertEquals(1.0, ExposureConstants.DEFAULT_SKIN_FACTOR)
    assertEquals(0.7, ExposureConstants.ACTIVITY_FACTOR_STATIONARY)
}
```

---

## Build Verification

| Check | Command | Threshold |
|-------|---------|-----------|
| Compile | `./gradlew assembleDebug` | Success |
| Lint | `./gradlew lint` | No errors |
| Ktlint | `./gradlew ktlintCheck` | Pass |
| Unit Tests | `./gradlew test` | 100% pass |
| Coverage | `./gradlew koverReport` | > 80% domain layer |
| Detekt | `./gradlew detekt` | No warnings (configurable) |

---

## Planned vs Implemented

> Unit and instrumentation test infrastructure exists under `app/src/test` and `app/src/androidTest`. Coverage is strongest around data/DAO behavior, exposure, privacy, movement, and tracking pipeline logic. CI, complete Compose UI journeys, live API validation, and production device/OEM recovery testing remain incomplete.
> This document defines target test strategy per `ROAD GUIDE.txt` (Parts 31, 32, 33).
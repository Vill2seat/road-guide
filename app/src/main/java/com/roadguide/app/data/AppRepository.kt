package com.roadguide.app.data

import com.roadguide.app.data.local.DataProvider
import com.roadguide.app.data.repository.*

class AppRepository(
    private val dataProvider: DataProvider
) {

    val locationPointRepository: LocationPointRepository =
        LocationPointRepositoryImpl(dataProvider.locationPointDao)

    val routeRepository: RouteRepository = RouteRepositoryImpl(dataProvider.routeDao)

    val routeSegmentRepository: RouteSegmentRepository =
        RouteSegmentRepositoryImpl(dataProvider.routeSegmentDao)

    val stayRepository: StayRepository = StayRepositoryImpl(dataProvider.stayDao)

    val placeRepository: PlaceRepository = PlaceRepositoryImpl(dataProvider.placeDao)

    val environmentalSampleRepository: EnvironmentalSampleRepository =
        EnvironmentalSampleRepositoryImpl(dataProvider.environmentalSampleDao)

    val exposureResultRepository: ExposureResultRepository =
        ExposureResultRepositoryImpl(dataProvider.exposureResultDao)

    val dailyReportRepository: DailyReportRepository =
        DailyReportRepositoryImpl(dataProvider.dailyReportDao)

    val privacyZoneRepository: PrivacyZoneRepository =
        PrivacyZoneRepositoryImpl(dataProvider.privacyZoneDao)

    val trackingSettingsRepository: TrackingSettingsRepository =
        TrackingSettingsRepositoryImpl(dataProvider.trackingPrefs)
}

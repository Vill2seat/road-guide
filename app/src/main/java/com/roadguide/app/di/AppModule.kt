package com.roadguide.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import com.roadguide.app.data.local.EnvironmentalValidator
import com.roadguide.app.data.local.dao.DailyReportDao
import com.roadguide.app.data.local.dao.EnvironmentalSampleDao
import com.roadguide.app.data.local.dao.ExposureResultDao
import com.roadguide.app.data.local.dao.LocationPointDao
import com.roadguide.app.data.local.dao.PlaceDao
import com.roadguide.app.data.local.dao.PrivacyZoneDao
import com.roadguide.app.data.local.dao.RouteDao
import com.roadguide.app.data.local.dao.RouteSegmentDao
import com.roadguide.app.data.local.dao.StayDao
import com.roadguide.app.data.local.db.RoadGuideDatabase
import com.roadguide.app.data.local.settings.TrackingPrefs
import com.roadguide.app.data.repository.DailyReportRepository
import com.roadguide.app.data.repository.DailyReportRepositoryImpl
import com.roadguide.app.data.repository.EnvironmentalSampleRepository
import com.roadguide.app.data.repository.EnvironmentalSampleRepositoryImpl
import com.roadguide.app.data.repository.ExposureResultRepository
import com.roadguide.app.data.repository.ExposureResultRepositoryImpl
import com.roadguide.app.data.repository.LocationPointRepository
import com.roadguide.app.data.repository.LocationPointRepositoryImpl
import com.roadguide.app.data.repository.PlaceRepository
import com.roadguide.app.data.repository.PlaceRepositoryImpl
import com.roadguide.app.data.repository.PrivacyZoneRepository
import com.roadguide.app.data.repository.PrivacyZoneRepositoryImpl
import com.roadguide.app.data.repository.RouteRepository
import com.roadguide.app.data.repository.RouteRepositoryImpl
import com.roadguide.app.data.repository.RouteSegmentRepository
import com.roadguide.app.data.repository.RouteSegmentRepositoryImpl
import com.roadguide.app.data.repository.StayRepository
import com.roadguide.app.data.repository.StayRepositoryImpl
import com.roadguide.app.data.repository.TrackingSettingsRepository
import com.roadguide.app.data.repository.TrackingSettingsRepositoryImpl
import com.roadguide.app.data.source.CamsSource
import com.roadguide.app.data.source.EnvironmentalDataFetcher
import com.roadguide.app.data.source.LocationSource
import com.roadguide.app.data.source.OpenMeteoWeatherSource
import com.roadguide.app.data.source.PermissionSource
import com.roadguide.app.domain.usecase.GetDailyReport
import com.roadguide.app.domain.usecase.GetEnvironmentalSample
import com.roadguide.app.domain.usecase.GetRouteSummaries
import com.roadguide.app.domain.usecase.GetTrackingSettings
import com.roadguide.app.domain.usecase.ToggleTracking
import com.roadguide.app.domain.usecase.impl.GetDailyReportImpl
import com.roadguide.app.domain.usecase.impl.GetEnvironmentalSampleImpl
import com.roadguide.app.domain.usecase.impl.GetRouteSummariesImpl
import com.roadguide.app.domain.usecase.impl.GetTrackingSettingsImpl
import com.roadguide.app.domain.usecase.impl.ToggleTrackingImpl
import com.roadguide.app.mapper.RouteMapper

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RoadGuideDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            RoadGuideDatabase::class.java,
            "road_guide.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideLocationPointDao(database: RoadGuideDatabase): LocationPointDao =
        database.locationPointDao()

    @Provides
    @Singleton
    fun provideRouteDao(database: RoadGuideDatabase): RouteDao =
        database.routeDao()

    @Provides
    @Singleton
    fun provideRouteSegmentDao(database: RoadGuideDatabase): RouteSegmentDao =
        database.routeSegmentDao()

    @Provides
    @Singleton
    fun provideStayDao(database: RoadGuideDatabase): StayDao =
        database.stayDao()

    @Provides
    @Singleton
    fun providePlaceDao(database: RoadGuideDatabase): PlaceDao =
        database.placeDao()

    @Provides
    @Singleton
    fun provideEnvironmentalSampleDao(database: RoadGuideDatabase): EnvironmentalSampleDao =
        database.environmentalSampleDao()

    @Provides
    @Singleton
    fun provideExposureResultDao(database: RoadGuideDatabase): ExposureResultDao =
        database.exposureResultDao()

    @Provides
    @Singleton
    fun provideDailyReportDao(database: RoadGuideDatabase): DailyReportDao =
        database.dailyReportDao()

    @Provides
    @Singleton
    fun providePrivacyZoneDao(database: RoadGuideDatabase): PrivacyZoneDao =
        database.privacyZoneDao()

    @Provides
    @Singleton
    fun provideTrackingPrefs(@ApplicationContext context: Context): TrackingPrefs =
        TrackingPrefs(context)

    @Provides
    @Singleton
    fun providePermissionSource(@ApplicationContext context: Context): PermissionSource =
        PermissionSource(context)

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10_000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .readTimeout(10_000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .writeTimeout(10_000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

    @Provides
    @Singleton
    fun provideOpenMeteoSource(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): OpenMeteoWeatherSource =
        OpenMeteoWeatherSource(context, okHttpClient)

    @Provides
    @Singleton
    fun provideCamsSource(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): CamsSource =
        CamsSource(context, okHttpClient)

    @Provides
    @Singleton
    fun provideEnvironmentalDataFetcher(
        openMeteo: OpenMeteoWeatherSource,
        cams: CamsSource,
        validator: EnvironmentalValidator
    ): EnvironmentalDataFetcher =
        EnvironmentalDataFetcher(openMeteo, cams, validator)

    @Provides
    @Singleton
    fun provideEnvironmentalValidator(): EnvironmentalValidator =
        EnvironmentalValidator()

    @Provides
    @Singleton
    fun provideLocationSource(@ApplicationContext context: Context): LocationSource =
        LocationSource(context)

    @Provides
    @Singleton
    fun provideEnvironmentalSampleRepository(
        dao: EnvironmentalSampleDao,
        fetcher: EnvironmentalDataFetcher,
        trackingPrefs: TrackingPrefs
    ): EnvironmentalSampleRepository =
        EnvironmentalSampleRepositoryImpl(dao, fetcher, trackingPrefs)

    @Provides
    @Singleton
    fun provideLocationPointRepository(dao: LocationPointDao): LocationPointRepository =
        LocationPointRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideRouteRepository(dao: RouteDao): RouteRepository =
        RouteRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideRouteSegmentRepository(dao: RouteSegmentDao): RouteSegmentRepository =
        RouteSegmentRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideStayRepository(dao: StayDao): StayRepository =
        StayRepositoryImpl(dao)

    @Provides
    @Singleton
    fun providePlaceRepository(dao: PlaceDao): PlaceRepository =
        PlaceRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideExposureResultRepository(dao: ExposureResultDao): ExposureResultRepository =
        ExposureResultRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideDailyReportRepository(dao: DailyReportDao): DailyReportRepository =
        DailyReportRepositoryImpl(dao)

    @Provides
    @Singleton
    fun providePrivacyZoneRepository(dao: PrivacyZoneDao): PrivacyZoneRepository =
        PrivacyZoneRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideTrackingSettingsRepository(trackingPrefs: TrackingPrefs): TrackingSettingsRepository =
        TrackingSettingsRepositoryImpl(trackingPrefs)

    @Provides
    @Singleton
    fun provideGetTrackingSettings(repository: TrackingSettingsRepository): GetTrackingSettings =
        GetTrackingSettingsImpl(repository)

    @Provides
    @Singleton
    fun provideRouteMapper(): RouteMapper = RouteMapper

    @Provides
    @Singleton
    fun provideGetRouteSummaries(
        repository: RouteRepository,
        mapper: RouteMapper
    ): GetRouteSummaries =
        GetRouteSummariesImpl(repository, mapper)

    @Provides
    @Singleton
    fun provideToggleTracking(repository: TrackingSettingsRepository): ToggleTracking =
        ToggleTrackingImpl(repository)

    @Provides
    @Singleton
    fun provideGetDailyReport(repository: DailyReportRepository): GetDailyReport =
        GetDailyReportImpl(repository)

    @Provides
    @Singleton
    fun provideGetEnvironmentalSample(repository: EnvironmentalSampleRepository): GetEnvironmentalSample =
        GetEnvironmentalSampleImpl(repository)
}

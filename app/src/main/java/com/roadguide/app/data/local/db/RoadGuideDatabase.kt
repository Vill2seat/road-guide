package com.roadguide.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.roadguide.app.data.local.Converters
import com.roadguide.app.data.local.dao.DailyReportDao
import com.roadguide.app.data.local.dao.EnvironmentalSampleDao
import com.roadguide.app.data.local.dao.ExposureResultDao
import com.roadguide.app.data.local.dao.LocationPointDao
import com.roadguide.app.data.local.dao.PlaceDao
import com.roadguide.app.data.local.dao.PrivacyZoneDao
import com.roadguide.app.data.local.dao.RouteDao
import com.roadguide.app.data.local.dao.RouteSegmentDao
import com.roadguide.app.data.local.dao.StayDao
import com.roadguide.app.data.local.entity.DailyReport
import com.roadguide.app.data.local.entity.DataRetentionPolicy
import com.roadguide.app.data.local.entity.EnvironmentalSample
import com.roadguide.app.data.local.entity.ExposureResult
import com.roadguide.app.data.local.entity.LocationPoint
import com.roadguide.app.data.local.entity.Place
import com.roadguide.app.data.local.entity.PrivacyZone
import com.roadguide.app.data.local.entity.Route
import com.roadguide.app.data.local.entity.RouteSegment
import com.roadguide.app.data.local.entity.Stay

@Database(
    entities = [
        LocationPoint::class,
        Route::class,
        RouteSegment::class,
        Stay::class,
        Place::class,
        EnvironmentalSample::class,
        ExposureResult::class,
        DailyReport::class,
        PrivacyZone::class,
        DataRetentionPolicy::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class RoadGuideDatabase : RoomDatabase() {

    abstract fun locationPointDao(): LocationPointDao
    abstract fun routeDao(): RouteDao
    abstract fun routeSegmentDao(): RouteSegmentDao
    abstract fun stayDao(): StayDao
    abstract fun placeDao(): PlaceDao
    abstract fun environmentalSampleDao(): EnvironmentalSampleDao
    abstract fun exposureResultDao(): ExposureResultDao
    abstract fun dailyReportDao(): DailyReportDao
    abstract fun privacyZoneDao(): PrivacyZoneDao

    companion object {
        @Volatile private var INSTANCE: RoadGuideDatabase? = null

        fun getInstance(context: Context): RoadGuideDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RoadGuideDatabase::class.java,
                    "road_guide.db"
                ).fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

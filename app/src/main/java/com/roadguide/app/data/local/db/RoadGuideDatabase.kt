package com.roadguide.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
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
    version = 3,
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

        // Migration from v1 to v2: add rawExposureScore column to exposure_results table
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE exposure_results ADD COLUMN rawExposureScore REAL NOT NULL DEFAULT 0.0"
                )
            }
        }

        // Migration from v2 to v3: add excludeFromExposure column to location_points table
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE location_points ADD COLUMN excludeFromExposure INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): RoadGuideDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RoadGuideDatabase::class.java,
                    "road_guide.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

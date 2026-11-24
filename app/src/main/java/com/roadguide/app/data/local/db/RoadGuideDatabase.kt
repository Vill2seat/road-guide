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
    version = 4,
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

        // Add the route foreign key while retaining points with missing parent routes.
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE location_points_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        timestamp INTEGER NOT NULL,
                        accuracyMeters REAL NOT NULL,
                        speedMetersPerSecond REAL,
                        bearing REAL,
                        altitude REAL,
                        provider TEXT NOT NULL,
                        isMock INTEGER NOT NULL,
                        qualityFlags INTEGER NOT NULL,
                        routeId INTEGER,
                        collectionDiagnostics TEXT,
                        excludeFromExposure INTEGER NOT NULL,
                        FOREIGN KEY(routeId) REFERENCES routes(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO location_points_new (
                        id, latitude, longitude, timestamp, accuracyMeters,
                        speedMetersPerSecond, bearing, altitude, provider, isMock,
                        qualityFlags, routeId, collectionDiagnostics, excludeFromExposure
                    )
                    SELECT
                        id, latitude, longitude, timestamp, accuracyMeters,
                        speedMetersPerSecond, bearing, altitude, provider, isMock,
                        qualityFlags,
                        CASE
                            WHEN routeId IS NOT NULL AND EXISTS (
                                SELECT 1 FROM routes WHERE routes.id = location_points.routeId
                            ) THEN routeId
                            ELSE NULL
                        END,
                        collectionDiagnostics, excludeFromExposure
                    FROM location_points
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE location_points")
                database.execSQL(
                    "ALTER TABLE location_points_new RENAME TO location_points"
                )
                database.execSQL(
                    "CREATE INDEX index_location_points_timestamp ON location_points(timestamp)"
                )
                database.execSQL(
                    "CREATE INDEX index_location_points_routeId ON location_points(routeId)"
                )
            }
        }

        fun getInstance(context: Context): RoadGuideDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RoadGuideDatabase::class.java,
                    "road_guide.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

package com.roadguide.app.data.local

import android.content.Context
import com.roadguide.app.data.local.db.RoadGuideDatabase
import com.roadguide.app.data.local.settings.TrackingPrefs

class DataProvider private constructor(
    private val database: RoadGuideDatabase,
    val trackingPrefs: TrackingPrefs
) {

    val locationPointDao = database.locationPointDao()
    val routeDao = database.routeDao()
    val routeSegmentDao = database.routeSegmentDao()
    val stayDao = database.stayDao()
    val placeDao = database.placeDao()
    val environmentalSampleDao = database.environmentalSampleDao()
    val exposureResultDao = database.exposureResultDao()
    val dailyReportDao = database.dailyReportDao()
    val privacyZoneDao = database.privacyZoneDao()

    companion object {
        @Volatile private var instance: DataProvider? = null

        fun getInstance(context: Context): DataProvider {
            return instance ?: synchronized(this) {
                instance ?: DataProvider(
                    RoadGuideDatabase.getInstance(context),
                    TrackingPrefs(context)
                ).also { instance = it }
            }
        }
    }
}

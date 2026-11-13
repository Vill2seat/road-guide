package com.roadguide.app.data.local

/** Validator constants for environment data validation */
object ValidatorConstants {
    /** Maximum age of env data in hours */
    val MAX_ENV_DATA_AGE_HOURS: Int = 3
    
    /** Maximum coordinate distance in kilometers */
    val MAX_COORD_DISTANCE_KM: Double = 30.0
    
    /** Maximum future offset in minutes */
    val MAX_FUTURE_OFFSET_MINUTES: Int = 15
}

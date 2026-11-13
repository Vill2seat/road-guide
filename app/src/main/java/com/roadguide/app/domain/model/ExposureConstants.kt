package com.roadguide.app.domain.model

/** Exposure model constants as specified in 05_EXPOSURE_ENGINE_AND_FORMULAS.md */
object ExposureConstants {
    /** Time reference window in seconds (30 minutes) */
    val TIME_REFERENCE_SECONDS: Double = 1800.0
    
    /** Maximum exposure time parameter T */
    val EXPOSURE_T_MAX: Double = 2.0
    
    /** Road source exposure factor */
    val EXPOSURE_ROAD_SOURCE_FACTOR: Double = 1.0
    
    /** Calibration coefficient */
    val EXPOSURE_CALIBRATION_K: Double = 5.0
    
    /** Default skin factor */
    val DEFAULT_SKIN_FACTOR: Double = 1.0
    
    /** Activity factor for stationary position */
    val ACTIVITY_FACTOR_STATIONARY: Double = 0.7
}

package com.roadguide.app.domain.model

object ExposureConstants {
    /** Time reference window in seconds (30 minutes) */
    const val TIME_REFERENCE_SECONDS: Double = 1800.0

    /** Maximum exposure time parameter T */
    const val EXPOSURE_T_MAX: Double = 2.0

    /** Road source exposure factor */
    const val EXPOSURE_ROAD_SOURCE_FACTOR: Double = 1.0

    /** Calibration coefficient (separate, do not apply as multiplier without spec alignment) */
    const val EXPOSURE_CALIBRATION_K: Double = 5.0

    /** Default skin factor */
    const val DEFAULT_SKIN_FACTOR: Double = 1.0

    /** Default screen factor */
    const val DEFAULT_SCREEN_FACTOR: Double = 1.0

    /** Activity factor for stationary position */
    const val ACTIVITY_FACTOR_STATIONARY: Double = 0.7
}

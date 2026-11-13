package com.roadguide.app

import com.roadguide.app.domain.model.RouteTab

/**
 * Navigation routes for the app.
 */
object ScreenRoutes {
    const val Tracker = "tracker"
    const val Timeline = "timeline"
    const val Planner = "planner"
    const val History = "history"
    const val Places = "places"
    const val Profile = "profile"
    const val Settings = "settings"

    /**
     * Maps a bottom nav tab to its route string.
     */
    fun tabToRoute(tab: RouteTab): String = when (tab) {
        RouteTab.TRACKER -> Tracker
        RouteTab.TIMELINE -> Timeline
        RouteTab.PLANNER -> Planner
        RouteTab.HISTORY -> History
        RouteTab.PLACES -> Places
    }
}
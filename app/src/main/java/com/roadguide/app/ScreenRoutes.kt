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
    const val SettingsTracking = "settings/tracking"
    const val SettingsPrivacy = "settings/privacy"
    const val SettingsEnvironmental = "settings/environmental"
    const val SettingsNotifications = "settings/notifications"
    const val SettingsMap = "settings/map"
    const val SettingsUnits = "settings/units"
    const val SettingsAccount = "settings/account"
    const val SettingsAbout = "settings/about"
    const val Permission = "permission"
    const val Weather = "weather"
    const val DailyReport = "daily_report"
    const val Onboarding = "onboarding"

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
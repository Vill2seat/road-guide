package com.roadguide.app.domain.model

/**
 * Bottom navigation tabs defined per design system spec (08_UI_UX_AND_DESIGN_SYSTEM.md).
 */
enum class RouteTab(val id: String, val label: String, val icon: String) {
    TRACKER("tracker", "Map", "map"),
    TIMELINE("timeline", "Timeline", "timeline"),
    PLANNER("planner", "Planner", "route"),
    HISTORY("history", "History", "calendar"),
    PLACES("places", "Places", "map-pin");

    companion object {
        fun all(): List<RouteTab> = values().toList()
    }
}

/**
 * Top-level destination accessible from top bar / avatar.
 */
enum class TopLevelDestination(val id: String, val label: String) {
    PROFILE("profile", "Profile"),
    SETTINGS("settings", "Settings")
}
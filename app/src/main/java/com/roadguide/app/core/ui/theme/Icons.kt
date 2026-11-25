package com.roadguide.app.core.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.outlined.Work
import androidx.compose.runtime.Composable
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Road Guide icon system.
 *
 * Outlined Material icons are used as Compose vectors and follow the Figma line-icon
 * treatment while exported SVG assets are progressively integrated.
 */
object RgIcon {

    // Navigation
    val Map = IconData("map", Color(0xFF317854))
    val Timeline = IconData("timeline", Color(0xFF317854))
    val History = IconData("history", Color(0xFF317854))
    val Route = IconData("route", Color(0xFF317854))
    val Calendar = IconData("calendar", Color(0xFF317854))
    val MapPin = IconData("map-pin", Color(0xFF317854))
    val User = IconData("user", Color(0xFF317854))
    val Settings = IconData("settings", Color(0xFF317854))
    val Bell = IconData("bell", Color(0xFF317854))
    val Search = IconData("search", Color(0xFF317854))
    val Plus = IconData("plus", Color(0xFF317854))
    val Layers = IconData("layers", Color(0xFF317854))
    val Locate = IconData("locate", Color(0xFF317854))
    val ChevronDown = IconData("chevron-down", Color(0xFF317854))
    val ChevronRight = IconData("chevron-right", Color(0xFF317854))
    val Back = IconData("back", Color(0xFF317854))
    val Close = IconData("close", Color(0xFF317854))

    // Transport
    val Footprints = IconData("footprints", Color(0xFF5A8FCF))
    val Car = IconData("car", Color(0xFF5A8FCF))
    val Bus = IconData("bus", Color(0xFF5A8FCF))
    val Bicycle = IconData("bicycle", Color(0xFF5A8FCF))
    val Train = IconData("train", Color(0xFF5A8FCF))
    val Tram = IconData("tram", Color(0xFF5A8FCF))
    val Motorcycle = IconData("motorcycle", Color(0xFF5A8FCF))
    val Ferry = IconData("ferry", Color(0xFF5A8FCF))

    // Places
    val School = IconData("school", Color(0xFF8FC9A8))
    val Bag = IconData("bag", Color(0xFF5A8FCF))

    // Environment
    val Tree = IconData("tree", Color(0xFF8FC9A8))
    val Wind = IconData("wind", Color(0xFF5A8FCF))
    val CloudSun = IconData("cloud-sun", Color(0xFFE7D18B))
    val Droplets = IconData("droplets", Color(0xFF5A8FCF))
    val Sun = IconData("sun", Color(0xFFE7D18B))
    val Cloud = IconData("cloud", Color(0xFF596B61))
    val Gauge = IconData("gauge", Color(0xFF596B61))
    val Leaf = IconData("leaf", Color(0xFF8FC9A8))

    // Actions
    val Play = IconData("play", Color(0xFF317854))
    val Pause = IconData("pause", Color(0xFF317854))
    val Check = IconData("check", Color(0xFF8FC9A8))
    val Pencil = IconData("pencil", Color(0xFF5A8FCF))
    val Trash = IconData("trash", Color(0xFFD98787))
    val Edit = IconData("edit", Color(0xFF5A8FCF))
    val Filter = IconData("filter", Color(0xFF596B61))
    val Crosshair = IconData("crosshair", Color(0xFF596B61))
    val Shield = IconData("shield", Color(0xFF317854))
    val Alert = IconData("alert", Color(0xFFE8A77F))
    val Info = IconData("info", Color(0xFF5A8FCF))
    val Battery = IconData("battery", Color(0xFF596B61))
    val Refresh = IconData("refresh", Color(0xFF596B61))
    val WifiOff = IconData("wifi-off", Color(0xFF596B61))

    // Status
    val CheckCircle = IconData("check-circle", Color(0xFF8FC9A8))
    val CloseCircle = IconData("close-circle", Color(0xFFD98787))
    val HelpCircle = IconData("help-circle", Color(0xFF596B61))
    val WarningCircle = IconData("warning-circle", Color(0xFFE8A77F))
    val SuccessCircle = IconData("success-circle", Color(0xFF8FC9A8))
    val ErrorCircle = IconData("error-circle", Color(0xFFD98787))

    // Settings / Profile
    val EyeOn = IconData("eye-on", Color(0xFF596B61))
    val EyeOff = IconData("eye-off", Color(0xFF596B61))
    val Home = IconData("home", Color(0xFF317854))
    val Work = IconData("work", Color(0xFF5A8FCF))
    val Chevron = IconData("chevron", Color(0xFF596B61))
    val Globe = IconData("globe", Color(0xFF5A8FCF))
    val Pin = IconData("pin", Color(0xFF317854))
    val Clock = IconData("clock", Color(0xFF596B61))
    val TrashLarge = IconData("trash-large", Color(0xFFD98787))

    data class IconData(
        val name: String,
        val color: Color
    )

    @Composable
    fun RgIconComposable(
        icon: IconData = Map,
        modifier: Modifier = Modifier,
        contentDescription: String? = null
    ) {
        Icon(
            imageVector = iconVector(icon.name),
            contentDescription = contentDescription,
            modifier = modifier,
            tint = icon.color
        )
    }

    @Composable
    fun RgIconButton(
        onClick: () -> Unit,
        icon: IconData = Map,
        modifier: Modifier = Modifier
    ) {
        androidx.compose.material3.IconButton(
            onClick = onClick,
            modifier = modifier
        ) {
            RgIconComposable(icon = icon, contentDescription = icon.name)
        }
    }

    private fun iconVector(name: String): ImageVector = when (name) {
        "map", "pin", "map-pin" -> Icons.Outlined.Place
        "timeline", "calendar" -> Icons.Outlined.CalendarToday
        "history" -> Icons.Outlined.BarChart
        "route" -> Icons.Outlined.Route
        "user" -> Icons.Outlined.Person
        "settings" -> Icons.Outlined.Settings
        "bell" -> Icons.Outlined.Notifications
        "search" -> Icons.Outlined.Search
        "layers" -> Icons.Outlined.Layers
        "locate", "crosshair" -> Icons.Outlined.MyLocation
        "chevron-down" -> Icons.Outlined.ExpandMore
        "chevron", "chevron-right" -> Icons.Outlined.ChevronRight
        "back" -> Icons.Outlined.ArrowBack
        "close" -> Icons.Outlined.Close
        "footprints" -> Icons.Outlined.DirectionsWalk
        "car" -> Icons.Outlined.DirectionsCar
        "bicycle" -> Icons.Outlined.DirectionsBike
        "bus", "train", "tram", "ferry" -> Icons.Outlined.DirectionsBus
        "school" -> Icons.Outlined.Event
        "bag", "work" -> Icons.Outlined.Work
        "tree", "leaf" -> Icons.Outlined.Park
        "wind" -> Icons.Outlined.Air
        "cloud-sun", "sun" -> Icons.Outlined.WbSunny
        "droplets" -> Icons.Outlined.WaterDrop
        "gauge" -> Icons.Outlined.Speed
        "play" -> Icons.Outlined.PlayArrow
        "pause" -> Icons.Outlined.Pause
        "check" -> Icons.Outlined.Check
        "trash", "trash-large" -> Icons.Outlined.Delete
        "edit", "pencil" -> Icons.Outlined.Edit
        "filter" -> Icons.Outlined.FilterList
        "shield" -> Icons.Outlined.Shield
        "alert", "warning-circle" -> Icons.Outlined.Warning
        "info" -> Icons.Outlined.Info
        "battery" -> Icons.Outlined.BatteryAlert
        "refresh" -> Icons.Outlined.Refresh
        "wifi-off" -> Icons.Outlined.WifiOff
        "check-circle", "success-circle" -> Icons.Outlined.CheckCircle
        "close-circle" -> Icons.Outlined.Cancel
        "help-circle" -> Icons.Outlined.Help
        "error-circle" -> Icons.Outlined.Error
        "eye-on" -> Icons.Outlined.Visibility
        "eye-off" -> Icons.Outlined.VisibilityOff
        "globe" -> Icons.Outlined.Public
        "clock" -> Icons.Outlined.AccessTime
        else -> Icons.Outlined.Place
    }
}

package com.roadguide.app.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Road Guide icon system.
 *
 * Placeholder icons using colored circles. Figma SVG exports (~199 of ~256) are
 * available in design/figma-export/icons/ but need to be added as vector drawables
 * to be used in Compose. Material Icons Extended is available as a fallback.
 */
object RgIcon {

    // Navigation
    val Map = IconData("map", Color(0xFF6FAF8E))
    val Timeline = IconData("timeline", Color(0xFF6FAF8E))
    val History = IconData("history", Color(0xFF6FAF8E))
    val Route = IconData("route", Color(0xFF6FAF8E))
    val Calendar = IconData("calendar", Color(0xFF6FAF8E))
    val MapPin = IconData("map-pin", Color(0xFF6FAF8E))
    val User = IconData("user", Color(0xFF6FAF8E))
    val Settings = IconData("settings", Color(0xFF6FAF8E))
    val Bell = IconData("bell", Color(0xFF6FAF8E))
    val Search = IconData("search", Color(0xFF6FAF8E))
    val Plus = IconData("plus", Color(0xFF6FAF8E))
    val Layers = IconData("layers", Color(0xFF6FAF8E))
    val Locate = IconData("locate", Color(0xFF6FAF8E))
    val ChevronDown = IconData("chevron-down", Color(0xFF6FAF8E))
    val ChevronRight = IconData("chevron-right", Color(0xFF6FAF8E))
    val Back = IconData("back", Color(0xFF6FAF8E))
    val Close = IconData("close", Color(0xFF6FAF8E))

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
    val Cloud = IconData("cloud", Color(0xFF7B8981))
    val Gauge = IconData("gauge", Color(0xFF7B8981))
    val Leaf = IconData("leaf", Color(0xFF8FC9A8))

    // Actions
    val Play = IconData("play", Color(0xFF6FAF8E))
    val Pause = IconData("pause", Color(0xFF6FAF8E))
    val Check = IconData("check", Color(0xFF8FC9A8))
    val Pencil = IconData("pencil", Color(0xFF5A8FCF))
    val Trash = IconData("trash", Color(0xFFD98787))
    val Edit = IconData("edit", Color(0xFF5A8FCF))
    val Filter = IconData("filter", Color(0xFF7B8981))
    val Crosshair = IconData("crosshair", Color(0xFF7B8981))
    val Shield = IconData("shield", Color(0xFF6FAF8E))
    val Alert = IconData("alert", Color(0xFFE8A77F))
    val Info = IconData("info", Color(0xFF5A8FCF))
    val Battery = IconData("battery", Color(0xFF7B8981))
    val Refresh = IconData("refresh", Color(0xFF7B8981))
    val WifiOff = IconData("wifi-off", Color(0xFF7B8981))

    // Status
    val CheckCircle = IconData("check-circle", Color(0xFF8FC9A8))
    val CloseCircle = IconData("close-circle", Color(0xFFD98787))
    val HelpCircle = IconData("help-circle", Color(0xFF7B8981))
    val WarningCircle = IconData("warning-circle", Color(0xFFE8A77F))
    val SuccessCircle = IconData("success-circle", Color(0xFF8FC9A8))
    val ErrorCircle = IconData("error-circle", Color(0xFFD98787))

    // Settings / Profile
    val EyeOn = IconData("eye-on", Color(0xFF7B8981))
    val EyeOff = IconData("eye-off", Color(0xFF7B8981))
    val Home = IconData("home", Color(0xFF6FAF8E))
    val Work = IconData("work", Color(0xFF5A8FCF))
    val Chevron = IconData("chevron", Color(0xFF7B8981))
    val Globe = IconData("globe", Color(0xFF5A8FCF))
    val Pin = IconData("pin", Color(0xFF6FAF8E))
    val Clock = IconData("clock", Color(0xFF7B8981))
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
        androidx.compose.foundation.layout.Box(
            modifier = modifier
                .size(RgDimens.IconMedium)
                .background(icon.color, CircleShape)
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
            RgIconComposable(icon = icon, modifier = Modifier.size(RgDimens.IconMedium))
        }
    }
}

package com.roadguide.app.data.source

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PermissionSourceTest {

    private val context = RuntimeEnvironment.getApplication()
    private val permissionSource = PermissionSource(context)

    @Test
    fun `requests foreground location before notification and background permissions`() {
        val shadowApplication = shadowOf(context)

        assertEquals(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            permissionSource.getMissingPermissions()
        )

        shadowApplication.grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        assertEquals(
            listOf(Manifest.permission.POST_NOTIFICATIONS),
            permissionSource.getMissingPermissions()
        )

        shadowApplication.grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertEquals(
            listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            permissionSource.getMissingPermissions()
        )

        shadowApplication.grantPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        assertTrue(permissionSource.hasAllPermissions())
        assertTrue(permissionSource.getMissingPermissions().isEmpty())
    }

    @Test
    @Config(sdk = [28])
    fun `does not require background location permission before Android 10`() {
        shadowOf(context).grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        assertTrue(permissionSource.hasLocationPermission())
        assertTrue(permissionSource.getMissingPermissions().isEmpty())
    }
}

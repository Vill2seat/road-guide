package com.roadguide.app.data.repository

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.roadguide.app.data.local.settings.TrackingPreferences
import com.roadguide.app.data.local.settings.TrackingPrefs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test for TrackingSettingsRepository.
 * Tests DataStore preferences read/write operations.
 */
@RunWith(AndroidJUnit4::class)
class TrackingSettingsRepositoryTest {

    private lateinit var repository: TrackingSettingsRepository
    private lateinit var prefs: TrackingPrefs

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        prefs = TrackingPrefs(context)
        repository = TrackingSettingsRepositoryImpl(prefs)
    }

    @After
    fun tearDown() {
        // Preferences are automatically cleaned up by DataStore
    }

    @Test
    fun `get tracking settings from DataStore`() = runBlocking {
        // Initial state should be defaults
        val settings = repository.preferencesFlow.first()

        assertEquals(false, settings.trackingEnabled)
        assertEquals(60, settings.intervalSeconds)
        assertEquals("metric", settings.units)
        assertEquals(100, settings.aqiThreshold)
    }

    @Test
    fun `set and get tracking enabled`() = runBlocking {
        repository.setTrackingEnabled(true)
        val settings = repository.preferencesFlow.first()

        assertTrue(settings.trackingEnabled)
    }

    @Test
    fun `set and get interval seconds`() = runBlocking {
        repository.setIntervalSeconds(120)
        val settings = repository.preferencesFlow.first()

        assertEquals(120, settings.intervalSeconds)
    }

    @Test
    fun `set and get units`() = runBlocking {
        repository.setUnits("imperial")
        val settings = repository.preferencesFlow.first()

        assertEquals("imperial", settings.units)
    }

    @Test
    fun `set and get aqi threshold`() = runBlocking {
        repository.setAqiThreshold(150)
        val settings = repository.preferencesFlow.first()

        assertEquals(150, settings.aqiThreshold)
    }
}

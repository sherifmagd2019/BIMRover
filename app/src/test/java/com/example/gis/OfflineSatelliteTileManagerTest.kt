package com.example.gis

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.model.MapEngine
import com.example.model.MapLayerMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineSatelliteTileManagerTest {

    private lateinit var app: Application
    private lateinit var manager: OfflineSatelliteTileManager

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        manager = OfflineSatelliteTileManager.getInstance(app)
    }

    @Test
    fun `manager initializes and reports initial cache status`() {
        val status = manager.cacheStatus.value
        assertNotNull(status)
        assertTrue(status.tileCount >= 0)
        assertTrue(status.cacheSizeMb >= 0.0)
    }

    @Test
    fun `pre-caching site tiles generates valid offline cache files`() = runBlocking {
        // Pre-cache tiles around a bounding box
        manager.preCacheSiteTiles(
            minLat = 37.7740,
            maxLat = 37.7755,
            minLng = -122.4205,
            maxLng = -122.4185,
            minZoom = 16,
            maxZoom = 17
        )

        val status = manager.cacheStatus.value
        assertTrue(status.tileCount > 0)
        assertEquals(1.0f, status.downloadProgress, 0.01f)
    }

    @Test
    fun `layer mode and map engine have descriptive titles`() {
        assertEquals("Satellite Ortho", MapLayerMode.SATELLITE.label)
        assertEquals("Hybrid + Contours", MapLayerMode.HYBRID.label)
        assertEquals("CAD Blueprint", MapLayerMode.CAD_BLUEPRINT.label)

        assertEquals("Offline GIS Canvas", MapEngine.OFFLINE_GIS.label)
        assertEquals("Google Maps Satellite", MapEngine.GOOGLE_MAPS.label)
    }
}

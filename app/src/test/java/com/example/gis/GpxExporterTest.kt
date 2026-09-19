package com.example.gis

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.StakedRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GpxExporterTest {

    private lateinit var app: Application
    private lateinit var sampleRecords: List<StakedRecordEntity>

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        sampleRecords = listOf(
            StakedRecordEntity(
                id = 1,
                pointName = "COL-A1",
                pointCategory = "Structural Column",
                targetLat = 37.7749290,
                targetLng = -122.4194155,
                targetElev = 35.420,
                measuredLat = 37.7749295,
                measuredLng = -122.4194160,
                measuredElev = 35.412,
                deltaNorthMeters = 0.005,
                deltaEastMeters = -0.004,
                deltaElevMeters = -0.008,
                totalHorizontalErrorMeters = 0.0064,
                isWithinTolerance = true,
                surveyorName = "Field Surveyor 1",
                notes = "Anchor bolts verified & torqued",
                timestamp = 1715000000000L
            ),
            StakedRecordEntity(
                id = 2,
                pointName = "PILE-B2",
                pointCategory = "Foundation Pile",
                targetLat = 37.7750100,
                targetLng = -122.4193500,
                targetElev = 32.100,
                measuredLat = 37.7750108,
                measuredLng = -122.4193490,
                measuredElev = 32.075,
                deltaNorthMeters = 0.025,
                deltaEastMeters = 0.015,
                deltaElevMeters = -0.025,
                totalHorizontalErrorMeters = 0.029,
                isWithinTolerance = false,
                surveyorName = "Field Surveyor 2",
                notes = "Rebar obstruction offset",
                timestamp = 1715000060000L
            )
        )
    }

    @Test
    fun `generateGpx with As-Built mode produces compliant GPX 1_1 with BIM metadata`() {
        val gpx = GpxExporter.generateGpx(
            records = sampleRecords,
            options = GpxExportOptions(
                mode = GpxExportMode.AS_BUILT_ONLY,
                includeExtensions = true,
                projectName = "Test Site Stakeout"
            )
        )

        assertTrue(gpx.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue(gpx.contains("<gpx version=\"1.1\""))
        assertTrue(gpx.contains("xmlns=\"http://www.topografix.com/GPX/1/1\""))
        assertTrue(gpx.contains("xmlns:bim=\"http://schemas.bimsurveyor.com/gpx/1.0\""))
        assertTrue(gpx.contains("<name>Test Site Stakeout</name>"))

        // Waypoints check
        assertTrue(gpx.contains("<name>COL-A1</name>"))
        assertTrue(gpx.contains("<name>PILE-B2</name>"))
        assertTrue(gpx.contains("<ele>35.412</ele>"))
        assertTrue(gpx.contains("<ele>32.075</ele>"))

        // Tolerance symbols
        assertTrue(gpx.contains("<sym>Flag, Green</sym>"))
        assertTrue(gpx.contains("<sym>Flag, Red</sym>"))

        // Extensions check
        assertTrue(gpx.contains("<bim:pointName>COL-A1</bim:pointName>"))
        assertTrue(gpx.contains("<bim:isWithinTolerance>true</bim:isWithinTolerance>"))
        assertTrue(gpx.contains("<bim:isWithinTolerance>false</bim:isWithinTolerance>"))
        assertTrue(gpx.contains("<bim:surveyor>Field Surveyor 1</bim:surveyor>"))
        assertTrue(gpx.contains("<bim:notes>Anchor bolts verified &amp; torqued</bim:notes>"))
    }

    @Test
    fun `generateGpx with As-Built and Design pairs generates paired waypoints`() {
        val gpx = GpxExporter.generateGpx(
            records = sampleRecords,
            options = GpxExportOptions(
                mode = GpxExportMode.AS_BUILT_AND_DESIGN,
                includeExtensions = false
            )
        )

        assertTrue(gpx.contains("<name>COL-A1_MEASURED</name>"))
        assertTrue(gpx.contains("<name>COL-A1_DESIGN</name>"))
        assertTrue(gpx.contains("<name>PILE-B2_MEASURED</name>"))
        assertTrue(gpx.contains("<name>PILE-B2_DESIGN</name>"))

        // Design target elevation
        assertTrue(gpx.contains("<ele>35.420</ele>"))
        assertTrue(gpx.contains("<ele>32.100</ele>"))
    }

    @Test
    fun `generateGpx with Design Only mode outputs reference CAD geometry`() {
        val gpx = GpxExporter.generateGpx(
            records = sampleRecords,
            options = GpxExportOptions(mode = GpxExportMode.DESIGN_ONLY)
        )

        assertTrue(gpx.contains("<name>COL-A1</name>"))
        assertTrue(gpx.contains("<type>Revit CAD Design Point</type>"))
        assertTrue(gpx.contains("<ele>35.420</ele>"))
    }

    @Test
    fun `generateGpx with deviation tracks includes trk elements`() {
        val gpx = GpxExporter.generateGpx(
            records = sampleRecords,
            options = GpxExportOptions(
                mode = GpxExportMode.AS_BUILT_ONLY,
                includeDeviationTracks = true
            )
        )

        assertTrue(gpx.contains("<trk>"))
        assertTrue(gpx.contains("<name>Deviation_COL-A1</name>"))
        assertTrue(gpx.contains("<name>Deviation_PILE-B2</name>"))
        assertTrue(gpx.contains("<trkseg>"))
        assertTrue(gpx.contains("<trkpt"))
    }

    @Test
    fun `generateSinglePointGpx creates valid isolated waypoint document`() {
        val singleGpx = GpxExporter.generateSinglePointGpx(sampleRecords[0])

        assertTrue(singleGpx.contains("<name>COL-A1</name>"))
        assertTrue(singleGpx.contains("<ele>35.412</ele>"))
        assertTrue(!singleGpx.contains("PILE-B2"))
    }

    @Test
    fun `escapeXml handles all XML special characters`() {
        val raw = "Test & <tag> \"quotes\" 'apostrophe'"
        val escaped = GpxExporter.escapeXml(raw)
        assertEquals("Test &amp; &lt;tag&gt; &quot;quotes&quot; &apos;apostrophe&apos;", escaped)
    }

    @Test
    fun `writeGpxToCache saves file to internal gpx directory`() {
        val content = GpxExporter.generateGpx(sampleRecords)
        val file = GpxExporter.writeGpxToCache(app, content, "test_asbuilt.gpx")

        assertNotNull(file)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertEquals("test_asbuilt.gpx", file.name)
    }

    @Test
    fun `generateDefaultFileName generates appropriate extension and mode prefix`() {
        val fileName1 = GpxExporter.generateDefaultFileName(GpxExportMode.AS_BUILT_ONLY)
        val fileName2 = GpxExporter.generateDefaultFileName(GpxExportMode.AS_BUILT_AND_DESIGN)

        assertTrue(fileName1.startsWith("BIM_Stakeout_AsBuilt_"))
        assertTrue(fileName1.endsWith(".gpx"))
        assertTrue(fileName2.startsWith("BIM_Stakeout_Pairs_"))
        assertTrue(fileName2.endsWith(".gpx"))
    }
}

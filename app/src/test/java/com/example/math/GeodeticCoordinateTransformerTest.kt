package com.example.math

import com.example.model.CutFillState
import com.example.model.ProjectAnchor
import com.example.model.RevitPoint
import com.example.model.SurveyUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GeodeticCoordinateTransformerTest {

    private val baseAnchor = ProjectAnchor(
        id = "test_anchor",
        siteName = "Test Site Alpha",
        latitude = 37.774929,
        longitude = -122.419416,
        elevationMeters = 35.420,
        trueNorthOffsetDegrees = 0.0,
        unit = SurveyUnit.METERS
    )

    @Test
    fun `origin local point transforms exactly to anchor geodetic coordinates`() {
        val originPoint = RevitPoint(
            id = "ORIGIN",
            name = "Base Origin",
            localX = 0.0,
            localY = 0.0,
            localZ = 0.0,
            category = "Benchmark"
        )

        val transformed = GeodeticCoordinateTransformer.transformPoint(originPoint, baseAnchor)

        assertEquals(baseAnchor.latitude, transformed.targetLatitude, 1e-9)
        assertEquals(baseAnchor.longitude, transformed.targetLongitude, 1e-9)
        assertEquals(baseAnchor.elevationMeters, transformed.targetElevationMeters, 1e-6)
        assertEquals(0.0, transformed.gridDeltaNorthMeters, 1e-6)
        assertEquals(0.0, transformed.gridDeltaEastMeters, 1e-6)
    }

    @Test
    fun `pure north offset translation adheres to WGS84 meridional curvature`() {
        val northDistance = 100.0 // 100 meters North
        val northPoint = RevitPoint(
            id = "N100",
            name = "Point North 100m",
            localX = 0.0,
            localY = northDistance,
            localZ = 0.0,
            category = "Column"
        )

        val transformed = GeodeticCoordinateTransformer.transformPoint(northPoint, baseAnchor)

        // Delta North must equal 100m
        assertEquals(100.0, transformed.gridDeltaNorthMeters, 1e-3)
        assertEquals(0.0, transformed.gridDeltaEastMeters, 1e-3)

        // Latitude should increase; longitude should remain unchanged
        assertTrue(transformed.targetLatitude > baseAnchor.latitude)
        assertEquals(baseAnchor.longitude, transformed.targetLongitude, 1e-7)

        // Calculated geodetic distance from anchor to transformed target must equal 100m
        val guidance = GeodeticCoordinateTransformer.calculateStakeoutGuidance(
            target = transformed,
            currentLat = baseAnchor.latitude,
            currentLng = baseAnchor.longitude,
            currentElevationMeters = baseAnchor.elevationMeters,
            deviceHeadingDegrees = 0f,
            horizontalToleranceMeters = 0.020
        )

        assertEquals(100.0, guidance.currentDistanceMeters, 0.05) // Within 5cm geodetic fidelity
        assertEquals(0.0, guidance.horizontalBearingDegrees, 0.1) // Heading 0° North
    }

    @Test
    fun `pure east offset translation adheres to WGS84 prime vertical curvature`() {
        val eastDistance = 50.0 // 50 meters East
        val eastPoint = RevitPoint(
            id = "E50",
            name = "Point East 50m",
            localX = eastDistance,
            localY = 0.0,
            localZ = 0.0,
            category = "Wall"
        )

        val transformed = GeodeticCoordinateTransformer.transformPoint(eastPoint, baseAnchor)

        // Delta East must equal 50m
        assertEquals(0.0, transformed.gridDeltaNorthMeters, 1e-3)
        assertEquals(50.0, transformed.gridDeltaEastMeters, 1e-3)

        // Longitude should increase; latitude should remain identical
        assertTrue(transformed.targetLongitude > baseAnchor.longitude)
        assertEquals(baseAnchor.latitude, transformed.targetLatitude, 1e-7)

        val guidance = GeodeticCoordinateTransformer.calculateStakeoutGuidance(
            target = transformed,
            currentLat = baseAnchor.latitude,
            currentLng = baseAnchor.longitude,
            currentElevationMeters = baseAnchor.elevationMeters,
            deviceHeadingDegrees = 0f,
            horizontalToleranceMeters = 0.020
        )

        assertEquals(50.0, guidance.currentDistanceMeters, 0.05)
        assertEquals(90.0, guidance.horizontalBearingDegrees, 0.1) // Heading 90° East
    }

    @Test
    fun `true north rotation rotates local axes accurately`() {
        // With True North = 90°, Local +Y (which was Revit Project North) rotates clockwise into Grid East!
        val rotatedAnchor = baseAnchor.copy(trueNorthOffsetDegrees = 90.0)

        val localPoint = RevitPoint(
            id = "ROT1",
            name = "Rotated Point",
            localX = 0.0,
            localY = 20.0, // 20m in Revit Y
            localZ = 0.0,
            category = "Column"
        )

        val transformed = GeodeticCoordinateTransformer.transformPoint(localPoint, rotatedAnchor)

        // 20m in Y rotated 90° clockwise points East:
        // gridDeltaNorth ≈ 0, gridDeltaEast ≈ 20.0
        assertEquals(0.0, transformed.gridDeltaNorthMeters, 1e-2)
        assertEquals(20.0, transformed.gridDeltaEastMeters, 1e-2)
    }

    @Test
    fun `us survey feet conversion scales coordinates accurately`() {
        val usFeetAnchor = baseAnchor.copy(unit = SurveyUnit.FEET)

        val pointInFeet = RevitPoint(
            id = "FT1",
            name = "Foot Point",
            localX = 100.0, // 100 US Survey feet
            localY = 0.0,
            localZ = 10.0, // 10 US Survey feet
            category = "Pier"
        )

        val transformed = GeodeticCoordinateTransformer.transformPoint(pointInFeet, usFeetAnchor)

        // 100 ft = 30.48006096 m
        val expectedEastMeters = 100.0 * (1200.0 / 3937.0)
        assertEquals(expectedEastMeters, transformed.gridDeltaEastMeters, 1e-4)

        val expectedElevMeters = baseAnchor.elevationMeters + (10.0 * (1200.0 / 3937.0))
        assertEquals(expectedElevMeters, transformed.targetElevationMeters, 1e-4)
    }

    @Test
    fun `cut fill determination classifies correctly`() {
        val target = GeodeticCoordinateTransformer.transformPoint(
            RevitPoint("C1", "Column", 0.0, 0.0, 10.0, "Column"),
            baseAnchor // Target Elevation = 35.42 + 10 = 45.42m
        )

        // Scenario 1: Current ground is at 46.42m (1.0m higher than design target) -> CUT 1.0m
        val cutGuidance = GeodeticCoordinateTransformer.calculateStakeoutGuidance(
            target = target,
            currentLat = target.targetLatitude,
            currentLng = target.targetLongitude,
            currentElevationMeters = 46.420,
            deviceHeadingDegrees = 0f,
            horizontalToleranceMeters = 0.020
        )
        assertEquals(CutFillState.CUT, cutGuidance.cutFillState)
        assertEquals(1.0, cutGuidance.deltaElevationMeters, 1e-3)

        // Scenario 2: Current ground is at 44.42m (1.0m lower than design target) -> FILL 1.0m
        val fillGuidance = GeodeticCoordinateTransformer.calculateStakeoutGuidance(
            target = target,
            currentLat = target.targetLatitude,
            currentLng = target.targetLongitude,
            currentElevationMeters = 44.420,
            deviceHeadingDegrees = 0f,
            horizontalToleranceMeters = 0.020
        )
        assertEquals(CutFillState.FILL, fillGuidance.cutFillState)
        assertEquals(-1.0, fillGuidance.deltaElevationMeters, 1e-3)

        // Scenario 3: Within 10mm elevation tolerance -> ON_GRADE
        val onGradeGuidance = GeodeticCoordinateTransformer.calculateStakeoutGuidance(
            target = target,
            currentLat = target.targetLatitude,
            currentLng = target.targetLongitude,
            currentElevationMeters = 45.425, // 5mm delta
            deviceHeadingDegrees = 0f,
            horizontalToleranceMeters = 0.020
        )
        assertEquals(CutFillState.ON_GRADE, onGradeGuidance.cutFillState)
    }

    @Test
    fun `horizontal tolerance detector flags target lock accurately`() {
        val target = GeodeticCoordinateTransformer.transformPoint(
            RevitPoint("C1", "Column", 0.0, 0.0, 0.0, "Column"),
            baseAnchor
        )

        // At exact target: distance = 0, within tolerance (<= 0.020m)
        val lockedGuidance = GeodeticCoordinateTransformer.calculateStakeoutGuidance(
            target = target,
            currentLat = target.targetLatitude,
            currentLng = target.targetLongitude,
            currentElevationMeters = target.targetElevationMeters,
            deviceHeadingDegrees = 0f,
            horizontalToleranceMeters = 0.020
        )
        assertTrue(lockedGuidance.isInTolerance)
        assertTrue(lockedGuidance.currentDistanceMeters < 0.001)

        // 1 meter away: not within tolerance
        val farGuidance = GeodeticCoordinateTransformer.calculateStakeoutGuidance(
            target = target,
            currentLat = target.targetLatitude + 0.00001,
            currentLng = target.targetLongitude,
            currentElevationMeters = target.targetElevationMeters,
            deviceHeadingDegrees = 0f,
            horizontalToleranceMeters = 0.020
        )
        assertFalse(farGuidance.isInTolerance)
    }

    @Test
    fun `generateRevitCadGridLines produces numbered and lettered georeferenced axes`() {
        val points = listOf(
            RevitPoint("C1", "Column 1", 0.0, 0.0, 0.0, "Column"),
            RevitPoint("C2", "Column 2", 12.0, 0.0, 0.0, "Column"),
            RevitPoint("C3", "Column 3", 0.0, 16.0, 0.0, "Column"),
            RevitPoint("C4", "Column 4", 12.0, 16.0, 0.0, "Column")
        )

        val gridLines = GeodeticCoordinateTransformer.generateRevitCadGridLines(baseAnchor, points)

        assertTrue(gridLines.isNotEmpty())
        val numberGrids = gridLines.filter { !it.isLetterGrid }
        val letterGrids = gridLines.filter { it.isLetterGrid }

        assertTrue(numberGrids.isNotEmpty())
        assertTrue(letterGrids.isNotEmpty())

        // Geodetic coordinates must be valid and non-zero
        for (g in gridLines) {
            assertTrue(g.startLat > 0.0)
            assertTrue(g.endLat > 0.0)
            assertTrue(g.startLng < 0.0)
            assertTrue(g.endLng < 0.0)
        }
    }

    @Test
    fun `generateStructuralFootprints generates closed 4-vertex polygons`() {
        val points = listOf(
            RevitPoint("COL-1", "Column 1", 5.0, 5.0, 0.0, "Structural Column"),
            RevitPoint("CORE-1", "Shear Core", 15.0, 15.0, 0.0, "Elevator Core")
        )

        val footprints = GeodeticCoordinateTransformer.generateStructuralFootprints(baseAnchor, points)

        assertEquals(2, footprints.size)
        for (fp in footprints) {
            assertEquals(4, fp.localVertices.size)
            assertEquals(4, fp.geodeticVertices.size)
            assertTrue(fp.geodeticVertices.all { it.first > 0.0 && it.second < 0.0 })
        }
    }
}

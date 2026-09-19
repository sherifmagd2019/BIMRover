package com.example.math

import com.example.model.CutFillState
import com.example.model.ProjectAnchor
import com.example.model.RevitCadGridLine
import com.example.model.RevitPoint
import com.example.model.RevitStructuralPolygon
import com.example.model.StakeoutGuidance
import com.example.model.SurveyUnit
import com.example.model.TransformedStakeoutPoint
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geodetic & BIM Coordinate Transformation Engine.
 *
 * Implements rigorous transformation from Autodesk Revit local project Cartesian
 * coordinates (Project Base Point / Survey Point) to real-world WGS-84 ellipsoidal
 * coordinates (EPSG:4326) and design elevation.
 *
 * WGS-84 Reference Ellipsoid Parameters:
 * - Semi-major axis (a) = 6,378,137.0 meters
 * - Flattening (f) = 1 / 298.257223563
 * - First eccentricity squared (e²) = 2f - f² ≈ 0.00669437999014
 */
object GeodeticCoordinateTransformer {

    const val WGS84_A: Double = 6378137.0
    const val WGS84_F: Double = 1.0 / 298.257223563
    const val WGS84_E_SQ: Double = 2.0 * WGS84_F - (WGS84_F * WGS84_F)
    const val DEG_TO_RAD: Double = PI / 180.0
    const val RAD_TO_DEG: Double = 180.0 / PI

    /**
     * Calculates the meridional radius of curvature M(phi) at a given latitude.
     * M is the radius of curvature in the meridian (North-South) plane.
     */
    fun meridionalRadius(latitudeRad: Double): Double {
        val sinLat = sin(latitudeRad)
        val denom = 1.0 - WGS84_E_SQ * sinLat * sinLat
        return (WGS84_A * (1.0 - WGS84_E_SQ)) / (denom * sqrt(denom))
    }

    /**
     * Calculates the prime vertical radius of curvature N(phi) at a given latitude.
     * N is the radius of curvature perpendicular to the meridian (East-West) plane.
     */
    fun primeVerticalRadius(latitudeRad: Double): Double {
        val sinLat = sin(latitudeRad)
        val denom = sqrt(1.0 - WGS84_E_SQ * sinLat * sinLat)
        return WGS84_A / denom
    }

    /**
     * Translates local Revit Cartesian offsets into geographic North and East metric offsets,
     * taking into account the Project True North rotation angle.
     *
     * In Autodesk Revit:
     * - Local X vector corresponds to Project Easting.
     * - Local Y vector corresponds to Project Northing.
     * - True North rotation angle theta represents the clockwise rotation angle
     *   from Project North to True North.
     *
     * Rotation Matrix:
     * Delta_North = Y * cos(theta) - X * sin(theta)
     * Delta_East  = X * cos(theta) + Y * sin(theta)
     *
     * @param localX Project Easting offset from anchor.
     * @param localY Project Northing offset from anchor.
     * @param trueNorthOffsetDegrees Clockwise angle from Project North to True North in degrees.
     * @return Pair of (Delta North in meters, Delta East in meters).
     */
    fun localToGridOffsets(
        localX: Double,
        localY: Double,
        trueNorthOffsetDegrees: Double
    ): Pair<Double, Double> {
        val thetaRad = trueNorthOffsetDegrees * DEG_TO_RAD
        val cosTheta = cos(thetaRad)
        val sinTheta = sin(thetaRad)

        val deltaNorth = localY * cosTheta - localX * sinTheta
        val deltaEast = localX * cosTheta + localY * sinTheta

        return Pair(deltaNorth, deltaEast)
    }

    /**
     * Core pure geodetic function:
     * Converts an anchor GPS position and True North angle with local Revit (X, Y, Z) offsets
     * to absolute WGS-84 real-world coordinates (Latitude, Longitude) and target elevation.
     *
     * @param anchorLat Anchor point latitude in decimal degrees.
     * @param anchorLng Anchor point longitude in decimal degrees.
     * @param anchorElevation Anchor point elevation in meters.
     * @param trueNorthAngleDegrees Clockwise angle from Project North to True North in degrees.
     * @param localX Local Revit X offset (Project East).
     * @param localY Local Revit Y offset (Project North).
     * @param localZ Local Revit Z elevation offset.
     * @param unit Measurement unit of the local inputs (Meters or Feet).
     * @return Calculated target (Latitude, Longitude, Elevation, DeltaNorth, DeltaEast).
     */
    fun transformRevitPointToWgs84(
        anchorLat: Double,
        anchorLng: Double,
        anchorElevation: Double,
        trueNorthAngleDegrees: Double,
        localX: Double,
        localY: Double,
        localZ: Double,
        unit: SurveyUnit = SurveyUnit.METERS
    ): TransformedResult {
        // 1. Convert local measurements to meters
        val localXMetric = unit.toMeters(localX)
        val localYMetric = unit.toMeters(localY)
        val localZMetric = unit.toMeters(localZ)

        // 2. Rotate by True North angle to obtain North and East metric offsets
        val (deltaNorth, deltaEast) = localToGridOffsets(localXMetric, localYMetric, trueNorthAngleDegrees)

        // 3. Project metric offsets onto the WGS-84 ellipsoid at the anchor latitude
        val anchorLatRad = anchorLat * DEG_TO_RAD
        val mRadius = meridionalRadius(anchorLatRad)
        val nRadius = primeVerticalRadius(anchorLatRad)

        val deltaLatRad = deltaNorth / mRadius
        val deltaLngRad = deltaEast / (nRadius * cos(anchorLatRad))

        val targetLat = anchorLat + (deltaLatRad * RAD_TO_DEG)
        val targetLng = anchorLng + (deltaLngRad * RAD_TO_DEG)
        val targetElevation = anchorElevation + localZMetric

        return TransformedResult(
            latitude = targetLat,
            longitude = targetLng,
            elevationMeters = targetElevation,
            deltaNorthMeters = deltaNorth,
            deltaEastMeters = deltaEast
        )
    }

    /**
     * Convenience method to transform a [RevitPoint] using a [ProjectAnchor].
     */
    fun transformPoint(
        point: RevitPoint,
        anchor: ProjectAnchor
    ): TransformedStakeoutPoint {
        val result = transformRevitPointToWgs84(
            anchorLat = anchor.latitude,
            anchorLng = anchor.longitude,
            anchorElevation = anchor.elevationMeters,
            trueNorthAngleDegrees = anchor.trueNorthOffsetDegrees,
            localX = point.localX,
            localY = point.localY,
            localZ = point.localZ,
            unit = anchor.unit
        )

        return TransformedStakeoutPoint(
            point = point,
            targetLatitude = result.latitude,
            targetLongitude = result.longitude,
            targetElevationMeters = result.elevationMeters,
            gridDeltaNorthMeters = result.deltaNorthMeters,
            gridDeltaEastMeters = result.deltaEastMeters
        )
    }

    /**
     * Calculates the local ellipsoidal metric offsets (Delta North, Delta East)
     * between point 1 (current) and point 2 (target).
     */
    fun calculateDeltaNorthEast(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Pair<Double, Double> {
        val phi1 = lat1 * DEG_TO_RAD
        val phi2 = lat2 * DEG_TO_RAD
        val lambda1 = lng1 * DEG_TO_RAD
        val lambda2 = lng2 * DEG_TO_RAD

        val meanPhi = (phi1 + phi2) / 2.0
        val mRadius = meridionalRadius(meanPhi)
        val nRadius = primeVerticalRadius(meanPhi)

        val deltaNorth = (phi2 - phi1) * mRadius
        val deltaEast = (lambda2 - lambda1) * nRadius * cos(meanPhi)

        return Pair(deltaNorth, deltaEast)
    }

    /**
     * Computes the geodetic distance in meters between two coordinates.
     */
    fun calculateGeodeticDistance(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Double {
        val (deltaNorth, deltaEast) = calculateDeltaNorthEast(lat1, lng1, lat2, lng2)
        return sqrt(deltaNorth * deltaNorth + deltaEast * deltaEast)
    }

    /**
     * Computes the forward azimuth (initial bearing in degrees 0..360°)
     * from point 1 to point 2.
     */
    fun calculateBearingDegrees(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): Double {
        val (deltaNorth, deltaEast) = calculateDeltaNorthEast(lat1, lng1, lat2, lng2)
        val bearingRad = atan2(deltaEast, deltaNorth)
        val bearingDeg = bearingRad * RAD_TO_DEG
        return (bearingDeg + 360.0) % 360.0
    }

    /**
     * Calculates real-time stakeout guidance metrics given the surveyor's current
     * GPS position, elevation, and device compass heading.
     */
    fun calculateStakeoutGuidance(
        target: TransformedStakeoutPoint,
        currentLat: Double,
        currentLng: Double,
        currentElevationMeters: Double,
        deviceHeadingDegrees: Float,
        horizontalToleranceMeters: Double = 0.020,
        verticalToleranceMeters: Double = 0.015
    ): StakeoutGuidance {
        val (deltaNorth, deltaEast) = calculateDeltaNorthEast(
            currentLat,
            currentLng,
            target.targetLatitude,
            target.targetLongitude
        )
        val distance = sqrt(deltaNorth * deltaNorth + deltaEast * deltaEast)
        val bearing = (atan2(deltaEast, deltaNorth) * RAD_TO_DEG + 360.0) % 360.0

        // Calculate relative bearing to surveyor device orientation (-180° to +180°)
        var relativeBearing = (bearing - deviceHeadingDegrees) % 360.0
        if (relativeBearing > 180.0) relativeBearing -= 360.0
        if (relativeBearing < -180.0) relativeBearing += 360.0

        // Vertical Grade Delta: Current Alt - Target Z
        // If current altitude > target level: CUT (excavate down)
        // If current altitude < target level: FILL (raise up)
        val elevationDelta = currentElevationMeters - target.targetElevationMeters
        val cutFillState = when {
            elevationDelta > verticalToleranceMeters -> CutFillState.CUT
            elevationDelta < -verticalToleranceMeters -> CutFillState.FILL
            else -> CutFillState.ON_GRADE
        }

        val isInTolerance = distance <= horizontalToleranceMeters

        return StakeoutGuidance(
            targetPoint = target,
            currentDistanceMeters = distance,
            horizontalBearingDegrees = bearing,
            relativeBearingDegrees = relativeBearing,
            deltaNorthMeters = deltaNorth,
            deltaEastMeters = deltaEast,
            deltaElevationMeters = elevationDelta,
            cutFillState = cutFillState,
            isInTolerance = isInTolerance,
            horizontalToleranceMeters = horizontalToleranceMeters,
            verticalToleranceMeters = verticalToleranceMeters
        )
    }

    /**
     * Generates CAD grid axis lines (e.g. Grids 1-4 and Grids A-D) projected
     * onto WGS-84 based on the project anchor and loaded points.
     */
    fun generateRevitCadGridLines(
        anchor: ProjectAnchor,
        points: List<RevitPoint>
    ): List<RevitCadGridLine> {
        if (points.isEmpty()) return emptyList()

        val minX = points.minOf { it.localX }
        val maxX = points.maxOf { it.localX }
        val minY = points.minOf { it.localY }
        val maxY = points.maxOf { it.localY }

        val padX = 4.0
        val padY = 4.0

        val distinctX = points.map { "%.2f".format(it.localX).toDouble() }.distinct().sorted()
        val distinctY = points.map { "%.2f".format(it.localY).toDouble() }.distinct().sorted()

        val gridLines = mutableListOf<RevitCadGridLine>()

        val selectedX = if (distinctX.size in 2..6) distinctX else listOf(minX, (minX + maxX) / 2.0, maxX)
        selectedX.forEachIndexed { i, xVal ->
            val label = "${i + 1}"
            val startY = minY - padY
            val endY = maxY + padY

            val startGeo = transformRevitPointToWgs84(
                anchor.latitude, anchor.longitude, anchor.elevationMeters,
                anchor.trueNorthOffsetDegrees, xVal, startY, 0.0, anchor.unit
            )
            val endGeo = transformRevitPointToWgs84(
                anchor.latitude, anchor.longitude, anchor.elevationMeters,
                anchor.trueNorthOffsetDegrees, xVal, endY, 0.0, anchor.unit
            )

            gridLines.add(
                RevitCadGridLine(
                    id = "GRID-$label",
                    label = label,
                    isLetterGrid = false,
                    startLocalX = xVal,
                    startLocalY = startY,
                    endLocalX = xVal,
                    endLocalY = endY,
                    startLat = startGeo.latitude,
                    startLng = startGeo.longitude,
                    endLat = endGeo.latitude,
                    endLng = endGeo.longitude
                )
            )
        }

        val selectedY = if (distinctY.size in 2..6) distinctY else listOf(minY, (minY + maxY) / 2.0, maxY)
        selectedY.forEachIndexed { i, yVal ->
            val letter = ('A' + (i % 26)).toString()
            val startX = minX - padX
            val endX = maxX + padX

            val startGeo = transformRevitPointToWgs84(
                anchor.latitude, anchor.longitude, anchor.elevationMeters,
                anchor.trueNorthOffsetDegrees, startX, yVal, 0.0, anchor.unit
            )
            val endGeo = transformRevitPointToWgs84(
                anchor.latitude, anchor.longitude, anchor.elevationMeters,
                anchor.trueNorthOffsetDegrees, endX, yVal, 0.0, anchor.unit
            )

            gridLines.add(
                RevitCadGridLine(
                    id = "GRID-$letter",
                    label = letter,
                    isLetterGrid = true,
                    startLocalX = startX,
                    startLocalY = yVal,
                    endLocalX = endX,
                    endLocalY = yVal,
                    startLat = startGeo.latitude,
                    startLng = startGeo.longitude,
                    endLat = endGeo.latitude,
                    endLng = endGeo.longitude
                )
            )
        }

        return gridLines
    }

    /**
     * Generates 2D footprint polygons (column piers and elevator core footprints)
     * projected into real-world geodetic coordinates.
     */
    fun generateStructuralFootprints(
        anchor: ProjectAnchor,
        points: List<RevitPoint>
    ): List<RevitStructuralPolygon> {
        val polygons = mutableListOf<RevitStructuralPolygon>()

        for (pt in points) {
            val halfWidth = when {
                pt.category.contains("Column", ignoreCase = true) -> 0.450
                pt.category.contains("Core", ignoreCase = true) -> 1.800
                pt.category.contains("Pile", ignoreCase = true) -> 0.600
                else -> 0.350
            }

            val localVertices = listOf(
                Pair(pt.localX - halfWidth, pt.localY - halfWidth),
                Pair(pt.localX + halfWidth, pt.localY - halfWidth),
                Pair(pt.localX + halfWidth, pt.localY + halfWidth),
                Pair(pt.localX - halfWidth, pt.localY + halfWidth)
            )

            val geodeticVertices = localVertices.map { (lx, ly) ->
                val res = transformRevitPointToWgs84(
                    anchor.latitude, anchor.longitude, anchor.elevationMeters,
                    anchor.trueNorthOffsetDegrees, lx, ly, pt.localZ, anchor.unit
                )
                Pair(res.latitude, res.longitude)
            }

            polygons.add(
                RevitStructuralPolygon(
                    id = "FP-${pt.id}",
                    name = pt.name,
                    category = pt.category,
                    localVertices = localVertices,
                    geodeticVertices = geodeticVertices
                )
            )
        }

        return polygons
    }

    data class TransformedResult(
        val latitude: Double,
        val longitude: Double,
        val elevationMeters: Double,
        val deltaNorthMeters: Double,
        val deltaEastMeters: Double
    )
}

package com.example.model

/**
 * Supported engineering measurement units in BIM Revit exports.
 */
enum class SurveyUnit(val label: String, val toMetersMultiplier: Double) {
    METERS("Meters (m)", 1.0),
    FEET("US Survey Feet (ft)", 0.3048006096),
    INTERNATIONAL_FEET("International Feet (ft)", 0.3048);

    fun toMeters(value: Double): Double = value * toMetersMultiplier
    fun fromMeters(meters: Double): Double = meters / toMetersMultiplier
}

/**
 * Real-world GPS Geodetic Anchor representing Autodesk Revit's Survey Point
 * or Project Base Point tied to WGS84 datum and True North rotation angle.
 */
data class ProjectAnchor(
    val id: String = "project_base_point",
    val siteName: String = "Skyline Tower - Structural Foundation",
    val latitude: Double = 37.774929,      // WGS84 Decimal degrees (e.g. San Francisco baseline)
    val longitude: Double = -122.419416,  // WGS84 Decimal degrees
    val elevationMeters: Double = 35.420,  // Orthometric / Ellipsoidal Height in meters
    val trueNorthOffsetDegrees: Double = 34.50, // Angle from Project North to True North (clockwise in degrees)
    val unit: SurveyUnit = SurveyUnit.METERS
)

/**
 * Single structural or architectural survey point exported from Autodesk Revit.
 */
data class RevitPoint(
    val id: String,
    val name: String,
    val localX: Double,       // Project Easting offset from Project Base Point
    val localY: Double,       // Project Northing offset from Project Base Point
    val localZ: Double,       // Local elevation offset (or absolute local level)
    val category: String = "Column Grid", // Column Grid, Foundation Pile, Anchor Bolt, Slab Edge, Pier
    val description: String = ""
)

/**
 * Result of the Geodetic transformation from Revit local Cartesian offsets
 * to absolute WGS-84 real-world coordinates and design elevation.
 */
data class TransformedStakeoutPoint(
    val point: RevitPoint,
    val targetLatitude: Double,
    val targetLongitude: Double,
    val targetElevationMeters: Double,
    val gridDeltaNorthMeters: Double,
    val gridDeltaEastMeters: Double
)

/**
 * Vertical grade status for earthwork and structural level verification.
 */
enum class CutFillState {
    CUT,       // Current elevation is above target level: needs excavation / trimming
    FILL,      // Current elevation is below target level: needs fill material / raising
    ON_GRADE   // Within surveyor tolerance
}

/**
 * Real-time dynamic guidance metric calculated against the user's current GPS position
 * and device heading.
 */
data class StakeoutGuidance(
    val targetPoint: TransformedStakeoutPoint,
    val currentDistanceMeters: Double,
    val horizontalBearingDegrees: Double,   // Geodetic bearing from user to target (0..360°)
    val relativeBearingDegrees: Double,     // Bearing relative to surveyor device heading (-180..+180°)
    val deltaNorthMeters: Double,           // Positive = Move North, Negative = Move South
    val deltaEastMeters: Double,            // Positive = Move East, Negative = Move West
    val deltaElevationMeters: Double,       // Current GPS Alt - Target Z
    val cutFillState: CutFillState,
    val isInTolerance: Boolean,             // True if horizontal error <= tolerance threshold
    val horizontalToleranceMeters: Double = 0.020, // 20mm RTK Survey Tolerance
    val verticalToleranceMeters: Double = 0.015    // 15mm Grade Tolerance
)

/**
 * Real-time GNSS hardware telemetry state.
 */
data class GnssTelemetry(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitudeMeters: Double = 0.0,
    val horizontalAccuracyMeters: Float = 0.02f,
    val verticalAccuracyMeters: Float = 0.03f,
    val deviceHeadingDegrees: Float = 0.0f,
    val satellitesCount: Int = 18,
    val isRtkFixed: Boolean = true,
    val isSimulated: Boolean = false,
    val lastUpdateTimestamp: Long = System.currentTimeMillis()
)

/**
 * Structural Revit CAD Grid Line projected onto WGS-84 real-world coordinates.
 */
data class RevitCadGridLine(
    val id: String,
    val label: String,
    val isLetterGrid: Boolean,
    val startLocalX: Double,
    val startLocalY: Double,
    val endLocalX: Double,
    val endLocalY: Double,
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double
)

/**
 * 2D boundary footprint of foundation pier or core wall geometry.
 */
data class RevitStructuralPolygon(
    val id: String,
    val name: String,
    val category: String,
    val localVertices: List<Pair<Double, Double>>,
    val geodeticVertices: List<Pair<Double, Double>>
)

/**
 * Visual layer mode for the satellite map display.
 */
enum class MapLayerMode(val label: String) {
    SATELLITE("Satellite Ortho"),
    HYBRID("Hybrid + Contours"),
    CAD_BLUEPRINT("CAD Blueprint")
}

/**
 * Map rendering engine selection (Offline Native GIS vs Google Maps).
 */
enum class MapEngine(val label: String) {
    OFFLINE_GIS("Offline GIS Canvas"),
    GOOGLE_MAPS("Google Maps Satellite")
}

/**
 * Status of locally cached satellite map tiles for zero-connectivity field use.
 */
data class OfflineSatelliteCacheStatus(
    val isCached: Boolean = true,
    val tileCount: Int = 148,
    val cacheSizeMb: Double = 4.2,
    val isPreCaching: Boolean = false,
    val downloadProgress: Float = 1.0f,
    val lastUpdatedText: String = "Ready for Offline Use"
)

package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.example.model.GnssTelemetry
import com.example.model.ProjectAnchor
import com.example.model.RevitCadGridLine
import com.example.model.RevitStructuralPolygon
import com.example.model.TransformedStakeoutPoint
import com.example.ui.theme.ExcavationAmber
import com.example.ui.theme.SafetyGreen
import com.example.ui.theme.SurveyorCyan
import com.example.ui.theme.SurveyorGold
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Google Maps Satellite Overlay Component.
 *
 * Provides high-resolution Google Maps satellite imagery with imported Revit geometry,
 * CAD structural grids, and real-time RTK stakeout guidance vectors.
 */
@Composable
fun GoogleMapsSatelliteOverlay(
    anchor: ProjectAnchor,
    points: List<TransformedStakeoutPoint>,
    selectedPoint: TransformedStakeoutPoint?,
    surveyorTelemetry: GnssTelemetry,
    gridLines: List<RevitCadGridLine>,
    structuralFootprints: List<RevitStructuralPolygon>,
    onSelectPoint: (TransformedStakeoutPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialPos = LatLng(anchor.latitude, anchor.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPos, 19f)
    }

    val mapProperties = remember {
        MapProperties(
            mapType = MapType.SATELLITE,
            isMyLocationEnabled = false,
            isBuildingEnabled = true
        )
    }

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = true,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false
        )
    }

    // Recenter if target point changes
    LaunchedEffect(selectedPoint) {
        if (selectedPoint != null) {
            val targetLatLng = LatLng(selectedPoint.targetLatitude, selectedPoint.targetLongitude)
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLng(targetLatLng),
                600
            )
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("google_maps_satellite_overlay")) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings
        ) {
            // 1. Revit CAD Structural Grid Lines
            for (grid in gridLines) {
                val start = LatLng(grid.startLat, grid.startLng)
                val end = LatLng(grid.endLat, grid.endLng)

                Polyline(
                    points = listOf(start, end),
                    color = SurveyorGold.copy(alpha = 0.85f),
                    width = 5f
                )
            }

            // 2. Foundation Footprints & Core Walls
            for (poly in structuralFootprints) {
                if (poly.geodeticVertices.size >= 3) {
                    val latLngs = poly.geodeticVertices.map { LatLng(it.first, it.second) }
                    Polygon(
                        points = latLngs,
                        fillColor = Color(0x4400BCD4),
                        strokeColor = Color(0xFF4DD0E1),
                        strokeWidth = 4f
                    )
                }
            }

            // 3. Dynamic Laser Stakeout Guide Line (Surveyor -> Target)
            if (selectedPoint != null) {
                val surveyorLatLng = LatLng(surveyorTelemetry.latitude, surveyorTelemetry.longitude)
                val targetLatLng = LatLng(selectedPoint.targetLatitude, selectedPoint.targetLongitude)

                Polyline(
                    points = listOf(surveyorLatLng, targetLatLng),
                    color = SurveyorGold,
                    width = 7f
                )
            }

            // 4. Project Base Point Anchor Marker
            Marker(
                state = MarkerState(position = LatLng(anchor.latitude, anchor.longitude)),
                title = "Revit Anchor: ${anchor.siteName}",
                snippet = "True North: ${"%.1f".format(anchor.trueNorthOffsetDegrees)}°",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            )

            // 5. Revit Stakeout Points
            for (pt in points) {
                val isSelected = (selectedPoint?.point?.id == pt.point.id)
                val markerHue = when {
                    isSelected -> BitmapDescriptorFactory.HUE_YELLOW
                    pt.point.category.contains("Pile", ignoreCase = true) -> BitmapDescriptorFactory.HUE_CYAN
                    pt.point.category.contains("Core", ignoreCase = true) -> BitmapDescriptorFactory.HUE_VIOLET
                    else -> BitmapDescriptorFactory.HUE_ORANGE
                }

                Marker(
                    state = MarkerState(position = LatLng(pt.targetLatitude, pt.targetLongitude)),
                    title = "${pt.point.id} (${pt.point.name})",
                    snippet = "Design Elev: ${"%.3f".format(pt.targetElevationMeters)}m | Local: (${"%.2f".format(pt.point.localX)}, ${"%.2f".format(pt.point.localY)})",
                    icon = BitmapDescriptorFactory.defaultMarker(markerHue),
                    onClick = {
                        onSelectPoint(pt)
                        false
                    }
                )
            }

            // 6. Surveyor Real-time Position
            Marker(
                state = MarkerState(position = LatLng(surveyorTelemetry.latitude, surveyorTelemetry.longitude)),
                title = if (surveyorTelemetry.isRtkFixed) "RTK Fixed Surveyor" else "Autonomous GPS",
                snippet = "Heading: ${"%.0f".format(surveyorTelemetry.deviceHeadingDegrees)}° | Acc: ±${"%.2f".format(surveyorTelemetry.horizontalAccuracyMeters)}m",
                icon = BitmapDescriptorFactory.defaultMarker(
                    if (surveyorTelemetry.isRtkFixed) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_BLUE
                ),
                rotation = surveyorTelemetry.deviceHeadingDegrees,
                flat = true
            )
        }
    }
}

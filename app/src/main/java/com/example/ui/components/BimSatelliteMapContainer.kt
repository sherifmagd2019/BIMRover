package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GnssTelemetry
import com.example.model.MapEngine
import com.example.model.MapLayerMode
import com.example.model.OfflineSatelliteCacheStatus
import com.example.model.ProjectAnchor
import com.example.model.RevitCadGridLine
import com.example.model.RevitStructuralPolygon
import com.example.model.TransformedStakeoutPoint
import com.example.ui.theme.ExcavationAmber
import com.example.ui.theme.SafetyGreen
import com.example.ui.theme.SurveyorCyan
import com.example.ui.theme.SurveyorGold

/**
 * Unified Satellite Map Container.
 *
 * Hosts both the 100% Offline GIS Satellite Canvas and Google Maps Satellite Overlay,
 * with layer mode switches, offline tile pre-caching, and interactive Revit point verification.
 */
@Composable
fun BimSatelliteMapContainer(
    anchor: ProjectAnchor,
    points: List<TransformedStakeoutPoint>,
    selectedPoint: TransformedStakeoutPoint?,
    surveyorTelemetry: GnssTelemetry,
    gridLines: List<RevitCadGridLine>,
    structuralFootprints: List<RevitStructuralPolygon>,
    mapLayerMode: MapLayerMode,
    mapEngine: MapEngine,
    offlineCacheStatus: OfflineSatelliteCacheStatus,
    isSunGlaze: Boolean,
    onSelectPoint: (TransformedStakeoutPoint) -> Unit,
    onSetMapLayerMode: (MapLayerMode) -> Unit,
    onSetMapEngine: (MapEngine) -> Unit,
    onPreCacheTiles: () -> Unit,
    onClearCache: () -> Unit,
    onNavigateToStakeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLayersMenu by remember { mutableStateOf(false) }
    var inspectedPoint by remember { mutableStateOf<TransformedStakeoutPoint?>(selectedPoint) }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Map Viewport (Active Engine)
        when (mapEngine) {
            MapEngine.OFFLINE_GIS -> {
                OfflineGisSatelliteCanvas(
                    anchor = anchor,
                    points = points,
                    selectedPoint = inspectedPoint ?: selectedPoint,
                    surveyorTelemetry = surveyorTelemetry,
                    gridLines = gridLines,
                    structuralFootprints = structuralFootprints,
                    mapLayerMode = mapLayerMode,
                    isSunGlaze = isSunGlaze,
                    onSelectPoint = { pt ->
                        inspectedPoint = pt
                        onSelectPoint(pt)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            MapEngine.GOOGLE_MAPS -> {
                GoogleMapsSatelliteOverlay(
                    anchor = anchor,
                    points = points,
                    selectedPoint = inspectedPoint ?: selectedPoint,
                    surveyorTelemetry = surveyorTelemetry,
                    gridLines = gridLines,
                    structuralFootprints = structuralFootprints,
                    onSelectPoint = { pt ->
                        inspectedPoint = pt
                        onSelectPoint(pt)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 2. Top Controls Bar: Engine Switcher & Offline Status
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 12.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Engine Switcher & Layers Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Engine Selector Chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = (mapEngine == MapEngine.OFFLINE_GIS),
                            onClick = { onSetMapEngine(MapEngine.OFFLINE_GIS) },
                            label = { Text("Offline GIS", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SurveyorGold,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.testTag("engine_chip_offline_gis")
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        FilterChip(
                            selected = (mapEngine == MapEngine.GOOGLE_MAPS),
                            onClick = { onSetMapEngine(MapEngine.GOOGLE_MAPS) },
                            label = { Text("Google Satellite", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SurveyorGold,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.testTag("engine_chip_google_maps")
                        )
                    }
                }

                // Layer Switcher Button
                Surface(
                    shape = CircleShape,
                    color = if (showLayersMenu) SurveyorCyan else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 4.dp,
                    modifier = Modifier.clickable { showLayersMenu = !showLayersMenu }
                ) {
                    IconButton(
                        onClick = { showLayersMenu = !showLayersMenu },
                        modifier = Modifier.size(42.dp).testTag("toggle_layers_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Map Layers",
                            tint = if (showLayersMenu) Color.Black else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Expanded Layers Menu
            AnimatedVisibility(
                visible = showLayersMenu,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("layers_menu_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "SATELLITE & CAD LAYERS",
                            style = MaterialTheme.typography.labelSmall,
                            color = SurveyorGold,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MapLayerMode.entries.forEach { mode ->
                                val isSelected = (mapLayerMode == mode)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        onSetMapLayerMode(mode)
                                        showLayersMenu = false
                                    },
                                    label = { Text(mode.label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SurveyorCyan,
                                        selectedLabelColor = Color.Black
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Offline Cache Pre-cache Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Offline Satellite Cache: ${offlineCacheStatus.tileCount} tiles (${offlineCacheStatus.cacheSizeMb} MB)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (offlineCacheStatus.isPreCaching) {
                                    LinearProgressIndicator(
                                        progress = { offlineCacheStatus.downloadProgress },
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        color = SurveyorGold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = onPreCacheTiles,
                                enabled = !offlineCacheStatus.isPreCaching,
                                contentPadding = ButtonDefaults.ContentPadding,
                                modifier = Modifier.testTag("pre_cache_tiles_button")
                            ) {
                                Icon(
                                    imageVector = if (offlineCacheStatus.isPreCaching) Icons.Default.CloudDownload else Icons.Default.CloudDone,
                                    contentDescription = "Pre-Cache",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (offlineCacheStatus.isPreCaching) "Caching..." else "Pre-Cache", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Offline Readiness Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (mapEngine == MapEngine.OFFLINE_GIS) Color(0xDD1B5E20) else Color(0xDD0D47A1),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SafetyGreen)
                    )
                    Text(
                        text = if (mapEngine == MapEngine.OFFLINE_GIS)
                            "100% Offline Field Mode (${offlineCacheStatus.tileCount} tiles cached)"
                        else
                            "Google Satellite Online Mode",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        // 3. Bottom Inspection Sheet: Shows Selected Point Coordinates & Guidance
        val pointToInspect = inspectedPoint ?: selectedPoint
        AnimatedVisibility(
            visible = (pointToInspect != null),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            if (pointToInspect != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("map_point_inspection_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = pointToInspect.point.id,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = SurveyorGold
                                )
                                Text(
                                    text = "${pointToInspect.point.name} • ${pointToInspect.point.category}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Design Elevation Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurveyorCyan.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Z: ${"%.3f".format(pointToInspect.targetElevationMeters)} m",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SurveyorCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Coordinate Breakdown: Local vs Geodetic
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Local Revit Coordinates
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Revit Local Coordinates", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("X: ${"%.3f".format(pointToInspect.point.localX)} m", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Y: ${"%.3f".format(pointToInspect.point.localY)} m", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // WGS-84 Geodetic
                            Surface(
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("WGS-84 Geodetic Target", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Lat: ${"%.7f".format(pointToInspect.targetLatitude)}°", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Lng: ${"%.7f".format(pointToInspect.targetLongitude)}°", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons: Set as Active Target & Launch Stakeout Bullseye
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onSelectPoint(pointToInspect) },
                                modifier = Modifier.weight(1f).testTag("map_set_as_target_button")
                            ) {
                                Icon(Icons.Default.PinDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Set Target", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    onSelectPoint(pointToInspect)
                                    onNavigateToStakeout()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SurveyorGold,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier.weight(1.2f).testTag("map_stake_point_now_button")
                            ) {
                                Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stake Out", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.BimSatelliteMapContainer
import com.example.ui.components.GpxExportDialog
import com.example.ui.theme.SurveyorGold
import com.example.viewmodel.BimSurveyorViewModel

/**
 * Dedicated BIM Site Map Screen.
 *
 * Visualizes imported Revit geometry, column grid lines, and foundation piles
 * superimposed on high-resolution satellite imagery with 100% offline support.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BimSiteMapScreen(
    viewModel: BimSurveyorViewModel,
    onNavigateToStakeout: () -> Unit,
    onNavigateToCalibration: () -> Unit,
    modifier: Modifier = Modifier
) {
    val anchor by viewModel.projectAnchor.collectAsState()
    val transformedPoints by viewModel.transformedPoints.collectAsState()
    val selectedIndex by viewModel.selectedPointIndex.collectAsState()
    val telemetry by viewModel.locationTracker.telemetry.collectAsState()
    val gridLines by viewModel.cadGridLines.collectAsState()
    val footprints by viewModel.structuralFootprints.collectAsState()
    val mapLayerMode by viewModel.mapLayerMode.collectAsState()
    val mapEngine by viewModel.mapEngine.collectAsState()
    val offlineCacheStatus by viewModel.offlineCacheStatus.collectAsState()
    val isSunGlaze by viewModel.isSunGlazeMode.collectAsState()
    val stakedRecords by viewModel.stakedRecords.collectAsState()
    var showGpxExportDialog by remember { mutableStateOf(false) }

    if (showGpxExportDialog && stakedRecords.isNotEmpty()) {
        GpxExportDialog(
            records = stakedRecords,
            projectName = anchor.siteName,
            onDismiss = { showGpxExportDialog = false }
        )
    }

    val selectedPoint = if (transformedPoints.isNotEmpty() && selectedIndex in transformedPoints.indices) {
        transformedPoints[selectedIndex]
    } else {
        null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Site Satellite Map",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (stakedRecords.isNotEmpty()) {
                        IconButton(
                            onClick = { showGpxExportDialog = true },
                            modifier = Modifier.testTag("map_top_bar_export_gpx_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Export Staked GPX",
                                tint = SurveyorGold
                            )
                        }
                    }

                    IconButton(
                        onClick = onNavigateToCalibration,
                        modifier = Modifier.testTag("map_top_bar_calibrate_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Revit Anchor Calibration"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("bim_site_map_top_bar")
            )
        },
        modifier = modifier.fillMaxSize().testTag("bim_site_map_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            BimSatelliteMapContainer(
                anchor = anchor,
                points = transformedPoints,
                selectedPoint = selectedPoint,
                surveyorTelemetry = telemetry,
                gridLines = gridLines,
                structuralFootprints = footprints,
                mapLayerMode = mapLayerMode,
                mapEngine = mapEngine,
                offlineCacheStatus = offlineCacheStatus,
                isSunGlaze = isSunGlaze,
                onSelectPoint = { pt ->
                    val idx = transformedPoints.indexOfFirst { it.point.id == pt.point.id }
                    if (idx != -1) {
                        viewModel.selectPoint(idx)
                    }
                },
                onSetMapLayerMode = { viewModel.setMapLayerMode(it) },
                onSetMapEngine = { viewModel.setMapEngine(it) },
                onPreCacheTiles = { viewModel.preCacheSiteSatelliteTiles() },
                onClearCache = { viewModel.clearSatelliteCache() },
                onNavigateToStakeout = onNavigateToStakeout,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

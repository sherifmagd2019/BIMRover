package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.North
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.model.ProjectAnchor
import com.example.model.SurveyUnit
import com.example.parser.RevitPointParser
import com.example.ui.theme.GradeOnColor
import com.example.ui.theme.SurveyorCyan
import com.example.ui.theme.SurveyorGold
import com.example.viewmodel.BimSurveyorViewModel

/**
 * Revit Calibration & Import View.
 *
 * Configures the Project Base Point anchor GPS, True North rotation angle,
 * and ingests Revit CSV/JSON point schedules.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevitCalibrationScreen(
    viewModel: BimSurveyorViewModel,
    onNavigateToStakeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val projectAnchor by viewModel.projectAnchor.collectAsState()
    val revitPoints by viewModel.revitPoints.collectAsState()
    val transformedPoints by viewModel.transformedPoints.collectAsState()
    val telemetry by viewModel.locationTracker.telemetry.collectAsState()

    var siteName by remember(projectAnchor) { mutableStateOf(projectAnchor.siteName) }
    var latText by remember(projectAnchor) { mutableStateOf(projectAnchor.latitude.toString()) }
    var lngText by remember(projectAnchor) { mutableStateOf(projectAnchor.longitude.toString()) }
    var elevText by remember(projectAnchor) { mutableStateOf(projectAnchor.elevationMeters.toString()) }
    var northAngleText by remember(projectAnchor) { mutableStateOf(projectAnchor.trueNorthOffsetDegrees.toString()) }
    var selectedUnit by remember(projectAnchor) { mutableStateOf(projectAnchor.unit) }

    var unitDropdownExpanded by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importType by remember { mutableStateOf("CSV") } // CSV or JSON
    var importStatusMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "REVIT CALIBRATION",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Project Base Point & Geodetic Alignment",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Project Anchor Settings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calibration_anchor_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PROJECT BASE POINT ANCHOR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = SurveyorGold
                        )

                        // Quick action: Use Current GPS as Anchor
                        OutlinedButton(
                            onClick = {
                                viewModel.calibrateAnchorWithCurrentGps()
                                latText = telemetry.latitude.toString()
                                lngText = telemetry.longitude.toString()
                                elevText = telemetry.altitudeMeters.toString()
                            },
                            modifier = Modifier.testTag("use_current_gps_button")
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Use Device GPS", fontSize = 10.sp)
                        }
                    }

                    OutlinedTextField(
                        value = siteName,
                        onValueChange = { siteName = it },
                        label = { Text("Project / Job Site Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = latText,
                            onValueChange = { latText = it },
                            label = { Text("Anchor Latitude (°)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lngText,
                            onValueChange = { lngText = it },
                            label = { Text("Anchor Longitude (°)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = elevText,
                            onValueChange = { elevText = it },
                            label = { Text("Base Elevation (m)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = northAngleText,
                            onValueChange = { northAngleText = it },
                            label = { Text("True North Offset (°)") },
                            leadingIcon = { Icon(Icons.Default.North, contentDescription = null, tint = SurveyorCyan) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Measurement Units Selector Dropdown
                    ExposedDropdownMenuBox(
                        expanded = unitDropdownExpanded,
                        onExpandedChange = { unitDropdownExpanded = !unitDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedUnit.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Revit Project Export Units") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = unitDropdownExpanded,
                            onDismissRequest = { unitDropdownExpanded = false }
                        ) {
                            SurveyUnit.entries.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.label) },
                                    onClick = {
                                        selectedUnit = unit
                                        unitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val lat = latText.toDoubleOrNull() ?: projectAnchor.latitude
                            val lng = lngText.toDoubleOrNull() ?: projectAnchor.longitude
                            val elev = elevText.toDoubleOrNull() ?: projectAnchor.elevationMeters
                            val north = northAngleText.toDoubleOrNull() ?: projectAnchor.trueNorthOffsetDegrees
                            viewModel.updateAnchor(siteName, lat, lng, elev, north, selectedUnit)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurveyorGold, contentColor = Color(0xFF101418)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("save_calibration_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply & Save Anchor Calibration", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 2. Sample Datasets & Ingestion Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "BENCHMARK PRESETS & DATASET INGESTION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.loadPreset("skyline") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Skyline High-Rise", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Button(
                            onClick = { viewModel.loadPreset("bridge") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Bridge Abutment", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            importText = RevitPointParser.exportToCsv(RevitPointParser.getSkylineTowerSamplePoints())
                            showImportDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_import_dialog_button")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Custom Revit Schedule (CSV / JSON)", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 3. Calculated WGS-84 Points Table
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("points_preview_table_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CALCULATED REAL-WORLD TARGETS (${transformedPoints.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = SurveyorGold
                        )

                        Text(
                            text = "True North: ${"%.1f".format(projectAnchor.trueNorthOffsetDegrees)}°",
                            fontSize = 11.sp,
                            color = SurveyorCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    transformedPoints.forEachIndexed { idx, pt ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    viewModel.selectPoint(idx)
                                    onNavigateToStakeout()
                                }
                                .padding(10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}. ${pt.point.name}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = pt.point.category,
                                        fontSize = 10.sp,
                                        color = SurveyorCyan
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Local: X ${"%.2f".format(pt.point.localX)}m, Y ${"%.2f".format(pt.point.localY)}m, Z ${"%.2f".format(pt.point.localZ)}m",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Target Z: ${"%.3f".format(pt.targetElevationMeters)}m",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = SurveyorGold
                                    )
                                }

                                Text(
                                    text = "WGS84: ${"%.7f".format(pt.targetLatitude)}°, ${"%.7f".format(pt.targetLongitude)}° (ΔN: ${"%.2f".format(pt.gridDeltaNorthMeters)}m, ΔE: ${"%.2f".format(pt.gridDeltaEastMeters)}m)",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = GradeOnColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onNavigateToStakeout,
                        colors = ButtonDefaults.buttonColors(containerColor = GradeOnColor, contentColor = Color(0xFF0C1B10)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_field_stakeout_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LAUNCH FIELD STAKEOUT VIEW", fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // Import Dialog
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("Import Revit Point Schedule", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = (importType == "CSV"),
                                onClick = { importType = "CSV" },
                                label = { Text("CSV Table") }
                            )
                            FilterChip(
                                selected = (importType == "JSON"),
                                onClick = { importType = "JSON" },
                                label = { Text("JSON Array") }
                            )
                        }

                        Text(
                            text = if (importType == "CSV")
                                "Columns: Point_ID, Local_X, Local_Y, Local_Z, Category, Description"
                            else
                                "Format: [{\"name\":\"C1\",\"localX\":12.5,\"localY\":20.0,\"localZ\":-2.0}]",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = importText,
                            onValueChange = { importText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            placeholder = { Text("Paste CSV or JSON here...") }
                        )

                        if (importStatusMsg != null) {
                            Text(
                                text = importStatusMsg ?: "",
                                fontSize = 11.sp,
                                color = SurveyorGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val count = if (importType == "CSV") {
                                viewModel.importCsvPoints(importText)
                            } else {
                                viewModel.importJsonPoints(importText)
                            }
                            if (count > 0) {
                                showImportDialog = false
                                importStatusMsg = null
                            } else {
                                importStatusMsg = "Unable to parse valid structural points from input."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurveyorGold, contentColor = Color(0xFF101418))
                    ) {
                        Text("Ingest & Transform Points", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

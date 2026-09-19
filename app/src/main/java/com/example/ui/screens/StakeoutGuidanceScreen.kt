package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CutFillState
import com.example.model.GnssTelemetry
import com.example.model.StakeoutGuidance
import com.example.model.TransformedStakeoutPoint
import com.example.ui.components.BullseyeReticleView
import com.example.ui.components.DirectionalCompassView
import com.example.ui.theme.GradeCutColor
import com.example.ui.theme.GradeFillColor
import com.example.ui.theme.GradeOnColor
import com.example.ui.theme.GradeWarningColor
import com.example.ui.theme.RtkFixedGreen
import com.example.ui.theme.SurveyorCyan
import com.example.ui.theme.SurveyorGold
import com.example.viewmodel.BimSurveyorViewModel
import kotlin.math.abs

/**
 * GPS Stakeout Guidance Screen.
 *
 * Real-time split metric surveying interface optimized for high sunlight outdoor visibility.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StakeoutGuidanceScreen(
    viewModel: BimSurveyorViewModel,
    onNavigateToCalibration: () -> Unit,
    onNavigateToMap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val guidance by viewModel.activeGuidance.collectAsState()
    val telemetry by viewModel.locationTracker.telemetry.collectAsState()
    val transformedPoints by viewModel.transformedPoints.collectAsState()
    val selectedIndex by viewModel.selectedPointIndex.collectAsState()
    val isSunGlaze by viewModel.isSunGlazeMode.collectAsState()
    val toleranceMeters by viewModel.toleranceMeters.collectAsState()

    var showPointPickerSheet by remember { mutableStateOf(false) }
    var showStoreDialog by remember { mutableStateOf(false) }
    var storeNotes by remember { mutableStateOf("") }
    var showSimulationPanel by remember { mutableStateOf(false) }

    // Haptic feedback when entering tolerance zone
    LaunchedEffect(guidance?.isInTolerance) {
        if (guidance?.isInTolerance == true) {
            triggerVibration(context)
        }
    }

    val currentTarget = if (selectedIndex in transformedPoints.indices) {
        transformedPoints[selectedIndex]
    } else null

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "BIM GPS STAKEOUT",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // GNSS Status Pill
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (telemetry.isRtkFixed) RtkFixedGreen.copy(alpha = 0.2f) else GradeWarningColor.copy(alpha = 0.2f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (telemetry.isRtkFixed) "RTK FIXED" else "DGPS FLOAT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (telemetry.isRtkFixed) RtkFixedGreen else GradeWarningColor
                                )
                            }
                        }
                        Text(
                            text = "H-Acc: ±${"%.3f".format(telemetry.horizontalAccuracyMeters)}m • Sat: ${telemetry.satellitesCount}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                actions = {
                    // Satellite Map Verification View
                    IconButton(
                        onClick = onNavigateToMap,
                        modifier = Modifier.testTag("open_satellite_map_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Satellite CAD Map Overlay",
                            tint = SurveyorGold
                        )
                    }

                    // Sun-Glaze Direct Sunlight Contrast Toggle
                    IconButton(
                        onClick = { viewModel.toggleSunGlazeMode() },
                        modifier = Modifier.testTag("sun_glaze_toggle")
                    ) {
                        Icon(
                            imageVector = if (isSunGlaze) Icons.Default.Contrast else Icons.Default.WbSunny,
                            contentDescription = "Toggle Direct Sunlight High Contrast",
                            tint = if (isSunGlaze) MaterialTheme.colorScheme.primary else SurveyorGold
                        )
                    }

                    // Simulation Stepper Panel Toggle
                    IconButton(
                        onClick = { showSimulationPanel = !showSimulationPanel },
                        modifier = Modifier.testTag("sim_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Field Simulation Controls",
                            tint = if (showSimulationPanel) SurveyorCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Points List Sheet Toggle
                    IconButton(
                        onClick = { showPointPickerSheet = true },
                        modifier = Modifier.testTag("open_points_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                            contentDescription = "Select Revit Point",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Selected Target Point Banner & Selector
            TargetPointHeaderCard(
                target = currentTarget,
                currentIndex = selectedIndex,
                totalPoints = transformedPoints.size,
                onPrevious = { viewModel.previousPoint() },
                onNext = { viewModel.nextPoint() },
                onOpenList = { showPointPickerSheet = true }
            )

            // 2. Simulation Step Pad (for indoor calibration, code reviews, and testing)
            AnimatedVisibility(visible = showSimulationPanel) {
                SimulationStepPad(
                    viewModel = viewModel,
                    telemetry = telemetry,
                    currentTarget = currentTarget
                )
            }

            // 3. Primary Metric Split Layout (Distance + Vertical Cut/Fill)
            guidance?.let { guide ->
                PrimaryGuidanceMetricCard(guide = guide)

                // 4. Directional Guidance & Reticle Dual-Pane
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left: Dynamic Directional Compass Rose
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("compass_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "DIRECTIONAL CUES",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            DirectionalCompassView(
                                relativeBearingDegrees = guide.relativeBearingDegrees,
                                deviceHeadingDegrees = telemetry.deviceHeadingDegrees,
                                isInTolerance = guide.isInTolerance,
                                modifier = Modifier.size(130.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = formatRelativeTurnText(guide.relativeBearingDegrees),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (guide.isInTolerance) GradeOnColor else SurveyorGold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Right: Sub-meter RTK Bullseye Reticle
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reticle_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "BULLSEYE RETICLE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            BullseyeReticleView(
                                deltaNorthMeters = guide.deltaNorthMeters,
                                deltaEastMeters = guide.deltaEastMeters,
                                distanceMeters = guide.currentDistanceMeters,
                                isInTolerance = guide.isInTolerance,
                                toleranceMeters = toleranceMeters,
                                modifier = Modifier.size(130.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (guide.isInTolerance) "ON PIN (LOCK)" else "SEEKING PIN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (guide.isInTolerance) GradeOnColor else SurveyorCyan,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 5. Discrete Cardinal Steps Guide ("Move 4.2m North, 1.8m West")
                CardinalOffsetsCard(guide = guide)

                // 6. Action Bar: Store Staked Point
                Button(
                    onClick = { showStoreDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("store_staked_point_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (guide.isInTolerance) GradeOnColor else SurveyorGold,
                        contentColor = Color(0xFF101418)
                    )
                ) {
                    Icon(
                        imageVector = if (guide.isInTolerance) Icons.Default.CheckCircle else Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (guide.isInTolerance) "STORE STAKED PIN (IN TOLERANCE)" else "STORE AS-BUILT OBSERVATION",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            } ?: run {
                // Empty state if no points loaded
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PinDrop,
                            contentDescription = null,
                            tint = SurveyorGold,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Revit Points Loaded",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Import a Revit CSV/JSON schedule or load a benchmark project in the Calibration view.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateToCalibration,
                            colors = ButtonDefaults.buttonColors(containerColor = SurveyorGold, contentColor = Color(0xFF101418))
                        ) {
                            Text("Open Calibration & Import", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Store Staked Point Dialog
        if (showStoreDialog && guidance != null) {
            AlertDialog(
                onDismissRequest = { showStoreDialog = false },
                title = {
                    Text(
                        text = "Store Staked Point",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Point: ${guidance?.targetPoint?.point?.name}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Horiz Error: ${"%.3f".format(guidance?.currentDistanceMeters)}m • Vert Delta: ${"%.3f".format(guidance?.deltaElevationMeters)}m",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = storeNotes,
                            onValueChange = { storeNotes = it },
                            label = { Text("Surveyor Notes / Pin Type") },
                            placeholder = { Text("e.g., #5 Rebar set, Hub & Tack, 50mm offset") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.storeCurrentStakedPoint(storeNotes)
                            storeNotes = ""
                            showStoreDialog = false
                            triggerVibration(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GradeOnColor, contentColor = Color(0xFF0C1B10))
                    ) {
                        Text("Confirm & Log", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStoreDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Points Bottom Sheet
        if (showPointPickerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPointPickerSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "SELECT REVIT TARGET POINT",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier.height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(transformedPoints) { index, pt ->
                            val isSelected = (index == selectedIndex)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        viewModel.selectPoint(index)
                                        showPointPickerSheet = false
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${index + 1}. ${pt.point.name}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${pt.point.category} • Local X: ${"%.2f".format(pt.point.localX)}m, Y: ${"%.2f".format(pt.point.localY)}m, Z: ${"%.2f".format(pt.point.localZ)}m",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = "WGS84: ${"%.6f".format(pt.targetLatitude)}, ${"%.6f".format(pt.targetLongitude)}",
                                            fontSize = 10.sp,
                                            color = SurveyorCyan,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetPointHeaderCard(
    target: TransformedStakeoutPoint?,
    currentIndex: Int,
    totalPoints: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenList: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("target_point_header_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Point")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "POINT ${currentIndex + 1} OF $totalPoints",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = target?.point?.name ?: "No Target",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Point")
                    }
                }

                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .clickable { onOpenList() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = target?.point?.category ?: "List",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SurveyorCyan
                    )
                }
            }

            if (target != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "WGS84 TARGET",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${"%.7f".format(target.targetLatitude)}°, ${"%.7f".format(target.targetLongitude)}°",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TARGET ELEV (Z)",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${"%.3f".format(target.targetElevationMeters)} m",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = SurveyorGold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryGuidanceMetricCard(guide: StakeoutGuidance) {
    val distanceColor by animateColorAsState(
        targetValue = when {
            guide.isInTolerance -> GradeOnColor
            guide.currentDistanceMeters < 0.5 -> SurveyorCyan
            guide.currentDistanceMeters < 2.0 -> SurveyorGold
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "dist_color"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("primary_guidance_metric_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Metric: Real-Time Distance to Target
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DISTANCE TO TARGET",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDistance(guide.currentDistanceMeters),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = distanceColor
                )
                Text(
                    text = if (guide.isInTolerance) "IN TOLERANCE (±${(guide.horizontalToleranceMeters * 1000).toInt()}mm)" else "AZIMUTH: ${"%.1f".format(guide.horizontalBearingDegrees)}°",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (guide.isInTolerance) GradeOnColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Metric: Vertical Grade Delta (CUT / FILL)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "VERTICAL GRADE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val (badgeBg, badgeText, badgeIcon) = when (guide.cutFillState) {
                    CutFillState.CUT -> Triple(GradeCutColor, "CUT ${"%.3f".format(guide.deltaElevationMeters)}m", Icons.Default.ArrowDownward)
                    CutFillState.FILL -> Triple(GradeFillColor, "FILL ${"%.3f".format(abs(guide.deltaElevationMeters))}m", Icons.Default.ArrowUpward)
                    CutFillState.ON_GRADE -> Triple(GradeOnColor, "ON GRADE", Icons.Default.CheckCircle)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(badgeBg.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(1.5.dp, badgeBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = badgeBg,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = badgeText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = badgeBg
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (guide.cutFillState) {
                        CutFillState.CUT -> "Lower / Excavate ground"
                        CutFillState.FILL -> "Build up / Fill grade"
                        CutFillState.ON_GRADE -> "Design Elevation Achieved"
                    },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CardinalOffsetsCard(guide: StakeoutGuidance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // North/South delta
            val nsLabel = if (guide.deltaNorthMeters >= 0) "NORTH" else "SOUTH"
            val nsValue = abs(guide.deltaNorthMeters)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "MOVE $nsLabel", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "${"%.3f".format(nsValue)} m",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = SurveyorGold
                )
            }

            Box(
                modifier = Modifier
                    .height(28.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            )

            // East/West delta
            val ewLabel = if (guide.deltaEastMeters >= 0) "EAST" else "WEST"
            val ewValue = abs(guide.deltaEastMeters)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "MOVE $ewLabel", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "${"%.3f".format(ewValue)} m",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = SurveyorCyan
                )
            }
        }
    }
}

@Composable
private fun SimulationStepPad(
    viewModel: BimSurveyorViewModel,
    telemetry: GnssTelemetry,
    currentTarget: TransformedStakeoutPoint?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("simulation_stepper_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FIELD SIMULATION CONTROLS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = SurveyorCyan
                )

                // Snap to current target pin
                if (currentTarget != null) {
                    TextButton(
                        onClick = {
                            viewModel.locationTracker.setPosition(
                                currentTarget.targetLatitude,
                                currentTarget.targetLongitude,
                                currentTarget.targetElevationMeters
                            )
                        }
                    ) {
                        Text("Snap to Target (0.00m)", fontSize = 10.sp, color = GradeOnColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                text = "Simulate GNSS pole movements (+0.1m fine, +0.5m coarse) or rotate device heading for testing.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Directional Stepper D-Pad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // North / South
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedButton(
                        onClick = { viewModel.locationTracker.stepSimulatedPosition(0.5, 0.0) },
                        modifier = Modifier.size(width = 80.dp, height = 34.dp)
                    ) {
                        Text("+0.5m N", fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { viewModel.locationTracker.stepSimulatedPosition(-0.5, 0.0) },
                        modifier = Modifier.size(width = 80.dp, height = 34.dp)
                    ) {
                        Text("-0.5m S", fontSize = 10.sp)
                    }
                }

                // East / West
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedButton(
                        onClick = { viewModel.locationTracker.stepSimulatedPosition(0.0, 0.5) },
                        modifier = Modifier.size(width = 80.dp, height = 34.dp)
                    ) {
                        Text("+0.5m E", fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { viewModel.locationTracker.stepSimulatedPosition(0.0, -0.5) },
                        modifier = Modifier.size(width = 80.dp, height = 34.dp)
                    ) {
                        Text("-0.5m W", fontSize = 10.sp)
                    }
                }

                // Fine Adjust / Elev / Heading
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedButton(
                        onClick = { viewModel.locationTracker.stepSimulatedPosition(0.0, 0.0, 0.1) },
                        modifier = Modifier.size(width = 80.dp, height = 34.dp)
                    ) {
                        Text("+0.1m Z", fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { viewModel.locationTracker.rotateHeading(15f) },
                        modifier = Modifier.size(width = 80.dp, height = 34.dp)
                    ) {
                        Text("Turn +15°", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

private fun formatDistance(distMeters: Double): String {
    return if (distMeters >= 1000.0) {
        "${"%.2f".format(distMeters / 1000.0)} km"
    } else {
        "${"%.3f".format(distMeters)} m"
    }
}

private fun formatRelativeTurnText(relBearing: Double): String {
    val absVal = abs(relBearing)
    return when {
        absVal <= 5.0 -> "FACING TARGET • WALK FORWARD"
        relBearing > 0 -> "TURN ${absVal.toInt()}° RIGHT"
        else -> "TURN ${absVal.toInt()}° LEFT"
    }
}

private fun triggerVibration(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(120)
        }
    } catch (_: Exception) {
        // Safe fallback if permission restricted
    }
}

package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GradeOnColor
import com.example.ui.theme.SurveyorCyan
import com.example.ui.theme.SurveyorGold
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Sub-meter RTK Bullseye Stakeout Reticle.
 *
 * Provides high-precision sub-centimeter visual feedback when positioning
 * survey prisms, GNSS rover poles, or physical stakes directly onto design targets.
 *
 * @param deltaNorthMeters Distance North from user to target (in meters).
 * @param deltaEastMeters Distance East from user to target (in meters).
 * @param distanceMeters Absolute distance to target.
 * @param isInTolerance True when within surveyor tolerance (e.g. <= 20mm).
 * @param toleranceMeters Configured threshold (e.g. 0.02m).
 */
@Composable
fun BullseyeReticleView(
    deltaNorthMeters: Double,
    deltaEastMeters: Double,
    distanceMeters: Double,
    isInTolerance: Boolean,
    toleranceMeters: Double = 0.020,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_anim"
    )

    val targetColor by animateColorAsState(
        targetValue = if (isInTolerance) GradeOnColor else SurveyorGold,
        animationSpec = tween(250),
        label = "target_color"
    )

    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor.copy(alpha = 0.5f))
            .border(2.dp, outlineColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val canvasRadius = min(size.width, size.height) / 2f

            // Dynamic scale: If distance is <= 0.5m, view zooms in to 0.5m full radius
            // Otherwise, full radius represents 2.0 meters
            val maxMeterRange = if (distanceMeters <= 0.5) 0.5 else 2.0
            val pxPerMeter = canvasRadius / maxMeterRange

            // 1. Crosshair Axes
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            drawLine(
                color = outlineColor.copy(alpha = 0.4f),
                start = Offset(center.x, 0f),
                end = Offset(center.x, size.height),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = dashEffect
            )
            drawLine(
                color = outlineColor.copy(alpha = 0.4f),
                start = Offset(0f, center.y),
                end = Offset(size.width, center.y),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = dashEffect
            )

            // 2. Concentric Metric Rings
            val rings = if (maxMeterRange == 0.5) {
                listOf(0.5, 0.25, 0.10, toleranceMeters)
            } else {
                listOf(2.0, 1.0, 0.5, 0.10, toleranceMeters)
            }

            for (r in rings) {
                val ringPx = (r * pxPerMeter).toFloat()
                if (ringPx <= canvasRadius) {
                    val isToleranceRing = (r == toleranceMeters)
                    drawCircle(
                        color = if (isToleranceRing) GradeOnColor else outlineColor.copy(alpha = 0.35f),
                        radius = ringPx,
                        center = center,
                        style = Stroke(
                            width = if (isToleranceRing) 2.dp.toPx() else 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }
            }

            // 3. Center Target Pin (Revit Target Location)
            drawCircle(
                color = targetColor,
                radius = 6.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.Black,
                radius = 2.5.dp.toPx(),
                center = center
            )

            // 4. Calculate Current User GPS Rover Position relative to Target
            // In reticle: Target is at center (0,0). User is at (-deltaEast, -deltaNorth).
            // (Or if viewing Target relative to User: Target is at (+deltaEast, +deltaNorth)).
            // Surveyors prefer the reticle centered on Design Target, showing Rover crosshair approaching it.
            val roverOffsetEast = -deltaEastMeters
            val roverOffsetNorth = -deltaNorthMeters
            val roverDist = sqrt(roverOffsetEast * roverOffsetEast + roverOffsetNorth * roverOffsetNorth)

            val clampedDist = min(roverDist, maxMeterRange)
            val scaleFactor = if (roverDist > 0) (clampedDist / roverDist) else 1.0
            val roverX = center.x + (roverOffsetEast * scaleFactor * pxPerMeter).toFloat()
            val roverY = center.y - (roverOffsetNorth * scaleFactor * pxPerMeter).toFloat() // North is up (-Y)

            val roverCenter = Offset(roverX, roverY)

            // 5. User Rover Icon / Reticle Crosshair
            val roverColor = if (isInTolerance) GradeOnColor else SurveyorCyan

            if (isInTolerance) {
                // Pulse halo on target
                drawCircle(
                    color = GradeOnColor.copy(alpha = 0.3f),
                    radius = (toleranceMeters * pxPerMeter * pulseScale).toFloat(),
                    center = center
                )
            }

            // Draw user pole beacon
            drawCircle(
                color = roverColor.copy(alpha = 0.35f),
                radius = 12.dp.toPx(),
                center = roverCenter
            )
            drawCircle(
                color = roverColor,
                radius = 5.dp.toPx(),
                center = roverCenter
            )
            // Rover pole crosshairs
            val armLen = 8.dp.toPx()
            drawLine(
                color = roverColor,
                start = Offset(roverCenter.x - armLen, roverCenter.y),
                end = Offset(roverCenter.x + armLen, roverCenter.y),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = roverColor,
                start = Offset(roverCenter.x, roverCenter.y - armLen),
                end = Offset(roverCenter.x, roverCenter.y + armLen),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Scale and range watermark
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        ) {
            Text(
                text = if (distanceMeters <= 0.5) "ZOOM: 0.5m FINE" else "RANGE: 2.0m",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = outlineColor
            )
        }

        if (isInTolerance) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .background(GradeOnColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .border(1.dp, GradeOnColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "LOCKED ON TARGET (±${(toleranceMeters * 1000).toInt()}mm)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GradeOnColor
                )
            }
        }
    }
}

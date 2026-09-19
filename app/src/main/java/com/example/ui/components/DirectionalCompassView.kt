package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GradeOnColor
import com.example.ui.theme.SurveyorCyan
import com.example.ui.theme.SurveyorGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Dynamic Field Directional Stakeout Compass & Pointer.
 *
 * Visually illustrates the relative orientation between the surveyor's current device heading
 * and the calculated target stakeout bearing.
 *
 * @param relativeBearingDegrees Bearing to target relative to device heading (-180°..+180°).
 * @param deviceHeadingDegrees True compass heading of the phone.
 * @param isInTolerance When true, renders in glowing lock state.
 */
@Composable
fun DirectionalCompassView(
    relativeBearingDegrees: Double,
    deviceHeadingDegrees: Float,
    isInTolerance: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedRelativeBearing by animateFloatAsState(
        targetValue = relativeBearingDegrees.toFloat(),
        animationSpec = tween(durationMillis = 200),
        label = "relative_bearing_anim"
    )

    val animatedHeading by animateFloatAsState(
        targetValue = deviceHeadingDegrees,
        animationSpec = tween(durationMillis = 200),
        label = "heading_anim"
    )

    val outlineColor = MaterialTheme.colorScheme.outline
    val primaryColor = if (isInTolerance) GradeOnColor else SurveyorGold
    val cyanColor = SurveyorCyan

    Box(
        modifier = modifier
            .size(160.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = min(size.width, size.height) / 2f

            // 1. Outer Compass Ring
            drawCircle(
                color = outlineColor.copy(alpha = 0.5f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // 2. Inner Ring
            drawCircle(
                color = outlineColor.copy(alpha = 0.25f),
                radius = radius * 0.72f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 3. Cardinal Tick Marks (N, E, S, W) rotated by device heading
            rotate(degrees = -animatedHeading, pivot = center) {
                for (angle in 0 until 360 step 30) {
                    val angleRad = angle * (PI / 180.0)
                    val isMajor = (angle % 90 == 0)
                    val tickLen = if (isMajor) 12.dp.toPx() else 6.dp.toPx()
                    val tickColor = if (angle == 0) GradeOnColor else outlineColor.copy(alpha = if (isMajor) 0.8f else 0.4f)
                    val strokeW = if (isMajor) 2.5.dp.toPx() else 1.2.dp.toPx()

                    val startX = center.x + (radius - tickLen) * sin(angleRad).toFloat()
                    val startY = center.y - (radius - tickLen) * cos(angleRad).toFloat()
                    val endX = center.x + radius * sin(angleRad).toFloat()
                    val endY = center.y - radius * cos(angleRad).toFloat()

                    drawLine(
                        color = tickColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 4. Device Forward Heading Indicator (Always points straight up relative to screen)
            drawLine(
                color = cyanColor,
                start = Offset(center.x, center.y - radius * 0.72f),
                end = Offset(center.x, center.y - radius),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 5. Dynamic Stakeout Target Vector Arrow (Rotates by relative bearing)
            rotate(degrees = animatedRelativeBearing, pivot = center) {
                val arrowLength = radius * 0.78f
                val arrowWidth = 14.dp.toPx()
                val arrowPath = Path().apply {
                    moveTo(center.x, center.y - arrowLength) // Arrow tip
                    lineTo(center.x + arrowWidth, center.y - (arrowLength * 0.4f))
                    lineTo(center.x + arrowWidth * 0.35f, center.y - (arrowLength * 0.45f))
                    lineTo(center.x + arrowWidth * 0.35f, center.y)
                    lineTo(center.x - arrowWidth * 0.35f, center.y)
                    lineTo(center.x - arrowWidth * 0.35f, center.y - (arrowLength * 0.45f))
                    lineTo(center.x - arrowWidth, center.y - (arrowLength * 0.4f))
                    close()
                }

                drawPath(
                    path = arrowPath,
                    color = primaryColor
                )

                // High-visibility border around arrow
                drawPath(
                    path = arrowPath,
                    color = Color.Black.copy(alpha = 0.6f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // 6. Center Hub Pivot
            drawCircle(
                color = if (isInTolerance) GradeOnColor else cyanColor,
                radius = 5.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.Black,
                radius = 2.dp.toPx(),
                center = center
            )
        }
    }
}

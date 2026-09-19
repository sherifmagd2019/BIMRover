package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GradeOnColor
import com.example.ui.theme.SurveyorCyan
import com.example.ui.theme.SurveyorGold

@Composable
fun VoiceGuidanceBanner(
    isVoiceEnabled: Boolean,
    isSpeaking: Boolean,
    lastSpokenCue: String,
    isInTolerance: Boolean,
    onToggleVoice: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speakingPulse"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = when {
                    !isVoiceEnabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    isInTolerance -> GradeOnColor.copy(alpha = 0.8f)
                    isSpeaking -> SurveyorCyan.copy(alpha = 0.8f)
                    else -> SurveyorGold.copy(alpha = 0.4f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onOpenSettings() }
            .testTag("voice_guidance_banner"),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !isVoiceEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                isInTolerance -> GradeOnColor.copy(alpha = 0.12f)
                isSpeaking -> SurveyorCyan.copy(alpha = 0.10f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Speaker / Mic icon badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                !isVoiceEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                isInTolerance -> GradeOnColor.copy(alpha = 0.25f)
                                isSpeaking -> SurveyorCyan.copy(alpha = 0.25f)
                                else -> SurveyorGold.copy(alpha = 0.2f)
                            }
                        )
                        .alpha(if (isSpeaking) pulseAlpha else 1.0f),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            !isVoiceEnabled -> Icons.Default.VolumeOff
                            isInTolerance -> Icons.Default.VolumeUp
                            isSpeaking -> Icons.Default.RecordVoiceOver
                            else -> Icons.Default.VolumeUp
                        },
                        contentDescription = if (isVoiceEnabled) "Voice active" else "Voice muted",
                        tint = when {
                            !isVoiceEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                            isInTolerance -> GradeOnColor
                            isSpeaking -> SurveyorCyan
                            else -> SurveyorGold
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isVoiceEnabled) {
                                if (isSpeaking) "SPEAKING CUE" else "VOICE GUIDANCE"
                            } else {
                                "VOICE MUTED"
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = when {
                                !isVoiceEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                                isInTolerance -> GradeOnColor
                                isSpeaking -> SurveyorCyan
                                else -> SurveyorGold
                            }
                        )

                        if (isVoiceEnabled && isSpeaking) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SurveyorCyan)
                                    .alpha(pulseAlpha)
                            )
                        }
                    }

                    Text(
                        text = if (isVoiceEnabled) {
                            lastSpokenCue.ifBlank { "Listening for stakeout proximity changes..." }
                        } else {
                            "Tap here or speaker icon to unmute speech cues"
                        },
                        fontSize = 12.sp,
                        fontWeight = if (isInTolerance || isSpeaking) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            !isVoiceEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                            isInTolerance -> GradeOnColor
                            isSpeaking -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quick actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleVoice,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("banner_toggle_voice_button")
                ) {
                    Icon(
                        imageVector = if (isVoiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Toggle Mute",
                        tint = if (isVoiceEnabled) SurveyorGold else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("banner_open_voice_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Audio Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

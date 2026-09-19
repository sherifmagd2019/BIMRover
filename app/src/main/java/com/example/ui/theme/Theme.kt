package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Custom composition local for Sun-Glaze High Sunlight toggle
val LocalSunGlazeMode = compositionLocalOf { false }

private val SurveyorDarkColorScheme = darkColorScheme(
    primary = SurveyorGold,
    onPrimary = Color(0xFF101418),
    primaryContainer = Color(0xFF332500),
    onPrimaryContainer = SurveyorGold,
    secondary = SurveyorCyan,
    onSecondary = Color(0xFF002026),
    secondaryContainer = Color(0xFF003844),
    onSecondaryContainer = SurveyorCyan,
    tertiary = SurveyorLime,
    onTertiary = Color(0xFF00220D),
    background = FieldDarkBackground,
    onBackground = FieldDarkTextPrimary,
    surface = FieldDarkSurface,
    onSurface = FieldDarkTextPrimary,
    surfaceVariant = FieldDarkSurfaceVariant,
    onSurfaceVariant = FieldDarkTextSecondary,
    outline = FieldDarkOutline,
    outlineVariant = Color(0xFF2C3544),
    error = GradeCutColor,
    onError = Color.White
)

private val SunGlazeLightColorScheme = lightColorScheme(
    primary = Color(0xFF0B0E14),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFB703),
    onPrimaryContainer = Color(0xFF101418),
    secondary = Color(0xFF005B66),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB5F5FF),
    onSecondaryContainer = Color(0xFF002026),
    tertiary = Color(0xFF00662A),
    onTertiary = Color.White,
    background = SunGlazeBackground,
    onBackground = SunGlazeTextPrimary,
    surface = SunGlazeSurface,
    onSurface = SunGlazeTextPrimary,
    surfaceVariant = SunGlazeSurfaceVariant,
    onSurfaceVariant = SunGlazeTextSecondary,
    outline = SunGlazeOutline,
    outlineVariant = Color(0xFF718096),
    error = GradeCutColor,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    sunGlazeMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (sunGlazeMode) SunGlazeLightColorScheme else SurveyorDarkColorScheme

    CompositionLocalProvider(LocalSunGlazeMode provides sunGlazeMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PatchPulseDarkColorScheme = darkColorScheme(
    primary = PulseMint,
    onPrimary = PulseMintDark,
    primaryContainer = PulseMintContainer,
    onPrimaryContainer = OnPulseMintContainer,
    secondary = TelemetryCyan,
    onSecondary = TelemetryCyanDark,
    secondaryContainer = TelemetryCyanContainer,
    onSecondaryContainer = OnTelemetryCyanContainer,
    tertiary = AlertCoral,
    onTertiary = Color.White,
    tertiaryContainer = AlertCoralContainer,
    onTertiaryContainer = OnAlertCoralContainer,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceContainer,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceContainer,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF1E2636),
    error = AlertCoral,
    onError = Color.White,
    errorContainer = AlertCoralContainer,
    onErrorContainer = OnAlertCoralContainer,
)

private val PatchPulseLightColorScheme = lightColorScheme(
    primary = PulseMintLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = TelemetryCyanLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = AlertCoralLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE4E6),
    onTertiaryContainer = Color(0xFF881337),
    background = LightBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceContainer,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = LightSurfaceContainer,
    outline = LightOutline,
    error = AlertCoralLight,
    onError = Color.White,
)

val PatchPulseShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun PatchPulseTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) PatchPulseDarkColorScheme else PatchPulseLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PatchPulseTypography,
        shapes = PatchPulseShapes,
        content = content,
    )
}

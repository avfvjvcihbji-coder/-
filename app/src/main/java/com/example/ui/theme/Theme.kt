package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StudioLightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = DeepPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),
    tertiary = VibrantCyan,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFFAFE),
    onTertiaryContainer = Color(0xFF164E63),
    background = BackgroundPureWhite,
    onBackground = CharcoalBlack,
    surface = BackgroundPureWhite,
    onSurface = CharcoalBlack,
    surfaceVariant = SurfaceStudioGray,
    onSurfaceVariant = SlateMuted,
    surfaceContainerLowest = BackgroundPureWhite,
    surfaceContainerLow = SurfaceStudioGray,
    surfaceContainer = SurfaceElevated,
    outline = SurfaceCardBorder,
    outlineVariant = SilverLine,
    error = ErrorRedInk,
    onError = Color.White
)

private val StudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MotionStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioLightColorScheme,
        typography = StudioTypography,
        shapes = StudioShapes,
        content = content
    )
}

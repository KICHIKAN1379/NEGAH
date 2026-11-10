package com.kichikan.negah.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

private val Midnight = Color(0xFF080D1B)
private val SurfaceNavy = Color(0xFF10182D)
private val ElevatedNavy = Color(0xFF17213B)
private val NeonMint = Color(0xFF00E5A3)
private val NeonPurple = Color(0xFF8B5CF6)
private val NeonPink = Color(0xFFEC4899)
private val TextPrimary = Color(0xFFF4F6FF)
private val TextSecondary = Color(0xFFA7B1CC)
private val BorderNavy = Color(0xFF2A3655)
private val ErrorRose = Color(0xFFFF647C)

private val NegahDarkColors = darkColorScheme(
    primary = NeonMint,
    onPrimary = Color(0xFF00291E),
    primaryContainer = Color(0xFF064B3B),
    onPrimaryContainer = Color(0xFFB7FFE8),
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF33245F),
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = NeonPink,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF5C2446),
    onTertiaryContainer = Color(0xFFFFD9EA),
    background = Midnight,
    onBackground = TextPrimary,
    surface = SurfaceNavy,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedNavy,
    onSurfaceVariant = TextSecondary,
    outline = BorderNavy,
    error = ErrorRose,
    onError = Color.White,
)

private val NegahLightColors = lightColorScheme(
    primary = Color(0xFF007F60),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F5E1),
    onPrimaryContainer = Color(0xFF002117),
    secondary = Color(0xFF6840B5),
    onSecondary = Color.White,
    tertiary = Color(0xFFB52D72),
    onTertiary = Color.White,
    background = Color(0xFFF6F7FC),
    onBackground = Color(0xFF171B2A),
    surface = Color.White,
    onSurface = Color(0xFF171B2A),
    surfaceVariant = Color(0xFFE9ECF5),
    onSurfaceVariant = Color(0xFF555E73),
    outline = Color(0xFFD0D5E2),
    error = Color(0xFFBA1A1A),
)

private val NegahShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Shared visual language based on the approved UX references:
 * midnight navy surfaces with mint, purple and pink neon accents.
 */
@Composable
fun NegahTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) NegahDarkColors else NegahLightColors,
        shapes = NegahShapes,
        content = content,
    )
}

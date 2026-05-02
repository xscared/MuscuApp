package com.example.muscuapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class MuscuColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val error: Color,
    val success: Color,
    val divider: Color
)

@Immutable
data class MuscuTypography(
    val displayLarge: TextStyle, // Titres massifs (Collapsing)
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val labelSmall: TextStyle,
    val timerLarge: TextStyle,
    val monoLabel: TextStyle   // Label typé technique/chrono
)

@Immutable
data class MuscuSpacing(
    val default: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp
)

val DarkMuscuColors = MuscuColors(
    background = Color(0xFF000000), // Vrai Noir (OLED)
    surface = Color(0xFF0A0A0A),    // Très proche du noir
    surfaceVariant = Color(0xFF151515),
    primary = Color(0xFFFF4500),    // Orange-Rouge Néon
    onPrimary = Color.White,
    secondary = Color(0xFF00E5FF),  // Cyan Électrique
    textPrimary = Color.White,
    textSecondary = Color(0xFF636366), // Gris sombre
    error = Color(0xFFFF3B30),
    success = Color(0xFF32D74B),
    divider = Color(0xFF1C1C1E)
)

val LightMuscuColors = MuscuColors(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFFE5E5EA),
    primary = Color(0xFFFF4500),
    onPrimary = Color.White,
    secondary = Color(0xFF007AFF),
    textPrimary = Color(0xFF1C1C1E),
    textSecondary = Color(0xFF8E8E93),
    error = Color(0xFFFF3B30),
    success = Color(0xFF34C759),
    divider = Color(0xFFC6C6C8)
)

val DefaultMuscuTypography = MuscuTypography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 42.sp,
        letterSpacing = (-1.5).sp,
        lineHeight = 44.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        letterSpacing = (-0.5).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp
    ),
    timerLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        fontSize = 64.sp,
        letterSpacing = (-2).sp
    ),
    monoLabel = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        color = Color(0xFFFF4500).copy(alpha = 0.8f)
    )
)

val LocalMuscuColors = staticCompositionLocalOf { DarkMuscuColors }
val LocalMuscuTypography = staticCompositionLocalOf { DefaultMuscuTypography }
val LocalMuscuSpacing = staticCompositionLocalOf { MuscuSpacing() }

object MuscuTheme {
    val colors: MuscuColors
        @Composable
        get() = LocalMuscuColors.current
    
    val typography: MuscuTypography
        @Composable
        get() = LocalMuscuTypography.current
    
    val spacing: MuscuSpacing
        @Composable
        get() = LocalMuscuSpacing.current
}

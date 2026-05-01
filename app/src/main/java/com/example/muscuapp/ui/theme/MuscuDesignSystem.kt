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
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val labelSmall: TextStyle,
    val timerLarge: TextStyle
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
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    primary = Color(0xFFFF5722), // Orange Électrique
    onPrimary = Color.White,
    secondary = Color(0xFF03DAC6),
    textPrimary = Color.White,
    textSecondary = Color(0xFFB3B3B3),
    error = Color(0xFFCF6679),
    success = Color(0xFF4CAF50),
    divider = Color(0xFF2C2C2C)
)

val LightMuscuColors = MuscuColors(
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    primary = Color(0xFFFF5722), // On garde l'orange
    onPrimary = Color.White,
    secondary = Color(0xFF018786),
    textPrimary = Color(0xFF121212),
    textSecondary = Color(0xFF757575),
    error = Color(0xFFB00020),
    success = Color(0xFF388E3C),
    divider = Color(0xFFE0E0E0)
)

val DefaultMuscuTypography = MuscuTypography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    timerLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 48.sp
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

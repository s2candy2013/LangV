package com.linusv.englishcoach.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// LangV brand tokens: orange drives action, teal signals progress and practice.
private val LangVOrange = Color(0xFFF57C00)
private val LangVOrangeContainer = Color(0xFFFFE8CC)
private val LangVTeal = Color(0xFF00897B)
private val LangVTealLight = Color(0xFF4DB6AC)
private val LangVTealContainer = Color(0xFFD8F2EE)
private val Ink = Color(0xFF1F2933)
private val MutedInk = Color(0xFF52606D)
private val WarmWhite = Color(0xFFFFFCF8)
private val DarkSurface = Color(0xFF16201F)
private val DarkBackground = Color(0xFF101615)

private val LightColors = lightColorScheme(
    primary = LangVOrange,
    onPrimary = Color(0xFF2D1600),
    primaryContainer = LangVOrangeContainer,
    onPrimaryContainer = Color(0xFF6A2A00),
    secondary = LangVTeal,
    onSecondary = Color.White,
    secondaryContainer = LangVTealContainer,
    onSecondaryContainer = Color(0xFF00443D),
    tertiary = LangVTeal,
    onTertiary = Color.White,
    tertiaryContainer = LangVTealContainer,
    onTertiaryContainer = Color(0xFF00443D),
    background = WarmWhite,
    surface = Color.White,
    surfaceVariant = Color(0xFFF3F5F4),
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = MutedInk,
    outline = Color(0xFFB8C4C2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB45E),
    onPrimary = Color(0xFF3D1E00),
    primaryContainer = Color(0xFF743A00),
    onPrimaryContainer = Color(0xFFFFDDB8),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF005047),
    onSecondaryContainer = Color(0xFFA6F2E9),
    tertiary = LangVTealLight,
    onTertiary = Color(0xFF003731),
    tertiaryContainer = Color(0xFF005047),
    onTertiaryContainer = Color(0xFFA6F2E9),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = Color(0xFF253331),
    onBackground = Color(0xFFE6F0EE),
    onSurface = Color(0xFFE6F0EE),
    onSurfaceVariant = Color(0xFFB9C9C6),
    outline = Color(0xFF71827F),
)

private val LangVShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun EnglishCoachTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = LangVShapes,
        content = content,
    )
}

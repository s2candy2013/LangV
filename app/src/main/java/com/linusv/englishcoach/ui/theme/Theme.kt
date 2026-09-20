package com.linusv.englishcoach.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Purple = Color(0xFF7C3AED)
private val PurpleLight = Color(0xFFA78BFA)
private val Cyan = Color(0xFF0891B2)

private val LightColors = lightColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    secondary = PurpleLight,
    tertiary = Cyan,
    background = Color(0xFFFAF5FF),
    surface = Color.White,
    onBackground = Color(0xFF1E1B4B),
    onSurface = Color(0xFF1E1B4B),
)

private val DarkColors = darkColorScheme(
    primary = PurpleLight,
    secondary = Color(0xFFC4B5FD),
    tertiary = Color(0xFF67E8F9),
    background = Color(0xFF120D1F),
    surface = Color(0xFF211936),
    onBackground = Color(0xFFF5F3FF),
    onSurface = Color(0xFFF5F3FF),
)

@Composable
fun EnglishCoachTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}

package com.supgarou.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.supgarou.app.model.Team
import com.supgarou.app.model.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF2C14E),
    onPrimary = Color(0xFF2B2000),
    primaryContainer = Color(0xFF4F3F12),
    onPrimaryContainer = Color(0xFFFFE08A),
    secondary = Color(0xFFB4A7FF),
    onSecondary = Color(0xFF1D1240),
    secondaryContainer = Color(0xFF342B5E),
    onSecondaryContainer = Color(0xFFE3DDFF),
    tertiary = Color(0xFF7FD6C2),
    onTertiary = Color(0xFF00382F),
    tertiaryContainer = Color(0xFF0F4D42),
    onTertiaryContainer = Color(0xFFA6F2DF),
    background = Color(0xFF13111A),
    onBackground = Color(0xFFE8E3F0),
    surface = Color(0xFF17151F),
    onSurface = Color(0xFFE8E3F0),
    surfaceVariant = Color(0xFF2A2634),
    onSurfaceVariant = Color(0xFFC4BDD2),
    surfaceContainerLowest = Color(0xFF0F0D15),
    surfaceContainerLow = Color(0xFF1A1722),
    surfaceContainer = Color(0xFF1E1B27),
    surfaceContainerHigh = Color(0xFF26222F),
    surfaceContainerHighest = Color(0xFF302B3A),
    outline = Color(0xFF8C8599),
    outlineVariant = Color(0xFF3F3A4A),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3A0000),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF7A5900),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDF9E),
    onPrimaryContainer = Color(0xFF261A00),
    secondary = Color(0xFF5B4FA8),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5DEFF),
    onSecondaryContainer = Color(0xFF170A5E),
    tertiary = Color(0xFF00695A),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1C1A22),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1C1A22),
    surfaceVariant = Color(0xFFE8E2EE),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F2FA),
    surfaceContainer = Color(0xFFF1EDF5),
    surfaceContainerHigh = Color(0xFFEBE7F0),
    surfaceContainerHighest = Color(0xFFE5E1EA),
    outline = Color(0xFF7A7581),
    outlineVariant = Color(0xFFCBC5D1),
)

object TeamColors {
    val village = Color(0xFF8E9AAF)
    val wolves = Color(0xFFE5484D)
    val solo = Color(0xFF3DD68C)
    val unknown = Color(0xFF5E586B)
}

fun teamColor(team: Team?): Color = when (team) {
    Team.VILLAGE -> TeamColors.village
    Team.WOLVES -> TeamColors.wolves
    Team.SOLO -> TeamColors.solo
    null -> TeamColors.unknown
}

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun SupGarouTheme(mode: ThemeMode, textScale: Float, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * textScale)) {
        MaterialTheme(colorScheme = if (isDarkTheme(mode)) DarkColors else LightColors, content = content)
    }
}

package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TacticalDarkColorScheme = darkColorScheme(
    primary = TechCyan,
    onPrimary = Color.White,
    secondary = TechTeal,
    onSecondary = Color(0xFF070B19),
    tertiary = SignalGreen,
    background = SlateDarkBg,
    onBackground = TextPrimary,
    surface = CardSlate,
    onSurface = TextPrimary,
    surfaceVariant = DividerColor,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor
)

private val TacticalLightColorScheme = lightColorScheme(
    primary = OutgoingBubble, // Premium Indigo
    onPrimary = Color.White,
    secondary = Color(0xFF2563EB), // Blue Connect
    onSecondary = Color.White,
    tertiary = Color(0xFF10B981), // Emerald Success
    background = LightBentoBg, // #F3F4F9 Soft Bento Background
    onBackground = Color(0xFF1E293B), // Navy/Indigo Dark slate text
    surface = Color.White,
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFE2E8F0)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Always keep dynamicColor set up beautifully but respect tactile custom styling
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TacticalDarkColorScheme else TacticalLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

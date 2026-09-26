/**
 * Farbpalette der App. Angelehnt an das Logbot-Branding (dunkles Navy + Blau-Akzent).
 * Wird in Phase 3/Branding ggf. durch Server-Branding-Farben überschrieben.
 */
package de.phytech.logbot.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Markenfarben
val LogbotBlue = Color(0xFF2D7FF9)
val LogbotBlueDark = Color(0xFF1A5FD0)
val LogbotNavy = Color(0xFF000820)

// Light
val LightPrimary = LogbotBlue
val LightOnPrimary = Color(0xFFFFFFFF)
val LightBackground = Color(0xFFFBFCFF)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF1A1C1E)

// Dark
val DarkPrimary = Color(0xFF8FB8FF)
val DarkOnPrimary = Color(0xFF002E69)
val DarkBackground = LogbotNavy
val DarkSurface = Color(0xFF10182E)
val DarkOnSurface = Color(0xFFE2E2E6)

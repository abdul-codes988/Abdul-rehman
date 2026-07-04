package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

object ThemeConfig {
    var isBlackTheme by mutableStateOf(false)
}

// --- Dynamic Theme Colors ---
val DeepDarkBlue: Color get() = if (ThemeConfig.isBlackTheme) Color(0xFF000000) else Color(0xFFFEF7FF)     // App Background
val CardSlateBlue: Color get() = if (ThemeConfig.isBlackTheme) Color(0xFF121212) else Color(0xFFFFFFFF)     // Card/Container Background
val BorderSlate: Color get() = if (ThemeConfig.isBlackTheme) Color.Transparent else Color(0xFFE6E1E5)       // Borders

val SiriCyan: Color get() = if (ThemeConfig.isBlackTheme) Color(0xFFFFFFFF) else Color(0xFF6750A4)          // M3 Purple Accent Primary
val SiriPurple: Color get() = if (ThemeConfig.isBlackTheme) Color(0xFFE5E5EA) else Color(0xFF8B5CF6)        // Purple Accent Secondary
val SiriIndigo: Color get() = if (ThemeConfig.isBlackTheme) Color(0xFF8E8E93) else Color(0xFF4F46E5)        // Indigo Accent
val SiriGreen: Color get() = if (ThemeConfig.isBlackTheme) Color(0xFFFFFFFF) else Color(0xFF16A34A)         // Green Accent
val SiriYellow: Color get() = if (ThemeConfig.isBlackTheme) Color(0xFFD1D1D6) else Color(0xFFD97706)        // Yellow Accent
val SiriRed: Color get() = if (ThemeConfig.isBlackTheme) Color(0xFFFFFFFF) else Color(0xFFB3261E)           // Red Accent

val TextPrimary: Color get() = if (ThemeConfig.isBlackTheme) Color(0xFFFFFFFF) else Color(0xFF1D1B20)       // High-Contrast Text
val TextSecondary: Color get() = if (ThemeConfig.isBlackTheme) Color(0xFF8E8E93) else Color(0xFF49454F)     // Secondary Text

// Support for warning custom blocks
val WarningLightBg: Color get() = if (ThemeConfig.isBlackTheme) Color(0xFF1C1C1E) else Color(0xFFF2B8B5)
val WarningDarkText: Color get() = if (ThemeConfig.isBlackTheme) Color(0xFFFFFFFF) else Color(0xFF601410)

// Legacy Material 3 theme fallback colors
val Purple80: Color get() = SiriCyan
val PurpleGrey80: Color get() = SiriIndigo
val Pink80: Color get() = SiriPurple

val Purple40: Color get() = SiriIndigo
val PurpleGrey40: Color get() = SiriPurple
val Pink40: Color get() = SiriRed

package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ProfessionalColorScheme = lightColorScheme(
    primary = SiriCyan,
    secondary = SiriIndigo,
    tertiary = SiriPurple,
    background = DeepDarkBlue,
    surface = CardSlateBlue,
    onPrimary = Color.White,
    onSecondary = TextPrimary,
    onTertiary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Set to false to force our custom polished branding
  content: @Composable () -> Unit,
) {
  val isBlack = ThemeConfig.isBlackTheme
  val colorScheme = darkColorScheme(
      primary = SiriCyan,
      secondary = SiriIndigo,
      tertiary = SiriPurple,
      background = DeepDarkBlue,
      surface = CardSlateBlue,
      onPrimary = if (isBlack) Color.Black else Color.White,
      onSecondary = TextPrimary,
      onTertiary = TextPrimary,
      onBackground = TextPrimary,
      onSurface = TextPrimary
  )

  MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
  )
}

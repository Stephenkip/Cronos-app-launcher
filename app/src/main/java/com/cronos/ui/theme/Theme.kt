package com.cronos.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.cronos.data.AccentMode

private val BlackBackground = Color(0xFF000000)
private val TextPrimary = Color(0xFFEDEDED)
private val TextSecondary = Color(0xFF8A8A8A)

@Composable
fun CronosTheme(
    accentColorMode: AccentMode = AccentMode.SYSTEM,
    followSystemFontSize: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val scheme: ColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) darkColorScheme() else lightColorScheme()
    }

    val adjustedScheme = scheme.copy(
        primary = if (accentColorMode == AccentMode.FIXED) Color(0xFF3F8CFF) else scheme.primary,
        background = BlackBackground,
        surface = BlackBackground,
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        onSurfaceVariant = TextSecondary,
        secondary = TextSecondary,
        tertiary = TextSecondary,
    )

    val baseSize = if (followSystemFontSize) 1f else 0.9f
    val typography = Typography(
        bodySmall = TextStyle(fontSize = (12f * baseSize).sp, color = TextPrimary),
        bodyMedium = TextStyle(fontSize = (12f * baseSize).sp, color = TextPrimary),
        bodyLarge = TextStyle(fontSize = (12f * baseSize).sp, color = TextPrimary),
        labelSmall = TextStyle(fontSize = (12f * baseSize).sp, color = TextPrimary),
        labelMedium = TextStyle(fontSize = (12f * baseSize).sp, color = TextPrimary),
        titleMedium = TextStyle(fontSize = (12f * baseSize).sp, color = TextPrimary),
    )

    MaterialTheme(
        colorScheme = adjustedScheme,
        typography = typography,
        content = content,
    )
}

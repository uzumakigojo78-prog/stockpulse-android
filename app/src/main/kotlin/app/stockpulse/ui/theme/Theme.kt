package app.stockpulse.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import app.stockpulse.data.AppFont
import app.stockpulse.data.AppSettings
import app.stockpulse.data.ThemeMode

fun AppFont.family(): FontFamily = when (this) {
    AppFont.DEFAULT -> FontFamily.Default
    AppFont.SERIF -> FontFamily.Serif
    AppFont.MONOSPACE -> FontFamily.Monospace
    AppFont.CURSIVE -> FontFamily.Cursive
}

@Composable
fun isDarkTheme(settings: AppSettings): Boolean = when (settings.theme) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

private fun Typography.withFont(f: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = f),
    displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f),
    headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f),
    titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f),
    bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f),
    labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
    displayLargeEmphasized = displayLargeEmphasized.copy(fontFamily = f),
    displayMediumEmphasized = displayMediumEmphasized.copy(fontFamily = f),
    displaySmallEmphasized = displaySmallEmphasized.copy(fontFamily = f),
    headlineLargeEmphasized = headlineLargeEmphasized.copy(fontFamily = f),
    headlineMediumEmphasized = headlineMediumEmphasized.copy(fontFamily = f),
    headlineSmallEmphasized = headlineSmallEmphasized.copy(fontFamily = f),
    titleLargeEmphasized = titleLargeEmphasized.copy(fontFamily = f),
    titleMediumEmphasized = titleMediumEmphasized.copy(fontFamily = f),
    titleSmallEmphasized = titleSmallEmphasized.copy(fontFamily = f),
    bodyLargeEmphasized = bodyLargeEmphasized.copy(fontFamily = f),
    bodyMediumEmphasized = bodyMediumEmphasized.copy(fontFamily = f),
    bodySmallEmphasized = bodySmallEmphasized.copy(fontFamily = f),
    labelLargeEmphasized = labelLargeEmphasized.copy(fontFamily = f),
    labelMediumEmphasized = labelMediumEmphasized.copy(fontFamily = f),
    labelSmallEmphasized = labelSmallEmphasized.copy(fontFamily = f),
)

@Composable
fun StockPulseTheme(settings: AppSettings = AppSettings(), content: @Composable () -> Unit) {
    val dark = isDarkTheme(settings)
    val context = LocalContext.current
    val colors: ColorScheme = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> expressiveLightColorScheme()
    }
    val typography = remember(settings.font) { Typography().withFont(settings.font.family()) }
    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        typography = typography,
        content = content,
    )
}

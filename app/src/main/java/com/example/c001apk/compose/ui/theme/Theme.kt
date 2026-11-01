package com.example.c001apk.compose.ui.theme

import android.app.Activity
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.c001apk.compose.ThemeType
import com.example.c001apk.compose.util.FontManager
import java.io.File

/**
 * iOS 26 inspired static light / dark palettes.
 *
 * The app intentionally no longer uses Material You / wallpaper-derived colors.
 * The palette follows Apple's system grouped background, label, separator and
 * system-blue accent colors so the existing Compose screens can share one
 * consistent iOS-like look.
 */
private val IosLightColorScheme = lightColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E9FF),
    onPrimaryContainer = Color(0xFF003A66),
    secondary = Color(0xFF5E5CE6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E4FF),
    onSecondaryContainer = Color(0xFF1B1A4A),
    tertiary = Color(0xFFFF9F0A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE8C2),
    onTertiaryContainer = Color(0xFF4A2A00),
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF3C3C43),
    surfaceTint = Color(0xFF0A84FF),
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFF0A84FF),
    error = Color(0xFFFF3B30),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFFC6C6C8),
    outlineVariant = Color(0xFFD1D1D6),
    scrim = Color(0x66000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE5E5EA),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF2F2F7),
    surfaceContainerHighest = Color(0xFFE5E5EA),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
)

private val IosDarkColorScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF004A99),
    onPrimaryContainer = Color(0xFFD6E9FF),
    secondary = Color(0xFF5E5CE6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2D2C6B),
    onSecondaryContainer = Color(0xFFE5E4FF),
    tertiary = Color(0xFFFF9F0A),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF5C3A00),
    onTertiaryContainer = Color(0xFFFFE8C2),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFEBEBF5),
    surfaceTint = Color(0xFF0A84FF),
    inverseSurface = Color(0xFFF2F2F7),
    inverseOnSurface = Color(0xFF1C1C1E),
    inversePrimary = Color(0xFF0A84FF),
    error = Color(0xFFFF453A),
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF38383A),
    outlineVariant = Color(0xFF48484A),
    scrim = Color(0xCC000000),
    surfaceBright = Color(0xFF3A3A3C),
    surfaceDim = Color(0xFF000000),
    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF3A3A3C),
    surfaceContainerLow = Color(0xFF1C1C1E),
    surfaceContainerLowest = Color(0xFF000000),
)

/** iOS-like rounded geometry used by cards, sheets, buttons and the glass bar. */
val IosShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(30.dp),
)

@Composable
fun C001apkComposeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeType: ThemeType = ThemeType.Default,
    seedColor: String? = null,
    materialYou: Boolean = true,
    pureBlack: Boolean = false,
    paletteStyle: Int = 0,
    fontScale: Float = 1.00f,
    contentScale: Float = 1.00f,
    content: @Composable () -> Unit,
) {
    // Kept in the signature for compatibility with existing preferences, but
    // the visual system is now a fixed iOS 26 light / dark palette.
    @Suppress("UNUSED_EXPRESSION")
    themeType
    @Suppress("UNUSED_EXPRESSION")
    seedColor
    @Suppress("UNUSED_EXPRESSION")
    materialYou
    @Suppress("UNUSED_EXPRESSION")
    pureBlack
    @Suppress("UNUSED_EXPRESSION")
    paletteStyle

    val colorScheme = if (darkTheme) IosDarkColorScheme else IosLightColorScheme

    // 全局自定义字体：设置了本地字体则用它重建 Typography，否则用系统默认
    val fontPath = FontManager.currentPath
    val typography = if (fontPath != null) {
        val family = runCatching {
            FontFamily(Font(File(fontPath), FontWeight.Normal, FontStyle.Normal))
        }.getOrNull()
        if (family != null) buildTypography(family) else Typography
    } else Typography

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            if (SDK_INT >= Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars =
                !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = IosShapes,
        content = {
            CompositionLocalProvider(
                LocalDensity provides Density(
                    LocalDensity.current.density * contentScale,
                    LocalDensity.current.fontScale * fontScale,
                )
            ) {
                content()
            }
        }
    )
}

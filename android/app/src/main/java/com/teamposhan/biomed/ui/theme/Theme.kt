package com.teamposhan.biomed.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = PoshanTeal,
    onPrimary = Color.White,
    primaryContainer = PoshanTealSoft,
    onPrimaryContainer = PoshanTealDeep,
    inversePrimary = DarkTeal,
    secondary = PoshanAmber,
    onSecondary = Color.White,
    secondaryContainer = PoshanAmberSoft,
    onSecondaryContainer = Color(0xFF3A2200),
    tertiary = PoshanViolet,
    onTertiary = Color.White,
    tertiaryContainer = PoshanVioletSoft,
    onTertiaryContainer = Color(0xFF1E1557),
    error = RiskPriority,
    onError = Color.White,
    errorContainer = RiskPrioritySoft,
    onErrorContainer = Color(0xFF4A0A08),
    background = PoshanSand,
    onBackground = PoshanInk,
    surface = PoshanSand,
    onSurface = PoshanInk,
    surfaceVariant = PoshanSandDeep,
    onSurfaceVariant = PoshanInkSoft,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F4EF),
    surfaceContainer = Color(0xFFF2EEE7),
    surfaceContainerHigh = Color(0xFFEDE8E0),
    surfaceContainerHighest = Color(0xFFE7E1D8),
    outline = PoshanOutline,
    outlineVariant = Color(0xFFE2DED6),
    scrim = Color(0xFF000000),
)

private val DarkScheme = darkColorScheme(
    primary = DarkTeal,
    onPrimary = Color(0xFF00382E),
    primaryContainer = DarkTealContainer,
    onPrimaryContainer = PoshanTealSoft,
    secondary = DarkAmber,
    onSecondary = Color(0xFF452A00),
    secondaryContainer = DarkAmberContainer,
    onSecondaryContainer = PoshanAmberSoft,
    tertiary = DarkViolet,
    onTertiary = Color(0xFF231A66),
    tertiaryContainer = DarkVioletContainer,
    onTertiaryContainer = PoshanVioletSoft,
    error = RiskPriorityDark,
    onError = Color(0xFF5F1310),
    errorContainer = Color(0xFF8C1D19),
    onErrorContainer = RiskPrioritySoft,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkBackground,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkOnSurfaceSoft,
    surfaceContainerLowest = Color(0xFF080E0D),
    surfaceContainerLow = DarkSurface,
    surfaceContainer = Color(0xFF18201E),
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = Color(0xFF243130),
    outline = Color(0xFF6D7A76),
    outlineVariant = Color(0xFF35423F),
)

@Immutable
data class RiskColors(
    val low: Color,
    val monitor: Color,
    val review: Color,
    val priority: Color,
    val lowSoft: Color,
    val monitorSoft: Color,
    val reviewSoft: Color,
    val prioritySoft: Color,
)

@Immutable
data class AccentColors(
    val mint: Color,
    val amber: Color,
    val coral: Color,
    val violet: Color,
)

val LocalRiskColors = staticCompositionLocalOf {
    RiskColors(RiskLow, RiskMonitor, RiskReview, RiskPriority, RiskLowSoft, RiskMonitorSoft, RiskReviewSoft, RiskPrioritySoft)
}

val LocalAccentColors = staticCompositionLocalOf {
    AccentColors(AccentMint, AccentAmber, AccentCoral, AccentViolet)
}

/** Colour scheme used by the Material expressive theme. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PoshanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val risk = if (darkTheme) {
        RiskColors(RiskLowDark, RiskMonitorDark, RiskReviewDark, RiskPriorityDark,
            RiskLowSoft.copy(alpha = 0.18f), RiskMonitorSoft.copy(alpha = 0.18f),
            RiskReviewSoft.copy(alpha = 0.18f), RiskPrioritySoft.copy(alpha = 0.18f))
    } else {
        RiskColors(RiskLow, RiskMonitor, RiskReview, RiskPriority, RiskLowSoft, RiskMonitorSoft, RiskReviewSoft, RiskPrioritySoft)
    }
    val accents = if (darkTheme) {
        AccentColors(
            AccentMint.copy(alpha = 0.14f),
            AccentAmber.copy(alpha = 0.14f),
            AccentCoral.copy(alpha = 0.14f),
            AccentViolet.copy(alpha = 0.14f),
        )
    } else {
        AccentColors(AccentMint, AccentAmber, AccentCoral, AccentViolet)
    }

    CompositionLocalProvider(
        LocalRiskColors provides risk,
        LocalAccentColors provides accents,
    ) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = MotionScheme.expressive(),
            shapes = PoshanShapes,
            typography = poshanTypography(),
            content = content,
        )
    }
}

/** Convenience accessors. */
val riskColors: RiskColors
    @Composable get() = LocalRiskColors.current

val accentColors: AccentColors
    @Composable get() = LocalAccentColors.current

package com.teamposhan.biomed.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teamposhan.biomed.R

/**
 * Typography for a multilingual children's health app.
 *
 * Two fonts are bundled because no single family covers Latin + Devanagari + Telugu:
 *   - **Baloo 2**          — Latin and Devanagari (rounded, generous x-height, open
 *                            counters: the traits that matter for early readers and
 *                            low-literacy caregivers)
 *   - **Baloo Tammudu 2**  — Telugu
 *
 * They are stylistic siblings, so switching language does not change the app's voice.
 * The family is chosen from the active locale at composition time rather than relying on
 * per-glyph fallback, which would otherwise drop Telugu into the system font.
 */
private val BalooLatin = FontFamily(
    Font(R.font.baloo2, FontWeight.Normal),
    Font(R.font.baloo2, FontWeight.Medium),
    Font(R.font.baloo2, FontWeight.SemiBold),
    Font(R.font.baloo2, FontWeight.Bold),
)

private val BalooTelugu = FontFamily(
    Font(R.font.baloo_tammudu2, FontWeight.Normal),
    Font(R.font.baloo_tammudu2, FontWeight.Medium),
    Font(R.font.baloo_tammudu2, FontWeight.SemiBold),
    Font(R.font.baloo_tammudu2, FontWeight.Bold),
)

val Baloo = BalooLatin

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun typographyFor(family: FontFamily): Typography {
    val base = Typography()

    // The default M3 scale is tuned for consumer apps read at arm's length by young eyes.
    // This app is read by grandparents, by Anganwadi workers outdoors in bright sun, and
    // by anyone with uncorrected vision, so the whole scale is enlarged and no style is
    // allowed below 13sp. Body text lands at 17/15sp rather than 16/14sp.
    fun TextStyle.scaled(size: TextUnit, line: TextUnit, weight: FontWeight? = null) = copy(
        fontFamily = family,
        fontSize = size,
        lineHeight = line,
        fontWeight = weight ?: fontWeight,
    )

    return base.copy(
        // Expressive emphasized scale for headings; the heavier voice of M3 Expressive.
        displayLarge = base.displayLargeEmphasized.scaled(60.sp, 68.sp),
        displayMedium = base.displayMediumEmphasized.scaled(48.sp, 56.sp),
        displaySmall = base.displaySmallEmphasized.scaled(38.sp, 46.sp),
        headlineLarge = base.headlineLargeEmphasized.scaled(34.sp, 42.sp),
        headlineMedium = base.headlineMediumEmphasized.scaled(30.sp, 38.sp),
        headlineSmall = base.headlineSmallEmphasized.scaled(26.sp, 34.sp),
        titleLarge = base.titleLargeEmphasized.scaled(24.sp, 30.sp),
        titleMedium = base.titleMediumEmphasized.scaled(19.sp, 26.sp),
        titleSmall = base.titleSmall.scaled(17.sp, 24.sp, FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.scaled(17.sp, 26.sp),
        bodyMedium = base.bodyMedium.scaled(15.sp, 22.sp),
        bodySmall = base.bodySmall.scaled(14.sp, 20.sp),
        labelLarge = base.labelLargeEmphasized.scaled(16.sp, 22.sp),
        labelMedium = base.labelMedium.scaled(14.sp, 18.sp, FontWeight.Medium),
        // 13sp is the floor: smaller than this stops being readable in the field.
        labelSmall = base.labelSmall.scaled(13.sp, 17.sp, FontWeight.Medium),
    )
}

/** Locale-aware typography: Telugu in, Telugu font out; everything else uses Baloo 2. */
@Composable
fun poshanTypography(): Typography {
    val locales = LocalConfiguration.current.locales
    val language = if (locales.isEmpty) "en" else locales[0].language
    val family = if (language == "te") BalooTelugu else BalooLatin
    return remember(family) { typographyFor(family) }
}

/** Generous rounding — a signature of the expressive shape scale. */
val PoshanShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/** Corner radius used by all input fields and text fields. */
val FieldCorner = RoundedCornerShape(20.dp)

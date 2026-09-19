package com.teamposhan.biomed.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.teamposhan.biomed.domain.risk.VitaminDRiskModel
import com.teamposhan.biomed.domain.skin.SkinType
import com.teamposhan.biomed.ui.theme.riskColors
import com.teamposhan.biomed.domain.skin.SkinMeasurement
import com.teamposhan.biomed.domain.skin.SunExposureFactor
import com.teamposhan.biomed.ui.components.Gap
import com.teamposhan.biomed.ui.components.MicroIcon
import com.teamposhan.biomed.ui.components.PoshanCard
import com.teamposhan.biomed.ui.components.SectionLabel
import com.teamposhan.biomed.ui.components.StatusChip
import com.teamposhan.biomed.ui.components.WhyContent
import com.teamposhan.biomed.ui.haptics.rememberAppHaptics
import kotlin.math.roundToInt

/**
 * The skin-tone reading, or the invitation to take one.
 *
 * Shows the category and the suggested minutes rather than the raw angle, because the angle
 * means nothing to a caregiver and a bare number invites being read as a score.
 */
@Composable
internal fun SkinToneCard(ita: Double?, onMeasure: () -> Unit) {
    val haptics = rememberAppHaptics()
    val usable = ita?.takeIf { !it.isNaN() }
    val guidance = usable?.let { SunExposureFactor.forIta(it) }

    PoshanCard(
        onClick = { haptics.click(); onMeasure() },
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Colorize,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    guidance?.category ?: "Measure skin tone",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Gap(2)
                Text(
                    if (usable == null) {
                        "Uses the camera and a grey card. Takes about half a minute."
                    } else {
                        "About ${SunExposureFactor.suggestedMinutes(usable)} minutes of " +
                            "midday sun, arms and legs uncovered."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (usable != null) {
                StatusChip("${usable.roundToInt()}°", MaterialTheme.colorScheme.primary)
            }
        }
        if (guidance != null) {
            Gap(10)
            Text(guidance.message, style = MaterialTheme.typography.bodyMedium)
            Gap(8)
            Text(
                "Tap to measure again",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * The working, shown with this child's own numbers.
 *
 * Every module explains its arithmetic on the About page; this one also shows the
 * intermediate values for the specific reading, because unlike a questionnaire score the
 * inputs here are invisible — nobody can check a colour measurement by remembering what
 * they answered.
 */
@Composable
internal fun SkinCalculationSection(ita: Double, measurement: SkinMeasurement?) {
    val guidance = SunExposureFactor.forIta(ita)

    SectionLabel("How this was calculated")
    Gap(8)

    if (measurement != null) {
        val m = measurement
        CalcCard(
            title = "1. Which pixels were used",
            formula = "kept ${m.rawPatch.keptPixels} of ${m.rawPatch.totalPixels}\n" +
                "dropped as shine     ${m.rawPatch.rejectedSpecular}\n" +
                "dropped as not skin  ${m.rawPatch.rejectedNonSkin}\n" +
                "middle colour = R ${m.rawPatch.r}  G ${m.rawPatch.g}  B ${m.rawPatch.b}",
            explanation = "Shiny spots are the light bouncing off the skin, not the skin " +
                "itself, so they are thrown away. What is left is checked to make sure it " +
                "really is skin and not hair, cloth or background. Then the middle value is " +
                "taken, so one stray hair or speck of dirt cannot change the answer.",
            source = "Shafer dichromatic reflection model 1985; Chai & Ngan skin-colour " +
                "thresholds 1999.",
        )
        CalcCard(
            title = "2. Removing the colour of the light",
            formula = if (m.usedCard) {
                "grey card looked like  R ${m.cardPatch?.r}  G ${m.cardPatch?.g}  " +
                    "B ${m.cardPatch?.b}\n" +
                    "the card is really grey, so shift it back to grey\n" +
                    "apply the same shift to the skin\n" +
                    "skin becomes  R ${m.balancedR}  G ${m.balancedG}  B ${m.balancedB}"
            } else {
                "no grey card in the picture - nothing corrected"
            },
            explanation = "A yellow bulb makes everything look yellow. The grey card is " +
                "known to have no colour of its own, so whatever colour it picks up came " +
                "from the light in the room. Taking that colour off the card takes it off " +
                "the skin too. This is what makes a reading indoors mean the same as a " +
                "reading outside.",
            source = "Von Kries colour adaptation.",
        )
        CalcCard(
            title = "3. Turning colour into numbers",
            formula = "camera colour -> true brightness -> standard colour scale\n" +
                "lightness  L* = ${"%.1f".format(m.lab.l)}\n" +
                "red-green  a* = ${"%.1f".format(m.lab.a)}   (thrown away)\n" +
                "yellow-blue b* = ${"%.1f".format(m.lab.b)}",
            explanation = "Two numbers describe the skin: how light it is, and how yellow " +
                "it is. The red number is worked out too but deliberately dropped, because " +
                "it mostly shows blood under the skin, which changes when a child cries or " +
                "is warm and has nothing to do with skin tone.",
        )
    }

    CalcCard(
        title = if (measurement != null) "4. The skin tone angle" else "The skin tone angle",
        formula = "angle = arctan((lightness − 50) ÷ yellowness)\n" +
            "      = ${"%.1f".format(ita)}°  → ${guidance.category}",
        explanation = "Lightness and yellowness are combined into one number. A higher " +
            "number means lighter skin, a lower number means darker skin. This is the " +
            "standard way skin tone is measured in research.",
        source = "Chardon, Cretois & Hourseau 1991; categories: very light above 55°, " +
            "light 41-55°, intermediate 28-41°, tan 10-28°, brown -30 to " +
            "10°, dark below -30°.",
    )
    CalcCard(
        title = if (measurement != null) "5. Time needed in the sun" else "Time needed in the sun",
        formula = "minutes = ${SunExposureFactor.BASELINE_MINUTES} × " +
            "${guidance.timeMultiplier} = ${SunExposureFactor.suggestedMinutes(ita)}",
        explanation = "The pigment that makes skin darker also blocks the sunlight the body " +
            "uses to make vitamin D. It is doing its job - it protects against burning - " +
            "but it means more time outside is needed for the same benefit. The starting " +
            "point is roughly what light skin needs around midday with arms and legs bare.",
    )
    CalcCard(
        title = "What this does NOT tell you",
        formula = "skin tone vs blood vitamin D level:  almost no link",
        explanation = "Skin tone cannot tell us whether a child's vitamin D is low or high. " +
            "Two children with exactly the same skin can have very different levels, and " +
            "the difference between them is about four times larger than anything skin tone " +
            "can predict. Only a blood test can measure vitamin D. This measurement answers " +
            "one question only: how long should this child be in the sun.",
    )
}

internal val whySkinTone = WhyContent(
    title = "How can a camera tell this?",
    plain = "It cannot see vitamin D. What it sees is how dark the skin is. The pigment " +
        "that makes skin darker blocks the same sunlight the body uses to make vitamin D. " +
        "So darker skin needs longer outside for the same benefit. That is all this " +
        "measures, and the only thing it is used for.",
    technical = "A grey card in the picture tells the app what colour the light is, so that " +
        "colour can be taken off the skin. Shiny spots and anything that is not skin are " +
        "dropped first. The remaining colour is converted to a standard scale and combined " +
        "into one angle (ITA). Auto brightness and auto colour on the camera are switched " +
        "off, otherwise the phone keeps re-correcting the picture underneath the " +
        "measurement. Nothing is sharpened or brightened - that would change the very " +
        "number being measured.",
    evidence = "Chardon et al. 1991 (the skin tone angle); Del Bino & Bernerd 2013 (angle " +
        "against measured pigment); Von Kries colour adaptation; Chai & Ngan 1999 (skin " +
        "detection).",
    limitation = "This is not a vitamin D test. Compared against real blood tests, skin " +
        "tone explains only about one twentieth of the difference between children. It " +
        "cannot say whether a child's vitamin D is low or high. Only a blood test can.",
)


/** Where module 3 currently is. */
internal enum class VitDStage { INTRO, HELP, CAPTURE_GUIDE, CAPTURE, FORM }

/**
 * A permanent way back into the walkthrough.
 *
 * Sits at the top of the form rather than behind a menu. The guidance is only read once
 * unprompted, and the person doing the measuring is often not the person who read it.
 */
@Composable
internal fun HelpStripe(onOpen: () -> Unit) {
    val haptics = rememberAppHaptics()
    PoshanCard(
        onClick = { haptics.click(); onOpen() },
        container = MaterialTheme.colorScheme.secondaryContainer,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MicroIcon(
                artKey = "micro_how_it_works",
                fallback = Icons.Filled.MenuBook,
                size = 22,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Show me how, step by step",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    "Pictures for every step. No medical words.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

/**
 * The module's headline state, with the reasons that produced it.
 *
 * Titled "Sunlight and drops", never "vitamin D level". The distinction is the whole point:
 * this says whether the child is getting what protects them, which the app can determine,
 * rather than what is in their blood, which it cannot.
 */
@Composable
internal fun RiskTierCard(result: VitaminDRiskModel.Result) {
    val haptics = rememberAppHaptics()
    LaunchedEffect(result.tier) {
        haptics.severity(
            when (result.tier) {
                VitaminDRiskModel.Tier.LOW -> 0
                VitaminDRiskModel.Tier.MODERATE -> 1
                VitaminDRiskModel.Tier.HIGH -> 2
            }
        )
    }
    val tint = when (result.tier) {
        VitaminDRiskModel.Tier.LOW -> riskColors.low
        VitaminDRiskModel.Tier.MODERATE -> riskColors.monitor
        VitaminDRiskModel.Tier.HIGH -> riskColors.review
    }

    PoshanCard(container = tint.copy(alpha = 0.12f), contentPadding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    result.tier.label,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = tint,
                )
                // The subject sits directly under the word at readable size. "High" alone
                // would be read as a vitamin D level; it is a level of risk.
                Text(
                    result.subject,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RiskDial(tier = result.tier, tint = tint)
        }

        Gap(10)
        Text(result.tier.plain, style = MaterialTheme.typography.bodyLarge)

        Gap(8)
        // The raw count, shown against this child's own ceiling. Without the denominator
        // "9" is meaningless, and with it the tier is checkable rather than a black box.
        Text(
            "${result.points} of ${result.maxPoints} risk points that could apply to this child",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (result.present.isNotEmpty()) {
            Gap(14)
            Text(
                "What is adding to the risk",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Gap(6)
            result.present.sortedByDescending { it.points }.forEach { c ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                    Text("•  ", color = tint)
                    Column(Modifier.weight(1f)) {
                        Text(c.label, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            c.plain,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (result.skinTypeMissing) {
            Gap(12)
            Text(
                "No skin reading was taken. Skin type is the biggest single factor here, so " +
                    "this score is likely an underestimate.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Gap(12)
        Text(
            result.disclaimer,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A three-step dial: Low, Moderate, High.
 *
 * Readable with no text at all, which is the point — a caregiver who cannot read the label
 * can still see which of three positions the marker is in, and that it is a scale of three
 * rather than a score out of a hundred.
 */
@Composable
private fun RiskDial(tier: VitaminDRiskModel.Tier, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        listOf(
            VitaminDRiskModel.Tier.HIGH,
            VitaminDRiskModel.Tier.MODERATE,
            VitaminDRiskModel.Tier.LOW,
        ).forEach { step ->
            val active = step == tier
            Box(
                Modifier
                    .padding(vertical = 3.dp)
                    .size(width = 34.dp, height = if (active) 14.dp else 9.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        if (active) tint else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
            )
        }
    }
}

/**
 * The skin typing result on its own — what was measured, as opposed to what was asked.
 *
 * Separated from the risk card because these are different kinds of claim. The type is a
 * measurement with a published basis; the risk tier is an assumption-laden model built on top
 * of it. Collapsing them into one card would let the model borrow the measurement's
 * credibility.
 */
@Composable
internal fun SkinTypeCard(ita: Double, type: SkinType) {
    val margin = SkinType.marginToEdge(ita)
    val borderline = margin < SkinType.BORDERLINE_MARGIN

    PoshanCard(contentPadding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Skin type ${type.roman}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${type.itaLabel} · ${type.plain}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip("${ita.roundToInt()}°", MaterialTheme.colorScheme.primary)
        }
        Gap(10)
        Text(
            "About ${SunExposureFactor.suggestedMinutes(ita)} minutes of midday sun, arms " +
                "and legs uncovered — roughly ${type.exposureMultiplier}x what the lightest " +
                "skin needs for the same benefit.",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (borderline) {
            Gap(10)
            // The reading's own error budget is 3-7 degrees of ITA, so a result this close to
            // a boundary could sit either side of it. Saying so is cheaper than pretending.
            Text(
                "This reading is close to the line between two skin types, so it could go " +
                    "either way. Take it again in better light if it matters.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The explanation for the risk model itself.
 *
 * Deliberately separate from [whySkinTone]. The skin reading and the risk score are different
 * kinds of claim with different standing, and a caregiver — or a judge — asking "how did you
 * get this number" deserves to see which parts are measured and which are assumed.
 */
internal val whyRiskModel = WhyContent(
    title = "How is this risk worked out?",
    plain = "We count the things known to make low vitamin D more likely — how dark the " +
        "skin is, how much sun the child actually gets, whether the daily drops are being " +
        "given, and a few background details. More of them means higher risk. It is a way " +
        "of deciding who to pay attention to first.",
    technical = "An additive score over documented risk factors, grouped into three tiers. " +
        "Skin type comes from the camera reading (ITA angle, published category " +
        "boundaries); every other factor is caregiver-reported, except sunlight " +
        "availability, which is computed from solar altitude at your latitude and today's " +
        "date. One interaction term is included: darker skin combined with little sun " +
        "outdoors, because the two compound rather than add.",
    evidence = "Melanin absorbs UV-B over the same 290-315 nm band that makes vitamin D " +
        "(Chardon 1991; Del Bino & Bernerd 2013 for the skin-typing step). Supplementation " +
        "guidance: IAP 2021 and the 2016 Global Consensus on Nutritional Rickets.",
    limitation = "The weights are assumptions, not fitted coefficients — no cohort of " +
        "under-fives with paired skin readings and blood results exists to fit them to. So " +
        "the tiers rank children against each other; they are not percentages, and \"High\" " +
        "does not mean the child is deficient. There is currently no validated non-invasive " +
        "test for vitamin D anywhere, which is why this is a risk score and not a " +
        "measurement. Only a blood test measures vitamin D.",
)

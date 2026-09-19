package com.teamposhan.biomed.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teamposhan.biomed.BuildConfig
import com.teamposhan.biomed.domain.attention.AttentionPhase
import com.teamposhan.biomed.domain.growth.GrowthInterpretation
import com.teamposhan.biomed.domain.milestones.AgeBand
import com.teamposhan.biomed.ui.components.Gap
import com.teamposhan.biomed.ui.components.InfoBanner
import com.teamposhan.biomed.ui.components.PoshanCard
import com.teamposhan.biomed.ui.components.SectionLabel

/**
 * Every calculation the app performs, written out.
 *
 * A screening tool that produces numbers a health worker is expected to act on should be
 * able to show its working. This is that page: the formulas, the cut-offs, the scoring
 * weights and the sources, in one place, so a supervising clinician can check what the app
 * is doing without reading the source, and a field worker can answer "where did that number
 * come from?".
 *
 * Deliberately not hidden behind a developer flag. If a calculation is too embarrassing to
 * show a user, it should be fixed rather than concealed.
 */
@Composable
fun AboutScreen() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        Gap(8)
        Text("How Poshan calculates", style = MaterialTheme.typography.headlineSmall)
        Gap(4)
        Text(
            "Every number this app shows, and where it comes from.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Gap(18)

        // ---- 1. Milestones -------------------------------------------------
        SectionLabel("Developmental milestones")
        Gap(8)
        CalcCard(
            title = "Answer scoring",
            formula = "Yes = 1.0    Maybe = 0.5    No = 0    Not sure = 0 + flag",
            explanation = "Each milestone earns credit toward the domain score. \"Not sure\" " +
                "scores zero like \"No\", and additionally raises a flag: a caregiver who " +
                "cannot say whether their child does an everyday behaviour has effectively " +
                "not observed it.",
        )
        CalcCard(
            title = "Developmental quotient (DQ)",
            formula = "DQ = (developmental age ÷ chronological age) × 100",
            explanation = "Developmental age is accumulated band by band: for each age band, " +
                "the fraction of credit earned is multiplied by the width of that band in " +
                "months, and the results are summed. A DQ of 100 means the child's " +
                "developmental age matches their actual age.",
        )
        CalcCard(
            title = "DQ interpretation",
            formula = "≥ 85 on track    70–84 concern    < 70 delayed",
            explanation = "Used for the composite score only. It is never shown as a " +
                "standalone verdict.",
        )
        CalcCard(
            title = "Domain status",
            formula = "any red flag → delayed\n" +
                "any missed milestone from an earlier band → delayed\n" +
                "concerns ≥ half the current band → delayed\n" +
                "concerns ≥ 1 → some concern\n" +
                "otherwise → on track",
            explanation = "A milestone missed from a band the child has already passed is a " +
                "delay by definition: they are late for it, not merely approaching it. That " +
                "is why it outranks the threshold rule.",
        )
        CalcCard(
            title = "Catch-up screening",
            formula = "questions asked = every band with minMonths ≤ child's age",
            explanation = "A child first screened at 11 months is asked the 0–3, 4–6 and " +
                "7–9 month questions as well as their own band. Skills a child should " +
                "already have are what reveal a delay.",
            source = "Question bank: ${AgeBand.entries.size} age bands, 123 questions " +
                "(AIIMS Delhi developmental milestones chart).",
        )

        // ---- 2. Growth -----------------------------------------------------
        Gap(14)
        SectionLabel("Growth and nutrition")
        Gap(8)
        CalcCard(
            title = "WHO z-score",
            formula = "z = ((value ÷ M)^L − 1) ÷ (L × S)        when L ≠ 0\n" +
                "z = ln(value ÷ M) ÷ S                   when L = 0",
            explanation = "L, M and S are the WHO's published Box-Cox power, median and " +
                "coefficient of variation for the child's exact age in months and their sex. " +
                "S is a coefficient of variation, not an absolute standard deviation.",
            source = "WHO Child Growth Standards, per-month z-score tables for weight-for-age, " +
                "length/height-for-age, BMI-for-age and arm circumference-for-age.",
        )
        CalcCard(
            title = "Plotting a point on the chart",
            formula = "value = M × (1 + L × S × z)^(1÷L)",
            explanation = "The inverse of the same relation, used to draw each SD curve.",
        )
        CalcCard(
            title = "Weight-for-age cut-offs",
            formula = "< −3 SD severely underweight\n" +
                "−3 to −2 SD underweight\n" +
                "−2 to +2 SD normal\n" +
                "> +2 SD high weight-for-age",
        )
        CalcCard(
            title = "Length/height-for-age cut-offs",
            formula = "< −3 SD severe stunting\n" +
                "−3 to −2 SD stunting\n" +
                "−2 to −1 SD mildly low, monitor\n" +
                "−1 to +2 SD normal",
            explanation = "Length is measured lying down below 24 months and height standing " +
                "from 24 months. The WHO publishes these as two separate tables, so there is " +
                "a small genuine step in the curves at 24 months.",
        )
        CalcCard(
            title = "BMI-for-age cut-offs",
            formula = "< −3 SD severe wasting\n" +
                "−3 to −2 SD wasting\n" +
                "> +1 SD overweight (≈85th percentile)\n" +
                "> +1.645 SD obesity (≈95th percentile)",
        )
        CalcCard(
            title = "MUAC (mid-upper arm circumference)",
            formula = "< ${GrowthInterpretation.MUAC_SAM_MM.toInt()} mm severe acute malnutrition\n" +
                "${GrowthInterpretation.MUAC_SAM_MM.toInt()}–${GrowthInterpretation.MUAC_MAM_MM.toInt()} mm moderate acute malnutrition\n" +
                "≥ ${GrowthInterpretation.MUAC_MAM_MM.toInt()} mm normal",
            explanation = "Fixed thresholds, not age-adjusted. Applies between " +
                "${GrowthInterpretation.MUAC_MIN_AGE_MONTHS.toInt()} and " +
                "${GrowthInterpretation.MUAC_MAX_AGE_MONTHS.toInt()} months only.",
        )
        CalcCard(
            title = "Percentile",
            formula = "P = Φ(z) × 100",
            explanation = "The normal cumulative distribution, computed with the Zelen & " +
                "Severo approximation.",
        )

        // ---- 3. Attention --------------------------------------------------
        Gap(14)
        SectionLabel("Attention and gaze")
        Gap(8)
        CalcCard(
            title = "Age phases",
            formula = AttentionPhase.entries.joinToString("\n") {
                "${it.ageLabel.padEnd(18)} ${it.label}${if (it.scored) "" else "  (not scored)"}"
            },
            explanation = "The tasks change with age because the meaning of a looking " +
                "measurement changes with age. Two phases are observation-only: no phone " +
                "screener is validated below 12 months or above 48.",
        )
        CalcCard(
            title = "Social preference",
            formula = "ratio = social dwell ÷ (social dwell + non-social dwell)",
            explanation = "Dwell time is counted in usable frames with gaze on that half of " +
                "the screen. Both a simple-social and a complex-social pairing are run and " +
                "reported separately.",
        )
        CalcCard(
            title = "Session validity",
            formula = "trial valid  : ≥ 50% usable frames\n" +
                "session valid: ≥ 6 valid trials\n" +
                "frame usable : face found, eyes open, |yaw| ≤ 15°, |pitch| ≤ 12°",
            explanation = "Below these thresholds the session is reported as \"not " +
                "completed\" rather than as a low result. A screening tool must never " +
                "confuse \"the child did not look\" with \"the tracker failed\".",
        )
        CalcCard(
            title = "Distance to the child",
            formula = "Z = focal length (px) × IPD (mm) ÷ observed IPD (px)",
            explanation = "Interpupillary distance is read from paediatric norms by age — " +
                "40.5 mm at 0–11 months rising to 51 mm at 4 years. Using the adult 63 mm " +
                "would overestimate distance for a 6-month-old by about 55%.",
            source = "MacLachlan & Howland (2002), 1311 subjects, 1 month to 19 years.",
        )
        CalcCard(
            title = "What is deliberately not measured",
            formula = "eye-region vs mouth looking\ngap-overlap disengagement\nsaccade velocity and amplitude",
            explanation = "The eye region subtends 2–4° and this camera's error is 2–6°. The " +
                "gap-overlap paradigm needs a target at 15° eccentricity and a phone reaches " +
                "13° at best. Saccades need ≥120 Hz sampling. These are reported as " +
                "unavailable rather than estimated.",
        )

        // ---- 4. Composite --------------------------------------------------
        Gap(14)
        SectionLabel("Overall risk")
        Gap(8)
        CalcCard(
            title = "Composite band",
            formula = "on track · monitor · review · priority",
            explanation = "Combines the developmental, growth, attention and vitamin D " +
                "outcomes. Any single severe finding — a red flag, severe stunting, SAM by " +
                "MUAC — raises the overall band on its own; they are not averaged away.",
        )

        Gap(16)
        InfoBanner(
            "This is a screening aid, not a diagnostic instrument. It is designed to help " +
                "decide whether a child should be seen by a developmental paediatrician or " +
                "an RCI-registered clinical psychologist.",
        )

        Gap(14)
        PoshanCard(contentPadding = PaddingValues(16.dp)) {
            Row {
                Text(
                    "Version",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(90.dp),
                )
                Text(
                    "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Gap(28)
    }
}

/** Shared with the vitamin D module, which shows its own calculation the same way. */
@Composable
internal fun CalcCard(
    title: String,
    formula: String,
    explanation: String? = null,
    source: String? = null,
) {
    PoshanCard(
        modifier = Modifier.padding(bottom = 10.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(0.dp))
        Gap(8)
        PoshanCard(
            container = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentPadding = PaddingValues(12.dp),
        ) {
            Text(
                formula,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
        explanation?.let {
            Gap(8)
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        source?.let {
            Gap(6)
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** The About page as spoken text, for the read-aloud button. */
fun aboutSpokenText(): String = buildString {
    appendLine("How Poshan calculates.")
    appendLine("This page lists every number the app shows and where it comes from.")
    appendLine("Milestone answers are scored: yes counts one, maybe counts a half, " +
        "no counts zero, and not sure counts zero and raises a flag.")
    appendLine("The developmental quotient is developmental age divided by actual age, " +
        "times one hundred.")
    appendLine("Growth is measured against the World Health Organization child growth " +
        "standards. A z-score of zero is exactly average, and minus two to plus two covers " +
        "about ninety five percent of healthy children.")
    appendLine("Arm circumference under one hundred and fifteen millimetres indicates " +
        "severe acute malnutrition.")
    appendLine("This is a screening aid, not a diagnosis.")
}

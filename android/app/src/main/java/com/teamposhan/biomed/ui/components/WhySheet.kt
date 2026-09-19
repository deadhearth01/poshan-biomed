package com.teamposhan.biomed.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The scientific reason behind something the app is doing, one tap away.
 *
 * Two audiences, one component. A caregiver wants to know *why they are being asked to do
 * this*; an Anganwadi worker or a reviewing clinician wants to know *what it rests on*. Both
 * are served by a short plain-language answer with the evidence underneath it, and neither
 * is served by that text sitting permanently on screen — which is why this is a sheet and
 * not a paragraph.
 *
 * Content is written to be readable by someone with no clinical training. Where a claim
 * comes from a specific finding, [WhyContent.evidence] names it; that line is the one a
 * reviewer will check.
 */
data class WhyContent(
    val title: String,
    /** Plain-language answer. Two or three sentences at most. */
    val plain: String,
    /** The technical or scientific basis. Optional — omit rather than invent one. */
    val technical: String? = null,
    /** Where the claim comes from. Named specifically enough to be checked. */
    val evidence: String? = null,
    /** Stated limits. A screening tool that hides its limits is not trustworthy. */
    val limitation: String? = null,
)

/**
 * A small "Why?" affordance. Place it next to whatever it explains — a score, a task name,
 * an instruction — rather than collecting explanations somewhere separate.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhyButton(
    content: WhyContent,
    modifier: Modifier = Modifier,
    label: String = "Why?",
) {
    var open by remember { mutableStateOf(false) }
    val haptics = com.teamposhan.biomed.ui.haptics.rememberAppHaptics()

    Row(
        modifier
            // Clip before clickable so the ripple follows the shape (docs/08 §1.1).
            .clip(MaterialTheme.shapes.small)
            .clickable { haptics.tick(); open = true }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    if (open) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            WhySheetBody(content)
        }
    }
}

@Composable
private fun WhySheetBody(content: WhyContent) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            content.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Text(content.plain, style = MaterialTheme.typography.bodyLarge)

        content.technical?.let {
            Column {
                SectionLabel("How it works")
                Spacer(Modifier.size(6.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        content.evidence?.let {
            PoshanCard(
                container = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentPadding = PaddingValues(14.dp),
            ) {
                SectionLabel("Evidence")
                Spacer(Modifier.size(6.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        content.limitation?.let {
            InfoBanner(it)
        }
    }
}

/**
 * The explanations themselves, kept in one place so the wording stays consistent wherever
 * a concept appears, and so a clinician can review them all at once.
 */
object WhyLibrary {

    val notSureIsFlagged = WhyContent(
        title = "Why does \"Not sure\" count as a concern?",
        plain = "These are everyday things a parent usually notices. If you genuinely cannot " +
            "say whether your child does one, that is worth looking into — so the app marks " +
            "it for follow-up rather than guessing on your behalf.",
        technical = "\"Not sure\" scores 0 like \"No\", and additionally raises a flag. It is " +
            "never averaged away into a middle value.",
        limitation = "A flag is not a diagnosis. It means this question is worth revisiting " +
            "with the health worker at the next visit.",
    )

    val catchUpQuestions = WhyContent(
        title = "Why am I being asked about younger ages?",
        plain = "If your child is being screened for the first time at 11 months, we still " +
            "ask about the 4-month and 7-month skills. Skills a child should already have " +
            "are exactly what tells us whether they are on track.",
        technical = "Every band up to and including the child's own is asked. A milestone " +
            "missed from a band the child has already passed marks that domain as delayed " +
            "outright, rather than as a borderline concern.",
    )

    val growthZScore = WhyContent(
        title = "What is an SD or z-score?",
        plain = "It says how far your child sits from the middle of healthy children the " +
            "same age and sex. 0 is exactly average. Between -2 and +2 covers about 95% of " +
            "healthy children.",
        technical = "Computed from the WHO Child Growth Standards using the published L, M " +
            "and S values for each month: z = ((value/M)^L − 1) / (L·S).",
        evidence = "WHO Child Growth Standards, per-month z-score tables for weight-for-age, " +
            "length/height-for-age, BMI-for-age and arm circumference-for-age.",
    )

    val muacCutoffs = WhyContent(
        title = "Why measure the upper arm?",
        plain = "Mid-upper arm circumference is a quick, reliable way to spot acute " +
            "malnutrition in children between 6 months and 5 years, using a simple tape.",
        technical = "Under 115 mm indicates severe acute malnutrition; 115–125 mm moderate; " +
            "125 mm and above normal. These are fixed cut-offs, not age-adjusted.",
        limitation = "Applies only between 6 and 59 months.",
    )

    val gazeNotDiagnosis = WhyContent(
        title = "What does the camera task actually tell us?",
        plain = "It records where your child looks and for how long. Children differ a great " +
            "deal, so one session is a snapshot, not a verdict. What matters most is how it " +
            "changes between visits.",
        technical = "The camera tracks face landmarks on-device to estimate looking " +
            "direction at a screen-half level. No video is stored or sent anywhere.",
        evidence = "The social-versus-geometric looking paradigm is the most consistent " +
            "finding across the eye-tracking screening literature.",
        limitation = "This is a screening aid, not a diagnosis. A phone camera cannot " +
            "resolve fine detail such as looking at the eyes versus the mouth, so those are " +
            "not reported.",
    )

    val observationOnlyPhase = WhyContent(
        title = "Why is there no score for my baby?",
        plain = "Below one year, there is not yet good enough evidence for this kind of " +
            "phone-camera screening to give a trustworthy score. What we record today " +
            "becomes the baseline that later visits are compared against.",
        technical = "Validated phone-based screeners start at 12–16 months. Below that, the " +
            "published infant signal is a decline in eye-region looking, which needs " +
            "specialist equipment this phone cannot match.",
        evidence = "Jones & Klin (2013) measured eye-region fixation with an infrared " +
            "tracker; the eye region on a phone is smaller than this camera's error.",
    )

    val sessionNotCompleted = WhyContent(
        title = "Why did it say \"not completed\"?",
        plain = "The camera could not follow your child well enough for the result to mean " +
            "anything — too dark, too much movement, or your child looked away for most of " +
            "it. That is reported honestly instead of being recorded as a low result.",
        technical = "A trial counts only when enough frames are usable, and a session counts " +
            "only with enough valid trials. Below that, no score is produced.",
        limitation = "Many sessions with young children do not produce usable data. Trying " +
            "again on a better day is normal and expected.",
    )

    val trajectoryMatters = WhyContent(
        title = "Why compare with earlier visits?",
        plain = "A change over time often says more than any single visit. A child whose " +
            "social looking is falling needs attention even if today's number looks normal.",
        evidence = "Infants later diagnosed with autism can begin with typical eye looking " +
            "that declines between 2 and 6 months; the decline carries more weight than an " +
            "initially low level.",
    )
}

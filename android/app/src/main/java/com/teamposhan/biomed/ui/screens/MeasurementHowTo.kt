package com.teamposhan.biomed.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.teamposhan.biomed.R
import com.teamposhan.biomed.ui.components.PoshanCard
import com.teamposhan.biomed.ui.components.rememberArtRes

/**
 * How to take each body measurement, in words a person can follow without training.
 *
 * Kept in one place because the same three measurements are taken in two different screens —
 * at enrolment and at every later visit — and the instructions drifting apart between them
 * is how two health workers end up measuring the same child two different ways.
 *
 * The wording was rewritten from the clinical version that shipped before. Terms like
 * "infantometer", "calibrated", "non-dominant" and "SAM/MAM" were removed: the person holding
 * the tape is often an Anganwadi worker or a parent, and an instruction that has to be
 * decoded before it can be followed is an instruction that gets skipped.
 */
data class MeasurementHowTo(
    val artKey: String,
    val fallbackArt: Int,
    val title: String,
    val steps: List<String>,
) {
    companion object {

        val weight = MeasurementHowTo(
            artKey = "guide_measure_weight",
            fallbackArt = R.drawable.illus_measure_weight,
            title = "How to weigh the child",
            steps = listOf(
                "Take off shoes, cap, sweater and anything heavy. One light layer is fine.",
                "Check the scale reads zero before the child stands or sits on it.",
                "Wait until the number stops moving, then write it down.",
            ),
        )

        /** Under two years the child is measured lying down; from two years, standing. */
        fun length(ageMonths: Float): MeasurementHowTo = if (ageMonths < 24f) {
            MeasurementHowTo(
                artKey = "guide_measure_length",
                fallbackArt = R.drawable.illus_measure_length,
                title = "How to measure length, lying down",
                steps = listOf(
                    "Lay the child flat on their back on the board.",
                    "One person holds the head gently against the top end, looking straight up.",
                    "Press the knees down flat and bring the footboard to the heels, then read.",
                ),
            )
        } else {
            MeasurementHowTo(
                artKey = "guide_measure_height",
                fallbackArt = R.drawable.illus_measure_length,
                title = "How to measure height, standing",
                steps = listOf(
                    "No shoes. Heels together, back of the head, shoulders and heels touching the wall.",
                    "Looking straight ahead, not up or down.",
                    "Bring the flat piece down onto the top of the head, then read.",
                ),
            )
        }

        val muac = MeasurementHowTo(
            artKey = "guide_measure_muac",
            fallbackArt = R.drawable.illus_measure_muac,
            title = "How to measure the arm",
            steps = listOf(
                "Use the left arm. Find the point halfway between the shoulder and the elbow.",
                "Let the arm hang loose at the side, not held up.",
                "Wrap the tape flat at that point, snug but not squeezing, then read.",
            ),
        )
    }
}

/**
 * The how-to card: a picture and three numbered lines.
 *
 * Uses generated artwork when it exists and the existing line drawing until then, so the
 * card is useful today and better later without any call site changing.
 */
@Composable
internal fun MeasurementGuideCard(guide: MeasurementHowTo) {
    val res = rememberArtRes(guide.artKey).takeIf { it != 0 } ?: guide.fallbackArt

    PoshanCard(modifier = Modifier.padding(bottom = 10.dp), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(res),
                    contentDescription = null,
                    modifier = Modifier.size(88.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(guide.title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(6.dp))
                guide.steps.forEachIndexed { i, step ->
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.Top) {
                        Text(
                            "${i + 1}.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            step,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

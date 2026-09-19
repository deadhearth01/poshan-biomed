package com.teamposhan.biomed.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teamposhan.biomed.data.model.AssessmentSession
import com.teamposhan.biomed.data.model.ChildProfile
import com.teamposhan.biomed.domain.age.AgeCalculator
import com.teamposhan.biomed.domain.report.AnswerTranscript
import com.teamposhan.biomed.ui.components.Gap
import com.teamposhan.biomed.ui.components.InfoBanner
import com.teamposhan.biomed.ui.components.PoshanCard
import com.teamposhan.biomed.ui.components.PoshanPrimaryButton
import com.teamposhan.biomed.ui.components.SectionLabel
import com.teamposhan.biomed.ui.theme.riskColors

/** Which module's transcript to show. */
enum class ModuleDetail { MILESTONES, NUTRITION, ATTENTION, VITAMIN_D }

/**
 * Every question asked, what was answered, and the result — for one module.
 *
 * Reached by tapping a module card in the report. The report itself stays a summary; this is
 * where the evidence behind it lives. Two people need it for different reasons: a health
 * worker deciding what to act on needs to see *which* behaviours were reported rather than a
 * count of concerns, and a parent returning months later needs to see what they actually
 * said last time.
 */
@Composable
fun ModuleDetailScreen(
    child: ChildProfile?,
    session: AssessmentSession?,
    module: ModuleDetail,
    onDone: () -> Unit,
) {
    if (session == null || child == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nothing recorded yet.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val age = remember(session) { session.ageMonthsAtAssessment }
    val sections = remember(session, module, age) {
        when (module) {
            ModuleDetail.MILESTONES -> AnswerTranscript.milestones(session, age)
            ModuleDetail.NUTRITION -> AnswerTranscript.nutrition(session, age)
            ModuleDetail.ATTENTION -> AnswerTranscript.markers(session, age)
            ModuleDetail.VITAMIN_D -> AnswerTranscript.vitaminD(session, age)
        }
    }

    val totalAnswered = sections.sumOf { it.answered }
    val totalFlagged = sections.sumOf { it.flagged }
    val totalNotSure = sections.sumOf { it.notSure }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
        ) {
            Text(
                when (module) {
                    ModuleDetail.MILESTONES -> "Everyday skills"
                    ModuleDetail.NUTRITION -> "Feeding"
                    ModuleDetail.ATTENTION -> "Looking and attention"
                    ModuleDetail.VITAMIN_D -> "Sunlight and vitamin D"
                },
                style = MaterialTheme.typography.headlineSmall,
            )
            Gap(4)
            Text(
                "${child.name} · ${AgeCalculator.format(age)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Gap(16)

            if (sections.isEmpty()) {
                InfoBanner("Nothing was answered in this part.")
                Gap(20)
            } else {
                PoshanCard(contentPadding = PaddingValues(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Tally("$totalAnswered", "answered")
                        Tally("$totalFlagged", "to watch", riskColors.review)
                        // Counted separately: "not sure" means unobserved, which is a
                        // different finding from "no" and is actionable in a different way.
                        Tally("$totalNotSure", "not sure", riskColors.monitor)
                    }
                }
                Gap(20)
            }

            sections.forEach { section ->
                SectionLabel(section.title)
                section.subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Gap(8)
                section.entries.forEach { entry -> EntryRow(entry) }
                Gap(18)
            }

            Gap(8)
        }
        Box(Modifier.padding(20.dp)) {
            PoshanPrimaryButton("Done", onClick = onDone)
        }
    }
}

@Composable
private fun Tally(value: String, label: String, tint: Color = MaterialTheme.colorScheme.primary) {
    Column {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = tint,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * One question and its answer.
 *
 * Unanswered questions are shown rather than hidden — a gap in the record is itself worth
 * seeing, and hiding them would make a half-finished module look complete.
 */
@Composable
private fun EntryRow(entry: AnswerTranscript.Entry) {
    val c = riskColors
    val tint = when {
        entry.notSure -> c.monitor
        entry.flagged -> c.review
        entry.answered -> c.low
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    PoshanCard(
        modifier = Modifier.padding(bottom = 8.dp),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .padding(top = 5.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(tint),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.text, style = MaterialTheme.typography.bodyMedium)
                entry.hint?.let {
                    Gap(2)
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                entry.answerLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (entry.flagged) FontWeight.Bold else FontWeight.Normal,
                color = tint,
            )
        }
        if (entry.notSure) {
            Gap(6)
            Text(
                "Flagged — the caregiver has not seen this, so it counts as not observed.",
                style = MaterialTheme.typography.labelSmall,
                color = c.monitor,
            )
        }
    }
}

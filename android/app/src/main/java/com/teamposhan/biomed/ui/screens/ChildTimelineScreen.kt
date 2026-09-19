package com.teamposhan.biomed.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teamposhan.biomed.data.model.AssessmentSession
import com.teamposhan.biomed.data.model.ChildProfile
import com.teamposhan.biomed.data.model.DomainStatus
import com.teamposhan.biomed.data.model.SessionStatus
import com.teamposhan.biomed.domain.age.AgeCalculator
import com.teamposhan.biomed.domain.growth.GrowthStandards
import com.teamposhan.biomed.data.model.ResponseType
import com.teamposhan.biomed.domain.milestones.MilestoneBank
import androidx.compose.foundation.clickable
import com.teamposhan.biomed.domain.milestones.AgeBand
import com.teamposhan.biomed.ui.components.PoshanProgress
import com.teamposhan.biomed.domain.milestones.BandCoverage
import com.teamposhan.biomed.ui.components.Gap
import com.teamposhan.biomed.ui.components.InfoBanner
import com.teamposhan.biomed.ui.components.PoshanCard
import com.teamposhan.biomed.ui.components.SectionLabel
import com.teamposhan.biomed.ui.components.StatusChip
import com.teamposhan.biomed.ui.components.WhyButton
import com.teamposhan.biomed.ui.components.WhyLibrary
import com.teamposhan.biomed.ui.theme.accentColors
import com.teamposhan.biomed.ui.theme.riskColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The child's whole record, laid out by age.
 *
 * One entry per milestone age band, each expanding to show everything recorded at that age:
 * which modules were completed, the domain outcomes, the growth measurements with their
 * z-scores, and a way through to that visit's charts and report.
 *
 * The point is that a screening record is only useful as a series. A single visit answers
 * "how is this child today"; the series answers "which way are they going", and for several
 * of the things this app screens for — growth faltering, declining social looking — the
 * direction is the finding.
 */
@Composable
fun ChildTimelineScreen(
    child: ChildProfile,
    sessions: List<AssessmentSession>,
    onOpenReport: (AssessmentSession) -> Unit,
    onOpenCharts: () -> Unit,
) {
    val ageMonths = AgeCalculator.assessmentMonths(child)
    val accents = accentColors
    val dateFormat = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }

    // Every band from birth to five years, not only the ones already reached.
    //
    // The list used to stop at the child's own age. That answered "what have we missed" but
    // not "what is coming", and a caregiver looking at their child's page wants to see the
    // whole road: nine checks from birth to five. Bands the child has not reached yet are
    // marked as upcoming rather than as gaps, so nothing reads as a failure.
    val bands = remember { AgeBand.entries.toList() }
    val currentBand = remember(ageMonths) { AgeBand.forAge(ageMonths) }

    // Answers filed under the band each QUESTION belongs to, not the band the session
    // happened in. Catch-up questions mean one visit answers several bands at once, and
    // grouping by session age left every earlier band reading "Not done yet" on the very
    // page whose job is to show the road from birth to five.
    val coverage = remember(sessions) { BandCoverage.forChild(sessions) }
    val byBand = remember(sessions) {
        sessions.groupBy { AgeBand.forAge(it.ageMonthsAtAssessment) }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        Gap(8)
        Text(child.name, style = MaterialTheme.typography.headlineMedium)
        Text(
            "${AgeCalculator.format(ageMonths)} · ${AgeBand.forAge(ageMonths).label}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Gap(16)

        // ---- growth across every visit ------------------------------------
        val measured = sessions.filter { it.growth != null }
        PoshanCard(onClick = onOpenCharts, container = accents.mint) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Growth charts", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (measured.isEmpty()) {
                            "No measurements recorded yet"
                        } else {
                            "${measured.size} measurement${if (measured.size == 1) "" else "s"} plotted"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.Filled.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Gap(22)

        // ---- what is done and what is left, for the current visit -----------
        //
        // The history below answers "what happened at past visits". This answers the more
        // immediate question a worker actually arrives with: what still needs doing today.
        val live = sessions.filter { it.status == SessionStatus.DRAFT }
            .maxByOrNull { it.startedAt }
            ?: sessions.maxByOrNull { it.startedAt }

        SectionLabel("This visit")
        Gap(8)
        CurrentVisitStatus(live)

        Gap(20)
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Assessment history")
            Spacer(Modifier.weight(1f))
            WhyButton(WhyLibrary.trajectoryMatters, label = "Why history?")
        }
        Gap(4)

        if (sessions.isEmpty()) {
            InfoBanner("No checks recorded yet. Start with the modules on the home screen.")
            Gap(12)
        }

        bands.reversed().forEach { band ->
            val bandSessions = byBand[band].orEmpty().sortedByDescending { it.startedAt }
            BandEntry(
                band = band,
                sessions = bandSessions,
                coverage = coverage[band],
                isCurrent = band == currentBand,
                // A band whose first month is still ahead of this child.
                isUpcoming = band.minMonths > ageMonths,
                dateFormat = dateFormat,
                onOpenReport = onOpenReport,
            )
            Gap(10)
        }
        Gap(24)
    }
}

@Composable
private fun BandEntry(
    band: AgeBand,
    sessions: List<AssessmentSession>,
    coverage: BandCoverage.Coverage?,
    isCurrent: Boolean,
    isUpcoming: Boolean,
    dateFormat: DateTimeFormatter,
    onOpenReport: (AssessmentSession) -> Unit,
) {
    // A band can be answered without a session of its own — that is the whole point of
    // catch-up questions — so it counts as started if either is true.
    val answered = coverage?.started == true
    var expanded by remember { mutableStateOf(isCurrent && (sessions.isNotEmpty() || answered)) }
    val c = riskColors
    val done = sessions.any { it.status == SessionStatus.COMPLETED } || coverage?.complete == true
    val inProgress = sessions.any { it.status == SessionStatus.DRAFT } ||
        (answered && coverage?.complete != true)

    val dotColor = when {
        done -> c.low
        inProgress -> c.monitor
        // Deliberately not the same grey as a missed band: a check the child is not old
        // enough for yet must not look like one that was skipped.
        isUpcoming -> MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    PoshanCard(
        onClick = { expanded = !expanded },
        container = if (isCurrent) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(12.dp).background(dotColor, CircleShape),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        band.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    )
                    if (isCurrent) {
                        Spacer(Modifier.width(8.dp))
                        StatusChip("Now", MaterialTheme.colorScheme.primary)
                    } else if (isUpcoming) {
                        Spacer(Modifier.width(8.dp))
                        StatusChip("Later", MaterialTheme.colorScheme.outline)
                    }
                }
                Text(
                    when {
                        // Order matters: "coming up" must win over "not done", otherwise a
                        // one-year-old's page shows six checks as missing.
                        isUpcoming && !answered -> "Coming up at ${band.minMonths} months"
                        // Answered as catch-up, with no session of its own. This is the case
                        // that previously read "Not done yet" despite being fully answered.
                        answered && sessions.isEmpty() && coverage != null ->
                            "${coverage.answered} of ${coverage.total} answered"
                        !answered && sessions.isEmpty() -> "Not done yet"
                        done -> "${sessions.size} check${if (sessions.size == 1) "" else "s"} done"
                        else -> "In progress"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (sessions.isNotEmpty() || answered) {
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (coverage != null && coverage.started) {
            Gap(10)
            PoshanProgress(value = coverage.fraction, showPercent = false)
            Gap(6)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${coverage.answered} of ${coverage.total} questions answered",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (coverage.concerns > 0) {
                    Text(
                        "${coverage.concerns} to watch",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.review,
                    )
                }
            }
            if (coverage.notSure > 0) {
                Text(
                    "${coverage.notSure} answered \"not sure\"",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.monitor,
                )
            }
        }

        AnimatedVisibility(visible = expanded && sessions.isNotEmpty()) {
            Column {
                Gap(12)
                sessions.forEach { session ->
                    SessionDetail(
                        session = session,
                        dateFormat = dateFormat,
                        onOpenReport = { onOpenReport(session) },
                    )
                    Gap(8)
                }
            }
        }
    }
}

@Composable
private fun SessionDetail(
    session: AssessmentSession,
    dateFormat: DateTimeFormatter,
    onOpenReport: () -> Unit,
) {
    val c = riskColors
    val date = remember(session) {
        Instant.ofEpochMilli(session.completedAt ?: session.startedAt)
            .atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormat)
    }

    PoshanCard(
        onClick = onOpenReport,
        container = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "At ${AgeCalculator.format(session.ageMonthsAtAssessment)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            session.composite?.band?.let {
                com.teamposhan.biomed.ui.components.RiskChip(it)
            }
        }

        // ---- which modules were completed at this visit ---------------------
        Gap(10)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ModuleDot("1", session.categoryResults.isNotEmpty(), c.low)
            ModuleDot("2", session.attention != null, c.low)
            ModuleDot("3", session.vitaminD != null, c.low)
            Spacer(Modifier.weight(1f))
            session.dq?.let {
                Text(
                    "DQ ${it.dq.toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // ---- domain outcomes -------------------------------------------------
        if (session.categoryResults.isNotEmpty()) {
            Gap(10)
            session.categoryResults.forEach { cat ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        categoryName(cat.category),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    com.teamposhan.biomed.ui.components.DomainChip(cat.status)
                }
                if (cat.missedEarlier.isNotEmpty()) {
                    Text(
                        "Delayed since ${cat.earliestMissBand ?: "an earlier band"} · " +
                            "${cat.missedEarlier.size} earlier milestone(s) missing",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.review,
                    )
                }
            }
        }

        // ---- growth at this visit, with z-scores ------------------------------
        session.growth?.let { g ->
            Gap(10)
            val age = session.ageMonthsAtAssessment
            val lengthWord = GrowthStandards.measurementLabel(age)
            val z = session.growthZ
            Text(
                "Growth",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Gap(4)
            MeasurementRow(
                "Weight",
                g.weightKg?.let { "%.1f kg".format(it) },
                z?.weightForAge,
            )
            MeasurementRow(
                lengthWord,
                g.heightCm?.let { "%.1f cm".format(it) },
                z?.heightForAge,
            )
            MeasurementRow(
                "BMI",
                GrowthStandards.bmi(g.weightKg, g.heightCm)?.let { "%.1f".format(it) },
                z?.bmiForAge,
            )
            MeasurementRow(
                "MUAC",
                g.muacMm?.let { "%.0f mm".format(it) },
                z?.muacForAge,
            )
        }

        // ---- what was actually answered ---------------------------------------
        if (session.answers.isNotEmpty()) {
            Gap(10)
            AnsweredQuestions(session)
        }

        // ---- attention -------------------------------------------------------
        session.attention?.let { a ->
            Gap(10)
            Text(
                "Attention & gaze",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Gap(2)
            if (a.inconclusive) {
                Text(
                    "Not completed — tracking quality too low to interpret",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "Engagement ${(a.engagementIndex * 100).toInt()}% · " +
                        "social ${(a.socialAttentionRatio * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Weight 8.2 kg   -1.4 SD  — the number and its meaning on one line. */
@Composable
private fun MeasurementRow(label: String, value: String?, z: Float?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(66.dp),
        )
        Text(
            value ?: "—",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        if (z != null) {
            Text(
                "%+.1f SD".format(z),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = zColor(z),
            )
        }
    }
}

@Composable
private fun ModuleDot(number: String, done: Boolean, doneColor: Color) {
    Box(
        Modifier
            .size(24.dp)
            .background(
                if (done) doneColor else MaterialTheme.colorScheme.surfaceContainerHighest,
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "Module $number completed",
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        } else {
            Text(
                number,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Module-by-module state for the visit in progress, with module 1 broken out by category.
 *
 * Module 1 is five separate questionnaires, and "milestones: in progress" tells a worker
 * nothing about which of the five they still have to sit down and do. The categories are
 * listed individually for that reason; modules 2 and 3 are single units and stay collapsed.
 */
@Composable
private fun CurrentVisitStatus(session: AssessmentSession?) {
    val c = riskColors

    if (session == null) {
        InfoBanner("No assessment started yet. Begin from the home screen.")
        return
    }

    val categories = session.categoryResults
    val m1Total = 5
    val m1Done = categories.count { it.answered > 0 }
    val m2Done = session.attention != null
    val m3Done = session.vitaminD != null
    val growthDone = session.growth != null
    val markersDone = session.markerAnswers.isNotEmpty()

    PoshanCard(contentPadding = PaddingValues(16.dp)) {
        ModuleStatusRow(
            number = "1",
            title = "Developmental milestones",
            detail = if (m1Done == 0) "Not started" else "$m1Done of $m1Total categories",
            complete = m1Done >= m1Total,
            partial = m1Done in 1 until m1Total,
        )

        // The five categories, so an unfinished module 1 says which part is unfinished.
        if (categories.isNotEmpty()) {
            Gap(6)
            Column(Modifier.padding(start = 34.dp)) {
                categories.forEach { cat ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(7.dp).background(
                                when {
                                    cat.answered == 0 -> MaterialTheme.colorScheme.outlineVariant
                                    cat.answered >= cat.total -> c.low
                                    else -> c.monitor
                                },
                                CircleShape,
                            ),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            categoryName(cat.category),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (cat.answered == 0) "—" else "${cat.answered}/${cat.total}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Gap(12)
        ModuleStatusRow(
            number = "2",
            title = "Attention & gaze",
            detail = when {
                m2Done && markersDone -> "Camera and questions done"
                m2Done -> "Camera done, questions pending"
                markersDone -> "Questions done, camera pending"
                else -> "Not started"
            },
            complete = m2Done && markersDone,
            partial = m2Done || markersDone,
        )

        Gap(12)
        ModuleStatusRow(
            number = "3",
            title = "Nutrition & vitamin D",
            detail = when {
                m3Done && growthDone -> "Measurements and questions done"
                growthDone -> "Measured, risk questions pending"
                m3Done -> "Questions done, measurements pending"
                else -> "Not started"
            },
            complete = m3Done && growthDone,
            partial = m3Done || growthDone,
        )
    }
}

@Composable
private fun ModuleStatusRow(
    number: String,
    title: String,
    detail: String,
    complete: Boolean,
    partial: Boolean,
) {
    val c = riskColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(26.dp).background(
                when {
                    complete -> c.low
                    partial -> c.monitor
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                },
                CircleShape,
            ),
            contentAlignment = Alignment.Center,
        ) {
            if (complete) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp),
                )
            } else {
                Text(
                    number,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (partial) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The milestone answers recorded at one visit, grouped by category.
 *
 * A worker returning to a child months later needs to see what was actually asked and how
 * it was answered, not only the domain verdict that came out of it — particularly for the
 * catch-up questions, where a "no" against a much younger band's milestone is the whole
 * finding.
 *
 * Collapsed by default: an assessment at 59 months can carry 123 answers, and expanding
 * that by default would bury everything below it.
 */
@Composable
private fun AnsweredQuestions(session: AssessmentSession) {
    var expanded by remember(session.id) { mutableStateOf(false) }
    val c = riskColors
    val haptics = com.teamposhan.biomed.ui.haptics.rememberAppHaptics()

    val answered = session.answers.filter { it.response != null }
    if (answered.isEmpty()) return

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable { haptics.tick(); expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Questions answered",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${answered.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                // Grouped by the band the question belongs to, so catch-up answers from
                // earlier ages read as a separate block rather than being mixed in.
                val byBand = answered.groupBy { answer ->
                    MilestoneBank.byId(answer.questionId)?.band
                }
                byBand.entries
                    .sortedBy { it.key?.minMonths ?: Int.MAX_VALUE }
                    .forEach { (band, items) ->
                        Gap(6)
                        Text(
                            band?.label ?: "Other",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        items.forEach { answer ->
                            val question = MilestoneBank.byId(answer.questionId)
                            val response = answer.response ?: return@forEach
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Box(
                                    Modifier
                                        .padding(top = 5.dp)
                                        .size(7.dp)
                                        .background(answerColour(response, c), CircleShape),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    question?.text ?: answer.questionId,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    answerLabel(response),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = answerColour(response, c),
                                )
                            }
                        }
                    }
                Gap(4)
            }
        }
    }
}

private fun answerLabel(response: ResponseType): String = when (response) {
    ResponseType.YES_OBSERVED -> "Yes"
    ResponseType.MAYBE -> "Maybe"
    ResponseType.NOT_YET -> "No"
    ResponseType.NOT_SURE -> "Not sure"
}

@Composable
private fun answerColour(
    response: ResponseType,
    c: com.teamposhan.biomed.ui.theme.RiskColors,
): androidx.compose.ui.graphics.Color = when (response) {
    ResponseType.YES_OBSERVED -> c.low
    ResponseType.MAYBE -> c.monitor
    ResponseType.NOT_YET -> c.review
    ResponseType.NOT_SURE -> c.priority
}

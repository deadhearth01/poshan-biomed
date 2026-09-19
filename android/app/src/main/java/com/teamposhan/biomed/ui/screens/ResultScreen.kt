package com.teamposhan.biomed.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import com.teamposhan.biomed.ui.components.PoshanProgress
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PictureAsPdf
import com.teamposhan.biomed.domain.report.PdfReportWriter
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Description
import com.teamposhan.biomed.domain.report.MarkdownReport
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teamposhan.biomed.R
import com.teamposhan.biomed.data.model.AssessmentCategory
import com.teamposhan.biomed.data.model.AssessmentSession
import com.teamposhan.biomed.data.model.AttentionResult
import com.teamposhan.biomed.data.model.CategoryResult
import com.teamposhan.biomed.data.model.ChildProfile
import com.teamposhan.biomed.data.model.CompositeRisk
import com.teamposhan.biomed.data.model.DevelopmentalQuotient
import com.teamposhan.biomed.data.model.DomainStatus
import com.teamposhan.biomed.data.model.GrowthZ
import com.teamposhan.biomed.data.model.RiskBand
import com.teamposhan.biomed.data.model.VitaminDResult
import com.teamposhan.biomed.data.model.ResponseType
import com.teamposhan.biomed.domain.nutrition.NutritionQuestionBank
import com.teamposhan.biomed.domain.nutrition.NutritionResult
import com.teamposhan.biomed.domain.age.AgeCalculator
import com.teamposhan.biomed.data.model.Sex
import com.teamposhan.biomed.domain.growth.GrowthStandards
import com.teamposhan.biomed.domain.growth.SampleGrowthData
import com.teamposhan.biomed.domain.immunization.DoseStatus
import com.teamposhan.biomed.domain.immunization.ImmunizationSchedule
import com.teamposhan.biomed.domain.risk.VitaminDInput
import com.teamposhan.biomed.ui.components.DomainChip
import com.teamposhan.biomed.ui.components.Gap
import com.teamposhan.biomed.ui.components.InfoBanner
import com.teamposhan.biomed.ui.components.PoshanCard
import com.teamposhan.biomed.ui.components.PoshanOutlinedButton
import com.teamposhan.biomed.ui.components.PoshanPrimaryButton
import com.teamposhan.biomed.ui.components.PoshanTonalButton
import com.teamposhan.biomed.ui.components.RiskChip
import com.teamposhan.biomed.ui.components.SectionLabel
import com.teamposhan.biomed.ui.components.StatTile
import com.teamposhan.biomed.ui.theme.riskColors

// ---------------------------------------------------------------------------
// Result
// ---------------------------------------------------------------------------

@Composable
fun ResultScreen(
    child: ChildProfile?,
    composite: CompositeRisk?,
    dq: DevelopmentalQuotient?,
    categories: List<CategoryResult>,
    attention: AttentionResult?,
    vitaminD: VitaminDResult?,
    growthZ: GrowthZ?,
    onReport: () -> Unit,
    onDone: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(stringResource(R.string.composite_risk_score), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            PoshanCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            bandLabel(composite?.band ?: RiskBand.ON_TRACK),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            child?.let { "${it.name} · ${AgeCalculator.format(AgeCalculator.assessmentMonths(it))}" } ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    RiskChip(composite?.band ?: RiskBand.ON_TRACK)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(stringResource(R.string.calculate_dq), "${dq?.dq?.toInt() ?: 0}%", Modifier.weight(1f))
                StatTile("Overall risk", "${((composite?.overall ?: 0f) * 100).toInt()}%", Modifier.weight(1f), accent = riskColors.monitor)
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Domains")
            categories.forEach { c ->
                PoshanCard(modifier = Modifier.padding(bottom = 10.dp), contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(categoryName(c.category), style = MaterialTheme.typography.titleMedium)
                            if (c.redFlags.isNotEmpty()) {
                                Text("Red flags: " + c.redFlags.joinToString(), style = MaterialTheme.typography.bodySmall, color = riskColors.priority)
                            }
                            // Milestones the child is already late for carry the most weight,
                            // so they are named rather than folded into a count.
                            if (c.missedEarlier.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Delayed since ${c.earliestMissBand ?: "an earlier band"} · ${c.missedEarlier.size} earlier milestone(s) not yet reached",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = riskColors.review,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                c.missedEarlier.take(4).forEach { missed ->
                                    Text(
                                        "• $missed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (c.missedEarlier.size > 4) {
                                    Text(
                                        "and ${c.missedEarlier.size - 4} more",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        DomainChip(c.status)
                    }
                }
            }

            attention?.takeIf { !it.inconclusive }?.let {
                PoshanCard(modifier = Modifier.padding(bottom = 10.dp), contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.attention_module), style = MaterialTheme.typography.titleMedium)
                            Text("Engagement ${(it.engagementIndex * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RiskChip(it.band)
                    }
                }
            }

            vitaminD?.let {
                PoshanCard(modifier = Modifier.padding(bottom = 10.dp), contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.vitamin_d_module), style = MaterialTheme.typography.titleMedium)
                            Text("Risk score ${it.score}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RiskChip(it.band)
                    }
                }
            }

            growthZ?.worst?.let {
                PoshanCard(modifier = Modifier.padding(bottom = 10.dp), contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.growth), style = MaterialTheme.typography.titleMedium)
                            Text("Worst z-score ${"%.1f".format(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DomainChip(
                            when {
                                it <= -2f -> DomainStatus.DELAYED
                                it < -1.5f -> DomainStatus.CONCERN
                                else -> DomainStatus.ON_TRACK
                            }
                        )
                    }
                }
            }

            composite?.takeIf { it.referrals.isNotEmpty() }?.let { comp ->
                Spacer(Modifier.height(10.dp))
                SectionLabel(stringResource(R.string.doctor_review_recommended))
                PoshanCard {
                    comp.referrals.forEach { r ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text("•  ", color = MaterialTheme.colorScheme.primary)
                            Text(r, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            composite?.takeIf { it.reasons.isNotEmpty() }?.let { comp ->
                Spacer(Modifier.height(12.dp))
                PoshanCard(container = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Text("Why this score", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    comp.reasons.forEach {
                        Text("•  $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            InfoBanner(stringResource(R.string.screening_not_diagnosis))
        }
        Box(Modifier.padding(20.dp)) {
            Column {
                PoshanOutlinedButton(stringResource(R.string.report_generation), onClick = onReport, icon = Icons.Filled.Share)
                Gap(10)
                PoshanPrimaryButton(stringResource(R.string.done), onClick = onDone, icon = Icons.Filled.Check)
            }
        }
    }
}

@Composable
private fun bandLabel(band: RiskBand): String = when (band) {
    RiskBand.ON_TRACK -> stringResource(R.string.no_concern)
    RiskBand.MONITOR -> stringResource(R.string.needs_review)
    RiskBand.REVIEW -> stringResource(R.string.doctor_review_recommended)
    RiskBand.PRIORITY -> stringResource(R.string.priority_review)
}

// ---------------------------------------------------------------------------
// Vitamin D risk
// ---------------------------------------------------------------------------

@Composable
private fun ToggleCard(
    label: String,
    checked: Boolean,
    supporting: String? = null,
    onChange: (Boolean) -> Unit,
) {
    PoshanCard(modifier = Modifier.padding(bottom = 10.dp), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                if (supporting != null) {
                    Text(
                        supporting,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

// ---------------------------------------------------------------------------
// Immunization
// ---------------------------------------------------------------------------

@Composable
fun ImmunizationScreen(ageMonths: Float, given: Set<String>, onToggle: (String) -> Unit) {
    val ageDays = (ageMonths * 30.4375f).toInt()
    val states = remember(ageDays, given) {
        ImmunizationSchedule.statusFor(ageDays, given, includeIap = true)
    }
    val overdue = states.filter { it.status == DoseStatus.OVERDUE }
    val dueSoon = states.filter { it.status == DoseStatus.DUE_SOON }
    val givenStates = states.filter { it.status == DoseStatus.GIVEN }
    val upcoming = states.filter { it.status == DoseStatus.UPCOMING }
    val c = riskColors
    val haptics = com.teamposhan.biomed.ui.haptics.rememberAppHaptics()

    // Which groups are open.
    //
    // The two that need action start open; the two that are reference start closed. With
    // thirty-odd doses in the full schedule, an all-expanded list buries the one overdue
    // dose that actually matters under twenty that are years away.
    var showOverdue by remember { mutableStateOf(true) }
    var showDue by remember { mutableStateOf(true) }
    var showGiven by remember { mutableStateOf(false) }
    var showUpcoming by remember { mutableStateOf(false) }

    // Grouped by what the caregiver needs to act on, not by the order the doses appear in
    // the schedule. A flat list of thirty checkboxes made an overdue birth dose look exactly
    // like a dose not due for another two years.
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        item {
            Text(
                stringResource(R.string.immunization),
                style = MaterialTheme.typography.headlineSmall,
            )
            Gap(4)
            Text(
                "${givenStates.size} of ${states.size} recorded",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Gap(14)
            PoshanProgress(
                value = if (states.isEmpty()) 0f else givenStates.size.toFloat() / states.size,
                showPercent = false,
            )
            Gap(16)
        }

        if (overdue.isNotEmpty()) {
            item {
                DoseSectionHeader(
                    title = "Overdue",
                    count = overdue.size,
                    colour = c.priority,
                    note = "Visit your Anganwadi centre or health centre.",
                    expanded = showOverdue,
                ) { haptics.tick(); showOverdue = !showOverdue }
            }
            if (showOverdue) {
                items(overdue, key = { it.dose.id }) { state ->
                    DoseRow(state, c.priority, ageDays) {
                        haptics.confirm(); onToggle(state.dose.id)
                    }
                }
            }
            item { Gap(18) }
        }

        if (dueSoon.isNotEmpty()) {
            item {
                DoseSectionHeader(
                    title = "Due now",
                    count = dueSoon.size,
                    colour = c.monitor,
                    note = "Ready to be given at this visit.",
                    expanded = showDue,
                ) { haptics.tick(); showDue = !showDue }
            }
            if (showDue) {
                items(dueSoon, key = { it.dose.id }) { state ->
                    DoseRow(state, c.monitor, ageDays) {
                        haptics.confirm(); onToggle(state.dose.id)
                    }
                }
            }
            item { Gap(18) }
        }

        if (givenStates.isNotEmpty()) {
            item {
                DoseSectionHeader(
                    title = "Taken",
                    count = givenStates.size,
                    colour = c.low,
                    note = "Already recorded. Tap one to correct a mistake.",
                    expanded = showGiven,
                ) { haptics.tick(); showGiven = !showGiven }
            }
            if (showGiven) {
                items(givenStates, key = { it.dose.id }) { state ->
                    DoseRow(state, c.low, ageDays) { haptics.tick(); onToggle(state.dose.id) }
                }
            }
            item { Gap(18) }
        }

        if (upcoming.isNotEmpty()) {
            item {
                DoseSectionHeader(
                    title = "Upcoming",
                    count = upcoming.size,
                    colour = MaterialTheme.colorScheme.outline,
                    note = "Not due yet — nothing to do now.",
                    expanded = showUpcoming,
                ) { haptics.tick(); showUpcoming = !showUpcoming }
            }
            if (showUpcoming) {
                items(upcoming, key = { it.dose.id }) { state ->
                    DoseRow(state, MaterialTheme.colorScheme.outline, ageDays) {
                        haptics.tick(); onToggle(state.dose.id)
                    }
                }
            }
        }

        item {
            Gap(16)
            InfoBanner(
                "Doses marked IAP are from the Indian Academy of Paediatrics schedule and " +
                    "are optional additions to the national schedule.",
            )
            Gap(24)
        }
    }
}

/**
 * A collapsible group header.
 *
 * The whole row is the tap target, and the count stays visible while collapsed so a closed
 * group still answers its own question — "how many are overdue" — without being opened.
 */
@Composable
private fun DoseSectionHeader(
    title: String,
    count: Int,
    colour: Color,
    note: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            // Clip before clickable so the ripple follows the rounded shape.
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onToggle)
            .padding(vertical = 10.dp, horizontal = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(colour, CircleShape))
            Spacer(Modifier.width(10.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "$count",
                style = MaterialTheme.typography.titleMedium,
                color = colour,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        note?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, top = 2.dp),
            )
        }
    }
}

/**
 * One dose. The whole row is the tap target for recording it, so a health worker marking
 * several doses does not have to hit a small checkbox each time.
 */
@Composable
private fun DoseRow(
    state: com.teamposhan.biomed.domain.immunization.DoseState,
    accent: Color,
    ageDays: Int,
    onToggle: () -> Unit,
) {
    val isGiven = state.status == DoseStatus.GIVEN
    PoshanCard(
        onClick = onToggle,
        modifier = Modifier.padding(bottom = 8.dp),
        container = if (isGiven) {
            MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .background(
                        if (isGiven) accent else MaterialTheme.colorScheme.surfaceContainerHighest,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isGiven) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = stringResource(R.string.given),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    state.dose.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        state.dose.group,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.dose.iapOnly) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "IAP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                // How late, in plain words. "Overdue" alone does not distinguish a dose
                // three days late from one three months late, and the difference matters.
                if (state.status == DoseStatus.OVERDUE) {
                    val lateDays = ageDays - state.dose.dueAgeDays
                    Text(
                        latenessLabel(lateDays),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/** Turns a number of days late into something a caregiver reads without doing arithmetic. */
private fun latenessLabel(days: Int): String = when {
    days >= 730 -> "over 2 years late"
    days >= 365 -> "over a year late"
    days >= 60 -> "${days / 30} months late"
    days >= 30 -> "1 month late"
    days >= 14 -> "${days / 7} weeks late"
    days >= 7 -> "1 week late"
    else -> "$days days late"
}

// ---------------------------------------------------------------------------
// Report
// ---------------------------------------------------------------------------

fun buildReportText(
    child: ChildProfile,
    session: AssessmentSession?,
    history: List<AssessmentSession> = listOfNotNull(session),
): String = com.teamposhan.biomed.domain.report.ReportBuilder.build(child, session, history)

@Composable
fun ReportScreen(
    child: ChildProfile?,
    session: AssessmentSession?,
    history: List<AssessmentSession> = emptyList(),
    onOpenChart: (GrowthStandards.Metric, Boolean) -> Unit = { _, _ -> },
    onOpenModule: (ModuleDetail) -> Unit = {},
    doctorName: String = "",
    doctorPhone: String = "",
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val text = remember(child, session, history) {
        if (child != null) buildReportText(child, session, history) else "No child selected."
    }

    // Demo mode: plots a fabricated trajectory so the charts, indicators and findings can
    // be checked end to end without entering a year of visits. Never written to the store.
    var sampleData by remember { mutableStateOf(false) }

    val realAge = session?.ageMonthsAtAssessment
        ?: child?.let { AgeCalculator.assessmentMonths(it) }
        ?: 0f
    val ageMonths = if (sampleData && realAge <= 0f) SampleGrowthData.FALLBACK_AGE_MONTHS else realAge
    val seed = child?.id?.hashCode() ?: 0

    val growth = if (sampleData) {
        SampleGrowthData.latestMeasurement(child?.sex ?: Sex.MALE, ageMonths, seed)
    } else {
        session?.growth
    }
    val growthZ = if (sampleData) {
        GrowthStandards.z(
            sex = child?.sex ?: Sex.MALE,
            ageMonths = ageMonths,
            weightKg = growth?.weightKg,
            heightCm = growth?.heightCm,
            muacMm = growth?.muacMm,
        )
    } else {
        session?.growthZ
    }
    val findings = remember(growth, growthZ, ageMonths) {
        com.teamposhan.biomed.domain.growth.GrowthInterpretation.findings(
            z = growthZ,
            muacMm = growth?.muacMm,
            ageMonths = ageMonths,
        )
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(stringResource(R.string.report_generation), style = MaterialTheme.typography.headlineSmall)
            if (child != null) {
                Text(
                    "${child.name} · ${AgeCalculator.format(ageMonths)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Gap(14)

            // ---------- growth charts, auto-plotted from the recorded values ----------
            child?.let { c ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("WHO growth charts")
                    Spacer(Modifier.weight(1f))
                    com.teamposhan.biomed.ui.components.WhyButton(
                        com.teamposhan.biomed.ui.components.WhyLibrary.growthZScore,
                        label = "What is SD?",
                    )
                }
                Text(
                    "Tap a chart to open the full WHO sheet · long-press for the cut-offs behind it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Gap(10)
                ToggleCard(
                    label = "Sample data",
                    checked = sampleData,
                    onChange = { sampleData = it },
                    supporting = "Plot a demo trajectory across the periodic visits to check the charts",
                )
                if (sampleData) {
                    InfoBanner(
                        "Showing fabricated measurements for testing. Nothing on this screen is this " +
                            "child's real data and nothing is saved.",
                    )
                    Gap(10)
                }
                GrowthStandards.Metric.entries.forEach { metric ->
                    GrowthChartCard(
                        metric = metric,
                        sex = c.sex,
                        ageMonths = ageMonths,
                        history = if (sampleData) {
                            SampleGrowthData.history(metric, c.sex, ageMonths, seed)
                                .map { (month, value) -> GrowthPoint(month, value) }
                        } else {
                            growthHistoryFor(metric, history, session)
                        },
                        onOpen = { onOpenChart(metric, sampleData) },
                        onOpenInfo = { onOpenChart(metric, sampleData) },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                Gap(4)
            }

            // ---------- what each module found ----------
            //
            // The report previously jumped from the growth charts straight to the raw text
            // dump, so a caregiver reading it on screen never saw the milestone or
            // attention results at all — only a monospaced block most of them cannot read.
            ReportModuleSections(
                session = session,
                childNutritionBackground = child?.nutritionBackground.orEmpty(),
                onOpenModule = onOpenModule,
            )

            // ---------- interpretation indicators ----------
            if (findings.isNotEmpty()) {
                SectionLabel("Findings")
                findings.forEach { f ->
                    PoshanCard(modifier = Modifier.padding(bottom = 10.dp), contentPadding = PaddingValues(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(f.label, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    f.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            DomainChip(f.severity)
                        }
                    }
                }
                Gap(6)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Full report")
                Spacer(Modifier.weight(1f))
                com.teamposhan.biomed.ui.components.WhyButton(
                    com.teamposhan.biomed.ui.components.WhyLibrary.gazeNotDiagnosis,
                    label = "What is this?",
                )
            }
            PoshanCard(container = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Text(
                    text,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
            }
            Spacer(Modifier.height(16.dp))
            InfoBanner(stringResource(R.string.screening_not_diagnosis))
        }
        Box(Modifier.padding(20.dp)) {
            Column {
                PoshanTonalButton(stringResource(R.string.share_report), icon = Icons.Filled.Share) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Poshan screening report")
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share report"))
                }
                Gap(10)
                PoshanTonalButton("Export as Markdown", icon = Icons.Filled.Description) {
                    if (child != null) {
                        // Written to the same reports cache the FileProvider exposes, so it
                        // shares by the same per-file grant as the PDF.
                        runCatching {
                            val md = MarkdownReport.build(child, session, history)
                            val dir = java.io.File(context.cacheDir, "reports").apply { mkdirs() }
                            val safe = child.name.trim()
                                .replace(Regex("[^A-Za-z0-9]+"), "_").take(24)
                            val stamp = java.time.format.DateTimeFormatter
                                .ofPattern("yyyyMMdd-HHmm")
                                .withZone(java.time.ZoneId.systemDefault())
                                .format(java.time.Instant.now())
                            java.io.File(dir, "poshan-$safe-$stamp.md")
                                .apply { writeText(md) }
                        }.onSuccess { file ->
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file,
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                // text/markdown so a viewer that understands it renders the
                                // tables rather than showing raw pipes.
                                type = "text/markdown"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "Poshan report - ${child.name}")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Markdown"))
                        }
                    }
                }
                Gap(10)
                PoshanTonalButton("Export as PDF", icon = Icons.Filled.PictureAsPdf) {
                    if (child != null) {
                        // Rendering is synchronous and can take a moment on a long history,
                        // but it is user-initiated and the alternative — a spinner and a
                        // background job — is more machinery than a few hundred lines of
                        // text layout warrants.
                        runCatching {
                            PdfReportWriter.write(context, child, session, history)
                        }.onSuccess { file ->
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file,
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    "Poshan report — ${child.name}",
                                )
                                // Without this the receiving app gets a URI it has no
                                // permission to open.
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share PDF"))
                        }
                    }
                }
                if (doctorPhone.isNotBlank() && child != null) {
                    Gap(10)
                    PoshanTonalButton(
                        "Send to ${doctorName.ifBlank { "the doctor" }}",
                        icon = Icons.Filled.Send,
                    ) {
                        // ACTION_SENDTO with an smsto: URI opens the messaging app with the
                        // number and text filled in, and stops there. The parent presses
                        // send. The app never transmits a child's health data on its own.
                        val body = buildString {
                            appendLine("Poshan screening report for ${child.name}")
                            appendLine()
                            append(text)
                        }
                        val intent = Intent(
                            Intent.ACTION_SENDTO,
                            android.net.Uri.parse("smsto:${doctorPhone.trim()}"),
                        ).apply { putExtra("sms_body", body) }
                        runCatching { context.startActivity(intent) }
                    }
                }
                Gap(10)
                PoshanPrimaryButton(stringResource(R.string.done), onClick = onDone)
            }
        }
    }
}

/**
 * Every recorded measurement for one indicator, oldest first, so the chart can draw the
 * child's trajectory across the periodic visits rather than a single lonely dot.
 */
fun growthHistoryFor(
    metric: GrowthStandards.Metric,
    sessions: List<AssessmentSession>,
    current: AssessmentSession?,
): List<GrowthPoint> {
    val all = (sessions + listOfNotNull(current)).distinctBy { it.id }
    return all.mapNotNull { s ->
        val g = s.growth ?: return@mapNotNull null
        val value = GrowthStandards.measuredValue(metric, g.weightKg, g.heightCm, g.muacMm)
            ?: return@mapNotNull null
        if (value <= 0f) return@mapNotNull null
        if (s.ageMonthsAtAssessment < GrowthStandards.firstMonth(metric)) return@mapNotNull null
        GrowthPoint(s.ageMonthsAtAssessment, value)
    }.sortedBy { it.ageMonths }
}

/**
 * The per-module results, rendered for reading rather than for sharing.
 *
 * Deliberately mirrors the structure of the shared text report so the two cannot drift: a
 * worker reading the screen and a clinician reading the shared text should be looking at
 * the same findings in the same order.
 */
@Composable
private fun ReportModuleSections(
    session: AssessmentSession?,
    childNutritionBackground: Map<String, ResponseType> = emptyMap(),
    onOpenModule: (ModuleDetail) -> Unit = {},
) {
    if (session == null) return
    val c = riskColors

    // ---- 1. developmental milestones ------------------------------------
    if (session.categoryResults.isNotEmpty()) {
        SectionLabel("Developmental milestones")
        Gap(8)
        session.dq?.let { dq ->
            PoshanCard(
                modifier = Modifier.padding(bottom = 10.dp),
                contentPadding = PaddingValues(16.dp),
                onClick = { onOpenModule(ModuleDetail.MILESTONES) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Developmental quotient", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Developmental age ${AgeCalculator.format(dq.developmentalAgeMonths)} " +
                                "against ${AgeCalculator.format(dq.chronologicalAgeMonths)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "${dq.dq.toInt()}%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            dq.dq >= 85f -> c.low
                            dq.dq >= 70f -> c.monitor
                            else -> c.review
                        },
                    )
                }
            }
        }
        session.categoryResults.forEach { cat ->
            PoshanCard(
                modifier = Modifier.padding(bottom = 8.dp),
                contentPadding = PaddingValues(14.dp),
                onClick = { onOpenModule(ModuleDetail.MILESTONES) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(categoryName(cat.category), style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${cat.answered} of ${cat.total} answered · ${cat.concerns} not yet achieved",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (cat.missedEarlier.isNotEmpty()) {
                            Text(
                                "Delayed since ${cat.earliestMissBand ?: "an earlier band"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = c.review,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    DomainChip(cat.status)
                }
                if (cat.redFlags.isNotEmpty()) {
                    Gap(6)
                    cat.redFlags.take(3).forEach { flag ->
                        Text(
                            "! $flag",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.priority,
                        )
                    }
                }
            }
        }
        Gap(10)
    }

    // ---- 2. attention and gaze -------------------------------------------
    session.attention?.let { a ->
        SectionLabel("Attention & gaze")
        Gap(8)
        PoshanCard(
            modifier = Modifier.padding(bottom = 10.dp),
            contentPadding = PaddingValues(16.dp),
            onClick = { onOpenModule(ModuleDetail.ATTENTION) },
        ) {
            if (a.inconclusive) {
                Text(
                    "Assessment not completed",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Gap(4)
                Text(
                    "The camera could not track reliably enough to interpret. This is not a " +
                        "low result — no valid measurement was obtained.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Engagement",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${(a.engagementIndex * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Gap(4)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Looking at the social half",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${(a.socialAttentionRatio * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Gap(4)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Usable frames",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${(a.validFrameRatio * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }

    // ---- 3. feeding and nutrition ------------------------------------------
    //
    // These answers were collected and synced from the first build but never rendered here,
    // so a report could be handed to a health worker with the entire feeding assessment
    // missing. The scoring already existed in NutritionResult; only the section was absent.
    val nutritionAnswers = session.nutritionAnswers + childNutritionBackground
    if (nutritionAnswers.isNotEmpty()) {
        val practice = NutritionQuestionBank.forAge(session.ageMonthsAtAssessment)
        val background = NutritionQuestionBank.backgroundQuestions
        val nutrition = NutritionResult.evaluate(practice, background, nutritionAnswers)

        SectionLabel("Feeding & nutrition")
        Gap(8)
        PoshanCard(
            modifier = Modifier.padding(bottom = 10.dp),
            contentPadding = PaddingValues(16.dp),
            onClick = { onOpenModule(ModuleDetail.NUTRITION) },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        nutrition.level.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "${nutrition.practiceAnswered} of ${nutrition.practiceTotal} feeding " +
                            "questions answered",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DomainChip(
                    when (nutrition.level) {
                        NutritionResult.Level.ADEQUATE -> DomainStatus.ON_TRACK
                        NutritionResult.Level.SOME_GAPS -> DomainStatus.CONCERN
                        NutritionResult.Level.NEEDS_SUPPORT -> DomainStatus.DELAYED
                        NutritionResult.Level.NOT_ASSESSED -> DomainStatus.NOT_ASSESSED
                    }
                )
            }
            Gap(8)
            Text(nutrition.level.guidance, style = MaterialTheme.typography.bodyMedium)

            if (nutrition.concerns.isNotEmpty()) {
                Gap(10)
                Text(
                    "To discuss",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Gap(4)
                // Named individually rather than counted: a health worker acts on which
                // practice is off, not on how many are.
                nutrition.concerns.take(6).forEach {
                    Text(
                        "• $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.review,
                    )
                }
                if (nutrition.concerns.size > 6) {
                    Text(
                        "and ${nutrition.concerns.size - 6} more",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (nutrition.backgroundAnswered > 0) {
                Gap(10)
                Text(
                    "Household background: ${nutrition.backgroundRisks.toInt()} risk " +
                        "factor(s) of ${nutrition.backgroundTotal} recorded",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    // ---- 4. vitamin D -----------------------------------------------------
    session.vitaminD?.let { v ->
        SectionLabel("Vitamin D risk")
        Gap(8)
        PoshanCard(
            modifier = Modifier.padding(bottom = 10.dp),
            contentPadding = PaddingValues(16.dp),
            onClick = { onOpenModule(ModuleDetail.VITAMIN_D) },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Risk score ${v.score}", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Questionnaire-based estimate, not a blood test",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RiskChip(v.band)
            }
            if (v.factors.isNotEmpty()) {
                Gap(8)
                v.factors.take(4).forEach {
                    Text(
                        "• $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    // ---- 5. immunisation --------------------------------------------------
    if (session.givenVaccines.isNotEmpty()) {
        SectionLabel("Immunisation")
        Gap(8)
        PoshanCard(
            modifier = Modifier.padding(bottom = 10.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            Text(
                "${session.givenVaccines.size} dose(s) recorded",
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

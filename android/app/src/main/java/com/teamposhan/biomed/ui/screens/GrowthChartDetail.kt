package com.teamposhan.biomed.ui.screens

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teamposhan.biomed.data.model.ChildProfile
import com.teamposhan.biomed.data.model.Sex
import com.teamposhan.biomed.domain.age.AgeCalculator
import com.teamposhan.biomed.domain.growth.GrowthInterpretation
import com.teamposhan.biomed.domain.growth.GrowthStandards
import com.teamposhan.biomed.ui.components.Gap
import com.teamposhan.biomed.ui.components.InfoBanner
import com.teamposhan.biomed.ui.components.PoshanCard
import com.teamposhan.biomed.ui.components.PoshanScaffold
import com.teamposhan.biomed.ui.components.SectionLabel
import com.teamposhan.biomed.ui.haptics.rememberAppHaptics
import com.teamposhan.biomed.ui.theme.riskColors
import kotlin.math.abs

/** Indicators the MUAC field cut-offs are shown beside, since they read the same child. */
private val MUAC_COMPANION_METRICS = setOf(
    GrowthStandards.Metric.WEIGHT_FOR_AGE,
    GrowthStandards.Metric.MUAC_FOR_AGE,
)

/**
 * The full WHO chart for one indicator, opened from the report.
 *
 * Everything a worker needs to defend the reading is on this one screen: the sheet itself
 * (pinch to zoom), the z-score with its percentile, what that classifies as, the cut-offs
 * it was judged against, and every previous measurement that formed the trajectory.
 */
@Composable
fun GrowthChartDetailScreen(
    child: ChildProfile?,
    metric: GrowthStandards.Metric,
    history: List<GrowthPoint>,
    ageMonths: Float,
    muacMm: Float?,
    isSample: Boolean = false,
    onBack: () -> Unit,
) {
    val sex = child?.sex ?: Sex.MALE
    val info = remember(metric, ageMonths) { GrowthStandards.info(metric, ageMonths) }
    var band by remember(ageMonths) { mutableStateOf(ChartBand.forAge(ageMonths)) }

    val latest = history.lastOrNull()
    val z = latest?.let { GrowthStandards.zFor(metric, sex, it.ageMonths, it.value) }
    val accent = zColor(z)
    val row = remember(metric, sex, latest) {
        GrowthStandards.lmsAt(metric, sex, latest?.ageMonths ?: ageMonths)
    }

    PoshanScaffold(title = info.title, onBack = onBack) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            child?.let {
                Text(
                    "${it.name} · ${AgeCalculator.format(ageMonths)} · ${sexLabel(it.sex)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (isSample) {
                Gap(10)
                InfoBanner(
                    "Sample data — these measurements are fabricated for testing the chart, " +
                        "not this child's record.",
                )
            }
            Gap(12)

            // ---- the sheet ----------------------------------------------------
            ZoomableGrowthChart(
                metric = metric,
                sex = sex,
                band = band,
                history = history,
                modifier = Modifier.fillMaxWidth(),
            )
            Gap(8)
            Text(
                "Pinch to zoom · drag to pan",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Gap(10)

            BandPicker(selected = band, onSelect = { band = it })
            Gap(18)

            // ---- the reading ---------------------------------------------------
            SectionLabel("This child's reading")
            PoshanCard(contentPadding = PaddingValues(18.dp)) {
                if (latest == null || z == null) {
                    Text(
                        "No measurement has been recorded for this indicator yet.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "%+.2f SD".format(z),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                            )
                            Text(
                                "${percentileLabel(GrowthStandards.percentile(z))} · ${GrowthStandards.interpret(z)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Box(
                            Modifier
                                .size(14.dp)
                                .background(accent, CircleShape),
                        )
                    }
                    Gap(14)
                    ReadingRow("Measured", "%.${info.decimals}f %s".format(latest.value, info.unit))
                    ReadingRow("WHO median for this age", "%.${info.decimals}f %s".format(row.m, info.unit))
                    ReadingRow(
                        "Difference from median",
                        "%+.${info.decimals}f %s".format(latest.value - row.m, info.unit),
                    )
                    ReadingRow(
                        "One SD at this age",
                        "%.${info.decimals}f %s".format(GrowthStandards.unitsPerSd(metric, sex, latest.ageMonths), info.unit),
                    )
                    GrowthStandards.severity(z)?.let {
                        ReadingRow("Deviation", it.replaceFirstChar { c -> c.uppercase() })
                    }
                }
            }

            // ---- classification -------------------------------------------------
            if (z != null) {
                Gap(18)
                SectionLabel("What this means")
                val finding = when (metric) {
                    GrowthStandards.Metric.WEIGHT_FOR_AGE -> GrowthInterpretation.weightForAge(z)
                    GrowthStandards.Metric.LENGTH_FOR_AGE -> GrowthInterpretation.lengthForAge(z, ageMonths)
                    GrowthStandards.Metric.BMI_FOR_AGE -> GrowthInterpretation.bmiForAge(z)
                    GrowthStandards.Metric.MUAC_FOR_AGE -> GrowthInterpretation.muacFinding(muacMm, ageMonths)
                }
                PoshanCard(contentPadding = PaddingValues(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(finding.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        com.teamposhan.biomed.ui.components.DomainChip(finding.severity)
                    }
                    Gap(6)
                    Text(
                        finding.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ---- cut-offs -------------------------------------------------------
            Gap(18)
            SectionLabel("SD cut-offs used")
            CutoffTable(metric = metric, ageMonths = ageMonths, currentZ = z)

            // MUAC is a fixed-threshold screen, not a z-score one — show it alongside
            // weight so the worker sees both acute and chronic indicators together.
            if (metric in MUAC_COMPANION_METRICS && muacMm != null) {
                Gap(18)
                SectionLabel("MUAC (acute malnutrition)")
                val muacFinding = GrowthInterpretation.muacFinding(muacMm, ageMonths)
                PoshanCard(contentPadding = PaddingValues(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("%.0f mm".format(muacMm), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(muacFinding.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        com.teamposhan.biomed.ui.components.DomainChip(muacFinding.severity)
                    }
                    Gap(8)
                    CutoffRow("< 115 mm", "Severe acute malnutrition (SAM)", riskColors.priority, muacMm < 115f)
                    CutoffRow("115 – 125 mm", "Moderate acute malnutrition (MAM)", riskColors.review, muacMm in 115f..125f)
                    CutoffRow("≥ 125 mm", "Normal", riskColors.low, muacMm > 125f)
                }
            }

            // ---- history --------------------------------------------------------
            if (history.size > 1) {
                Gap(18)
                SectionLabel("Measurement history")
                PoshanCard(contentPadding = PaddingValues(16.dp)) {
                    history.reversed().forEach { p ->
                        val pz = GrowthStandards.zFor(metric, sex, p.ageMonths, p.value)
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                AgeCalculator.format(p.ageMonths),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "%.${info.decimals}f %s".format(p.value, info.unit),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                "%+.1f SD".format(pz),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = zColor(pz),
                            )
                        }
                    }
                    trendNote(history, metric, sex)?.let {
                        Gap(6)
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ---- how to read ----------------------------------------------------
            Gap(18)
            SectionLabel("How to read this chart")
            PoshanCard(contentPadding = PaddingValues(18.dp)) {
                LegendRow(Color(0xFF00843D), "0", "The median — half of healthy children sit above this line, half below.")
                LegendRow(Color(0xFFF07E26), "±1", "Still the normal range. Worth watching if the child is drifting outward.")
                LegendRow(Color(0xFFD41E4B), "±2", "The boundary of normal. Outside it, the child needs follow-up.")
                LegendRow(Color(0xFF000000), "±3", "Severe. Refer today.")
                Gap(8)
                Text(
                    "The x-axis is age in completed months and years; the y-axis is ${info.axisTitle.lowercase()}. " +
                        "The dot is this child. When several visits are recorded, the line joining them is the growth " +
                        "trajectory — its direction matters as much as any single point.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (metric == GrowthStandards.Metric.LENGTH_FOR_AGE) {
                Gap(12)
                InfoBanner(GrowthStandards.measurementHint(ageMonths))
            }
            if (metric == GrowthStandards.Metric.BMI_FOR_AGE || metric == GrowthStandards.Metric.LENGTH_FOR_AGE) {
                Gap(12)
                InfoBanner(
                    "The WHO publishes this indicator as two sheets, split at 24 months, because the child " +
                        "stops being measured lying down and starts being measured standing. There is a small, " +
                        "expected step in the curves at that point.",
                )
            }
            Gap(28)
        }
    }
}

// ---------------------------------------------------------------------------

@Composable
private fun BandPicker(selected: ChartBand, onSelect: (ChartBand) -> Unit) {
    val haptics = rememberAppHaptics()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ChartBand.entries.forEach { b ->
            FilterChip(
                selected = selected == b,
                onClick = { haptics.tick(); onSelect(b) },
                label = { Text(b.label, style = MaterialTheme.typography.labelMedium) },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(),
            )
        }
    }
}

@Composable
private fun ReadingRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CutoffTable(metric: GrowthStandards.Metric, ageMonths: Float, currentZ: Float?) {
    val c = riskColors
    val what = GrowthStandards.measurementLabel(ageMonths)
    val rows: List<Triple<String, String, Color>> = when (metric) {
        GrowthStandards.Metric.WEIGHT_FOR_AGE -> listOf(
            Triple("Below -3 SD", "Severely underweight (severe malnutrition)", c.priority),
            Triple("-3 to -2 SD", "Underweight (moderate undernutrition)", c.review),
            Triple("-2 to +2 SD", "Normal weight range", c.low),
            Triple("Above +2 SD", "High weight-for-age — possible overweight", c.review),
        )
        GrowthStandards.Metric.LENGTH_FOR_AGE -> listOf(
            Triple("Below -3 SD", "Severe stunting — urgent medical and nutritional care", c.priority),
            Triple("-3 to -2 SD", "Stunting / short stature for age", c.review),
            Triple("-2 to -1 SD", "Mildly low $what — worth monitoring", c.monitor),
            Triple("-1 to +2 SD", "Normal — about 95% of children", c.low),
            Triple("Above +2 SD", "Very tall for age (~2.3%)", c.monitor),
        )
        GrowthStandards.Metric.BMI_FOR_AGE -> listOf(
            Triple("Below -3 SD", "Severe wasting (SAM)", c.priority),
            Triple("-3 to -2 SD", "Wasting (MAM)", c.review),
            Triple("-2 to +1 SD", "Normal", c.low),
            Triple("Above +1 SD", "Overweight (~85th percentile)", c.review),
            Triple("Above +1.645 SD", "Obesity (~95th percentile)", c.priority),
        )
        // MUAC is screened on the fixed field cut-offs; the z-curve is the reference behind them.
        GrowthStandards.Metric.MUAC_FOR_AGE -> listOf(
            Triple("Below -3 SD", "Severe acute malnutrition by arm circumference", c.priority),
            Triple("-3 to -2 SD", "Moderate acute malnutrition", c.review),
            Triple("-2 to +2 SD", "Normal arm circumference for age", c.low),
            Triple("Above +2 SD", "Above the reference range", c.monitor),
        )
    }
    PoshanCard(contentPadding = PaddingValues(16.dp)) {
        rows.forEach { (range, meaning, color) ->
            CutoffRow(range, meaning, color, isActive(metric, range, currentZ))
        }
    }
}

/** Highlights the band the child actually falls in, so the table is not just reference. */
private fun isActive(metric: GrowthStandards.Metric, range: String, z: Float?): Boolean {
    if (z == null) return false
    return when (range) {
        "Below -3 SD" -> z < -3f
        "-3 to -2 SD" -> z >= -3f && z < -2f
        "-2 to +2 SD" -> z >= -2f && z <= 2f
        "-2 to -1 SD" -> z >= -2f && z < -1f
        "-1 to +2 SD" -> z >= -1f && z <= 2f
        "-2 to +1 SD" -> z >= -2f && z <= 1f
        "Above +2 SD" -> z > 2f
        "Above +1 SD" -> z > 1f && z <= 1.645f
        "Above +1.645 SD" -> z > 1.645f
        else -> false
    }
}

@Composable
private fun CutoffRow(range: String, meaning: String, color: Color, active: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(
            range,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) color else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(104.dp),
        )
        Text(
            meaning,
            style = MaterialTheme.typography.bodySmall,
            color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LegendRow(color: Color, sd: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 18.dp, height = 3.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(10.dp))
        Text(sd, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun percentileLabel(p: Float): String = when {
    p < 0.5f -> "below the 1st percentile"
    p > 99.5f -> "above the 99th percentile"
    else -> "${p.toInt()}th percentile"
}

private fun sexLabel(sex: Sex): String = when (sex) {
    Sex.MALE -> "Boy"
    Sex.FEMALE -> "Girl"
    Sex.OTHER -> "Not specified"
}

/** Growth faltering shows up as a falling z, even while the raw measurement rises. */
private fun trendNote(
    history: List<GrowthPoint>,
    metric: GrowthStandards.Metric,
    sex: Sex,
): String? {
    if (history.size < 2) return null
    val first = history.first()
    val last = history.last()
    val z0 = GrowthStandards.zFor(metric, sex, first.ageMonths, first.value)
    val z1 = GrowthStandards.zFor(metric, sex, last.ageMonths, last.value)
    val delta = z1 - z0
    return when {
        abs(delta) < 0.25f -> "Tracking steadily along the same z-curve since the first visit."
        delta <= -0.5f -> "Crossing downward by %.1f SD since the first visit — growth faltering, review feeding and illness.".format(abs(delta))
        delta < 0f -> "Drifting down %.1f SD since the first visit. Watch at the next visit.".format(abs(delta))
        delta >= 0.5f -> "Catch-up growth: up %.1f SD since the first visit.".format(delta)
        else -> "Slightly up (%.1f SD) since the first visit.".format(delta)
    }
}

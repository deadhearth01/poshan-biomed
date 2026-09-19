package com.teamposhan.biomed.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamposhan.biomed.data.model.AssessmentSession
import com.teamposhan.biomed.data.model.ChildProfile
import com.teamposhan.biomed.data.model.Sex
import com.teamposhan.biomed.domain.age.AgeCalculator
import com.teamposhan.biomed.domain.growth.GrowthStandards
import com.teamposhan.biomed.domain.growth.SampleGrowthData
import com.teamposhan.biomed.ui.components.Gap
import com.teamposhan.biomed.ui.components.InfoBanner
import com.teamposhan.biomed.ui.components.PoshanCard
import com.teamposhan.biomed.ui.components.PoshanPrimaryButton

/**
 * All four WHO charts for a child, reachable from home without finishing an assessment
 * first — growth is tracked across visits, so it should not be locked behind a report.
 */
@Composable
fun GrowthChartsHubScreen(
    child: ChildProfile?,
    sessions: List<AssessmentSession>,
    latest: AssessmentSession?,
    onOpenChart: (GrowthStandards.Metric, Boolean) -> Unit,
    onRecordMeasurement: () -> Unit,
) {
    var sampleData by remember { mutableStateOf(false) }

    val sex = child?.sex ?: Sex.MALE
    val realAge = latest?.ageMonthsAtAssessment
        ?: child?.let { AgeCalculator.assessmentMonths(it) }
        ?: 0f
    val ageMonths = if (sampleData && realAge <= 0f) SampleGrowthData.FALLBACK_AGE_MONTHS else realAge
    val seed = child?.id?.hashCode() ?: 0

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        if (child == null) {
            InfoBanner("Add a child first — the charts plot against that child's age and sex.")
            return@Column
        }

        Text(
            "${child.name} · ${AgeCalculator.format(ageMonths)}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Gap(12)

        PoshanCard(contentPadding = PaddingValues(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Sample data", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Plot a demo trajectory to check the charts",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = sampleData, onCheckedChange = { sampleData = it })
            }
        }
        Gap(10)

        val anyReal = sessions.any { it.growth != null } || latest?.growth != null
        if (sampleData) {
            InfoBanner("Fabricated measurements for testing. Nothing here is saved.")
            Gap(10)
        } else if (!anyReal) {
            InfoBanner("No measurements recorded yet. The curves below are the WHO reference.")
            Gap(10)
            PoshanPrimaryButton("Record measurements", onClick = onRecordMeasurement)
            Gap(14)
        }

        GrowthStandards.Metric.entries.forEach { metric ->
            GrowthChartCard(
                metric = metric,
                sex = sex,
                ageMonths = ageMonths,
                history = if (sampleData) {
                    SampleGrowthData.history(metric, sex, ageMonths, seed)
                        .map { (month, value) -> GrowthPoint(month, value) }
                } else {
                    growthHistoryFor(metric, sessions, latest)
                },
                onOpen = { onOpenChart(metric, sampleData) },
                onOpenInfo = { onOpenChart(metric, sampleData) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
        }
        Gap(24)
    }
}

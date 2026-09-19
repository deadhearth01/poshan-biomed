package com.teamposhan.biomed.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teamposhan.biomed.domain.schedule.AssessmentSchedule
import com.teamposhan.biomed.domain.schedule.ScheduledVisit
import com.teamposhan.biomed.domain.schedule.VisitStatus
import com.teamposhan.biomed.ui.components.PoshanCard
import com.teamposhan.biomed.ui.theme.riskColors

/**
 * Periodic surveillance timeline: the 2, 4, 6, 9, 12, 15, 18, 24, 30, 36, 48 and 60 month
 * contacts, each showing whether it is done, in progress, due or overdue.
 */
@Composable
fun AssessmentTimeline(
    visits: List<ScheduledVisit>,
    onOpenVisit: ((ScheduledVisit) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val c = riskColors
    PoshanCard(modifier = modifier, contentPadding = PaddingValues(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Surveillance schedule", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${AssessmentSchedule.completedCount(visits)} of ${visits.size} visits completed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        visits.forEachIndexed { index, visit ->
            val (tint, label) = when (visit.status) {
                VisitStatus.COMPLETED -> c.low to "Done"
                VisitStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary to "In progress"
                VisitStatus.DUE -> c.monitor to "Due now"
                VisitStatus.OVERDUE -> c.priority to "Overdue"
                VisitStatus.UPCOMING -> MaterialTheme.colorScheme.outline to "Upcoming"
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .then(
                        if (onOpenVisit != null && visit.status != VisitStatus.UPCOMING) {
                            Modifier
                                .padding(vertical = 0.dp)
                        } else Modifier
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // rail: dot + connecting line
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
                    Box(Modifier.size(18.dp).background(tint.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                        when (visit.status) {
                            VisitStatus.COMPLETED -> Icon(Icons.Filled.Check, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
                            VisitStatus.OVERDUE -> Icon(Icons.Filled.Warning, contentDescription = null, tint = tint, modifier = Modifier.size(11.dp))
                            VisitStatus.IN_PROGRESS -> Icon(Icons.Filled.Schedule, contentDescription = null, tint = tint, modifier = Modifier.size(11.dp))
                            else -> Box(Modifier.size(7.dp).background(tint, CircleShape))
                        }
                    }
                    if (index < visits.lastIndex) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(26.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).padding(bottom = if (index < visits.lastIndex) 0.dp else 0.dp)) {
                    Text(
                        visit.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (visit.status == VisitStatus.DUE || visit.status == VisitStatus.IN_PROGRESS) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    Text(
                        "${visit.bandLabel} questionnaire · $label",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (visit.status == VisitStatus.OVERDUE) tint else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teamposhan.biomed.domain.age.AgeCalculator
import com.teamposhan.biomed.domain.attention.AttentionPhase
import com.teamposhan.biomed.domain.attention.GazeMetric
import com.teamposhan.biomed.domain.attention.MetricGroup
import com.teamposhan.biomed.domain.attention.PhaseProtocol
import com.teamposhan.biomed.domain.attention.SessionStage
import com.teamposhan.biomed.ui.components.Gap
import com.teamposhan.biomed.ui.components.InfoBanner
import com.teamposhan.biomed.ui.components.PoshanCard
import com.teamposhan.biomed.ui.components.PoshanPrimaryButton
import com.teamposhan.biomed.ui.components.SectionLabel
import com.teamposhan.biomed.ui.components.StatTile
import com.teamposhan.biomed.ui.theme.accentColors
import com.teamposhan.biomed.ui.theme.riskColors

/**
 * Module 2's phase overview — the counterpart to module 1's category overview.
 *
 * Module 1 tells the caregiver up front which five categories they are about to work
 * through. Module 2 previously jumped from a single intro screen straight into a live
 * camera session, so nobody could see what the module contained, why the tasks differed
 * from another child's, or where they were in it. This screen closes that gap.
 *
 * It also states plainly whether the session will produce a score. Two of the four age
 * phases are deliberately observation-only, and a caregiver who is not told that will read
 * a blank result as a failure of the app.
 */
@Composable
fun AttentionPhaseOverviewScreen(
    childName: String,
    ageMonths: Float,
    onStartStage: (SessionStage) -> Unit,
    onBegin: () -> Unit,
) {
    val phase = remember(ageMonths) { AttentionPhase.forAge(ageMonths) }
    val probes = remember(phase) { PhaseProtocol.probesFor(phase) }
    val accents = accentColors

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
        ) {
            Text("Attention & gaze", style = MaterialTheme.typography.headlineSmall)
            Gap(4)
            Text(
                "$childName · ${AgeCalculator.format(ageMonths)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Gap(16)

            // ---- which phase, and what that means -----------------------------
            PoshanCard(container = accents.violet) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            phase.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            phase.ageLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    com.teamposhan.biomed.ui.components.StatusChip(
                        if (phase.scored) "Scored" else "Observation",
                        if (phase.scored) riskColors.low else riskColors.monitor,
                    )
                }
                Gap(10)
                Text(
                    phase.purpose,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!phase.scored) {
                Gap(12)
                InfoBanner(
                    "At this age the camera records how your child looks and attends, but " +
                        "does not produce a risk score. What is recorded today becomes the " +
                        "baseline that later visits are compared against.",
                )
            }

            Gap(20)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Stages", "${SessionStage.entries.size}", Modifier.weight(1f))
                StatTile("Tasks", "${probes.size}", Modifier.weight(1f))
                StatTile("Time", "~4 min", Modifier.weight(1f))
            }

            // ---- the four stages every session runs through --------------------
            Gap(22)
            SectionLabel("How the session runs")
            Gap(8)
            SessionStage.entries.forEachIndexed { index, stage ->
                StageRow(
                    number = index + 1,
                    stage = stage,
                    accent = accents.mint,
                    onClick = { onStartStage(stage) },
                )
                Gap(10)
            }

            // ---- the tasks chosen for this child's age -------------------------
            Gap(12)
            SectionLabel("Tasks for this age")
            Gap(4)
            Text(
                "Chosen for ${AgeCalculator.format(ageMonths)}. A younger or older child " +
                    "gets a different set — a task only appears once it means something at " +
                    "that age.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Gap(10)
            probes.forEach { probe ->
                PoshanCard(
                    modifier = Modifier.padding(bottom = 8.dp),
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(8.dp).background(riskColors.low, CircleShape),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(probe.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                probeExplanation(probe),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ---- what will be reported ----------------------------------------
            if (phase.scored) {
                Gap(14)
                SectionLabel("What the report will show")
                Gap(8)
                MetricGroup.entries.forEach { group ->
                    val reportable = PhaseProtocol.reportableMetrics(phase)
                        .filter { it.group == group }
                    if (reportable.isNotEmpty()) {
                        PoshanCard(
                            modifier = Modifier.padding(bottom = 8.dp),
                            contentPadding = PaddingValues(14.dp),
                        ) {
                            Text(
                                group.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Gap(4)
                            reportable.forEach { metric ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = riskColors.low,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        metric.label,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
                Gap(4)
                InfoBanner(
                    "Some looking measures cannot be resolved by a phone camera and are not " +
                        "reported. This is a screening aid, never a diagnosis.",
                )
            }

            Gap(24)
        }
        Box(Modifier.padding(20.dp)) {
            PoshanPrimaryButton("Begin", onClick = onBegin)
        }
    }
}

@Composable
private fun StageRow(
    number: Int,
    stage: SessionStage,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    PoshanCard(onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).background(accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$number",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stage.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    stage.purpose,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Per-stage instruction screen — the counterpart to module 1's category intro.
 *
 * Module 2 asks more of the caregiver than module 1 does: hold the phone steady, call the
 * child's name only when prompted, stay quiet during the others. Those instructions were
 * previously bundled into one wall of text on a single intro screen and read by nobody.
 */
@Composable
fun AttentionStageIntroScreen(
    stage: SessionStage,
    ageMonths: Float,
    onStart: () -> Unit,
) {
    val phase = remember(ageMonths) { AttentionPhase.forAge(ageMonths) }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
        ) {
            Text(stage.label, style = MaterialTheme.typography.headlineSmall)
            Gap(6)
            Text(
                stage.purpose,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Gap(20)

            SectionLabel("What to do")
            Gap(8)
            stageInstructions(stage, phase).forEachIndexed { i, line ->
                PoshanCard(
                    modifier = Modifier.padding(bottom = 8.dp),
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            "${i + 1}.",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Gap(10)
            InfoBanner(stageCaveat(stage))
            Gap(20)
        }
        Box(Modifier.padding(20.dp)) {
            PoshanPrimaryButton("Start", onClick = onStart)
        }
    }
}

// ---------------------------------------------------------------------------
// Copy
// ---------------------------------------------------------------------------

private fun probeExplanation(probe: PhaseProtocol.Probe): String = when (probe) {
    PhaseProtocol.Probe.VISUAL_ORIENTING ->
        "Does your child's gaze follow a slow moving shape?"
    PhaseProtocol.Probe.FACE_LOOKING ->
        "How much of the time does your child look at a face on screen?"
    PhaseProtocol.Probe.SOCIAL_VS_GEOMETRIC ->
        "Given a choice between people and moving patterns, which does your child watch?"
    PhaseProtocol.Probe.RESPONSE_TO_NAME ->
        "Does your child turn when you call their name, and how quickly?"
    PhaseProtocol.Probe.GAZE_FOLLOWING ->
        "When someone on screen looks to one side, does your child look there too?"
    PhaseProtocol.Probe.COMPLEX_SOCIAL ->
        "The same choice, but with a busier, more demanding social scene."
    PhaseProtocol.Probe.TOUCH_TASK ->
        "A short bubble-popping game — measures reaching and response speed."
}

private fun stageInstructions(stage: SessionStage, phase: AttentionPhase): List<String> =
    when (stage) {
        SessionStage.QUALITY_GATE -> listOf(
            "Sit your child on your lap, facing the screen, about an arm's length away.",
            "Hold the phone steady at your child's eye level. Prop it against something if you can.",
            "Find a spot with even light on your child's face. Avoid a bright window behind them.",
            "Wait for the green tracking indicator before continuing — if it does not appear, " +
                "move somewhere brighter and try again.",
        )
        SessionStage.BASELINE -> listOf(
            "Let your child settle and watch the screen.",
            "Stay quiet during this part — no pointing, no talking to them.",
            "It is fine if they look away. The app records that too.",
        )
        SessionStage.AGE_SPECIFIC_TASKS -> buildList {
            add("Keep holding the phone steady and stay out of your child's line of sight.")
            add("Stay silent unless the screen asks you to speak.")
            if (PhaseProtocol.Probe.RESPONSE_TO_NAME in PhaseProtocol.probesFor(phase)) {
                add(
                    "When the screen shows \"Call your child's name\", say their name once, " +
                        "in your normal voice, from behind the phone.",
                )
            }
            if (PhaseProtocol.Probe.TOUCH_TASK in PhaseProtocol.probesFor(phase)) {
                add("For the bubble game, let your child touch the screen themselves. Do not guide their hand.")
            }
            add("If your child becomes upset, press Stop. You can restart from the beginning.")
        }
        SessionStage.TRAJECTORY -> listOf(
            "Nothing to do here — the app compares today's session with previous visits.",
            "The direction of change matters more than any single visit's numbers.",
        )
    }

private fun stageCaveat(stage: SessionStage): String = when (stage) {
    SessionStage.QUALITY_GATE ->
        "If the camera cannot track your child reliably, the session is marked \"not completed\" " +
            "rather than recorded as a low result. That is deliberate."
    SessionStage.BASELINE ->
        "Every child is compared with their own baseline, not against other children."
    SessionStage.AGE_SPECIFIC_TASKS ->
        "A tired, hungry or unsettled child gives an unreliable result. Come back another time " +
            "if today is not a good day."
    SessionStage.TRAJECTORY ->
        "A single session cannot show a trend. This becomes useful from the second visit onwards."
}

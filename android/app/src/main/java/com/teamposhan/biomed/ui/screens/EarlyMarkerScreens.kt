package com.teamposhan.biomed.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teamposhan.biomed.R
import com.teamposhan.biomed.data.model.ResponseType
import com.teamposhan.biomed.domain.age.AgeCalculator
import com.teamposhan.biomed.domain.markers.EarlyMarkerBank
import com.teamposhan.biomed.domain.markers.EarlyMarkerQuestion
import com.teamposhan.biomed.domain.markers.EarlyMarkerResult
import com.teamposhan.biomed.domain.markers.MarkerWeight
import com.teamposhan.biomed.domain.markers.Observer
import com.teamposhan.biomed.domain.markers.Polarity
import com.teamposhan.biomed.ui.components.Gap
import com.teamposhan.biomed.ui.components.InfoBanner
import com.teamposhan.biomed.ui.components.PoshanCard
import com.teamposhan.biomed.ui.components.PoshanPrimaryButton
import com.teamposhan.biomed.ui.components.PoshanProgress
import com.teamposhan.biomed.ui.components.SectionLabel
import com.teamposhan.biomed.ui.components.StatusChip
import com.teamposhan.biomed.ui.components.WhyButton
import com.teamposhan.biomed.ui.components.WhyContent
import com.teamposhan.biomed.ui.haptics.rememberAppHaptics
import com.teamposhan.biomed.ui.theme.riskColors

/**
 * Intro for the early-marker questionnaire — the caregiver-report half of module 2.
 */
@Composable
fun EarlyMarkerIntroScreen(
    childName: String,
    ageMonths: Float,
    onStart: () -> Unit,
) {
    val questions = remember(ageMonths) { EarlyMarkerBank.forAge(ageMonths) }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
        ) {
            Text("What you have noticed", style = MaterialTheme.typography.headlineSmall)
            Gap(4)
            Text(
                "$childName · ${AgeCalculator.format(ageMonths)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Gap(16)

            PoshanCard {
                Text(
                    "The camera measures how your child looks at the screen. These questions " +
                        "cover what a camera cannot see — how your child sounds, how they " +
                        "react to touch and noise, and whether anything has changed.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Gap(12)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip("${questions.size} questions", MaterialTheme.colorScheme.primary)
                    StatusChip("~3 min", MaterialTheme.colorScheme.outline)
                }
            }

            Gap(16)
            InfoBanner(
                "Answer for what usually happens, not for one bad day. \"Not sure\" is a " +
                    "real answer — it is better than guessing.",
            )

            Gap(16)
            SectionLabel("Who should answer")
            Gap(6)
            Observer.entries.filter { obs -> questions.any { it.observer == obs } }.forEach { obs ->
                val count = questions.count { it.observer == obs }
                PoshanCard(
                    modifier = Modifier.padding(bottom = 8.dp),
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(obs.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(
                            "$count",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Gap(6)
            Text(
                "A parent sees the home; an Anganwadi worker sees the child in a group. " +
                    "Different answers from the two are useful, not a contradiction.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Gap(20)
        }
        Box(Modifier.padding(20.dp)) {
            PoshanPrimaryButton("Start", onClick = onStart)
        }
    }
}

/**
 * One marker question at a time, using module 1's answer template so caregivers meet the
 * same four options everywhere in the app.
 */
@Composable
fun EarlyMarkerQuestionScreen(
    ageMonths: Float,
    answers: Map<String, ResponseType>,
    onAnswer: (EarlyMarkerQuestion, ResponseType) -> Unit,
    onFinish: () -> Unit,
) {
    val questions = remember(ageMonths) { EarlyMarkerBank.forAge(ageMonths) }
    var index by remember { mutableIntStateOf(0) }

    if (questions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No marker questions for this age.")
        }
        return
    }

    val question = questions[index.coerceIn(0, questions.lastIndex)]
    val current = answers[question.id]
    val c = riskColors

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Early markers",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${index + 1} / ${questions.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusChip(question.band.label, MaterialTheme.colorScheme.primary)
            if (question.weight == MarkerWeight.REGRESSION) {
                StatusChip("Important", c.priority)
            } else if (question.weight == MarkerWeight.CRITICAL) {
                StatusChip("Key question", c.review)
            }
        }
        Spacer(Modifier.height(10.dp))
        PoshanProgress(value = (index + 1).toFloat() / questions.size, showPercent = false)
        Spacer(Modifier.height(18.dp))

        AnimatedContent(
            targetState = question.id,
            transitionSpec = {
                (slideInHorizontally(spring(stiffness = 400f)) { it / 6 } + fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(tween(180)) { -it / 8 } + fadeOut(tween(120)))
            },
            label = "marker-question",
        ) { id ->
            val q = questions.firstOrNull { it.id == id } ?: question
            Column {
                PoshanCard(contentPadding = PaddingValues(20.dp)) {
                    Text(q.text, style = MaterialTheme.typography.titleMedium)
                    Gap(10)
                    Text(
                        q.rationale,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (q.observer != Observer.ANY) {
                        Gap(10)
                        Text(
                            "Best answered by: ${q.observer.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        fun choose(option: ResponseType) {
            onAnswer(question, option)
            if (index < questions.lastIndex) index++ else onFinish()
        }

        // The same four answers as module 1. What "yes" *means* differs by question — for a
        // skill it is reassuring, for a behaviour like hand-flapping it is the concern — and
        // that inversion is handled in scoring, never by changing the buttons under the
        // caregiver.
        val yesAccent = if (question.polarity == Polarity.SKILL) c.low else c.review
        val noAccent = if (question.polarity == Polarity.SKILL) c.review else c.low

        MarkerOption(stringResource(R.string.response_yes), Icons.Filled.Check, yesAccent, current == ResponseType.YES_OBSERVED) {
            choose(ResponseType.YES_OBSERVED)
        }
        Gap(10)
        MarkerOption(stringResource(R.string.response_maybe), Icons.Filled.Remove, c.monitor, current == ResponseType.MAYBE) {
            choose(ResponseType.MAYBE)
        }
        Gap(10)
        MarkerOption(stringResource(R.string.response_no), Icons.Filled.Close, noAccent, current == ResponseType.NOT_YET) {
            choose(ResponseType.NOT_YET)
        }
        Gap(10)
        MarkerOption(stringResource(R.string.response_not_sure), Icons.Filled.HelpOutline, c.priority, current == ResponseType.NOT_SURE) {
            choose(ResponseType.NOT_SURE)
        }

        Spacer(Modifier.height(10.dp))
        WhyButton(whyEarlyMarkers, label = "Why these questions?")
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            com.teamposhan.biomed.ui.components.TextLink("Previous", enabled = index > 0) {
                if (index > 0) index--
            }
            com.teamposhan.biomed.ui.components.TextLink("Finish") { onFinish() }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Outcome of the marker questionnaire — a referral signal, never a probability. */
@Composable
fun EarlyMarkerResultScreen(
    ageMonths: Float,
    answers: Map<String, ResponseType>,
    onDone: () -> Unit,
) {
    val questions = remember(ageMonths) { EarlyMarkerBank.forAge(ageMonths) }
    val result = remember(questions, answers) { EarlyMarkerResult.evaluate(questions, answers) }
    val c = riskColors

    val accent = when (result.signal) {
        EarlyMarkerResult.Signal.REFER -> c.priority
        EarlyMarkerResult.Signal.SEVERAL -> c.review
        EarlyMarkerResult.Signal.SOME -> c.monitor
        else -> c.low
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
        ) {
            Text("Early markers", style = MaterialTheme.typography.headlineSmall)
            Gap(14)

            PoshanCard(contentPadding = PaddingValues(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp).background(accent, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        result.signal.label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                }
                Gap(10)
                Text(result.signal.guidance, style = MaterialTheme.typography.bodyMedium)
                Gap(12)
                Text(
                    "${result.answered} of ${result.total} questions answered",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (result.regressionReported) {
                Gap(14)
                PoshanCard(container = c.prioritySoft, contentPadding = PaddingValues(18.dp)) {
                    Text(
                        "A lost skill was reported",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Gap(6)
                    Text(
                        "Losing words, gestures or social skills a child previously had is " +
                            "not a phase and should not be watched and waited on. Arrange a " +
                            "developmental assessment now.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            if (result.criticalMarkers.isNotEmpty()) {
                Gap(16)
                SectionLabel("Points to raise")
                Gap(6)
                result.criticalMarkers.forEach { marker ->
                    PoshanCard(
                        modifier = Modifier.padding(bottom = 8.dp),
                        contentPadding = PaddingValues(14.dp),
                    ) {
                        Text(marker, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Gap(14)
            InfoBanner(
                "This is not a diagnosis and does not say your child has autism. It says " +
                    "whether some things are worth a closer look by someone qualified.",
            )
            Gap(10)
            WhyButton(whyEarlyMarkers, label = "How this is worked out")
            Gap(20)
        }
        Box(Modifier.padding(20.dp)) {
            PoshanPrimaryButton("Done", onClick = onDone)
        }
    }
}

@Composable
private fun MarkerOption(
    label: String,
    icon: ImageVector,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    PoshanCard(
        onClick = { haptics.click(); onClick() },
        container = if (selected) {
            accent.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).background(accent.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = accent)
            }
        }
    }
}

@Composable
private fun stringResource(id: Int): String = androidx.compose.ui.res.stringResource(id)

private val whyEarlyMarkers = WhyContent(
    title = "Why these questions?",
    plain = "The camera can measure where your child looks, but it cannot hear how they cry, " +
        "feel how they react to being held, or know what they could do last month. You can. " +
        "These questions cover exactly that.",
    technical = "Markers are counted rather than scored. A reported loss of a previously " +
        "held skill, or two or more key markers together, raises a referral signal on its " +
        "own instead of being averaged in.",
    evidence = "Comprehensive Developmental Guide for Early Autism Identification in the " +
        "Indian Population, including its parent, Anganwadi worker and health worker " +
        "monitoring tables.",
    limitation = "This is a first-stage screen. Most children with a marker or two will not " +
        "have autism. It exists to decide who should be looked at more closely, by someone " +
        "qualified to do so.",
)

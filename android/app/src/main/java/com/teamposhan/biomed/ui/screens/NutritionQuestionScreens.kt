package com.teamposhan.biomed.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.teamposhan.biomed.ui.components.GuideArt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teamposhan.biomed.R
import com.teamposhan.biomed.data.model.ResponseType
import com.teamposhan.biomed.domain.age.AgeCalculator
import com.teamposhan.biomed.domain.milestones.AgeBand
import com.teamposhan.biomed.domain.nutrition.NutritionQuestion
import com.teamposhan.biomed.domain.nutrition.NutritionQuestionBank
import com.teamposhan.biomed.domain.nutrition.NutritionResult
import com.teamposhan.biomed.domain.nutrition.RiskPolarity
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
 * Feeding-practice questions for the child's current age band.
 *
 * Same four answers and the same one-question-at-a-time rhythm as the milestone
 * questionnaire, because a caregiver should not have to learn a second way of answering
 * halfway through a visit.
 */
@Composable
fun NutritionQuestionScreen(
    ageMonths: Float,
    answers: Map<String, ResponseType>,
    onAnswer: (NutritionQuestion, ResponseType) -> Unit,
    onFinish: () -> Unit,
) {
    val questions = remember(ageMonths) { NutritionQuestionBank.forAge(ageMonths) }
    var index by remember { mutableIntStateOf(0) }
    val c = riskColors

    if (questions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No nutrition questions for this age.")
        }
        return
    }

    val question = questions[index.coerceIn(0, questions.lastIndex)]
    val current = answers[question.id]

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Feeding",
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
        StatusChip(AgeBand.forAge(ageMonths).label, MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        PoshanProgress(value = (index + 1).toFloat() / questions.size, showPercent = false)
        Spacer(Modifier.height(18.dp))

        AnimatedContent(
            targetState = question.id,
            transitionSpec = {
                (slideInHorizontally(spring(stiffness = 400f)) { it / 6 } + fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(tween(180)) { -it / 8 } + fadeOut(tween(120)))
            },
            label = "nutrition-question",
        ) { id ->
            val q = questions.firstOrNull { it.id == id } ?: question
            PoshanCard(contentPadding = PaddingValues(0.dp)) {
                // The picture carries the question for caregivers who read slowly, so it
                // sits above the text at full card width rather than beside it.
                NutritionArt.forQuestion(q.id)?.let { art ->
                    Image(
                        painter = painterResource(art),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.4f)
                            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(Modifier.padding(20.dp)) {
                    Text(q.text, style = MaterialTheme.typography.titleMedium)
                    q.rationale?.let {
                        Gap(10)
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

        NutritionAnswers(question, current, ::choose)

        Spacer(Modifier.height(10.dp))
        WhyButton(whyNutrition, label = "Why these questions?")
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

/**
 * Background and risk factors, asked once per child rather than at every visit.
 *
 * Presented as a scrolling form grouped by theme rather than one question at a time: these
 * are quick factual answers about circumstances the caregiver already knows, and paging
 * through nineteen of them individually would be tedious where paging through feeding
 * questions is not.
 */
@Composable
fun NutritionBackgroundScreen(
    answers: Map<String, ResponseType>,
    onAnswer: (NutritionQuestion, ResponseType) -> Unit,
    onFinish: () -> Unit,
) {
    val sections = remember { NutritionQuestionBank.background }
    val answered = sections.flatMap { it.questions }.count { answers[it.id] != null }
    val total = NutritionQuestionBank.backgroundCount

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            Gap(8)
            Text("Background", style = MaterialTheme.typography.headlineSmall)
            Gap(4)
            Text(
                "Asked once, not at every visit.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Gap(12)
            PoshanProgress(value = if (total == 0) 0f else answered.toFloat() / total, showPercent = false)
            Gap(8)
            InfoBanner(
                "These are about your household and circumstances, not about anything you " +
                    "have done. They help the health worker know what support to offer.",
            )
            Gap(16)

            sections.forEach { section ->
                SectionLabel(section.title)
                Gap(8)
                section.questions.forEach { question ->
                    BackgroundRow(
                        question = question,
                        current = answers[question.id],
                        onAnswer = { onAnswer(question, it) },
                    )
                    Gap(8)
                }
                Gap(10)
            }

            WhyButton(whyBackground, label = "Why is this asked?")
            Gap(20)
        }
        Box(Modifier.padding(20.dp)) {
            PoshanPrimaryButton("Save", onClick = onFinish)
        }
    }
}

/** Result of the nutrition questionnaire. */
@Composable
fun NutritionResultScreen(
    ageMonths: Float,
    answers: Map<String, ResponseType>,
    onDone: () -> Unit,
) {
    val practice = remember(ageMonths) { NutritionQuestionBank.forAge(ageMonths) }
    val background = remember { NutritionQuestionBank.backgroundQuestions }
    val result = remember(practice, background, answers) {
        NutritionResult.evaluate(practice, background, answers)
    }
    val c = riskColors
    val accent = when (result.level) {
        NutritionResult.Level.ADEQUATE -> c.low
        NutritionResult.Level.SOME_GAPS -> c.monitor
        NutritionResult.Level.NEEDS_SUPPORT -> c.review
        NutritionResult.Level.NOT_ASSESSED -> MaterialTheme.colorScheme.outline
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
        ) {
            Text("Feeding", style = MaterialTheme.typography.headlineSmall)
            Gap(14)

            PoshanCard(contentPadding = PaddingValues(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp).background(accent, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        result.level.label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                }
                Gap(10)
                Text(result.level.guidance, style = MaterialTheme.typography.bodyMedium)
                Gap(12)
                Text(
                    "${result.practiceAnswered} of ${result.practiceTotal} feeding questions answered",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (result.concerns.isNotEmpty()) {
                Gap(16)
                SectionLabel("To discuss")
                Gap(6)
                result.concerns.forEach { concern ->
                    PoshanCard(
                        modifier = Modifier.padding(bottom = 8.dp),
                        contentPadding = PaddingValues(14.dp),
                    ) {
                        Text(concern, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Gap(14)
            InfoBanner(
                "The measurements — weight, height and arm circumference against the WHO " +
                    "growth standards — are what say how this child is actually growing. " +
                    "These questions say what to talk about.",
            )
            Gap(20)
        }
        Box(Modifier.padding(20.dp)) {
            PoshanPrimaryButton("Done", onClick = onDone)
        }
    }
}

// ---------------------------------------------------------------------------

@Composable
private fun NutritionAnswers(
    question: NutritionQuestion,
    current: ResponseType?,
    onChoose: (ResponseType) -> Unit,
) {
    val c = riskColors
    // "Yes" is reassuring for a practice question and concerning for a risk factor. The
    // colour follows the meaning; the scoring inversion lives on the question itself.
    val yesAccent = if (question.polarity == RiskPolarity.PRACTICE) c.low else c.review
    val noAccent = if (question.polarity == RiskPolarity.PRACTICE) c.review else c.low

    NutritionOption(stringResource(R.string.response_yes), Icons.Filled.Check, yesAccent, current == ResponseType.YES_OBSERVED) {
        onChoose(ResponseType.YES_OBSERVED)
    }
    Gap(10)
    NutritionOption(stringResource(R.string.response_maybe), Icons.Filled.Remove, c.monitor, current == ResponseType.MAYBE) {
        onChoose(ResponseType.MAYBE)
    }
    Gap(10)
    NutritionOption(stringResource(R.string.response_no), Icons.Filled.Close, noAccent, current == ResponseType.NOT_YET) {
        onChoose(ResponseType.NOT_YET)
    }
    Gap(10)
    NutritionOption(stringResource(R.string.response_not_sure), Icons.Filled.HelpOutline, c.priority, current == ResponseType.NOT_SURE) {
        onChoose(ResponseType.NOT_SURE)
    }
}

@Composable
private fun NutritionOption(
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

/** A compact yes/no row, for the background form where paging would be tedious. */
@Composable
private fun BackgroundRow(
    question: NutritionQuestion,
    current: ResponseType?,
    onAnswer: (ResponseType) -> Unit,
) {
    val haptics = rememberAppHaptics()
    val c = riskColors
    PoshanCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Ids are NUT_BG_1..19 and the drawables are nut_bg_1..19, so the key is just
            // the id lowercased. Missing art collapses to nothing rather than a grey box —
            // these arrive in batches and a half-delivered set should not look broken.
            GuideArt(
                artKey = question.id.lowercase(),
                modifier = Modifier.width(72.dp),
                ratio = 1f,
                placeholderWhenMissing = false,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                question.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }
        Gap(10)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                ResponseType.YES_OBSERVED to stringResource(R.string.response_yes),
                ResponseType.NOT_YET to stringResource(R.string.response_no),
                ResponseType.NOT_SURE to stringResource(R.string.response_not_sure),
            ).forEach { (type, label) ->
                val selected = current == type
                val accent = when {
                    type == ResponseType.NOT_SURE -> c.priority
                    question.isConcerning(type) -> c.review
                    else -> c.low
                }
                Box(
                    Modifier
                        .weight(1f)
                        .background(
                            if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                            MaterialTheme.shapes.small,
                        )
                        .clickable { haptics.click(); onAnswer(type) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

private val whyNutrition = WhyContent(
    title = "Why these feeding questions?",
    plain = "What a child should be eating changes completely across the first five years — " +
        "milk only at first, then soft foods from about six months, then family food. The " +
        "questions change with your child's age for that reason.",
    technical = "Question sets follow the WHO complementary-feeding guidance: complementary " +
        "foods from about 6 months alongside continued breastfeeding through 23 months, " +
        "3–4 meals a day for 9–23 month olds with nutritious snacks as needed, and " +
        "increasing variety and food consistency with age.",
    limitation = "Answers describe feeding practice. How your child is actually growing " +
        "comes from the weight, height and arm measurements, not from these questions.",
)

private val whyBackground = WhyContent(
    title = "Why ask about our household?",
    plain = "Things like water, cooking fuel and a mother's health are known to affect " +
        "children's nutrition across whole communities. Knowing them helps the health " +
        "worker offer the right support.",
    technical = "These factors are associated with undernutrition risk at population level. " +
        "They are asked once per child rather than at every visit, because they rarely " +
        "change between visits.",
    limitation = "These are not a prediction about your child. A household can have several " +
        "of these factors and a perfectly well-nourished child, and the reverse is also true.",
)

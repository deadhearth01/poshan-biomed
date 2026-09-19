package com.teamposhan.biomed.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.teamposhan.biomed.domain.growth.MuacTrigger
import androidx.compose.material3.TextButton
import com.teamposhan.biomed.ui.components.GuideArt
import com.teamposhan.biomed.ui.components.GuidedStepsScreen
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teamposhan.biomed.R
import com.teamposhan.biomed.data.model.AssessmentAnswer
import com.teamposhan.biomed.data.model.AssessmentCategory
import com.teamposhan.biomed.data.model.CategoryResult
import com.teamposhan.biomed.data.model.DevelopmentalQuotient
import com.teamposhan.biomed.data.model.DomainStatus
import com.teamposhan.biomed.data.model.GrowthMeasurement
import com.teamposhan.biomed.data.model.GrowthZ
import com.teamposhan.biomed.data.model.NutritionData
import com.teamposhan.biomed.data.model.ResponseType
import com.teamposhan.biomed.domain.milestones.MilestoneQuestion
import com.teamposhan.biomed.ui.components.DomainChip
import com.teamposhan.biomed.ui.components.Gap
import com.teamposhan.biomed.ui.components.InfoBanner
import com.teamposhan.biomed.ui.components.PoshanCard
import com.teamposhan.biomed.ui.components.PoshanOutlinedButton
import com.teamposhan.biomed.ui.components.PoshanPrimaryButton
import com.teamposhan.biomed.ui.components.PoshanProgress
import com.teamposhan.biomed.ui.components.PoshanTextField
import com.teamposhan.biomed.ui.components.SectionLabel
import com.teamposhan.biomed.ui.components.SmallIconButton
import com.teamposhan.biomed.ui.components.StatTile
import com.teamposhan.biomed.ui.haptics.rememberAppHaptics
import com.teamposhan.biomed.ui.theme.FieldCorner
import com.teamposhan.biomed.ui.theme.riskColors

@Composable
fun categoryName(category: AssessmentCategory): String = when (category) {
    AssessmentCategory.GROSS_MOTOR -> stringResource(R.string.cat_gross_motor)
    AssessmentCategory.FINE_MOTOR -> stringResource(R.string.cat_fine_motor)
    AssessmentCategory.COMMUNICATION -> stringResource(R.string.cat_communication)
    AssessmentCategory.SOCIAL_EMOTIONAL -> stringResource(R.string.cat_social_emotional)
    AssessmentCategory.NUTRITION -> stringResource(R.string.cat_nutrition)
}

@Composable
fun categoryDesc(category: AssessmentCategory): String = when (category) {
    AssessmentCategory.GROSS_MOTOR -> stringResource(R.string.cat_gross_motor_desc)
    AssessmentCategory.FINE_MOTOR -> stringResource(R.string.cat_fine_motor_desc)
    AssessmentCategory.COMMUNICATION -> stringResource(R.string.cat_communication_desc)
    AssessmentCategory.SOCIAL_EMOTIONAL -> stringResource(R.string.cat_social_emotional_desc)
    AssessmentCategory.NUTRITION -> stringResource(R.string.cat_nutrition_desc)
}

// ---------------------------------------------------------------------------

@Composable
fun AssessmentIntroScreen(childName: String, ageLabel: String, onStart: () -> Unit) {
    // Same shape as modules 2 and 3: what this is, then how to do it, then start.
    var showGuide by remember { mutableStateOf(false) }
    if (showGuide) {
        GuidedStepsScreen(
            title = "Before you start",
            steps = GuideContent.module1,
            why = GuideContent.whyModule1,
            finishLabel = "Start",
            onFinish = onStart,
            onExit = { showGuide = false },
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            GuideArt("hero_module_1", ratio = 1.9f)
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.assessment_introduction), style = MaterialTheme.typography.headlineSmall)
            Gap(6)
            Text("$childName · $ageLabel", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            PoshanCard {
                Text(stringResource(R.string.assessment_intro_body), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetaPill("~10 min")
                    MetaPill("5 categories")
                    MetaPill("Offline")
                }
            }
            Spacer(Modifier.height(18.dp))
            SectionLabel("What we check")
            listOf(
                AssessmentCategory.GROSS_MOTOR,
                AssessmentCategory.FINE_MOTOR,
                AssessmentCategory.COMMUNICATION,
                AssessmentCategory.SOCIAL_EMOTIONAL,
                AssessmentCategory.NUTRITION,
            ).forEachIndexed { index, cat ->
                CategoryArtRow(index + 1, cat)
                Gap(10)
            }
            Spacer(Modifier.height(8.dp))
            InfoBanner(stringResource(R.string.screening_not_diagnosis))
        }
        Box(Modifier.padding(20.dp)) {
            PoshanPrimaryButton(
                "Show me how, then start",
                onClick = { showGuide = true },
                icon = Icons.Filled.Check,
            )
        }
    }
}

@Composable
private fun MetaPill(text: String) {
    Box(
        Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) { Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun CategoryArtRow(number: Int, cat: AssessmentCategory) {
    PoshanCard(contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(MilestoneArt.forCategory(cat.name)),
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("$number. ${categoryName(cat)}", style = MaterialTheme.typography.titleMedium)
                Text(categoryDesc(cat), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun CategoryOverviewScreen(
    categories: List<CategoryResult>,
    nutritionDone: Boolean,
    onOpenCategory: (AssessmentCategory) -> Unit,
    onOpenNutrition: () -> Unit,
    onReview: () -> Unit,
) {
    val totalDone = categories.count { it.status != DomainStatus.NOT_ASSESSED } + if (nutritionDone) 1 else 0
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(stringResource(R.string.category_overview), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            PoshanProgress(value = totalDone / 5f, label = "$totalDone of 5 completed")
            Spacer(Modifier.height(20.dp))
            categories.forEach { result ->
                CategoryProgressRow(
                    cat = result.category,
                    progress = "${result.answered}/${result.total}",
                    status = result.status,
                    onClick = { onOpenCategory(result.category) },
                )
                Gap(10)
            }
            CategoryProgressRow(
                cat = AssessmentCategory.NUTRITION,
                progress = if (nutritionDone) "Done" else "",
                status = if (nutritionDone) DomainStatus.ON_TRACK else DomainStatus.NOT_ASSESSED,
                onClick = onOpenNutrition,
            )
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.padding(20.dp)) {
            PoshanPrimaryButton(stringResource(R.string.assessment_review), onClick = onReview, icon = Icons.Filled.Check)
        }
    }
}

@Composable
private fun CategoryProgressRow(
    cat: AssessmentCategory,
    progress: String,
    status: DomainStatus,
    onClick: () -> Unit,
) {
    PoshanCard(onClick = onClick, contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(MilestoneArt.forCategory(cat.name)),
                contentDescription = null,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(categoryName(cat), style = MaterialTheme.typography.titleMedium)
                Text(
                    if (progress.isNotEmpty()) "$progress answered" else categoryDesc(cat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DomainChip(status)
        }
    }
}

@Composable
fun CategoryIntroScreen(category: AssessmentCategory, questionCount: Int, onStart: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Image(
                painter = painterResource(MilestoneArt.forCategory(category.name)),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(28.dp)),
            )
            Spacer(Modifier.height(18.dp))
            Text(categoryName(category), style = MaterialTheme.typography.headlineSmall)
            Gap(6)
            Text(categoryDesc(category), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Questions", "$questionCount", Modifier.weight(1f))
                StatTile("Time", "~${questionCount * 30 / 60 + 1} min", Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            InfoBanner("Try the activity with your child if possible. Not a good day? Choose \"Could not try today\".")
        }
        Box(Modifier.padding(20.dp)) {
            PoshanPrimaryButton(stringResource(R.string.start_category), onClick = onStart)
        }
    }
}

@Composable
fun QuestionScreen(
    category: AssessmentCategory,
    questions: List<MilestoneQuestion>,
    answersById: Map<String, AssessmentAnswer>,
    /** The child's own age band; anything older than this is a catch-up question. */
    currentBand: com.teamposhan.biomed.domain.milestones.AgeBand? = null,
    onAnswer: (MilestoneQuestion, ResponseType) -> Unit,
    onFinish: () -> Unit,
) {
    var index by remember { mutableIntStateOf(0) }
    if (questions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.error_generic)) }
        return
    }
    val question = questions[index.coerceIn(0, questions.lastIndex)]
    val current = answersById[question.id]?.response

    // Scrollable: with four response options, the navigation row and a tall question card,
    // the content exceeds a short screen and the last options were unreachable.
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(categoryName(category), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${index + 1} / ${questions.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        // Which age band this milestone belongs to. Earlier bands are marked as catch-up
        // so the caregiver understands why they are being asked about younger skills.
        val isCatchUp = currentBand != null && question.band.minMonths < currentBand.minMonths
        com.teamposhan.biomed.ui.components.StatusChip(
            if (isCatchUp) "Catch-up · ${question.band.label}" else question.band.label,
            if (isCatchUp) riskColors.monitor else MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(10.dp))
        PoshanProgress(value = (index + 1).toFloat() / questions.size, showPercent = false)
        Spacer(Modifier.height(18.dp))

        AnimatedContent(
            targetState = question.id,
            transitionSpec = {
                (slideInHorizontally(spring(stiffness = 400f)) { it / 6 } + fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(tween(180)) { -it / 8 } + fadeOut(tween(120)))
            },
            label = "question",
        ) { id ->
            val q = questions.firstOrNull { it.id == id } ?: question
            Column {
                PoshanCard(contentPadding = PaddingValues(0.dp)) {
                    val art = MilestoneArt.forQuestion(q.id) ?: MilestoneArt.forCategory(q.category.name)
                    Image(
                        painter = painterResource(art),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1.35f).clip(
                            RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        ),
                    )
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(q.text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                            ReadAloudButton(q.text)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))

        fun choose(option: ResponseType) {
            onAnswer(question, option)
            if (index < questions.lastIndex) index++ else onFinish()
        }

        ResponseOption(
            stringResource(R.string.response_yes),
            Icons.Filled.Check,
            riskColors.low,
            current == ResponseType.YES_OBSERVED,
        ) { choose(ResponseType.YES_OBSERVED) }
        Gap(10)
        ResponseOption(
            stringResource(R.string.response_maybe),
            Icons.Filled.Remove,
            riskColors.monitor,
            current == ResponseType.MAYBE,
        ) { choose(ResponseType.MAYBE) }
        Gap(10)
        ResponseOption(
            stringResource(R.string.response_no),
            Icons.Filled.Close,
            riskColors.review,
            current == ResponseType.NOT_YET,
        ) { choose(ResponseType.NOT_YET) }
        Gap(10)
        ResponseOption(
            stringResource(R.string.response_not_sure),
            Icons.Filled.HelpOutline,
            riskColors.priority,
            current == ResponseType.NOT_SURE,
        ) { choose(ResponseType.NOT_SURE) }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            com.teamposhan.biomed.ui.components.WhyButton(
                com.teamposhan.biomed.ui.components.WhyLibrary.notSureIsFlagged,
                label = "About these answers",
            )
            if (isCatchUp) {
                com.teamposhan.biomed.ui.components.WhyButton(
                    com.teamposhan.biomed.ui.components.WhyLibrary.catchUpQuestions,
                    label = "Why this age?",
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            com.teamposhan.biomed.ui.components.TextLink("Previous", enabled = index > 0) { if (index > 0) index-- }
            com.teamposhan.biomed.ui.components.TextLink(stringResource(R.string.category_review)) { onFinish() }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ResponseOption(label: String, icon: ImageVector, accent: Color, selected: Boolean, onClick: () -> Unit) {
    val haptics = rememberAppHaptics()
    val c = riskColors
    val container = if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerLow
    PoshanCard(
        onClick = {
            if (accent == c.priority) haptics.reject() else haptics.click()
            onClick()
        },
        container = container,
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accent)
            Spacer(Modifier.width(14.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = accent)
        }
    }
}

@Composable
fun CategoryReviewScreen(
    category: AssessmentCategory,
    questions: List<MilestoneQuestion>,
    answersById: Map<String, AssessmentAnswer>,
    onEdit: () -> Unit,
    onContinue: () -> Unit,
) {
    val answered = questions.count { answersById[it.id]?.response != null }
    val concerns = questions.count {
        val r = answersById[it.id]?.response
        r == ResponseType.NOT_YET || r == ResponseType.NOT_SURE
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(stringResource(R.string.category_review), style = MaterialTheme.typography.headlineSmall)
            Text(categoryName(category), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Answered", "$answered/${questions.size}", Modifier.weight(1f))
                StatTile("Concerns", concerns.toString(), Modifier.weight(1f),
                    accent = if (concerns > 0) riskColors.monitor else riskColors.low)
            }
            Spacer(Modifier.height(18.dp))
            questions.forEach { q ->
                val r = answersById[q.id]?.response
                PoshanCard(modifier = Modifier.padding(bottom = 10.dp), contentPadding = PaddingValues(16.dp)) {
                    Text(q.text, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        responseLabel(r),
                        style = MaterialTheme.typography.labelLarge,
                        color = when (r) {
                            ResponseType.YES_OBSERVED -> riskColors.low
                            ResponseType.NOT_YET -> riskColors.priority
                            ResponseType.NOT_SURE -> riskColors.monitor
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
        Box(Modifier.padding(20.dp)) {
            Column {
                PoshanOutlinedButton(stringResource(R.string.change_answer), onClick = onEdit)
                Gap(10)
                PoshanPrimaryButton(stringResource(R.string.continue_label), onClick = onContinue)
            }
        }
    }
}

@Composable
private fun responseLabel(r: ResponseType?): String = when (r) {
    ResponseType.YES_OBSERVED -> stringResource(R.string.response_achieved)
    ResponseType.NOT_YET -> stringResource(R.string.response_not_achieved)
    ResponseType.NOT_SURE -> stringResource(R.string.response_not_sure)
    ResponseType.MAYBE -> stringResource(R.string.response_maybe)
    null -> stringResource(R.string.response_not_observed)
}

// ---------------------------------------------------------------------------
// Nutrition
// ---------------------------------------------------------------------------

/**
 * The arm field when it has not been prompted.
 *
 * Present but quiet. MUAC is an independent criterion for severe acute malnutrition, so a
 * worker who wants to record it must always be able to — the growth trigger changes whether
 * the app *asks*, never whether it *accepts*.
 */
@Composable
private fun MuacOptionalField(value: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(value.isNotBlank()) }
    if (!expanded) {
        TextButton(onClick = { expanded = true }) {
            Text("Record the arm measurement anyway")
        }
    } else {
        MeasurementGuideCard(MeasurementHowTo.muac)
        Spacer(Modifier.height(10.dp))
        PoshanTextField(
            value, onChange, "Arm circumference",
            suffix = "mm",
            keyboardType = KeyboardType.Number,
            supportingText = "Optional. Under 115 mm is severe, 115-125 mm is moderate",
        )
    }
}

@Composable
fun NutritionScreen(
    ageMonths: Float,
    sex: com.teamposhan.biomed.data.model.Sex,
    initial: NutritionData?,
    measurement: GrowthMeasurement?,
    onSave: (NutritionData, GrowthMeasurement) -> Unit,
) {
    var weight by remember { mutableStateOf(measurement?.weightKg?.toString() ?: "") }
    var height by remember { mutableStateOf(measurement?.heightCm?.toString() ?: "") }
    var muac by remember { mutableStateOf(measurement?.muacMm?.toString() ?: "") }
    var meals by remember { mutableStateOf(initial?.mealsPerDay?.toString() ?: "") }
    var feedingStatus by remember { mutableStateOf(initial?.feedingStatus ?: "Breastfeeding") }
    var difficulty by remember { mutableStateOf(initial?.feedingDifficulty ?: false) }
    var illness by remember { mutableStateOf(initial?.recentIllness ?: false) }
    var access by remember { mutableStateOf(initial?.foodAccessConcern ?: false) }
    val groups = remember { mutableStateOf(initial?.foodGroups?.toSet() ?: emptySet()) }

    val lengthLabel = com.teamposhan.biomed.domain.growth.GrowthStandards.measurementLabel(ageMonths)
    val lengthHint = com.teamposhan.biomed.domain.growth.GrowthStandards.measurementHint(ageMonths)
    // The arm tape is prompted by the growth numbers, not asked for on every child. See
    // MuacTrigger for why, and for the sensitivity that trade costs.
    val muacDecision = remember(sex, ageMonths, weight, height) {
        MuacTrigger.evaluate(sex, ageMonths, weight.toFloatOrNull(), height.toFloatOrNull())
    }
    val muacApplicable = muacDecision !is MuacTrigger.Decision.OutOfAgeRange

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Image(
                painter = painterResource(R.drawable.illus_cat_nutrition),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(2.0f).clip(RoundedCornerShape(28.dp)),
            )
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.cat_nutrition), style = MaterialTheme.typography.headlineSmall)
            Text(categoryDesc(AssessmentCategory.NUTRITION), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))

            SectionLabel(stringResource(R.string.growth_measurements))

            // --- weight ---
            MeasurementGuideCard(MeasurementHowTo.weight)
            Spacer(Modifier.height(10.dp))
            PoshanTextField(weight, { weight = it }, stringResource(R.string.weight), suffix = "kg", keyboardType = KeyboardType.Decimal)

            Spacer(Modifier.height(18.dp))

            // --- length / height ---
            MeasurementGuideCard(MeasurementHowTo.length(ageMonths))
            Spacer(Modifier.height(10.dp))
            PoshanTextField(
                height, { height = it }, lengthLabel,
                suffix = "cm",
                keyboardType = KeyboardType.Decimal,
                supportingText = lengthHint,
            )

            Spacer(Modifier.height(18.dp))

            // --- arm circumference, prompted by the growth numbers ---
            when (val d = muacDecision) {
                is MuacTrigger.Decision.OutOfAgeRange -> Unit

                is MuacTrigger.Decision.NeedsWeightAndHeight -> {
                    InfoBanner(
                        "Enter weight and height first. If either is low for this age, the " +
                            "app will ask for the arm measurement as well.",
                    )
                    Spacer(Modifier.height(10.dp))
                    MuacOptionalField(muac) { muac = it }
                }

                is MuacTrigger.Decision.NotNeeded -> {
                    InfoBanner(
                        "Weight and height are both in the normal range " +
                            "(lowest %+.1f SD), so the arm measurement is not needed today."
                                .format(d.worstZ),
                    )
                    Spacer(Modifier.height(10.dp))
                    MuacOptionalField(muac) { muac = it }
                }

                is MuacTrigger.Decision.Required -> {
                    PoshanCard(
                        container = riskColors.reviewSoft,
                        contentPadding = PaddingValues(16.dp),
                    ) {
                        Text(
                            "Please measure the arm",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Gap(6)
                        Text(
                            "This child's %s is %+.1f SD, which is below the normal range. " .format(d.trigger, d.worstZ) +
                                "The arm measurement checks for wasting and decides whether " +
                                "they need referral today.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    MeasurementGuideCard(MeasurementHowTo.muac)
                    Spacer(Modifier.height(10.dp))
                    PoshanTextField(
                        muac, { muac = it }, "Arm circumference",
                        suffix = "mm",
                        keyboardType = KeyboardType.Number,
                        supportingText = "Under 115 mm is severe, 115-125 mm is moderate",
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel(stringResource(R.string.feeding_nutrition_questions))
            Text("Feeding", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Breastfeeding", "Mixed", "Family food").forEach { opt ->
                    FilterChip(selected = feedingStatus == opt, onClick = { feedingStatus = opt }, label = { Text(opt) })
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Food groups eaten yesterday", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            val allGroups = listOf("Grains", "Dal/Legumes", "Milk", "Eggs", "Meat/Fish", "Vegetables", "Fruits", "Fats/Oil")
            allGroups.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { g ->
                        FilterChip(
                            selected = groups.value.contains(g),
                            onClick = {
                                groups.value = if (groups.value.contains(g)) groups.value - g else groups.value + g
                            },
                            label = { Text(g) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(8.dp))
            PoshanTextField(meals, { meals = it }, "Meals per day", keyboardType = KeyboardType.Number, filter = { input -> input.filter { c -> c.isDigit() }.take(2) })
            Spacer(Modifier.height(16.dp))
            ToggleRow("Feeding difficulty", difficulty) { difficulty = it }
            ToggleRow("Illness in the last 2 weeks", illness) { illness = it }
            ToggleRow("Difficulty accessing food", access) { access = it }
            Spacer(Modifier.height(16.dp))
            InfoBanner(stringResource(R.string.screening_not_diagnosis))
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.padding(20.dp)) {
            PoshanPrimaryButton(stringResource(R.string.save)) {
                onSave(
                    NutritionData(
                        feedingStatus = feedingStatus,
                        foodGroups = groups.value.toList(),
                        mealsPerDay = meals.toIntOrNull(),
                        feedingDifficulty = difficulty,
                        recentIllness = illness,
                        foodAccessConcern = access,
                        dietaryDiversityScore = groups.value.size,
                    ),
                    GrowthMeasurement(
                        weightKg = weight.toFloatOrNull(),
                        heightCm = height.toFloatOrNull(),
                        muacMm = muac.toFloatOrNull(),
                    ),
                )
            }
        }
    }
}

/** Illustration + numbered steps for a measurement, so field workers get it right. */
@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun GrowthResultCard(z: GrowthZ?) {
    if (z == null) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile("Weight/Age", z.weightForAge?.let { "%.1f SD".format(it) } ?: "—", Modifier.weight(1f))
        StatTile("Height/Age", z.heightForAge?.let { "%.1f SD".format(it) } ?: "—", Modifier.weight(1f))
        StatTile("BMI/Age", z.bmiForAge?.let { "%.1f SD".format(it) } ?: "—", Modifier.weight(1f))
    }
}

@Composable
fun AssessmentReviewScreen(
    categories: List<CategoryResult>,
    nutrition: NutritionData?,
    growthZ: GrowthZ?,
    dq: DevelopmentalQuotient?,
    onSubmit: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(stringResource(R.string.assessment_review), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            categories.forEach { c ->
                PoshanCard(modifier = Modifier.padding(bottom = 10.dp), contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(categoryName(c.category), style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${c.answered}/${c.total} answered · ${c.concerns} concerns",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DomainChip(c.status)
                    }
                    if (c.redFlags.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Red flags: " + c.redFlags.joinToString(), style = MaterialTheme.typography.bodySmall, color = riskColors.priority)
                    }
                }
            }
            PoshanCard(modifier = Modifier.padding(bottom = 10.dp), contentPadding = PaddingValues(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(categoryName(AssessmentCategory.NUTRITION), style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (nutrition != null) "Dietary diversity ${nutrition.dietaryDiversityScore}/8" else "Not entered",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DomainChip(if (nutrition != null) DomainStatus.ON_TRACK else DomainStatus.NOT_ASSESSED)
                }
            }
            Spacer(Modifier.height(12.dp))
            SectionLabel(stringResource(R.string.growth_charts))
            GrowthResultCard(growthZ)
            Spacer(Modifier.height(16.dp))
            dq?.let {
                PoshanCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.calculate_dq), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.dq_formula), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Developmental age ${"%.1f".format(it.developmentalAgeMonths)} mo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("${it.dq.toInt()}%", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.padding(20.dp)) {
            PoshanPrimaryButton(stringResource(R.string.submit_assessment), onClick = onSubmit, icon = Icons.Filled.Check)
        }
    }
}

// ---------------------------------------------------------------------------
// Read aloud
// ---------------------------------------------------------------------------

@Composable
private fun ReadAloudButton(text: String) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { }
        tts = engine
        onDispose { engine.stop(); engine.shutdown() }
    }
    SmallIconButton(Icons.Filled.VolumeUp) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "q")
    }
}

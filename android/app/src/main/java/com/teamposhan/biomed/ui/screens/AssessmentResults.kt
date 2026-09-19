package com.teamposhan.biomed.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.teamposhan.biomed.data.model.AttentionResult
import com.teamposhan.biomed.data.model.CategoryResult
import com.teamposhan.biomed.data.model.ChildProfile
import com.teamposhan.biomed.data.model.CompositeRisk
import com.teamposhan.biomed.data.model.DevelopmentalQuotient
import com.teamposhan.biomed.data.model.DomainStatus
import com.teamposhan.biomed.data.model.GrowthZ
import com.teamposhan.biomed.data.model.RiskBand
import com.teamposhan.biomed.domain.age.AgeCalculator
import com.teamposhan.biomed.domain.risk.ProphylaxisStatus
import com.teamposhan.biomed.domain.skin.SkinType
import com.teamposhan.biomed.domain.skin.SunExposureFactor
import com.teamposhan.biomed.ui.components.DomainChip
import com.teamposhan.biomed.ui.components.Gap
import com.teamposhan.biomed.ui.components.GuideArt
import com.teamposhan.biomed.ui.components.InfoBanner
import com.teamposhan.biomed.ui.components.PoshanCard
import com.teamposhan.biomed.ui.components.PoshanPrimaryButton
import com.teamposhan.biomed.ui.components.PoshanOutlinedButton
import com.teamposhan.biomed.ui.components.SectionLabel
import com.teamposhan.biomed.ui.haptics.rememberAppHaptics
import com.teamposhan.biomed.ui.theme.riskColors
import kotlin.math.roundToInt

/**
 * What a caregiver sees when a visit is finished.
 *
 * Rewritten around one decision: **the front of this screen carries no raw numbers.** The
 * previous version led with a composite percentage, a DQ percentage and a z-score, which is
 * the wrong information for the person reading it — a parent cannot act on "engagement 43%"
 * and will read any percentage as a mark out of a hundred for their child.
 *
 * So each module gets one plain sentence and a status colour, and every number that produced
 * it lives one tap away in a drawer. Nothing is hidden: the drawers hold more detail than the
 * old screen ever showed, including the arithmetic. It is just not the first thing a worried
 * parent meets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentResultsScreen(
    child: ChildProfile?,
    session: AssessmentSession?,
    liveDq: DevelopmentalQuotient?,
    liveCategories: List<CategoryResult>,
    onReport: () -> Unit,
    onDone: () -> Unit,
) {
    val dq = session?.dq ?: liveDq
    val categories = session?.categoryResults?.takeIf { it.isNotEmpty() } ?: liveCategories
    val composite = session?.composite
    val attention = session?.attention
    val growthZ = session?.growthZ
    val skinIta = session?.skinItaDegrees
    val prophylaxis = runCatching {
        ProphylaxisStatus.valueOf(session?.prophylaxisStatus ?: "NOT_ASKED")
    }.getOrDefault(ProphylaxisStatus.NOT_ASKED)

    var openSheet by remember { mutableStateOf<ResultSheet?>(null) }

    // The result announces itself with a weight matched to what it says. On-track is a light
    // settling; a referral is firmer. The page is often opened by someone anxious about what
    // it will say, and the hand reads it before the eye does.
    val haptics = rememberAppHaptics()
    val band = composite?.band ?: RiskBand.ON_TRACK
    LaunchedEffect(band) {
        haptics.severity(
            when (band) {
                RiskBand.ON_TRACK -> 0
                RiskBand.MONITOR -> 1
                RiskBand.REVIEW -> 2
                RiskBand.PRIORITY -> 3
            }
        )
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {

            HeroVerdict(child = child, band = composite?.band ?: RiskBand.ON_TRACK)

            Gap(22)
            SectionLabel("The three checks")
            Gap(8)

            ModuleResultCard(
                artKey = "micro_module_milestones",
                name = "Growth and everyday skills",
                verdict = milestoneSentence(dq, categories, growthZ),
                status = worstOf(categories),
                onOpen = { openSheet = ResultSheet.MILESTONES },
            )
            Gap(10)
            ModuleResultCard(
                artKey = "micro_module_attention",
                name = "Looking and attention",
                verdict = attentionSentence(attention),
                status = attentionStatus(attention),
                onOpen = { openSheet = ResultSheet.ATTENTION },
            )
            Gap(10)
            ModuleResultCard(
                artKey = "micro_module_vitamind",
                name = "Sunlight and vitamin D",
                verdict = vitaminDSentence(skinIta, prophylaxis, session),
                status = vitaminDStatus(session),
                onOpen = { openSheet = ResultSheet.VITAMIN_D },
            )

            composite?.takeIf { it.referrals.isNotEmpty() }?.let { comp ->
                Gap(22)
                SectionLabel("Worth showing a doctor")
                Gap(8)
                PoshanCard(
                    container = riskColors.reviewSoft,
                    contentPadding = PaddingValues(16.dp),
                ) {
                    comp.referrals.forEach { r ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                            Text("•  ", color = MaterialTheme.colorScheme.primary)
                            Text(r, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Gap(8)
                    Text(
                        "Take this to the Anganwadi centre or health centre. The checks " +
                            "needed there are free.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Gap(20)
            InfoBanner(
                "This is a check, not a diagnosis. It cannot name a condition. What it can " +
                    "do is tell you when it is worth having someone look more closely.",
            )
            Gap(16)
        }

        Box(Modifier.padding(20.dp)) {
            Column {
                PoshanOutlinedButton("Share the report", onClick = onReport, icon = Icons.Filled.Share)
                Gap(10)
                PoshanPrimaryButton("Done", onClick = onDone, icon = Icons.Filled.Check)
            }
        }
    }

    openSheet?.let { which ->
        ModalBottomSheet(
            onDismissRequest = { openSheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                when (which) {
                    ResultSheet.MILESTONES -> MilestoneDetail(dq, categories, growthZ, child)
                    ResultSheet.ATTENTION -> AttentionDetail(attention)
                    ResultSheet.VITAMIN_D -> VitaminDDetail(skinIta, prophylaxis, session)
                }
                Gap(40)
            }
        }
    }
}

internal enum class ResultSheet { MILESTONES, ATTENTION, VITAMIN_D }

// =========================================================================
// Front of the screen
// =========================================================================

/**
 * The one thing the reader should take away, said once and said plainly.
 *
 * No percentage. A band is a decision about what to do next, and that is what is written
 * here; the numbers behind it are in the drawers.
 */
@Composable
private fun HeroVerdict(child: ChildProfile?, band: RiskBand) {
    val (art, headline, body) = when (band) {
        RiskBand.ON_TRACK -> Triple(
            "micro_on_track",
            "Everything looks on track",
            "Nothing here needs acting on today. Come back for the next check when the app " +
                "reminds you.",
        )
        RiskBand.MONITOR -> Triple(
            "micro_watch",
            "A few things to keep an eye on",
            "Nothing urgent. Keep doing what you are doing and check again at the next visit.",
        )
        RiskBand.REVIEW -> Triple(
            "micro_look_closer",
            "Worth a closer look",
            "Some answers are worth showing to a health worker. This does not mean something " +
                "is wrong.",
        )
        RiskBand.PRIORITY -> Triple(
            "micro_see_doctor",
            "Please see a health worker soon",
            "A few of today's answers should be checked by someone who can examine your " +
                "child properly.",
        )
    }
    val tint = bandColour(band)

    PoshanCard(container = tint.copy(alpha = 0.12f), contentPadding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GuideArt(art, modifier = Modifier.size(72.dp), ratio = 1f)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    headline,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                child?.let {
                    Gap(2)
                    Text(
                        "${it.name} · ${AgeCalculator.format(AgeCalculator.assessmentMonths(it))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Gap(12)
        Text(body, style = MaterialTheme.typography.bodyLarge)
    }
}

/** One module, one sentence, one colour, and a way to the numbers. */
@Composable
private fun ModuleResultCard(
    artKey: String,
    name: String,
    verdict: String,
    status: DomainStatus,
    onOpen: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    PoshanCard(onClick = { haptics.click(); onOpen() }, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GuideArt(artKey, modifier = Modifier.size(48.dp), ratio = 1f)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Gap(2)
                Text(
                    verdict,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DomainChip(status)
        }
        Gap(10)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "See the numbers and how they were worked out",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// =========================================================================
// The drawers
// =========================================================================

@Composable
private fun MilestoneDetail(
    dq: DevelopmentalQuotient?,
    categories: List<CategoryResult>,
    growthZ: GrowthZ?,
    child: ChildProfile?,
) {
    Text(
        "Growth and everyday skills",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Gap(14)

    if (dq != null && dq.chronologicalAgeMonths > 0f) {
        BigNumber(
            value = "${dq.dq.roundToInt()}",
            caption = "out of 100",
            meaning = dqMeaning(dq.dq),
            tint = when {
                dq.dq >= 85f -> riskColors.low
                dq.dq >= 70f -> riskColors.monitor
                else -> riskColors.review
            },
        )
        Gap(8)
        Text(
            "This compares the skills your child shows now with what is usual at " +
                "${AgeCalculator.format(dq.chronologicalAgeMonths)}. A child doing the " +
                "skills usual for their age lands around 100.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Gap(18)
    }

    if (categories.any { it.status != DomainStatus.NOT_ASSESSED }) {
        SectionLabel("Each kind of skill")
        Gap(8)
        categories.forEach { c ->
            PoshanCard(
                modifier = Modifier.padding(bottom = 8.dp),
                contentPadding = PaddingValues(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(categoryName(c.category), style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${c.answered} of ${c.total} answered",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DomainChip(c.status)
                }
                if (c.missedEarlier.isNotEmpty()) {
                    Gap(8)
                    Text(
                        "Things most children can do by now:",
                        style = MaterialTheme.typography.labelMedium,
                        color = riskColors.review,
                    )
                    c.missedEarlier.take(4).forEach {
                        Text(
                            "• $it",
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
        }
        Gap(10)
    }

    growthZ?.let { z ->
        SectionLabel("Growth")
        Gap(8)
        ZRow("Weight for age", z.weightForAge)
        ZRow("Height for age", z.heightForAge)
        ZRow("Weight for height", z.bmiForAge)
        ZRow("Arm circumference", z.muacForAge)
        Gap(6)
        Text(
            "Zero means exactly average for children of this age. Between −2 and +2 is the " +
                "normal range that most healthy children sit in.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Gap(16)
    }

    SectionLabel("How this was worked out")
    Gap(8)
    CalcCard(
        title = "Scoring each answer",
        formula = "Yes = 1    Maybe = 0.5    No = 0    Not sure = 0 and flagged",
        explanation = "\"Not sure\" scores the same as \"No\" and also raises a flag. A " +
            "caregiver who cannot say whether their child does an everyday thing has, in " +
            "effect, not seen it.",
    )
    CalcCard(
        title = "The number out of 100",
        formula = "score = (skill age ÷ real age) × 100",
        explanation = "Skill age is built up band by band: for each age band, the share of " +
            "credit earned is multiplied by how many months that band covers, and those are " +
            "added together.",
    )
    CalcCard(
        title = "When a skill counts as late",
        formula = "any red flag → late\n" +
            "anything missed from an earlier age → late\n" +
            "half or more of this age group missed → late\n" +
            "otherwise → some concern, or on track",
        explanation = "A skill missed from an age the child has already passed outranks " +
            "everything else. They are not approaching it — they are already past due for it.",
    )
    CalcCard(
        title = "Growth numbers",
        formula = "z = ((measurement ÷ M)^L − 1) ÷ (L × S)",
        explanation = "L, M and S are published values for every age and sex. The result " +
            "says how far this child sits from the middle of healthy children of the same " +
            "age, counted in standard steps.",
        source = "WHO Child Growth Standards, 2006.",
    )
}

@Composable
private fun AttentionDetail(a: AttentionResult?) {
    Text(
        "Looking and attention",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Gap(14)

    if (a == null || a.inconclusive) {
        PoshanCard(container = riskColors.monitorSoft, contentPadding = PaddingValues(16.dp)) {
            Text(
                "Not enough was seen this time",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Gap(6)
            Text(
                "The camera did not get enough clear views of your child's face. That " +
                    "happens often — the child looked away, moved, or the light was behind " +
                    "them. It says nothing about your child. Try again when they are rested.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Gap(16)
        if (a != null) {
            SectionLabel("What the camera managed to see")
            Gap(8)
            MetricRow("Clear views of the face", "${(a.validFrameRatio * 100).roundToInt()}%")
            MetricRow("Time looking at the screen", "${(a.onScreenRatio * 100).roundToInt()}%")
            Gap(6)
            Text(
                "A session needs a good share of clear views before anything can be read " +
                    "from it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    BigNumber(
        value = "${(a.socialAttentionRatio * 100).roundToInt()}%",
        caption = "of looking time on the face",
        meaning = socialMeaning(a.socialAttentionRatio),
        tint = bandColour(a.band),
    )
    Gap(8)
    Text(
        "Two things were on screen at once: a friendly face and a moving toy. This is the " +
            "share of the time your child chose the face.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Gap(18)
    SectionLabel("The rest of the numbers")
    Gap(8)
    MetricRow("Time looking at the screen", "${(a.onScreenRatio * 100).roundToInt()}%")
    a.responseToNameRatio?.let {
        MetricRow("Turned when their name was called", "${(it * 100).roundToInt()}%")
    }
    MetricRow("Clear views of the face", "${(a.validFrameRatio * 100).roundToInt()}%")
    MetricRow("How long it lasted", "${(a.durationMs / 1000f).roundToInt()} seconds")

    Gap(18)
    SectionLabel("How this was worked out")
    Gap(8)
    CalcCard(
        title = "Which side was being looked at",
        formula = "face landmarks → head direction + eye direction → left half or right half",
        explanation = "The camera finds points on the face and works out which way the head " +
            "and eyes are pointing. That is enough to say which half of the screen is being " +
            "looked at. No picture of your child is kept.",
    )
    CalcCard(
        title = "Share of attention on the face",
        formula = "time on the face ÷ (time on the face + time on the toy)",
        explanation = "Only time when the child was actually looking at the screen counts. " +
            "Looking away is not counted against them.",
    )
    CalcCard(
        title = "When a result is set aside",
        formula = "too few clear views → no result at all",
        explanation = "If the camera could not see the face often enough, the app reports " +
            "nothing rather than reporting something weak. A thin result is worse than none.",
    )
    CalcCard(
        title = "What this cannot do",
        formula = "phone camera ≈ 15-30 pictures per second",
        explanation = "That is far too slow to measure quick eye movements, so the app does " +
            "not claim to. It only reports slow, steady things: which side, and for how " +
            "long. It is never a diagnosis and it never names a condition.",
    )
}

@Composable
private fun VitaminDDetail(
    skinIta: Double?,
    prophylaxis: ProphylaxisStatus,
    session: AssessmentSession?,
) {
    Text(
        "Sunlight and vitamin D",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Gap(14)

    val ita = skinIta?.takeIf { !it.isNaN() }
    val type = SkinType.fromIta(ita)
    if (ita != null && type != null) {
        BigNumber(
            value = "${SunExposureFactor.suggestedMinutes(ita)}",
            caption = "minutes of midday sun",
            meaning = "Skin type ${type.roman} — ${type.itaLabel}",
            tint = riskColors.low,
        )
        Gap(8)
        Text(
            SunExposureFactor.caregiverMessage(ita),
            style = MaterialTheme.typography.bodyMedium,
        )
        Gap(18)
    }

    if (prophylaxis != ProphylaxisStatus.NOT_ASKED) {
        SectionLabel("The daily drops")
        Gap(8)
        MetricRow("Right now", prophylaxis.label)
        Gap(16)
    }

    session?.vitaminD?.factors?.takeIf { it.isNotEmpty() }?.let { notes ->
        SectionLabel("Noted today")
        Gap(8)
        notes.forEach {
            PoshanCard(
                modifier = Modifier.padding(bottom = 8.dp),
                contentPadding = PaddingValues(14.dp),
            ) { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
        Gap(10)
    }

    if (ita != null) {
        // Passing a null measurement shows the formula and the limits without the
        // per-pixel stages, which belong to the moment of capture and are not stored.
        SkinCalculationSection(ita = ita, measurement = null)
    } else {
        SectionLabel("How this works")
        Gap(8)
        CalcCard(
            title = "No skin reading taken",
            formula = "skin tone not measured this visit",
            explanation = "Without it the app cannot suggest how long this child should be " +
                "in the sun. Everything else in this module still applies: the daily drops " +
                "are for every baby under one year, whatever their skin tone.",
        )
    }
}

// =========================================================================
// Small parts
// =========================================================================

/** One number, large, with what it means written underneath in words. */
@Composable
private fun BigNumber(value: String, caption: String, meaning: String, tint: Color) {
    val scale by animateFloatAsState(1f, tween(400), label = "big-number")
    PoshanCard(container = tint.copy(alpha = 0.12f), contentPadding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = tint,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp * scale),
            )
        }
        Gap(4)
        Text(meaning, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

/** A z-score as a small bar with the normal range marked, plus the number. */
@Composable
private fun ZRow(label: String, z: Float?) {
    if (z == null) return
    val tint = when {
        z <= -3f || z >= 3f -> riskColors.priority
        z <= -2f || z >= 2f -> riskColors.review
        z <= -1.5f -> riskColors.monitor
        else -> riskColors.low
    }
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                "%+.1f".format(z),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = tint,
            )
        }
        Gap(4)
        // A plain scale from -4 to +4 with the healthy band shaded and a marker on it, so
        // the position is readable without understanding what a z-score is.
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .then(Modifier),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
            val fraction = ((z + 4f) / 8f).coerceIn(0.02f, 0.98f)
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(10.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(tint))
            }
        }
    }
}

// =========================================================================
// Turning results into sentences
// =========================================================================

private fun worstOf(categories: List<CategoryResult>): DomainStatus = when {
    categories.isEmpty() -> DomainStatus.NOT_ASSESSED
    categories.any { it.status == DomainStatus.DELAYED } -> DomainStatus.DELAYED
    categories.any { it.status == DomainStatus.CONCERN } -> DomainStatus.CONCERN
    categories.any { it.status != DomainStatus.NOT_ASSESSED } -> DomainStatus.ON_TRACK
    else -> DomainStatus.NOT_ASSESSED
}

private fun milestoneSentence(
    dq: DevelopmentalQuotient?,
    categories: List<CategoryResult>,
    growthZ: GrowthZ?,
): String {
    if (categories.none { it.status != DomainStatus.NOT_ASSESSED }) return "Not done yet"
    val late = categories.filter { it.status == DomainStatus.DELAYED }
    val growthLow = (growthZ?.worst ?: 0f) <= -2f
    return when {
        late.isNotEmpty() && growthLow ->
            "Some skills and growth are worth checking"
        late.size == 1 -> "${categoryPlain(late.first())} is worth checking"
        late.size > 1 -> "A few kinds of skill are worth checking"
        growthLow -> "Skills look fine; growth is worth checking"
        categories.any { it.status == DomainStatus.CONCERN } ->
            "Mostly on track, a few to watch"
        else -> "Doing the things usual for this age"
    }
}

private fun categoryPlain(c: CategoryResult): String = when (c.category.name) {
    "GROSS_MOTOR" -> "Moving about"
    "FINE_MOTOR" -> "Using the hands"
    "COMMUNICATION" -> "Talking and understanding"
    "SOCIAL_EMOTIONAL" -> "Playing with people"
    "NUTRITION" -> "Eating"
    else -> "One kind of skill"
}

private fun attentionSentence(a: AttentionResult?): String = when {
    a == null -> "Not done yet"
    a.inconclusive -> "Not enough was seen — can be tried again"
    a.band == RiskBand.ON_TRACK -> "Looked at faces and responded well"
    a.band == RiskBand.MONITOR -> "Looked around as expected, worth repeating"
    else -> "Worth showing to a health worker"
}

private fun attentionStatus(a: AttentionResult?): DomainStatus = when {
    a == null -> DomainStatus.NOT_ASSESSED
    a.inconclusive -> DomainStatus.NOT_ASSESSED
    a.band == RiskBand.ON_TRACK -> DomainStatus.ON_TRACK
    a.band == RiskBand.MONITOR -> DomainStatus.CONCERN
    else -> DomainStatus.DELAYED
}

private fun vitaminDSentence(
    ita: Double?,
    prophylaxis: ProphylaxisStatus,
    session: AssessmentSession?,
): String {
    val band = session?.vitaminD?.band
    val type = SkinType.fromIta(ita)
    val minutes = ita?.takeIf { !it.isNaN() }?.let { SunExposureFactor.suggestedMinutes(it) }
    return when {
        session?.vitaminD == null && ita == null -> "Not done yet"
        band == RiskBand.PRIORITY -> "Something should be looked at today"
        band == RiskBand.REVIEW -> "Higher risk of low vitamin D"
        prophylaxis == ProphylaxisStatus.NOT_GIVING -> "The daily drops have not been started"
        band == RiskBand.MONITOR -> "Some risk of low vitamin D"
        type != null && minutes != null ->
            "Skin type ${type.roman} · about $minutes minutes of midday sun"
        else -> "Daily drops and sunlight are on track"
    }
}

private fun vitaminDStatus(session: AssessmentSession?): DomainStatus =
    when (session?.vitaminD?.band) {
        null -> DomainStatus.NOT_ASSESSED
        RiskBand.ON_TRACK -> DomainStatus.ON_TRACK
        RiskBand.MONITOR -> DomainStatus.CONCERN
        else -> DomainStatus.DELAYED
    }

private fun dqMeaning(dq: Float): String = when {
    dq >= 85f -> "Doing the things usual for this age"
    dq >= 70f -> "A little behind — worth watching"
    else -> "Behind what is usual — worth a closer look"
}

private fun socialMeaning(ratio: Float): String = when {
    ratio >= 0.5f -> "Preferred the face, which is what we hope to see"
    ratio >= 0.35f -> "Shared attention between the face and the toy"
    else -> "Preferred the moving toy over the face"
}

@Composable
private fun bandColour(band: RiskBand): Color = when (band) {
    RiskBand.ON_TRACK -> riskColors.low
    RiskBand.MONITOR -> riskColors.monitor
    RiskBand.REVIEW -> riskColors.review
    RiskBand.PRIORITY -> riskColors.priority
}

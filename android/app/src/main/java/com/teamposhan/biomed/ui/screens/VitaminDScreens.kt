package com.teamposhan.biomed.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.teamposhan.biomed.data.model.ResponseType
import com.teamposhan.biomed.domain.age.AgeCalculator
import com.teamposhan.biomed.domain.risk.AdherenceOutcome
import com.teamposhan.biomed.domain.risk.ProphylaxisAdvice
import com.teamposhan.biomed.domain.risk.ProphylaxisStatus
import com.teamposhan.biomed.domain.risk.RicketsSign
import com.teamposhan.biomed.domain.risk.VitaminDRiskModel
import com.teamposhan.biomed.domain.risk.SunWindow
import com.teamposhan.biomed.domain.risk.VitaminDCounselling
import com.teamposhan.biomed.domain.skin.SkinColorimetry
import com.teamposhan.biomed.domain.skin.SkinType
import com.teamposhan.biomed.domain.skin.SkinMeasurement
import com.teamposhan.biomed.domain.skin.SunExposureFactor
import com.teamposhan.biomed.ui.components.Gap
import com.teamposhan.biomed.ui.components.GuideArt
import com.teamposhan.biomed.ui.components.GuidedStepsScreen
import com.teamposhan.biomed.ui.components.InfoBanner
import com.teamposhan.biomed.ui.components.MicroIcon
import com.teamposhan.biomed.ui.components.PoshanCard
import com.teamposhan.biomed.ui.components.PoshanPrimaryButton
import com.teamposhan.biomed.ui.components.SectionLabel
import com.teamposhan.biomed.ui.components.StatusChip
import com.teamposhan.biomed.ui.components.WhyButton
import com.teamposhan.biomed.ui.components.WhyContent
import com.teamposhan.biomed.ui.haptics.rememberAppHaptics
import com.teamposhan.biomed.ui.theme.accentColors
import com.teamposhan.biomed.ui.theme.riskColors
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Module 3: vitamin D prophylaxis and rickets referral.
 *
 * Replaces the old risk questionnaire. The screen leads with the action — give the drops —
 * because for every infant under one year that action is the same regardless of anything the
 * caregiver answers. The sign check and counselling sit underneath it.
 */
@Composable
fun VitaminDScreen(
    ageMonths: Float,
    latitude: Double?,
    answers: Map<String, ResponseType>,
    prophylaxisStatus: ProphylaxisStatus,
    skinIta: Double?,
    onSkinIta: (Double) -> Unit,
    onProphylaxisStatus: (ProphylaxisStatus) -> Unit,
    onAnswer: (String, ResponseType) -> Unit,
    onSave: () -> Unit,
) {
    // Module 3 is a walkthrough first and a form second. The walkthrough is not optional
    // the first time through: the reading is only meaningful if the arm, the card and the
    // light are right, and none of that is guessable from the capture screen on its own.
    var stage by remember {
        mutableStateOf(
            if (skinIta == null && prophylaxisStatus == ProphylaxisStatus.NOT_ASKED) {
                VitDStage.INTRO
            } else {
                VitDStage.FORM
            }
        )
    }
    var lastMeasurement by remember { mutableStateOf<SkinMeasurement?>(null) }
    var showIncomplete by remember { mutableStateOf(false) }
    val haptics = rememberAppHaptics()

    when (stage) {
        VitDStage.INTRO -> {
            GuidedStepsScreen(
                title = "Vitamin D",
                steps = GuideContent.module3Intro,
                why = GuideContent.whyModule3,
                finishLabel = "Start",
                onFinish = { stage = VitDStage.FORM },
                onExit = { stage = VitDStage.FORM },
            )
            return
        }
        VitDStage.HELP -> {
            GuidedStepsScreen(
                title = "How to do this",
                steps = GuideContent.module3,
                why = GuideContent.whyModule3,
                finishLabel = "Got it",
                onFinish = { stage = VitDStage.FORM },
                onExit = { stage = VitDStage.FORM },
            )
            return
        }
        VitDStage.CAPTURE_GUIDE -> {
            // Shown every time the camera is opened, not once. These seven steps are the
            // difference between a reading and a number, and a walkthrough read earlier in
            // the visit has already been forgotten by the time the card comes out.
            GuidedStepsScreen(
                title = "Skin tone",
                steps = GuideContent.module3Capture,
                why = whySkinTone,
                finishLabel = "Open the camera",
                onFinish = { stage = VitDStage.CAPTURE },
                onExit = { stage = VitDStage.FORM },
            )
            return
        }
        VitDStage.CAPTURE -> {
            // The capture takes over the whole screen rather than living in a dialog:
            // aiming a reticle at a squirming child's arm needs the full viewport.
            SkinCaptureScreen(
                onDone = { m ->
                    lastMeasurement = m
                    onSkinIta(m.reading.itaDegrees)
                    stage = VitDStage.FORM
                },
                onCancel = { stage = VitDStage.FORM },
            )
            return
        }
        VitDStage.FORM -> Unit
    }
    val advice = remember(ageMonths) { ProphylaxisAdvice.forAge(ageMonths) }
    val adherence = remember(prophylaxisStatus, advice) {
        AdherenceOutcome.evaluate(prophylaxisStatus, advice)
    }
    val signs = remember(ageMonths) { RicketsSign.forAge(ageMonths) }
    val counselling = remember(ageMonths) { VitaminDCounselling.forAge(ageMonths) }
    val triggered = remember(answers, ageMonths) {
        VitaminDCounselling.triggered(answers, ageMonths)
    }
    val c = riskColors
    val accents = accentColors

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            Gap(8)
            GuideArt("hero_module_3", ratio = 1.9f)
            Gap(16)
            Text("Vitamin D", style = MaterialTheme.typography.headlineSmall)
            Gap(4)
            Text(
                AgeCalculator.format(ageMonths),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Gap(14)
            HelpStripe(onOpen = { stage = VitDStage.HELP })
            Gap(16)

            // ---- the action, first ------------------------------------------
            //
            // Shown only where there is actually an action. Over one year the advice was
            // "No daily supplement needed", which took the most prominent card on the screen
            // to tell the reader that nothing applies to them — a heading that states an
            // absence is worse than no heading.
            if (advice.universal) {
                PoshanCard(container = accents.amber, contentPadding = PaddingValues(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            advice.headline,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        StatusChip("Every baby", MaterialTheme.colorScheme.primary)
                    }
                    Gap(8)
                    Text(advice.detail, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (advice.universal) {
                Gap(14)
                SectionLabel("Is the baby getting it?")
                Gap(8)
                ProphylaxisStatus.entries
                    .filterNot { it == ProphylaxisStatus.NOT_ASKED }
                    .forEach { status ->
                        AdherenceOption(
                            label = status.label,
                            selected = prophylaxisStatus == status,
                            accent = when (status) {
                                ProphylaxisStatus.GIVING_DAILY -> c.low
                                ProphylaxisStatus.GIVING_SOMETIMES -> c.monitor
                                else -> c.review
                            },
                            onClick = { onProphylaxisStatus(status) },
                        )
                        Gap(8)
                    }
                if (prophylaxisStatus != ProphylaxisStatus.NOT_ASKED) {
                    Gap(4)
                    InfoBanner(adherence.message)
                }
                Gap(8)
                WhyButton(whyProphylaxis, label = "Why every baby?")
            }

            // ---- skin typing ----------------------------------------------------
            Gap(20)
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Skin type")
                Spacer(Modifier.weight(1f))
                WhyButton(whySkinTone, label = "How does this work?")
            }
            Gap(8)
            val skinType = remember(skinIta) { SkinType.fromIta(skinIta) }
            if (skinType != null && skinIta != null) {
                SkinTypeCard(ita = skinIta, type = skinType)
                Gap(8)
            }
            SkinToneCard(
                ita = skinIta,
                onMeasure = { stage = VitDStage.CAPTURE_GUIDE },
            )

            // The "Sunlight today" card has been removed at the team's request. The solar
            // window is still computed and still feeds the risk model — an unusable sun
            // angle for the season is one of the weighted factors — it is simply no longer
            // shown as its own card.

            // ---- signs to check ------------------------------------------------
            Gap(20)
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("A quick look")
                Spacer(Modifier.weight(1f))
                WhyButton(whySigns, label = "What am I looking for?")
            }
            Gap(6)
            Text(
                "A quick look at the wrists, chest and legs. Most children have none of " +
                    "these.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Gap(10)
            signs.forEach { sign ->
                SignRow(
                    sign = sign,
                    current = answers[sign.id],
                    onAnswer = { onAnswer(sign.id, it) },
                )
                Gap(8)
            }

            // The referral card that used to sit here has been removed at the team's
            // request. Nothing is lost from the record: any sign the caregiver marked is
            // still stored on the session, still counts towards the module's band, and
            // still appears in the report and the PDF under "Signs checked". What has gone
            // is the on-screen instruction to go and get checked.

            // ---- counselling ---------------------------------------------------
            Gap(20)
            SectionLabel("A few more questions")
            Gap(8)
            counselling.forEach { item ->
                CounsellingRow(
                    item = item,
                    current = answers[item.id],
                    onAnswer = { onAnswer(item.id, it) },
                )
                Gap(8)
            }

            if (triggered.isNotEmpty()) {
                Gap(12)
                SectionLabel("What helps")
                Gap(8)
                triggered.forEach { item ->
                    PoshanCard(
                        modifier = Modifier.padding(bottom = 8.dp),
                        contentPadding = PaddingValues(14.dp),
                    ) {
                        Text(item.advice, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // ---- risk stratification --------------------------------------------
            Gap(22)
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Risk of low vitamin D")
                Spacer(Modifier.weight(1f))
                WhyButton(whyRiskModel, label = "How is this worked out?")
            }
            Gap(8)
            RiskTierCard(
                result = remember(skinType, latitude, answers, ageMonths, prophylaxisStatus) {
                    VitaminDRiskModel.assess(
                        skinType = skinType,
                        answers = answers,
                        ageMonths = ageMonths,
                        prophylaxis = prophylaxisStatus,
                        sunWindowAvailable = latitude?.let {
                            SunWindow.todayWindow(it, LocalDate.now().dayOfYear).available
                        },
                    )
                },
            )

            // ---- how the number was produced -----------------------------------
            if (skinIta != null) {
                Gap(22)
                SkinCalculationSection(ita = skinIta, measurement = lastMeasurement)
            }

            Gap(14)
            InfoBanner(
                "The daily drops are for every baby under one year, whatever this page says.",
            )
            Gap(24)
        }
        Box(Modifier.padding(20.dp)) {
            Column {
                // What is still missing, named. A disabled button with no explanation is
                // the most frustrating state in a form — the user can see they are blocked
                // and not why.
                val missing = buildList {
                    if (skinIta == null || skinIta.isNaN()) add("the skin tone reading")
                    val unanswered = signs.count { answers[it.id] == null } +
                        counselling.count { answers[it.id] == null }
                    if (unanswered > 0) {
                        add("$unanswered question${if (unanswered == 1) "" else "s"}")
                    }
                    if (advice.universal && prophylaxisStatus == ProphylaxisStatus.NOT_ASKED) {
                        add("whether the baby is getting the drops")
                    }
                }

                if (missing.isNotEmpty() && showIncomplete) {
                    PoshanCard(
                        container = riskColors.reviewSoft,
                        contentPadding = PaddingValues(14.dp),
                    ) {
                        Text(
                            "Still to do",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Gap(4)
                        Text(
                            missing.joinToString(", ") + ".",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Gap(10)
                }

                PoshanPrimaryButton(
                    "Save",
                    enabled = missing.isEmpty(),
                    onClick = onSave,
                )

                if (missing.isNotEmpty()) {
                    Gap(8)
                    // A disabled button swallows taps, so the prompt is a separate row the
                    // user can actually press to find out what is left.
                    Text(
                        "Why can't I save?",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                haptics.reject()
                                showIncomplete = true
                            }
                            .padding(8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SunWindowCard(latitude: Double?, ita: Double?) {
    val today = remember { LocalDate.now() }
    val window = remember(latitude, today) {
        latitude?.let { SunWindow.todayWindow(it, today.dayOfYear) }
    }

    PoshanCard(contentPadding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MicroIcon(
                artKey = "micro_sun_window",
                fallback = Icons.Filled.WbSunny,
                size = 24,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Sunlight today",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Gap(8)
        if (window == null) {
            Text(
                "Turn on location to see the best time for sunlight where you are.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                window.format(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Gap(4)
            Text(
                "In this window your shadow is shorter than you are — that is when sunlight " +
                    "makes vitamin D.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Gap(8)
            Text(SunWindow.GUIDANCE, style = MaterialTheme.typography.bodyMedium)
        }
        if (ita != null && !ita.isNaN()) {
            Gap(10)
            // Personalised from the measured skin tone rather than a flat number: the same
            // ten minutes that suffices for light skin is not the same dose for dark skin.
            Text(
                SunExposureFactor.caregiverMessage(ita),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Gap(10)
        InfoBanner(SunWindow.CAVEAT)
    }
}

@Composable
private fun AdherenceOption(
    label: String,
    selected: Boolean,
    accent: Color,
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
                Modifier.size(22.dp).background(
                    if (selected) accent else MaterialTheme.colorScheme.surfaceContainerHighest,
                    CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun SignRow(
    sign: RicketsSign,
    current: ResponseType?,
    onAnswer: (ResponseType) -> Unit,
) {
    val haptics = rememberAppHaptics()
    val c = riskColors
    PoshanCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            // The picture does real work on this screen: the caregiver is being asked to
            // recognise a physical sign, and the words alone leave them guessing at what
            // "widened" or "a row of bumps" actually looks like.
            GuideArt(
                artKey = "vd_sign_" + sign.id.removePrefix("rs_"),
                modifier = Modifier.width(92.dp),
                ratio = 1f,
                placeholderWhenMissing = false,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(sign.question, style = MaterialTheme.typography.bodyLarge)
                Gap(6)
                Text(
                    sign.whatToLookFor,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Gap(10)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            YesNoChip(
                label = "Yes",
                selected = current == ResponseType.YES_OBSERVED,
                accent = c.review,
                modifier = Modifier.weight(1f),
            ) { haptics.confirm(); onAnswer(ResponseType.YES_OBSERVED) }
            YesNoChip(
                label = "No",
                selected = current == ResponseType.NOT_YET,
                accent = c.low,
                modifier = Modifier.weight(1f),
            ) { haptics.click(); onAnswer(ResponseType.NOT_YET) }
            YesNoChip(
                label = "Not sure",
                selected = current == ResponseType.NOT_SURE,
                accent = c.monitor,
                modifier = Modifier.weight(1f),
            ) { haptics.click(); onAnswer(ResponseType.NOT_SURE) }
        }
    }
}

@Composable
private fun CounsellingRow(
    item: VitaminDCounselling,
    current: ResponseType?,
    onAnswer: (ResponseType) -> Unit,
) {
    val haptics = rememberAppHaptics()
    val c = riskColors
    PoshanCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GuideArt(
                artKey = "vd_bg_" + item.id.removePrefix("vd_"),
                modifier = Modifier.width(72.dp),
                ratio = 1f,
                placeholderWhenMissing = false,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                item.question,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }
        Gap(10)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            YesNoChip(
                label = "Yes",
                selected = current == ResponseType.YES_OBSERVED,
                accent = c.monitor,
                modifier = Modifier.weight(1f),
            ) { haptics.click(); onAnswer(ResponseType.YES_OBSERVED) }
            YesNoChip(
                label = "No",
                selected = current == ResponseType.NOT_YET,
                accent = c.low,
                modifier = Modifier.weight(1f),
            ) { haptics.click(); onAnswer(ResponseType.NOT_YET) }
        }
    }
}

@Composable
private fun YesNoChip(
    label: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .clip(MaterialTheme.shapes.small)
            .background(
                if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            .clickable(onClick = onClick)
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

private val whyProphylaxis = WhyContent(
    title = "Why does every baby need this?",
    plain = "Breastmilk is the best food for a baby but carries very little vitamin D. That " +
        "is true of every mother, everywhere — it is not a sign that anything is wrong. So " +
        "the guidance is daily drops for every baby under one year, whether breastfed or " +
        "on formula.",
    technical = "A single daily dose from birth to twelve months, independent of feeding " +
        "method. The health worker or the bottle gives the amount — preparations differ, so " +
        "the app deliberately does not print a figure a parent might convert wrongly. More " +
        "is not better and carries a real toxicity risk.",
    evidence = "Indian Academy of Paediatrics 2021 guidelines; Global Consensus on " +
        "Prevention and Management of Nutritional Rickets 2016.",
    limitation = "After the first birthday routine drops are not recommended — vitamin D " +
        "should come from food, fortified foods and sunlight instead.",
)

private val whySigns = WhyContent(
    title = "What am I looking for?",
    plain = "Severe, long-standing vitamin D shortage can soften a child's bones. When that " +
        "happens it shows in a few specific places — the wrists, the ribs and the legs. " +
        "These are the same signs India's national child health screening looks for.",
    technical = "Any single sign triggers referral. There is no score, because no published " +
        "study reports how accurate these signs are as a screening test in a general " +
        "population, and inventing a weighting would be a false precision.",
    evidence = "RBSK screening card item B5, birth to six years: wrist widening, bowing of " +
        "legs, nodular swelling on the chest.",
    limitation = "Most children with a sign will not have active rickets. Community " +
        "screening in the Gambia flagged about 3 in 100 children, of whom only a small " +
        "fraction had active disease. Referral is a check, not a diagnosis.",
)

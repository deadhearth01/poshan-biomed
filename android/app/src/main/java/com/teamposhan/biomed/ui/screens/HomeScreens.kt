package com.teamposhan.biomed.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.AccountCircle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.teamposhan.biomed.R
import com.teamposhan.biomed.data.model.AssessmentSession
import com.teamposhan.biomed.data.model.ChildProfile
import com.teamposhan.biomed.data.model.RiskBand
import com.teamposhan.biomed.data.model.SessionStatus
import com.teamposhan.biomed.data.model.Sex
import com.teamposhan.biomed.data.model.UserRole
import com.teamposhan.biomed.domain.age.AgeCalculator
import com.teamposhan.biomed.domain.growth.GrowthStandards
import com.teamposhan.biomed.domain.milestones.AgeBand
import com.teamposhan.biomed.domain.schedule.AssessmentSchedule
import com.teamposhan.biomed.domain.schedule.ScheduledVisit
import com.teamposhan.biomed.domain.schedule.VisitStatus
import com.teamposhan.biomed.ui.components.ChildAvatarImage
import com.teamposhan.biomed.ui.components.MicroIcon
import com.teamposhan.biomed.ui.components.GuideArt
import com.teamposhan.biomed.ui.components.Gap
import com.teamposhan.biomed.ui.components.InfoBanner
import com.teamposhan.biomed.ui.components.PoshanCard
import com.teamposhan.biomed.ui.components.PoshanPrimaryButton
import com.teamposhan.biomed.ui.components.PoshanProgress
import com.teamposhan.biomed.ui.components.PoshanTextField
import com.teamposhan.biomed.ui.components.RiskChip
import com.teamposhan.biomed.ui.components.SectionLabel
import com.teamposhan.biomed.ui.haptics.rememberAppHaptics
import com.teamposhan.biomed.ui.theme.accentColors
import com.teamposhan.biomed.ui.theme.riskColors
import java.time.LocalDate

@Composable
fun HomeScreen(
    role: UserRole,
    child: ChildProfile?,
    latest: AssessmentSession?,
    sessions: List<AssessmentSession>,
    syncMessage: String?,
    syncing: Boolean,
    onSync: () -> Unit,
    onStartAssessment: () -> Unit,
    onAttention: () -> Unit,
    onImmunization: () -> Unit,
    onReport: () -> Unit,
    onAddChild: () -> Unit,
    onOpenChild: () -> Unit,
    onVitaminD: () -> Unit,
    onGrowthCharts: () -> Unit = {},
) {
    val accents = accentColors

    // Deliberately sparse. The previous version stacked a hero card, z-score chips, a
    // next-visit card, a progress bar, three module tiles, a continue button and a 2x2
    // action grid on one screen — every one of them justifiable, all of them together
    // unreadable. What survives: who this is, the one thing to do next, and a way to
    // everything else.
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(12.dp))

        if (child == null) {
            EmptyHome(onAddChild = onAddChild)
            return@Column
        }

        val ageMonths = AgeCalculator.assessmentMonths(child)

        // ---- who ------------------------------------------------------------
        ChildHeader(
            child = child,
            ageMonths = ageMonths,
            latest = latest,
            onClick = onOpenChild,
        )

        Spacer(Modifier.height(28.dp))

        // ---- the three modules, each with its own state ----------------------
        //
        // An earlier pass reduced this to a single "next step" card. That was too far: a
        // caregiver could not see what the screening consisted of, how far through it they
        // were, or go back to a finished part. All three are shown, with the next one
        // emphasised so there is still one obvious thing to do.
        val m1Done = latest?.categoryResults?.isNotEmpty() == true
        val m2Done = latest?.attention != null
        val m3Done = latest?.vitaminD != null
        val doneCount = listOf(m1Done, m2Done, m3Done).count { it }
        val currentStep = when {
            !m1Done -> 1
            !m2Done -> 2
            !m3Done -> 3
            else -> 0
        }

        ModuleRow(
            number = 1,
            title = "Milestones",
            subtitle = moduleOneSubtitle(latest),
            done = m1Done,
            isNext = currentStep == 1,
            accent = accents.mint,
            onClick = onStartAssessment,
        )
        Spacer(Modifier.height(10.dp))
        ModuleRow(
            number = 2,
            title = "Attention & gaze",
            subtitle = if (m2Done) "Completed" else "Camera activity and a few questions",
            done = m2Done,
            isNext = currentStep == 2,
            accent = accents.violet,
            onClick = onAttention,
        )
        Spacer(Modifier.height(10.dp))
        ModuleRow(
            number = 3,
            title = "Nutrition & vitamin D",
            subtitle = if (m3Done) "Completed" else "Measurements and risk questions",
            done = m3Done,
            isNext = currentStep == 3,
            accent = accents.amber,
            onClick = onVitaminD,
        )

        Spacer(Modifier.height(18.dp))
        ProgressDots(done = doneCount, total = 3)

        // A header that answers "where are we" before anything is read.
        //
        // Placed after the modules rather than above them: the first thing on the screen
        // should be the child and the work, not decoration. It changes with the state of the
        // visit, so opening the app twice in one day does not look identical.
        Spacer(Modifier.height(20.dp))
        GuideArt(
            homeStateArtKey(
                doneCount = doneCount,
                needsReview = latest?.composite?.band == RiskBand.REVIEW ||
                    latest?.composite?.band == RiskBand.PRIORITY,
                offline = syncMessage == null || syncMessage.startsWith("Offline"),
            ),
            ratio = 1.9f,
            placeholderWhenMissing = false,
        )

        if (currentStep == 0) {
            Spacer(Modifier.height(18.dp))
            PoshanPrimaryButton(
                stringResource(R.string.view_report),
                onClick = onReport,
                icon = Icons.Filled.Assessment,
            )
        }

        Spacer(Modifier.height(32.dp))

        // ---- everything else, quietly ----------------------------------------
        QuietAction(Icons.Filled.BarChart, "Growth charts", onGrowthCharts, "micro_growth_chart")
        QuietAction(Icons.Filled.Assessment, "Report", onReport, "micro_report")
        QuietAction(
            Icons.Filled.Vaccines,
            stringResource(R.string.immunization),
            onImmunization,
            "micro_immunization",
        )

        if (latest?.status == SessionStatus.DRAFT) {
            Spacer(Modifier.height(20.dp))
            InfoBanner(stringResource(R.string.save_and_resume))
        }

        Spacer(Modifier.height(20.dp))
        CloudSyncRow(syncMessage, syncing, onSync)
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun EmptyHome(onAddChild: () -> Unit) {
    Column {
        Spacer(Modifier.height(24.dp))
        GuideArt("micro_no_child_yet", ratio = 1.5f)
        Spacer(Modifier.height(28.dp))
        Text(
            stringResource(R.string.no_children_yet),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.home_add_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        PoshanPrimaryButton(
            stringResource(R.string.add_child),
            onClick = onAddChild,
            icon = Icons.Filled.Add,
        )
    }
}

/** Name, age, and a risk chip only when there is one. Nothing else competes here. */
@Composable
private fun ChildHeader(
    child: ChildProfile,
    ageMonths: Float,
    latest: AssessmentSession?,
    onClick: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable { haptics.tick(); onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChildAvatarImage(
            childId = child.id,
            name = child.name,
            sex = child.sex.name,
            ageMonths = ageMonths,
            size = 56,
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                child.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                AgeCalculator.format(ageMonths),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        latest?.composite?.let { RiskChip(it.band) }
    }
}

/**
 * One module: its number, name, state, and whether it is the one to do next.
 *
 * The next module is filled and raised; completed ones carry a tick and recede; later ones
 * stay quiet but remain tappable, because a caregiver may legitimately want to redo the
 * growth measurements without first re-answering 40 milestone questions.
 */
@Composable
private fun ModuleRow(
    number: Int,
    title: String,
    subtitle: String,
    done: Boolean,
    isNext: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    PoshanCard(
        onClick = { haptics.progressTick(number / 3f); onClick() },
        container = when {
            isNext -> accent
            done -> MaterialTheme.colorScheme.surfaceContainerLow
            else -> MaterialTheme.colorScheme.surfaceContainerLowest
        },
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(
                        if (done) riskColors.low else MaterialTheme.colorScheme.surfaceContainerHighest,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (done) {
                    MicroIcon(
                        artKey = "micro_complete",
                        fallback = Icons.Filled.Check,
                        size = 20,
                        tint = Color.White,
                        contentDescription = "Completed",
                    )
                } else {
                    Text(
                        "$number",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isNext) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** How many of module 1's five categories are answered, for the subtitle. */
private fun moduleOneSubtitle(latest: AssessmentSession?): String {
    val results = latest?.categoryResults.orEmpty()
    if (results.isEmpty()) return "Questions about what your child can do"
    val done = results.count { it.answered > 0 }
    return if (done >= results.size) "Completed" else "$done of ${results.size} categories answered"
}

/** Three dots instead of a progress bar and a label. Same information, no words. */
@Composable
private fun ProgressDots(done: Int, total: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(total) { i ->
            Box(
                Modifier
                    .padding(horizontal = 5.dp)
                    .size(if (i < done) 10.dp else 8.dp)
                    .background(
                        if (i < done) {
                            riskColors.low
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        CircleShape,
                    ),
            )
        }
    }
}

/** A secondary destination: one line, one icon, no card weight. */
@Composable
private fun QuietAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    artKey: String? = null,
) {
    val haptics = rememberAppHaptics()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { haptics.tick(); onClick() }
            .padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MicroIcon(
            artKey = artKey ?: "",
            fallback = icon,
            size = 24,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(18.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CloudSyncRow(message: String?, syncing: Boolean, onSync: () -> Unit) {
    val c = riskColors
    val done = message?.startsWith("Synced") == true
    PoshanCard(onClick = onSync, container = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MicroIcon(
                artKey = if (syncing) "micro_syncing" else "micro_offline_saved",
                fallback = when {
                    syncing -> Icons.Filled.CloudSync
                    done -> Icons.Filled.CloudDone
                    else -> Icons.Filled.CloudOff
                },
                tint = when {
                    syncing -> MaterialTheme.colorScheme.primary
                    done -> c.low
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_cloud_backup), style = MaterialTheme.typography.titleSmall)
                Text(
                    message ?: stringResource(R.string.home_cloud_auto),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ChildListScreen(
    children: List<ChildProfile>,
    onOpen: (ChildProfile) -> Unit,
    onAdd: () -> Unit,
    onEdit: (ChildProfile) -> Unit = {},
    onDelete: (ChildProfile) -> Unit = {},
) {
    val accents = accentColors
    val haptics = rememberAppHaptics()
    // Held rather than acted on immediately: deleting a child destroys every assessment ever
    // recorded for them, and there is no undo.
    var pendingDelete by remember { mutableStateOf<ChildProfile?>(null) }

    pendingDelete?.let { child ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete ${child.name}?") },
            text = {
                Text(
                    "This removes the child and every assessment recorded for them. It " +
                        "cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    haptics.reject()
                    onDelete(child)
                    pendingDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Keep") }
            },
        )
    }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(children, key = { it.id }) { child ->
                PoshanCard(onClick = { onOpen(child) }, contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChildAvatarImage(
                            childId = child.id,
                            name = child.name,
                            sex = child.sex.name,
                            ageMonths = AgeCalculator.assessmentMonths(child),
                            size = 48,
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(child.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${AgeCalculator.format(AgeCalculator.assessmentMonths(child))} · ${if (child.sex == Sex.FEMALE) "Girl" else "Boy"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text("ID ${child.id.take(8)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        IconButton(onClick = { haptics.click(); onEdit(child) }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Edit ${child.name}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { haptics.click(); pendingDelete = child }) {
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = "Delete ${child.name}",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
        Box(Modifier.padding(20.dp)) {
            PoshanPrimaryButton(stringResource(R.string.add_child), onClick = onAdd, icon = Icons.Filled.Add)
        }
    }
}

@Composable
fun ChildFormScreen(
    existing: ChildProfile?,
    onSave: (ChildProfile) -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var dob by remember {
        mutableStateOf(existing?.let { LocalDate.ofEpochDay(it.dateOfBirthEpochDay) } ?: LocalDate.now().minusMonths(12))
    }
    var sex by remember { mutableStateOf(existing?.sex ?: Sex.MALE) }
    var premature by remember { mutableStateOf(existing?.bornPremature ?: false) }
    var gaWeeks by remember { mutableStateOf(existing?.gestationalAgeWeeks?.toString() ?: "") }
    var parentName by remember { mutableStateOf(existing?.parentName ?: "") }
    var parentMobile by remember { mutableStateOf(existing?.parentMobile ?: "") }
    var village by remember { mutableStateOf(existing?.village ?: "") }
    var weight by remember { mutableStateOf(existing?.baselineWeightKg?.toString() ?: "") }
    var height by remember { mutableStateOf(existing?.baselineHeightCm?.toString() ?: "") }
    var muac by remember { mutableStateOf(existing?.baselineMuacMm?.toString() ?: "") }

    // Recomputed on every edit so the switch, the weeks field and the date all show their
    // effect immediately.
    val correction = remember(premature, gaWeeks, dob) {
        AgeCalculator.correctionState(premature, gaWeeks.toIntOrNull(), dob)
    }

    val lengthLabel = com.teamposhan.biomed.domain.growth.GrowthStandards.measurementLabel(
        AgeCalculator.chronologicalMonths(dob)
    )
    val muacApplicable = AgeCalculator.chronologicalMonths(dob) in 6f..59f

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(stringResource(R.string.complete_child_details), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(18.dp))

            PoshanTextField(name, { name = it }, stringResource(R.string.child_name))
            Gap(14)

            PoshanCard(
                onClick = {
                    val cal = java.util.Calendar.getInstance()
                    cal.set(dob.year, dob.monthValue - 1, dob.dayOfMonth)
                    DatePickerDialog(
                        context,
                        { _, y, m, d -> dob = LocalDate.of(y, m + 1, d) },
                        cal.get(java.util.Calendar.YEAR),
                        cal.get(java.util.Calendar.MONTH),
                        cal.get(java.util.Calendar.DAY_OF_MONTH),
                    ).apply {
                        // Bounded, because it was not. A future date produced a negative age
                        // that every downstream calculation then inherited — question band,
                        // z-scores, vaccine schedule — and the form displayed it as a dash
                        // rather than refusing it.
                        datePicker.maxDate = System.currentTimeMillis()
                        datePicker.minDate = java.time.LocalDate.now()
                            .minusYears(6)
                            .atStartOfDay(java.time.ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                    }.show()
                },
                contentPadding = PaddingValues(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.date_of_birth), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(dob.toString(), style = MaterialTheme.typography.titleMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            AgeCalculator.format(AgeCalculator.chronologicalMonths(dob)),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        // The corrected age is what the screening actually uses, so it is
                        // shown here rather than only appearing later on the home screen,
                        // where an unexplained smaller number reads as a bug.
                        (correction as? AgeCalculator.CorrectionState.Applied)?.let {
                            Text(
                                "${AgeCalculator.format(it.correctedMonths)} corrected",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Gap(16)

            SectionLabel(stringResource(R.string.sex_at_birth))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = sex == Sex.MALE, onClick = { sex = Sex.MALE }, label = { Text(stringResource(R.string.male)) })
                FilterChip(selected = sex == Sex.FEMALE, onClick = { sex = Sex.FEMALE }, label = { Text(stringResource(R.string.female)) })
                FilterChip(selected = sex == Sex.OTHER, onClick = { sex = Sex.OTHER }, label = { Text(stringResource(R.string.other)) })
            }
            Gap(16)

            PoshanCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.born_prematurely), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.age_applicable_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = premature, onCheckedChange = { premature = it })
                }
                if (premature) {
                    Spacer(Modifier.height(14.dp))
                    PoshanTextField(
                        gaWeeks, { gaWeeks = it }, stringResource(R.string.gestational_age_weeks),
                        keyboardType = KeyboardType.Number,
                        suffix = "weeks",
                        filter = { input -> input.filter { c -> c.isDigit() }.take(2) },
                    )
                    Spacer(Modifier.height(10.dp))
                    // The switch on its own does nothing — the correction needs a gestational
                    // age to subtract. Previously it silently did nothing and looked broken.
                    when (val c = correction) {
                        is AgeCalculator.CorrectionState.NeedsGestationalAge -> Text(
                            "Enter how many weeks the pregnancy reached. Without it the age " +
                                "cannot be adjusted and this switch has no effect.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        is AgeCalculator.CorrectionState.TermGestation -> Text(
                            "${c.weeks} weeks counts as full term, so no adjustment is " +
                                "needed. Adjustment applies below " +
                                "${AgeCalculator.PRETERM_THRESHOLD_WEEKS} weeks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        is AgeCalculator.CorrectionState.PastCeiling -> Text(
                            "Past two years, adjustment for prematurity is no longer used.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        is AgeCalculator.CorrectionState.Applied -> Text(
                            "Born ${c.weeksEarly} weeks early. Screening will use " +
                                "${AgeCalculator.format(c.correctedMonths)} instead of " +
                                "${AgeCalculator.format(c.chronologicalMonths)}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        AgeCalculator.CorrectionState.NotApplicable -> Unit
                    }
                }
            }
            Gap(16)

            SectionLabel("Measurements at enrolment")
            Gap(6)
            // The same how-to cards the assessment screen shows. They were previously only
            // on the later screen, so the very first measurement a child ever gets — the one
            // every future comparison is made against — was taken with no guidance at all.
            MeasurementGuideCard(MeasurementHowTo.weight)
            PoshanTextField(weight, { weight = it }, stringResource(R.string.weight), suffix = "kg", keyboardType = KeyboardType.Decimal)
            Gap(16)
            MeasurementGuideCard(
                MeasurementHowTo.length(AgeCalculator.chronologicalMonths(dob))
            )
            PoshanTextField(
                height, { height = it }, lengthLabel, suffix = "cm",
                keyboardType = KeyboardType.Decimal,
                supportingText = com.teamposhan.biomed.domain.growth.GrowthStandards.measurementHint(AgeCalculator.chronologicalMonths(dob)),
            )
            Gap(16)
            if (muacApplicable) {
                MeasurementGuideCard(MeasurementHowTo.muac)
            }
            PoshanTextField(
                muac, { muac = it }, "Arm circumference", suffix = "mm",
                keyboardType = KeyboardType.Number,
                enabled = muacApplicable,
                supportingText = if (muacApplicable) {
                    "Around the middle of the upper arm, e.g. 138"
                } else {
                    "Taken from 6 months to 5 years"
                },
            )
            Spacer(Modifier.height(18.dp))

            PoshanTextField(parentName, { parentName = it }, stringResource(R.string.parent_caregiver))
            Gap(14)
            PoshanTextField(parentMobile, { parentMobile = it }, stringResource(R.string.mobile_number), keyboardType = KeyboardType.Phone)
            Gap(14)
            PoshanTextField(village, { village = it }, stringResource(R.string.village_area))
            Spacer(Modifier.height(16.dp))
        }
        Box(Modifier.padding(20.dp)) {
            PoshanPrimaryButton(
                stringResource(R.string.save),
                // A premature child with no gestational age would save a switch that does
                // nothing, so the form asks for the missing piece rather than accepting it.
                enabled = name.isNotBlank() &&
                    correction !is AgeCalculator.CorrectionState.NeedsGestationalAge,
            ) {
                onSave(
                    ChildProfile(
                        id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                        name = name.trim(),
                        dateOfBirthEpochDay = dob.toEpochDay(),
                        sex = sex,
                        bornPremature = premature,
                        gestationalAgeWeeks = gaWeeks.toIntOrNull(),
                        parentName = parentName,
                        parentMobile = parentMobile,
                        village = village,
                        baselineWeightKg = weight.toFloatOrNull(),
                        baselineHeightCm = height.toFloatOrNull(),
                        baselineMuacMm = muac.toFloatOrNull(),
                    ),
                )
            }
        }
    }
}

@Composable
fun ChildDetailScreen(
    child: ChildProfile,
    sessions: List<AssessmentSession>,
    onStartAssessment: () -> Unit,
    onAttention: () -> Unit,
    onReport: () -> Unit,
) {
    val accents = accentColors
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(64.dp).background(accents.mint, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(child.name.take(1).uppercase(), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(child.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${AgeCalculator.format(AgeCalculator.assessmentMonths(child))} · ${AgeBand.forAge(AgeCalculator.assessmentMonths(child)).label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AgeCalculator.correctedMonths(child)?.let {
            Gap(12)
            InfoBanner("${stringResource(R.string.corrected_age)}: ${AgeCalculator.format(it)}")
        }
        Gap(20)
        AssessmentTimeline(
            visits = com.teamposhan.biomed.domain.schedule.AssessmentSchedule.timeline(
                ageMonths = AgeCalculator.assessmentMonths(child),
                sessions = sessions,
            ),
        )
        Gap(10)
        PoshanPrimaryButton(stringResource(R.string.start_step_assessment), onClick = onStartAssessment, icon = Icons.Filled.Assessment)
        Gap(10)
        com.teamposhan.biomed.ui.components.PoshanTonalButton(stringResource(R.string.attention_module), onClick = onAttention, icon = Icons.Filled.Visibility)
        Gap(10)
        com.teamposhan.biomed.ui.components.PoshanOutlinedButton(stringResource(R.string.report_generation), onClick = onReport, icon = Icons.Filled.BarChart)

        if (sessions.isNotEmpty()) {
            Spacer(Modifier.height(26.dp))
            SectionLabel("History")
            sessions.forEach { s ->
                PoshanCard(modifier = Modifier.padding(bottom = 10.dp), contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                java.text.SimpleDateFormat("d MMM yyyy, h:mm a", java.util.Locale.getDefault())
                                    .format(java.util.Date(s.startedAt)),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                "DQ ${s.dq?.dq?.toInt() ?: 0}% · ${s.answeredCount} answers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        s.composite?.let { RiskChip(it.band) }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

/**
 * The child switcher for the Home app bar: the active child's initial, or a plain icon
 * when none is chosen yet. Switching child is a context change rather than a destination,
 * so it lives here instead of taking a third of the bottom bar.
 */
@Composable
fun ChildSwitcherAction(
    child: ChildProfile?,
    childCount: Int,
    onClick: () -> Unit,
) {
    val haptics = rememberAppHaptics()
    IconButton(onClick = { haptics.tick(); onClick() }) {
        if (child != null) {
            Box(
                Modifier.size(36.dp).background(accentColors.mint, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    child.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            Icon(
                Icons.Filled.ChildCare,
                contentDescription = if (childCount > 0) "Choose a child" else "Add a child",
            )
        }
    }
}

/**
 * App-bar entry to the calculations page.
 *
 * Kept visible rather than buried in a settings menu: a health worker asked "where did that
 * number come from?" should be one tap from the answer.
 */
/**
 * Opens the signed-in person's own details.
 *
 * In the top bar rather than buried in a menu: it is also where the doctor's number is set,
 * which is the thing a parent goes looking for when they want to send a report.
 */
@Composable
fun ProfileAction(onClick: () -> Unit) {
    val haptics = rememberAppHaptics()
    IconButton(onClick = { haptics.tick(); onClick() }) {
        Icon(
            Icons.Filled.AccountCircle,
            contentDescription = "Your profile",
        )
    }
}

@Composable
fun AboutAction(onClick: () -> Unit) {
    val haptics = rememberAppHaptics()
    androidx.compose.material3.IconButton(onClick = { haptics.tick(); onClick() }) {
        Icon(
            Icons.Filled.Functions,
            contentDescription = "How the app calculates",
        )
    }
}

/**
 * Which home illustration matches the state of this visit.
 *
 * Order is by what the reader most needs to know, not by how far through the visit they are:
 * a referral outranks a finished checklist, because a completed visit that produced a
 * referral is not a "done" moment.
 */
private fun homeStateArtKey(
    doneCount: Int,
    needsReview: Boolean,
    offline: Boolean,
): String = when {
    needsReview -> "home_state_review_needed"
    doneCount == 3 -> "home_state_all_done"
    doneCount > 0 -> "home_state_in_progress"
    offline -> "home_state_offline"
    else -> "home_state_nothing_done"
}

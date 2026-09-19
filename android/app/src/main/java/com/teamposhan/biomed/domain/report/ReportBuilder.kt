package com.teamposhan.biomed.domain.report

import com.teamposhan.biomed.data.model.AssessmentSession
import com.teamposhan.biomed.data.model.ChildProfile
import com.teamposhan.biomed.data.model.DomainStatus
import com.teamposhan.biomed.data.model.RiskBand
import com.teamposhan.biomed.domain.attention.AttentionPhase
import com.teamposhan.biomed.domain.markers.EarlyMarkerBank
import com.teamposhan.biomed.domain.markers.EarlyMarkerResult
import com.teamposhan.biomed.domain.risk.ProphylaxisAdvice
import com.teamposhan.biomed.domain.risk.ProphylaxisStatus
import com.teamposhan.biomed.domain.risk.RicketsScreenResult
import com.teamposhan.biomed.domain.risk.VitaminDRiskModel
import com.teamposhan.biomed.domain.skin.SkinType
import com.teamposhan.biomed.domain.skin.SunExposureFactor
import com.teamposhan.biomed.domain.age.AgeCalculator
import com.teamposhan.biomed.domain.milestones.BandCoverage
import com.teamposhan.biomed.domain.growth.GrowthInterpretation
import com.teamposhan.biomed.domain.nutrition.NutritionQuestionBank
import com.teamposhan.biomed.domain.nutrition.NutritionResult
import com.teamposhan.biomed.domain.growth.GrowthStandards
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The shareable text report.
 *
 * Rewritten from a flat key-value dump into something a health worker can read at a glance
 * and a doctor can act on. Three principles:
 *
 *  1. **Findings first.** Anything needing action appears before the raw numbers. A report
 *     that buries "severe stunting" under twelve lines of demographics gets skimmed past.
 *  2. **Every number carries its interpretation.** `-2.4 SD` means nothing to most readers;
 *     `-2.4 SD (below normal — stunting)` means something to everyone.
 *  3. **Absent is stated, not omitted.** A missing measurement is written as "not measured",
 *     never silently dropped, because a reader cannot tell the difference between "not done"
 *     and "forgot to include".
 */
object ReportBuilder {

    private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy")

    /**
     * @param history every session for this child, used for the cross-visit band coverage.
     *   Defaults to just the current session so existing callers keep working.
     */
    fun build(
        child: ChildProfile,
        session: AssessmentSession?,
        history: List<AssessmentSession> = listOfNotNull(session),
    ): String = buildString {
        header(child, session)
        if (session == null) {
            appendLine()
            appendLine("No assessment has been recorded for this child yet.")
            appendLine()
            footer()
            return@buildString
        }
        actionSummary(session)
        developmental(session)
        bandCoverage(history)
        growth(child, session)
        attention(session)
        vitaminD(session)
        immunization(session)
        footer()
    }

    // ---------------------------------------------------------------------

    private fun StringBuilder.rule(char: String = "=") = appendLine(char.repeat(52))

    private fun StringBuilder.section(title: String) {
        appendLine()
        appendLine(title.uppercase())
        rule("-")
    }

    /** Pads a label so values line up in a monospaced view. */
    private fun StringBuilder.row(label: String, value: String) =
        appendLine("  ${label.padEnd(26)}$value")

    private fun StringBuilder.header(child: ChildProfile, session: AssessmentSession?) {
        rule()
        appendLine("POSHAN CHILD SCREENING REPORT")
        rule()
        appendLine()
        row("Child", child.name)
        row("Date of birth", LocalDate.ofEpochDay(child.dateOfBirthEpochDay).format(dateFormat))
        row("Sex", child.sex.name.lowercase().replaceFirstChar { it.uppercase() })
        row("Age today", AgeCalculator.format(AgeCalculator.chronologicalMonths(child)))
        if (child.bornPremature) {
            row("Born premature", "Yes, ${child.gestationalAgeWeeks ?: "?"} weeks")
            AgeCalculator.correctedMonths(child)?.let {
                row("Corrected age", AgeCalculator.format(it))
            }
        }
        session?.let {
            row("Assessed at age", AgeCalculator.format(it.ageMonthsAtAssessment))
            row(
                "Assessment date",
                Instant.ofEpochMilli(it.completedAt ?: it.startedAt)
                    .atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormat),
            )
            row("Recorded by", it.assessorRole.name.replace('_', ' ').lowercase())
        }
        if (child.village.isNotBlank()) row("Village / area", child.village)
    }

    /**
     * What a reader needs in the first ten seconds: does anything require action, and what.
     */
    private fun StringBuilder.actionSummary(session: AssessmentSession) {
        section("Summary")

        val band = session.composite?.band
        row(
            "Overall",
            when (band) {
                RiskBand.ON_TRACK -> "On track"
                RiskBand.MONITOR -> "Monitor — reassess at the next visit"
                RiskBand.REVIEW -> "Review — refer for developmental assessment"
                RiskBand.PRIORITY -> "Priority — refer without delay"
                null -> "Not scored"
            },
        )

        val actions = mutableListOf<String>()

        session.categoryResults
            .filter { it.status == DomainStatus.DELAYED }
            .forEach { c ->
                val since = c.earliestMissBand?.let { " (delayed since $it)" } ?: ""
                actions += "${prettyCategory(c.category.name)} is delayed$since"
            }
        session.categoryResults.flatMap { it.redFlags }.distinct().forEach {
            actions += "Red flag: $it"
        }
        session.growthZ?.let { z ->
            val findings = GrowthInterpretation.findings(
                z, session.growth?.muacMm, session.ageMonthsAtAssessment,
            )
            findings.filter { it.severity == DomainStatus.DELAYED }.forEach {
                actions += "${it.label} — ${it.detail}"
            }
        }

        if (actions.isEmpty()) {
            appendLine()
            appendLine("  No findings requiring action at this visit.")
        } else {
            appendLine()
            appendLine("  NEEDS ATTENTION:")
            actions.forEach { appendLine("   * $it") }
        }
    }

    private fun StringBuilder.developmental(session: AssessmentSession) {
        section("1. Developmental milestones")
        if (session.categoryResults.isEmpty()) {
            appendLine("  Not assessed.")
            return
        }
        session.dq?.let {
            row("Developmental quotient", "${it.dq.toInt()}%")
            row(
                "Developmental age",
                "${AgeCalculator.format(it.developmentalAgeMonths)} " +
                    "(chronological ${AgeCalculator.format(it.chronologicalAgeMonths)})",
            )
            appendLine()
        }
        session.categoryResults.forEach { c ->
            appendLine("  ${prettyCategory(c.category.name)}")
            row("  Status", statusLabel(c.status))
            row("  Answered", "${c.answered} of ${c.total}")
            row("  Not yet achieved", "${c.concerns}")
            if (c.missedEarlier.isNotEmpty()) {
                row("  Missed from earlier ages", "${c.missedEarlier.size}")
                c.missedEarlier.take(5).forEach { appendLine("      - $it") }
                if (c.missedEarlier.size > 5) {
                    appendLine("      - and ${c.missedEarlier.size - 5} more")
                }
            }
            if (c.redFlags.isNotEmpty()) {
                c.redFlags.forEach { appendLine("      ! $it") }
            }
            appendLine()
        }
        appendLine("  Scoring: Yes = 1, Maybe = 0.5, No = 0, Not sure = 0 and flagged.")
    }

    /**
     * What has been answered in each age band, across every visit.
     *
     * Reported separately from the current visit's domain scores because it answers a
     * different question: not "how is this child doing" but "what have we actually covered".
     * Catch-up questions mean one visit answers several bands, and until now nothing in the
     * report showed that.
     */
    private fun StringBuilder.bandCoverage(history: List<AssessmentSession>) {
        val covered = BandCoverage.coveredBands(history)
        if (covered.isEmpty()) return

        appendLine()
        appendLine("  Coverage by age band (all visits):")
        covered.forEach { c ->
            val note = buildList {
                if (c.concerns > 0) add("${c.concerns} to watch")
                if (c.notSure > 0) add("${c.notSure} not sure")
            }.joinToString(", ")
            row(
                "   ${c.band.label}",
                "${c.answered}/${c.total}" + if (note.isEmpty()) "" else "  ($note)",
            )
        }
    }

    private fun StringBuilder.growth(child: ChildProfile, session: AssessmentSession) {
        section("2. Growth and nutrition")
        val g = session.growth
        val z = session.growthZ
        val age = session.ageMonthsAtAssessment
        if (g == null && z == null) {
            appendLine("  Not measured.")
            // No early return: the feeding questionnaire is reported below and is
            // independent of whether anyone had a scale to hand.
            feeding(child, session)
            return
        }
        val lengthWord = GrowthStandards.measurementLabel(age)

        row("Weight", g?.weightKg?.let { "%.1f kg".format(it) } ?: "not measured")
        row(lengthWord, g?.heightCm?.let { "%.1f cm".format(it) } ?: "not measured")
        row("MUAC", g?.muacMm?.let { "%.0f mm".format(it) } ?: "not measured")
        appendLine()

        fun zLine(label: String, value: Float?) {
            if (value == null) {
                row(label, "not measured")
            } else {
                row(
                    label,
                    "%+.2f SD  (%s, %s)".format(
                        value,
                        GrowthStandards.interpret(value) ?: "-",
                        percentileWord(GrowthStandards.percentile(value)),
                    ),
                )
            }
        }
        appendLine("  WHO z-scores:")
        zLine("  Weight for age", z?.weightForAge)
        zLine("  $lengthWord for age", z?.heightForAge)
        zLine("  BMI for age", z?.bmiForAge)
        zLine("  Arm circumference", z?.muacForAge)

        val findings = GrowthInterpretation.findings(z, g?.muacMm, age)
        if (findings.isNotEmpty()) {
            appendLine()
            appendLine("  Interpretation:")
            findings.forEach { appendLine("   * ${it.label}: ${it.detail}") }
        }
        appendLine()
        appendLine("  Normal range is -2 SD to +2 SD (about 95% of healthy children).")
        feeding(child, session)
    }

    /**
     * The feeding questionnaire.
     *
     * This section header has said "Growth and nutrition" since the first build while
     * printing only growth, so every report shared with a health worker was missing the
     * entire feeding assessment — answers that were collected, stored and synced but never
     * read back out.
     *
     * The household background factors are merged in from the child profile rather than the
     * session: they describe the home, are asked once, and would otherwise always score zero
     * here.
     */
    private fun StringBuilder.feeding(child: ChildProfile, session: AssessmentSession) {
        val answers = session.nutritionAnswers + child.nutritionBackground
        if (answers.isEmpty()) {
            appendLine()
            appendLine("  Feeding: not assessed.")
            return
        }

        val practice = NutritionQuestionBank.forAge(session.ageMonthsAtAssessment)
        val background = NutritionQuestionBank.backgroundQuestions
        val r = NutritionResult.evaluate(practice, background, answers)

        appendLine()
        appendLine("  Feeding practice:")
        row("  Assessment", r.level.label)
        row("  Questions answered", "${r.practiceAnswered} of ${r.practiceTotal}")
        if (r.backgroundAnswered > 0) {
            row(
                "  Household factors",
                "${r.backgroundRisks.toInt()} risk factor(s) of ${r.backgroundTotal}",
            )
        }
        if (r.concerns.isNotEmpty()) {
            appendLine()
            appendLine("  To discuss:")
            r.concerns.forEach { appendLine("   * $it") }
        }
        appendLine()
        appendLine("  ${r.level.guidance}")
    }

    /**
     * Module 2, in full.
     *
     * Both halves. The camera session and the caregiver questionnaire are separate
     * instruments answering different questions, and the questionnaire half was previously
     * absent from every report despite being the only half that works below nine months.
     */
    private fun StringBuilder.attention(session: AssessmentSession) {
        section("3. Attention and gaze")
        val age = session.ageMonthsAtAssessment
        val phase = AttentionPhase.forAge(age)
        row("Age phase", phase.label)
        row("Scored", if (phase.scored) "yes" else "no - observation only at this age")
        appendLine("  Purpose: ${phase.purpose}")
        appendLine()

        // --- camera half ---------------------------------------------------
        appendLine("  Camera session:")
        val a = session.attention
        when {
            a == null -> appendLine("   Not attempted.")
            a.inconclusive -> {
                appendLine("   NOT COMPLETED - the camera could not track reliably enough.")
                appendLine("   This is not a low result. No valid measurement was obtained.")
                row("   Usable frames", "${(a.validFrameRatio * 100).toInt()}%")
                row("   Duration", "${a.durationMs / 1000} s")
            }
            else -> {
                row("   Task", a.taskType.name.replace('_', ' ').lowercase())
                row("   Duration", "${a.durationMs / 1000} s")
                row("   Usable frames", "${(a.validFrameRatio * 100).toInt()}%")
                row("   Time looking at screen", "${(a.onScreenRatio * 100).toInt()}%")
                row("   Looking at social half", "${(a.socialAttentionRatio * 100).toInt()}%")
                a.responseToNameRatio?.let {
                    row("   Responded to name", "${(it * 100).toInt()}%")
                }
                a.disengagementLatencyMs?.let {
                    row("   Disengagement latency", "%.0f ms".format(it))
                }
                row("   Blink rate", "%.1f per min".format(a.blinkRatePerMin))
                row("   Engagement index", "${(a.engagementIndex * 100).toInt()}%")
                row("   Band", a.band.name.replace('_', ' ').lowercase())

                appendLine()
                appendLine("   Looking preference:")
                row("    Social side", "${(a.socialAttentionRatio * 100).toInt()}%")
                // Not the complement of the social share: time on neither side counts
                // towards neither, so the two do not sum to 100.
                row("    Geometric side", "${(a.geometricPreference * 100).toInt()}%")

                appendLine()
                appendLine("   Joint attention (gaze following):")
                if (a.gazeFollowRatio == null) {
                    appendLine("    Not measured.")
                } else {
                    row("    Followed the cue", "${(a.gazeFollowRatio * 100).toInt()}%")
                    a.gazeFollowLatencyMs?.let { row("    Mean latency", "%.0f ms".format(it)) }
                }

                if (a.pursuitCorrelation != null) {
                    appendLine()
                    appendLine("   Following a moving target:")
                    row("    Eye-target correlation", "%.2f".format(a.pursuitCorrelation))
                    a.pursuitTrackingFraction?.let {
                        row("    Time on target", "${(it * 100).toInt()}%")
                    }
                }

                a.disengagementRatio?.let {
                    appendLine()
                    row("   Shifted attention when asked", "${(it * 100).toInt()}%")
                }

                if (a.touchTargetsShown > 0) {
                    appendLine()
                    appendLine("   Touch task:")
                    row("    Bubbles popped", "${a.touchTargetsPopped} of ${a.touchTargetsShown}")
                    a.meanTouchLatencyMs?.let { row("    Mean reaction", "$it ms") }
                }

                appendLine()
                appendLine("   Data quality:")
                row("    Confidence", "${(a.confidence * 100).toInt()}%")
                row("    Usable trials", "${a.validTrialCount} of ${a.seenTrialCount}")
                row("    Mean fixation", "${a.meanFixationMs} ms")
                row("    Head movement", "%.1f deg/s".format(a.headMovementRate))
            }
        }

        // --- caregiver half ------------------------------------------------
        appendLine()
        appendLine("  Caregiver-reported markers:")
        val questions = EarlyMarkerBank.forAge(age)
        val m = EarlyMarkerResult.evaluate(questions, session.markerAnswers)
        if (m.answered == 0) {
            appendLine("   Not assessed.")
        } else {
            row("   Assessment", m.signal.label)
            row("   Questions answered", "${m.answered} of ${m.total}")
            row("   Markers noted", "%.1f".format(m.markerCount))
            if (m.regressionReported) {
                appendLine("   REGRESSION REPORTED - loss of a previously held skill.")
                appendLine("   This warrants referral on its own, whatever else is here.")
            }
            if (m.criticalMarkers.isNotEmpty()) {
                appendLine()
                appendLine("   Markers of note:")
                m.criticalMarkers.forEach { appendLine("    * $it") }
            }
            appendLine()
            appendLine("   ${m.signal.guidance}")
        }

        appendLine()
        appendLine("  This is a screening aid, not a diagnosis, and it names no condition.")
        appendLine("  A phone camera runs at roughly 15-30 frames per second and cannot")
        appendLine("  resolve eyes versus mouth, so no such metric is reported.")
    }

    /**
     * Module 3, in full.
     *
     * Reports the measurement (skin type) and the model (risk tier) separately, because they
     * carry different weight: one is a reading with a published basis, the other a score
     * built on stated assumptions.
     */
    private fun StringBuilder.vitaminD(session: AssessmentSession) {
        section("4. Vitamin D")
        val v = session.vitaminD
        val ita = session.skinItaDegrees
        val type = SkinType.fromIta(ita)

        // --- measured ------------------------------------------------------
        appendLine("  Skin typing (measured):")
        if (type == null || ita == null) {
            appendLine("   No skin reading taken.")
            appendLine("   Skin type is the largest single risk factor, so any score below")
            appendLine("   is likely an underestimate.")
        } else {
            row("   ITA angle", "%.1f deg".format(ita))
            row("   Skin type", "${type.roman} (${type.itaLabel})")
            row("   Sun needed", "about ${SunExposureFactor.suggestedMinutes(ita)} min midday")
            row("   Relative to lightest skin", "${type.exposureMultiplier}x exposure time")
            val margin = SkinType.marginToEdge(ita)
            if (margin < SkinType.BORDERLINE_MARGIN) {
                appendLine("   BORDERLINE - within %.1f deg of the next skin type.".format(margin))
            }
            appendLine("   Method: grey-card white balance, CIE L*a*b*, ITA = arctan((L*-50)/b*).")
        }

        // --- supplementation ------------------------------------------------
        appendLine()
        val advice = ProphylaxisAdvice.forAge(session.ageMonthsAtAssessment)
        appendLine("  Supplementation:")
        appendLine("   ${advice.headline}")
        val status = runCatching { ProphylaxisStatus.valueOf(session.prophylaxisStatus) }
            .getOrDefault(ProphylaxisStatus.NOT_ASKED)
        row("   Currently", if (status == ProphylaxisStatus.NOT_ASKED) "not asked" else status.label)

        // --- rickets signs ---------------------------------------------------
        appendLine()
        appendLine("  Rickets sign check (RBSK item B5):")
        val screen = RicketsScreenResult.evaluate(session.vitaminDAnswers)
        if (session.vitaminDAnswers.isEmpty()) {
            appendLine("   Not assessed.")
        } else if (screen.signsPresent.isEmpty()) {
            appendLine("   No signs observed.")
        } else {
            screen.signsPresent.forEach { appendLine("    * ${it.question}") }
            if (screen.urgent) {
                appendLine("   URGENT - needs to be seen today.")
            } else if (screen.refer) {
                appendLine("   Refer for a doctor's check.")
            }
        }

        // --- modelled --------------------------------------------------------
        appendLine()
        appendLine("  Deficiency risk (modelled):")
        if (v == null) {
            appendLine("   Not assessed.")
        } else {
            val risk = VitaminDRiskModel.assess(
                skinType = type,
                answers = session.vitaminDAnswers,
                ageMonths = session.ageMonthsAtAssessment,
                prophylaxis = status,
                sunWindowAvailable = null,
            )
            row("   Risk tier", risk.tier.label)
            row("   Score", "${risk.points} of ${risk.maxPoints} applicable risk points")
            row("   Referral band", v.band.name.replace('_', ' ').lowercase())
            if (risk.present.isNotEmpty()) {
                appendLine()
                appendLine("   Contributing factors:")
                risk.present.sortedByDescending { it.points }.forEach {
                    appendLine("    * ${it.label}  (+${it.points})")
                }
            }
            appendLine()
            appendLine("   ${risk.tier.plain}")
        }

        appendLine()
        appendLine("  This is a risk score built from established risk factors, not a")
        appendLine("  measurement. No validated non-invasive test for vitamin D status")
        appendLine("  exists. Only a blood test measures how much vitamin D a child has.")
    }

    private fun StringBuilder.immunization(session: AssessmentSession) {
        section("5. Immunisation")
        if (session.givenVaccines.isEmpty()) {
            appendLine("  No doses recorded.")
            return
        }
        row("Doses recorded", "${session.givenVaccines.size}")
        session.givenVaccines.forEach { appendLine("   * $it") }
    }

    private fun StringBuilder.footer() {
        appendLine()
        rule()
        appendLine("This is a screening report, not a diagnosis. It is intended to help decide")
        appendLine("whether a child should be seen by a developmental paediatrician or an")
        appendLine("RCI-registered clinical psychologist. Please bring this report to that visit.")
        rule()
    }

    // ---------------------------------------------------------------------

    private fun prettyCategory(raw: String): String =
        raw.split('_').joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }

    private fun statusLabel(status: DomainStatus): String = when (status) {
        DomainStatus.ON_TRACK -> "On track"
        DomainStatus.CONCERN -> "Some concern"
        DomainStatus.DELAYED -> "Delayed — needs review"
        DomainStatus.NOT_ASSESSED -> "Not assessed"
    }

    private fun percentileWord(p: Float): String = when {
        p < 1f -> "below 1st percentile"
        p > 99f -> "above 99th percentile"
        else -> "${p.toInt()}th percentile"
    }
}

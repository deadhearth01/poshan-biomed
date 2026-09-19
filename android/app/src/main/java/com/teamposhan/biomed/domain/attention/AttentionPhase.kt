package com.teamposhan.biomed.domain.attention

/**
 * Age-adaptive phase structure for module 2.
 *
 * ## Why phases at all
 *
 * The meaning of a looking measurement changes with age, so one universal threshold cannot
 * serve a 3-month-old and a 3-year-old. Response to name is not a valid probe before about
 * 12 months; gaze following barely exists before 9; a 2-month-old cannot do a touch task.
 * Comparing a raw gaze score against a single cut-off across 0-60 months would be
 * meaningless at both ends.
 *
 * ## Why FOUR phases and not five
 *
 * A five-phase design splitting 0-2 and 2-6 months was considered and rejected. The
 * proposal rested on Jones & Klin (2013), which found that **eye fixation** declines between
 * 2 and 6 months in infants later diagnosed with autism, and that the decline matters more
 * than an initially low level. That finding is real and important — but it is a measurement
 * of the **eye region specifically**, made on a calibrated corneal-reflection tracker.
 *
 * On a phone the eye region subtends roughly 2-4 degrees, while uncalibrated RGB gaze error
 * is 2-6 degrees and even calibrated infrared tracking of toddlers averages 1.3 degrees.
 * **We cannot separate eyes from mouth, so we cannot reproduce that signal.** Building the
 * most important infant phase on the one metric this hardware cannot measure would produce
 * confident numbers with nothing underneath them.
 *
 * The supporting evidence is also thinner than it first appears below 12 months: a
 * commentary on Jones & Klin notes inconsistent findings across infant eye-tracking studies
 * up to 12 months, with some finding group differences and others none; and the 2025
 * systematic review of automated infant eye tracking (793 articles, first two years) found
 * the literature too heterogeneous in paradigm, dependent variable and age for
 * meta-analysis, flagging power and replication as open problems.
 *
 * So the infant window is retained — but as **observation, not scoring**.
 *
 * ## Where the boundaries come from
 *
 * Not invented. Each aligns to both a published boundary and a milestone band:
 *
 *  - **12 months** — response to name becomes a valid probe (100% of low-risk controls
 *    respond by the first or second call at 12 months); gaze following is established
 *    (92% producing joint-attention bids by 9 months). Below this, the probes do not work.
 *  - **18/19 months** — the subgroup split used by the 2026 eye-tracking screening
 *    meta-analysis is exactly "≤18 months" versus "19-36 months".
 *  - **36/37 months** — the upper bound of that meta-analysis's evidence, and of most
 *    validated cohorts.
 *  - **48 months** — GeoPref's validated ceiling. Beyond it there is no phone-based
 *    evidence at all.
 *
 * These also line up with the milestone [com.teamposhan.biomed.domain.milestones.AgeBand]
 * boundaries, so the two timelines interlock rather than cutting across each other.
 */
enum class AttentionPhase(
    val minMonths: Float,
    val maxMonths: Float,
    val label: String,
    /** Whether a risk score may be emitted at all in this phase. */
    val scored: Boolean,
    val purpose: String,
) {
    /**
     * Observation only. Establishes the child's own baseline so later visits have something
     * to be compared against, and feeds the caregiver questionnaire — which *can* ask about
     * eye contact and its decline, the thing the camera cannot resolve.
     */
    INFANT_OBSERVATION(
        0f, 12f,
        "Infant observation",
        scored = false,
        purpose = "Baseline visual orienting and face looking. No risk score: no phone-based " +
            "screener is validated below 12 months, and the key published infant signal " +
            "(eye-region fixation decline, 2-6 months) is below this hardware's resolution.",
    ),

    /**
     * The first phase where the social probes are developmentally valid. Evidence here is
     * real but thinner than the next phase — SenseToKnow's validated floor is 16 months,
     * EarliPoint's is 16.
     */
    EARLY_SCREENING(
        12f, 19f,
        "Early social attention",
        scored = true,
        purpose = "Geometric-versus-social preference, response to name, and gaze following " +
            "become valid probes. Scored, with the caveat that the strongest cohorts start " +
            "at 16 months.",
    ),

    /**
     * The peak-evidence window: SenseToKnow (16-40 months, AUC 0.92 remote), EarliPoint
     * (16-30), GeoPref (12-48), and the meta-analysis's own 19-36 subgroup all sit here.
     */
    CORE_SCREENING(
        19f, 37f,
        "Core social screening",
        scored = true,
        purpose = "The full battery against the strongest evidence base. Geo/Social is the " +
            "most consistent paradigm across the literature (Hedges g = -0.91, I-squared = 0%).",
    ),

    /**
     * Above the validated ceiling of every phone-based screener. Run, but never scored as
     * though the norms transferred.
     */
    PRESCHOOL_EXPLORATORY(
        37f, 61f,
        "Preschool (exploratory)",
        scored = false,
        purpose = "GeoPref is validated to 48 months and nothing phone-based beyond it. " +
            "Complex social scenes are presented and recorded, but no risk score is emitted.",
    );

    val ageLabel: String
        get() = if (maxMonths >= 61f) "${minMonths.toInt()}-60 months"
        else "${minMonths.toInt()}-${(maxMonths - 1).toInt()} months"

    companion object {
        fun forAge(ageMonths: Float): AttentionPhase =
            entries.lastOrNull { ageMonths >= it.minMonths } ?: INFANT_OBSERVATION

        /** Phases whose results may contribute to a composite risk indicator. */
        val scorable: List<AttentionPhase> get() = entries.filter { it.scored }
    }
}

/**
 * The stages every session runs through, regardless of phase.
 *
 * Separating these from the age phases is what keeps the design tractable: the *stages* are
 * fixed machinery, the *content* of stage 3 is what varies by age.
 */
enum class SessionStage(val label: String, val purpose: String) {
    /**
     * Nothing is scored until the tracking itself is shown to be working. A session that
     * fails here is reported as "not completed", never as low risk.
     */
    QUALITY_GATE(
        "Set-up and tracking check",
        "Face detected, both irises tracked, head pose inside the envelope, achieved frame " +
            "rate adequate, distance plausible. Also the window in which implicit " +
            "calibration is harvested from an attention-getter at a known screen position.",
    ),

    /**
     * Each child is their own control. An absolute looking percentage means little without
     * knowing how this child attends to a neutral stimulus on this device today.
     */
    BASELINE(
        "Baseline attention",
        "A neutral stimulus establishes this child's own on-screen attention and fixation " +
            "characteristics before any social contrast is introduced.",
    ),

    /** The only stage whose content depends on [AttentionPhase]. */
    AGE_SPECIFIC_TASKS(
        "Age-specific tasks",
        "Phase-appropriate social and attention probes.",
    ),

    /**
     * The stage that makes the module worth repeating. A single session gives a level; the
     * comparison across sessions gives a direction, and direction is what the developmental
     * guidance actually asks caregivers to watch for.
     */
    TRAJECTORY(
        "Trajectory",
        "Compare against this child's previous sessions. Report the change and its " +
            "direction, not only today's level.",
    ),
}

/**
 * Which probes run in a given phase.
 *
 * Deliberately conservative: a probe appears only where it is developmentally valid, which
 * is why the infant phase carries so few. Attention disengagement appears nowhere at all —
 * the gap-overlap paradigm needs 15 degrees of eccentricity and a 6 inch phone at 30 cm can
 * only reach 13.0.
 */
object PhaseProtocol {

    enum class Probe(val label: String, val minMonths: Float) {
        VISUAL_ORIENTING("Visual orienting to a moving target", 0f),
        FACE_LOOKING("Face looking", 0f),
        SOCIAL_VS_GEOMETRIC("Social versus geometric preference", 12f),
        RESPONSE_TO_NAME("Response to name", 12f),
        GAZE_FOLLOWING("Gaze following", 12f),
        COMPLEX_SOCIAL("Complex social scene preference", 19f),
        TOUCH_TASK("Bubble-popping touch task", 16f),
    }

    fun probesFor(phase: AttentionPhase): List<Probe> = when (phase) {
        AttentionPhase.INFANT_OBSERVATION -> listOf(
            Probe.VISUAL_ORIENTING,
            Probe.FACE_LOOKING,
        )
        AttentionPhase.EARLY_SCREENING -> listOf(
            Probe.FACE_LOOKING,
            Probe.SOCIAL_VS_GEOMETRIC,
            Probe.RESPONSE_TO_NAME,
            Probe.GAZE_FOLLOWING,
            Probe.TOUCH_TASK,
        )
        AttentionPhase.CORE_SCREENING -> listOf(
            Probe.FACE_LOOKING,
            Probe.SOCIAL_VS_GEOMETRIC,
            Probe.COMPLEX_SOCIAL,
            Probe.RESPONSE_TO_NAME,
            Probe.GAZE_FOLLOWING,
            Probe.TOUCH_TASK,
        )
        AttentionPhase.PRESCHOOL_EXPLORATORY -> listOf(
            Probe.SOCIAL_VS_GEOMETRIC,
            Probe.COMPLEX_SOCIAL,
            Probe.GAZE_FOLLOWING,
            Probe.TOUCH_TASK,
        )
    }

    /**
     * Metrics reportable in this phase: those the hardware supports, intersected with the
     * probes that actually ran.
     */
    fun reportableMetrics(phase: AttentionPhase): List<GazeMetric> {
        if (!phase.scored) return emptyList()
        val probes = probesFor(phase).toSet()
        return GazeMetric.reportable.filter { metric ->
            when (metric) {
                GazeMetric.SOCIAL_SCENE_FIXATION,
                GazeMetric.OBJECT_FIXATION,
                GazeMetric.SOCIAL_PREFERENCE,
                GazeMetric.SOCIAL_VS_OBJECT_ALLOCATION,
                -> Probe.SOCIAL_VS_GEOMETRIC in probes
                GazeMetric.FACE_FIXATION, GazeMetric.FACE_PREFERENCE -> Probe.FACE_LOOKING in probes
                GazeMetric.ATTENTION_PERSISTENCE -> true
                else -> false
            }
        }
    }
}

/**
 * How often module 2 should be repeated.
 *
 * The milestone questionnaire runs once per age band. Gaze does not need to be that
 * frequent above two years, and benefits from being *more* frequent below one — not because
 * a score is produced there, but because the trajectory needs points on it.
 */
object AttentionSchedule {

    /** Target ages in months for a gaze/attention session. */
    val visitMonths: List<Int> = listOf(3, 6, 9, 12, 15, 18, 24, 30, 36, 48, 60)

    fun nextVisitAfter(ageMonths: Float): Int? =
        visitMonths.firstOrNull { it > ageMonths }

    /** True when this child is due a session now, within a one-month window. */
    fun isDue(ageMonths: Float, completedAtMonths: List<Float>): Boolean {
        val target = visitMonths.lastOrNull { ageMonths >= it - 1f } ?: return false
        return completedAtMonths.none { kotlin.math.abs(it - target) <= 2f }
    }
}

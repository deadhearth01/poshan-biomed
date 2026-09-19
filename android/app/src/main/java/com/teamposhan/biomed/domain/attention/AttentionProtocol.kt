package com.teamposhan.biomed.domain.attention

/**
 * Age-banded eye-tracking protocol.
 *
 * The design follows the published evidence for phone-feasible screening:
 *  - large half-screen AOIs (robust to the ~1â€“4Â° accuracy of a front camera)
 *  - social vs non-social preferential looking, sides counterbalanced per trial
 *  - response-to-name windows (an early social-orienting marker)
 *  - a simplified gap/overlap disengagement probe ("does gaze leave the centre?")
 *  - short total duration, because sustained compliance falls off fast under 5 years
 *
 * Under 5 months gaze mapping is not reliable, so the plan switches to a general
 * movement observation instead of forcing a gaze task.
 */
/**
 * Which camera task, if any, a child of a given age can be given.
 *
 * ## Why the gaze tasks start at 9 months
 *
 * Every probe in the gaze protocol depends on a skill the child has to already possess:
 *
 *  - **Paired preference** needs the infant to disengage from one side and shift to the
 *    other on their own. Before about 4 months, attention is "sticky" and a young infant can
 *    remain locked on the first thing shown for the whole trial, which scores as a
 *    preference for whatever happened to be on that side.
 *  - **Gaze following** is what makes a cue trial mean anything, and it is not reliably
 *    present before 9 months. Roughly 9 in 10 infants are producing joint-attention bids by
 *    then; earlier, a null result says nothing about the child.
 *  - **Response to name** is weakest of all this early and only becomes a usable probe near
 *    12 months.
 *
 * Running these on a 4-month-old produces numbers, which is precisely the danger: they look
 * like measurements and carry no information. So below 9 months the module does not open the
 * camera at all. It asks the caregiver instead, through the same yes/maybe/no questionnaire
 * module 1 uses, because a parent *can* report eye contact, social smiling and whether it
 * has changed — the things the camera cannot resolve at this age.
 *
 * Between 9 and 12 months the camera runs but nothing is scored; see
 * [AttentionPhase.INFANT_OBSERVATION]. The session establishes this child's own baseline for
 * later visits to be read against.
 */
enum class AttentionBand(val label: String, val minMonths: Float, val maxMonths: Float) {
    /** No camera task. The caregiver questionnaire carries this age entirely. */
    MOVEMENT("Under 9 months", 0f, 9f),
    INFANT("9-24 months", 9f, 24f),
    PRESCHOOL("2-5 years", 24f, 60f);

    /** Whether the camera protocol can be run at all at this age. */
    val cameraTask: Boolean get() = this != MOVEMENT

    companion object {
        /** The age below which the camera protocol is not attempted. */
        const val GAZE_MIN_MONTHS = 9f

        fun forAge(months: Float): AttentionBand = when {
            months < GAZE_MIN_MONTHS -> MOVEMENT
            months < 24f -> INFANT
            else -> PRESCHOOL
        }
    }
}

sealed interface AttentionSegment {
    val startMs: Long
    val endMs: Long
    val label: String

    /** Plain attention-getter; used between trials and before the first pair. */
    data class Getter(override val startMs: Long, override val endMs: Long) : AttentionSegment {
        override val label: String get() = "Attention"
    }

    /**
     * Which social stimulus is paired against the dynamic geometric one.
     *
     * The GeoPref test originally paired geometry against *simple* social video (children
     * doing yoga). Pierce et al. 2018 (PMID 29581878) added a **Complex Social** condition —
     * richer, more socially demanding scenes — and tested whether the effect generalised.
     * It did, and combining both conditions identified about a third of autistic toddlers
     * as the GeoPref subtype: sensitivity 35%, specificity 94%, AUC 0.75 in 227 toddlers
     * (76 with autism), against roughly half that sensitivity for the original condition
     * alone.
     *
     * So both conditions are presented and counterbalanced. Running only one leaves half
     * the available sensitivity on the table for no saving in session length that matters.
     */
    enum class PairCondition(val label: String) {
        /** Original GeoPref: dynamic geometry vs simple social video. */
        SIMPLE_SOCIAL("Simple social"),

        /** Complex Social GeoPref: dynamic geometry vs a richer social scene. */
        COMPLEX_SOCIAL("Complex social"),
    }

    /** Social vs non-social split presentation. */
    data class Pair(
        override val startMs: Long,
        override val endMs: Long,
        val trialIndex: Int,
        val socialOnLeft: Boolean,
        val condition: PairCondition = PairCondition.SIMPLE_SOCIAL,
    ) : AttentionSegment {
        override val label: String get() = "Looking"
    }

    /** Caregiver (or the app) calls the child's name; gaze/head turn is measured. */
    data class NameCall(
        override val startMs: Long,
        override val endMs: Long,
        val index: Int,
    ) : AttentionSegment {
        override val label: String get() = "Name call"
    }

    /** Central stimulus, then a peripheral target â€” measures disengagement. */
    data class Disengage(
        override val startMs: Long,
        override val endMs: Long,
        val trialIndex: Int,
        val targetOnLeft: Boolean,
    ) : AttentionSegment {
        override val label: String get() = "Shifting"
    }

    /**
     * Joint-attention probe (Gazefinder-style gaze following): a face turns its eyes AND
     * head to one side, then a target appears there. We measure whether the child's first
     * fixation lands on the cued side, and how quickly.
     */
    data class GazeFollow(
        override val startMs: Long,
        override val endMs: Long,
        val trialIndex: Int,
        val cueOnLeft: Boolean,
    ) : AttentionSegment {
        override val label: String get() = "Following"
    }

    /** Free movement observation for infants under 5 months. */
    data class MovementObservation(
        override val startMs: Long,
        override val endMs: Long,
    ) : AttentionSegment {
        override val label: String get() = "Movement"
    }
}

data class AttentionPlan(
    val band: AttentionBand,
    val segments: List<AttentionSegment>,
    val totalMs: Long,
    val distanceCm: Int,
    val caregiverHoldsDevice: Boolean,
    val gazeEnabled: Boolean,
    val pairTrials: Int,
    val nameCalls: Int,
    val disengagementTrials: Int,
    val gazeFollowTrials: Int,
) {
    fun segmentAt(elapsedMs: Long): AttentionSegment? =
        segments.firstOrNull { elapsedMs >= it.startMs && elapsedMs < it.endMs }

    fun aoiAt(elapsedMs: Long): AoiContext? {
        val segment = segmentAt(elapsedMs)
        return if (segment is AttentionSegment.Pair) AoiContext(segment.socialOnLeft) else null
    }

    fun progress(elapsedMs: Long): Float =
        if (totalMs <= 0) 0f else (elapsedMs.toFloat() / totalMs).coerceIn(0f, 1f)
}

object AttentionProtocol {

    private const val GETTER_MS = 3_000L
    private const val PAIR_MS = 11_000L
    private const val NAME_WINDOW_MS = 4_000L
    private const val NAME_GAP_MS = 2_000L
    private const val DISENGAGE_CENTRE_MS = 2_500L
    private const val DISENGAGE_WINDOW_MS = 4_000L
    private const val GAZE_FOLLOW_MS = 4_500L
    private const val MOVEMENT_OBSERVATION_MS = 180_000L

    fun build(ageMonths: Float): AttentionPlan {
        val band = AttentionBand.forAge(ageMonths)
        return when (band) {
            AttentionBand.MOVEMENT -> AttentionPlan(
                band = band,
                segments = listOf(
                    AttentionSegment.MovementObservation(0, MOVEMENT_OBSERVATION_MS)
                ),
                totalMs = MOVEMENT_OBSERVATION_MS,
                distanceCm = 30,
                caregiverHoldsDevice = true,
                gazeEnabled = false,
                pairTrials = 0,
                nameCalls = 0,
                disengagementTrials = 0,
                gazeFollowTrials = 0,
            )

            AttentionBand.INFANT -> {
                val pairs = 4
                val names = 3
                val gazeFollow = 3
                val segments = mutableListOf<AttentionSegment>()
                var t = 0L

                segments += AttentionSegment.Getter(t, t + GETTER_MS); t += GETTER_MS
                repeat(pairs) { i ->
                    segments += AttentionSegment.Pair(
                        t, t + PAIR_MS, i,
                        socialOnLeft = i % 2 == 1,
                        // Side alternates every trial, condition every second trial, so
                        // side and condition are crossed rather than confounded.
                        condition = if ((i / 2) % 2 == 0) {
                            AttentionSegment.PairCondition.SIMPLE_SOCIAL
                        } else {
                            AttentionSegment.PairCondition.COMPLEX_SOCIAL
                        },
                    )
                    t += PAIR_MS
                    if (i < pairs - 1) {
                        segments += AttentionSegment.Getter(t, t + GETTER_MS)
                        t += GETTER_MS
                    }
                }
                repeat(names) { i ->
                    segments += AttentionSegment.Getter(t, t + NAME_GAP_MS); t += NAME_GAP_MS
                    segments += AttentionSegment.NameCall(t, t + NAME_WINDOW_MS, i); t += NAME_WINDOW_MS
                }
                repeat(gazeFollow) { i ->
                    segments += AttentionSegment.Getter(t, t + GETTER_MS); t += GETTER_MS
                    segments += AttentionSegment.GazeFollow(
                        startMs = t,
                        endMs = t + GAZE_FOLLOW_MS,
                        trialIndex = i,
                        cueOnLeft = i % 2 == 0,
                    )
                    t += GAZE_FOLLOW_MS
                }

                AttentionPlan(
                    band = band,
                    segments = segments,
                    totalMs = t,
                    distanceCm = 30,
                    caregiverHoldsDevice = true,
                    gazeEnabled = true,
                    pairTrials = pairs,
                    nameCalls = names,
                    disengagementTrials = 0,
                    gazeFollowTrials = gazeFollow,
                )
            }

            AttentionBand.PRESCHOOL -> {
                val pairs = 6
                val names = 3
                val disengage = 3
                val gazeFollow = 3
                val segments = mutableListOf<AttentionSegment>()
                var t = 0L

                segments += AttentionSegment.Getter(t, t + GETTER_MS); t += GETTER_MS
                repeat(pairs) { i ->
                    segments += AttentionSegment.Pair(
                        t, t + PAIR_MS, i,
                        socialOnLeft = i % 2 == 1,
                        // Side alternates every trial, condition every second trial, so
                        // side and condition are crossed rather than confounded.
                        condition = if ((i / 2) % 2 == 0) {
                            AttentionSegment.PairCondition.SIMPLE_SOCIAL
                        } else {
                            AttentionSegment.PairCondition.COMPLEX_SOCIAL
                        },
                    )
                    t += PAIR_MS
                    if (i < pairs - 1) {
                        segments += AttentionSegment.Getter(t, t + GETTER_MS)
                        t += GETTER_MS
                    }
                }
                repeat(names) { i ->
                    segments += AttentionSegment.Getter(t, t + NAME_GAP_MS); t += NAME_GAP_MS
                    segments += AttentionSegment.NameCall(t, t + NAME_WINDOW_MS, i); t += NAME_WINDOW_MS
                }
                repeat(disengage) { i ->
                    segments += AttentionSegment.Disengage(
                        startMs = t,
                        endMs = t + DISENGAGE_CENTRE_MS + DISENGAGE_WINDOW_MS,
                        trialIndex = i,
                        targetOnLeft = i % 2 == 0,
                    )
                    t += DISENGAGE_CENTRE_MS + DISENGAGE_WINDOW_MS
                }
                repeat(gazeFollow) { i ->
                    segments += AttentionSegment.Getter(t, t + GETTER_MS); t += GETTER_MS
                    segments += AttentionSegment.GazeFollow(
                        startMs = t,
                        endMs = t + GAZE_FOLLOW_MS,
                        trialIndex = i,
                        cueOnLeft = i % 2 == 0,
                    )
                    t += GAZE_FOLLOW_MS
                }

                AttentionPlan(
                    band = band,
                    segments = segments,
                    totalMs = t,
                    distanceCm = 45,
                    caregiverHoldsDevice = false,
                    gazeEnabled = true,
                    pairTrials = pairs,
                    nameCalls = names,
                    disengagementTrials = disengage,
                    gazeFollowTrials = gazeFollow,
                )
            }
        }
    }
}

/** Setup instructions shown before the session â€” the quality of these decides the data. */
data class SetupStep(val title: String, val body: String)

object AttentionInstructions {

    fun steps(plan: AttentionPlan): List<SetupStep> = buildList {
        add(
            SetupStep(
                "Distance",
                if (plan.band == AttentionBand.MOVEMENT) {
                    "Lay the baby on their back and hold the phone about 30 cm above them."
                } else if (plan.caregiverHoldsDevice) {
                    "Hold the phone about 30 cm (one forearm) from your child's face, at their eye level."
                } else {
                    "Place the phone about 45 cm (arm's length) from your child, at their eye level."
                }
            )
        )
        add(
            SetupStep(
                "Lighting",
                "Use a well-lit room with the light behind the phone. Avoid a bright window or lamp behind the child â€” it darkens the face."
            )
        )
        add(
            SetupStep(
                "Position",
                "Keep the phone steady. Rest it on a stand or your hand â€” do not move it during the check."
            )
        )
        add(
            SetupStep(
                "Environment",
                "Quiet room, no TV, no siblings behind the phone. Turn off notifications."
            )
        )
        if (plan.caregiverHoldsDevice) {
            add(
                SetupStep(
                    "Your role",
                    "Sit behind the phone and stay quiet. Only speak when the app asks you to call your child's name."
                )
            )
        } else {
            add(
                SetupStep(
                    "Your role",
                    "Sit beside or behind your child, out of the camera. Stay quiet unless the app asks you to call their name."
                )
            )
        }
        add(
            SetupStep(
                "What happens",
                "Your child will see simple moving pictures â€” a friendly face on one side and a moving shape on the other. We watch where they look and how they respond to their name. It takes about " +
                    "${(plan.totalMs / 60_000f).let { if (it < 1f) "1" else "%.0f".format(it) }} minute(s)."
            )
        )
        add(
            SetupStep(
                "If your child is not interested",
                "Stop any time. If we cannot collect enough usable frames we will simply say the check was inconclusive â€” that is not a delay."
            )
        )
    }
}

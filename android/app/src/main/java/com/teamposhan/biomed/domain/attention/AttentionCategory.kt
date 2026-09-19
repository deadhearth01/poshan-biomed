package com.teamposhan.biomed.domain.attention

/**
 * Module 2's user-facing structure: **five assessment categories plus a quality gate**.
 *
 * This replaces a flat list of twenty-odd metrics. Fixation counts, saccade rates, dwell
 * runs and gaze allocation are still computed — they are the *measurements inside* these
 * categories, not categories themselves. A caregiver, and a reviewing clinician, should see
 * five things and a pass/fail on data quality.
 *
 * It deliberately mirrors module 1, where five developmental domains each aggregate many
 * individual questions.
 *
 * ```
 * 1. Face & Eye Gaze          → social gaze
 * 2. Social vs Non-Social     → social preference
 * 3. Attention Shifting       → shift latency and success
 * 4. Gaze Following           → shared attention
 * 5. Visual Engagement        → sustained engagement
 * ── Session Quality ──────────  valid / invalid  (gates all of the above)
 * ```
 */
enum class AttentionCategory(
    val label: String,
    val summaryLabel: String,
    /** The youngest age at which this category means anything. */
    val minMonths: Float,
    val caregiverDescription: String,
) {
    FACE_AND_EYE_GAZE(
        label = "Face & eye gaze",
        summaryLabel = "Social gaze",
        minMonths = 0f,
        caregiverDescription = "How much of the time your child looks at a face on the screen.",
    ),
    SOCIAL_VS_NONSOCIAL(
        label = "Social vs non-social attention",
        summaryLabel = "Social preference",
        minMonths = 12f,
        caregiverDescription = "Given a choice between people and moving patterns, which your " +
            "child chooses to watch.",
    ),
    ATTENTION_SHIFTING(
        label = "Attention shifting",
        summaryLabel = "Shifting",
        minMonths = 6f,
        caregiverDescription = "How easily your child moves their attention from one thing to " +
            "another.",
    ),
    GAZE_FOLLOWING(
        label = "Gaze following & joint attention",
        summaryLabel = "Shared attention",
        minMonths = 12f,
        caregiverDescription = "Whether your child looks where someone else is looking.",
    ),
    VISUAL_ENGAGEMENT(
        label = "Visual engagement",
        summaryLabel = "Engagement",
        minMonths = 0f,
        caregiverDescription = "How long and how steadily your child stays interested in the screen.",
    );

    fun appliesAt(ageMonths: Float): Boolean = ageMonths >= minMonths

    companion object {
        fun forAge(ageMonths: Float): List<AttentionCategory> =
            entries.filter { it.appliesAt(ageMonths) }
    }
}

/**
 * The raw measurements behind each category.
 *
 * Stored as real quantities, never as invented 0-100 clinical scores. A standardised score
 * requires a reference dataset this project does not yet have; producing one now would mean
 * inventing the normative distribution, and every downstream reader would treat it as real.
 *
 * Nullable throughout, with no defaults. A missing measurement must never become zero —
 * zero social looking is itself a clinical finding and must not be manufactured by a gap in
 * the data.
 */
data class AttentionMeasurements(
    // --- 1. Face & eye gaze ---------------------------------------------
    val faceFixationPercent: Float? = null,
    val meanFixationDurationMs: Float? = null,
    val fixationCount: Int? = null,

    /**
     * Eye-region fixation as a fraction of face-looking time.
     *
     * **Always null on this hardware, by design.** The eye region subtends roughly 2-4
     * degrees; uncalibrated phone gaze error is 2-6 degrees, and even calibrated infrared
     * tracking of toddlers averages 1.3. Eyes cannot be separated from mouth. The field
     * exists so the shape of the record is stable if a better gaze model arrives, not so a
     * value can be guessed into it.
     */
    val eyeRegionFixationPercent: Float? = null,

    // --- 2. Social vs non-social ----------------------------------------
    val socialFixationPercent: Float? = null,
    val nonSocialFixationPercent: Float? = null,
    /** social / (social + non-social). The GeoPref primitive. */
    val socialPreferenceRatio: Float? = null,
    /** Same ratio for the Complex Social pairing, reported separately. */
    val complexSocialPreferenceRatio: Float? = null,

    // --- 3. Attention shifting ------------------------------------------
    /** Frames-quantised; a within-session index, not comparable to published latencies. */
    val shiftLatencyMs: Float? = null,
    val successfulShifts: Int? = null,
    val shiftTrials: Int? = null,
    val returnRate: Float? = null,

    // --- 4. Gaze following ----------------------------------------------
    val gazeFollowingSuccesses: Int? = null,
    val gazeFollowingTrials: Int? = null,
    val gazeFollowingLatencyMs: Float? = null,
    val alternationCount: Int? = null,

    // --- 5. Visual engagement -------------------------------------------
    val engagementPercent: Float? = null,
    val longestEngagementMs: Long? = null,
    val engagementInterruptions: Int? = null,

    // --- response to name (feeds category 4) -----------------------------
    val nameResponseRate: Float? = null,
    val nameResponseConsistent: Boolean? = null,
    val nameResponseLatencyMs: Float? = null,
) {
    /** The headline number for a category, or null when it was not measured. */
    fun headline(category: AttentionCategory): Float? = when (category) {
        AttentionCategory.FACE_AND_EYE_GAZE -> faceFixationPercent
        AttentionCategory.SOCIAL_VS_NONSOCIAL -> socialPreferenceRatio?.times(100f)
        AttentionCategory.ATTENTION_SHIFTING ->
            if (shiftTrials != null && shiftTrials > 0 && successfulShifts != null) {
                successfulShifts.toFloat() / shiftTrials * 100f
            } else null
        AttentionCategory.GAZE_FOLLOWING ->
            if (gazeFollowingTrials != null && gazeFollowingTrials > 0 && gazeFollowingSuccesses != null) {
                gazeFollowingSuccesses.toFloat() / gazeFollowingTrials * 100f
            } else null
        AttentionCategory.VISUAL_ENGAGEMENT -> engagementPercent
    }

    /** Every measurement in a category, for the detailed report. */
    fun detail(category: AttentionCategory): List<Pair<String, String>> = when (category) {
        AttentionCategory.FACE_AND_EYE_GAZE -> listOfNotNull(
            faceFixationPercent?.let { "Face fixation" to "%.0f%%".format(it) },
            meanFixationDurationMs?.let { "Mean fixation" to "%.0f ms".format(it) },
            fixationCount?.let { "Fixations" to "$it" },
            "Eye-region fixation" to "not measurable on a phone camera",
        )
        AttentionCategory.SOCIAL_VS_NONSOCIAL -> listOfNotNull(
            socialFixationPercent?.let { "Social" to "%.0f%%".format(it) },
            nonSocialFixationPercent?.let { "Non-social" to "%.0f%%".format(it) },
            socialPreferenceRatio?.let { "Preference (simple)" to "%.2f".format(it) },
            complexSocialPreferenceRatio?.let { "Preference (complex)" to "%.2f".format(it) },
        )
        AttentionCategory.ATTENTION_SHIFTING -> listOfNotNull(
            shiftLatencyMs?.let { "Shift latency" to "%.0f ms (relative)".format(it) },
            if (shiftTrials != null && successfulShifts != null) {
                "Successful shifts" to "$successfulShifts / $shiftTrials"
            } else null,
            returnRate?.let { "Return rate" to "%.0f%%".format(it * 100) },
        )
        AttentionCategory.GAZE_FOLLOWING -> listOfNotNull(
            if (gazeFollowingTrials != null && gazeFollowingSuccesses != null) {
                "Gaze following" to "$gazeFollowingSuccesses / $gazeFollowingTrials"
            } else null,
            gazeFollowingLatencyMs?.let { "Mean response" to "%.0f ms".format(it) },
            alternationCount?.let { "Person-object-person" to "$it" },
            nameResponseRate?.let { "Response to name" to "%.0f%%".format(it * 100) },
            nameResponseConsistent?.let { "Consistent across calls" to if (it) "Yes" else "No" },
        )
        AttentionCategory.VISUAL_ENGAGEMENT -> listOfNotNull(
            engagementPercent?.let { "Engaged time" to "%.0f%%".format(it) },
            longestEngagementMs?.let { "Longest stretch" to "%.1f s".format(it / 1000f) },
            engagementInterruptions?.let { "Interruptions" to "$it" },
        )
    }
}

/**
 * A category's outcome for one session.
 *
 * [band] is deliberately coarse and deliberately not a number out of 100. Until there is a
 * validated reference dataset, the app can honestly say "lower than expected for this age"
 * or "not enough data" — it cannot honestly say "61".
 */
data class AttentionCategoryResult(
    val category: AttentionCategory,
    val headlinePercent: Float?,
    val band: Band,
    val detail: List<Pair<String, String>>,
) {
    enum class Band(val label: String) {
        TYPICAL("Within expected range"),
        BORDERLINE("Slightly below expected"),
        LOW("Lower than expected"),
        NOT_MEASURED("Not measured this session"),
        NOT_APPLICABLE("Not assessed at this age"),
    }
}

/**
 * Session quality — the gate, not a sixth category.
 *
 * Nothing above is interpreted unless this passes. A screening instrument must never confuse
 * "the child did not look" with "the tracker failed", and reporting a low score from an
 * untrackable session does exactly that.
 */
data class SessionQualityVerdict(
    val valid: Boolean,
    val trackingConfidencePercent: Float,
    val validSamplePercent: Float,
    val faceDetected: Boolean,
    val excessiveHeadMovement: Boolean,
    val validTrials: Int,
    val requiredTrials: Int,
    val reason: String?,
) {
    companion object {
        /** Matches the published infant criterion this module anchors on. */
        const val MIN_VALID_SAMPLE_PERCENT = 50f
        const val MIN_VALID_TRIALS = 6

        fun from(quality: CaptureQuality, validTrials: Int): SessionQualityVerdict {
            val validPercent = quality.usableRatio * 100f
            val headMovement = quality.framesRejectedPose >
                (quality.framesWithFace.coerceAtLeast(1) / 2)
            val reason = when {
                quality.framesWithFace == 0 -> "The camera never detected a face."
                validPercent < MIN_VALID_SAMPLE_PERCENT ->
                    "Too few usable frames (%.0f%%). Try somewhere brighter and steadier."
                        .format(validPercent)
                headMovement -> "Too much head movement to track reliably."
                validTrials < MIN_VALID_TRIALS ->
                    "Only $validTrials of $MIN_VALID_TRIALS trials produced usable data."
                else -> null
            }
            return SessionQualityVerdict(
                valid = reason == null,
                trackingConfidencePercent = quality.faceRatio * 100f,
                validSamplePercent = validPercent,
                faceDetected = quality.framesWithFace > 0,
                excessiveHeadMovement = headMovement,
                validTrials = validTrials,
                requiredTrials = MIN_VALID_TRIALS,
                reason = reason,
            )
        }
    }
}

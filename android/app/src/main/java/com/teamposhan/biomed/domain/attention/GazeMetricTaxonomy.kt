package com.teamposhan.biomed.domain.attention

/**
 * The full looking-behaviour metric set, and — just as importantly — an honest statement of
 * which members of it this hardware can actually measure.
 *
 * Three groups:
 *
 * ```
 * GAZE                  ATTENTION                  VISUAL ENGAGEMENT
 *  - Eye-region fixation  - Orientation latency      - Social preference
 *  - Face fixation        - Fixation duration        - Face preference
 *  - Social-scene fixation- Attention disengagement  - Shared attention
 *  - Object fixation      - Attention persistence    - Social-vs-object allocation
 * ```
 *
 * A screening instrument that reports a number for a quantity it cannot resolve is worse
 * than one that reports nothing, because the number will be believed. Each metric therefore
 * carries a [Support] level derived from the physical limits of a single RGB front camera at
 * 15-30 Hz on a ~6 inch screen, and the UI must respect it: anything below [Support.FULL] is
 * never shown to a caregiver as a value with a normal range.
 */
enum class MetricGroup(val label: String) {
    GAZE("Gaze"),
    ATTENTION("Attention"),
    VISUAL_ENGAGEMENT("Visual engagement"),
}

/** How far this hardware can be trusted for a given metric. */
enum class Support {
    /** Measurable and reportable. */
    FULL,

    /** Computable, but only as a within-session or within-device relative index. */
    RELATIVE_ONLY,

    /** Physically out of reach here. Recorded as unavailable, never estimated. */
    UNSUPPORTED,
}

/**
 * One metric: what it is, which group it belongs to, whether we can measure it, and why.
 * The `why` strings are deliberately verbose — they are the answer to "can we just show
 * this number anyway", asked six months from now by someone without this context.
 */
enum class GazeMetric(
    val group: MetricGroup,
    val label: String,
    val support: Support,
    val rationale: String,
) {
    // ---- GAZE ------------------------------------------------------------
    EYE_REGION_FIXATION(
        MetricGroup.GAZE,
        "Eye-region fixation",
        Support.UNSUPPORTED,
        "The eye region of a face on a phone subtends roughly 2-4 degrees. Uncalibrated " +
            "RGB gaze error is 2-6 degrees, and even calibrated infrared tracking of toddlers " +
            "averages 1.3 degrees with an upper range near 3.9. Eyes-versus-mouth cannot be " +
            "separated at this scale.",
    ),
    FACE_FIXATION(
        MetricGroup.GAZE,
        "Face fixation",
        Support.FULL,
        "A face presented at half-screen size is a large AOI, comfortably larger than the " +
            "error budget. Proportion of usable frames with gaze inside the face AOI.",
    ),
    SOCIAL_SCENE_FIXATION(
        MetricGroup.GAZE,
        "Social-scene fixation",
        Support.FULL,
        "Half-screen AOI. This is the primitive Duke's SenseToKnow reports as percentage of " +
            "time gazing at the social half, and it is the outcome measure this module is built on.",
    ),
    OBJECT_FIXATION(
        MetricGroup.GAZE,
        "Object fixation",
        Support.FULL,
        "The complement of the social half. Same geometry, same confidence.",
    ),

    // ---- ATTENTION -------------------------------------------------------
    ORIENTATION_LATENCY(
        MetricGroup.ATTENTION,
        "Orientation latency",
        Support.RELATIVE_ONLY,
        "Quantised to the frame interval: 33 ms at 30 Hz, 67 ms at 15 Hz. Usable for " +
            "comparing trials within a session; not comparable to published latencies " +
            "collected at 60-500 Hz.",
    ),
    FIXATION_DURATION(
        MetricGroup.ATTENTION,
        "Fixation duration",
        Support.RELATIVE_ONLY,
        "The canonical minimum fixation is 100 ms, which is 3 samples at 30 Hz. Fixation " +
            "analysis is only reported as stable from 60 Hz upward. Retained as a relative " +
            "index; never reported in milliseconds against a norm.",
    ),
    ATTENTION_DISENGAGEMENT(
        MetricGroup.ATTENTION,
        "Attention disengagement",
        Support.UNSUPPORTED,
        "Two independent blockers. The published gap-overlap paradigm places the peripheral " +
            "target at 15 degrees eccentricity; the maximum reachable on a 6 inch phone is " +
            "13.0 degrees at 30 cm and 7.9 at 50 cm. And the discriminating group difference " +
            "is about 130 ms, which is 2-4 frames.",
    ),
    ATTENTION_PERSISTENCE(
        MetricGroup.ATTENTION,
        "Attention persistence",
        Support.FULL,
        "Longest continuous run of on-screen, usable frames. Robust at any frame rate " +
            "because it is a count of contiguous samples, not a timing measurement.",
    ),

    // ---- VISUAL ENGAGEMENT ----------------------------------------------
    SOCIAL_PREFERENCE(
        MetricGroup.VISUAL_ENGAGEMENT,
        "Social preference",
        Support.FULL,
        "Social dwell over total dwell across paired trials. The GeoPref primitive.",
    ),
    FACE_PREFERENCE(
        MetricGroup.VISUAL_ENGAGEMENT,
        "Face preference",
        Support.FULL,
        "Dwell on the face AOI relative to competing non-face content in the same trial.",
    ),
    SHARED_ATTENTION(
        MetricGroup.VISUAL_ENGAGEMENT,
        "Shared attention",
        Support.RELATIVE_ONLY,
        "Gaze-following: whether the first look after a cue lands on the cued side. The " +
            "side decision is sound, but no published accuracy norms by month exist, so it " +
            "is a model feature, never a pass or fail.",
    ),
    SOCIAL_VS_OBJECT_ALLOCATION(
        MetricGroup.VISUAL_ENGAGEMENT,
        "Social-vs-object allocation",
        Support.FULL,
        "The ratio of social to object dwell, and how it shifts across the session.",
    );

    companion object {
        fun inGroup(group: MetricGroup): List<GazeMetric> = entries.filter { it.group == group }
        val reportable: List<GazeMetric> get() = entries.filter { it.support == Support.FULL }
    }
}

/**
 * A computed value for one metric.
 *
 * [value] is null whenever the metric is unsupported or the session did not produce enough
 * usable data. There is deliberately no default: a missing measurement must never silently
 * become zero, because zero social looking is itself a clinically meaningful finding and
 * must not be manufactured by a gap in the data.
 */
data class MetricValue(
    val metric: GazeMetric,
    val value: Float?,
    /** Usable frames the value rests on, so a thin result can be recognised as thin. */
    val sampleCount: Int = 0,
) {
    val available: Boolean get() = value != null

    /** Formatted for display, honouring the support level. */
    fun display(): String = when {
        metric.support == Support.UNSUPPORTED -> "not measurable on this device"
        value == null -> "no data"
        else -> "%.0f%%".format(value * 100)
    }
}

/**
 * Longitudinal eye-contact trajectory.
 *
 * Jones & Klin (2013) found that infants later diagnosed with autism often begin with
 * typical eye-gaze patterns that **decline between 2 and 6 months**, and that the decline is
 * more clinically significant than an initial absence: it marks diminishing interest in
 * social connection during a formative period. By 24 months these children may look at a
 * caregiver's eyes for around half as long as their peers.
 *
 * A per-session score cannot see this. The comparison is between visits, which is why the
 * periodic assessment schedule matters to module 2 and not only to growth monitoring.
 */
object GazeTrajectory {

    data class Point(val ageMonths: Float, val socialLookingRatio: Float)

    enum class Trend { RISING, STABLE, DECLINING, INSUFFICIENT_DATA }

    data class Assessment(
        val trend: Trend,
        /** Change in social-looking ratio from the first visit to the latest. */
        val deltaRatio: Float?,
        val note: String,
    )

    /** A drop of this much in the social-looking ratio between visits is treated as a decline. */
    private const val DECLINE_THRESHOLD = 0.10f

    fun assess(points: List<Point>): Assessment {
        val ordered = points.sortedBy { it.ageMonths }
        if (ordered.size < 2) {
            return Assessment(
                Trend.INSUFFICIENT_DATA,
                null,
                "At least two completed sessions are needed before a trend means anything.",
            )
        }
        val first = ordered.first()
        val last = ordered.last()
        val delta = last.socialLookingRatio - first.socialLookingRatio

        val trend = when {
            delta <= -DECLINE_THRESHOLD -> Trend.DECLINING
            delta >= DECLINE_THRESHOLD -> Trend.RISING
            else -> Trend.STABLE
        }

        val note = when (trend) {
            Trend.DECLINING -> buildString {
                append("Social looking has fallen ")
                append("%.0f".format(kotlin.math.abs(delta) * 100))
                append(" points since ")
                append(formatAge(first.ageMonths))
                append(". A decline carries more weight than a consistently low level, ")
                append("particularly between 2 and 6 months. Discuss at the next visit.")
            }
            Trend.RISING -> "Social looking has increased since ${formatAge(first.ageMonths)}."
            Trend.STABLE -> "Social looking is tracking steadily since ${formatAge(first.ageMonths)}."
            Trend.INSUFFICIENT_DATA -> ""
        }
        return Assessment(trend, delta, note)
    }

    private fun formatAge(months: Float): String {
        val m = months.toInt()
        return if (m < 24) "$m months" else "${m / 12} years"
    }
}

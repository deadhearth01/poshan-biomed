package com.teamposhan.biomed.domain.attention

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Live measurement of how good the tracking actually is — precision and accuracy — so the
 * architecture can be validated empirically on real devices and real children instead of
 * argued from the literature.
 *
 * Two distinct quantities, routinely conflated:
 *
 *  - **Precision** is spread: how much the estimate jitters while the subject holds still.
 *    Measured as RMS sample-to-sample distance (RMS-S2S). It needs no ground truth.
 *  - **Accuracy** is bias: how far the estimate sits from where the subject was actually
 *    looking. It needs a known target, which is why it can only be sampled during a
 *    validation look at a stimulus of known position.
 *
 * Reference values, from calibrated 60 Hz infrared trackers (Frontiers Psychol 2018;9:803):
 *
 * | Group | Accuracy (mean) | RMS-S2S precision |
 * |---|---|---|
 * | Adults | 0.78 deg | 0.11-0.17 deg |
 * | 18-month-olds | 1.31 deg (range 0.18-3.85) | 0.25 deg |
 * | 30-month-olds | 1.29 deg (range 0.67-2.33) | 0.23-0.27 deg |
 *
 * Those are the numbers to beat, and we will not beat them: that is research hardware with
 * calibration. The point of measuring is to know how far off we are, and whether the
 * left/right decision the screening actually relies on survives it.
 */
class TrackingQualityMeter(
    /** Horizontal field the normalised gaze range 0..1 is taken to span, in degrees. */
    private val gazeSpanDegrees: Float = DEFAULT_GAZE_SPAN_DEGREES,
) {

    private data class Sample(val tMs: Long, val x: Float, val y: Float)

    private val window = ArrayDeque<Sample>()
    private val windowRms = ArrayDeque<Float>()

    private var accuracySum = 0f
    private var accuracyCount = 0
    private var worstAccuracy = 0f

    /** Feed every usable frame. Blink and pose-rejected frames must not be included. */
    fun onFrame(frame: FaceTrackingFrame) {
        if (!frame.usable) return
        window.addLast(Sample(frame.tMs, frame.gazeX, frame.gazeY))
        // 200 ms sliding window: the raw RMS-S2S form is very sensitive to spikes, so the
        // reported figure is the median across windows rather than a running total.
        while (window.size > 1 && frame.tMs - window.first().tMs > WINDOW_MS) {
            window.removeFirst()
        }
        if (window.size >= MIN_SAMPLES_PER_WINDOW) {
            windowRms.addLast(rmsS2S(window))
            if (windowRms.size > MAX_WINDOWS) windowRms.removeFirst()
        }
    }

    /**
     * Records an accuracy sample: where the subject was looking, in normalised screen
     * coordinates, versus where we estimated. Only call this when the subject is genuinely
     * fixating the target — during an implicit-calibration lock-on, for instance.
     */
    fun onValidationLook(estimatedX: Float, estimatedY: Float, targetX: Float, targetY: Float) {
        val dx = (estimatedX - targetX) * gazeSpanDegrees
        val dy = (estimatedY - targetY) * gazeSpanDegrees
        val offset = sqrt(dx * dx + dy * dy)
        accuracySum += offset
        accuracyCount++
        if (offset > worstAccuracy) worstAccuracy = offset
    }

    /** RMS sample-to-sample distance within one window, converted to degrees. */
    private fun rmsS2S(samples: Collection<Sample>): Float {
        if (samples.size < 2) return 0f
        val list = samples.toList()
        var sum = 0.0
        for (i in 0 until list.size - 1) {
            val dx = (list[i + 1].x - list[i].x) * gazeSpanDegrees
            val dy = (list[i + 1].y - list[i].y) * gazeSpanDegrees
            sum += (dx * dx + dy * dy).toDouble()
        }
        return sqrt(sum / (list.size - 1)).toFloat()
    }

    fun snapshot(): TrackingQuality {
        val precision = if (windowRms.isEmpty()) {
            null
        } else {
            windowRms.sorted()[windowRms.size / 2]
        }
        return TrackingQuality(
            precisionDegrees = precision,
            accuracyDegrees = if (accuracyCount == 0) null else accuracySum / accuracyCount,
            worstAccuracyDegrees = if (accuracyCount == 0) null else worstAccuracy,
            accuracySamples = accuracyCount,
        )
    }

    fun reset() {
        window.clear()
        windowRms.clear()
        accuracySum = 0f
        accuracyCount = 0
        worstAccuracy = 0f
    }

    companion object {
        private const val WINDOW_MS = 200L
        private const val MIN_SAMPLES_PER_WINDOW = 3
        private const val MAX_WINDOWS = 300

        /**
         * The normalised gaze ratio 0..1 is not a calibrated angle. This constant is the
         * working assumption that the usable eye-in-head range spans roughly +/-25 degrees,
         * so precision and accuracy below are expressed in *approximate* degrees. It is a
         * scale for comparison across sessions and devices, not an absolute calibration —
         * which is exactly the gap a harvested calibration point would close.
         */
        const val DEFAULT_GAZE_SPAN_DEGREES = 50f
    }
}

/** A precision/accuracy readout, in approximate degrees of visual angle. */
data class TrackingQuality(
    val precisionDegrees: Float?,
    val accuracyDegrees: Float?,
    val worstAccuracyDegrees: Float?,
    val accuracySamples: Int,
) {
    /**
     * Whether the left/right screen-half decision — the only spatial call the screening
     * actually depends on — survives this much error.
     *
     * Landscape with edge-placed targets gives about +/-7.8 degrees of separation at 40 cm.
     * A decision is reliable when the combined bias and jitter stay well inside that.
     */
    fun halfScreenVerdict(separationDegrees: Float = LANDSCAPE_EDGE_SEPARATION_DEGREES): String {
        val p = precisionDegrees ?: return "not enough data"
        val a = accuracyDegrees ?: 0f
        val total = a + 2f * p
        return when {
            total < separationDegrees * 0.5f -> "reliable"
            total < separationDegrees -> "marginal"
            else -> "not reliable"
        }
    }

    companion object {
        /** Landscape, targets at the screen edges, 40 cm viewing distance. */
        const val LANDSCAPE_EDGE_SEPARATION_DEGREES = 7.8f
        /** Portrait half-centroids at 40 cm — documented to show why portrait was abandoned. */
        const val PORTRAIT_HALF_SEPARATION_DEGREES = 2.25f
    }
}

/** Formats a nullable degree figure for the developer overlay. */
fun Float?.degreesOrDash(): String = this?.let { "%.2f°".format(it) } ?: "—"

/** True when two independent distance estimates disagree enough to distrust both. */
fun distanceEstimatesDiverge(primaryCm: Float?, crossCheckCm: Float?): Boolean {
    if (primaryCm == null || crossCheckCm == null) return false
    if (primaryCm <= 0f) return false
    return abs(primaryCm - crossCheckCm) / primaryCm > 0.15f
}

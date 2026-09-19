package com.teamposhan.biomed.domain.attention

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * A drawn target that moves along a known path, and the maths for scoring how well a child
 * follows it.
 *
 * ## Why this is worth more than another picture
 *
 * Every other probe in module 2 compares looking against a *region* — which half of the
 * screen, which side of a pair. This one compares looking against an **exact position at an
 * exact millisecond**, because the app drew it. That changes what can be measured:
 *
 *  - **Smooth pursuit** becomes available. Pursuit is a slow, continuous eye movement
 *    (typically 5-20°/s here), which a 15-30 fps camera samples perfectly adequately —
 *    unlike saccades, which it cannot resolve at all and which this module deliberately does
 *    not claim.
 *  - **Calibration** becomes available for free. A target sweeping through known screen
 *    positions gives a set of (true position, measured gaze) pairs, which is exactly what is
 *    needed to correct the systematic offset and scale error in uncalibrated gaze. A single
 *    fixation dot gives one point; a sweep gives dozens.
 *  - **It sidesteps the portrait problem entirely.** A face has to be physically large on
 *    screen before an infant can process it; a high-contrast moving blob does not. It can be
 *    as large and as bold as the display allows.
 *
 * ## Why it is drawn rather than filmed
 *
 * Ground truth. A video of a moving object has no machine-readable position track, so the
 * best you could do is annotate it by hand and hope the playback clock matches. Drawing it
 * means position is a function, known to the millisecond, on every device.
 */
object PursuitTarget {

    /**
     * Where the target sits at a given time, in normalised screen coordinates (0..1).
     *
     * Paths are expressed in normalised space so they are resolution independent. Amplitude
     * is deliberately kept inside the margins — a target that reaches the screen edge invites
     * the child to look past it, and edge pixels are where gaze estimation is least reliable.
     */
    sealed interface Path {
        fun positionAt(tMs: Long, durationMs: Long): Pair<Float, Float>

        /**
         * Left-right sweep at constant speed, reversing at the ends.
         *
         * The classic pursuit stimulus. Constant velocity is what keeps the measure
         * interpretable: with a varying-speed target you cannot separate poor pursuit from
         * a child who simply anticipated the turn.
         */
        data class Horizontal(
            val marginFraction: Float = 0.15f,
            val sweeps: Int = 3,
            val y: Float = 0.5f,
        ) : Path {
            override fun positionAt(tMs: Long, durationMs: Long): Pair<Float, Float> {
                val phase = (tMs.toFloat() / durationMs * sweeps).mod(1f)
                // Triangle wave: constant speed out and back, unlike a sinusoid which
                // slows at the turns and would depress apparent gain there.
                val t = if (phase < 0.5f) phase * 2f else 2f - phase * 2f
                val x = marginFraction + t * (1f - 2 * marginFraction)
                return x to y
            }
        }

        /**
         * A slow circle. Exercises both axes and is markedly more engaging than a straight
         * line, which matters for holding a toddler's attention for the full window.
         */
        data class Circular(
            val radiusFraction: Float = 0.3f,
            val turns: Int = 2,
        ) : Path {
            override fun positionAt(tMs: Long, durationMs: Long): Pair<Float, Float> {
                val theta = (tMs.toFloat() / durationMs) * turns * 2f * Math.PI.toFloat()
                return (0.5f + radiusFraction * cos(theta)) to
                    (0.5f + radiusFraction * sin(theta))
            }
        }

        /**
         * Steps between fixed positions, holding at each.
         *
         * Not a pursuit stimulus — this is the calibration path. Holding still at known
         * points is what yields clean (true, measured) pairs; a continuously moving target
         * smears them by the tracking lag.
         */
        data class Steps(
            val points: List<Pair<Float, Float>> = DEFAULT_CALIBRATION_POINTS,
        ) : Path {
            override fun positionAt(tMs: Long, durationMs: Long): Pair<Float, Float> {
                if (points.isEmpty()) return 0.5f to 0.5f
                val per = durationMs / points.size
                val i = (tMs / per.coerceAtLeast(1)).toInt().coerceIn(0, points.lastIndex)
                return points[i]
            }
        }
    }

    /**
     * Five points: centre and four inset corners.
     *
     * Inset rather than at the corners themselves, for the same reason paths keep a margin —
     * gaze estimates degrade towards the edges, so calibrating on edge points would fit the
     * correction to the least reliable data.
     */
    val DEFAULT_CALIBRATION_POINTS = listOf(
        0.5f to 0.5f,
        0.2f to 0.25f,
        0.8f to 0.25f,
        0.2f to 0.75f,
        0.8f to 0.75f,
    )

    /** One sample: where the target was, and where the child was looking. */
    data class Observation(
        val tMs: Long,
        val targetX: Float,
        val targetY: Float,
        val gazeX: Float,
        val gazeY: Float,
        val valid: Boolean,
    )

    /**
     * How well the child followed.
     *
     * @param trackingFraction share of valid samples where gaze was within [ON_TARGET_RADIUS]
     *   of the target. The headline number, and the most robust to noise.
     * @param correlation Pearson correlation between gaze and target position along the
     *   axis of travel. **The primary measure.** It asks whether the eyes moved *with* the
     *   target, which is the actual question, and it is immune to the per-child offset and
     *   scale error that uncalibrated gaze always carries.
     * @param meanErrorFraction average distance between gaze and target, in screen widths.
     * @param validFraction share of samples with a usable gaze estimate at all.
     */
    data class PursuitResult(
        val trackingFraction: Float,
        val correlation: Float?,
        val meanErrorFraction: Float,
        val validFraction: Float,
        val sampleCount: Int,
    ) {
        /**
         * Whether this is worth reporting.
         *
         * A pursuit score built on a handful of frames says nothing, and the honest output in
         * that case is "not measured" rather than a number with wide invisible error bars.
         */
        val usable: Boolean get() = sampleCount >= MIN_SAMPLES && validFraction >= 0.5f
    }

    /**
     * Within this fraction of screen width counts as on target.
     *
     * Tightened from 0.18 after simulation: at 0.18 a child who simply stared at the centre
     * scored 0.50, because a sweeping target passes near the middle for much of its path. At
     * 0.12 the same behaviour scores 0.29 against 0.97 for real pursuit. Note this measure
     * stays offset-sensitive by nature, which is why [correlation] leads.
     */
    const val ON_TARGET_RADIUS = 0.12f

    /** Below this many samples, no pursuit result is emitted. */
    const val MIN_SAMPLES = 20

    fun score(observations: List<Observation>): PursuitResult {
        val valid = observations.filter { it.valid }
        if (observations.isEmpty()) {
            return PursuitResult(0f, null, 0f, 0f, 0)
        }
        val validFraction = valid.size.toFloat() / observations.size
        if (valid.size < 2) {
            return PursuitResult(0f, null, 0f, validFraction, observations.size)
        }

        val errors = valid.map { hypot(it.gazeX - it.targetX, it.gazeY - it.targetY) }
        val onTarget = errors.count { it <= ON_TARGET_RADIUS }

        // Correlation, not a path-length ratio.
        //
        // Path-length ratio was tried first and is worthless at these frame rates: gaze path
        // accumulates landmark jitter on every frame while the target path stays smooth, so
        // the ratio saturates at its clamp for every behaviour including a child who ignores
        // the target completely. Measured on simulated sessions, path gain read 1.50 for
        // perfect pursuit, weak pursuit and staring-at-the-centre alike.
        //
        // Correlation separates them cleanly — 1.00 / 0.72 / 0.05 for the same three cases —
        // because it asks whether the eyes moved *with* the target rather than how far they
        // travelled. It is also invariant to offset and scale, which is exactly the error
        // uncalibrated gaze carries.
        val correlation = correlationOnAxisOfTravel(valid)

        return PursuitResult(
            trackingFraction = onTarget.toFloat() / valid.size,
            correlation = correlation,
            meanErrorFraction = errors.average().toFloat(),
            validFraction = validFraction,
            sampleCount = observations.size,
        )
    }

    /**
     * Correlation along whichever axis the target actually moved in.
     *
     * Picking the axis matters: for a horizontal sweep the vertical channel is pure noise,
     * and averaging it in would halve a perfect score. For a circular path both axes carry
     * signal and are averaged.
     */
    private fun correlationOnAxisOfTravel(valid: List<Observation>): Float? {
        val spanX = (valid.maxOf { it.targetX } - valid.minOf { it.targetX })
        val spanY = (valid.maxOf { it.targetY } - valid.minOf { it.targetY })
        if (spanX < 0.05f && spanY < 0.05f) return null

        val cx = if (spanX >= 0.05f) pearson(valid.map { it.gazeX }, valid.map { it.targetX }) else null
        val cy = if (spanY >= 0.05f) pearson(valid.map { it.gazeY }, valid.map { it.targetY }) else null

        return when {
            cx != null && cy != null -> (cx + cy) / 2f
            else -> cx ?: cy
        }
    }

    private fun pearson(a: List<Float>, b: List<Float>): Float? {
        val n = a.size
        if (n < 3) return null
        val ma = a.average().toFloat()
        val mb = b.average().toFloat()
        var num = 0f
        var da = 0f
        var db = 0f
        for (i in 0 until n) {
            val xa = a[i] - ma
            val xb = b[i] - mb
            num += xa * xb
            da += xa * xa
            db += xb * xb
        }
        if (da < 1e-6f || db < 1e-6f) return null
        val r = num / (kotlin.math.sqrt(da) * kotlin.math.sqrt(db))
        return if (r.isFinite()) r.coerceIn(-1f, 1f) else null
    }

    /**
     * Fits a simple affine gaze correction from calibration observations.
     *
     * Uncalibrated gaze from landmarks carries a per-child offset (where their eyes sit
     * relative to the model's average face) and a scale error (how far the estimator thinks
     * the eyes rotate). Both are close to linear over the small angles a phone screen spans,
     * so an independent per-axis fit removes most of the error without needing a model that
     * the data could not support.
     *
     * Returns null when the points are too few or too clustered to fit — a degenerate fit
     * would be far worse than no correction.
     */
    fun fitCorrection(observations: List<Observation>): GazeCorrection? {
        val valid = observations.filter { it.valid }
        if (valid.size < MIN_SAMPLES) return null

        fun fitAxis(measured: List<Float>, truth: List<Float>): Pair<Float, Float>? {
            val n = measured.size
            val mMean = measured.average().toFloat()
            val tMean = truth.average().toFloat()
            var num = 0f
            var den = 0f
            for (i in 0 until n) {
                num += (measured[i] - mMean) * (truth[i] - tMean)
                den += (measured[i] - mMean) * (measured[i] - mMean)
            }
            // Measured gaze barely varied: the child stared at one spot, or tracking failed.
            // A slope fitted through that is arbitrary.
            if (den < 1e-4f) return null
            val slope = num / den
            if (!slope.isFinite() || abs(slope) < 0.2f || abs(slope) > 5f) return null
            return slope to (tMean - slope * mMean)
        }

        val x = fitAxis(valid.map { it.gazeX }, valid.map { it.targetX }) ?: return null
        val y = fitAxis(valid.map { it.gazeY }, valid.map { it.targetY }) ?: return null
        return GazeCorrection(x.first, x.second, y.first, y.second)
    }
}

/** Per-child affine correction applied to raw gaze estimates. */
data class GazeCorrection(
    val scaleX: Float,
    val offsetX: Float,
    val scaleY: Float,
    val offsetY: Float,
) {
    fun apply(gazeX: Float, gazeY: Float): Pair<Float, Float> =
        (scaleX * gazeX + offsetX).coerceIn(0f, 1f) to
            (scaleY * gazeY + offsetY).coerceIn(0f, 1f)

    companion object {
        /** No correction — used until a calibration has been fitted. */
        val IDENTITY = GazeCorrection(1f, 0f, 1f, 0f)
    }
}

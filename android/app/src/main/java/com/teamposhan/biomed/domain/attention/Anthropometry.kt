package com.teamposhan.biomed.domain.attention

/**
 * Age-conditioned facial anthropometry.
 *
 * Distance to the child is recovered from a known physical scale on the face. The scale
 * references adults use are badly wrong for the children this app screens: the adult
 * interpupillary distance of ~63 mm applied to a 6-month-old **overestimates distance by
 * 55%**, and every quantity derived from distance — degrees-per-pixel, the fixation
 * dispersion threshold in pixels, any gaze-ray/screen intersection — inherits that error.
 *
 * The app already knows the child's age, so the correct value is a lookup away.
 *
 * Sources:
 *  - IPD: MacLachlan & Howland, *Ophthalmic Physiol Opt* 22:175-182 (2002); 1311 subjects,
 *    4294 visits, 1 month to 19 years.
 *  - Horizontal corneal diameter 0-36 months: PMC9589223.
 *    Note these are clinical white-to-white limbal measurements; MediaPipe's iris
 *    landmarks trace the *visible* iris, which measures smaller photographically, so the
 *    iris path is a cross-check rather than the primary reference.
 */
object Anthropometry {

    private data class Norm(val months: Float, val ipdMm: Float, val irisMm: Float)

    // ipdMm from MacLachlan & Howland; irisMm interpolated from the 0-36 month corneal
    // series and held at the adult 11.7 mm constant from 36 months on.
    private val norms = listOf(
        Norm(0f, 40.5f, 9.78f),
        Norm(6f, 40.5f, 10.86f),
        Norm(12f, 46.5f, 11.38f),
        Norm(24f, 47.5f, 11.72f),
        Norm(36f, 49.5f, 11.70f),
        Norm(48f, 51.0f, 11.70f),
        Norm(60f, 52.5f, 11.70f),
    )

    private fun interpolate(ageMonths: Float, pick: (Norm) -> Float): Float {
        val a = ageMonths.coerceIn(0f, 60f)
        if (a <= norms.first().months) return pick(norms.first())
        if (a >= norms.last().months) return pick(norms.last())
        val upper = norms.first { it.months >= a }
        val lower = norms.last { it.months < a }
        val t = (a - lower.months) / (upper.months - lower.months)
        return pick(lower) + (pick(upper) - pick(lower)) * t
    }

    /** Interpupillary distance in millimetres for a child of this age. */
    fun ipdMm(ageMonths: Float): Float = interpolate(ageMonths) { it.ipdMm }

    /** Visible horizontal iris diameter in millimetres for a child of this age. */
    fun irisDiameterMm(ageMonths: Float): Float = interpolate(ageMonths) { it.irisMm }

    /**
     * Distance from camera to face, in centimetres.
     *
     * `Z = f_px * realWidth_mm / observedWidth_px`, the standard pinhole relation. Returns
     * null when the inputs cannot support an estimate rather than inventing a number.
     */
    fun distanceCm(focalLengthPx: Float, realWidthMm: Float, observedWidthPx: Float): Float? {
        if (focalLengthPx <= 0f || observedWidthPx <= 1e-3f || realWidthMm <= 0f) return null
        val mm = focalLengthPx * realWidthMm / observedWidthPx
        val cm = mm / 10f
        return if (cm.isFinite() && cm in 5f..300f) cm else null
    }

    /**
     * Degrees of visual angle subtended by one screen pixel at this distance.
     * Recomputed per frame: a child leaning in halves the correct value, which is why a
     * fixed pixel threshold for fixation detection is wrong.
     */
    fun degreesPerScreenPixel(distanceCm: Float, screenMmPerPx: Float): Float {
        if (distanceCm <= 0f || screenMmPerPx <= 0f) return 0f
        val distanceMm = distanceCm * 10f
        return Math.toDegrees(2.0 * kotlin.math.atan((screenMmPerPx / 2.0) / distanceMm)).toFloat()
    }
}

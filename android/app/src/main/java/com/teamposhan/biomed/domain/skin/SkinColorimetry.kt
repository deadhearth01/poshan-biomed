package com.teamposhan.biomed.domain.skin

import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.pow

/**
 * Skin colorimetry: camera pixels to Individual Typology Angle.
 *
 * ITA° = arctan((L* - 50) / b*) x 180/pi
 *
 * Uses L* and b* and deliberately discards a*, because a* carries the haemoglobin signal
 * (blood perfusion, flushing) while L* and b* carry melanin. Dropping it is what makes ITA°
 * a melanin proxy rather than a general colour measure.
 *
 * ## What this can and cannot do
 *
 * ITA° estimates **how efficiently this child's skin can make vitamin D** — melanin is a
 * physical UV-B filter sitting above the 7-dehydrocholesterol in the epidermis. It does not
 * estimate how much vitamin D the child has: against measured serum 25(OH)D, ITA° explains
 * about 5% of the variance, roughly 0.6 ng/mL per 10 degrees. Across the full range of
 * Indian skin tones that is about 2 ng/mL, against an individual standard deviation of
 * 8.8 ng/mL. It cannot classify a child as deficient and must never be presented as doing so.
 *
 * ## Why the pipeline is fussy
 *
 * The same child photographed under a tungsten bulb and in daylight produces completely
 * different L* and b*. Without correction the app would compute two different "measurements"
 * for one child and present both with equal confidence. So:
 *
 *  - a grey reference card in frame provides the white point, per shot
 *  - no contrast enhancement of any kind: CLAHE and similar rewrite L* to look good, which
 *    destroys the quantity being measured
 *  - sRGB is linearised properly before the XYZ transform rather than treating gamma-encoded
 *    values as linear
 */
object SkinColorimetry {

    /** D65 reference white, the standard illuminant for sRGB and for CIE L*a*b*. */
    private const val XN = 95.047
    private const val YN = 100.000
    private const val ZN = 108.883

    data class Lab(val l: Double, val a: Double, val b: Double)

    /**
     * A measured patch of skin.
     *
     * [reliable] is false when the capture conditions make the number untrustworthy — too
     * dark, blown highlights, or a b* at or below zero which puts the arctan in a quadrant
     * the ITA° formula does not define. An unreliable reading is reported as unavailable
     * rather than shown with a caveat, because a number on screen gets believed.
     */
    data class SkinReading(
        val itaDegrees: Double,
        val lab: Lab,
        val reliable: Boolean,
        val note: String? = null,
    )

    // ---------------------------------------------------------------------
    // Colour conversion
    // ---------------------------------------------------------------------

    /**
     * Undoes the sRGB transfer function.
     *
     * Camera JPEGs are gamma-encoded. Feeding those values straight into the XYZ matrix —
     * which expects linear light — is a common and invisible error: it does not crash, it
     * just returns the wrong L*.
     */
    private fun linearise(channel: Double): Double {
        val c = channel.coerceIn(0.0, 1.0)
        return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun labF(t: Double): Double {
        val epsilon = 216.0 / 24389.0
        val kappa = 24389.0 / 27.0
        return if (t > epsilon) cbrt(t) else (kappa * t + 16.0) / 116.0
    }

    /** sRGB (0..255, gamma-encoded) to CIE L*a*b* under D65. */
    fun rgbToLab(r: Int, g: Int, b: Int): Lab {
        val rl = linearise(r / 255.0)
        val gl = linearise(g / 255.0)
        val bl = linearise(b / 255.0)

        // sRGB D65 primaries.
        val x = (0.4124564 * rl + 0.3575761 * gl + 0.1804375 * bl) * 100.0
        val y = (0.2126729 * rl + 0.7151522 * gl + 0.0721750 * bl) * 100.0
        val z = (0.0193339 * rl + 0.1191920 * gl + 0.9503041 * bl) * 100.0

        val fx = labF(x / XN)
        val fy = labF(y / YN)
        val fz = labF(z / ZN)

        return Lab(
            l = 116.0 * fy - 16.0,
            a = 500.0 * (fx - fy),
            b = 200.0 * (fy - fz),
        )
    }

    // ---------------------------------------------------------------------
    // White balance from a reference card
    // ---------------------------------------------------------------------

    /**
     * Von Kries-style correction against a neutral grey card photographed in the same frame.
     *
     * The card is known to be neutral, so whatever cast it shows is the illuminant's. Scaling
     * each channel so the card reads neutral removes that cast from the skin patch too.
     *
     * This is the step that makes the reading comparable between a tungsten-lit room and open
     * shade. Without it the app is measuring the lighting as much as the child.
     */
    /**
     * Per-channel gains that make the reference card read neutral.
     *
     * Returned separately from [whiteBalance] because the correction has to be applied to
     * every pixel *before* the skin/non-skin test runs, not to the patch average afterwards:
     * the segmentation thresholds assume neutral light.
     *
     * Returns unit gains when the card is unusable, so a blown or black card degrades to
     * "no correction" instead of scaling the reading by a wild factor.
     */
    fun illuminantGains(cardR: Int, cardG: Int, cardB: Int): Triple<Double, Double, Double> {
        if (cardR < 20 || cardG < 20 || cardB < 20) return Triple(1.0, 1.0, 1.0)
        if (cardR > 245 || cardG > 245 || cardB > 245) return Triple(1.0, 1.0, 1.0)
        val grey = (cardR + cardG + cardB) / 3.0
        return Triple(grey / cardR, grey / cardG, grey / cardB)
    }

    fun whiteBalance(
        skinR: Int, skinG: Int, skinB: Int,
        cardR: Int, cardG: Int, cardB: Int,
    ): Triple<Int, Int, Int> {
        val (gr, gg, gb) = illuminantGains(cardR, cardG, cardB)
        return Triple(
            (skinR * gr).toInt().coerceIn(0, 255),
            (skinG * gg).toInt().coerceIn(0, 255),
            (skinB * gb).toInt().coerceIn(0, 255),
        )
    }

    // ---------------------------------------------------------------------
    // ITA
    // ---------------------------------------------------------------------

    /**
     * Computes ITA° from a Lab value, refusing rather than guessing where the formula does
     * not apply.
     *
     * The classic formula assumes b* > 0, which holds for skin under reasonable light. A b*
     * at or below zero means the white balance failed or the patch is not skin, and arctan
     * would return a confident wrong answer.
     */
    fun itaFrom(lab: Lab): SkinReading {
        val reasons = mutableListOf<String>()

        if (lab.b <= 0.5) {
            return SkinReading(
                itaDegrees = Double.NaN,
                lab = lab,
                reliable = false,
                note = "Colour balance failed — retake with the grey card clearly in frame.",
            )
        }
        // Underexposure lands on L* first, and L* is the numerator.
        if (lab.l < 15.0) reasons += "too dark"
        if (lab.l > 92.0) reasons += "overexposed"

        val ita = Math.toDegrees(atan((lab.l - 50.0) / lab.b))
        return SkinReading(
            itaDegrees = ita,
            lab = lab,
            reliable = reasons.isEmpty(),
            note = if (reasons.isEmpty()) null else "Retake in better light (${reasons.joinToString()}).",
        )
    }

    /** Full path: skin patch plus reference card to a reading. */
    fun measure(
        skinR: Int, skinG: Int, skinB: Int,
        cardR: Int, cardG: Int, cardB: Int,
    ): SkinReading {
        val (r, g, b) = whiteBalance(skinR, skinG, skinB, cardR, cardG, cardB)
        return itaFrom(rgbToLab(r, g, b))
    }

    /**
     * Robust patch average.
     *
     * The mean is taken in **linear light**, not on gamma-encoded values — averaging gamma
     * values biases dark. Pixels far from the patch median are dropped first, so a stray
     * hair, a shadow edge or a speck of dirt does not drag the reading.
     */
    fun averagePatch(pixels: IntArray): Triple<Int, Int, Int> {
        if (pixels.isEmpty()) return Triple(0, 0, 0)

        val lum = pixels.map { p ->
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            0.2126 * r + 0.7152 * g + 0.0722 * b
        }
        val median = lum.sorted()[lum.size / 2]
        val keep = pixels.indices.filter { abs(lum[it] - median) < 40.0 }
        val use = if (keep.size >= pixels.size / 4) keep else pixels.indices.toList()

        var rl = 0.0
        var gl = 0.0
        var bl = 0.0
        use.forEach { i ->
            val p = pixels[i]
            rl += linearise(((p shr 16) and 0xFF) / 255.0)
            gl += linearise(((p shr 8) and 0xFF) / 255.0)
            bl += linearise((p and 0xFF) / 255.0)
        }
        val n = use.size.toDouble()

        fun encode(v: Double): Int {
            val c = (v / n).coerceIn(0.0, 1.0)
            val s = if (c <= 0.0031308) c * 12.92 else 1.055 * c.pow(1.0 / 2.4) - 0.055
            return (s * 255.0).toInt().coerceIn(0, 255)
        }
        return Triple(encode(rl), encode(gl), encode(bl))
    }
}

/**
 * ITA° to the guidance the app actually gives.
 *
 * Note what is deliberately absent: any band named for risk of deficiency. The output is a
 * **sun exposure multiplier** — more melanin means more time outdoors for the same synthesis —
 * which is a mechanistic consequence of the measurement rather than a claim about the child's
 * vitamin D status.
 *
 * The standard ITA° categories (very light >55, light 41-55, intermediate 28-41, tan 10-28,
 * brown -30-10, dark <-30) are kept as-is rather than collapsed into three, because Indian
 * children cluster in the intermediate-to-brown range and a three-band scheme would place
 * nearly everyone in one band, discriminating nothing.
 */
object SunExposureFactor {

    data class Guidance(
        val category: String,
        /** Multiplier on baseline exposure time for equivalent synthesis. */
        val timeMultiplier: Float,
        val message: String,
    )

    /** Baseline minutes near solar noon, for light skin, roughly a third of the body exposed. */
    const val BASELINE_MINUTES = 10

    fun forIta(ita: Double): Guidance = when {
        ita > 55 -> Guidance(
            "Very light", 0.7f,
            "Skin makes vitamin D quickly. Short spells outside are enough, and burning is " +
                "the greater risk.",
        )
        ita > 41 -> Guidance(
            "Light", 1.0f,
            "A short spell outside near midday is enough.",
        )
        ita > 28 -> Guidance(
            "Intermediate", 1.5f,
            "A little longer outside than lighter skin needs for the same benefit.",
        )
        ita > 10 -> Guidance(
            "Tan", 2.0f,
            "Around twice as long outside as lighter skin, for the same vitamin D.",
        )
        ita > -30 -> Guidance(
            "Brown", 3.0f,
            "Melanin filters sunlight well, so noticeably longer outside is needed. This is " +
                "protection against burning, not a problem with the skin.",
        )
        else -> Guidance(
            "Dark", 4.0f,
            "Melanin filters sunlight strongly. Considerably longer outside is needed for " +
                "the same vitamin D.",
        )
    }

    /** Suggested minutes near solar noon for this child. */
    fun suggestedMinutes(ita: Double): Int =
        (BASELINE_MINUTES * forIta(ita).timeMultiplier).toInt().coerceIn(5, 45)

    /**
     * The sentence shown to the caregiver. Never mentions deficiency, risk tiers or levels —
     * it is advice about time outdoors, which is the only thing this measurement supports.
     */
    fun caregiverMessage(ita: Double): String {
        val g = forIta(ita)
        return "${g.message} About ${suggestedMinutes(ita)} minutes around midday, with arms " +
            "and legs uncovered."
    }
}

package com.teamposhan.biomed.domain.skin

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Established image-processing filters applied before colorimetry.
 *
 * Every filter here is a published, widely-replicated method rather than something tuned by
 * eye. That matters because each one changes the number the app reports, and an
 * unattributable filter is an unauditable measurement.
 *
 * Order is deliberate and each stage removes pixels the next stage would be misled by:
 *
 * ```
 * raw patch
 *   -> specular rejection      remove mirror reflections (Klinker et al. dichromatic model)
 *   -> grey-card white balance  correct the illuminant first (von Kries)
 *   -> skin segmentation        keep only skin (Chai & Ngan YCbCr thresholds)
 *   -> median filter            remove impulse noise and stray hairs
 * ```
 *
 * White balance comes **before** segmentation, not after. The Chai & Ngan thresholds were
 * derived under neutral light; under a strong tungsten cast genuine skin drifts outside them
 * and the whole patch is thrown away as "not skin". Correcting the cast first means the
 * segmentation sees what it was designed to see.
 */
object SkinFilters {

    // ---------------------------------------------------------------------
    // 1. Specular highlight rejection
    // ---------------------------------------------------------------------

    /**
     * Rejects specular (mirror) reflections.
     *
     * Under the dichromatic reflection model (Shafer 1985; Klinker, Shafer & Kanade 1988),
     * reflected light is the sum of a **body** component carrying the pigment's colour and a
     * **surface** component that is the illuminant's own colour. Only the body component
     * carries information about melanin.
     *
     * Specular pixels are bright and nearly unsaturated — they are the light source, not the
     * skin. A single highlight on an oiled infant cheek can lift a patch mean by several L*
     * units, which is larger than the entire ITA° effect being measured.
     *
     * Detected as high value with low saturation in HSV, the standard formulation.
     */
    fun isSpecular(r: Int, g: Int, b: Int): Boolean {
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val value = maxC / 255.0
        val saturation = if (maxC == 0) 0.0 else (maxC - minC).toDouble() / maxC
        // Bright and washed out: the illuminant reflecting off the surface.
        return value > 0.90 && saturation < 0.20
    }

    // ---------------------------------------------------------------------
    // 2. Skin segmentation
    // ---------------------------------------------------------------------

    /**
     * Chai & Ngan (1999) YCbCr skin-colour thresholds: 77 <= Cb <= 127, 133 <= Cr <= 173.
     *
     * One of the most replicated results in skin detection, and the reason it is used here
     * rather than an RGB rule: chrominance (Cb, Cr) separates skin from non-skin far more
     * consistently across skin tones than RGB does, because melanin changes luminance much
     * more than it changes chrominance. A threshold that works for light skin in RGB
     * typically fails for dark skin; this one does not.
     *
     * Its purpose here is rejection, not detection — discarding hair, clothing, background
     * and shadow from a patch the user has already pointed at.
     */
    fun isSkin(r: Int, g: Int, b: Int): Boolean {
        // ITU-R BT.601, matching the coefficients the original thresholds were derived with.
        val cb = 128.0 - 0.168736 * r - 0.331264 * g + 0.5 * b
        val cr = 128.0 + 0.5 * r - 0.418688 * g - 0.081312 * b
        return cb in 77.0..127.0 && cr in 133.0..173.0
    }

    // ---------------------------------------------------------------------
    // 3. Median filter
    // ---------------------------------------------------------------------

    /**
     * Per-channel median over the surviving pixels.
     *
     * The median is the standard estimator against impulse noise: unlike a mean it is
     * unmoved by a minority of extreme values, so a stray hair, a fleck of dirt or a hot
     * sensor pixel cannot shift the result. Applied per channel because a vector median is
     * far more expensive for no benefit at this patch size.
     */
    fun channelMedian(values: IntArray): Int {
        if (values.isEmpty()) return 0
        val sorted = values.sortedArray()
        return sorted[sorted.size / 2]
    }

    // ---------------------------------------------------------------------
    // Pipeline
    // ---------------------------------------------------------------------

    /** What the filter chain kept, and what it threw away. */
    data class FilteredPatch(
        val r: Int,
        val g: Int,
        val b: Int,
        val totalPixels: Int,
        val keptPixels: Int,
        val rejectedSpecular: Int,
        val rejectedNonSkin: Int,
        /** Whether the on-device segmentation model decided what counted as skin. */
        val usedModel: Boolean = false,
    ) {
        val keptFraction: Float
            get() = if (totalPixels == 0) 0f else keptPixels.toFloat() / totalPixels

        /**
         * A patch is usable when most of it survived.
         *
         * Below half, the user has most likely framed hair, clothing or background rather
         * than skin, and the median of what remains is not representative.
         */
        val usable: Boolean get() = keptFraction >= 0.5f && keptPixels >= 100
    }

    /**
     * Runs the rejection and averaging stages over a patch.
     *
     * [gains] are the per-channel illuminant corrections from the grey card, applied to each
     * pixel before it is judged. Passing 1.0 gains skips the correction, which is what
     * happens when no card is in frame.
     *
     * Returns the per-channel median of pixels that are skin and not specular, along with
     * the rejection counts so the UI can explain a refusal rather than simply failing.
     */
    fun filterPatch(
        pixels: IntArray,
        gains: Triple<Double, Double, Double>,
        modelSkin: BooleanArray? = null,
    ): FilteredPatch {
        if (pixels.isEmpty()) return FilteredPatch(0, 0, 0, 0, 0, 0, 0, false)

        var specular = 0
        var nonSkin = 0
        val keptR = ArrayList<Int>(pixels.size)
        val keptG = ArrayList<Int>(pixels.size)
        val keptB = ArrayList<Int>(pixels.size)
        val useModel = modelSkin != null && modelSkin.size == pixels.size

        pixels.forEachIndexed { i, p ->
            val r = (((p shr 16) and 0xFF) * gains.first).toInt().coerceIn(0, 255)
            val g = (((p shr 8) and 0xFF) * gains.second).toInt().coerceIn(0, 255)
            val b = ((p and 0xFF) * gains.third).toInt().coerceIn(0, 255)
            // The model decides what is skin when it is available; the colour rule is the
            // fallback, not a second vote. Requiring both to agree would inherit exactly the
            // warm-light failure the model was brought in to fix.
            val isSkinPixel = if (useModel) modelSkin!![i] else isSkin(r, g, b)
            when {
                isSpecular(r, g, b) -> specular++
                !isSkinPixel -> nonSkin++
                else -> {
                    keptR += r
                    keptG += g
                    keptB += b
                }
            }
        }

        return FilteredPatch(
            r = channelMedian(keptR.toIntArray()),
            g = channelMedian(keptG.toIntArray()),
            b = channelMedian(keptB.toIntArray()),
            totalPixels = pixels.size,
            keptPixels = keptR.size,
            rejectedSpecular = specular,
            rejectedNonSkin = nonSkin,
            usedModel = useModel,
        )
    }

    /**
     * Filters a grey reference card patch.
     *
     * Skin segmentation is deliberately not applied — the card is not skin. Specular
     * rejection is, because a glossy card catches highlights like anything else, and a
     * blown card would scale the whole correction wrongly.
     */
    fun filterCard(pixels: IntArray): FilteredPatch {
        if (pixels.isEmpty()) return FilteredPatch(0, 0, 0, 0, 0, 0, 0, false)

        var specular = 0
        val keptR = ArrayList<Int>(pixels.size)
        val keptG = ArrayList<Int>(pixels.size)
        val keptB = ArrayList<Int>(pixels.size)

        pixels.forEach { p ->
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            if (isSpecular(r, g, b)) {
                specular++
            } else {
                keptR += r
                keptG += g
                keptB += b
            }
        }
        return FilteredPatch(
            r = channelMedian(keptR.toIntArray()),
            g = channelMedian(keptG.toIntArray()),
            b = channelMedian(keptB.toIntArray()),
            totalPixels = pixels.size,
            keptPixels = keptR.size,
            rejectedSpecular = specular,
            rejectedNonSkin = 0,
        )
    }
}

/**
 * The complete measurement: filters, white balance, colour conversion, ITA°.
 *
 * Every stage is reported, not just the answer, so the calculation can be shown to the user
 * and checked by a reviewer.
 */
data class SkinMeasurement(
    /**
     * The sampled pixels themselves, kept so the exact input to the calculation can be
     * rebuilt as an image and shown. A few thousand ints per frame, discarded with the
     * frame; only the locked one is ever rendered.
     */
    val source: SkinPatchSampler.Patch,
    /** Per-channel illuminant gains that were applied, or 1.0 when no card was usable. */
    val gains: Triple<Double, Double, Double>,
    /** The patch as the sensor saw it, before any illuminant correction. */
    val rawPatch: SkinFilters.FilteredPatch,
    /** The same patch after the grey-card gains, and the one the result is computed from. */
    val skinPatch: SkinFilters.FilteredPatch,
    val cardPatch: SkinFilters.FilteredPatch?,
    val lab: SkinColorimetry.Lab,
    val reading: SkinColorimetry.SkinReading,
    val usedCard: Boolean,
) {
    val balancedR: Int get() = skinPatch.r
    val balancedG: Int get() = skinPatch.g
    val balancedB: Int get() = skinPatch.b

    val trustworthy: Boolean
        get() = skinPatch.usable && reading.reliable && usedCard

    /**
     * Why the reading should not be relied on, or null when it is sound.
     *
     * A measurement without a reference card is reported as untrustworthy even when every
     * other stage passed: without a known white point the app is measuring the room lighting
     * as much as the child.
     */
    val caveat: String?
        get() = when {
            !usedCard ->
                "Put the grey card in the right-hand box. Without it the light in the room " +
                    "changes the answer."
            !skinPatch.usable ->
                "Most of that box was not skin. Fill it with bare skin from the inner arm, " +
                    "and move out of direct sun."
            !reading.reliable -> reading.note
            else -> null
        }

    companion object {
        /**
         * Runs the whole pipeline.
         *
         * Order matters and is the reverse of the obvious one: the card is read first, its
         * gains are applied to every skin pixel, and only then is each pixel judged as skin
         * or not. Segmenting first would discard genuine skin under warm light, because the
         * published thresholds assume a neutral illuminant.
         */
        fun from(
            skin: SkinPatchSampler.Patch,
            cardPixels: IntArray?,
        ): SkinMeasurement {
            val card = cardPixels
                ?.let { SkinFilters.filterCard(it) }
                ?.takeIf { it.keptPixels > 50 }

            val unit = Triple(1.0, 1.0, 1.0)
            val gains = card
                ?.let { SkinColorimetry.illuminantGains(it.r, it.g, it.b) }
                ?: unit

            val raw = SkinFilters.filterPatch(skin.pixels, unit, skin.modelSkin)
            val balanced = if (gains == unit) {
                raw
            } else {
                SkinFilters.filterPatch(skin.pixels, gains, skin.modelSkin)
            }

            val lab = SkinColorimetry.rgbToLab(balanced.r, balanced.g, balanced.b)
            return SkinMeasurement(
                source = skin,
                gains = gains,
                rawPatch = raw,
                skinPatch = balanced,
                cardPatch = card,
                lab = lab,
                reading = SkinColorimetry.itaFrom(lab),
                // A card that was found but judged unusable (blown, in shadow) yields unit
                // gains, and a reading built on unit gains has not actually been corrected.
                usedCard = card != null && gains != unit,
            )
        }
    }
}

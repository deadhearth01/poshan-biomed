package com.teamposhan.biomed.domain.skin

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.pow

/**
 * Rebuilds the sampled patch as a set of images — one per stage of the calculation.
 *
 * The point is auditability. The numbers on the working panel are correct but abstract: a
 * caregiver, a health worker or a reviewer cannot tell from "kept 2103 of 4000" whether the
 * app was looking at an arm or at a sleeve. These images show the actual pixels the ITA was
 * computed from, at each point where the pipeline changed them.
 *
 * Built **only on demand**, from the locked measurement. Generating five bitmaps per frame at
 * 30 fps would stall the analysis thread for no benefit; nobody is watching them until the
 * reading has settled and the drawer is opened.
 */
object StagePreviews {

    /**
     * One stage, with an explanation of what it shows.
     *
     * [caption] is written for someone who has never heard of a colour space. The point of
     * showing an image is lost if it needs a glossary.
     */
    data class Stage(
        val title: String,
        val caption: String,
        val image: Bitmap,
    )

    /**
     * Every stage, in the order the pipeline applies them.
     *
     * Returns empty when the patch has no usable geometry — a subsampled patch with zero rows
     * cannot be reshaped into anything.
     */
    fun build(m: SkinMeasurement): List<Stage> {
        val src = m.source
        if (src.cols <= 0 || src.rows <= 0 || src.pixels.isEmpty()) return emptyList()

        val stages = mutableListOf<Stage>()

        // 1 -------------------------------------------------------------- as seen
        stages += Stage(
            title = "1. What the camera saw",
            caption = "The patch inside the left box, exactly as the sensor recorded it — " +
                "including whatever colour the light in the room happens to be.",
            image = bitmap(src) { it },
        )

        // 2 -------------------------------------------------------- colour corrected
        val (gr, gg, gb) = m.gains
        if (m.usedCard) {
            stages += Stage(
                title = "2. After the grey card correction",
                caption = "The same pixels with the room's colour removed, using the grey " +
                    "card as the reference. This is what makes a reading indoors mean the " +
                    "same as one outdoors.",
                image = bitmap(src) { p ->
                    rgb(
                        (red(p) * gr).toInt().coerceIn(0, 255),
                        (green(p) * gg).toInt().coerceIn(0, 255),
                        (blue(p) * gb).toInt().coerceIn(0, 255),
                    )
                },
            )
        } else {
            stages += Stage(
                title = "2. No colour correction",
                caption = "No usable grey card was found, so nothing could be corrected. " +
                    "This is why the reading was not accepted.",
                image = bitmap(src) { it },
            )
        }

        // 3 ------------------------------------------------------------- what was used
        val useModel = src.modelSkin != null && src.modelSkin.size == src.pixels.size
        stages += Stage(
            title = "3. Which pixels were used",
            caption = "Green pixels counted towards the answer. Red were dropped as shine — " +
                "light bouncing off the skin rather than the skin itself. Grey were dropped " +
                "as not skin: hair, cloth, background.",
            image = bitmapIndexed(src) { i, p ->
                val r = (red(p) * gr).toInt().coerceIn(0, 255)
                val g = (green(p) * gg).toInt().coerceIn(0, 255)
                val b = (blue(p) * gb).toInt().coerceIn(0, 255)
                val isSkin = if (useModel) src.modelSkin!![i] else SkinFilters.isSkin(r, g, b)
                when {
                    SkinFilters.isSpecular(r, g, b) -> rgb(220, 60, 60)
                    !isSkin -> rgb(120, 120, 120)
                    // Kept pixels stay their real colour, tinted green, so the patch is
                    // still recognisable as skin rather than becoming a flat mask.
                    else -> rgb((r * 0.55).toInt(), (g * 0.55 + 110).toInt().coerceAtMost(255), (b * 0.55).toInt())
                }
            },
        )

        // 4 ------------------------------------------------------------- lightness
        stages += Stage(
            title = "4. Lightness only",
            caption = "How light or dark each pixel is, with colour removed. This is the " +
                "main ingredient of the skin tone angle.",
            image = bitmap(src) { p ->
                val lab = SkinColorimetry.rgbToLab(
                    (red(p) * gr).toInt().coerceIn(0, 255),
                    (green(p) * gg).toInt().coerceIn(0, 255),
                    (blue(p) * gb).toInt().coerceIn(0, 255),
                )
                val v = (lab.l / 100.0 * 255).toInt().coerceIn(0, 255)
                rgb(v, v, v)
            },
        )

        // 5 ------------------------------------------------------------- yellowness
        stages += Stage(
            title = "5. Yellowness only",
            caption = "How yellow each pixel is. The app divides the lightness by this to " +
                "get the angle. The red part of the colour is worked out too, then thrown " +
                "away — it mostly shows blood under the skin.",
            image = bitmap(src) { p ->
                val lab = SkinColorimetry.rgbToLab(
                    (red(p) * gr).toInt().coerceIn(0, 255),
                    (green(p) * gg).toInt().coerceIn(0, 255),
                    (blue(p) * gb).toInt().coerceIn(0, 255),
                )
                // b* runs roughly -50..+50 for real skin; mapped blue-to-yellow so the
                // direction of the axis is visible rather than merely its magnitude.
                val t = ((lab.b + 50.0) / 100.0).coerceIn(0.0, 1.0)
                rgb(
                    (40 + 215 * t).toInt(),
                    (40 + 195 * t).toInt(),
                    (200 - 170 * t).toInt(),
                )
            },
        )

        return stages
    }

    // ------------------------------------------------------------------ helpers

    private fun red(p: Int) = (p shr 16) and 0xFF
    private fun green(p: Int) = (p shr 8) and 0xFF
    private fun blue(p: Int) = p and 0xFF
    private fun rgb(r: Int, g: Int, b: Int) =
        (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    private inline fun bitmap(src: SkinPatchSampler.Patch, transform: (Int) -> Int): Bitmap =
        bitmapIndexed(src) { _, p -> transform(p) }

    private inline fun bitmapIndexed(
        src: SkinPatchSampler.Patch,
        transform: (Int, Int) -> Int,
    ): Bitmap {
        val out = IntArray(src.cols * src.rows)
        for (i in out.indices) out[i] = transform(i, src.pixels[i])
        return Bitmap.createBitmap(out, src.cols, src.rows, Bitmap.Config.ARGB_8888)
    }
}

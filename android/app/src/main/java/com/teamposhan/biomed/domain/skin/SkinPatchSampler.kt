package com.teamposhan.biomed.domain.skin

import android.graphics.Bitmap

/**
 * Pulls pixel patches, and their model skin-mask entries, out of a camera frame.
 *
 * Works on a bitmap already rotated to the orientation the operator sees, rather than on the
 * raw sensor buffer. That is deliberate: the reticles are drawn in display space, and reading
 * display coordinates out of an unrotated buffer is a 90-degree error in portrait — the app
 * would measure the background while showing a box on the arm. Rotating once, up front, means
 * one coordinate space for the preview, the drawn reticle, the sampled pixels and the
 * segmentation mask, and no mapping to get wrong.
 */
object SkinPatchSampler {

    /** A rectangle in the displayed frame, normalised 0..1. */
    data class Reticle(val left: Float, val top: Float, val right: Float, val bottom: Float)

    /**
     * The pixels inside a reticle, with the model's verdict on each one.
     *
     * [modelSkin] is null when the segmentation model was unavailable, which the filter
     * stage reads as "fall back to the colour rule" rather than as "none of this is skin".
     */
    data class Patch(
        val pixels: IntArray,
        val modelSkin: BooleanArray?,
        /** Grid width of the subsampled patch, so it can be rebuilt as an image. */
        val cols: Int = 0,
        /** Grid height of the subsampled patch. */
        val rows: Int = 0,
    ) {
        // Array fields: identity comparison is the honest default here, and these are never
        // used as map keys or compared for equality.
        override fun equals(other: Any?): Boolean = this === other
        override fun hashCode(): Int = System.identityHashCode(this)
    }

    /**
     * Reads one patch as packed 0xRRGGBB values.
     *
     * Subsamples on a fixed stride so a large reticle costs no more than a small one: a few
     * thousand pixels is far more than a median needs, and this runs on every frame.
     */
    fun patch(
        bitmap: Bitmap,
        reticle: Reticle,
        mask: BooleanArray?,
        maxSamples: Int = 4000,
    ): Patch {
        val w = bitmap.width
        val h = bitmap.height
        val x0 = (reticle.left * w).toInt().coerceIn(0, w - 1)
        val y0 = (reticle.top * h).toInt().coerceIn(0, h - 1)
        val x1 = (reticle.right * w).toInt().coerceIn(x0 + 1, w)
        val y1 = (reticle.bottom * h).toInt().coerceIn(y0 + 1, h)

        val rectW = x1 - x0
        val rectH = y1 - y0
        val total = rectW.toLong() * rectH
        val step = if (total <= maxSamples) {
            1
        } else {
            Math.sqrt(total.toDouble() / maxSamples).toInt().coerceAtLeast(1)
        }

        val colours = ArrayList<Int>(maxSamples + 16)
        val skin = if (mask != null) ArrayList<Boolean>(maxSamples + 16) else null

        // Row and column counts are tracked so the flattened array can be rebuilt into an
        // image later. Without them the samples are just a bag of colours and the operator
        // can never be shown the actual pixels a reading was computed from.
        var rows = 0
        var cols = 0
        var y = y0
        while (y < y1) {
            var x = x0
            var rowCols = 0
            while (x < x1) {
                val argb = bitmap.getPixel(x, y)
                colours += argb and 0x00FFFFFF
                skin?.add(mask!!.getOrElse(y * w + x) { false })
                rowCols++
                x += step
            }
            if (rowCols > 0) {
                cols = rowCols
                rows++
            }
            y += step
        }

        // A ragged final row would misalign every row after it when reshaped to a grid, so
        // the tail is dropped rather than rendered skewed.
        val usable = rows * cols
        val trimmed = if (colours.size > usable) colours.subList(0, usable) else colours

        return Patch(
            pixels = trimmed.toIntArray(),
            modelSkin = skin?.let { m ->
                (if (m.size > usable) m.subList(0, usable) else m).toBooleanArray()
            },
            cols = cols,
            rows = rows,
        )
    }
}

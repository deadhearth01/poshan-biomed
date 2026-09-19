package com.teamposhan.biomed.domain.skin

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter

/**
 * On-device skin segmentation for the vitamin D capture.
 *
 * ## What this replaces, and why
 *
 * The colorimetry pipeline needs to know which pixels in the reticle are actually skin. It
 * did that with the Chai & Ngan YCbCr thresholds — a published, well-replicated rule, but a
 * fixed one. Fixed chrominance bounds have a specific failure: they were derived under
 * neutral light, so under a warm bulb or deep shade genuine skin drifts outside them and the
 * patch is thrown away as "not skin". The person measuring sees a refusal and has no idea
 * why, because the arm plainly is an arm.
 *
 * This model decides from shape and context rather than colour alone, so a warm cast does not
 * make an arm stop being an arm. It also correctly rejects things a colour rule cannot:
 * wooden furniture, a brown floor, a tan cloth — all of which sit inside the skin chrominance
 * box and were previously averaged into the reading.
 *
 * ## What it deliberately does not do
 *
 * It finds skin. It says nothing about vitamin D, and no model on this device can: the
 * quantity is four orders of magnitude below what a camera can detect, and the wavelengths
 * that matter are outside what the sensor sees at all. A model here improves the *measurement
 * of skin tone* and nothing downstream of it. Anything claiming to read a vitamin D level
 * from a photograph would be producing a confident number with nothing underneath.
 *
 * ## Failure behaviour
 *
 * If the model cannot be created — missing asset, unsupported device, out of memory — this
 * returns null masks and the pipeline falls back to the YCbCr rule. A capture never fails
 * because the model is unavailable; it just becomes a little more brittle in odd lighting,
 * and the working panel says which method was used.
 */
class SkinSegmenter(context: Context) : AutoCloseable {

    /**
     * Selfie multiclass output categories.
     *
     * 0 background · 1 hair · 2 body-skin · 3 face-skin · 4 clothes · 5 other.
     *
     * Both skin classes are accepted. The reading is taken on the inner forearm, which the
     * model labels body-skin, but accepting face-skin too costs nothing and keeps the
     * pipeline honest if a reading is ever taken elsewhere.
     */
    private val skinClasses = byteArrayOf(2, 3)

    private val segmenter: ImageSegmenter? = runCatching {
        ImageSegmenter.createFromOptions(
            context,
            ImageSegmenter.ImageSegmenterOptions.builder()
                .setBaseOptions(
                    BaseOptions.builder()
                        .setModelAssetPath(MODEL_ASSET)
                        .build()
                )
                // IMAGE, not LIVE_STREAM: this is a still measurement the operator aims,
                // and a synchronous call keeps the mask and the pixels it describes from
                // ever being one frame out of step with each other.
                .setRunningMode(RunningMode.IMAGE)
                .setOutputCategoryMask(true)
                .setOutputConfidenceMasks(false)
                .build()
        )
    }.getOrNull()

    /** True when the model loaded and masks will actually be produced. */
    val available: Boolean get() = segmenter != null

    /**
     * A per-pixel skin mask for [bitmap], row-major, one entry per pixel.
     *
     * Returns null when the model is unavailable or inference failed, which the caller must
     * treat as "fall back to the colour rule" rather than as "no skin".
     */
    fun skinMask(bitmap: Bitmap): BooleanArray? {
        val seg = segmenter ?: return null
        return runCatching {
            val result = seg.segment(BitmapImageBuilder(bitmap).build())
            val maskImage = result.categoryMask().orElse(null) ?: return null
            val buffer = ByteBufferExtractor.extract(maskImage)

            val width = maskImage.width
            val height = maskImage.height
            val out = BooleanArray(width * height)
            var i = 0
            while (i < out.size && i < buffer.limit()) {
                val category = buffer.get(i)
                out[i] = category == skinClasses[0] || category == skinClasses[1]
                i++
            }
            // A mask that does not cover the bitmap cannot be indexed against it, and
            // silently sampling the wrong pixels is worse than not using the model.
            if (width != bitmap.width || height != bitmap.height) null else out
        }.getOrNull()
    }

    override fun close() {
        runCatching { segmenter?.close() }
    }

    companion object {
        const val MODEL_ASSET = "selfie_multiclass_256x256.tflite"
    }
}

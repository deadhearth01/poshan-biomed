package com.teamposhan.biomed.domain.attention

/**
 * One fully analysed camera frame.
 *
 * Carries the raw landmark geometry as well as the derived signals, so the developer mesh
 * overlay and the screening pipeline read from exactly the same data — an overlay drawn
 * from a second, parallel computation would be able to look right while the measurement
 * was wrong.
 *
 * All coordinates are normalised 0..1 in the **display** frame: mirroring for the front
 * camera has already been applied, once, at the source. Nothing downstream should flip
 * anything again.
 */
data class FaceTrackingFrame(
    /** Monotonic capture timestamp in milliseconds (start of exposure, not callback time). */
    val tMs: Long,
    val facePresent: Boolean,

    /** Normalised landmark coordinates, 478 points when a face is present. */
    val landmarkX: FloatArray = FloatArray(0),
    val landmarkY: FloatArray = FloatArray(0),

    val headPose: HeadPose = HeadPose.UNKNOWN,

    /** Normalised gaze in the display frame; 0.5 is centre. */
    val gazeX: Float = 0.5f,
    val gazeY: Float = 0.5f,

    val eyesOpen: Boolean = false,
    val blinkScore: Float = 0f,

    /** Distance from camera to face, centimetres, from age-conditioned anthropometry. */
    val distanceCm: Float? = null,
    /** Independent iris-based estimate; a large divergence from [distanceCm] is a quality flag. */
    val irisDistanceCm: Float? = null,

    /** Observed interpupillary separation in pixels — the scale reference. */
    val ipdPx: Float = 0f,

    /** False when the head-pose gate rejected this frame. */
    val poseInEnvelope: Boolean = false,

    /** Milliseconds from capture to the result being delivered. */
    val inferenceLatencyMs: Long = 0L,

    /**
     * Dimensions of the upright, display-oriented frame the landmarks are normalised
     * against. The overlay needs these to reproduce PreviewView's centre-crop: mapping
     * 0..1 straight onto the canvas stretches the mesh whenever the camera's aspect ratio
     * (4:3) differs from the screen's, which on a tall phone it always does.
     */
    val frameWidth: Int = 0,
    val frameHeight: Int = 0,
) {
    val usable: Boolean get() = facePresent && eyesOpen && poseInEnvelope

    // Equals/hashCode are not meaningful for a frame carrying large arrays, and the
    // generated versions would compare 478 floats on every recomposition.
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)

    companion object {
        fun noFace(tMs: Long, latencyMs: Long = 0L) =
            FaceTrackingFrame(tMs = tMs, facePresent = false, inferenceLatencyMs = latencyMs)
    }
}

/**
 * Running per-session capture quality.
 *
 * A screening instrument must never confuse "the child did not look" with "the tracker
 * failed", so the denominator of every ratio is recorded explicitly rather than inferred.
 */
data class CaptureQuality(
    val framesIn: Int = 0,
    val framesWithFace: Int = 0,
    val framesUsable: Int = 0,
    val framesRejectedPose: Int = 0,
    val framesBlink: Int = 0,
    val errors: Int = 0,
    val medianIntervalMs: Long = 0L,
    val effectiveFps: Float = 0f,
    val delegate: String = "unknown",
) {
    val faceRatio: Float get() = if (framesIn == 0) 0f else framesWithFace.toFloat() / framesIn
    val usableRatio: Float get() = if (framesIn == 0) 0f else framesUsable.toFloat() / framesIn
}

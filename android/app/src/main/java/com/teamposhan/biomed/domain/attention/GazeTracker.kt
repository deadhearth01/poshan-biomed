package com.teamposhan.biomed.domain.attention

import kotlin.math.abs
import kotlin.math.hypot

/**
 * One analysed camera frame, reduced to the signals the screening needs.
 *
 * [gazeX] is 0..1 across the child's view (0.5 = centre) and is already corrected for
 * the mirrored front camera. [yaw]/[pitch] are head-pose proxies in radians;
 * positive yaw means the face has turned toward the image-left.
 */
data class GazeSample(
    val tMs: Long,
    val facePresent: Boolean,
    val gazeX: Float = 0.5f,
    val gazeY: Float = 0.5f,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val eyesOpen: Boolean = false,
)

enum class GazeRegion { LEFT, CENTRE, RIGHT, AWAY }

/** Which half of the screen currently holds the social stimulus (or null between trials). */
data class AoiContext(val socialOnLeft: Boolean)

/**
 * MediaPipe FaceLandmarker geometry helpers.
 *
 * Landmark indices follow the 478-point face mesh: 468 is the right-iris centre, 473 the
 * left-iris centre, with the eye corners and lids around them. The gaze ratios are
 * calibration-free: iris position expressed as a fraction of the eye opening. Because the
 * two eyes move in opposite image directions, they are combined into one screen axis.
 */
object GazeGeometry {

    /** 478-point mesh: iris centres. Public so the dev overlay marks the same points the maths uses. */
    const val IRIS_RIGHT_CENTRE = 468
    const val IRIS_LEFT_CENTRE = 473
    private const val IRIS_R_CENTRE = 468
    private const val IRIS_L_CENTRE = 473
    private const val R_EYE_OUTER = 33
    private const val R_EYE_INNER = 133
    private const val L_EYE_INNER = 362
    private const val L_EYE_OUTER = 263
    private const val R_LID_UPPER = 159
    private const val R_LID_LOWER = 145
    private const val L_LID_UPPER = 386
    private const val L_LID_LOWER = 374
    private const val NOSE_TIP = 1
    private const val CHIN = 152
    private const val FOREHEAD = 10
    private const val L_CHEEK = 234
    private const val R_CHEEK = 454

    fun horizontalGaze(x: FloatArray, y: FloatArray): Float {
        val rOuter = x[R_EYE_OUTER]
        val rInner = x[R_EYE_INNER]
        val rRatio = if (abs(rInner - rOuter) > 1e-5f) (x[IRIS_R_CENTRE] - rInner) / (rOuter - rInner) else 0.5f

        val lInner = x[L_EYE_INNER]
        val lOuter = x[L_EYE_OUTER]
        val lRatio = if (abs(lOuter - lInner) > 1e-5f) (x[IRIS_L_CENTRE] - lInner) / (lOuter - lInner) else 0.5f

        return (((1f - rRatio) + lRatio) / 2f).coerceIn(0f, 1f)
    }

    fun verticalGaze(y: FloatArray): Float {
        val rUp = y[R_LID_UPPER]
        val rDown = y[R_LID_LOWER]
        val rRatio = if (abs(rDown - rUp) > 1e-5f) (y[IRIS_R_CENTRE] - rUp) / (rDown - rUp) else 0.5f
        val lUp = y[L_LID_UPPER]
        val lDown = y[L_LID_LOWER]
        val lRatio = if (abs(lDown - lUp) > 1e-5f) (y[IRIS_L_CENTRE] - lUp) / (lDown - lUp) else 0.5f
        return ((rRatio + lRatio) / 2f).coerceIn(0f, 1f)
    }

    /** Yaw proxy in radians; positive = turned toward the image-left cheek. */
    fun yaw(x: FloatArray, y: FloatArray): Float {
        val span = hypot(x[L_CHEEK] - x[R_CHEEK], y[L_CHEEK] - y[R_CHEEK]).coerceAtLeast(1e-4f)
        val dLeft = hypot(x[NOSE_TIP] - x[L_CHEEK], y[NOSE_TIP] - y[L_CHEEK])
        val dRight = hypot(x[NOSE_TIP] - x[R_CHEEK], y[NOSE_TIP] - y[R_CHEEK])
        return ((dLeft - dRight) / span) * 2.2f
    }

    /** Pitch proxy in radians; positive = chin lifted (looking up). */
    fun pitch(x: FloatArray, y: FloatArray): Float {
        val span = hypot(x[CHIN] - x[FOREHEAD], y[CHIN] - y[FOREHEAD]).coerceAtLeast(1e-4f)
        val toNose = hypot(x[NOSE_TIP] - x[FOREHEAD], y[NOSE_TIP] - y[FOREHEAD])
        return ((toNose / span) - 0.5f) * 2.0f
    }

    /** Rough viewing distance from the inter-iris span (horizontal iris ≈ 11.7 mm). */
    fun approximateDistanceCm(irisSpanNormalised: Float): Float? {
        if (irisSpanNormalised <= 1e-4f) return null
        // Empirical mapping for a ~70° front camera; kept indicative on purpose.
        val cm = 0.42f / irisSpanNormalised
        return cm.coerceIn(15f, 150f)
    }
}

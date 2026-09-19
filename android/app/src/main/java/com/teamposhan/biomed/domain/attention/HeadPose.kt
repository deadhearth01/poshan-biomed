package com.teamposhan.biomed.domain.attention

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2

/**
 * Head pose from MediaPipe's facial transformation matrix.
 *
 * FaceLandmarker can emit a 4x4 matrix mapping the canonical face model onto the detected
 * face, fitted by Procrustes analysis. That is a real rigid rotation — unlike the
 * landmark-distance ratios with hand-tuned constants (`* 2.2f`) this replaces, which were
 * documented as radians but were not radians in any meaningful sense.
 *
 * **The translation is deliberately not exposed.** The matrix is solved against a camera
 * hardcoded to a 63 degree vertical FOV in MediaPipe's own graph
 * (`face_geometry_from_landmarks_graph.cc`), which our front camera is not, and the
 * canonical model is adult-scaled — so fitting it to a child's face places that face
 * further away than it is. Distance comes from [Anthropometry] instead.
 */
data class HeadPose(
    /** Radians. Positive = turned toward the image-right. */
    val yaw: Float,
    /** Radians. Positive = chin lifted. */
    val pitch: Float,
    /** Radians. Positive = head tilted clockwise in the image. */
    val roll: Float,
    /** False when the decomposition hit gimbal lock and the angles are unreliable. */
    val reliable: Boolean = true,
) {
    val yawDegrees: Float get() = Math.toDegrees(yaw.toDouble()).toFloat()
    val pitchDegrees: Float get() = Math.toDegrees(pitch.toDouble()).toFloat()
    val rollDegrees: Float get() = Math.toDegrees(roll.toDouble()).toFloat()

    companion object {
        val UNKNOWN = HeadPose(0f, 0f, 0f, reliable = false)

        /**
         * Decomposes a row-major 4x4 transformation matrix to intrinsic Y-X-Z Euler angles
         * (yaw, then pitch, then roll) — the convention that matches human intuition about
         * heads.
         *
         * Gimbal lock for Y-X-Z occurs at pitch = +/-90 degrees, i.e. a child looking
         * straight up or down. There yaw and roll become degenerate and jump wildly, so the
         * frame is flagged unreliable rather than emitting garbage.
         */
        fun fromMatrix(m: FloatArray): HeadPose {
            if (m.size != 16) return UNKNOWN

            // Row-major: R = [ m0 m1 m2 ; m4 m5 m6 ; m8 m9 m10 ]
            val r20 = m[8]
            val r21 = m[9]
            val r22 = m[10]
            val r01 = m[1]
            val r11 = m[5]

            val sinPitch = (-r21).coerceIn(-1f, 1f)
            if (abs(sinPitch) > 0.998f) {
                // Gimbal lock: yaw and roll are not separable here.
                return HeadPose(0f, asin(sinPitch), 0f, reliable = false)
            }

            return HeadPose(
                yaw = atan2(r20, r22),
                pitch = asin(sinPitch),
                roll = atan2(r01, r11),
                reliable = true,
            )
        }

        /**
         * Mirrors a pose for a front camera whose frame has been horizontally flipped.
         * Yaw and roll invert; pitch does not.
         */
        fun mirrored(pose: HeadPose): HeadPose =
            pose.copy(yaw = -pose.yaw, roll = -pose.roll)
    }
}

/**
 * The envelope within which gaze estimation is trustworthy.
 *
 * Outside roughly +/-15 degrees of yaw, head rotation injects more apparent gaze error than
 * the signal contains: every 1 degree of yaw displaces apparent gaze by about 0.83 degrees,
 * so a 10 degree turn produces ~8.3 degrees of phantom shift against a left/right budget of
 * 7.8 degrees (landscape, edge-placed targets, 40 cm). Frames outside the envelope are
 * rejected rather than silently corrupting the measurement.
 */
object PoseGate {
    const val MAX_YAW_DEGREES = 15f
    const val MAX_PITCH_DEGREES = 12f

    fun accepts(pose: HeadPose): Boolean =
        pose.reliable &&
            abs(pose.yawDegrees) <= MAX_YAW_DEGREES &&
            abs(pose.pitchDegrees) <= MAX_PITCH_DEGREES
}

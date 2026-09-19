package com.teamposhan.biomed.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mediapipe.tasks.vision.facelandmarker.FaceMeshTopology
import com.teamposhan.biomed.domain.attention.CaptureQuality
import com.teamposhan.biomed.domain.attention.FaceTrackingFrame
import com.teamposhan.biomed.domain.attention.GazeGeometry
import com.teamposhan.biomed.domain.attention.PoseGate
import com.teamposhan.biomed.domain.attention.TrackingQuality
import com.teamposhan.biomed.domain.attention.degreesOrDash
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Which diagnostic layers are drawn. Every layer is independently switchable. */
data class MeshLayers(
    val tessellation: Boolean = true,
    val contours: Boolean = true,
    val iris: Boolean = true,
    val poseAxes: Boolean = true,
    val gaze: Boolean = true,
    val hud: Boolean = true,
)

/**
 * Developer overlay: draws what MediaPipe is actually tracking, on top of the live preview.
 *
 * Two things this gets right that a naive overlay does not:
 *
 *  1. **The topology is MediaPipe's own.** Tessellation, oval, lips, eyes, brows and irises
 *     come from the connection sets shipped with the model (via [FaceMeshTopology]), so the
 *     wireframe is the real triangulation rather than a scatter of dots.
 *  2. **The mapping reproduces PreviewView's centre-crop.** `PreviewView.ScaleType.FILL_CENTER`
 *     scales the camera frame to *cover* the view and crops the overflow. Mapping normalised
 *     landmarks straight onto the canvas instead stretches them by the ratio between the
 *     camera's 4:3 and the screen's aspect — on a tall phone the mesh then sits visibly off
 *     the face even when the tracking is perfect.
 *
 * It reads the same [FaceTrackingFrame] the screening pipeline consumes, so if the mesh sits
 * correctly on the face, the maths underneath is using the same correct geometry.
 */
@Composable
fun FaceMeshOverlay(
    frame: FaceTrackingFrame?,
    quality: CaptureQuality,
    tracking: TrackingQuality,
    modifier: Modifier = Modifier,
    layers: MeshLayers = MeshLayers(),
) {
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val f = frame ?: return@Canvas
            if (!f.facePresent || f.landmarkX.size < 478) return@Canvas
            val map = FrameMapping.of(f, size.width, size.height)

            if (layers.tessellation) {
                drawEdges(f, map, FaceMeshTopology.tessellation, MeshColor, 0.7f)
            }
            if (layers.contours) {
                drawEdges(f, map, FaceMeshTopology.faceOval, OvalColor, 2.0f)
                drawEdges(f, map, FaceMeshTopology.lips, OvalColor, 1.8f)
                drawEdges(f, map, FaceMeshTopology.leftEye, EyeColor, 1.8f)
                drawEdges(f, map, FaceMeshTopology.rightEye, EyeColor, 1.8f)
                drawEdges(f, map, FaceMeshTopology.leftBrow, BrowColor, 1.8f)
                drawEdges(f, map, FaceMeshTopology.rightBrow, BrowColor, 1.8f)
            }
            if (layers.iris) drawIris(f, map)
            if (layers.poseAxes) drawPoseAxes(f, map)
            if (layers.gaze) drawGazeMarker(f, map)
        }
        if (layers.hud) {
            TrackingHud(
                frame = frame,
                quality = quality,
                tracking = tracking,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Geometry
// ---------------------------------------------------------------------------

private val MeshColor = Color(0x66FFFFFF)
private val OvalColor = Color(0xFF00E5FF)
private val EyeColor = Color(0xFF448AFF)
private val BrowColor = Color(0xFFE040FB)
private val IrisColor = Color(0xFFFFC400)
private val GoodColor = Color(0xFF00E676)
private val BadColor = Color(0xFFFF5252)

/**
 * Normalised-landmark to canvas mapping that mirrors `PreviewView.ScaleType.FILL_CENTER`:
 * scale to cover, then centre the overflow so the cropped-away margins are split evenly.
 */
private class FrameMapping(
    private val scale: Float,
    private val dx: Float,
    private val dy: Float,
    private val frameW: Float,
    private val frameH: Float,
) {
    fun x(nx: Float): Float = nx * frameW * scale + dx
    fun y(ny: Float): Float = ny * frameH * scale + dy

    companion object {
        fun of(f: FaceTrackingFrame, canvasW: Float, canvasH: Float): FrameMapping {
            val fw = (if (f.frameWidth > 0) f.frameWidth else 480).toFloat()
            val fh = (if (f.frameHeight > 0) f.frameHeight else 640).toFloat()
            val scale = max(canvasW / fw, canvasH / fh)
            return FrameMapping(
                scale = scale,
                dx = (canvasW - fw * scale) / 2f,
                dy = (canvasH - fh * scale) / 2f,
                frameW = fw,
                frameH = fh,
            )
        }
    }
}

private fun DrawScope.point(f: FaceTrackingFrame, map: FrameMapping, i: Int): Offset =
    Offset(map.x(f.landmarkX[i]), map.y(f.landmarkY[i]))

/** Draws a flattened `[start, end, ...]` connection list. */
private fun DrawScope.drawEdges(
    f: FaceTrackingFrame,
    map: FrameMapping,
    edges: IntArray,
    color: Color,
    widthDp: Float,
) {
    val w = widthDp.dp.toPx()
    val n = f.landmarkX.size
    var i = 0
    while (i + 1 < edges.size) {
        val a = edges[i]
        val b = edges[i + 1]
        if (a < n && b < n) {
            drawLine(color, point(f, map, a), point(f, map, b), strokeWidth = w)
        }
        i += 2
    }
}

private fun DrawScope.drawIris(f: FaceTrackingFrame, map: FrameMapping) {
    drawEdges(f, map, FaceMeshTopology.leftIris, IrisColor, 2.0f)
    drawEdges(f, map, FaceMeshTopology.rightIris, IrisColor, 2.0f)
    val right = point(f, map, GazeGeometry.IRIS_RIGHT_CENTRE)
    val left = point(f, map, GazeGeometry.IRIS_LEFT_CENTRE)
    listOf(right, left).forEach { drawCircle(IrisColor, radius = 2.dp.toPx(), center = it) }
    // The scale reference distance is derived from: a wrong IPD shows up here immediately.
    drawLine(IrisColor.copy(alpha = 0.5f), right, left, strokeWidth = 1.dp.toPx())
}

/**
 * Head-pose axes at the nose tip: blue forward, green up, red right. If these swing the
 * wrong way as the child turns, handedness is inverted — a failure that otherwise looks
 * exactly like a clinical finding.
 */
private fun DrawScope.drawPoseAxes(f: FaceTrackingFrame, map: FrameMapping) {
    val pose = f.headPose
    if (!pose.reliable) return
    val origin = point(f, map, NOSE_TIP)
    val len = size.minDimension * 0.12f

    val cy = cos(pose.yaw); val sy = sin(pose.yaw)
    val cp = cos(pose.pitch); val sp = sin(pose.pitch)
    val cr = cos(pose.roll); val sr = sin(pose.roll)

    val forward = Offset(origin.x + len * sy * cp, origin.y + len * sp)
    val up = Offset(origin.x + len * (-sr), origin.y - len * cr * cp)
    val right = Offset(origin.x + len * cr * cy, origin.y - len * sr)

    val stroke = 2.5.dp.toPx()
    drawLine(Color(0xFF448AFF), origin, forward, strokeWidth = stroke)
    drawLine(GoodColor, origin, up, strokeWidth = stroke)
    drawLine(BadColor, origin, right, strokeWidth = stroke)
}

private fun DrawScope.drawGazeMarker(f: FaceTrackingFrame, map: FrameMapping) {
    val colour = if (f.usable) GoodColor else BadColor
    val x = map.x(f.gazeX)
    drawLine(colour.copy(alpha = 0.8f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
    drawCircle(
        colour,
        radius = 6.dp.toPx(),
        center = Offset(x, map.y(f.gazeY)),
        style = Stroke(width = 2.dp.toPx()),
    )
}

// ---------------------------------------------------------------------------
// HUD
// ---------------------------------------------------------------------------

@Composable
private fun TrackingHud(
    frame: FaceTrackingFrame?,
    quality: CaptureQuality,
    tracking: TrackingQuality,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .background(Color(0xCC000000), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        HudLine("delegate", quality.delegate, GoodColor.takeIf { quality.delegate == "GPU" })
        HudLine(
            "fps",
            "%.1f (%d ms)".format(quality.effectiveFps, quality.medianIntervalMs),
            // Below ~15 fps almost nothing in the literature-derived metric set survives.
            if (quality.effectiveFps >= 15f) GoodColor else BadColor,
        )
        HudLine("frames", "${quality.framesIn} in · ${quality.framesWithFace} face")
        HudLine(
            "usable",
            "%.0f%%".format(quality.usableRatio * 100),
            if (quality.usableRatio >= 0.5f) GoodColor else BadColor,
        )
        HudLine("rejected", "pose ${quality.framesRejectedPose} · blink ${quality.framesBlink}")
        if (quality.errors > 0) HudLine("errors", quality.errors.toString(), BadColor)

        HudDivider()

        if (frame == null || !frame.facePresent) {
            HudLine("face", "not detected", BadColor)
        } else {
            val p = frame.headPose
            HudLine("frame", "${frame.frameWidth}x${frame.frameHeight}")
            HudLine(
                "yaw/pitch/roll",
                "%.0f / %.0f / %.0f°".format(p.yawDegrees, p.pitchDegrees, p.rollDegrees),
                if (frame.poseInEnvelope) GoodColor else BadColor,
            )
            HudLine(
                "gate",
                "±${PoseGate.MAX_YAW_DEGREES.toInt()}° yaw, ±${PoseGate.MAX_PITCH_DEGREES.toInt()}° pitch",
            )
            HudLine("gaze", "%.2f, %.2f".format(frame.gazeX, frame.gazeY))
            HudLine("blink", "%.2f".format(frame.blinkScore), if (frame.eyesOpen) null else BadColor)
            HudLine("ipd", "%.3f norm".format(frame.ipdPx))
            HudLine("latency", "${frame.inferenceLatencyMs} ms")
        }

        HudDivider()

        HudLine("precision RMS-S2S", tracking.precisionDegrees.degreesOrDash())
        HudLine(
            "accuracy",
            tracking.accuracyDegrees.degreesOrDash() +
                if (tracking.accuracySamples > 0) "  (n=${tracking.accuracySamples})" else "",
        )
        HudLine("worst", tracking.worstAccuracyDegrees.degreesOrDash())
        val verdict = tracking.halfScreenVerdict()
        HudLine(
            "L/R decision",
            verdict,
            when (verdict) {
                "reliable" -> GoodColor
                "marginal" -> Color(0xFFFFB300)
                "not reliable" -> BadColor
                else -> null
            },
        )
        Text(
            "ref: toddler 1.3° acc / 0.25° prec on calibrated IR",
            color = Color(0xFF9E9E9E),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun HudLine(label: String, value: String, accent: Color? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(
            label,
            color = Color(0xFFBDBDBD),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            value,
            color = accent ?: Color.White,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HudDivider() {
    Box(
        Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .background(Color(0x33FFFFFF)),
    ) { Text(" ", fontSize = 1.sp) }
}

private const val NOSE_TIP = 1

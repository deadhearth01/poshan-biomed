package com.teamposhan.biomed.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.teamposhan.biomed.domain.attention.PursuitTarget
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The drawn moving target.
 *
 * Drawn rather than filmed or drawn-from-an-asset for one reason: **position is a function of
 * time**, identical on every device, known to the millisecond. That is what lets the recorder
 * compare gaze against truth rather than against a region.
 *
 * Design constraints come from infant vision, not from taste:
 *
 *  - **Large.** In portrait each half of a split screen subtends under 5 degrees at 40 cm,
 *    which is why faces read poorly there. A drawn shape has no such limit and is sized at
 *    roughly a sixth of the screen width — comfortably above any acuity threshold in the
 *    0-5 age range.
 *  - **High contrast, low spatial frequency.** Infant contrast sensitivity peaks near
 *    0.5-2 cycles per degree, far below the adult peak, so the shape is bold and solid with
 *    no fine detail. Detail finer than about an eighth of the shape would simply not register.
 *  - **A halo behind it.** Separates the target from the background at low contrast
 *    sensitivity and makes the leading edge easier to track.
 *  - **Gentle pulsing.** Motion of the shape itself, distinct from motion across the screen,
 *    holds attention for the length of a trial. It does not move the centroid, so it cannot
 *    corrupt the position that scoring uses.
 */
@Composable
fun PursuitStage(
    path: PursuitTarget.Path,
    elapsedMs: Long,
    durationMs: Long,
    shape: TargetShape = TargetShape.STAR,
    modifier: Modifier = Modifier,
) {
    val (tx, ty) = path.positionAt(elapsedMs, durationMs)

    val transition = rememberInfiniteTransition(label = "pursuit-target")
    val pulse by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spin",
    )

    val primary = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.surfaceContainerLowest

    Box(modifier.fillMaxSize().background(bg)) {
        Canvas(Modifier.fillMaxSize()) {
            val centre = Offset(tx * size.width, ty * size.height)
            // A sixth of the short edge: large enough to be trivially resolvable at any age
            // in range, small enough that its position is unambiguous.
            val radius = min(size.width, size.height) / 6f * pulse

            // Halo first, so the target reads against the background even for a child with
            // low contrast sensitivity.
            drawCircle(primary.copy(alpha = 0.18f), radius * 1.7f, centre)
            drawCircle(primary.copy(alpha = 0.30f), radius * 1.25f, centre)

            when (shape) {
                TargetShape.STAR -> drawStar(centre, radius, spin, primary)
                TargetShape.BALL -> {
                    drawCircle(primary, radius, centre)
                    // Offset highlight: gives the ball a 3D read, which holds attention
                    // better than a flat disc.
                    drawCircle(
                        Color.White.copy(alpha = 0.8f),
                        radius * 0.3f,
                        Offset(centre.x - radius * 0.3f, centre.y - radius * 0.3f),
                    )
                }
                TargetShape.RING -> {
                    drawCircle(primary, radius, centre, style = Stroke(width = radius * 0.4f))
                    drawCircle(Color.White.copy(alpha = 0.9f), radius * 0.18f, centre)
                }
            }
        }
    }
}

enum class TargetShape { STAR, BALL, RING }

/** A five-pointed star. Solid fill, no outline detail — see the class note on spatial frequency. */
private fun DrawScope.drawStar(centre: Offset, radius: Float, rotationDeg: Float, colour: Color) {
    val points = 5
    val inner = radius * 0.45f
    val path = Path()
    val start = Math.toRadians(rotationDeg.toDouble() - 90.0)
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else inner
        val a = start + i * Math.PI / points
        val x = centre.x + (r * cos(a)).toFloat()
        val y = centre.y + (r * sin(a)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, colour)
    // A white core marks the centroid, which is the point the child's gaze is scored
    // against. Giving them something to actually fixate tightens the estimate.
    drawCircle(Color.White.copy(alpha = 0.85f), radius * 0.16f, centre)
}

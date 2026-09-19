package com.teamposhan.biomed.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.teamposhan.biomed.data.model.Sex
import com.teamposhan.biomed.domain.growth.GrowthStandards
import com.teamposhan.biomed.domain.growth.Lms
import com.teamposhan.biomed.ui.components.PoshanCard
import com.teamposhan.biomed.ui.haptics.rememberAppHaptics
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// WHO chart rendering
//
// Deliberately drawn to match the printed WHO Child Growth Standards sheet the field
// staff already know: blue surround, white plot, a grid line at every month and every
// unit, a darker rule at each completed year, and the z-curves in the WHO's own colours
// (0 green, ±1 orange, ±2 crimson, ±3 black) with the SD number at the right-hand end.
// ---------------------------------------------------------------------------

/** The WHO publishes each indicator as a birth-to-2 and a 2-to-5 sheet. */
enum class ChartBand(val from: Float, val to: Float, val label: String) {
    BIRTH_TO_2(0f, 24f, "Birth to 2 years"),
    TWO_TO_5(24f, 60f, "2 to 5 years"),
    BIRTH_TO_5(0f, 60f, "Birth to 5 years");

    companion object {
        fun forAge(ageMonths: Float): ChartBand =
            if (ageMonths < 24f) BIRTH_TO_2 else TWO_TO_5
    }
}

/** A measurement to plot: one per visit. */
data class GrowthPoint(val ageMonths: Float, val value: Float)

private object WhoPalette {
    val frame = Color(0xFF1B9CD8)
    val frameText = Color.White
    val plot = Color.White
    val gridMinor = Color(0xFFDCDCDC)
    val gridSub = Color(0xFFEFEFEF)
    val gridMajor = Color(0xFF8A8A8A)
    val z0 = Color(0xFF00843D)
    val z1 = Color(0xFFF07E26)
    val z2 = Color(0xFFD41E4B)
    val z3 = Color(0xFF000000)

    fun forSd(sd: Float): Color = when (abs(sd)) {
        0f -> z0
        1f -> z1
        2f -> z2
        else -> z3
    }
}

/** How near a press must land to a recorded visit for that visit to be read out. */
private const val SNAP_MONTHS = 2f

/** The z-curves the WHO sheet draws, outermost first so the median lands on top. */
private val SD_CURVES = listOf(3f, -3f, 2f, -2f, 1f, -1f, 0f)

/**
 * The WHO growth chart itself: frame, grid, z-curves and the child's plotted trajectory.
 *
 * @param history every recorded measurement for this indicator, oldest first. The latest
 *   point is the one that gets the emphasised marker and the drop-line to the axis.
 * @param zoom set by the detail screen's pinch gesture; 1f is the printed sheet's scale.
 */
@Composable
fun WhoGrowthChart(
    metric: GrowthStandards.Metric,
    sex: Sex,
    band: ChartBand,
    history: List<GrowthPoint>,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    showMonthNumbers: Boolean = true,
    zoom: Float = 1f,
    pan: Offset = Offset.Zero,
    probeEnabled: Boolean = false,
) {
    val density = LocalDensity.current
    val info = remember(metric, band) { GrowthStandards.info(metric, band.from) }
    val rows = remember(metric, sex, band) {
        GrowthStandards.curve(metric, sex, band.from, band.to)
    }

    val latest = history.lastOrNull()
    val z = latest?.let { GrowthStandards.zFor(metric, sex, it.ageMonths, it.value) }
    val markerColor = zColor(z)

    var started by remember(metric, sex, band) { mutableStateOf(!animate) }
    LaunchedEffect(metric, sex, band, history.size) { started = true }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 90f),
        label = "growth-plot",
    )

    // The y-window: the printed range for the band, widened if the child falls outside it
    // (a severely wasted child must still appear on the sheet, not be clipped off it).
    val range = remember(metric, band, history) {
        val printed = if (band == ChartBand.TWO_TO_5) info.olderRange else info.youngRange
        var lo = printed.start
        var hi = printed.endInclusive
        history.forEach { p ->
            if (p.value < lo) lo = floor(p.value - 1f)
            if (p.value > hi) hi = ceil(p.value + 1f)
        }
        // Keep ±3 SD on screen wherever the curves run.
        rows.forEach { r ->
            val low = GrowthStandards.valueAt(r, -3f)
            val high = GrowthStandards.valueAt(r, 3f)
            if (low < lo) lo = floor(low)
            if (high > hi) hi = ceil(high)
        }
        lo..hi
    }
    val yStep = if (band == ChartBand.TWO_TO_5) info.olderStep else info.youngStep

    // Long-press anywhere on the sheet to read the curves off at that age; drag to sweep.
    var probeX by remember(metric, band) { mutableStateOf<Float?>(null) }
    val probeModifier = if (probeEnabled) {
        Modifier.pointerInput(metric, band, zoom, pan) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset -> probeX = offset.x },
                onDragEnd = { probeX = null },
                onDragCancel = { probeX = null },
                onDrag = { change, _ -> probeX = change.position.x },
            )
        }
    } else {
        Modifier
    }

    Canvas(modifier.then(probeModifier)) {
        val geo = ChartGeometry(
            scope = this,
            density = density,
            band = band,
            yMin = range.start,
            yMax = range.endInclusive,
            zoom = zoom,
            pan = pan,
        )

        geo.drawFrame()
        geo.drawGrid(yStep, showMonthNumbers)
        geo.drawCurves(rows)
        geo.drawAxisText(info, yStep, showMonthNumbers)
        geo.drawChild(history, progress, markerColor)
        probeX?.let { geo.drawProbe(it, metric, sex, info, history) }
    }
}

// ---------------------------------------------------------------------------
// Geometry + drawing
// ---------------------------------------------------------------------------

private class ChartGeometry(
    val scope: DrawScope,
    val density: Density,
    val band: ChartBand,
    val yMin: Float,
    val yMax: Float,
    val zoom: Float,
    val pan: Offset,
) {
    fun dp(value: Float): Float = with(density) { value.dp.toPx() }

    private val w = scope.size.width
    private val h = scope.size.height

    // blue frame -> white plot -> drawing area
    private val whiteLeft = dp(30f)
    private val whiteRight = w - dp(26f)
    private val whiteTop = dp(8f)
    private val whiteBottom = h - dp(28f)

    /** The strip at the right of the white area that carries the SD numbers. */
    private val sdLabelStrip = dp(16f)

    val plotLeft = whiteLeft + dp(2f)
    val plotRight = whiteRight - sdLabelStrip
    val plotTop = whiteTop + dp(4f)
    val plotBottom = whiteBottom - dp(4f)

    val plotW = plotRight - plotLeft
    private val plotH = plotBottom - plotTop

    fun x(month: Float): Float {
        val t = ((month - band.from) / (band.to - band.from)).coerceIn(0f, 1f)
        return plotLeft + (t * plotW) * zoom + pan.x
    }

    fun y(value: Float): Float {
        val t = ((value - yMin) / (yMax - yMin)).coerceIn(0f, 1f)
        return plotBottom - (t * plotH) * zoom + pan.y
    }

    private fun paint(sizeSp: Float, color: Color, bold: Boolean = false, align: android.graphics.Paint.Align = android.graphics.Paint.Align.LEFT) =
        android.graphics.Paint().apply {
            this.color = color.toArgb()
            textSize = dp(sizeSp)
            isAntiAlias = true
            textAlign = align
            typeface = if (bold) {
                android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            } else {
                android.graphics.Typeface.DEFAULT
            }
        }

    fun drawFrame() = with(scope) {
        drawRoundRect(
            color = WhoPalette.frame,
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(dp(10f), dp(10f)),
        )
        drawRect(
            color = WhoPalette.plot,
            topLeft = Offset(whiteLeft, whiteTop),
            size = androidx.compose.ui.geometry.Size(whiteRight - whiteLeft, whiteBottom - whiteTop),
        )
    }

    /**
     * Grid at every unit and every month, with a heavier rule at each completed year.
     *
     * Zooming in subdivides each cell rather than just magnifying it: at 1.8x every cell
     * is halved, at 3x quartered, so a reading can be taken off the sheet as precisely as
     * the zoom level suggests it should be.
     */
    fun drawGrid(yStep: Float, showMonthNumbers: Boolean) = with(scope) {
        val subdivisions = when {
            zoom >= 3f -> 4
            zoom >= 1.8f -> 2
            else -> 1
        }
        clipPlot {
            // Sub-grid first, so the main gridlines stay on top of it.
            if (subdivisions > 1) {
                val subY = yStep / subdivisions
                var v = ceil(yMin / subY) * subY
                while (v <= yMax + 0.001f) {
                    val yy = y(v)
                    drawLine(WhoPalette.gridSub, Offset(plotLeft, yy), Offset(plotRight, yy), strokeWidth = dp(0.4f))
                    v += subY
                }
                val subMonth = baseMonthStep() / subdivisions
                var m = band.from
                while (m <= band.to + 0.001f) {
                    drawLine(
                        color = WhoPalette.gridSub,
                        start = Offset(x(m), plotTop),
                        end = Offset(x(m), plotBottom),
                        strokeWidth = dp(0.4f),
                    )
                    m += subMonth
                }
            }

            var v = ceil(yMin / yStep) * yStep
            while (v <= yMax + 0.001f) {
                val yy = y(v)
                drawLine(WhoPalette.gridMinor, Offset(plotLeft, yy), Offset(plotRight, yy), strokeWidth = dp(0.6f))
                v += yStep
            }

            // A vertical line per month on the 2-year sheet, per quarter on the 5-year one.
            var m = band.from
            while (m <= band.to + 0.001f) {
                val isYear = (m % 12f) == 0f
                drawLine(
                    color = if (isYear) WhoPalette.gridMajor else WhoPalette.gridMinor,
                    start = Offset(x(m), plotTop),
                    end = Offset(x(m), plotBottom),
                    strokeWidth = if (isYear) dp(0.9f) else dp(0.6f),
                )
                m += baseMonthStep()
            }
        }
    }

    private fun baseMonthStep(): Float = if (band == ChartBand.BIRTH_TO_5) 3f else 1f

    /** Screen x back to an age in months — the inverse of [x]. */
    private fun monthAt(px: Float): Float {
        val t = ((px - pan.x - plotLeft) / (plotW * zoom)).coerceIn(0f, 1f)
        return band.from + t * (band.to - band.from)
    }

    /**
     * The long-press readout.
     *
     * Deliberately shows ONLY the child's own plotted measurement — not the seven
     * reference curves. The reference values are what the sheet already draws; what the
     * worker is asking when they press the line is "what did we actually record here?".
     * With no measurement at that age there is nothing to report, so only the crosshair
     * and the age appear.
     */
    fun drawProbe(
        probeX: Float,
        metric: GrowthStandards.Metric,
        sex: Sex,
        info: GrowthStandards.MetricInfo,
        history: List<GrowthPoint>,
    ) = with(scope) {
        val month = monthAt(probeX).coerceIn(
            maxOf(band.from, GrowthStandards.firstMonth(metric)),
            band.to,
        )

        // Snap to the nearest recorded visit rather than reading off an interpolated
        // position the child was never measured at.
        val plotted = history
            .filter { it.ageMonths in band.from..band.to }
            .minByOrNull { abs(it.ageMonths - month) }
            ?.takeIf { abs(it.ageMonths - month) <= SNAP_MONTHS }

        val px = plotted?.let { x(it.ageMonths) } ?: x(month)

        clipPlot {
            drawLine(
                color = Color.Black.copy(alpha = 0.5f),
                start = Offset(px, plotTop),
                end = Offset(px, plotBottom),
                strokeWidth = dp(1f),
            )
            plotted?.let {
                val py = y(it.value)
                drawCircle(Color.Black.copy(alpha = 0.18f), radius = dp(11f), center = Offset(px, py))
                drawCircle(Color.White, radius = dp(5f), center = Offset(px, py))
                drawCircle(Color.Black.copy(alpha = 0.75f), radius = dp(5f), center = Offset(px, py), style = Stroke(width = dp(1.4f)))
            }
        }

        val lines = mutableListOf<Pair<String, Color>>()
        if (plotted == null) {
            lines += monthLabel(month) to Color.Black
            lines += "no measurement" to Color(0xFF6B6B6B)
        } else {
            val z = GrowthStandards.zFrom(GrowthStandards.lmsAt(metric, sex, plotted.ageMonths), plotted.value)
            lines += monthLabel(plotted.ageMonths) to Color.Black
            lines += "%.${info.decimals}f %s".format(plotted.value, info.unit) to Color(0xFF1B1B1B)
            lines += "%+.1f SD".format(z) to sdTextColor(z)
        }

        val textPaint = paint(10f, Color.Black)
        val lineHeight = dp(14f)
        val boxWidth = lines.maxOf { textPaint.measureText(it.first) } + dp(18f)
        val boxHeight = lineHeight * lines.size + dp(12f)
        // Flip the callout to the other side when it would run off the sheet.
        val boxLeft = if (px + dp(10f) + boxWidth > plotRight) px - dp(10f) - boxWidth else px + dp(10f)
        val boxTop = (plotTop + dp(6f)).coerceAtMost(plotBottom - boxHeight - dp(4f))

        drawRoundRect(
            color = Color.White.copy(alpha = 0.97f),
            topLeft = Offset(boxLeft, boxTop),
            size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(dp(8f), dp(8f)),
        )
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.25f),
            topLeft = Offset(boxLeft, boxTop),
            size = androidx.compose.ui.geometry.Size(boxWidth, boxHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(dp(8f), dp(8f)),
            style = Stroke(width = dp(0.8f)),
        )

        val canvas = drawContext.canvas.nativeCanvas
        lines.forEachIndexed { i, (text, color) ->
            canvas.drawText(
                text,
                boxLeft + dp(9f),
                boxTop + dp(15f) + lineHeight * i,
                paint(10f, color, bold = i == 0),
            )
        }
    }

    private fun sdTextColor(z: Float): Color = when {
        z < -3f || z > 3f -> WhoPalette.z3
        z < -2f || z > 2f -> WhoPalette.z2
        z < -1f || z > 1f -> WhoPalette.z1
        else -> WhoPalette.z0
    }

    private fun monthLabel(month: Float): String {
        val whole = month.roundToInt()
        return when {
            whole == 0 -> "Birth"
            whole % 12 == 0 -> "${whole / 12} yr"
            else -> "$whole mo"
        }
    }

    fun drawCurves(rows: List<Lms>) = with(scope) {
        if (rows.isEmpty()) return@with
        clipPlot {
            SD_CURVES.forEach { sd ->
                val path = Path()
                var first = true
                rows.forEach { r ->
                    val px = x(r.month)
                    val py = y(GrowthStandards.valueAt(r, sd))
                    if (first) { path.moveTo(px, py); first = false } else path.lineTo(px, py)
                }
                drawPath(
                    path,
                    color = WhoPalette.forSd(sd),
                    style = Stroke(width = if (sd == 0f) dp(1.6f) else dp(1.1f)),
                )
            }
        }

        // SD numbers in the right-hand strip, at the height each curve ends on.
        val last = rows.last()
        SD_CURVES.forEach { sd ->
            val py = y(GrowthStandards.valueAt(last, sd))
            if (py < plotTop - dp(6f) || py > plotBottom + dp(6f)) return@forEach
            scope.drawContext.canvas.nativeCanvas.drawText(
                if (sd > 0) sd.toInt().toString() else sd.toInt().toString(),
                plotRight + dp(3f),
                py + dp(4f),
                paint(10f, WhoPalette.forSd(sd), bold = true),
            )
        }
    }

    /** Axis numbers sit on the blue frame in white, exactly as on the printed sheet. */
    fun drawAxisText(info: GrowthStandards.MetricInfo, yStep: Float, showMonthNumbers: Boolean) = with(scope) {
        val canvas = drawContext.canvas.nativeCanvas
        val small = paint(9f, WhoPalette.frameText, align = android.graphics.Paint.Align.RIGHT)

        // y numbers, left and right, skipping any that would collide.
        val minGap = dp(11f)
        var lastY = Float.MAX_VALUE
        var v = ceil(yMin / yStep) * yStep
        while (v <= yMax + 0.001f) {
            val yy = y(v)
            if (yy in plotTop..plotBottom && abs(yy - lastY) > minGap) {
                canvas.drawText(formatTick(v), whiteLeft - dp(3f), yy + dp(3f), small)
                canvas.drawText(
                    formatTick(v),
                    whiteRight + dp(3f),
                    yy + dp(3f),
                    paint(9f, WhoPalette.frameText),
                )
                lastY = yy
            }
            v += yStep
        }

        // x labels: month number inside each year, then the year marker underneath.
        val centred = paint(8f, WhoPalette.frameText, align = android.graphics.Paint.Align.CENTER)
        val yearPaint = paint(9f, WhoPalette.frameText, bold = true, align = android.graphics.Paint.Align.CENTER)
        val monthY = whiteBottom + dp(9f)
        val yearY = whiteBottom + dp(20f)

        if (showMonthNumbers && band != ChartBand.BIRTH_TO_5) {
            var m = band.from
            while (m <= band.to) {
                val inYear = (m % 12f).toInt()
                if (inYear != 0) {
                    val px = x(m)
                    if (px in plotLeft..plotRight) canvas.drawText(inYear.toString(), px, monthY, centred)
                }
                m += 1f
            }
            canvas.drawText("Months", whiteLeft + dp(2f), monthY, paint(8f, WhoPalette.frameText))
        }

        var yearMonth = band.from
        while (yearMonth <= band.to) {
            if (yearMonth % 12f == 0f) {
                val px = x(yearMonth)
                if (px in plotLeft - dp(10f)..plotRight + dp(10f)) {
                    val label = when (val years = (yearMonth / 12f).toInt()) {
                        0 -> "Birth"
                        1 -> "1 year"
                        else -> "$years years"
                    }
                    canvas.drawText(label, px, yearY, yearPaint)
                }
            }
            yearMonth += 12f
        }

        // Axis titles.
        canvas.save()
        canvas.rotate(-90f, dp(11f), (plotTop + plotBottom) / 2f)
        canvas.drawText(
            info.axisTitle,
            dp(11f),
            (plotTop + plotBottom) / 2f + dp(3f),
            paint(9f, WhoPalette.frameText, align = android.graphics.Paint.Align.CENTER),
        )
        canvas.restore()
    }

    fun drawChild(history: List<GrowthPoint>, progress: Float, markerColor: Color) = with(scope) {
        if (history.isEmpty()) return@with
        val visible = history.filter { it.ageMonths in band.from..band.to }
        if (visible.isEmpty()) return@with

        clipPlot {
            // The trajectory across visits, drawn in as the animation runs.
            if (visible.size > 1) {
                val drawnCount = 1 + ((visible.size - 1) * progress).roundToInt()
                val path = Path()
                visible.take(drawnCount).forEachIndexed { i, p ->
                    val px = x(p.ageMonths)
                    val py = y(p.value)
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                drawPath(
                    path,
                    color = markerColor.copy(alpha = 0.85f),
                    style = Stroke(width = dp(1.8f)),
                )
                visible.take(drawnCount).dropLast(1).forEach { p ->
                    drawCircle(markerColor.copy(alpha = 0.75f), radius = dp(2.5f), center = Offset(x(p.ageMonths), y(p.value)))
                }
            }

            val latest = visible.last()
            // A single measurement animates up from the median so the caregiver sees where
            // it lands relative to "normal" rather than a dot appearing from nowhere.
            val px = x(latest.ageMonths)
            val py = y(latest.value)

            drawLine(
                color = markerColor.copy(alpha = 0.45f),
                start = Offset(px, py),
                end = Offset(px, plotBottom),
                strokeWidth = dp(1f),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dp(3f), dp(3f))),
            )
            drawCircle(markerColor.copy(alpha = 0.20f * progress), radius = dp(9f) * progress, center = Offset(px, py))
            drawCircle(markerColor, radius = dp(4.5f), center = Offset(px, py))
            drawCircle(Color.White, radius = dp(1.8f), center = Offset(px, py))
        }
    }

    private inline fun DrawScope.clipPlot(block: () -> Unit) {
        clipRect(plotLeft, plotTop, plotRight, plotBottom) { block() }
    }

    private fun formatTick(v: Float): String =
        if (abs(v - v.roundToInt()) < 0.05f) v.roundToInt().toString() else "%.1f".format(v)
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).roundToInt(),
    (red * 255).roundToInt(),
    (green * 255).roundToInt(),
    (blue * 255).roundToInt(),
)

private inline fun DrawScope.clipRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    block: DrawScope.() -> Unit,
) {
    drawContext.canvas.save()
    drawContext.canvas.clipRect(left, top, right, bottom)
    block()
    drawContext.canvas.restore()
}

// ---------------------------------------------------------------------------
// Cards
// ---------------------------------------------------------------------------

@Composable
fun zColor(z: Float?): Color {
    val c = com.teamposhan.biomed.ui.theme.riskColors
    return when {
        z == null -> MaterialTheme.colorScheme.outline
        z < -3f || z > 3f -> c.priority
        z < -2f || z > 2f -> c.review
        z < -1f -> c.monitor
        else -> c.low
    }
}

/**
 * The chart as it appears in the report: a compact WHO sheet that opens full screen on
 * tap, and jumps straight to the reading explanation on long-press.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GrowthChartCard(
    metric: GrowthStandards.Metric,
    sex: Sex,
    ageMonths: Float,
    history: List<GrowthPoint>,
    onOpen: () -> Unit,
    onOpenInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberAppHaptics()
    val band = remember(ageMonths) { ChartBand.forAge(ageMonths) }
    val info = remember(metric, ageMonths) { GrowthStandards.info(metric, ageMonths) }
    val latest = history.lastOrNull()
    val z = latest?.let { GrowthStandards.zFor(metric, sex, it.ageMonths, it.value) }
    val accent = zColor(z)

    PoshanCard(
        modifier = modifier.combinedClickable(
            onClick = { haptics.tick(); onOpen() },
            onLongClick = { haptics.confirm(); onOpenInfo() },
        ),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(info.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${band.label} · z-scores",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (z != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(9.dp)) { drawCircle(accent) }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "%+.1f SD".format(z),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        WhoGrowthChart(
            metric = metric,
            sex = sex,
            band = band,
            history = history,
            modifier = Modifier
                .fillMaxWidth()
                .height(215.dp)
                .clip(RoundedCornerShape(10.dp)),
            showMonthNumbers = band != ChartBand.BIRTH_TO_5,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (latest == null) {
                "No measurement recorded yet."
            } else {
                "Measured %.${info.decimals}f %s · tap to open the full chart".format(latest.value, info.unit)
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The full-screen chart with pinch-zoom and pan, used by the detail screen.
 */
@Composable
fun ZoomableGrowthChart(
    metric: GrowthStandards.Metric,
    sex: Sex,
    band: ChartBand,
    history: List<GrowthPoint>,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(metric, band) {
                detectTransformGestures { _, drag, gestureZoom, _ ->
                    val next = (scale * gestureZoom).coerceIn(1f, 4f)
                    // Panning is only meaningful while zoomed in; snap back at 1x.
                    scale = next
                    offset = if (next <= 1.001f) {
                        Offset.Zero
                    } else {
                        val limit = size.width * (next - 1f)
                        Offset(
                            (offset.x + drag.x).coerceIn(-limit, limit),
                            (offset.y + drag.y).coerceIn(-limit, limit),
                        )
                    }
                }
            },
    ) {
        WhoGrowthChart(
            metric = metric,
            sex = sex,
            band = band,
            history = history,
            modifier = Modifier.fillMaxWidth().height(340.dp),
            showMonthNumbers = true,
            zoom = scale,
            pan = offset,
            probeEnabled = true,
        )
    }
}

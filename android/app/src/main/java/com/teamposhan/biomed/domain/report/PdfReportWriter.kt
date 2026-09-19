package com.teamposhan.biomed.domain.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.teamposhan.biomed.data.model.AssessmentSession
import com.teamposhan.biomed.data.model.ChildProfile
import com.teamposhan.biomed.data.model.ResponseType
import com.teamposhan.biomed.domain.age.AgeCalculator
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Renders the full assessment as a PDF, question by question.
 *
 * ## Why a PDF and not the text report
 *
 * The text report is a summary meant to be pasted into a message. What a referral actually
 * needs is the evidence: every question put to the caregiver and what they answered. That is
 * hundreds of lines, it has structure, and it has to survive being printed and carried to a
 * clinic where the app does not exist.
 *
 * ## Layout
 *
 * A4 at 72 dpi (595 x 842 points), laid out as a single downward cursor with automatic page
 * breaks. Deliberately plain: no images, no colour fills, nothing that costs ink or fails on
 * a cheap printer. The only colour is a mark against flagged answers, and that is also
 * carried by a text marker so it survives greyscale printing.
 */
object PdfReportWriter {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 40f
    private const val BOTTOM_LIMIT = PAGE_H - 48f

    private val title = Paint().apply {
        color = Color.BLACK; textSize = 20f; isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val heading = Paint().apply {
        color = Color.BLACK; textSize = 13f; isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val body = Paint().apply {
        color = Color.DKGRAY; textSize = 10.5f; isAntiAlias = true
    }
    private val bodyDark = Paint().apply {
        color = Color.BLACK; textSize = 10.5f; isAntiAlias = true
    }
    private val small = Paint().apply {
        color = Color.GRAY; textSize = 8.5f; isAntiAlias = true
    }
    private val flagged = Paint().apply {
        color = Color.rgb(176, 58, 46); textSize = 10.5f; isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val rule = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.6f }

    /** Cursor over a growing document, which starts a new page when it runs out of room. */
    private class Cursor(private val doc: PdfDocument) {
        var page: PdfDocument.Page = newPage(1)
        var canvas: Canvas = page.canvas
        var y = MARGIN + 10f
        private var pageNo = 1

        private fun newPage(n: Int): PdfDocument.Page =
            doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, n).create())

        fun ensure(space: Float) {
            if (y + space <= BOTTOM_LIMIT) return
            finishPage()
            pageNo++
            page = newPage(pageNo)
            canvas = page.canvas
            y = MARGIN + 10f
        }

        fun finishPage() {
            canvas.drawText("Page $pageNo", PAGE_W - MARGIN - 40f, PAGE_H - 28f, small)
            doc.finishPage(page)
        }

        fun gap(h: Float) { y += h }
    }

    /**
     * Draws wrapped text and returns the height used.
     *
     * Word wrapping is done here rather than with StaticLayout because the text is plain and
     * a StaticLayout per line would allocate heavily across a report this long.
     */
    private fun Cursor.text(s: String, paint: Paint, x: Float, maxW: Float, lineH: Float = 14f) {
        var remaining = s
        while (remaining.isNotEmpty()) {
            val count = paint.breakText(remaining, true, maxW, null)
            var cut = count
            if (cut < remaining.length) {
                val lastSpace = remaining.lastIndexOf(' ', cut)
                if (lastSpace > 0) cut = lastSpace
            }
            ensure(lineH)
            canvas.drawText(remaining.substring(0, cut).trim(), x, y, paint)
            y += lineH
            remaining = remaining.substring(cut).trimStart()
        }
    }

    /**
     * Writes the report and returns the file.
     *
     * Written into `cacheDir/reports`, which is what the FileProvider exposes. Cache rather
     * than files: a shared PDF is a transient artefact, and the child's record already lives
     * in the store.
     */
    fun write(
        context: Context,
        child: ChildProfile,
        session: AssessmentSession?,
        history: List<AssessmentSession>,
    ): File {
        val doc = PdfDocument()
        val c = Cursor(doc)
        val contentW = PAGE_W - MARGIN * 2

        // ---- header -------------------------------------------------------
        c.ensure(30f)
        c.canvas.drawText("Child screening report", MARGIN, c.y, title)
        c.y += 22f
        c.text("Poshan — offline child health screening", small, MARGIN, contentW, 12f)
        c.gap(6f)
        c.canvas.drawLine(MARGIN, c.y, PAGE_W - MARGIN, c.y, rule)
        c.gap(16f)

        val age = session?.ageMonthsAtAssessment ?: AgeCalculator.assessmentMonths(child)
        listOf(
            "Name" to child.name,
            "Age at assessment" to AgeCalculator.format(age),
            "Sex" to child.sex.name.lowercase().replaceFirstChar { it.uppercase() },
            "Date of birth" to java.time.LocalDate.ofEpochDay(child.dateOfBirthEpochDay).toString(),
            "Report generated" to DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
                .withZone(ZoneId.systemDefault()).format(Instant.now()),
        ).forEach { (k, v) ->
            c.ensure(14f)
            c.canvas.drawText(k, MARGIN, c.y, small)
            c.canvas.drawText(v, MARGIN + 130f, c.y, bodyDark)
            c.y += 14f
        }
        c.gap(10f)

        if (session == null) {
            c.text("No assessment has been recorded for this child yet.", body, MARGIN, contentW)
            c.finishPage()
            return save(context, doc, child)
        }

        // ---- the transcripts ----------------------------------------------
        val blocks = listOf(
            "Everyday skills" to AnswerTranscript.milestones(session, age),
            "Feeding" to AnswerTranscript.nutrition(session, age),
            "Looking and attention" to AnswerTranscript.markers(session, age),
            "Sunlight and vitamin D" to AnswerTranscript.vitaminD(session, age),
        )

        blocks.forEach { (moduleTitle, sections) ->
            if (sections.isEmpty()) return@forEach
            c.ensure(34f)
            c.gap(8f)
            c.canvas.drawText(moduleTitle, MARGIN, c.y, heading)
            c.y += 6f
            c.canvas.drawLine(MARGIN, c.y, PAGE_W - MARGIN, c.y, rule)
            c.y += 14f

            sections.forEach { section ->
                c.ensure(20f)
                c.canvas.drawText(section.title, MARGIN, c.y, bodyDark)
                val tally = buildList {
                    add("${section.answered} answered")
                    if (section.flagged > 0) add("${section.flagged} to watch")
                    if (section.notSure > 0) add("${section.notSure} not sure")
                }.joinToString(" · ")
                c.canvas.drawText(tally, MARGIN + 300f, c.y, small)
                c.y += 14f

                section.entries.forEach { entry ->
                    // "!" marker as well as colour, so a greyscale print still shows which
                    // answers matter. A report that only works in colour does not work.
                    val marker = if (entry.flagged) "!" else " "
                    val paint = if (entry.flagged) flagged else body
                    c.ensure(16f)
                    c.canvas.drawText(marker, MARGIN, c.y, paint)
                    val answerX = PAGE_W - MARGIN - 70f
                    c.canvas.drawText(entry.answerLabel, answerX, c.y, paint)
                    // Question text is wrapped inside the column left of the answer.
                    val savedY = c.y
                    c.y = savedY
                    c.text(entry.text, paint, MARGIN + 12f, answerX - MARGIN - 24f, 13f)
                    entry.hint?.let { c.text(it, small, MARGIN + 12f, contentW - 24f, 11f) }
                    c.gap(3f)
                }
                c.gap(8f)
            }
        }

        // ---- the existing text summary, appended verbatim ------------------
        c.ensure(40f)
        c.gap(10f)
        c.canvas.drawText("Summary", MARGIN, c.y, heading)
        c.y += 6f
        c.canvas.drawLine(MARGIN, c.y, PAGE_W - MARGIN, c.y, rule)
        c.y += 14f
        ReportBuilder.build(child, session, history).lines().forEach { line ->
            if (line.isBlank()) c.gap(6f) else c.text(line, body, MARGIN, contentW, 12f)
        }

        c.gap(14f)
        c.text(
            "This is a screening report, not a diagnosis. It cannot name a condition. " +
                "Please discuss it with a health worker or doctor.",
            small, MARGIN, contentW, 11f,
        )

        c.finishPage()
        return save(context, doc, child)
    }

    private fun save(context: Context, doc: PdfDocument, child: ChildProfile): File {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val safeName = child.name.trim().replace(Regex("[^A-Za-z0-9]+"), "_").take(24)
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")
            .withZone(ZoneId.systemDefault()).format(Instant.now())
        val file = File(dir, "poshan-$safeName-$stamp.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }
}

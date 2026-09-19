package com.teamposhan.biomed.domain.milestones

import com.teamposhan.biomed.data.model.AssessmentSession
import com.teamposhan.biomed.data.model.ResponseType

/**
 * What a child has actually answered, organised by the age band each **question** belongs to.
 *
 * ## The problem this solves
 *
 * Screening asks catch-up questions: a child first seen at 11 months is asked the 0–3, 4–6
 * and 7–9 month questions as well as their own band, because a skill missed from an age
 * already passed is the delay signal.
 *
 * But everything downstream filed those answers under the **session's** age band. So an
 * 11-month-old answering thirty questions spanning four bands produced one entry under
 * "10–12 months" and three empty bands above it, on a page whose whole purpose is showing
 * the road from birth to five. The 0–3 band read "Not done yet" even though every 0–3
 * question had just been answered.
 *
 * Coverage is therefore computed from the question's own band, recovered through
 * [MilestoneBank.byId]. No model change: the band was always derivable, just never used.
 */
object BandCoverage {

    /** One age band's state, aggregated across every session a child has ever had. */
    data class Coverage(
        val band: AgeBand,
        val answered: Int,
        val total: Int,
        /** Answers scoring below full credit — "no", "maybe" or "not sure". */
        val concerns: Int,
        /** Answers that were explicitly "not sure", which is its own signal. */
        val notSure: Int,
        /** When any question in this band was last answered. */
        val lastAnsweredAt: Long?,
    ) {
        val complete: Boolean get() = total > 0 && answered >= total
        val started: Boolean get() = answered > 0
        val fraction: Float get() = if (total == 0) 0f else answered.toFloat() / total

        /**
         * Whether this band is worth a second look.
         *
         * Deliberately not a verdict — a band with concerns may simply have been answered
         * for a child too young to have reached those skills. The domain status in
         * [com.teamposhan.biomed.data.model.CategoryResult] is where a judgement belongs;
         * this is a count.
         */
        val hasConcerns: Boolean get() = concerns > 0
    }

    /**
     * Coverage for every band from birth to five, across all of a child's sessions.
     *
     * All bands, not only those reached: the child page shows the whole road, and a band the
     * child is not old enough for must read as upcoming rather than as missing.
     *
     * When the same question was answered in more than one session the **latest** answer
     * wins, because a repeat assessment supersedes rather than duplicates.
     */
    fun forChild(sessions: List<AssessmentSession>): Map<AgeBand, Coverage> {
        // questionId -> the most recent answer to it, across every session.
        val latest = HashMap<String, Pair<ResponseType, Long>>()
        sessions.forEach { session ->
            session.answers.forEach { answer ->
                val response = answer.response ?: return@forEach
                val existing = latest[answer.questionId]
                if (existing == null || answer.updatedAt > existing.second) {
                    latest[answer.questionId] = response to answer.updatedAt
                }
            }
        }

        val byBand = MilestoneBank.all().groupBy { it.band }

        return AgeBand.entries.associateWith { band ->
            val questions = byBand[band].orEmpty()
            var answered = 0
            var concerns = 0
            var notSure = 0
            var last: Long? = null

            questions.forEach { q ->
                val entry = latest[q.id] ?: return@forEach
                answered++
                val (response, at) = entry
                if (response.score < 1f) concerns++
                if (response == ResponseType.NOT_SURE) notSure++
                if (last == null || at > last!!) last = at
            }

            Coverage(
                band = band,
                answered = answered,
                total = questions.size,
                concerns = concerns,
                notSure = notSure,
                lastAnsweredAt = last,
            )
        }
    }

    /**
     * The bands a child has answered anything in, oldest first.
     *
     * Useful where only the covered bands matter — a report section, say — as opposed to the
     * child page, which needs the full road including what is still ahead.
     */
    fun coveredBands(sessions: List<AssessmentSession>): List<Coverage> =
        forChild(sessions).values.filter { it.started }.sortedBy { it.band.minMonths }
}

package com.teamposhan.biomed.domain.schedule

import com.teamposhan.biomed.data.model.AssessmentSession
import com.teamposhan.biomed.data.model.SessionStatus
import com.teamposhan.biomed.domain.milestones.AgeBand

/**
 * Periodic developmental surveillance schedule.
 *
 * Twelve health-contact points, as used for well-child visits and growth monitoring in
 * India (and aligning with the immunisation contact points in the National Immunisation
 * Schedule): **2, 4, 6, 9, 12, 15, 18, 24, 30, 36, 48 and 60 months**.
 *
 * Each contact is mapped to the milestone question bank band the child is in at that age,
 * so "the assessment needed at 9 months" resolves to a concrete question set. Three bands
 * are visited twice (4–6, 13–18, 25–36) — that is deliberate: surveillance re-checks the
 * previous items and adds the newly emerging ones.
 *
 * A contact becomes due as soon as the child reaches it and stays offered afterwards,
 * because a late screen is far better than no screen. A child first seen at 11 months is
 * shown the earlier contacts as overdue (catch-up screening).
 */
enum class VisitStatus { COMPLETED, IN_PROGRESS, DUE, UPCOMING, OVERDUE }

data class ScheduledVisit(
    /** The contact age in months this assessment is due at. */
    val ageMonths: Int,
    /** Human label, e.g. "9 months", "1 year". */
    val label: String,
    val status: VisitStatus,
    val completedAt: Long? = null,
    val sessionId: String? = null,
    /** Which question-bank band this contact screens. */
    val bandLabel: String,
    /** What the caregiver should focus on at this contact. */
    val focus: String,
)

object AssessmentSchedule {

    /** Every contact point: age in months -> label, question band, and the focus. */
    private data class Contact(val ageMonths: Int, val label: String, val focus: String)

    private val contacts: List<Contact> = listOf(
        Contact(2, "2 months", "Head control, response to sound, first social smile"),
        Contact(4, "4 months", "Head steady, reaching, babbling, smiling back"),
        Contact(6, "6 months", "Rolling, grasping, responding to speech, curiosity"),
        Contact(9, "9 months", "Sitting without support, transferring objects, responding to name"),
        Contact(12, "1 year", "Standing with support, pincer grip, gestures, pointing"),
        Contact(15, "15 months", "Walking, stacking, first meaningful words"),
        Contact(18, "18 months", "Walking alone, spoon use, 10+ words, pretend play"),
        Contact(24, "2 years", "Running, two-word phrases, two-step instructions, peers"),
        Contact(30, "30 months", "Jumping, short sentences, 100+ words, pretend play"),
        Contact(36, "3 years", "Stairs, drawing, 200+ words, turn-taking"),
        Contact(48, "4 years", "Hopping, circle drawing, storytelling, group play"),
        Contact(60, "5 years", "Balance, name and address, cooperative play, self-care"),
    )

    /** Presented to the UI as (ageMonths, label). */
    val visits: List<Pair<Int, String>> = contacts.map { it.ageMonths to it.label }

    fun focusFor(ageMonths: Int): String =
        contacts.firstOrNull { it.ageMonths == ageMonths }?.focus.orEmpty()

    /** Grace period after a contact before it is flagged overdue. */
    private const val GRACE_MONTHS = 1.5f

    fun timeline(ageMonths: Float, sessions: List<AssessmentSession>): List<ScheduledVisit> {
        val completed = sessions
            .filter { it.status == SessionStatus.COMPLETED }
            .sortedBy { it.startedAt }
        val drafts = sessions.filter { it.status == SessionStatus.DRAFT }

        val usedSessions = mutableSetOf<String>()

        return contacts.map { contact ->
            val band = AgeBand.forAge(contact.ageMonths.toFloat())

            // A contact counts as done when a completed session screened this band at or
            // after the contact age (so a 6-month contact is satisfied by a 6-month screen,
            // not by the 4-month one).
            val match = completed
                .filter { it.id !in usedSessions }
                .filter { AgeBand.forAge(it.ageMonthsAtAssessment) == band }
                .firstOrNull { it.ageMonthsAtAssessment >= contact.ageMonths - 0.5f }
            if (match != null) usedSessions += match.id

            val draftInBand = drafts
                .filter { AgeBand.forAge(it.ageMonthsAtAssessment) == band }
                .firstOrNull { it.ageMonthsAtAssessment >= contact.ageMonths - 0.5f }

            val status = when {
                match != null -> VisitStatus.COMPLETED
                draftInBand != null -> VisitStatus.IN_PROGRESS
                ageMonths > contact.ageMonths + GRACE_MONTHS -> VisitStatus.OVERDUE
                ageMonths >= contact.ageMonths - 0.5f -> VisitStatus.DUE
                else -> VisitStatus.UPCOMING
            }

            ScheduledVisit(
                ageMonths = contact.ageMonths,
                label = contact.label,
                status = status,
                completedAt = match?.completedAt ?: match?.startedAt,
                sessionId = match?.id,
                bandLabel = band.label,
                focus = contact.focus,
            )
        }
    }

    /** The contact the caregiver should do next. */
    fun nextActionable(timeline: List<ScheduledVisit>): ScheduledVisit? =
        timeline.firstOrNull { it.status == VisitStatus.OVERDUE }
            ?: timeline.firstOrNull { it.status == VisitStatus.IN_PROGRESS }
            ?: timeline.firstOrNull { it.status == VisitStatus.DUE }

    fun completedCount(timeline: List<ScheduledVisit>): Int =
        timeline.count { it.status == VisitStatus.COMPLETED }
}

package com.teamposhan.biomed.domain.risk

import com.teamposhan.biomed.data.model.ResponseType
import com.teamposhan.biomed.domain.milestones.AgeBand
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

/**
 * Module 3, rebuilt as **prophylaxis + referral**, not risk scoring.
 *
 * ## Why the previous risk score was removed
 *
 * It summed nine caregiver-reported factors into a 0-12 score and split that into four risk
 * bands. Three independent findings make that indefensible:
 *
 * 1. **No validated vitamin D risk questionnaire exists for children under 5 in any
 *    population**, and none exists for South Asian children of any age. The closest
 *    instrument (New Zealand preschoolers) was rejected by its own authors as having
 *    "insufficient predictive ability for use in clinical situations".
 *    Where pediatric questionnaires have been tested against serum 25(OH)D they score
 *    AUC 0.58-0.72 — at or near chance. A sun-exposure questionnaire in a *tropical*
 *    setting produced AUC 0.559, p = 0.172.
 *
 * 2. **IAP 2021 states: "Routine screening of apparently healthy children for vitamin D
 *    deficiency is not recommended."** The 2016 Global Consensus adds: "Population-based
 *    screening with serum 25OHD, serum ALP, or radiographs is not indicated." Screening
 *    every 0-5 child was contrary to the national paediatric guideline.
 *
 * 3. **The bands drove no differential action.** Every infant 0-12 months should receive
 *    400 IU/day regardless of risk. A "low risk" band telling a caregiver their
 *    exclusively-breastfed four-month-old does not need a supplement was actively wrong.
 *
 * And the confirmatory test is not reachable: serum 25(OH)D appears once in ICMR's National
 * Essential Diagnostics List 2025 — Sub-District/District Hospital only, absent from village,
 * sub-centre, PHC and CHC. A private test costs over Rs 1,500 against an ICDS food budget of
 * Rs 8 per child per day.
 *
 * ## What replaced it
 *
 * The measured gap is **delivery**, not identification. In a Delhi cohort of 330 infants all
 * prescribed vitamin D at birth, only 41.5% ever received it and **only 8.8% received it
 * correctly** — because only 35% of mothers were given any verbal explanation. That is what
 * an app can fix.
 *
 * So: a universal prophylaxis prompt, a binary clinical-sign referral aligned to India's own
 * RBSK screening item, and targeted counselling. No score, no risk tier, no blood-test prompt.
 */

// ---------------------------------------------------------------------------
// A. Universal prophylaxis
// ---------------------------------------------------------------------------

/**
 * The recommended daily dose for a child of this age.
 *
 * Follows IAP 2021, which is India-specific, is what RBSK aligns with, and is the
 * conservative option. Note the 2025 Indian endocrinology consensus recommends 600 IU for
 * ages 1-3 where IAP recommends none routinely; that disagreement is flagged for the
 * clinical advisor rather than silently resolved here.
 */
data class ProphylaxisAdvice(
    val dailyIu: Int?,
    val headline: String,
    val detail: String,
    /** True when the recommendation applies to every child of this age, no assessment needed. */
    val universal: Boolean,
) {
    companion object {
        /** IAP's ceiling for routine supplementation. Above this, toxicity becomes a risk. */
        const val MAX_SAFE_DAILY_IU = 400

        fun forAge(ageMonths: Float): ProphylaxisAdvice = when {
            ageMonths < 12f -> ProphylaxisAdvice(
                dailyIu = 400,
                // No dose figure on screen. The number belongs on the bottle and in the
                // health worker's hands, not in an app a parent reads unsupervised —
                // "400 IU" invites someone to work out their own equivalent from whatever
                // preparation they have, and the preparations differ by an order of
                // magnitude. [dailyIu] is kept for the record and the clinician-facing
                // report; the caregiver is told to give the daily drops, not a number.
                headline = "Vitamin D drops every day",
                detail = "Every baby under one year needs this, whether breastfed or on " +
                    "formula. Breastmilk alone does not carry enough vitamin D — this is " +
                    "not about anything the mother is doing wrong.",
                universal = true,
            )
            else -> ProphylaxisAdvice(
                dailyIu = null,
                headline = "No daily supplement needed",
                detail = "After one year, vitamin D should come from food, fortified foods " +
                    "and sunlight rather than a routine supplement. Calcium matters too — " +
                    "milk, curd, ragi, whole dals and green leafy vegetables.",
                universal = false,
            )
        }
    }
}

/** What the caregiver reported about actually giving the supplement. */
enum class ProphylaxisStatus(val label: String) {
    GIVING_DAILY("Given every day"),
    GIVING_SOMETIMES("Given some days"),
    NOT_GIVING("Not being given"),
    NOT_ASKED("Not asked yet"),
}

/**
 * Follow-up for the adherence answer.
 *
 * The Delhi compliance study found the failure was almost entirely explanation: 65% of
 * mothers received no verbal explanation at discharge and only a third understood why the
 * drops were needed. So a gap here produces counselling, not a risk label.
 */
data class AdherenceOutcome(
    val status: ProphylaxisStatus,
    val needsFollowUp: Boolean,
    val message: String,
) {
    companion object {
        fun evaluate(status: ProphylaxisStatus, advice: ProphylaxisAdvice): AdherenceOutcome {
            if (!advice.universal) {
                return AdherenceOutcome(
                    status, needsFollowUp = false,
                    message = "No daily supplement is needed at this age.",
                )
            }
            return when (status) {
                ProphylaxisStatus.GIVING_DAILY -> AdherenceOutcome(
                    status, false,
                    "Good. Keep going every day until the first birthday.",
                )
                ProphylaxisStatus.GIVING_SOMETIMES -> AdherenceOutcome(
                    status, true,
                    "It works best given every single day. Try linking it to something you " +
                        "already do daily, such as the morning feed.",
                )
                ProphylaxisStatus.NOT_GIVING -> AdherenceOutcome(
                    status, true,
                    "Ask your Anganwadi worker or ASHA for vitamin D drops. It is one drop " +
                        "a day and costs very little.",
                )
                ProphylaxisStatus.NOT_ASKED -> AdherenceOutcome(
                    status, true, "Ask whether the baby is getting daily vitamin D drops.",
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// B. Clinical signs — RBSK item B5
// ---------------------------------------------------------------------------

/**
 * Signs a community worker can be trained to see, taken from India's own RBSK screening
 * card (condition 12, item B5: "Look for Wrist Widening / Bowing of legs / nodular swelling
 * on the chest. If Yes, refer").
 *
 * Deliberately excluded, with reasons — these appear in clinical descriptions of rickets but
 * are not safely assessable by a non-clinician:
 *  - **craniotabes**: present in 22% of entirely normal newborns, and requires skilled
 *    palpation of neonatal cranial sutures
 *  - **frontal bossing**: no published sensitivity or specificity; overlaps normal infant
 *    head shape
 *  - **delayed fontanelle closure**: the threshold is 2 years, which is useless for the
 *    0-2 window where prophylaxis actually matters
 *  - **hypotonia**: needs a neurological examination; no diagnostic performance published
 *
 * Output is refer / do not refer. There is no score, because there is no published
 * sensitivity or specificity for this triad as a screening instrument, and inventing weights
 * for it would repeat the mistake this module was rebuilt to remove.
 */
enum class RicketsSign(
    val id: String,
    val question: String,
    val whatToLookFor: String,
    val minMonths: Float,
) {
    WRIST_WIDENING(
        "rs_wrist",
        "Do the wrists or ankles look swollen or widened?",
        "Compare both wrists. Look for a bony bulge just above the hand, wider than you " +
            "would expect for the child's size.",
        0f,
    ),
    RACHITIC_ROSARY(
        "rs_rosary",
        "Are there small bumps in a row down the front of the chest?",
        "Run your fingers gently down each side of the breastbone. Feel for small bumps " +
            "standing out in a row, like a string of beads.",
        0f,
    ),
    LEG_BOWING(
        "rs_legs",
        "Do the legs curve outwards or inwards when the child stands?",
        "Only for a child who already walks. Look from the front with feet together: a " +
            "wide gap at the knees, or knees touching with a gap at the ankles.",
        12f,
    ),
    CANNOT_WALK(
        "rs_walk",
        "Is the child unable to stand or walk when others of the same age can?",
        "Ask the caregiver to compare with other children in the family or neighbourhood.",
        12f,
    ),
    LEG_PAIN(
        "rs_pain",
        "Does the child complain of leg pain when walking, or tire very easily?",
        "A child who sits out play they used to join in.",
        18f,
    ),
    SEIZURE(
        "rs_seizure",
        "Has the child ever had a fit or unexplained stiffening episode?",
        "Refer straight away. Low calcium from severe vitamin D deficiency can cause fits.",
        0f,
    );

    fun appliesAt(ageMonths: Float): Boolean = ageMonths >= minMonths

    companion object {
        fun forAge(ageMonths: Float): List<RicketsSign> = entries.filter { it.appliesAt(ageMonths) }
    }
}

/**
 * Referral decision.
 *
 * Any single sign refers. That is RBSK's own rule, and the alternative — requiring two or
 * more, or weighting them — would be numbers we cannot source.
 *
 * Expect false positives: community fieldworker screening for visible rickets deformity in
 * The Gambia flagged 3.3% of children, of whom only about 1.5% had radiographically active
 * rickets. That is tolerable **only** because the destination is the existing free
 * RBSK Mobile Health Team to District Early Intervention Centre pathway, where calcium,
 * phosphorus, alkaline phosphatase and X-ray are available at no cost. The app must never
 * send a family to a private laboratory instead.
 */
data class RicketsScreenResult(
    val signsPresent: List<RicketsSign>,
    val urgent: Boolean,
    val refer: Boolean,
) {
    companion object {
        fun evaluate(answers: Map<String, ResponseType>): RicketsScreenResult {
            val present = RicketsSign.entries.filter {
                answers[it.id] == ResponseType.YES_OBSERVED
            }
            return RicketsScreenResult(
                signsPresent = present,
                urgent = present.contains(RicketsSign.SEIZURE),
                refer = present.isNotEmpty(),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// C. Sunlight timing — deterministic astronomy, used as advice only
// ---------------------------------------------------------------------------

/**
 * When the sun is high enough today for the skin to make vitamin D.
 *
 * Solar altitude is computable offline from latitude, date and time — no network, no API.
 * Above roughly 45 degrees of solar altitude, cutaneous synthesis proceeds; below it,
 * effectively none does. The everyday form of that rule: your shadow is shorter than you are.
 *
 * **This produces advice, never a risk input.** Aerosol load and ozone are not available
 * offline, exposed body-surface area is unknowable, skin type is not measurable on the
 * device, and Indian field measurements found schoolchildren given 30 minutes daily at
 * 11:00-12:00 showed *no* rise in serum 25(OH)D during winter because the UV-B index was too
 * low. Sunlight advice must therefore never be presented as a substitute for the supplement.
 */
object SunWindow {

    /** Solar altitude above which meaningful vitamin D synthesis occurs. */
    private const val EFFECTIVE_ALTITUDE_DEGREES = 45.0

    data class Window(val startHour: Int, val startMinute: Int, val endHour: Int, val endMinute: Int) {
        val available: Boolean get() = startHour >= 0

        fun format(): String = if (!available) {
            "The sun does not get high enough today for the skin to make vitamin D."
        } else {
            "%02d:%02d to %02d:%02d".format(startHour, startMinute, endHour, endMinute)
        }

        companion object {
            val NONE = Window(-1, 0, -1, 0)
        }
    }

    /**
     * Solar altitude in degrees, for a latitude and a day of the year at a given local hour.
     *
     * Standard declination and hour-angle formulation. Accurate to well within the precision
     * this advice needs; it is choosing an hour band, not pointing a telescope.
     */
    fun solarAltitude(latitudeDeg: Double, dayOfYear: Int, localHour: Double): Double {
        val declination = 23.45 * sin(Math.toRadians(360.0 * (284 + dayOfYear) / 365.0))
        val hourAngle = 15.0 * (localHour - 12.0)
        val latRad = Math.toRadians(latitudeDeg)
        val decRad = Math.toRadians(declination)
        val haRad = Math.toRadians(hourAngle)
        val sinAltitude = sin(latRad) * sin(decRad) + cos(latRad) * cos(decRad) * cos(haRad)
        return Math.toDegrees(kotlin.math.asin(sinAltitude.coerceIn(-1.0, 1.0)))
    }

    /** The band of local time today during which solar altitude stays above the threshold. */
    fun todayWindow(latitudeDeg: Double, dayOfYear: Int): Window {
        var start = -1.0
        var end = -1.0
        // Quarter-hour steps: finer resolution than the advice can justify.
        var h = 5.0
        while (h <= 19.0) {
            val altitude = solarAltitude(latitudeDeg, dayOfYear, h)
            if (altitude >= EFFECTIVE_ALTITUDE_DEGREES) {
                if (start < 0) start = h
                end = h
            }
            h += 0.25
        }
        if (start < 0) return Window.NONE
        return Window(
            startHour = start.toInt(),
            startMinute = ((start % 1.0) * 60).toInt(),
            endHour = end.toInt(),
            endMinute = ((end % 1.0) * 60).toInt(),
        )
    }

    /**
     * How long, and how much skin. India-specific guidance for Fitzpatrick type IV-V skin,
     * which needs substantially more exposure than lighter skin for the same synthesis.
     */
    const val GUIDANCE = "Around 15 minutes near midday, with the arms and legs uncovered — " +
        "roughly a third of the body. Indian skin needs longer in the sun than lighter skin " +
        "does. Do not let the child burn."

    const val CAVEAT = "Sunlight alone is often not enough, especially in winter and in " +
        "cities with heavy air pollution. For babies under one year the daily drops matter more."
}

// ---------------------------------------------------------------------------
// D. Counselling triggers — the old risk items, repurposed
// ---------------------------------------------------------------------------

/**
 * The factors the old questionnaire scored, kept as **counselling triggers**.
 *
 * They are genuine recognised risk factors — the Global Consensus lists them as such. What
 * they cannot do is sum into a valid score. So each one now fires a specific piece of advice
 * instead of contributing points to a band.
 *
 * Calcium is included, which the old nine items omitted entirely: the serum threshold below
 * which rickets risk rises moves from about 28 to 40 nmol/L depending on dietary calcium,
 * and calcium-deficiency rickets is a major pathway in low-calcium-diet populations.
 */
enum class VitaminDCounselling(
    val id: String,
    val question: String,
    val advice: String,
) {
    BREASTFED_NO_SUPPLEMENT(
        "vd_bf",
        "Is the baby breastfed without daily vitamin D drops?",
        "Breastmilk is the best food for your baby but does not carry enough vitamin D. " +
            "Daily drops are needed alongside it for the whole first year.",
    ),
    LITTLE_SUN(
        "vd_sun",
        "Does the child spend most of the day indoors?",
        "A short spell outside near midday helps. See the sun window on this screen for " +
            "today's timing.",
    ),
    COVERED(
        "vd_cover",
        "Is the child's skin mostly covered when outdoors?",
        "Sunlight has to reach bare skin. Arms and legs uncovered for a short time is enough.",
    ),
    POLLUTION(
        "vd_air",
        "Do you live in a city or an area with heavy smoke or dust?",
        "Haze and smoke block the sunlight that makes vitamin D. Measured levels in polluted " +
            "parts of Delhi were less than half those in cleaner areas nearby.",
    ),
    PREMATURE(
        "vd_preterm",
        "Was the baby born early?",
        "Babies born early start with smaller stores. Daily drops matter even more.",
    ),
    MATERNAL(
        "vd_mother",
        "Does the mother have low vitamin D, or spend little time outdoors?",
        "A mother's vitamin D level closely tracks her baby's at birth. Ask the health " +
            "worker about the mother's own intake.",
    ),
    LOW_CALCIUM(
        "vd_calcium",
        "Does the child rarely eat milk, curd, ragi, dals or green leafy vegetables?",
        "Calcium and vitamin D work together — weak bones can come from too little calcium " +
            "even when vitamin D is adequate. Ragi, curd, whole dals and greens are " +
            "inexpensive sources.",
    ),
    LOW_INTAKE(
        "vd_diet",
        "Does the child rarely eat eggs, fish or fortified foods?",
        "Egg yolk, fish and fortified milk or oil add some vitamin D. They help, but for " +
            "babies they do not replace the drops.",
    );

    companion object {
        fun forAge(ageMonths: Float): List<VitaminDCounselling> = when {
            ageMonths < 12f -> entries.toList()
            // Breastfeeding-without-drops stops being the headline issue after infancy.
            else -> entries.filterNot { it == BREASTFED_NO_SUPPLEMENT || it == PREMATURE }
        }

        /** Advice for every factor the caregiver confirmed. */
        fun triggered(answers: Map<String, ResponseType>, ageMonths: Float): List<VitaminDCounselling> =
            forAge(ageMonths).filter { answers[it.id] == ResponseType.YES_OBSERVED }
    }
}

/**
 * The whole module's outcome.
 *
 * Note what is absent: no score, no band, no probability, no "deficiency risk". The outputs
 * are an action (give the drops), a decision (refer or not) and a set of things to talk about.
 */
data class VitaminDOutcome(
    val advice: ProphylaxisAdvice,
    val adherence: AdherenceOutcome,
    val screen: RicketsScreenResult,
    val counselling: List<VitaminDCounselling>,
) {
    /** True when anything here needs a health worker's attention. */
    val needsAttention: Boolean get() = screen.refer || adherence.needsFollowUp
}

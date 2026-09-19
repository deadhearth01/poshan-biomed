package com.teamposhan.biomed.domain.risk

import com.teamposhan.biomed.data.model.ResponseType
import com.teamposhan.biomed.domain.skin.RiskContribution
import com.teamposhan.biomed.domain.skin.SkinType

/**
 * Risk stratification for vitamin D deficiency.
 *
 * ## What this is
 *
 * A **screening prior**: it estimates how likely this child is to be deficient, by counting
 * established risk factors. It is the same class of instrument as a cardiovascular risk score
 * — a way of deciding who warrants attention first, built from factors whose association
 * with the outcome is documented.
 *
 * ## What this is not
 *
 * Not a measurement, not a diagnosis, and not a serum concentration. There is at present **no
 * validated non-invasive biomarker for vitamin D status** — nothing optical, and nothing in
 * sweat, saliva or breath that has cleared validation. Any tool in this space is necessarily
 * doing risk stratification rather than measurement, and saying so is not a weakness of this
 * design; it is the honest description of the whole field today.
 *
 * ## Stated assumptions — this is a proof of concept
 *
 * These are assumptions, deliberately made so the concept can be demonstrated end to end.
 * Each is listed so a reviewer can attack it directly rather than having to reverse-engineer
 * it from the arithmetic:
 *
 *  1. **Factors are additive, with one exception.** Dark skin combined with little sun
 *     outdoors gets an explicit interaction term, because required exposure rises at the
 *     same time as available exposure falls and the two compound. Every other pair is
 *     assumed independent, which is certainly wrong in detail.
 *  2. **Weights are ordinal, not fitted.** They encode direction and rough relative magnitude
 *     as reported in the literature. They are **not** regression coefficients from a cohort,
 *     because no such cohort exists for under-fives in this setting.
 *  3. **Bands are relative tiers, not calibrated probabilities.** "High" means this child
 *     carries most of the risk factors that could apply to them, not "X% chance of
 *     deficiency". Calibration would need paired serum data that does not exist for this
 *     population.
 *  4. **Skin type is treated as constitutive.** The reading is taken on the inner forearm
 *     precisely to approximate unexposed baseline pigmentation rather than tan.
 *  5. **Self-reported exposure is taken at face value.** Recall bias on "time outdoors" is
 *     well known and unmeasured here.
 *
 * ## Designed to be superseded
 *
 * Contributions are a list, and [assess] accepts [extra] contributions from outside. When a
 * further model arrives — a second optical channel, a fitted coefficient set, an actual
 * biomarker — it adds its contributions and the banding, the UI and the explanation screen
 * pick it up without modification. That is why nothing here hard-codes the factor set.
 */
object VitaminDRiskModel {

    /**
     * Risk tier.
     *
     * Three tiers because the action space has three entries: carry on, change something, get
     * seen. A finer scale would imply a precision the inputs do not support.
     */
    enum class Tier(val label: String, val plain: String) {
        LOW(
            "Low",
            "Few of the things that lead to low vitamin D apply to this child.",
        ),
        MODERATE(
            "Moderate",
            "Some of the things that lead to low vitamin D apply here. Worth changing what " +
                "you can.",
        ),
        HIGH(
            "High",
            "Several things that lead to low vitamin D apply at once. Worth raising at the " +
                "next health visit.",
        );
    }

    /**
     * Tier boundaries, as a **fraction of the worst score this child could have scored**.
     *
     * Absolute point thresholds were tried first and failed twice over, which is worth
     * recording because both failures are easy to reintroduce:
     *
     *  - **The top tier saturated.** Realistic urban cases scored 13 to 23 against a
     *    threshold of 6, so everything above the line was indistinguishable. In a population
     *    where indoor living and darker skin are the norm, nearly every child would have
     *    been High — the same band-compression failure that makes a three-band ITA scheme
     *    useless here.
     *  - **Skin type alone reached Moderate.** A rural dark-skinned infant, outdoors daily
     *    and reliably taking the drops, scored 4 on pigmentation alone. That child is doing
     *    everything available to them; flagging them tells the health worker nothing
     *    actionable and tells the parent something untrue.
     *
     * Scoring against each child's own achievable maximum fixes both. It also keeps an
     * infant and a four-year-old comparable, since the factors that apply to them differ.
     */
    private const val MODERATE_FRACTION = 0.30f
    private const val HIGH_FRACTION = 0.55f

    data class Result(
        val tier: Tier,
        val points: Int,
        val maxPoints: Int,
        /** Score as a share of this child's own achievable worst case. */
        val fraction: Float,
        val skinType: SkinType?,
        val contributions: List<RiskContribution>,
    ) {
        /** Only the factors that actually fired. */
        val present: List<RiskContribution> get() = contributions.filter { it.points > 0 }

        /**
         * True when no skin reading was taken.
         *
         * Surfaced because skin type is the largest single contributor, so a result computed
         * without it is systematically softer than one computed with it, and a caregiver
         * should not read the two as equivalent.
         */
        val skinTypeMissing: Boolean get() = skinType == null

        /** The sentence that must travel with the tier wherever it is shown. */
        val subject: String get() = "Risk of low vitamin D"

        val disclaimer: String
            get() = "This counts the things that make low vitamin D more likely. It is not a " +
                "blood test and it cannot say how much vitamin D this child has — no phone " +
                "can. Only a blood test measures that."
    }

    /**
     * Scores one child.
     *
     * @param skinType from the ITA reading, or null when no reading was taken
     * @param answers the module's counselling answers
     * @param ageMonths drives which factors apply at all
     * @param prophylaxis whether the daily drops are actually being given
     * @param sunWindowAvailable whether the sun reaches an effective altitude at this
     *   latitude and date; null when location is unknown
     * @param extra contributions from any model added later
     */
    fun assess(
        skinType: SkinType?,
        answers: Map<String, ResponseType>,
        ageMonths: Float,
        prophylaxis: ProphylaxisStatus,
        sunWindowAvailable: Boolean?,
        extra: List<RiskContribution> = emptyList(),
    ): Result {
        val yes = { id: String -> answers[id] == ResponseType.YES_OBSERVED }
        val out = mutableListOf<RiskContribution>()

        // --- 1. Skin type ----------------------------------------------------
        // The largest single contributor, and the only one this app measures rather than
        // asks about.
        skinType?.let {
            out += RiskContribution(
                id = "skin_type",
                label = "Skin type ${it.roman} (${it.itaLabel})",
                points = it.riskPoints,
                plain = "Darker skin needs longer in the sun to make the same vitamin D. " +
                    "That is the skin protecting itself, not a problem with the child.",
                evidence = "Melanin competitively absorbs UV-B over the same 290-315 nm band " +
                    "that drives previtamin D3 synthesis. ITA-to-phototype mapping: Chardon " +
                    "1991; Del Bino & Bernerd 2013.",
            )
        }

        // --- 2. Exposure -----------------------------------------------------
        val indoors = yes(VitaminDCounselling.LITTLE_SUN.id)
        val covered = yes(VitaminDCounselling.COVERED.id)
        val polluted = yes(VitaminDCounselling.POLLUTION.id)

        if (indoors) {
            out += RiskContribution(
                "indoors", "Mostly indoors", 2,
                "Sunlight on bare skin is how the body makes most of its vitamin D.",
                "Consistently the strongest modifiable predictor in paediatric cohorts.",
            )
        }
        if (covered) {
            out += RiskContribution(
                "covered", "Skin covered outdoors", 2,
                "Sunlight has to reach bare skin. Arms and legs uncovered for a short " +
                    "while is enough.",
                "Clothing coverage is an established independent predictor of low 25(OH)D.",
            )
        }
        if (polluted) {
            out += RiskContribution(
                "pollution", "Heavy smoke, dust or haze", 1,
                "Haze and smoke block the part of sunlight that makes vitamin D.",
                "Children in polluted areas of Delhi showed roughly half the 25(OH)D of " +
                    "those in cleaner areas nearby.",
            )
        }
        if (sunWindowAvailable == false) {
            out += RiskContribution(
                "no_uv_window", "Sun too low this time of year", 2,
                "Where you are, the sun does not climb high enough right now for skin to " +
                    "make vitamin D, whatever the weather looks like.",
                "Below roughly 45 degrees solar altitude, atmospheric path length removes " +
                    "effectively all UV-B.",
            )
        }

        // Interaction term, and the only one. Dark skin indoors is materially worse than the
        // additive model gives it credit for, because the required exposure rises at the same
        // time as the available exposure falls.
        val darkSkin = skinType == SkinType.V || skinType == SkinType.VI
        if (darkSkin && (indoors || covered)) {
            out += RiskContribution(
                "skin_exposure_interaction", "Darker skin and little sun together", 2,
                "This child needs more time in the sun than most, and is getting less. The " +
                    "two together matter more than either on its own.",
                "Required exposure scales with melanin while available exposure falls; the " +
                    "effects compound rather than add.",
            )
        }

        // --- 3. Supplementation ---------------------------------------------
        val dropsApply = ProphylaxisAdvice.forAge(ageMonths).universal
        if (dropsApply) {
            when (prophylaxis) {
                ProphylaxisStatus.NOT_GIVING -> out += RiskContribution(
                    "no_drops", "Daily drops not started", 3,
                    "Every baby under one year needs daily vitamin D drops, breastfed or not.",
                    "IAP 2021; Global Consensus on Nutritional Rickets 2016. 400 IU/day, " +
                        "birth to twelve months, regardless of feeding.",
                )
                ProphylaxisStatus.GIVING_SOMETIMES -> out += RiskContribution(
                    "partial_drops", "Drops missed some days", 1,
                    "The drops work by being daily. Missed days add up.",
                    "Adherence to prescribed paediatric supplementation is low in practice; " +
                        "correct delivery has been reported below 10%.",
                )
                else -> Unit
            }
        }

        if (yes(VitaminDCounselling.BREASTFED_NO_SUPPLEMENT.id)) {
            out += RiskContribution(
                "bf_no_supp", "Breastfed without drops", 2,
                "Breastmilk is the best food for a baby but carries very little vitamin D. " +
                    "That is true of every mother and is not a sign anything is wrong.",
                "Unsupplemented human milk supplies roughly 25 IU/L against a 400 IU/day " +
                    "requirement.",
            )
        }

        // --- 4. Background ---------------------------------------------------
        if (yes(VitaminDCounselling.PREMATURE.id)) {
            out += RiskContribution(
                "preterm", "Born early", 1,
                "Babies born early start with smaller stores.",
                "Third-trimester transfer is when most neonatal stores accumulate.",
            )
        }
        if (yes(VitaminDCounselling.MATERNAL.id)) {
            out += RiskContribution(
                "maternal", "Mother likely low too", 2,
                "A baby's vitamin D at birth closely follows the mother's.",
                "Cord blood 25(OH)D tracks maternal status closely; maternal deficiency is " +
                    "highly prevalent in Indian cohorts.",
            )
        }
        if (yes(VitaminDCounselling.LOW_CALCIUM.id)) {
            out += RiskContribution(
                "low_calcium", "Little calcium in the diet", 1,
                "Calcium and vitamin D work together. Ragi, curd, whole dals and greens are " +
                    "inexpensive sources.",
                "Low dietary calcium causes rickets even where vitamin D is adequate, and " +
                    "accelerates its catabolism.",
            )
        }
        if (yes(VitaminDCounselling.LOW_INTAKE.id)) {
            out += RiskContribution(
                "low_intake", "Little vitamin D in the diet", 1,
                "Egg yolk, fish and fortified milk or oil add some. For babies they do not " +
                    "replace the drops.",
                "Dietary vitamin D is a minor contributor in unfortified diets.",
            )
        }

        out += extra

        val points = out.sumOf { it.points }
        // The denominator moves with which factors applied to this child, so an infant and a
        // four-year-old are not scored against the same ceiling.
        val maxPoints = maxPointsFor(ageMonths, skinType != null) + extra.sumOf { it.points }

        val fraction = if (maxPoints <= 0) 0f else points.toFloat() / maxPoints
        return Result(
            tier = when {
                fraction >= HIGH_FRACTION -> Tier.HIGH
                fraction >= MODERATE_FRACTION -> Tier.MODERATE
                else -> Tier.LOW
            },
            points = points,
            maxPoints = maxPoints,
            fraction = fraction,
            skinType = skinType,
            contributions = out,
        )
    }

    /** The worst achievable score for a child of this age, for context on the scale. */
    private fun maxPointsFor(ageMonths: Float, hasSkinType: Boolean): Int {
        var max = 0
        if (hasSkinType) max += SkinType.VI.riskPoints + 2 // type + interaction
        max += 2 + 2 + 1 + 2                               // indoors, covered, pollution, no window
        if (ProphylaxisAdvice.forAge(ageMonths).universal) max += 3 + 2 // drops, breastfed
        max += 1 + 2 + 1 + 1                               // preterm, maternal, calcium, intake
        return max
    }
}

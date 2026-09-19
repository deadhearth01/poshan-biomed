package com.teamposhan.biomed.domain.skin

/**
 * Skin phototyping from the Individual Typology Angle.
 *
 * This is what ITA° is actually validated for. Del Bino & Bernerd (2013) measured ITA°
 * against extracted melanin content across the full range of human skin and found a strong
 * monotonic relationship; Chardon, Cretois & Hourseau (1991) defined the angle and its
 * category boundaries for exactly this purpose. Used as a **classifier of skin type**, ITA°
 * is on solid published ground — quite unlike using it to estimate a serum concentration.
 *
 * The boundaries below are the published ones, unmodified. They are not tuned, not
 * re-derived, and not collapsed into fewer bands.
 *
 * ## Why skin type belongs in a vitamin D risk model
 *
 * Melanin is a competitive absorber of UV-B in the epidermis, over the same 290-315 nm band
 * that converts 7-dehydrocholesterol to previtamin D3. More melanin means a longer exposure
 * for the same cutaneous synthesis. This is photochemistry, not correlation, and it is the
 * mechanism behind the consistently higher deficiency prevalence reported in darker-skinned
 * populations at a given latitude and exposure.
 *
 * So skin type is a genuine, mechanistically grounded **risk factor**. It is not a
 * measurement of vitamin D, and this file never treats it as one.
 */
enum class SkinType(
    val roman: String,
    val itaLabel: String,
    /**
     * Approximate multiple of the exposure time a Type II skin needs for equivalent
     * synthesis. Ordinal, not a calibrated constant — see [VitaminDRiskModel] on assumptions.
     */
    val exposureMultiplier: Float,
    /** Points contributed to the deficiency-risk score. */
    val riskPoints: Int,
    val plain: String,
) {
    I("I", "Very light", 0.7f, 0, "Burns easily, rarely tans"),
    II("II", "Light", 1.0f, 0, "Burns easily, tans a little"),
    III("III", "Intermediate", 1.5f, 1, "Sometimes burns, tans gradually"),
    IV("IV", "Tan", 2.0f, 2, "Rarely burns, tans easily"),
    V("V", "Brown", 3.0f, 3, "Very rarely burns, tans deeply"),
    VI("VI", "Dark", 4.0f, 4, "Never burns, deeply pigmented");

    companion object {
        /**
         * Published ITA° boundaries (Chardon 1991; the six-category scheme used throughout
         * the dermatology and cosmetic-science literature).
         *
         * Returns null for a NaN angle so an unusable reading cannot silently become Type I.
         */
        fun fromIta(itaDegrees: Double?): SkinType? {
            val ita = itaDegrees?.takeIf { !it.isNaN() } ?: return null
            return when {
                ita > 55 -> I
                ita > 41 -> II
                ita > 28 -> III
                ita > 10 -> IV
                ita > -30 -> V
                else -> VI
            }
        }

        /**
         * How far the reading sits from the nearest category edge, in degrees.
         *
         * Reported because the measurement's own error budget is roughly 1.5 to 2.6 units of
         * L*, which is 3 to 7 degrees of ITA. A reading two degrees from a boundary should
         * not be presented with the same confidence as one sitting mid-band, and the UI says
         * so rather than hiding it.
         */
        fun marginToEdge(itaDegrees: Double): Double {
            val edges = listOf(55.0, 41.0, 28.0, 10.0, -30.0)
            return edges.minOf { kotlin.math.abs(itaDegrees - it) }
        }

        /** Below this many degrees from a boundary, the type assignment is called borderline. */
        const val BORDERLINE_MARGIN = 4.0
    }
}

/**
 * A single contributing factor in the deficiency-risk model.
 *
 * Carried as data rather than folded into a number so the UI can always show *why* a child
 * scored what they scored, and so a future model can add its own contributions without this
 * file knowing anything about it.
 */
data class RiskContribution(
    val id: String,
    val label: String,
    val points: Int,
    /** What this factor is, in the caregiver's words. */
    val plain: String,
    /** Where the effect direction comes from. */
    val evidence: String,
    /** Set by models added later; the built-in factors leave it null. */
    val source: String? = null,
)

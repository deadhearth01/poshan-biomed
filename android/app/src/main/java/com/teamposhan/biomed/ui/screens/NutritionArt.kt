package com.teamposhan.biomed.ui.screens

import androidx.annotation.DrawableRes
import com.teamposhan.biomed.R

/**
 * Artwork for the nutrition questions.
 *
 * Unlike [MilestoneArt], this mapping is mechanical: the drawable ids were generated from
 * the question ids in the first place (`NUT_M0_3_1` → `nut_m0_3_1`), so lowercasing the id
 * is a correct derivation rather than a guess. The lookup is still an explicit table so a
 * missing asset shows up as a null here instead of a crash at resource resolution.
 *
 * Artwork exists for 0–24 months. Later bands fall back to the section illustration, which
 * is a deliberate blank rather than a wrong picture: for most caregivers the image is what
 * actually conveys the question, so the wrong one is worse than none.
 */
object NutritionArt {

    private val art: Map<String, Int> = mapOf(
        // 0-3 months
        "NUT_M0_3_1" to R.drawable.nut_m0_3_1,
        "NUT_M0_3_2" to R.drawable.nut_m0_3_2,
        "NUT_M0_3_3" to R.drawable.nut_m0_3_3,
        "NUT_M0_3_4" to R.drawable.nut_m0_3_4,
        // 4-6 months
        "NUT_M4_6_1" to R.drawable.nut_m4_6_1,
        "NUT_M4_6_2" to R.drawable.nut_m4_6_2,
        "NUT_M4_6_3" to R.drawable.nut_m4_6_3,
        "NUT_M4_6_4" to R.drawable.nut_m4_6_4,
        // 7-9 months: complementary feeding begins
        "NUT_M7_9_1" to R.drawable.nut_m7_9_1,
        "NUT_M7_9_2" to R.drawable.nut_m7_9_2,
        "NUT_M7_9_3" to R.drawable.nut_m7_9_3,
        "NUT_M7_9_4" to R.drawable.nut_m7_9_4,
        // 10-12 months
        "NUT_M10_12_1" to R.drawable.nut_m10_12_1,
        "NUT_M10_12_2" to R.drawable.nut_m10_12_2,
        "NUT_M10_12_3" to R.drawable.nut_m10_12_3,
        "NUT_M10_12_4" to R.drawable.nut_m10_12_4,
        // 13-18 months
        "NUT_M13_18_1" to R.drawable.nut_m13_18_1,
        "NUT_M13_18_2" to R.drawable.nut_m13_18_2,
        "NUT_M13_18_3" to R.drawable.nut_m13_18_3,
        "NUT_M13_18_4" to R.drawable.nut_m13_18_4,
        // 19-24 months
        "NUT_M19_24_1" to R.drawable.nut_m19_24_1,
        "NUT_M19_24_2" to R.drawable.nut_m19_24_2,
        "NUT_M19_24_3" to R.drawable.nut_m19_24_3,
        "NUT_M19_24_4" to R.drawable.nut_m19_24_4,
    )

    /** Artwork for this question, or null when none has been generated yet. */
    @DrawableRes
    fun forQuestion(questionId: String): Int? = art[questionId]

    /** How many of the feeding-practice questions currently have their own artwork. */
    val mappedCount: Int get() = art.size
}

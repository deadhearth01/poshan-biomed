package com.teamposhan.biomed.ui.components

/**
 * Maps a logical artwork slot to the drawables that can fill it, best first.
 *
 * Artwork arrives in batches and rarely under the exact names the code asked for: a set
 * generated as `icon3d_eye_glow` is the right picture for the slot the code calls
 * `micro_module_attention`. Renaming the delivered files would work once and then break the
 * next time that batch is regenerated, because the generator still produces its own names.
 *
 * So the resolution order is: **the requested name first, then the aliases.** A batch
 * delivered under the exact slot name always wins, and today's near-enough art fills the gap
 * until then. Nothing has to be renamed, re-wired, or regenerated to take the upgrade.
 */
object ArtAliases {

    private val aliases: Map<String, List<String>> = mapOf(
        // --- module identity -------------------------------------------------
        "micro_module_milestones" to listOf("icon3d_magnify_footprint", "icon3d_growth_ribbon"),
        "micro_module_attention" to listOf("icon3d_eye_glow"),
        "micro_module_vitamind" to listOf("icon3d_sun_droplet"),

        // --- sections --------------------------------------------------------
        "micro_growth_chart" to listOf("icon3d_growth_ribbon", "icon3d_weighing_scale"),
        "micro_immunization" to listOf("icon3d_vaccine_shield"),
        "micro_report" to listOf("icon3d_heart_check"),
        "micro_sun_window" to listOf("icon3d_sun_droplet"),
        "micro_how_it_works" to listOf("icon3d_magnify_footprint"),

        // --- status ----------------------------------------------------------
        "micro_on_track" to listOf("icon3d_heart_check"),
        "micro_complete" to listOf("icon3d_heart_check"),
        // Deliberately unaliased: "watch", "look closer" and "see a doctor" have no
        // near-enough stand-in in the current set, and using a reassuring heart for a
        // referral would misreport the result. They fall back to their Material icon.

        // --- empty states and page headers -----------------------------------
        "micro_no_child_yet" to listOf("empty_no_children"),
        "micro_offline_saved" to listOf("empty_offline"),
        "micro_privacy" to listOf("empty_offline"),

        "home_state_nothing_done" to listOf("onboard_screening", "splash_hero_poshan"),
        "home_state_in_progress" to listOf("onboard_screening"),
        "home_state_all_done" to listOf("empty_assessment_complete"),
        "home_state_offline" to listOf("empty_offline"),
        // Originally left unaliased on the grounds that a cheerful picture above a referral
        // is worse than none. That was right about the celebratory candidates and wrong
        // about the outcome: it is the state a tester most often sits in, so the home screen
        // showed a blank box. A neutral screening scene is not celebratory and does not
        // misreport the result.
        "home_state_review_needed" to listOf("onboard_screening"),

        "hero_module_1" to listOf("illus_cat_gross_motor", "onboard_screening"),
        "hero_module_2" to listOf("empty_camera_attention"),
        "hero_module_3" to listOf("icon3d_sun_droplet"),

        "page_growth_hero" to listOf("icon3d_growth_ribbon"),
        "page_vaccines_hero" to listOf("icon3d_vaccine_shield"),
        "page_report_hero" to listOf("icon3d_heart_check"),
        "page_child_hero" to listOf("splash_hero_poshan"),

        // --- module 3 walkthrough --------------------------------------------
        // The four narrative steps were not in this batch. Rather than show four blank
        // panels in a row, the nearest delivered picture stands in; each is still the right
        // subject, just less specific than the step it illustrates.
        "guide_m3_what_this_is" to listOf("guide_m3_shade", "icon3d_sun_droplet"),
        "guide_m3_not_a_blood_test" to listOf("icon3d_heart_check"),
        "guide_m3_daily_drops" to listOf("icon3d_sun_droplet"),
        "guide_m3_what_you_get" to listOf("guide_m3_shade"),
    )

    /** Every drawable name worth trying for [slot], in order of preference. */
    fun candidates(slot: String): List<String> =
        if (slot.isEmpty()) emptyList() else listOf(slot) + aliases[slot].orEmpty()
}

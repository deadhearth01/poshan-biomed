package com.teamposhan.biomed.ui.screens

import androidx.annotation.DrawableRes
import com.teamposhan.biomed.R

/**
 * Maps a milestone question id to its illustration.
 *
 * The mapping is explicit rather than derived from the drawable name: the question text and
 * the artwork brief were written separately, so a name like `ms_13_18_pincer` has to be
 * matched to "Can your child pick up small objects using thumb and finger?" by a human who
 * has read both. A fuzzy match here would silently put the wrong picture beside a question,
 * which is worse than no picture at all — a caregiver answers what they see.
 *
 * Questions without artwork fall back to their category illustration.
 */
object MilestoneArt {

    private val art: Map<String, Int> = mapOf(
        "M0_3_GROSS_MOTOR_1" to R.drawable.ms_0_3_lift_head,
        "M0_3_GROSS_MOTOR_2" to R.drawable.ms_0_3_move_arms_legs,
        "M0_3_GROSS_MOTOR_3" to R.drawable.ms_0_3_head_control,
        "M0_3_FINE_MOTOR_1" to R.drawable.ms_0_3_open_hands,
        "M0_3_FINE_MOTOR_2" to R.drawable.ms_0_3_hands_to_mouth,
        "M0_3_FINE_MOTOR_3" to R.drawable.ms_0_3_holds_object,
        "M0_3_COMMUNICATION_1" to R.drawable.ms_0_3_cooing,
        "M0_3_COMMUNICATION_2" to R.drawable.ms_0_3_reacts_voice,
        "M0_3_COMMUNICATION_3" to R.drawable.ms_0_3_listens,
        "M0_3_SOCIAL_EMOTIONAL_1" to R.drawable.ms_0_3_look_at_face,
        "M0_3_SOCIAL_EMOTIONAL_2" to R.drawable.ms_0_3_responsive_smile,
        "M0_3_SOCIAL_EMOTIONAL_3" to R.drawable.ms_0_3_interest_interaction,
        "M0_3_SOCIAL_EMOTIONAL_4" to R.drawable.ms_0_3_turn_to_sound,
        "M4_6_GROSS_MOTOR_1" to R.drawable.ms_4_6_steady_head,
        "M4_6_GROSS_MOTOR_2" to R.drawable.ms_4_6_push_up_arms,
        "M4_6_GROSS_MOTOR_3" to R.drawable.ms_4_6_roll_tummy_back,
        "M4_6_GROSS_MOTOR_4" to R.drawable.ms_4_6_bear_weight_legs,
        "M4_6_FINE_MOTOR_1" to R.drawable.ms_4_6_grasp_toy,
        "M4_6_FINE_MOTOR_2" to R.drawable.ms_4_6_grab_object,
        "M4_6_FINE_MOTOR_3" to R.drawable.ms_4_6_mouthing,
        "M4_6_COMMUNICATION_1" to R.drawable.ms_4_6_babble,
        "M4_6_COMMUNICATION_2" to R.drawable.ms_4_6_responds_speech,
        "M4_6_COMMUNICATION_3" to R.drawable.ms_4_6_sounds_attention,
        "M4_6_SOCIAL_EMOTIONAL_1" to R.drawable.ms_4_6_laughs,
        "M4_6_SOCIAL_EMOTIONAL_2" to R.drawable.ms_4_6_stares_faces,
        "M4_6_SOCIAL_EMOTIONAL_3" to R.drawable.ms_4_6_curious,
        "M7_9_GROSS_MOTOR_1" to R.drawable.ms_7_9_sit_unsupported,
        "M7_9_GROSS_MOTOR_2" to R.drawable.ms_7_9_rolls_both_ways,
        "M7_9_GROSS_MOTOR_3" to R.drawable.ms_7_9_changes_position,
        "M7_9_GROSS_MOTOR_4" to R.drawable.ms_7_9_attempts_crawl,
        "M7_9_FINE_MOTOR_1" to R.drawable.ms_7_9_reach_grasp,
        "M7_9_FINE_MOTOR_2" to R.drawable.ms_7_9_pass_toy,
        "M7_9_FINE_MOTOR_3" to R.drawable.ms_7_9_thumb_fingers,
        "M7_9_COMMUNICATION_1" to R.drawable.ms_7_9_babble_sounds,
        "M7_9_COMMUNICATION_2" to R.drawable.ms_7_9_responds_name,
        "M7_9_COMMUNICATION_3" to R.drawable.ms_7_9_imitates_sounds,
        "M7_9_SOCIAL_EMOTIONAL_1" to R.drawable.ms_7_9_recognises_people,
        "M7_9_SOCIAL_EMOTIONAL_2" to R.drawable.ms_7_9_prefers_caregiver,
        "M7_9_SOCIAL_EMOTIONAL_3" to R.drawable.ms_7_9_peek_a_boo,
        "M10_12_GROSS_MOTOR_1" to R.drawable.ms_10_12_sits_alone,
        "M10_12_GROSS_MOTOR_2" to R.drawable.ms_10_12_crawls,
        "M10_12_GROSS_MOTOR_3" to R.drawable.ms_10_12_pull_up_stand,
        "M10_12_GROSS_MOTOR_4" to R.drawable.ms_10_12_stands_support,
        "M10_12_FINE_MOTOR_1" to R.drawable.ms_10_12_pincer_grip,
        "M10_12_FINE_MOTOR_2" to R.drawable.ms_10_12_releases_object,
        "M10_12_FINE_MOTOR_3" to R.drawable.ms_10_12_points,
        "M10_12_COMMUNICATION_1" to R.drawable.ms_10_12_follows_instruction,
        "M10_12_COMMUNICATION_2" to R.drawable.ms_10_12_waves,
        "M10_12_COMMUNICATION_3" to R.drawable.ms_10_12_first_words,
        "M10_12_SOCIAL_EMOTIONAL_1" to R.drawable.ms_10_12_responds_name,
        "M10_12_SOCIAL_EMOTIONAL_2" to R.drawable.ms_10_12_imitates_actions,
        "M10_12_SOCIAL_EMOTIONAL_3" to R.drawable.ms_10_12_plays_caregiver,
        "M13_18_GROSS_MOTOR_1" to R.drawable.ms_13_18_stands_alone,
        "M13_18_GROSS_MOTOR_2" to R.drawable.ms_13_18_walk_unsupported,
        "M13_18_GROSS_MOTOR_3" to R.drawable.ms_13_18_squats,
        "M13_18_GROSS_MOTOR_4" to R.drawable.ms_13_18_climbs_furniture,
        "M13_18_FINE_MOTOR_1" to R.drawable.ms_13_18_pincer,
        "M13_18_FINE_MOTOR_2" to R.drawable.ms_13_18_stack_blocks,
        "M13_18_FINE_MOTOR_3" to R.drawable.ms_13_18_uses_spoon,
        "M13_18_FINE_MOTOR_4" to R.drawable.ms_13_18_scribbles,
        "M13_18_COMMUNICATION_1" to R.drawable.ms_13_18_meaningful_words,
        "M13_18_COMMUNICATION_2" to R.drawable.ms_13_18_understands_simple,
        "M13_18_COMMUNICATION_3" to R.drawable.ms_13_18_points_wants,
        "M13_18_COMMUNICATION_4" to R.drawable.ms_13_18_imitates_words,
        "M13_18_SOCIAL_EMOTIONAL_1" to R.drawable.ms_13_18_shows_affection,
        "M13_18_SOCIAL_EMOTIONAL_2" to R.drawable.ms_13_18_interactive_games,
        "M13_18_SOCIAL_EMOTIONAL_3" to R.drawable.ms_13_18_interest_children,
        "M19_24_GROSS_MOTOR_1" to R.drawable.ms_19_24_walks,
        "M19_24_GROSS_MOTOR_2" to R.drawable.ms_19_24_runs,
        "M19_24_GROSS_MOTOR_3" to R.drawable.ms_19_24_climbs_stairs,
        "M19_24_GROSS_MOTOR_4" to R.drawable.ms_19_24_kick_ball,
        "M19_24_FINE_MOTOR_1" to R.drawable.ms_19_24_builds_tower,
        "M19_24_FINE_MOTOR_2" to R.drawable.ms_19_24_turn_pages,
        "M19_24_FINE_MOTOR_3" to R.drawable.ms_19_24_scribbles,
        "M19_24_COMMUNICATION_1" to R.drawable.ms_19_24_several_words,
        "M19_24_COMMUNICATION_2" to R.drawable.ms_19_24_two_words,
        "M19_24_COMMUNICATION_3" to R.drawable.ms_19_24_two_step,
        "M19_24_SOCIAL_EMOTIONAL_1" to R.drawable.ms_19_24_communicates_wants,
        "M19_24_SOCIAL_EMOTIONAL_2" to R.drawable.ms_19_24_independence,
        "M19_24_SOCIAL_EMOTIONAL_3" to R.drawable.ms_19_24_interacts,
        "M25_36_GROSS_MOTOR_1" to R.drawable.ms_25_36_runs_steadily,
        "M25_36_GROSS_MOTOR_2" to R.drawable.ms_25_36_jumps,
        "M25_36_GROSS_MOTOR_3" to R.drawable.ms_25_36_stairs,
        "M25_36_GROSS_MOTOR_4" to R.drawable.ms_25_36_throws,
        "M25_36_FINE_MOTOR_1" to R.drawable.ms_25_36_copies_lines,
        "M25_36_FINE_MOTOR_2" to R.drawable.ms_25_36_stack_blocks,
        "M25_36_FINE_MOTOR_3" to R.drawable.ms_25_36_turns_pages,
        "M25_36_COMMUNICATION_1" to R.drawable.ms_25_36_short_sentences,
        "M25_36_COMMUNICATION_2" to R.drawable.ms_25_36_unfamiliar_understand,
        "M25_36_COMMUNICATION_3" to R.drawable.ms_25_36_names_objects,
        "M25_36_COMMUNICATION_4" to R.drawable.ms_25_36_answers_questions,
        "M25_36_SOCIAL_EMOTIONAL_1" to R.drawable.ms_25_36_shows_interest,
        "M25_36_SOCIAL_EMOTIONAL_2" to R.drawable.ms_25_36_plays_children,
        "M25_36_SOCIAL_EMOTIONAL_3" to R.drawable.ms_25_36_pretend_play,
    )

    /** Artwork for this question, or null to fall back to the category illustration. */
    @DrawableRes
    fun forQuestion(questionId: String): Int? = art[questionId]

    @DrawableRes
    fun forCategory(categoryName: String): Int = when (categoryName) {
        "GROSS_MOTOR" -> R.drawable.illus_cat_gross_motor
        "FINE_MOTOR" -> R.drawable.illus_cat_fine_motor
        "COMMUNICATION" -> R.drawable.illus_cat_communication
        "SOCIAL_EMOTIONAL" -> R.drawable.illus_cat_social_emotional
        "NUTRITION" -> R.drawable.illus_cat_nutrition
        else -> R.drawable.illus_cat_gross_motor
    }

    /** How many of the 123 questions currently have their own artwork. */
    val mappedCount: Int get() = art.size
}

package com.teamposhan.biomed.ui.navigation

object Routes {
    const val LANGUAGE = "language"
    const val GUIDE = "guide"
    const val LOGIN = "login"
    const val CONSENT = "consent"
    const val PERMISSIONS = "permissions"
    const val USER_TYPE = "user_type"
    const val PROFILE_SETUP = "profile_setup"

    /** The signed-in person's own details, shown back to them and editable. */
    const val PROFILE = "profile"

    const val HOME = "home"
    const val CHILD_LIST = "child_list"
    const val CHILD_FORM = "child_form"

    /** The same form, pre-filled, for correcting a child's details after enrolment. */
    const val CHILD_EDIT = "child_edit/{childId}"

    /** Every question and answer for one module of the current assessment. */
    const val MODULE_DETAIL = "module_detail/{module}"
    const val CHILD_DETAIL = "child_detail/{childId}"

    const val ASSESSMENT_INTRO = "assessment_intro"
    const val CATEGORY_OVERVIEW = "category_overview"
    const val CATEGORY_INTRO = "category_intro/{category}"
    const val QUESTION = "question/{category}"
    const val CATEGORY_REVIEW = "category_review/{category}"
    const val NUTRITION = "nutrition"

    /** Age-banded feeding-practice questions. */
    const val NUTRITION_QUESTIONS = "nutrition_questions"

    /** One-off household background and risk factors. */
    const val NUTRITION_BACKGROUND = "nutrition_background"

    const val NUTRITION_RESULT = "nutrition_result"
    const val GROWTH = "growth"
    const val ASSESSMENT_REVIEW = "assessment_review"
    const val RESULT = "result"

    const val ATTENTION_INTRO = "attention_intro"

    /** Phase overview: which stages and tasks this child's age band gets, and why. */
    const val ATTENTION_PHASE_OVERVIEW = "attention_phase_overview"

    /** Per-stage instructions, shown before the live camera session begins. */
    const val ATTENTION_STAGE_INTRO = "attention_stage_intro/{stage}"

    const val ATTENTION_SESSION = "attention_session"
    const val ATTENTION_RESULT = "attention_result"

    /** Module 2's caregiver-report half: early markers from the developmental guide. */
    const val EARLY_MARKERS_INTRO = "early_markers_intro"
    const val EARLY_MARKERS_QUESTIONS = "early_markers_questions"
    const val EARLY_MARKERS_RESULT = "early_markers_result"

    const val VITAMIN_D = "vitamin_d"
    const val IMMUNIZATION = "immunization"
    const val REPORT = "report"

    /** One specific past assessment, opened from the child timeline. */
    const val SESSION_REPORT = "session_report/{sessionId}"

    /** Every calculation the app performs, shown transparently. */
    const val ABOUT = "about"

    /** Full-screen WHO growth chart for one indicator, opened from the report. */
    const val GROWTH_CHART = "growth_chart/{metric}?sample={sample}"

    fun sessionReport(sessionId: String) = "session_report/$sessionId"

    fun attentionStageIntro(stage: String) = "attention_stage_intro/$stage"

    fun growthChart(metric: String, sample: Boolean = false) = "growth_chart/$metric?sample=$sample"

    fun childDetail(childId: String) = "child_detail/$childId"

    fun childEdit(childId: String) = "child_edit/$childId"

    fun moduleDetail(module: String) = "module_detail/$module"
    fun categoryIntro(category: String) = "category_intro/$category"
    fun question(category: String) = "question/$category"
    fun categoryReview(category: String) = "category_review/$category"
}

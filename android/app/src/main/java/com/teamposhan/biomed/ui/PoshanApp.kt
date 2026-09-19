package com.teamposhan.biomed.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.teamposhan.biomed.data.local.PoshanContainer
import com.teamposhan.biomed.data.model.AssessmentCategory
import com.teamposhan.biomed.data.model.UserRole
import com.teamposhan.biomed.domain.age.AgeCalculator
import com.teamposhan.biomed.domain.milestones.AgeBand
import com.teamposhan.biomed.locale.AppLocale
import com.teamposhan.biomed.ui.components.NavItem
import com.teamposhan.biomed.ui.components.PoshanBottomBar
import com.teamposhan.biomed.ui.components.PoshanScaffold
import com.teamposhan.biomed.ui.components.RepeatModuleDialog
import com.teamposhan.biomed.ui.screens.ModuleDetail
import com.teamposhan.biomed.ui.screens.ModuleDetailScreen
import com.teamposhan.biomed.ui.screens.ProfileScreen
import com.teamposhan.biomed.ui.navigation.Routes
import com.teamposhan.biomed.ui.screens.AssessmentResultsScreen
import com.teamposhan.biomed.ui.screens.AssessmentIntroScreen
import com.teamposhan.biomed.ui.screens.AssessmentReviewScreen
import com.teamposhan.biomed.ui.screens.AttentionIntroScreen
import com.teamposhan.biomed.ui.screens.AttentionResultScreen
import com.teamposhan.biomed.ui.screens.AttentionSessionScreen
import com.teamposhan.biomed.ui.screens.CategoryIntroScreen
import com.teamposhan.biomed.ui.screens.CategoryOverviewScreen
import com.teamposhan.biomed.ui.screens.CategoryReviewScreen
import com.teamposhan.biomed.ui.screens.ChildDetailScreen
import com.teamposhan.biomed.ui.screens.ChildFormScreen
import com.teamposhan.biomed.ui.screens.ChildListScreen
import com.teamposhan.biomed.ui.screens.ConsentScreen
import com.teamposhan.biomed.ui.screens.GuideScreen
import com.teamposhan.biomed.ui.screens.HomeScreen
import com.teamposhan.biomed.ui.screens.ImmunizationScreen
import com.teamposhan.biomed.ui.screens.LanguageScreen
import com.teamposhan.biomed.ui.screens.LoginScreen
import com.teamposhan.biomed.ui.screens.NutritionScreen
import com.teamposhan.biomed.ui.screens.PermissionsScreen
import com.teamposhan.biomed.ui.screens.ProfileSetupScreen
import com.teamposhan.biomed.ui.screens.QuestionScreen
import com.teamposhan.biomed.data.model.Sex
import com.teamposhan.biomed.domain.growth.GrowthStandards
import com.teamposhan.biomed.domain.growth.SampleGrowthData
import com.teamposhan.biomed.ui.screens.GrowthPoint
import com.teamposhan.biomed.ui.screens.ChildSwitcherAction
import com.teamposhan.biomed.ui.screens.AboutAction
import com.teamposhan.biomed.ui.screens.ProfileAction
import com.teamposhan.biomed.ui.screens.EarlyMarkerIntroScreen
import com.teamposhan.biomed.ui.screens.NutritionBackgroundScreen
import com.teamposhan.biomed.ui.screens.NutritionQuestionScreen
import com.teamposhan.biomed.ui.screens.NutritionResultScreen
import com.teamposhan.biomed.ui.screens.EarlyMarkerQuestionScreen
import com.teamposhan.biomed.ui.screens.EarlyMarkerResultScreen
import com.teamposhan.biomed.ui.screens.AboutScreen
import com.teamposhan.biomed.ui.screens.AttentionPhaseOverviewScreen
import com.teamposhan.biomed.ui.screens.ChildTimelineScreen
import com.teamposhan.biomed.ui.screens.AttentionStageIntroScreen
import com.teamposhan.biomed.ui.screens.GrowthChartDetailScreen
import com.teamposhan.biomed.ui.screens.GrowthChartsHubScreen
import com.teamposhan.biomed.ui.screens.growthHistoryFor
import com.teamposhan.biomed.ui.screens.ReportScreen
import com.teamposhan.biomed.ui.screens.ResultScreen
import com.teamposhan.biomed.ui.screens.UserTypeScreen
import com.teamposhan.biomed.ui.screens.VitaminDScreen
import com.teamposhan.biomed.ui.theme.PoshanTheme
import androidx.compose.material3.Text

// Children is reached from the app-bar action on Home rather than a tab: switching child
// is a context change, not a destination, and it kept stealing a third of the bottom bar.
private val bottomItems = listOf(
    NavItem(Routes.HOME, "Home", Icons.Filled.Home),
    NavItem(Routes.REPORT, "Report", Icons.Filled.BarChart),
)

@Composable
fun PoshanApp(container: PoshanContainer) {
    val vm: AppViewModel = viewModel(factory = AppViewModelFactory(container))
    val nav = rememberNavController()

    // Push local changes to the cloud every few seconds for as long as the app is open.
    //
    // Every answer is written to disk the instant it is tapped, so local data is never at
    // risk — but a device that is only synced at the end of a session loses everything if
    // it is lost or wiped mid-visit, and a worker moving between households may not reopen
    // the app for hours. Each tick is a no-op when offline or when a sync is already in
    // flight, so this costs nothing without a connection.
    DisposableEffect(Unit) {
        vm.startContinuousSync()
        onDispose { vm.stopContinuousSync() }
    }

    val role by vm.role.collectAsState()
    val children by vm.children.collectAsState()
    val activeChild by vm.activeChild.collectAsState()
    val session by vm.session.collectAsState()
    val sessions by vm.sessions.collectAsState()
    val syncState by vm.syncState.collectAsState()
    val syncing by vm.syncing.collectAsState()
    val gaze by vm.lastGaze.collectAsState()
    val onboarded by vm.onboarded.collectAsState()

    PoshanTheme {
        // Wait until we know whether onboarding is done, so the app resumes where it left off.
        val start = onboarded
        if (start == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                com.teamposhan.biomed.ui.components.PoshanLoader()
            }
            return@PoshanTheme
        }

        NavHost(
            navController = nav,
            startDestination = if (start) Routes.HOME else Routes.LANGUAGE,
            enterTransition = {
                slideInHorizontally(tween(260), initialOffsetX = { it / 10 }) + fadeIn(tween(220))
            },
            exitTransition = { fadeOut(tween(140)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = {
                slideOutHorizontally(tween(220), targetOffsetX = { it / 10 }) + fadeOut(tween(140))
            },
        ) {
            composable(Routes.LANGUAGE) {
                LanguageScreen(current = AppLocale.current()) { code ->
                    vm.setLanguage(code)
                    nav.navigate(Routes.GUIDE)
                }
            }
            composable(Routes.GUIDE) { GuideScreen { nav.navigate(Routes.LOGIN) } }
            composable(Routes.LOGIN) {
                LoginScreen(onPhoneLogin = { nav.navigate(Routes.CONSENT) }, onGuest = { nav.navigate(Routes.CONSENT) })
            }
            composable(Routes.CONSENT) { ConsentScreen { nav.navigate(Routes.PERMISSIONS) } }
            composable(Routes.PERMISSIONS) { PermissionsScreen { nav.navigate(Routes.USER_TYPE) } }
            composable(Routes.USER_TYPE) {
                UserTypeScreen { picked ->
                    vm.setRoleHint(picked)
                    nav.navigate(Routes.PROFILE_SETUP)
                }
            }
            composable(Routes.PROFILE_SETUP) {
                ProfileSetupScreen(role = role) { profile ->
                    vm.completeOnboarding(role, profile)
                    nav.navigate(Routes.HOME) { popUpTo(Routes.LANGUAGE) { inclusive = true } }
                }
            }

            // ---- main shell with bottom navigation --------------------------------

            composable(Routes.HOME) {
                MainShell(
                    nav,
                    Routes.HOME,
                    actions = {
                        com.teamposhan.biomed.ui.components.ReadAloudAction(
                            textProvider = {
                                val c = activeChild
                                if (c == null) {
                                    "No child added yet. Tap add child to begin."
                                } else {
                                    "${c.name}, ${AgeCalculator.format(vm.ageMonths(c))}. " +
                                        "Tap the highlighted card to continue the screening."
                                }
                            },
                        )
                        ProfileAction(onClick = { nav.navigate(Routes.PROFILE) })
                        AboutAction(onClick = { nav.navigate(Routes.ABOUT) })
                        ChildSwitcherAction(
                            child = activeChild,
                            childCount = children.size,
                            onClick = { nav.navigate(Routes.CHILD_LIST) },
                        )
                    },
                ) {
                    // Derived from observed state, and preferring the in-progress draft.
                    // Reading the repo directly here did not recompose when an answer was
                    // saved, so finished modules kept showing as not started.
                    val latest = activeChild?.let { c ->
                        session?.takeIf { it.childId == c.id }
                            ?: sessions.filter { it.childId == c.id }.maxByOrNull { it.startedAt }
                    }
                    // Set when someone opens a module this child has already finished. The
                    // navigation is deferred until they say whether they want to look at the
                    // old result or start a fresh one.
                    var repeatPrompt by remember {
                        mutableStateOf<Triple<AppViewModel.Module, String, String>?>(null)
                    }

                    // Opens a module, asking first if it has been done before.
                    val openModule: (AppViewModel.Module, String, String) -> Unit =
                        { module, name, route ->
                            val child = activeChild
                            if (child == null) {
                                nav.navigate(Routes.CHILD_FORM)
                            } else {
                                vm.startSession(child)
                                val progress = vm.progressOf(session, child, module)
                                val done = vm.lastCompleted(child.id, module)
                                when {
                                    // Half-finished: resume silently. Everything is already
                                    // persisted on each answer, so "resume" just means opening
                                    // it and letting the saved answers show through. Asking
                                    // "do it again or see results" here offered neither of the
                                    // two things the user actually wanted.
                                    progress.resumable -> nav.navigate(route)

                                    // Genuinely finished before: offer results or a fresh run.
                                    done != null -> repeatPrompt = Triple(module, name, route)

                                    else -> nav.navigate(route)
                                }
                            }
                        }

                    repeatPrompt?.let { (module, name, route) ->
                        val child = activeChild
                        val done = child?.let { vm.lastCompleted(it.id, module) }
                        if (child == null || done == null) {
                            repeatPrompt = null
                        } else {
                            RepeatModuleDialog(
                                moduleName = name,
                                completedAt = done,
                                onViewResult = {
                                    repeatPrompt = null
                                    nav.navigate(Routes.REPORT)
                                },
                                onRepeat = {
                                    repeatPrompt = null
                                    // A separate assessment, so the earlier one survives
                                    // with its own date instead of being written over.
                                    vm.startSession(child, forceNew = true)
                                    nav.navigate(route)
                                },
                                onDismiss = { repeatPrompt = null },
                            )
                        }
                    }

                    HomeScreen(
                        role = role,
                        child = activeChild,
                        latest = latest,
                        sessions = activeChild?.let { c -> sessions.filter { it.childId == c.id } } ?: emptyList(),
                        syncMessage = syncState?.message,
                        syncing = syncing,
                        onSync = { vm.syncNow() },
                        onStartAssessment = {
                            openModule(
                                AppViewModel.Module.MILESTONES,
                                "Growth and everyday skills",
                                Routes.ASSESSMENT_INTRO,
                            )
                        },
                        onAttention = {
                            val child = activeChild
                            if (child == null) {
                                nav.navigate(Routes.CHILD_FORM)
                            } else {
                                // Under 9 months there is no camera task worth running —
                                // the probes depend on skills the child does not have yet —
                                // so the module IS the caregiver questionnaire.
                                val route = if (
                                    vm.ageMonths(child) <
                                        com.teamposhan.biomed.domain.attention
                                            .AttentionBand.GAZE_MIN_MONTHS
                                ) {
                                    Routes.EARLY_MARKERS_INTRO
                                } else {
                                    Routes.ATTENTION_INTRO
                                }
                                openModule(
                                    AppViewModel.Module.ATTENTION,
                                    "Looking and attention",
                                    route,
                                )
                            }
                        },
                        onVitaminD = {
                            openModule(
                                AppViewModel.Module.VITAMIN_D,
                                "Sunlight and vitamin D",
                                Routes.VITAMIN_D,
                            )
                        },
                        onImmunization = {
                            if (session == null) activeChild?.let { vm.startSession(it) }
                            nav.navigate(Routes.IMMUNIZATION)
                        },
                        onReport = { nav.navigate(Routes.REPORT) },
                        onAddChild = { nav.navigate(Routes.CHILD_FORM) },
                        onOpenChild = { nav.navigate(Routes.CHILD_LIST) },
                        onGrowthCharts = { nav.navigate(Routes.GROWTH) },
                    )
                }
            }

            composable(Routes.CHILD_LIST) {
                MainShell(nav, Routes.CHILD_LIST, title = "Children") {
                    ChildListScreen(
                        children = children,
                        onOpen = { child ->
                            vm.setActiveChild(child)
                            nav.navigate(Routes.childDetail(child.id))
                        },
                        onEdit = { child -> nav.navigate(Routes.childEdit(child.id)) },
                        onDelete = { child -> vm.deleteChild(child.id) },
                        onAdd = { nav.navigate(Routes.CHILD_FORM) },
                    )
                }
            }

            composable(
                Routes.MODULE_DETAIL,
                arguments = listOf(navArgument("module") { type = NavType.StringType }),
            ) { entry ->
                val module = runCatching {
                    ModuleDetail.valueOf(entry.arguments?.getString("module").orEmpty())
                }.getOrDefault(ModuleDetail.MILESTONES)
                val child = activeChild
                val latestSession = session ?: child?.let { vm.latestFor(it.id) }
                PoshanScaffold(title = "Answers", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        ModuleDetailScreen(
                            child = child,
                            session = latestSession,
                            module = module,
                            onDone = { nav.popBackStack() },
                        )
                    }
                }
            }

            composable(
                Routes.CHILD_EDIT,
                arguments = listOf(navArgument("childId") { type = NavType.StringType }),
            ) { entry ->
                val childId = entry.arguments?.getString("childId")
                val existing = children.firstOrNull { it.id == childId }
                PoshanScaffold(title = "Edit child", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        if (existing == null) {
                            // Deleted from another screen while this one was open.
                            LaunchedEffect(Unit) { nav.popBackStack() }
                        } else {
                            ChildFormScreen(existing = existing) { updated ->
                                vm.updateChild(updated)
                                // Keep the active child in step, or the home screen would go
                                // on showing the values as they were before the edit.
                                if (activeChild?.id == updated.id) vm.setActiveChild(updated)
                                nav.popBackStack()
                            }
                        }
                    }
                }
            }

            composable(Routes.PROFILE) {
                val profile by vm.profile.collectAsState()
                PoshanScaffold(title = "Your profile", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        ProfileScreen(
                            profile = profile,
                            onSave = { vm.updateProfile(it) },
                            onDone = { nav.popBackStack() },
                        )
                    }
                }
            }

            composable(Routes.CHILD_FORM) {
                PoshanScaffold(title = "Child details", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(
                        Modifier.padding(padding)
                    ) {
                        ChildFormScreen(existing = null) { child ->
                            vm.addChild(child)
                            vm.setActiveChild(child)
                            nav.popBackStack()
                        }
                    }
                }
            }

            composable(
                Routes.CHILD_DETAIL,
                arguments = listOf(navArgument("childId") { type = NavType.StringType }),
            ) { entry ->
                val childId = entry.arguments?.getString("childId").orEmpty()
                val child = children.firstOrNull { it.id == childId }
                PoshanScaffold(title = child?.name ?: "Child", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        if (child != null) {
                            ChildTimelineScreen(
                                child = child,
                                sessions = sessions.filter { it.childId == child.id },
                                onOpenReport = { session ->
                                    vm.setActiveChild(child)
                                    nav.navigate(Routes.sessionReport(session.id))
                                },
                                onOpenCharts = {
                                    vm.setActiveChild(child)
                                    nav.navigate(Routes.GROWTH)
                                },
                            )
                        }
                    }
                }
            }

            composable("child_detail_legacy") {
                val child = children.firstOrNull()
                PoshanScaffold(title = child?.name ?: "Child", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        if (child != null) {
                            ChildDetailScreen(
                                child = child,
                                sessions = sessions.filter { it.childId == child.id },
                                onStartAssessment = {
                                    vm.setActiveChild(child)
                                    vm.startSession(child)
                                    nav.navigate(Routes.ASSESSMENT_INTRO)
                                },
                                onAttention = {
                                    vm.setActiveChild(child)
                                    nav.navigate(Routes.ATTENTION_INTRO)
                                },
                                onReport = {
                                    vm.setActiveChild(child)
                                    nav.navigate(Routes.REPORT)
                                },
                            )
                        }
                    }
                }
            }

            // ---- assessment flow --------------------------------------------------

            composable(Routes.ASSESSMENT_INTRO) {
                val child = activeChild
                PoshanScaffold(title = "Screening", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        if (child != null) {
                            AssessmentIntroScreen(
                                childName = child.name,
                                ageLabel = "${AgeCalculator.format(vm.ageMonths(child))} Â· ${AgeBand.forAge(vm.ageMonths(child)).label}",
                                onStart = { nav.navigate(Routes.CATEGORY_OVERVIEW) },
                            )
                        }
                    }
                }
            }

            composable(Routes.CATEGORY_OVERVIEW) {
                PoshanScaffold(title = "Categories", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        CategoryOverviewScreen(
                            categories = vm.scoredCategories(),
                            nutritionDone = session?.nutrition != null,
                            onOpenCategory = { nav.navigate(Routes.categoryIntro(it.name)) },
                            // Feeding questions first, then the measurements screen.
                        onOpenNutrition = { nav.navigate(Routes.NUTRITION_QUESTIONS) },
                            onReview = { nav.navigate(Routes.ASSESSMENT_REVIEW) },
                        )
                    }
                }
            }

            composable(
                Routes.CATEGORY_INTRO,
                arguments = listOf(navArgument("category") { type = NavType.StringType }),
            ) { entry ->
                val category = categoryOf(entry.arguments?.getString("category"))
                PoshanScaffold(title = "Category", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        CategoryIntroScreen(
                            category = category,
                            questionCount = vm.questionsFor(category).size,
                            onStart = { nav.navigate(Routes.question(category.name)) },
                        )
                    }
                }
            }

            composable(
                Routes.QUESTION,
                arguments = listOf(navArgument("category") { type = NavType.StringType }),
            ) { entry ->
                val category = categoryOf(entry.arguments?.getString("category"))
                val questions = vm.questionsFor(category)
                val answersById = session?.answers?.associateBy { it.questionId } ?: emptyMap()
                PoshanScaffold(
                    title = "",
                    onBack = { nav.popBackStack() },
                ) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        QuestionScreen(
                            category = category,
                            questions = questions,
                            answersById = answersById,
                            currentBand = vm.currentBand(),
                            onAnswer = { q, r -> vm.answer(q.id, q.category, r) },
                            onFinish = { nav.navigate(Routes.categoryReview(category.name)) },
                        )
                    }
                }
            }

            composable(
                Routes.CATEGORY_REVIEW,
                arguments = listOf(navArgument("category") { type = NavType.StringType }),
            ) { entry ->
                val category = categoryOf(entry.arguments?.getString("category"))
                val questions = vm.questionsFor(category)
                val answersById = session?.answers?.associateBy { it.questionId } ?: emptyMap()
                PoshanScaffold(title = "Review", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        CategoryReviewScreen(
                            category = category,
                            questions = questions,
                            answersById = answersById,
                            onEdit = { nav.navigate(Routes.question(category.name)) },
                            onContinue = { nav.popBackStack(Routes.CATEGORY_OVERVIEW, inclusive = false) },
                        )
                    }
                }
            }

            composable(Routes.NUTRITION) {
                PoshanScaffold(title = "Nutrition & growth", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        NutritionScreen(
                            ageMonths = activeChild?.let { vm.ageMonths(it) } ?: 12f,
                            // Needed for the z-scores that decide whether the arm
                            // measurement is prompted.
                            sex = activeChild?.sex
                                ?: com.teamposhan.biomed.data.model.Sex.OTHER,
                            initial = session?.nutrition,
                            measurement = session?.growth,
                            onSave = { nutrition, measurement ->
                                vm.saveNutrition(nutrition)
                                vm.saveGrowth(measurement)
                                nav.popBackStack()
                            },
                        )
                    }
                }
            }

            // ---- nutrition questionnaire -----------------------------------------

            composable(Routes.NUTRITION_QUESTIONS) {
                val child = activeChild
                PoshanScaffold(title = "Feeding", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        NutritionQuestionScreen(
                            ageMonths = child?.let { vm.ageMonths(it) } ?: 12f,
                            answers = session?.nutritionAnswers ?: emptyMap(),
                            onAnswer = { q, r -> vm.answerNutrition(q.id, r) },
                            onFinish = {
                                // The background set is asked once; skip straight past it
                                // when this child already has it on file.
                                val hasBackground = (child?.nutritionBackground?.size ?: 0) >= 10
                                if (hasBackground) {
                                    nav.navigate(Routes.NUTRITION_RESULT)
                                } else {
                                    nav.navigate(Routes.NUTRITION_BACKGROUND)
                                }
                            },
                        )
                    }
                }
            }

            composable(Routes.NUTRITION_BACKGROUND) {
                val child = activeChild
                PoshanScaffold(title = "Background", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        NutritionBackgroundScreen(
                            answers = child?.nutritionBackground ?: emptyMap(),
                            onAnswer = { q, r -> vm.answerNutritionBackground(q.id, r) },
                            onFinish = { nav.navigate(Routes.NUTRITION_RESULT) },
                        )
                    }
                }
            }

            composable(Routes.NUTRITION_RESULT) {
                val child = activeChild
                PoshanScaffold(title = "Feeding", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        NutritionResultScreen(
                            ageMonths = child?.let { vm.ageMonths(it) } ?: 12f,
                            answers = (session?.nutritionAnswers ?: emptyMap()) +
                                (child?.nutritionBackground ?: emptyMap()),
                            onDone = { nav.popBackStack(Routes.HOME, inclusive = false) },
                        )
                    }
                }
            }

            composable(Routes.ASSESSMENT_REVIEW) {
                PoshanScaffold(title = "Review", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        AssessmentReviewScreen(
                            categories = vm.scoredCategories(),
                            nutrition = session?.nutrition,
                            growthZ = session?.growthZ,
                            dq = vm.liveDq(),
                            onSubmit = {
                                vm.finalize()
                                nav.navigate(Routes.RESULT) { popUpTo(Routes.HOME) { inclusive = false } }
                            },
                        )
                    }
                }
            }

            // ---- module 2 ---------------------------------------------------------

            composable(Routes.ATTENTION_INTRO) {
                val child = activeChild
                PoshanScaffold(title = "Attention & gaze", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        if (child != null) {
                            AttentionIntroScreen(
                                childName = child.name,
                                ageMonths = vm.ageMonths(child),
                                onStart = { nav.navigate(Routes.ATTENTION_PHASE_OVERVIEW) },
                            )
                        }
                    }
                }
            }

            // Phase overview — module 2's counterpart to module 1's category overview.
            composable(Routes.ATTENTION_PHASE_OVERVIEW) {
                val child = activeChild
                PoshanScaffold(title = "Attention & gaze", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        if (child != null) {
                            AttentionPhaseOverviewScreen(
                                childName = child.name,
                                ageMonths = vm.ageMonths(child),
                                onStartStage = { stage ->
                                    nav.navigate(Routes.attentionStageIntro(stage.name))
                                },
                                onBegin = {
                                    nav.navigate(
                                        Routes.attentionStageIntro(
                                            com.teamposhan.biomed.domain.attention.SessionStage.QUALITY_GATE.name
                                        )
                                    )
                                },
                            )
                        }
                    }
                }
            }

            composable(
                Routes.ATTENTION_STAGE_INTRO,
                arguments = listOf(navArgument("stage") { type = NavType.StringType }),
            ) { entry ->
                val stage = runCatching {
                    com.teamposhan.biomed.domain.attention.SessionStage.valueOf(
                        entry.arguments?.getString("stage").orEmpty()
                    )
                }.getOrDefault(com.teamposhan.biomed.domain.attention.SessionStage.QUALITY_GATE)
                val child = activeChild
                PoshanScaffold(title = stage.label, onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        AttentionStageIntroScreen(
                            stage = stage,
                            ageMonths = child?.let { vm.ageMonths(it) } ?: 12f,
                            onStart = {
                                // The trajectory stage is computed, not performed, so it ends
                                // the flow rather than opening the camera.
                                if (stage == com.teamposhan.biomed.domain.attention.SessionStage.TRAJECTORY) {
                                    nav.popBackStack(Routes.ATTENTION_PHASE_OVERVIEW, inclusive = false)
                                } else {
                                    nav.navigate(Routes.ATTENTION_SESSION)
                                }
                            },
                        )
                    }
                }
            }

            composable(Routes.ATTENTION_SESSION) {
                AttentionSessionScreen(
                    childName = activeChild?.name ?: "baby",
                    ageMonths = activeChild?.let { vm.ageMonths(it) } ?: 12f,
                    onComplete = { result, metrics ->
                        vm.saveAttention(result)
                        vm.recordGazeMetrics(metrics)
                        nav.navigate(Routes.ATTENTION_RESULT)
                    },
                    onBack = { nav.popBackStack() },
                )
            }

            // ---- module 2, caregiver-report half ----------------------------------

            composable(Routes.EARLY_MARKERS_INTRO) {
                val child = activeChild
                PoshanScaffold(title = "Early markers", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        if (child != null) {
                            EarlyMarkerIntroScreen(
                                childName = child.name,
                                ageMonths = vm.ageMonths(child),
                                onStart = { nav.navigate(Routes.EARLY_MARKERS_QUESTIONS) },
                            )
                        }
                    }
                }
            }

            composable(Routes.EARLY_MARKERS_QUESTIONS) {
                val child = activeChild
                PoshanScaffold(title = "", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        EarlyMarkerQuestionScreen(
                            ageMonths = child?.let { vm.ageMonths(it) } ?: 12f,
                            answers = session?.markerAnswers ?: emptyMap(),
                            onAnswer = { q, r -> vm.answerMarker(q.id, r) },
                            onFinish = { nav.navigate(Routes.EARLY_MARKERS_RESULT) },
                        )
                    }
                }
            }

            composable(Routes.EARLY_MARKERS_RESULT) {
                val child = activeChild
                PoshanScaffold(
                    title = "Early markers",
                    onBack = { nav.popBackStack() },
                    actions = {
                        com.teamposhan.biomed.ui.components.ReadAloudAction(
                            textProvider = { "Early marker questions complete." },
                        )
                    },
                ) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        EarlyMarkerResultScreen(
                            ageMonths = child?.let { vm.ageMonths(it) } ?: 12f,
                            answers = session?.markerAnswers ?: emptyMap(),
                            onDone = { nav.popBackStack(Routes.HOME, inclusive = false) },
                        )
                    }
                }
            }

            composable(Routes.ATTENTION_RESULT) {
                PoshanScaffold(title = "Attention result", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        AttentionResultScreen(
                            result = session?.attention ?: com.teamposhan.biomed.data.model.AttentionResult(),
                            metrics = gaze,
                            // The camera half is only half of module 2; the marker questions
                            // capture what a camera cannot see.
                            onContinue = { nav.navigate(Routes.EARLY_MARKERS_INTRO) },
                        )
                    }
                }
            }

            // ---- module 3 + tracking ---------------------------------------------

            composable(Routes.VITAMIN_D) {
                val child = activeChild
                PoshanScaffold(title = "Vitamin D", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        VitaminDScreen(
                            ageMonths = child?.let { vm.ageMonths(it) } ?: 12f,
                            // Latitude drives the sun-window calculation, which is
                            // deterministic and offline. Null simply hides that card.
                            latitude = vm.lastKnownLatitude(),
                            answers = session?.vitaminDAnswers ?: emptyMap(),
                            prophylaxisStatus = runCatching {
                                com.teamposhan.biomed.domain.risk.ProphylaxisStatus.valueOf(
                                    session?.prophylaxisStatus ?: "NOT_ASKED"
                                )
                            }.getOrDefault(com.teamposhan.biomed.domain.risk.ProphylaxisStatus.NOT_ASKED),
                            skinIta = session?.skinItaDegrees,
                            onSkinIta = { vm.setSkinIta(it) },
                            onProphylaxisStatus = { vm.setProphylaxisStatus(it) },
                            onAnswer = { id, response -> vm.answerVitaminD(id, response) },
                            onSave = {
                                vm.completeVitaminD()
                                nav.popBackStack(Routes.HOME, inclusive = false)
                            },
                        )
                    }
                }
            }

            composable(Routes.IMMUNIZATION) {
                val child = activeChild
                PoshanScaffold(title = "Immunization", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        if (child != null) {
                            ImmunizationScreen(
                                ageMonths = vm.ageMonths(child),
                                given = session?.givenVaccines?.toSet() ?: emptySet(),
                                onToggle = { vm.toggleVaccine(it) },
                            )
                        }
                    }
                }
            }

            // ---- results ----------------------------------------------------------

            composable(Routes.RESULT) {
                val child = activeChild
                val latestSession = session ?: child?.let { vm.latestFor(it.id) }
                PoshanScaffold(title = "Result", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        AssessmentResultsScreen(
                            child = child,
                            session = latestSession,
                            liveDq = vm.liveDq(),
                            liveCategories = vm.scoredCategories(),
                            onReport = { nav.navigate(Routes.REPORT) },
                            onDone = { nav.popBackStack(Routes.HOME, inclusive = false) },
                        )
                    }
                }
            }

            // A specific historical assessment, addressed by id. The plain REPORT route
            // always shows the latest, which is wrong when reading back a past visit.
            composable(
                Routes.SESSION_REPORT,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
            ) { entry ->
                val sessionId = entry.arguments?.getString("sessionId").orEmpty()
                val target = sessions.firstOrNull { it.id == sessionId }
                val child = children.firstOrNull { it.id == target?.childId } ?: activeChild
                PoshanScaffold(
                    title = "Report",
                    onBack = { nav.popBackStack() },
                    actions = {
                        com.teamposhan.biomed.ui.components.ReadAloudAction(
                            textProvider = {
                                child?.let {
                                    com.teamposhan.biomed.domain.report.ReportBuilder.build(it, target)
                                } ?: "No report available."
                            },
                        )
                    },
                ) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        ReportScreen(
                            child = child,
                            session = target,
                            history = sessions.filter { it.childId == child?.id },
                            doctorName = vm.profile.value?.doctorName.orEmpty(),
                            doctorPhone = vm.profile.value?.doctorPhone.orEmpty(),
                            onOpenModule = { nav.navigate(Routes.moduleDetail(it.name)) },
                            onOpenChart = { metric, sample ->
                                nav.navigate(Routes.growthChart(metric.name, sample))
                            },
                            onDone = { nav.popBackStack() },
                        )
                    }
                }
            }

            composable(Routes.ABOUT) {
                PoshanScaffold(
                    title = "About",
                    onBack = { nav.popBackStack() },
                    actions = {
                        com.teamposhan.biomed.ui.components.ReadAloudAction(
                            textProvider = { com.teamposhan.biomed.ui.screens.aboutSpokenText() },
                        )
                    },
                ) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        AboutScreen()
                    }
                }
            }

            composable(Routes.REPORT) {
                val child = activeChild
                val latestSession = session ?: child?.let { vm.latestFor(it.id) }
                PoshanScaffold(
                    title = "Report",
                    onBack = { nav.popBackStack() },
                    actions = {
                        com.teamposhan.biomed.ui.components.ReadAloudAction(
                            textProvider = {
                                child?.let {
                                    com.teamposhan.biomed.domain.report.ReportBuilder.build(it, latestSession)
                                } ?: "No child selected."
                            },
                        )
                    },
                ) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        ReportScreen(
                            child = child,
                            session = latestSession,
                            history = sessions.filter { it.childId == child?.id },
                            doctorName = vm.profile.value?.doctorName.orEmpty(),
                            doctorPhone = vm.profile.value?.doctorPhone.orEmpty(),
                            onOpenModule = { nav.navigate(Routes.moduleDetail(it.name)) },
                            onOpenChart = { metric, sample ->
                                nav.navigate(Routes.growthChart(metric.name, sample))
                            },
                            onDone = { nav.popBackStack(Routes.HOME, inclusive = false) },
                        )
                    }
                }
            }

            composable(Routes.GROWTH) {
                val child = activeChild
                val latestSession = session ?: child?.let { vm.latestFor(it.id) }
                PoshanScaffold(title = "Growth charts", onBack = { nav.popBackStack() }) { padding ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
                        GrowthChartsHubScreen(
                            child = child,
                            sessions = sessions.filter { it.childId == child?.id },
                            latest = latestSession,
                            onOpenChart = { metric, sample ->
                                nav.navigate(Routes.growthChart(metric.name, sample))
                            },
                            onRecordMeasurement = {
                                if (session == null) child?.let { vm.startSession(it) }
                                nav.navigate(Routes.NUTRITION)
                            },
                        )
                    }
                }
            }

            // The WHO sheet, opened full screen from a chart in the report.
            composable(
                Routes.GROWTH_CHART,
                arguments = listOf(
                    navArgument("metric") { type = NavType.StringType },
                    navArgument("sample") { type = NavType.BoolType; defaultValue = false },
                ),
            ) { entry ->
                val metric = runCatching {
                    GrowthStandards.Metric.valueOf(entry.arguments?.getString("metric").orEmpty())
                }.getOrDefault(GrowthStandards.Metric.WEIGHT_FOR_AGE)
                val sample = entry.arguments?.getBoolean("sample") ?: false
                val child = activeChild
                val latestSession = session ?: child?.let { vm.latestFor(it.id) }
                val childSessions = sessions.filter { it.childId == child?.id }
                val realAge = latestSession?.ageMonthsAtAssessment
                    ?: child?.let { AgeCalculator.assessmentMonths(it) }
                    ?: 0f
                val ageMonths = if (sample && realAge <= 0f) {
                    SampleGrowthData.FALLBACK_AGE_MONTHS
                } else {
                    realAge
                }
                val seed = child?.id?.hashCode() ?: 0
                val sampleMeasurement = if (sample) {
                    SampleGrowthData.latestMeasurement(child?.sex ?: Sex.MALE, ageMonths, seed)
                } else {
                    null
                }
                GrowthChartDetailScreen(
                    child = child,
                    metric = metric,
                    history = if (sample) {
                        SampleGrowthData.history(metric, child?.sex ?: Sex.MALE, ageMonths, seed)
                            .map { (month, value) -> GrowthPoint(month, value) }
                    } else {
                        growthHistoryFor(metric, childSessions, latestSession)
                    },
                    ageMonths = ageMonths,
                    muacMm = sampleMeasurement?.muacMm ?: latestSession?.growth?.muacMm,
                    isSample = sample,
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}

/** Wraps a main destination with the bottom navigation bar. */
@Composable
private fun MainShell(
    nav: NavHostController,
    route: String,
    title: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    val backStackEntry by nav.currentBackStackEntryAsState()
    val current = backStackEntry?.destination?.route ?: route
    PoshanScaffold(
        title = title ?: "",
        actions = actions,
        bottomBar = {
            PoshanBottomBar(
                items = bottomItems,
                // Children no longer has a tab, so keep Home lit while it is open rather
                // than leaving the bar with nothing selected.
                selectedRoute = if (bottomItems.any { it.route == current }) current else Routes.HOME,
                onSelect = { target ->
                    if (target != current) {
                        nav.navigate(target) {
                            popUpTo(Routes.HOME) { inclusive = target == Routes.HOME }
                            launchSingleTop = true
                        }
                    }
                },
            )
        },
    ) { padding ->
        androidx.compose.foundation.layout.Column(Modifier.padding(padding)) {
            content()
        }
    }
}

private fun categoryOf(name: String?): AssessmentCategory =
    AssessmentCategory.entries.firstOrNull { it.name == name } ?: AssessmentCategory.GROSS_MOTOR

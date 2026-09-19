package com.teamposhan.biomed.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.teamposhan.biomed.ui.components.GuidedStepsScreen
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.teamposhan.biomed.R
import com.teamposhan.biomed.data.model.AttentionResult
import com.teamposhan.biomed.data.model.AttentionTaskType
import com.teamposhan.biomed.data.model.RiskBand
import com.teamposhan.biomed.domain.age.AgeCalculator
import com.teamposhan.biomed.domain.attention.AttentionBand
import com.teamposhan.biomed.domain.attention.AttentionInstructions
import com.teamposhan.biomed.domain.attention.AttentionMetrics
import com.teamposhan.biomed.domain.attention.AttentionPlan
import com.teamposhan.biomed.domain.attention.AttentionProtocol
import com.teamposhan.biomed.domain.attention.AttentionRecorder
import com.teamposhan.biomed.domain.attention.AttentionSegment
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.compose.foundation.clickable
import androidx.compose.runtime.saveable.rememberSaveable
import com.teamposhan.biomed.domain.attention.CaptureQuality
import com.teamposhan.biomed.domain.attention.FaceTrackingFrame
import com.teamposhan.biomed.domain.attention.TrackingQuality
import com.teamposhan.biomed.domain.attention.TrackingQualityMeter
import com.teamposhan.biomed.domain.attention.toGazeSample
import com.teamposhan.biomed.domain.attention.StimulusAssets
import androidx.compose.foundation.layout.fillMaxHeight
import com.teamposhan.biomed.domain.attention.FaceGazeAnalyzer
import com.teamposhan.biomed.domain.attention.GazeSample
import com.teamposhan.biomed.ui.components.Gap
import androidx.compose.material.icons.filled.Lock
import com.teamposhan.biomed.ui.components.GuideArt
import com.teamposhan.biomed.ui.components.MicroIcon
import com.teamposhan.biomed.ui.components.InfoBanner
import com.teamposhan.biomed.ui.components.LockLandscape
import com.teamposhan.biomed.ui.components.PoshanCard
import com.teamposhan.biomed.ui.components.PoshanPrimaryButton
import com.teamposhan.biomed.ui.components.PoshanTonalButton
import com.teamposhan.biomed.ui.components.PoshanProgress
import com.teamposhan.biomed.ui.components.RiskChip
import com.teamposhan.biomed.ui.components.SectionLabel
import com.teamposhan.biomed.ui.components.StatTile
import com.teamposhan.biomed.ui.theme.AccentViolet
import com.teamposhan.biomed.ui.theme.accentColors
import com.teamposhan.biomed.ui.theme.riskColors
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

// ---------------------------------------------------------------------------
// Intro with full setup instructions
// ---------------------------------------------------------------------------

@Composable
fun AttentionIntroScreen(childName: String, ageMonths: Float, onStart: () -> Unit) {
    val plan = remember(ageMonths) { AttentionProtocol.build(ageMonths) }
    val steps = remember(plan) { AttentionInstructions.steps(plan) }
    val accents = accentColors

    // The setup walkthrough sits between this page and the camera rather than beside it.
    // Reading a list of conditions is not the same as being shown each one, and every item
    // in it is a condition the measurement is only valid under.
    var showGuide by remember { mutableStateOf(false) }
    if (showGuide) {
        GuidedStepsScreen(
            title = "Before you start",
            steps = GuideContent.module2,
            why = GuideContent.whyModule2,
            finishLabel = "Start the check",
            onFinish = onStart,
            onExit = { showGuide = false },
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            GuideArt("hero_module_2", ratio = 1.9f)
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.attention_intro_title), style = MaterialTheme.typography.headlineSmall)
            Gap(4)
            Text(
                "$childName Â· ${AgeCalculator.format(ageMonths)} Â· ${plan.band.label}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(14.dp))

            if (!plan.gazeEnabled) {
                InfoBanner(
                    stringResource(R.string.attention_movement_task_desc),
                    container = accents.amber,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(14.dp))
            }

            PoshanCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Duration", "${(plan.totalMs / 60_000f).let { if (it < 1f) "<1" else "%.0f".format(it) }} min", Modifier.weight(1f))
                    StatTile("Distance", "${plan.distanceCm} cm", Modifier.weight(1f))
                    StatTile("Trials", "${plan.pairTrials + plan.nameCalls + plan.disengagementTrials}", Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("Before you start")
            steps.forEachIndexed { i, step ->
                StepCard(index = i + 1, title = step.title, body = step.body)
                Gap(10)
            }

            Spacer(Modifier.height(8.dp))
            SectionLabel("What we measure")
            listOf(
                stringResource(R.string.attention_measure_engagement) to stringResource(R.string.attention_measure_engagement_desc),
                stringResource(R.string.attention_measure_social) to stringResource(R.string.attention_measure_social_desc),
                stringResource(R.string.attention_measure_sustained) to stringResource(R.string.attention_measure_sustained_desc),
                stringResource(R.string.attention_measure_blink) to stringResource(R.string.attention_measure_blink_desc),
            ).forEach { (t, d) ->
                PoshanCard(modifier = Modifier.padding(bottom = 10.dp), contentPadding = PaddingValues(14.dp)) {
                    Text(t, style = MaterialTheme.typography.titleSmall)
                    Text(d, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            // Stated on the intro, not only inside the walkthrough: a caregiver deciding
            // whether to point a camera at their child needs this before they agree, not
            // after they have started.
            PoshanCard(contentPadding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MicroIcon(
                        artKey = "micro_privacy",
                        fallback = Icons.Filled.Lock,
                        size = 24,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "No photo or video of your child is kept. The camera only works out " +
                            "which way the eyes are pointing.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            InfoBanner(stringResource(R.string.screening_not_diagnosis))
        }
        Box(Modifier.padding(20.dp)) {
            PoshanPrimaryButton(
                "Show me how, then start",
                onClick = { showGuide = true },
                icon = Icons.Filled.CameraAlt,
            )
        }
    }
}

@Composable
private fun StepCard(index: Int, title: String, body: String) {
    PoshanCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(30.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("$index", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun segmentLabelRes(label: String): Int = when (label) {
    "Attention" -> R.string.prompt_steady
    "Looking" -> R.string.attention_task_looking
    "Name call" -> R.string.attention_task_name
    "Shifting" -> R.string.attention_task_shift
    else -> R.string.attention_movement_task
}

// ---------------------------------------------------------------------------
// Session
// ---------------------------------------------------------------------------

@Composable
fun AttentionSessionScreen(
    childName: String,
    ageMonths: Float,
    onComplete: (AttentionResult, AttentionMetrics) -> Unit,
    onBack: () -> Unit,
) {
    // Landscape for the duration of the session. See LockLandscape: in portrait each half
    // of the paired stimulus subtends under 5 degrees, which is below what infant face
    // processing needs; landscape roughly doubles it.
    LockLandscape()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val plan = remember(ageMonths) { AttentionProtocol.build(ageMonths) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }

    if (!hasPermission) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            InfoBanner(stringResource(R.string.attention_camera_permission))
            Gap(16)
            PoshanPrimaryButton(stringResource(R.string.attention_grant_camera)) {
                launcher.launch(Manifest.permission.CAMERA)
            }
        }
        return
    }

    // Bumping the key rebuilds the recorder and every derived counter, which is how a
    // stopped session is restarted cleanly: a child who fussed through the first attempt
    // needs a fresh run, not a partial record stitched onto the old one.
    var sessionKey by remember { mutableStateOf(0) }
    val recorder = remember(sessionKey) { AttentionRecorder() }
    var startAt by remember(sessionKey) { mutableStateOf(0L) }
    var elapsed by remember(sessionKey) { mutableStateOf(0L) }
    var sample by remember(sessionKey) { mutableStateOf<GazeSample?>(null) }
    var running by remember(sessionKey) { mutableStateOf(true) }

    // Hoisted so the diagnostic toggle can live above the Stop button, where it is
    // reachable, instead of under the caregiver prompt card that overlays the camera view.
    // Camera drawer starts open: a session that silently tracks nothing looks exactly
    // like a session where the child never looked, and the operator must be able to tell
    // those apart before the trials begin.
    var cameraDrawerOpen by rememberSaveable { mutableStateOf(true) }
    var devMode by rememberSaveable { mutableStateOf(false) }
    // Each overlay layer toggles independently: the tessellation is the heaviest to draw and
    // the most visually noisy, so it is often the one to switch off while checking whether
    // the iris or the pose axes are tracking correctly.
    var layerTessellation by rememberSaveable { mutableStateOf(true) }
    var layerContours by rememberSaveable { mutableStateOf(true) }
    var layerIris by rememberSaveable { mutableStateOf(true) }
    var layerPose by rememberSaveable { mutableStateOf(true) }
    var layerGaze by rememberSaveable { mutableStateOf(true) }
    var layerHud by rememberSaveable { mutableStateOf(true) }
    var devFrame by remember { mutableStateOf<FaceTrackingFrame?>(null) }
    var devQuality by remember { mutableStateOf(CaptureQuality()) }
    var devTracking by remember { mutableStateOf(TrackingQuality(null, null, null, 0)) }
    var spokenNameCalls by remember(sessionKey) { mutableStateOf(setOf<Int>()) }
    var markedDisengage by remember(sessionKey) { mutableStateOf(setOf<Int>()) }

    // Text-to-speech for the response-to-name trials.
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { }
        tts = engine
        onDispose { engine.stop(); engine.shutdown() }
    }

    val segment = plan.segmentAt(elapsed)

    fun finishSession() {
        running = false
        val metrics = recorder.finish()
        onComplete(metrics.toAttentionResult(), metrics)
    }

    // After the gaze trials the child plays a short bubble-popping game. This is the touch
    // modality used by SenseToKnow and adds a motor/response-speed signal to the screen.
    var gameActive by remember { mutableStateOf(false) }
    var gameTargets by remember { mutableStateOf(0) }
    var gameShown by remember { mutableStateOf(0) }

    // Clock
    LaunchedEffect(sessionKey) {
        startAt = System.currentTimeMillis()
        while (running && !gameActive) {
            elapsed = System.currentTimeMillis() - startAt
            if (elapsed >= plan.totalMs) {
                gameActive = true
            }
            delay(60)
        }
    }

    // Game clock â€” ends after 8 targets or 25 seconds.
    LaunchedEffect(gameActive) {
        if (!gameActive) return@LaunchedEffect
        val gameStart = System.currentTimeMillis()
        while (running && gameActive) {
            if (gameTargets >= 8 || System.currentTimeMillis() - gameStart > 25_000L) {
                gameActive = false
            }
            delay(100)
        }
        if (running) finishSession()
    }

    // Side effects when a segment begins
    LaunchedEffect(segment) {
        // Close the previous trial's validity window before opening a new one.
        recorder.endTrial()
        when (val s = segment) {
            is AttentionSegment.Pair -> {
                recorder.beginTrial(100 + s.trialIndex)
            }
            is AttentionSegment.GazeFollow -> {
                recorder.beginTrial(200 + s.trialIndex)
                recorder.markGazeFollowCue(s.cueOnLeft)
            }
            is AttentionSegment.NameCall -> {
                // Opened outside the dedup guard, and on every entry to the segment: the
                // trial window is about data quality for this stretch of time, which applies
                // whether or not the name has already been spoken once.
                recorder.beginTrial(300 + s.index)
                if (!spokenNameCalls.contains(s.index)) {
                    spokenNameCalls = spokenNameCalls + s.index
                    tts?.speak(childName, TextToSpeech.QUEUE_FLUSH, null, "name-${s.index}")
                    recorder.markNameCall()
                }
            }
            is AttentionSegment.Disengage -> {
                recorder.beginTrial(400 + s.trialIndex)
                if (!markedDisengage.contains(s.trialIndex)) {
                    markedDisengage = markedDisengage + s.trialIndex
                    // Target appears after the centre phase.
                    delay(2_500)
                    recorder.markDisengageTarget(s.targetOnLeft)
                }
            }
            else -> Unit
        }
    }

    fun finishSessionUnused() = Unit

    Column(Modifier.fillMaxSize()) {
        // A running header while the session is live.
        //
        // The screen is otherwise deliberately bare — the child is meant to be looking at
        // the stimulus, not at chrome — so the operator previously had no idea which module
        // they were in or how far through it. This is small, dim and pinned to the very top
        // edge, where an adult can read it and a child seated at arm's length will not be
        // drawn to it.
        // Which sub-module is running, which trial within it, and how far through.
        //
        // The header used to show only the segment label, which sits on "Looking" for the
        // full eleven seconds of a trial and then again for the next one. With four to six
        // consecutive pair trials that reads as frozen — the operator cannot tell a running
        // session from a hung one. The trial counter and the bar both move continuously, so
        // progress is always visible.
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    subModuleName(segment),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                trialCounter(segment, plan)?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Gap(4)
            LinearProgressIndicator(
                progress = {
                    if (plan.totalMs <= 0) 0f
                    else (elapsed.toFloat() / plan.totalMs).coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
            if (gameActive) {
                BubbleGame(
                    onTargetShown = {
                        gameShown++
                        recorder.markTouchTargetShown()
                    },
                    onTargetPopped = { latency ->
                        gameTargets++
                        recorder.recordTouchPop(latency)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                CameraPreview(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    devMode = devMode,
                    layers = MeshLayers(
                        tessellation = layerTessellation,
                        contours = layerContours,
                        iris = layerIris,
                        poseAxes = layerPose,
                        gaze = layerGaze,
                        hud = layerHud,
                    ),
                    onFrame = { f -> devFrame = f },
                    onQuality = { q, t -> devQuality = q; devTracking = t },
                    onSample = { s -> sample = s },
                )
            } else if (plan.gazeEnabled) {
                // Order matters. The stimulus fills the screen and the camera sits above it
                // in a small drawer: previously the preview was drawn first and the
                // full-bleed stimulus covered it completely, so the operator could never see
                // whether a face was being tracked.
                //
                // In developer mode the preview goes full screen instead, because the mesh
                // overlay is unreadable at thumbnail size.
                StimulusStage(
                    plan = plan,
                    segment = segment,
                    elapsed = elapsed,
                    modifier = Modifier.fillMaxSize(),
                )

                val meshLayers = MeshLayers(
                    tessellation = layerTessellation,
                    contours = layerContours,
                    iris = layerIris,
                    poseAxes = layerPose,
                    gaze = layerGaze,
                    hud = layerHud,
                )

                // A single camera composable, hoisted so the analyzer is built once and
                // survives the drawer opening, the sheet rising and devMode toggling.
                // Re-mounting it would restart MediaPipe mid-session and lose the frame
                // counters the validity gate depends on.
                val livePreview: @Composable () -> Unit = {
                    CameraPreview(
                        context = context,
                        lifecycleOwner = lifecycleOwner,
                        devMode = devMode,
                        layers = meshLayers,
                        onFrame = { f -> devFrame = f },
                        onQuality = { q, t -> devQuality = q; devTracking = t },
                        onSample = { s ->
                            sample = s
                            recorder.onSample(s, plan.aoiAt(elapsed))
                        },
                    )
                }

                if (!devMode) {
                    CameraDrawer(
                        expanded = cameraDrawerOpen,
                        quality = devQuality,
                        faceDetected = sample?.facePresent == true,
                        onToggle = { cameraDrawerOpen = !cameraDrawerOpen },
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                        preview = livePreview,
                    )
                }

                TrackingSheet(
                    visible = devMode,
                    layers = meshLayers,
                    onToggleLayer = { which ->
                        when (which) {
                            "mesh" -> layerTessellation = !layerTessellation
                            "contours" -> layerContours = !layerContours
                            "iris" -> layerIris = !layerIris
                            "pose" -> layerPose = !layerPose
                            "gaze" -> layerGaze = !layerGaze
                            "hud" -> layerHud = !layerHud
                        }
                    },
                    onClose = { devMode = false },
                    modifier = Modifier.align(Alignment.BottomCenter),
                    preview = livePreview,
                )
                GazeDot(sample = sample, modifier = Modifier.fillMaxSize())
            } else {
                MovementObservationStage(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    onSample = { s ->
                        sample = s
                        recorder.onSample(s, null)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Top HUD
            PoshanCard(
                modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                container = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
                contentPadding = PaddingValues(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            segment?.label?.let { stringResource(segmentLabelRes(it)) } ?: stringResource(R.string.finishing),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(Modifier.height(6.dp))
                        PoshanProgress(value = plan.progress(elapsed), showPercent = false)
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        "${((plan.totalMs - elapsed).coerceAtLeast(0) / 1000).toInt()}s",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Caregiver prompt.
            //
            // Hidden while the tracking sheet is up: both sit at the bottom of the same Box,
            // so the prompt and the old face/frames card were drawing straight over the
            // sheet. The face/frames card is gone entirely — the camera drawer already
            // carries detection state and the usable-frame percentage, and showing the same
            // thing twice in two different formats was the reason the bottom of this screen
            // felt crowded.
            val prompt = caregiverPrompt(segment)
            if (prompt != null && !devMode) {
                PoshanCard(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                    container = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f),
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
        Column(Modifier.padding(20.dp)) {
            DevTrackingControls(
                devMode = devMode,
                onToggleDev = { devMode = !devMode },
                modifier = Modifier.padding(bottom = 10.dp),
            )
            if (running) {
                PoshanPrimaryButton(text = stringResource(R.string.attention_stop)) { finishSession() }
            } else {
                PoshanPrimaryButton(text = stringResource(R.string.attention_restart)) {
                    devTracking = TrackingQuality(null, null, null, 0)
                    devFrame = null
                    sessionKey++
                }
                Gap(10)
                PoshanTonalButton(text = stringResource(R.string.continue_label)) { finishSession() }
            }
        }
    }
}

@Composable
private fun caregiverPrompt(segment: AttentionSegment?): String? = when (segment) {
    is AttentionSegment.NameCall -> stringResource(R.string.prompt_call_name)
    is AttentionSegment.Disengage -> stringResource(R.string.prompt_stay_quiet)
    is AttentionSegment.Getter -> stringResource(R.string.prompt_steady)
    else -> null
}

private fun AttentionMetrics.toAttentionResult(): AttentionResult {
    val band = when {
        inconclusive -> RiskBand.MONITOR
        engagementIndex >= 0.65f -> RiskBand.ON_TRACK
        engagementIndex >= 0.45f -> RiskBand.MONITOR
        engagementIndex >= 0.30f -> RiskBand.REVIEW
        else -> RiskBand.PRIORITY
    }
    return AttentionResult(
        taskType = AttentionTaskType.PREFERENTIAL_LOOKING,
        validFrameRatio = validFrameRatio,
        onScreenRatio = onScreenRatio,
        socialAttentionRatio = socialPreference,
        blinkRatePerMin = blinkRatePerMin,
        disengagementLatencyMs = disengagementLatencyMs,
        responseToNameRatio = responseToNameRatio,
        engagementIndex = engagementIndex,
        band = band,
        inconclusive = inconclusive,
        durationMs = durationMs,

        gazeFollowRatio = gazeFollowRatio,
        gazeFollowLatencyMs = gazeFollowLatencyMs,
        geometricPreference = geometricPreference,
        responseToNameLatencyMs = responseToNameLatencyMs,
        disengagementRatio = disengagementRatio,

        confidence = confidence,
        validTrialCount = validTrialCount,
        seenTrialCount = seenTrialCount,
        sustainedAttention = sustainedAttention,
        meanFixationMs = meanFixationMs,
        longestFixationMs = longestFixationMs,
        headMovementRate = headMovementRate,

        touchTargetsShown = touchTargetsShown,
        touchTargetsPopped = touchTargetsPopped,
        meanTouchLatencyMs = meanTouchLatencyMs,
    )
}

// ---------------------------------------------------------------------------
// Stimulus stage
// ---------------------------------------------------------------------------

@Composable
private fun StimulusStage(plan: AttentionPlan, segment: AttentionSegment?, elapsed: Long, modifier: Modifier = Modifier) {
    // Real artwork wherever it exists, with the drawn version kept underneath as a
    // fallback. The drawn stimuli were adequate placeholders, but the literature is clear
    // that what discriminates is rich social content — a character attending to something,
    // with the thing they are attending to visible — which a few circles cannot supply.
    ImageStimulus(plan = plan, segment = segment, elapsed = elapsed, modifier = modifier)
}

@Composable
private fun ImageStimulus(
    plan: AttentionPlan,
    segment: AttentionSegment?,
    elapsed: Long,
    modifier: Modifier = Modifier,
) {
    val ageMonths = plan.band.minMonths
    Box(modifier) {
        when (segment) {
            is AttentionSegment.Getter -> {
                // Cycled by elapsed time so consecutive getters differ: a target that varies
                // between appearances holds infant attention far better than a repeat.
                val index = (elapsed / 3_000L).toInt()
                StimulusImage(StimulusAssets.getter(index), "Attention getter")
            }

            is AttentionSegment.Pair -> {
                // Two panels at the outer edges. Placement is the measurement here: the
                // wider the separation, the more angular distance there is between the two
                // AOIs for the gaze estimator to resolve.
                Row(Modifier.fillMaxSize()) {
                    val socialFirst = segment.socialOnLeft
                    val social = StimulusAssets.social(segment.condition)
                    val geometric = StimulusAssets.geometric(segment.condition)
                    StimulusImage(
                        if (socialFirst) social else geometric,
                        if (socialFirst) "Social scene" else "Geometric pattern",
                        Modifier.weight(1f).fillMaxHeight(),
                    )
                    StimulusImage(
                        if (socialFirst) geometric else social,
                        if (socialFirst) "Geometric pattern" else "Social scene",
                        Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }

            is AttentionSegment.GazeFollow -> {
                StimulusImage(StimulusAssets.gazeCue(segment.cueOnLeft), "Gaze cue")
            }

            is AttentionSegment.NameCall -> {
                StimulusImage(StimulusAssets.responseToName, "Neutral scene")
            }

            is AttentionSegment.MovementObservation -> {
                StimulusImage(
                    StimulusAssets.infantStimulus(ageMonths, (elapsed / 8_000L).toInt()),
                    "Infant stimulus",
                )
            }

            // Disengage has no generated asset; the drawn version still serves it.
            else -> DrawnStimulusStage(plan, segment, elapsed, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun StimulusImage(
    @androidx.annotation.DrawableRes res: Int,
    description: String,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    Image(
        painter = painterResource(res),
        contentDescription = description,
        modifier = modifier,
        // Crop rather than fit: letterbox bars change the effective stimulus size, and the
        // AOI geometry assumes the panel is filled.
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
    )
}

@Composable
private fun DrawnStimulusStage(plan: AttentionPlan, segment: AttentionSegment?, elapsed: Long, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "stim")
    val bob by transition.animateFloat(
        initialValue = -0.05f, targetValue = 0.05f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Reverse),
        label = "bob",
    )
    val spin by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val accents = accentColors
    val primary = MaterialTheme.colorScheme.primary
    val violet = MaterialTheme.colorScheme.tertiary

    Canvas(modifier) {
        when (segment) {
            is AttentionSegment.Getter -> {
                val centre = Offset(size.width / 2f, size.height / 2f)
                drawCircle(color = primary.copy(alpha = 0.14f), radius = size.minDimension * 0.20f * pulse, center = centre)
                drawCircle(color = primary.copy(alpha = 0.26f), radius = size.minDimension * 0.13f * pulse, center = centre)
                drawCircle(color = primary, radius = size.minDimension * 0.055f, center = centre)
            }

            is AttentionSegment.Pair -> {
                val pad = size.width * 0.05f
                val top = size.height * 0.30f
                val bottom = size.height * 0.62f
                val leftRect = Rect(pad, top, size.width / 2f - pad, bottom)
                val rightRect = Rect(size.width / 2f + pad, top, size.width - pad, bottom)

                drawRoundRect(accents.mint, leftRect.topLeft, Size(leftRect.width, leftRect.height), CornerRadius(44f, 44f))
                drawRoundRect(accents.violet, rightRect.topLeft, Size(rightRect.width, rightRect.height), CornerRadius(44f, 44f))

                val socialRect = if (segment.socialOnLeft) leftRect else rightRect
                val objectRect = if (segment.socialOnLeft) rightRect else leftRect
                drawFace(socialRect, bob)
                drawObject(objectRect, spin, violet = Color(0xFF5A4BC4))
            }

            is AttentionSegment.NameCall -> {
                val centre = Offset(size.width / 2f, size.height / 2f)
                drawCircle(color = primary.copy(alpha = 0.12f), radius = size.minDimension * 0.22f * pulse, center = centre)
                drawCircle(color = accents.amber, radius = size.minDimension * 0.15f, center = centre)
            }

            is AttentionSegment.Disengage -> {
                val targetPhase = elapsed - segment.startMs > 2_500L
                val centre = Offset(size.width / 2f, size.height / 2f)
                if (!targetPhase) {
                    drawCircle(color = primary, radius = size.minDimension * 0.10f, center = centre)
                    drawCircle(color = primary.copy(alpha = 0.18f), radius = size.minDimension * 0.16f, center = centre, style = Stroke(width = 6f))
                } else {
                    drawCircle(color = primary.copy(alpha = 0.25f), radius = size.minDimension * 0.08f, center = centre)
                    val tx = if (segment.targetOnLeft) size.width * 0.16f else size.width * 0.84f
                    val ty = size.height * 0.30f
                    drawCircle(color = accents.coral, radius = size.minDimension * 0.075f * pulse, center = Offset(tx, ty))
                    drawCircle(color = Color(0xFFC4462A), radius = size.minDimension * 0.035f, center = Offset(tx, ty))
                }
            }

            is AttentionSegment.GazeFollow -> {
                // A face turns its gaze (and head) to one side, then a target appears there.
                val targetPhase = elapsed - segment.startMs > 1_500L
                val dir = if (segment.cueOnLeft) -1f else 1f
                val faceCentre = Offset(size.width / 2f + dir * size.width * 0.10f, size.height * 0.46f)
                val r = size.minDimension * 0.16f
                drawCircle(color = Color(0xFFFFD9A8), radius = r, center = faceCentre)
                drawCircle(color = Color(0xFF2B2B2B), radius = r * 0.11f, center = Offset(faceCentre.x - r * 0.35f + dir * r * 0.16f, faceCentre.y - r * 0.16f))
                drawCircle(color = Color(0xFF2B2B2B), radius = r * 0.11f, center = Offset(faceCentre.x + r * 0.35f + dir * r * 0.16f, faceCentre.y - r * 0.16f))
                drawArc(
                    color = Color(0xFF8C3B2B),
                    startAngle = 20f, sweepAngle = 140f, useCenter = false,
                    topLeft = Offset(faceCentre.x - r * 0.4f, faceCentre.y + r * 0.05f),
                    size = Size(r * 0.8f, r * 0.6f),
                    style = Stroke(width = r * 0.10f),
                )
                if (targetPhase) {
                    val tx = if (segment.cueOnLeft) size.width * 0.12f else size.width * 0.88f
                    val ty = size.height * 0.40f
                    drawRoundRect(
                        color = accents.amber,
                        topLeft = Offset(tx - size.minDimension * 0.07f, ty - size.minDimension * 0.07f),
                        size = Size(size.minDimension * 0.14f, size.minDimension * 0.14f),
                        cornerRadius = CornerRadius(18f, 18f),
                    )
                    drawCircle(color = Color(0xFFB87414), radius = size.minDimension * 0.035f * pulse, center = Offset(tx, ty))
                }
            }

            is AttentionSegment.MovementObservation -> Unit
            null -> Unit
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFace(rect: Rect, bob: Float) {
    val centre = Offset(rect.center.x, rect.center.y + rect.height * bob * 0.5f)
    val r = minOf(rect.width, rect.height) * 0.30f
    drawCircle(color = Color(0xFFFFD9A8), radius = r, center = centre)
    drawCircle(color = Color(0xFF2B2B2B), radius = r * 0.10f, center = Offset(centre.x - r * 0.34f, centre.y - r * 0.20f))
    drawCircle(color = Color(0xFF2B2B2B), radius = r * 0.10f, center = Offset(centre.x + r * 0.34f, centre.y - r * 0.20f))
    drawArc(
        color = Color(0xFF8C3B2B),
        startAngle = 20f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(centre.x - r * 0.45f, centre.y - r * 0.05f),
        size = Size(r * 0.9f, r * 0.7f),
        style = Stroke(width = r * 0.10f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawObject(rect: Rect, spin: Float, violet: Color) {
    val centre = Offset(rect.center.x, rect.center.y)
    val r = minOf(rect.width, rect.height) * 0.26f
    rotate(spin, pivot = centre) {
        val path = Path()
        val sides = 5
        for (i in 0 until sides) {
            val angle = (i.toFloat() / sides) * 2f * Math.PI
            val px = centre.x + r * kotlin.math.cos(angle).toFloat()
            val py = centre.y + r * kotlin.math.sin(angle).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        drawPath(path, color = violet)
    }
}

@Composable
private fun GazeDot(sample: GazeSample?, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val s = sample ?: return@Canvas
        if (!s.facePresent) return@Canvas
        // A GazeSample defaults to (0, 0) before the estimator has produced anything, and
        // that default was being drawn — a marker parked in the corner the moment the screen
        // opened, before any face had been found. Exact zeros are the sentinel for "not
        // computed yet", so skip them; a genuine gaze at the very corner is not measurable
        // anyway and is discarded by the AOI thresholds regardless.
        if (s.gazeX == 0f && s.gazeY == 0f) return@Canvas
        val cx = size.width * s.gazeX
        val cy = size.height * s.gazeY
        drawCircle(color = primary.copy(alpha = 0.16f), radius = 40f, center = Offset(cx, cy))
        drawCircle(color = primary, radius = 12f, center = Offset(cx, cy))
        drawCircle(color = Color.White, radius = 4.5f, center = Offset(cx, cy))
    }
}

@Composable
private fun MovementObservationStage(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onSample: (GazeSample) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        CameraPreview(context = context, lifecycleOwner = lifecycleOwner, onSample = onSample)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PoshanCard(
                modifier = Modifier.padding(24.dp),
                container = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f),
            ) {
                Text(
                    stringResource(R.string.attention_movement_task_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Result
// ---------------------------------------------------------------------------

@Composable
fun AttentionResultScreen(result: AttentionResult, metrics: AttentionMetrics?, onContinue: () -> Unit) {
    val c = riskColors
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(stringResource(R.string.attention_result), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(14.dp))
            PoshanCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (result.inconclusive) stringResource(R.string.attention_inconclusive) else stringResource(R.string.attention_engagement),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "${(result.engagementIndex * 100).toInt()}%",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    RiskChip(result.band)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(stringResource(R.string.attention_onscreen), "${(result.onScreenRatio * 100).toInt()}%", Modifier.weight(1f))
                StatTile(stringResource(R.string.attention_measure_social), "${(result.socialAttentionRatio * 100).toInt()}%", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(stringResource(R.string.attention_blink), "${result.blinkRatePerMin.toInt()}/min", Modifier.weight(1f))
                StatTile(
                    stringResource(R.string.attention_frames_valid),
                    "${(result.validFrameRatio * 100).toInt()}%",
                    Modifier.weight(1f),
                    accent = if (result.validFrameRatio < 0.55f) c.monitor else c.low,
                )
            }

            // Everything the session produced, read from the stored result rather than
            // from the live metrics object.
            //
            // `metrics` only exists in the moments right after a session finishes, so a
            // result opened later from the report showed four tiles and nothing else.
            // AttentionResult now carries the full set, so the same screen works whether it
            // is reached from the session or from the child's history.
            Spacer(Modifier.height(18.dp))
            SectionLabel("Looking preference")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    "Social side",
                    "${(result.socialAttentionRatio * 100).toInt()}%",
                    Modifier.weight(1f),
                )
                // Not the complement of the social share: time on neither side counts
                // towards neither, so the two do not sum to 100.
                StatTile(
                    "Geometric side",
                    "${(result.geometricPreference * 100).toInt()}%",
                    Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("Responding")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    "Turned to name",
                    result.responseToNameRatio?.let { "${(it * 100).toInt()}%" } ?: "not measured",
                    Modifier.weight(1f),
                )
                StatTile(
                    "Name latency",
                    result.responseToNameLatencyMs?.let { "${it.toInt()} ms" } ?: "-",
                    Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    "Followed a look",
                    result.gazeFollowRatio?.let { "${(it * 100).toInt()}%" } ?: "not measured",
                    Modifier.weight(1f),
                )
                StatTile(
                    "Follow latency",
                    result.gazeFollowLatencyMs?.let { "${it.toInt()} ms" } ?: "-",
                    Modifier.weight(1f),
                )
            }
            if (result.disengagementLatencyMs != null || result.disengagementRatio != null) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        "Shift latency",
                        result.disengagementLatencyMs?.let { "${it.toInt()} ms" } ?: "-",
                        Modifier.weight(1f),
                    )
                    StatTile(
                        "Shifted when asked",
                        result.disengagementRatio?.let { "${(it * 100).toInt()}%" } ?: "-",
                        Modifier.weight(1f),
                    )
                }
            }

            if (result.pursuitCorrelation != null) {
                Spacer(Modifier.height(18.dp))
                SectionLabel("Following a moving target")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        "Eye-target match",
                        "%.2f".format(result.pursuitCorrelation),
                        Modifier.weight(1f),
                    )
                    StatTile(
                        "Time on target",
                        result.pursuitTrackingFraction?.let { "${(it * 100).toInt()}%" } ?: "-",
                        Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("How the eyes moved")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Blink rate", "${result.blinkRatePerMin.toInt()}/min", Modifier.weight(1f))
                StatTile(
                    "Mean fixation",
                    "${result.meanFixationMs} ms",
                    Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    "Longest fixation",
                    "${result.longestFixationMs} ms",
                    Modifier.weight(1f),
                )
                StatTile(
                    "Sustained attention",
                    "${(result.sustainedAttention * 100).toInt()}%",
                    Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    "Head movement",
                    "%.1f deg/s".format(result.headMovementRate),
                    Modifier.weight(1f),
                )
                StatTile(
                    "Session length",
                    "${result.durationMs / 1000} s",
                    Modifier.weight(1f),
                )
            }

            if (result.touchTargetsShown > 0) {
                Spacer(Modifier.height(18.dp))
                SectionLabel("Bubble game")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        "Popped",
                        "${result.touchTargetsPopped} of ${result.touchTargetsShown}",
                        Modifier.weight(1f),
                    )
                    StatTile(
                        "Mean reaction",
                        result.meanTouchLatencyMs?.let { "$it ms" } ?: "-",
                        Modifier.weight(1f),
                    )
                }
            }

            // Quality last, and never omitted: every number above is only worth as much as
            // the share of frames the camera could actually use.
            Spacer(Modifier.height(18.dp))
            SectionLabel("How good was the data")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    "Clear views of the face",
                    "${(result.validFrameRatio * 100).toInt()}%",
                    Modifier.weight(1f),
                    accent = if (result.validFrameRatio < 0.55f) c.monitor else c.low,
                )
                StatTile(
                    "Confidence",
                    "${(result.confidence * 100).toInt()}%",
                    Modifier.weight(1f),
                    accent = if (result.confidence < 0.5f) c.monitor else c.low,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    "Usable trials",
                    "${result.validTrialCount} of ${result.seenTrialCount}",
                    Modifier.weight(1f),
                )
                StatTile(
                    "Time looking at screen",
                    "${(result.onScreenRatio * 100).toInt()}%",
                    Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))
            PoshanCard {
                Text(stringResource(R.string.attention_how_to_read), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.attention_how_to_read_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            InfoBanner(stringResource(R.string.screening_not_diagnosis))
        }
        Box(Modifier.padding(20.dp)) {
            PoshanPrimaryButton(stringResource(R.string.continue_label), onClick = onContinue, icon = Icons.Filled.Check)
        }
    }
}

// ---------------------------------------------------------------------------
// Touch task â€” bubble popping (SenseToKnow-style motor / response-speed signal)
// ---------------------------------------------------------------------------

@Composable
private fun BubbleGame(
    onTargetShown: () -> Unit,
    onTargetPopped: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accents = accentColors
    val primary = MaterialTheme.colorScheme.primary
    var target by remember { mutableStateOf<Offset?>(null) }
    var shownAt by remember { mutableStateOf(0L) }
    var size by remember { mutableStateOf(Size(1f, 1f)) }

    LaunchedEffect(Unit) {
        while (true) {
            target = null
            delay(500)
            val r1 = Math.random().toFloat()
            val r2 = Math.random().toFloat()
            target = Offset(0.16f + r1 * 0.68f, 0.22f + r2 * 0.5f)
            shownAt = System.currentTimeMillis()
            onTargetShown()
            delay(2_600)
        }
    }

    val transition = rememberInfiniteTransition(label = "bubble")
    val pulse by transition.animateFloat(
        initialValue = 0.85f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "bubblePulse",
    )

    Box(
        modifier
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    val t = target ?: return@detectTapGestures
                    val centre = Offset(t.x * size.width, t.y * size.height)
                    val radius = minOf(size.width, size.height) * 0.13f
                    if ((tap - centre).getDistance() <= radius * 1.35f) {
                        onTargetPopped(System.currentTimeMillis() - shownAt)
                        target = null
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            size = this.size
            val t = target ?: return@Canvas
            val centre = Offset(t.x * this.size.width, t.y * this.size.height)
            val radius = minOf(this.size.width, this.size.height) * 0.13f * pulse
            drawCircle(AccentViolet.copy(alpha = 0.35f), radius = radius * 1.25f, center = centre)
            drawCircle(primary.copy(alpha = 0.85f), radius = radius, center = centre)
            drawCircle(Color.White.copy(alpha = 0.75f), radius = radius * 0.32f, center = Offset(centre.x - radius * 0.28f, centre.y - radius * 0.28f))
        }
        // Minimal instruction, top of the screen.
        Text(
            "Pop the bubbles!",
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 90.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}


@Composable
private fun CameraPreview(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    ageMonths: Float = 24f,
    devMode: Boolean = false,
    layers: MeshLayers = MeshLayers(),
    onFrame: (FaceTrackingFrame) -> Unit = {},
    onQuality: (CaptureQuality, TrackingQuality) -> Unit = { _, _ -> },
    onSample: (GazeSample) -> Unit,
) {
    // One dedicated thread: MediaPipe's GPU delegate must be created and used on the same
    // thread, and the analyzer builds itself lazily on first frame for exactly that reason.
    val executor = remember { Executors.newSingleThreadExecutor() }

    var frame by remember { mutableStateOf<FaceTrackingFrame?>(null) }
    var quality by remember { mutableStateOf(CaptureQuality()) }
    var tracking by remember { mutableStateOf(TrackingQuality(null, null, null, 0)) }
    val meter = remember { TrackingQualityMeter() }

    val analyzer = remember {
        FaceGazeAnalyzer(
            context = context,
            mirrored = true,
            ageMonths = ageMonths,
        ) { f ->
            // The screening pipeline and the developer overlay consume the SAME frame, so
            // the overlay cannot look right while the measurement is wrong.
            onSample(f.toGazeSample())
            meter.onFrame(f)
            frame = f
            onFrame(f)
        }
    }

    // Poll the analyzer's counters rather than emitting state from the camera thread on
    // every frame, which would recompose the whole tree 30 times a second.
    LaunchedEffect(devMode) {
        while (devMode) {
            quality = analyzer.quality
            tracking = meter.snapshot()
            onQuality(quality, tracking)
            kotlinx.coroutines.delay(250)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            analyzer.close()
            executor.shutdown()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    // Pinned resolution: left to the device default, inference cost and
                    // landmark precision vary silently across the fleet. The face mesh input
                    // is 256x256, so anything above VGA is wasted conversion work.
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .setResolutionSelector(
                            ResolutionSelector.Builder()
                                .setAspectRatioStrategy(
                                    AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
                                )
                                .setResolutionStrategy(
                                    ResolutionStrategy(
                                        android.util.Size(640, 480),
                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                                    )
                                )
                                .build()
                        )
                        .build()
                        .also { it.setAnalyzer(executor, analyzer) }
                    runCatching {
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                            analysis,
                        )
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
        )

        if (devMode) {
            FaceMeshOverlay(
                frame = frame,
                quality = quality,
                tracking = tracking,
                layers = layers,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Developer tracking controls.
 *
 * Rendered by the session screen directly above the Stop button rather than inside the
 * camera view: the caregiver prompt card is aligned to the bottom of the preview and was
 * covering these entirely.
 */
@Composable
private fun DevTrackingControls(
    devMode: Boolean,
    onToggleDev: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = com.teamposhan.biomed.ui.haptics.rememberAppHaptics()
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DevChip(
            label = if (devMode) "Tracking ON" else "Show tracking",
            active = devMode,
            onClick = { haptics.tick(); onToggleDev() },
        )
    }
}

/** One switchable chip per overlay layer, so any of them can be isolated. */
@Composable
private fun MeshLayerToggles(
    tessellation: Boolean,
    contours: Boolean,
    iris: Boolean,
    poseAxes: Boolean,
    gaze: Boolean,
    hud: Boolean,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = com.teamposhan.biomed.ui.haptics.rememberAppHaptics()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Overlay layers",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DevChip("Mesh", tessellation) { haptics.tick(); onToggle("mesh") }
            DevChip("Contours", contours) { haptics.tick(); onToggle("contours") }
            DevChip("Iris", iris) { haptics.tick(); onToggle("iris") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DevChip("Pose", poseAxes) { haptics.tick(); onToggle("pose") }
            DevChip("Gaze", gaze) { haptics.tick(); onToggle("gaze") }
            DevChip("Stats", hud) { haptics.tick(); onToggle("hud") }
        }
    }
}

@Composable
private fun DevChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (active) Color(0xE600E5FF) else MaterialTheme.colorScheme.surfaceContainerHighest
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            label,
            color = if (active) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}


/**
 * The sub-module name, in words an operator can act on.
 *
 * The segment's own label is deliberately terse for the child-facing screen; this is the
 * adult-facing version and says what is being measured right now.
 */
private fun subModuleName(segment: AttentionSegment?): String = when (segment) {
    is AttentionSegment.Getter -> "Getting attention"
    is AttentionSegment.Pair -> "Looking preference"
    is AttentionSegment.NameCall -> "Response to name"
    is AttentionSegment.Disengage -> "Shifting attention"
    is AttentionSegment.GazeFollow -> "Following a look"
    is AttentionSegment.MovementObservation -> "Watching movement"
    null -> "Finishing"
}

/**
 * "Trial 2 of 4" for the segments that repeat.
 *
 * Null for the ones that do not, rather than a misleading "1 of 1".
 */
private fun trialCounter(segment: AttentionSegment?, plan: AttentionPlan): String? = when (segment) {
    is AttentionSegment.Pair -> "Trial ${segment.trialIndex + 1} of ${plan.pairTrials}"
    is AttentionSegment.NameCall -> "Call ${segment.index + 1} of ${plan.nameCalls}"
    is AttentionSegment.Disengage ->
        "Trial ${segment.trialIndex + 1} of ${plan.disengagementTrials}"
    is AttentionSegment.GazeFollow ->
        "Trial ${segment.trialIndex + 1} of ${plan.gazeFollowTrials}"
    else -> null
}

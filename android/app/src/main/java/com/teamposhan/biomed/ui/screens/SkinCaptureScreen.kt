package com.teamposhan.biomed.ui.screens

import android.content.Context
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import com.teamposhan.biomed.domain.skin.StagePreviews
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.material.icons.filled.Layers
import androidx.compose.foundation.Image
import com.teamposhan.biomed.domain.skin.SkinType
import com.teamposhan.biomed.domain.skin.SkinReadingLock
import androidx.compose.ui.draw.clip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.teamposhan.biomed.domain.skin.SkinMeasurement
import com.teamposhan.biomed.domain.skin.SkinPatchSampler
import com.teamposhan.biomed.domain.skin.SkinSegmenter
import com.teamposhan.biomed.ui.components.Gap
import com.teamposhan.biomed.ui.components.InfoBanner
import com.teamposhan.biomed.ui.components.PoshanCard
import com.teamposhan.biomed.ui.components.PoshanOutlinedButton
import com.teamposhan.biomed.ui.components.PoshanPrimaryButton
import com.teamposhan.biomed.ui.haptics.rememberAppHaptics
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * Where the two sample patches sit on screen.
 *
 * Fixed rather than detected. A detector would drift frame to frame and the operator could
 * never tell which pixels produced the number; a fixed reticle the operator aims makes the
 * measurement repeatable and any failure obvious while it is happening.
 */
private val SKIN_RETICLE = SkinPatchSampler.Reticle(0.18f, 0.34f, 0.46f, 0.62f)
private val CARD_RETICLE = SkinPatchSampler.Reticle(0.56f, 0.38f, 0.84f, 0.58f)

/**
 * Skin-tone capture for the vitamin D module.
 *
 * Measures how strongly this child's skin filters UV-B, which sets how long outdoors is
 * needed for the same amount of vitamin D. It does **not** measure vitamin D.
 *
 * The reading recomputes on every frame while the operator aims, so a bad capture is visible
 * before it is saved rather than after.
 */
@OptIn(ExperimentalCamera2Interop::class)
@Composable
fun SkinCaptureScreen(
    onDone: (SkinMeasurement) -> Unit,
    onCancel: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = rememberAppHaptics()
    val executor = remember { Executors.newSingleThreadExecutor() }
    val context = LocalContext.current
    // Built once and reused: creating the interpreter costs far more than running it, and
    // rebuilding it per frame would stall the analysis thread.
    val segmenter = remember { SkinSegmenter(context) }
    var live by remember { mutableStateOf<SkinMeasurement?>(null) }
    var showWorking by remember { mutableStateOf(false) }
    var showStages by remember { mutableStateOf(false) }
    // Acquire-and-freeze, like camera focus. Whatever is on screen when a thumb lands is the
    // worst possible sample: the press itself shakes the phone.
    val lock = remember { SkinReadingLock() }
    var lockState by remember { mutableStateOf<SkinReadingLock.State>(
        SkinReadingLock.State.Waiting("Starting camera")
    ) }
    // Readings taken before exposure and white balance are frozen drift frame to frame, so
    // none of them may be saved.
    var locked by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            segmenter.close()
            executor.shutdown()
        }
    }

    // The most useful haptic in the app.
    //
    // Aiming at the inner arm of a child who will not hold still means watching the arm, not
    // the screen. A rising pulse the moment the reading becomes usable says "now" without
    // the operator having to look up — and a softer one if it slips away again, so a lost
    // frame is felt rather than discovered after pressing the button.
    val acquired = lockState as? SkinReadingLock.State.Locked
    LaunchedEffect(acquired != null) {
        if (acquired != null) haptics.completion(1f)
    }

    // A single mark when exposure and white balance freeze, so the wait has a felt end.
    LaunchedEffect(locked) { if (locked) haptics.click() }

    Column(Modifier.fillMaxSize()) {
      Column(
          Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
      ) {
        Gap(8)
        Text("Skin tone", style = MaterialTheme.typography.headlineSmall)
        Gap(4)
        Text(
            "Hold the grey card flat against the inner arm, in the shade, and fill both boxes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Gap(14)

        Box(
            Modifier
                .fillMaxWidth()
                // Locked to the analyser's 4:3 so preview and sampled pixels share one
                // coordinate space. Any other ratio crops the preview and the drawn reticle
                // stops matching the pixels actually read.
                .aspectRatio(3f / 4f),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        bindSkinCamera(
                            ctx = ctx,
                            lifecycleOwner = lifecycleOwner,
                            previewView = this,
                            executor = executor,
                            segmenter = segmenter,
                            onMeasurement = {
                                live = it
                                lockState = lock.offer(it)
                            },
                            onLocked = { locked = true },
                        )
                    }
                },
            )
            ReticleOverlay(live, acquired != null)
        }

        Gap(14)
        LockCard(state = lockState, locked = locked)
        Gap(12)
        // The pipeline, live. Each swatch is what the next stage actually receives, so a bad
        // white balance or a patch that is not skin is visible as a colour rather than only
        // as a refusal message.
        live?.let { FilterStages(it) }

        // The full picture-by-picture version, offered only once a reading has locked.
        // Before that the images change every frame and mean nothing; after it, they are the
        // exact pixels the saved number was computed from.
        if (acquired != null) {
            Gap(10)
            PoshanCard(
                onClick = { haptics.click(); showStages = true },
                container = MaterialTheme.colorScheme.secondaryContainer,
                contentPadding = PaddingValues(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "See the pictures behind this reading",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            "Each step, as an image",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
        Gap(12)
        // The working is a drawer rather than a section: it is long, it changes on every
        // frame, and someone aiming a camera at a child does not want it pushing the
        // shutter button off screen. It stays one tap away for anyone who wants it.
        PoshanCard(
            onClick = { haptics.click(); showWorking = true },
            container = MaterialTheme.colorScheme.secondaryContainer,
            contentPadding = PaddingValues(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Functions,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "See the working",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        Gap(10)
        InfoBanner(
            "This reads skin tone only. It tells us how long in the sun this child needs — " +
                "it cannot tell us how much vitamin D the child has.",
        )

        Gap(16)
      }
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Weighted for the same reason as the walkthrough: an unweighted button in a
            // Row fills the width and hides the one beside it.
            PoshanOutlinedButton(
                if (acquired != null) "Again" else "Cancel",
                modifier = Modifier.weight(1f),
                onClick = {
                    haptics.click()
                    if (acquired != null) {
                        lock.reset()
                        lockState = SkinReadingLock.State.Waiting("Ready")
                    } else {
                        onCancel()
                    }
                },
            )
            Box(Modifier.weight(1.6f)) {
                if (acquired != null) {
                    PoshanPrimaryButton(
                        "Save this reading",
                        onClick = { haptics.confirm(); onDone(acquired.measurement) },
                    )
                } else {
                    // Not merely disabled — there is nothing to save until the reading has
                    // settled, and the label says which of the two states the user is in.
                    PoshanPrimaryButton(
                        "Hold steady…",
                        enabled = false,
                        onClick = {},
                    )
                }
            }
        }
    }

    if (showWorking) {
        LiveWorkingSheet(measurement = live, onDismiss = { showWorking = false })
    }

    if (showStages) {
        acquired?.let { StagePreviewSheet(it.measurement) { showStages = false } }
    }
}

/**
 * The calculation as a sequence of pictures.
 *
 * Built from the locked measurement only, and only when opened — five bitmaps per frame at
 * 30 fps would stall the analysis thread to show nobody anything.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StagePreviewSheet(measurement: SkinMeasurement, onDismiss: () -> Unit) {
    val stages = remember(measurement) { StagePreviews.build(measurement) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(
                "Behind this reading",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Gap(4)
            Text(
                "These are the actual pixels the number was worked out from, at each step.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Gap(18)

            if (stages.isEmpty()) {
                Text(
                    "The patch was too small to show.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            stages.forEach { stage ->
                Text(
                    stage.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Gap(8)
                Image(
                    bitmap = stage.image.asImageBitmap(),
                    contentDescription = stage.title,
                    contentScale = ContentScale.FillWidth,
                    // Nearest-neighbour: the patch is a subsampled grid a couple of hundred
                    // pixels across, and smoothing it would blend exactly the per-pixel
                    // decisions this view exists to expose.
                    filterQuality = FilterQuality.None,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(14.dp)),
                )
                Gap(8)
                Text(
                    stage.caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Gap(22)
            }
            Gap(20)
        }
    }
}

/**
 * The full calculation, live, while the camera is still running.
 *
 * Recomputed from the same measurement the reticle colours come from, so what the sheet
 * shows and what the app would save cannot disagree.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveWorkingSheet(measurement: SkinMeasurement?, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            val ita = measurement?.reading?.itaDegrees
            if (measurement == null || ita == null || ita.isNaN()) {
                Text(
                    "Aim the camera first",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Gap(8)
                Text(
                    "Once both boxes have something in them, every step of the calculation " +
                        "appears here with the actual numbers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SkinCalculationSection(ita = ita, measurement = measurement)
            }
            Gap(32)
        }
    }
}

/**
 * Binds preview and analysis, and pins the camera's own processing.
 *
 * Auto white balance is the problem this has to solve. Left on, the camera silently
 * re-neutralises every frame, so the grey-card correction lands on top of a correction the
 * phone already made and the reading drifts as the operator pans. Locking AWB and AE turns
 * the frame into a measurement rather than a pleasing picture.
 */
@OptIn(ExperimentalCamera2Interop::class)
private fun bindSkinCamera(
    ctx: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    executor: java.util.concurrent.Executor,
    segmenter: SkinSegmenter,
    onMeasurement: (SkinMeasurement) -> Unit,
    onLocked: () -> Unit,
) {
    val main = androidx.core.content.ContextCompat.getMainExecutor(ctx)
    val providerFuture = ProcessCameraProvider.getInstance(ctx)
    providerFuture.addListener({
        val provider = providerFuture.get()

        // One resolution selector for BOTH use cases. Preview and analysis must share an
        // aspect ratio: the reticle is drawn over the preview but read from the analysis
        // buffer, so if the preview were 16:9 and the analysis 4:3 the operator would be
        // aiming at a differently-cropped picture than the one being measured.
        val resolution = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                ResolutionStrategy(
                    android.util.Size(640, 480),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                )
            )
            .build()

        val preview = Preview.Builder()
            .setResolutionSelector(resolution)
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val analysisBuilder = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setResolutionSelector(resolution)

        // Scene effects and HDR tone curves rewrite luminance, which is the numerator of
        // ITA, so they are switched off from the first frame. Wrapped because these keys
        // are optional in the Camera2 spec and a device that rejects one must still give a
        // working preview.
        runCatching {
            Camera2Interop.Extender(analysisBuilder).setCaptureRequestOption(
                android.hardware.camera2.CaptureRequest.CONTROL_EFFECT_MODE,
                android.hardware.camera2.CameraMetadata.CONTROL_EFFECT_MODE_OFF,
            )
        }

        val analysis = analysisBuilder.build().also { ia ->
            ia.setAnalyzer(executor) { image ->
                try {
                    // Rotate once, here. Everything downstream — the drawn reticle, the
                    // sampled pixels and the segmentation mask — then lives in the single
                    // coordinate space the operator is actually looking at.
                    val upright = image.toUprightBitmap()
                    val mask = segmenter.skinMask(upright)
                    val skin = SkinPatchSampler.patch(upright, SKIN_RETICLE, mask)
                    // No mask for the card: the card is not skin, and asking a skin model
                    // about it would reject every pixel of it.
                    val card = SkinPatchSampler.patch(upright, CARD_RETICLE, null)
                    onMeasurement(SkinMeasurement.from(skin, card.pixels))
                } catch (t: Throwable) {
                    // One bad frame must not end the session; the next one is 33 ms away.
                } finally {
                    image.close()
                }
            }
        }

        runCatching {
            provider.unbindAll()
            val camera = provider.bindToLifecycle(
                lifecycleOwner,
                // Rear camera: front sensors on budget phones apply far heavier smoothing,
                // and the operator needs to see the child's arm anyway.
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )

            // Exposure and white balance are locked AFTER a settling delay, not at bind.
            // Locking from the first request freezes whatever the sensor happened to open
            // with — typically a badly wrong exposure — and every subsequent reading
            // inherits it. Letting the camera converge first and then freezing it gives a
            // frame that is both correctly exposed and stable while the operator pans.
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                runCatching {
                    androidx.camera.camera2.interop.Camera2CameraControl
                        .from(camera.cameraControl)
                        .setCaptureRequestOptions(
                            androidx.camera.camera2.interop.CaptureRequestOptions.Builder()
                                .setCaptureRequestOption(
                                    android.hardware.camera2.CaptureRequest.CONTROL_AE_LOCK,
                                    true,
                                )
                                .setCaptureRequestOption(
                                    android.hardware.camera2.CaptureRequest.CONTROL_AWB_LOCK,
                                    true,
                                )
                                .build()
                        )
                    onLocked()
                }
            }, SETTLE_MS)
        }
    }, main)
}

/**
 * How long the camera is given to settle before exposure and white balance are frozen.
 *
 * Long enough for auto-exposure to converge on a typical phone, short enough that the
 * operator is still holding the phone where they aimed it.
 */
private const val SETTLE_MS = 1500L

@Composable
private fun ReticleOverlay(measurement: SkinMeasurement?, isLocked: Boolean) {
    val good = isLocked
    val skinOk = measurement?.skinPatch?.usable == true
    val cardOk = measurement?.usedCard == true

    Canvas(Modifier.fillMaxSize()) {
        fun box(r: SkinPatchSampler.Reticle, ok: Boolean) {
            drawRect(
                color = if (ok) Color(0xFF4CAF50) else Color.White,
                topLeft = Offset(r.left * size.width, r.top * size.height),
                size = Size((r.right - r.left) * size.width, (r.bottom - r.top) * size.height),
                style = Stroke(width = if (ok) 6f else 4f),
            )
        }
        drawRect(Color.Black.copy(alpha = if (good) 0.08f else 0.22f))
        box(SKIN_RETICLE, skinOk)
        box(CARD_RETICLE, cardOk)
    }
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Bottom) {
        Text(
            when {
                isLocked -> "Locked"
                cardOk -> "Grey card found"
                else -> "Put the grey card in the right-hand box"
            },
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}

/**
 * Acquisition state: waiting, filling, or locked.
 *
 * Shows the reading only once it is locked. A number that flickers while the operator aims
 * invites them to press at the moment it happens to look right, which is exactly the sampling
 * behaviour the lock exists to prevent.
 */
@Composable
private fun LockCard(state: SkinReadingLock.State, locked: Boolean) {
    PoshanCard(contentPadding = PaddingValues(16.dp)) {
        if (!locked) {
            Text(
                "Setting up the camera…",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Gap(4)
            Text(
                "Hold steady for a moment while the brightness settles.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@PoshanCard
        }

        when (state) {
            is SkinReadingLock.State.Waiting -> {
                Text(
                    state.reason,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )
                Gap(4)
                Text(
                    "The reading will lock on its own once the picture is good.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is SkinReadingLock.State.Acquiring -> {
                Text(
                    "Hold steady…",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Gap(8)
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Gap(6)
                Text(
                    "${state.framesHeld} of ${SkinReadingLock.REQUIRED_FRAMES} steady frames",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is SkinReadingLock.State.Locked -> {
                val type = SkinType.fromIta(state.itaDegrees)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${state.itaDegrees.roundToInt()}°",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            type?.let { "Locked · Skin type ${it.roman}" } ?: "Locked",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        // The spread is this reading's own repeatability. Publishing it is
                        // what separates a settled measurement from a lucky frame.
                        Text(
                            "steady to ±%.1f°".format(state.spreadDeg),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                type?.let {
                    val margin = SkinType.marginToEdge(state.itaDegrees)
                    if (margin < SkinType.BORDERLINE_MARGIN) {
                        Gap(8)
                        Text(
                            "Close to the line between two skin types. Take it again in " +
                                "better light if it matters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The pipeline stages, live, as colour swatches.
 *
 * Each swatch is literally what the next stage receives. A bad white balance shows up as a
 * visibly tinted "corrected" swatch, and a reticle full of cloth shows up as a kept-pixel
 * count near zero — both visible before anything is saved, and both far more legible than a
 * refusal message.
 */
@Composable
private fun FilterStages(m: SkinMeasurement) {
    PoshanCard(contentPadding = PaddingValues(14.dp)) {
        Text(
            "What the app is seeing",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Gap(10)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Swatch("As seen", m.rawPatch.r, m.rawPatch.g, m.rawPatch.b, Modifier.weight(1f))
            m.cardPatch?.let {
                Swatch("Grey card", it.r, it.g, it.b, Modifier.weight(1f))
            }
            Swatch(
                "Corrected",
                m.balancedR, m.balancedG, m.balancedB,
                Modifier.weight(1f),
            )
        }
        Gap(10)
        Text(
            "Kept ${m.skinPatch.keptPixels} of ${m.skinPatch.totalPixels} pixels · " +
                "dropped ${m.skinPatch.rejectedSpecular} shiny, " +
                "${m.skinPatch.rejectedNonSkin} not skin",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            if (m.skinPatch.usedModel) {
                "Skin found by the on-device model"
            } else {
                "Skin found by colour rule (model unavailable)"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun Swatch(label: String, r: Int, g: Int, b: Int, modifier: Modifier = Modifier) {
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(r, g, b)),
        )
        Gap(4)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The analysis frame as a bitmap turned the way the operator is holding the phone.
 *
 * CameraX hands back the sensor buffer untouched and reports how far it must be turned to
 * look upright. Doing that turn here, once, is what lets every later stage work in plain
 * display coordinates.
 */
private fun androidx.camera.core.ImageProxy.toUprightBitmap(): android.graphics.Bitmap {
    val source = toBitmap()
    val degrees = imageInfo.rotationDegrees
    if (degrees == 0) return source
    val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
    return android.graphics.Bitmap.createBitmap(
        source, 0, 0, source.width, source.height, matrix, true,
    )
}

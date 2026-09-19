package com.teamposhan.biomed.domain.attention

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.hypot

/**
 * CameraX analyser: front-camera frames to [FaceTrackingFrame], via MediaPipe
 * FaceLandmarker (478 landmarks including iris, plus blendshapes and the facial
 * transformation matrix).
 *
 * Everything runs on-device. No frames are stored or transmitted.
 *
 * This is a rebuild. The previous version had four defects that corrupted the measurement
 * rather than merely degrading it, all fixed here:
 *
 *  1. **Timestamps were wall-clock**, read at *callback* time. `System.currentTimeMillis()`
 *     is not monotonic — it jumps on NTP sync — which violates MediaPipe's strictly-
 *     increasing precondition and was failing silently into an empty error listener. Worse,
 *     stamping at callback time folded the inference latency into the sample time, and that
 *     latency varies with scene content, so behaviour-correlated jitter was being injected
 *     into the very time series the screening analyses. Now: `imageInfo.timestamp`,
 *     monotonic, referenced to start of exposure.
 *  2. **Rotation was never applied** — on most phones in portrait the mesh ran on a sideways
 *     face. The frame is now rotated (and mirrored) into display orientation with an
 *     explicit [Matrix] before inference, rather than delegating to MediaPipe's
 *     `ImageProcessingOptions`: CameraX reports the clockwise rotation needed to make the
 *     buffer upright, which is not reliably the same convention MediaPipe applies, and a
 *     90-degree disagreement puts every landmark on its side. Doing the transform here
 *     makes the output frame identical to what PreviewView displays, so the developer
 *     overlay and the maths share one geometry.
 *  3. **Mirroring was applied inconsistently** — gaze was flipped but head pose was not, so
 *     the two disagreed about which way was left. Now mirrored exactly once, at the source,
 *     so every downstream consumer shares one handedness.
 *  4. **Head pose was a landmark-distance heuristic** with hand-tuned constants. Now
 *     MediaPipe's Procrustes-fitted transformation matrix (see [HeadPose]).
 *
 * Threading: MediaPipe's GPU delegate must be created on the same thread that uses it, so
 * construction is deferred to the first [analyze] call on the CameraX executor rather than
 * happening in `init`.
 */
class FaceGazeAnalyzer(
    private val context: Context,
    private val mirrored: Boolean = true,
    private val ageMonths: Float = 24f,
    private val onFrame: (FaceTrackingFrame) -> Unit,
) : ImageAnalysis.Analyzer {

    private var landmarker: FaceLandmarker? = null
    private var built = false
    private var usingGpu = false

    private var lastSentTs = Long.MIN_VALUE
    private var reusableBitmap: Bitmap? = null
    private var orientedBitmap: Bitmap? = null
    @Volatile private var frameWidth = 0
    @Volatile private var frameHeight = 0

    /** Capture timestamp keyed by the timestamp handed to MediaPipe, to recover latency. */
    private val pendingCaptureNs = HashMap<Long, Long>()

    private val intervals = ArrayDeque<Long>()
    private var lastFrameTs = 0L

    @Volatile var quality = CaptureQuality()
        private set

    val isReady: Boolean get() = landmarker != null

    // ---------------------------------------------------------------------
    // Construction
    // ---------------------------------------------------------------------

    private fun supportsGpu(): Boolean = runCatching {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        // MediaPipe's GPU inference calculators require OpenGL ES 3.1 or better.
        am.deviceConfigurationInfo.reqGlEsVersion >= 0x00030001
    }.getOrDefault(false)

    private fun build(useGpu: Boolean): FaceLandmarker {
        val base = BaseOptions.builder()
            .setModelAssetPath(MODEL_ASSET)
            .setDelegate(if (useGpu) Delegate.GPU else Delegate.CPU)
            .build()
        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(base)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumFaces(1)
            .setMinFaceDetectionConfidence(0.4f)
            .setMinFacePresenceConfidence(0.4f)
            .setMinTrackingConfidence(0.4f)
            .setOutputFaceBlendshapes(true)
            // Real head pose, instead of hand-rolled landmark ratios.
            .setOutputFacialTransformationMatrixes(true)
            .setResultListener { result, _ -> handleResult(result) }
            .setErrorListener { quality = quality.copy(errors = quality.errors + 1) }
            .build()
        return FaceLandmarker.createFromOptions(context, options)
    }

    /** Built lazily on the analysis thread; the GPU delegate cannot cross threads. */
    private fun ensureBuilt() {
        if (built) return
        built = true
        val wantGpu = supportsGpu()
        landmarker = runCatching {
            usingGpu = wantGpu
            build(wantGpu)
        }.recoverCatching {
            // Documented failure path: some devices report GLES 3.1 but reject the delegate.
            usingGpu = false
            build(false)
        }.getOrNull()
        quality = quality.copy(
            delegate = when {
                landmarker == null -> "unavailable"
                usingGpu -> "GPU"
                else -> "CPU"
            }
        )
    }

    // ---------------------------------------------------------------------
    // Frame ingest
    // ---------------------------------------------------------------------

    override fun analyze(imageProxy: ImageProxy) {
        try {
            ensureBuilt()
            val lm = landmarker ?: return

            // Monotonic, capture-referenced. Never System.currentTimeMillis().
            val captureNs = imageProxy.imageInfo.timestamp
            var tsMs = captureNs / 1_000_000L
            // MediaPipe requires strictly increasing timestamps. Nudge rather than drop, so
            // the frame is still counted and the achieved rate stays honest.
            if (tsMs <= lastSentTs) tsMs = lastSentTs + 1
            lastSentTs = tsMs

            val raw = copyToReusableBitmap(imageProxy) ?: return
            val upright = orientForDisplay(raw, imageProxy.imageInfo.rotationDegrees)
            frameWidth = upright.width
            frameHeight = upright.height
            pendingCaptureNs[tsMs] = System.nanoTime()

            lm.detectAsync(BitmapImageBuilder(upright).build(), tsMs)

            recordInterval(tsMs)
            quality = quality.copy(framesIn = quality.framesIn + 1)
        } catch (_: Throwable) {
            quality = quality.copy(errors = quality.errors + 1)
        } finally {
            imageProxy.close()
        }
    }

    /**
     * Copies the frame into a single reused Bitmap. The previous version allocated a fresh
     * one per frame — about 1.2 MB at 640x480, roughly 37 MB/s of GC churn at 30 fps, which
     * a 2 GB device feels.
     */
    private fun copyToReusableBitmap(proxy: ImageProxy): Bitmap? = runCatching {
        val w = proxy.width
        val h = proxy.height
        val target = reusableBitmap?.takeIf { it.width == w && it.height == h }
            ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { reusableBitmap = it }

        val plane = proxy.planes.firstOrNull() ?: return@runCatching null
        // RGBA_8888 output configured on the ImageAnalysis use case; some devices pad rows.
        if (proxy.format == android.graphics.ImageFormat.FLEX_RGBA_8888 ||
            plane.pixelStride == 4
        ) {
            if (plane.rowStride == w * 4) {
                plane.buffer.rewind()
                target.copyPixelsFromBuffer(plane.buffer)
                return@runCatching target
            }
        }
        // Fallback for YUV or padded strides: CameraX's own native conversion.
        proxy.toBitmap()
    }.getOrNull()

    /**
     * Rotates the frame upright and mirrors it for the front camera, in one transform.
     *
     * The result matches what PreviewView shows, which is the whole point: an overlay drawn
     * against a different geometry can look plausible while sitting several degrees off the
     * real face.
     */
    private fun orientForDisplay(src: Bitmap, rotationDegrees: Int): Bitmap {
        val swap = rotationDegrees == 90 || rotationDegrees == 270
        val outW = if (swap) src.height else src.width
        val outH = if (swap) src.width else src.height

        if (rotationDegrees == 0 && !mirrored) return src

        val target = orientedBitmap?.takeIf { it.width == outW && it.height == outH }
            ?: Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
                .also { orientedBitmap = it }

        val matrix = Matrix().apply {
            // Rotate about the source centre, then move that centre onto the output centre.
            postTranslate(-src.width / 2f, -src.height / 2f)
            postRotate(rotationDegrees.toFloat())
            if (mirrored) postScale(-1f, 1f)
            postTranslate(outW / 2f, outH / 2f)
        }
        Canvas(target).apply {
            drawColor(android.graphics.Color.BLACK)
            drawBitmap(src, matrix, null)
        }
        return target
    }

    private fun recordInterval(tsMs: Long) {
        if (lastFrameTs != 0L) {
            intervals.addLast(tsMs - lastFrameTs)
            if (intervals.size > INTERVAL_WINDOW) intervals.removeFirst()
            val sorted = intervals.sorted()
            val median = sorted[sorted.size / 2]
            quality = quality.copy(
                medianIntervalMs = median,
                effectiveFps = if (median > 0) 1000f / median else 0f,
            )
        }
        lastFrameTs = tsMs
    }

    // ---------------------------------------------------------------------
    // Result handling
    // ---------------------------------------------------------------------

    private fun handleResult(result: FaceLandmarkerResult) {
        // Echoes the capture timestamp we supplied — never re-read the clock here.
        val tMs = result.timestampMs()
        val latencyMs = pendingCaptureNs.remove(tMs)
            ?.let { (System.nanoTime() - it) / 1_000_000L } ?: 0L
        if (pendingCaptureNs.size > 32) pendingCaptureNs.clear()

        val faces = result.faceLandmarks()
        val lm = faces?.firstOrNull()
        if (lm == null || lm.size < 478) {
            onFrame(FaceTrackingFrame.noFace(tMs, latencyMs))
            return
        }

        // Mirroring and rotation were applied to the pixels before inference, so these
        // coordinates are already in display orientation and display handedness. Nothing
        // downstream flips anything again — that inconsistency is what previously left
        // gaze and head pose disagreeing about which way was left.
        val x = FloatArray(lm.size) { lm[it].x() }
        val y = FloatArray(lm.size) { lm[it].y() }

        val rawPose = result.facialTransformationMatrixes()
            .orElse(null)
            ?.firstOrNull()
            ?.let { HeadPose.fromMatrix(it) }
            ?: HeadPose.UNKNOWN
        val pose = rawPose

        val blinkScore = blinkScore(result)
        val eyesOpen = blinkScore < BLINK_THRESHOLD

        val gazeX = GazeGeometry.horizontalGaze(x, y)
        val gazeY = GazeGeometry.verticalGaze(y)

        val ipdPx = irisSeparationPx(x, y)
        val inEnvelope = PoseGate.accepts(pose)

        val frame = FaceTrackingFrame(
            tMs = tMs,
            facePresent = true,
            landmarkX = x,
            landmarkY = y,
            headPose = pose,
            gazeX = gazeX.coerceIn(0f, 1f),
            gazeY = gazeY.coerceIn(0f, 1f),
            eyesOpen = eyesOpen,
            blinkScore = blinkScore,
            ipdPx = ipdPx,
            poseInEnvelope = inEnvelope,
            inferenceLatencyMs = latencyMs,
            frameWidth = frameWidth,
            frameHeight = frameHeight,
        )

        quality = quality.copy(
            framesWithFace = quality.framesWithFace + 1,
            framesUsable = quality.framesUsable + if (frame.usable) 1 else 0,
            framesRejectedPose = quality.framesRejectedPose + if (!inEnvelope) 1 else 0,
            framesBlink = quality.framesBlink + if (!eyesOpen) 1 else 0,
        )

        onFrame(frame)
    }

    /**
     * Per-eye blink score. The maximum, not the mean: averaging the two eyes suppresses a
     * unilateral blink, during which the iris landmarks on the closed side are hallucinated
     * behind the lid and slide with it.
     */
    private fun blinkScore(result: FaceLandmarkerResult): Float = runCatching {
        val categories = result.faceBlendshapes().orElse(null)?.firstOrNull()
            ?: return@runCatching 0f
        categories
            .filter { it.categoryName() == "eyeBlinkLeft" || it.categoryName() == "eyeBlinkRight" }
            .maxOfOrNull { it.score() } ?: 0f
    }.getOrDefault(0f)

    private fun irisSeparationPx(x: FloatArray, y: FloatArray): Float {
        if (x.size < 478) return 0f
        return hypot(
            x[GazeGeometry.IRIS_LEFT_CENTRE] - x[GazeGeometry.IRIS_RIGHT_CENTRE],
            y[GazeGeometry.IRIS_LEFT_CENTRE] - y[GazeGeometry.IRIS_RIGHT_CENTRE],
        )
    }

    fun close() {
        runCatching { landmarker?.close() }
        landmarker = null
        reusableBitmap = null
        orientedBitmap = null
        pendingCaptureNs.clear()
    }

    private companion object {
        const val MODEL_ASSET = "face_landmarker.task"
        const val INTERVAL_WINDOW = 60
        // Continuous coefficient; a child mid-blink crosses this within one or two frames.
        const val BLINK_THRESHOLD = 0.5f
    }
}

/** Legacy shape kept so existing callers compile while module 2 is migrated. */
fun FaceTrackingFrame.toGazeSample(): GazeSample = GazeSample(
    tMs = tMs,
    facePresent = facePresent,
    gazeX = gazeX,
    gazeY = gazeY,
    yaw = headPose.yaw,
    pitch = headPose.pitch,
    eyesOpen = eyesOpen,
)

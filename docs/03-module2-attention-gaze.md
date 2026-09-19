# Module 2 — Attention, Gaze and Looking Behaviour

**Status: the paradigms are sound; the signal chain underneath them is not.** This document describes what is implemented today, what three independent research reviews found wrong with it, and the target architecture. Read §5 before writing any code against this module.

---

## 1. What module 2 is for

The child is shown stimuli on the phone screen while the front camera watches. From that video we derive looking behaviour — where the child looked, for how long, whether they turned to their name, how much they moved their head — and combine those into a screening signal for developmental concern.

The clinical precedent is Duke's **SenseToKnow** (Perochon et al., *Nature Medicine* 2023, [PMC10579093](https://pmc.ncbi.nlm.nih.gov/articles/PMC10579093/)): a tablet app that records at 30 fps from the front camera, extracts landmarks, head pose, gaze and blink features, and reports *"percentage of time the child gazed at the social half of the screen"* — a half-screen decision, not a gaze coordinate. It reached 87.8% sensitivity / 80.8% specificity in 475 toddlers.

That altitude — coarse looking direction, aggregated over well-designed stimuli — is the right target. We are not building an eye tracker.

---

## 2. Current implementation

| File | Lines | Role |
|---|---|---|
| `domain/attention/FaceGazeAnalyzer.kt` | 122 | CameraX `ImageAnalysis.Analyzer`; runs MediaPipe FaceLandmarker, emits `GazeSample` |
| `domain/attention/GazeTracker.kt` | 98 | `GazeSample`, `GazeRegion`, `AoiContext`, and `GazeGeometry` — the landmark maths |
| `domain/attention/AttentionProtocol.kt` | 313 | Trial sequencing: stimulus schedule, AOI sides, name calls, cue trials |
| `domain/attention/AttentionRecorder.kt` | 433 | Streaming metric accumulation; emits `AttentionMetrics` |
| `ui/screens/AttentionScreen.kt` | 856 | Camera binding, stimulus presentation, session UI |

**Stack as configured:** MediaPipe Tasks Vision `FaceLandmarker`, 478 landmarks including iris, blendshapes on, facial transformation matrix **off**, `Delegate.CPU`, `RunningMode.LIVE_STREAM`, `numFaces=1`, model asset `face_landmarker.task`.

**Gaze as computed:** `GazeGeometry.horizontalGaze()` takes the iris centre's position as a fraction of the distance between the eye corners, averages the two eyes, and thresholds into LEFT / CENTRE / RIGHT. Head pose comes from `yaw()` / `pitch()`, which are landmark-distance ratios scaled by constants (`* 2.2f`, `* 2.0f`). Distance comes from `approximateDistanceCm()`, which is `0.42f / irisSpanNormalised`.

**Metrics produced:** mean fixation duration (streaming I-DT, dispersion threshold `0.055f`), saccade count and rate, social/geometric AOI dwell ratios, response-to-name, disengagement latency, gaze-following accuracy with cue-side scoring, head-movement rate, blink rate, bubble-touch task results, per-trial validity gating (≥50% usable frames), and a session confidence score.

---

## 3. Defects in the current signal chain

Found by code audit and by three research reviews. Ordered by severity.

### 3.1 Timestamps — the most serious correctness bug

`FaceGazeAnalyzer` passes `System.currentTimeMillis()` as the MediaPipe packet timestamp **and** re-reads the clock in `handleResult()` to stamp the sample.

Three separate failures follow:

1. **Wall clock is not monotonic.** It jumps on NTP sync and can move backwards. MediaPipe's `TaskRunner.validateInputTimestamp()` rejects any packet whose timestamp is not strictly greater than the last, with `FAILED_PRECONDITION`. Our `setErrorListener` is empty (`{ /* frame skipped */ }`), so this fails **silently**. On a field device syncing time over a flaky connection, this is a live failure mode.
2. **Sample time is result time, not capture time.** The gap between them is the full inference latency — 30–90+ ms on CPU — and that latency *varies with scene content*. We are injecting content-correlated jitter into the very time series we then analyse. Any velocity estimate inherits a bias that tracks the child's behaviour, which is exactly the confound that makes a biomarker uninterpretable.
3. **Frames vanish unaccounted.** The `if (ts > lastTimestampMs)` guard silently drops two frames landing in the same millisecond, so we have no record of the true achieved sampling rate.

**Correct source:** `ImageProxy.imageInfo.timestamp` — nanoseconds, start of exposure, based on `CLOCK_MONOTONIC` or `CLOCK_BOOTTIME` per `SENSOR_INFO_TIMESTAMP_SOURCE`. Read that characteristic once and record which base was obtained.

### 3.2 The head-yaw confound makes iris-ratio gaze unusable

From a reduced-eye model (derivation in the gaze research report; no published paper gives this transfer function directly):

```
g_apparent ≈ g − 0.83·ψ
```

**Every 1° of head yaw injects ~0.83° of apparent gaze error, in the opposite direction.** The mechanism: the eye corners sit ~10 mm in front of the eyeball's rotation centre, so head rotation translates the corner midpoint in the image while also compressing the corner separation by `cos ψ`.

| Head yaw | Apparent gaze error | As % of full LEFT↔RIGHT range |
|---|---|---|
| 5° | ≈ 4.2° | 7% |
| 10° | ≈ 8.3° | 15% |
| 20° | ≈ 16.6° | 29% |
| 30° | ≈ 25° | 43% |

This is **bias, not noise** — averaging more frames does not remove it. Google states the point directly for the underlying model: *"iris tracking does not infer the location at which people are looking"* ([MediaPipe Iris](https://research.google/blog/mediapipe-iris-real-time-iris-tracking-depth-estimation/)).

### 3.3 The screen is too small in portrait

Angular separation between the centroids of the two screen halves, 6″ phone:

| Configuration | @ 30 cm | @ 40 cm | @ 50 cm |
|---|---|---|---|
| **Portrait**, half-centroids | 2.99° | **2.25°** | 1.80° |
| Landscape, half-centroids | 6.61° | 4.97° | 3.98° |
| **Landscape, edge-placed targets** | 10.4° | **7.83°** | 6.28° |

A 5° head turn already exceeds the entire portrait budget. Portrait, uncalibrated, is a coin flip. **Switching to landscape with edge-placed stimuli raises the signal 3.5× and costs nothing** — the single highest-leverage change available.

### 3.4 Adult anthropometry applied to infants

`approximateDistanceCm()` uses a magic constant; the principled version uses a known physical scale reference. Both available references are age-dependent, and the adult values are badly wrong for our users:

| Age | Mean IPD (mm) | Corneal diameter (mm) |
|---|---|---|
| 0–11 mo | 40.5 | 9.78 (0–1 mo) |
| 12–23 mo | 46.5 | 11.38 |
| 24–35 mo | 47.5 | 11.72 |
| 36–47 mo | 49.5 | — |
| 48–59 mo | 51.0 | — |
| Adult | ~63 | 11.7 |

Sources: MacLachlan & Howland 2002 (1311 subjects, 1 month–19 years); corneal diameter 0–36 months, [PMC9589223](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC9589223/).

Using the adult 63 mm IPD for a 6-month-old **overestimates distance by 55%**, and every downstream quantity that depends on Z — degrees-per-pixel, the I-DT threshold in pixels, any gaze-ray/screen intersection — is wrong by the same factor. The app already knows the child's age; this is a lookup table away from correct.

Compounding this: infants have proportionally shorter interocular distance in pixels at a given distance, so the iris-ratio denominator shrinks and **the same absolute landmark error produces a larger gaze error for younger children — an age-correlated bias in an age-stratified instrument.**

### 3.5 Vertical gaze is structurally confounded with blinking

`verticalGaze()` divides the iris y-position by the eyelid aperture. During a blink the aperture approaches zero; the `1e-5f` guard then returns `0.5f` — "looking straight ahead" — precisely when the eyes are shut. Worse, MediaPipe does not drop iris landmarks during a blink: it hallucinates them behind the closed lid, and they slide with the lid, producing a fast spurious excursion.

### 3.6 Handedness is inconsistent

`analyze()` never applies `imageProxy.imageInfo.rotationDegrees`, so on most phones in portrait the mesh runs on a sideways face. Mirroring is then "corrected" by `gazeX = 1f - gazeX` *after* the ratio is computed — but `yaw()` and `verticalGaze()` keep the unmirrored buffer handedness. **Gaze and head pose disagree about which way is left.** Get this wrong and it looks like a clinical finding.

### 3.7 Units break the link to the literature

The I-DT dispersion threshold is `0.055f` in normalised gaze-ratio units. Published I-DT thresholds are in **degrees of visual angle** — 1.0° dispersion, 100 ms minimum (Salvucci & Goldberg 2000; Blignaut 2009). Without a screen mapping and a live distance estimate our "fixations" are not comparable to any published definition. The algorithm name matches the literature; the parameter does not.

### 3.8 Other defects

| # | Issue |
|---|---|
| 1 | `Delegate.CPU` — GPU is ~6× faster (Pixel 9 Pro: 15 ms vs 88 ms). On budget SoCs CPU delegate is plausibly 3–7 fps, which is below any usable threshold. **Must be measured on target hardware.** |
| 2 | Landmarker built in `init{}` on the composition thread but used on a `newSingleThreadExecutor` — this breaks the moment GPU delegate is enabled; Google's sample requires same-thread construction and use |
| 3 | `setOutputFacialTransformationMatrixes` is off, so we hand-roll head pose instead of taking MediaPipe's Procrustes-fitted rotation matrix for free |
| 4 | No `ResolutionSelector` — output resolution is device-dependent, so inference cost and landmark precision vary silently across the fleet |
| 5 | Per-frame `Bitmap` allocation via `imageProxy.toBitmap()` — ~1.2 MB/frame at 640×480, ≈37 MB/s of GC churn at 30 fps on a 2 GB device |
| 6 | No AE lock, no fixed FPS range — exposure hunting modulates landmark noise, especially in bright outdoor light |
| 7 | Blink threshold `avg(eyeBlinkL, eyeBlinkR) < 0.5` suppresses unilateral blinks and chatters with no hysteresis |
| 8 | `runCatching{}.getOrNull()` on landmarker construction turns a hard failure into a screener that runs and produces nothing; `isReady` is never checked in the camera path |
| 9 | Version drift: MediaPipe 0.10.26 (current 1.0.0), CameraX 1.4.1 (current 1.6.2) |
| 10 | Blendshape `eyeLook*` coefficients are a re-encoding of the same landmarks (the blendshape model's input is 1×146×2 landmark coordinates, not pixels) — they add no independent information. Only `eyeBlink*` earns its place |

---

## 4. What is and is not measurable on this hardware

Three reviews converged on this. The frame rate and the screen size are hard physical limits, not implementation quality.

### 4.1 Defensible at 15–30 Hz

- **AOI dwell / proportion of looking to one screen half** — no significant difference in fixation-detection rate between 60 and 120 Hz for presence/dwell measures
- **Face-present rate, on-screen vs off-screen attention**
- **Head-pose distribution** — and note this is *more trustworthy than our gaze signal*, and is itself a clinical variable (SenseToKnow uses head-movement rate, complexity and acceleration as three separate features)
- **Blink rate** (25–30 fps sufficient for rate; SenseToKnow uses it at 30 fps)
- **Bubble-touch motor metrics** — touch is exact and frame-rate-independent. **This is our highest-quality signal.**
- **Distance to face**, to ~5–10% with age-conditioned anthropometry

### 4.2 Not defensible — remove or relabel

| Metric | Why |
|---|---|
| **Saccade rate / count** | 60 Hz detects only 56% of saccades, 120 Hz only 68%. At 30 Hz you have 1–2 samples per saccade. Not measurable. |
| **Saccade amplitude / peak velocity / latency** | Needs ≥120 Hz, >200 Hz for velocity features |
| **Fixation duration as a continuous variable** | Stable only from 60 Hz. At 30 Hz a 100 ms fixation is 3 samples |
| **Disengagement / gap-overlap** | Two independent killers: the published 15° eccentricity is geometrically impossible (max 13.0° at 30 cm, 7.9° at 50 cm), and the discriminating effect is ~130 ms = 2–4 frames |
| **Smooth-pursuit gain** | Requires continuous velocity estimation; no validated RGB-camera infant implementation exists |
| **Sub-AOI gaze (eyes vs mouth)** | Those regions subtend 2–4°; toddler accuracy on *infrared* hardware is 1.3° mean with a 3.85° upper range |
| **Quadrants, small AOIs, gaze heatmaps** | Need ≲1°. Google's 0.6–1.0° phone system required personal calibration and a controlled setup |
| **Blink duration / dynamics** | Needs ~100 fps for duration, ~250 fps for velocity |
| **Any comparison against published infant norms** | Those were collected on 60–500 Hz calibrated IR trackers. Our numbers are not on the same scale, and the difference is not a constant that can be divided out |

### 4.3 Age range

The validated evidence base for this class of instrument is **12–48 months** (GeoPref 12–48; SenseToKnow 16–40; EarliPoint 16–30). Below 12 months and above 48 there is no supporting literature. Nobody has shown this method works on infants, and MediaPipe's landmark accuracy on under-2s is unvalidated — an adjacent newborn-pose study found all models degraded on newborns, collapsing at AP90.

**Module 2 should not emit a score outside 12–48 months.**

### 4.4 Metrics with no age norms

These can be computed and used as model features, but must **never** be displayed with a normal range: head-movement rate, gaze-following accuracy, geometric preference on a phone-sized display (the published 69% cut-off was derived on AOIs roughly twice our linear size), and the session confidence score (no published precedent — relabel it a data-quality indicator).

And one number worth remembering: **GeoPref has 17% sensitivity** at 98% specificity. It is a rule-in subtype marker. It misses five of every six autistic children and must never be a screener's backbone.

---

## 5. Target architecture

### 5.1 Stack decision

**Keep MediaPipe FaceLandmarker.** Of everything surveyed it is the only on-device option in 2026 that produces iris landmarks under Apache-2.0 with an API-23/24 floor. ML Kit Face Mesh has no iris; ARCore Augmented Faces has no iris and requires certified devices; legacy MediaPipe Iris was end-of-lifed in March 2023.

Licensing note for a deployable product: **GazeCapture and ETH-XGaze are non-commercial licences.** L2CS-Net's code is MIT but its weights' training-data provenance must be checked separately. This rules out several of the best-performing published models.

### 5.2 Pipeline

```
STAGE 0  CAPTURE
  CameraX front camera, LANDSCAPE, stimuli at screen edges (±5.5 cm).
  ResolutionSelector pinned 640×480. OUTPUT_IMAGE_FORMAT_RGBA_8888, reused Bitmap.
  STRATEGY_KEEP_ONLY_LATEST. AE_TARGET_FPS_RANGE (30,30); AE lock after settle.
  Resolve mirroring + rotation ONCE, in one place; assert with a known-side test.
  Intrinsics: LENS_INTRINSIC_CALIBRATION → focal/sensor formula → HFOV assumption.
  LOG WHICH PATH WAS USED.

STAGE 1  LANDMARKS
  FaceLandmarker LIVE_STREAM, GPU delegate with GLES-3.1 capability gate and
  CPU fallback; built and used on ONE dedicated thread.
  setOutputFaceBlendshapes(true), setOutputFacialTransformationMatrixes(true).
  Timestamp = ImageProxy.imageInfo.timestamp (monotonic, capture-referenced).
  Non-empty errorListener.

STAGE 2  VALIDITY GATE (before any maths)
  Reject frame on: no face | blink (adaptive per-session EAR threshold + eyeBlink
  blendshape, ±2 frame dilation) | low landmark confidence.
  Mark INVALID. Never interpolate, never zero-fill.

STAGE 3  ANTHROPOMETRY + DISTANCE
  IPD_mm ← age lookup (§3.4). Z = f_px · IPD_mm / ipd_px.
  Cross-check against iris-derived Z; divergence >15% is a quality flag.
  deg_per_px recomputed EVERY FRAME from live Z.

STAGE 4  HEAD POSE
  MediaPipe facial transformation matrix for rotation (row-major 4×4).
  DO NOT trust its translation: the matrix is solved against a hardcoded 63°
  vertical-FOV camera (face_geometry_from_landmarks_graph.cc) that our front
  camera is not, and the canonical model is adult-scaled.
  Optionally cross-check with our own solvePnP (SQPNP + RANSAC) over ~40–60 rigid
  canonical vertices, scaled by IPD ratio.
  Keep rotation as a quaternion; smooth with SLERP. Decompose to Y-X-Z Euler only
  for logging and gating; guard gimbal lock.
  GATE: reject frame if |yaw| > 15° or |pitch| > 12°.

STAGE 5  GAZE VECTOR
  Eyeball centre from head pose; optical axis = iris centre − eyeball centre.
  Visual axis = optical axis rotated by kappa (default 5.0° h / 1.3° v), replaced
  by the per-child value once calibration is harvested.
  If the two eyes disagree by >8°, flag the frame.

STAGE 6  IMPLICIT CALIBRATION (concurrent, first ~20 s)
  Looming/contracting animated target + sound at 2–3 known corners.
  Auto-accept when dispersion < 1.5° for ≥200 ms AND head faces screen.
  1 point → constant offset (absorbs kappa, the largest single error term).
  2–3 points → offset + per-axis gain. Re-anchor mid-session for drift.
  Zero points harvested → continue, but FLAG THE SESSION.

STAGE 7  SCREEN POINT
  Needs the camera's physical offset from the display — Android does not expose
  this. Build a small per-device-model table; default to camera centred, ~8 mm
  above the active area. A wrong SIGN on dx is fatal; 5 mm of error is 0.7°.
  One Euro filter (f_cmin ≈ 1.0 Hz, β ≈ 0.007) on the ONLINE stream only.
  PERSIST THE RAW UNFILTERED STREAM.

STAGE 8  EVENTS
  Offline, on raw samples: Savitzky-Golay then I-DT, dispersion 1.0° converted to
  px per-frame via Stage 3, minimum duration 150 ms.
  Bridge gaps < 100 ms. Blink frames are gaps, not data.
  PRIMARY OUTCOME = dwell time per screen half + number of looks (runs).
  No saccade amplitude / velocity / latency at 30 Hz.

STAGE 9  METRICS + GATE
  Trial valid iff ≥70% valid samples (or SenseToKnow's ≥50%, pre-registered).
  Session valid iff ≥6 valid trials AND ≥50% of frames pass the head-pose gate.
  Attach the calibrated/uncalibrated flag to every derived result.
```

**Why never a moving average:** a K-tap boxcar imposes fixed group delay and linearly interpolates *across* the saccade, dividing peak velocity by K so the I-VT threshold is never crossed — the saccade disappears and two fixations merge into one spurious long one. This is the documented mechanism by which "more robust data" yields "apparently longer fixation durations" in the infant literature. Use One Euro online (adaptive cutoff: heavy smoothing when still, light when fast), Savitzky-Golay offline.

### 5.3 Worth prototyping in parallel

**iCatcher+** ([Erel et al., *AMPPS* 2023](https://doi.org/10.1177/25152459221147250)) classifies infant looking as LEFT / RIGHT / AWAY directly from video using five consecutive frames — **no calibration, no head-pose model, no coordinates**. Validated on 4 months–3.5 years including field and home-webcam footage; ~90% agreement with trained human coders in an independent replication.

If our outcome measure is which half of the screen the child looked at, reconstructing gaze coordinates is a strictly harder problem than the one we need to solve. Run a quantised ResNet18-class variant alongside the geometric pipeline as a cross-check: where they agree, the result is defensible; where they disagree, flag the frame. The heavier RegNetY_16GF variant (~80M params) is not viable on-device.

### 5.4 Per-session telemetry (record all of it)

`framesIn`, `framesWithFace`, `errorCount`, achieved median Δt and its IQR, valid-gaze rate, effective sampling rate, RMS-S2S precision (median of 200 ms windows), head-pose mean and 95th percentile per axis, % frames outside the compensation envelope, distance mean/SD/range, blink rate, IPD-vs-iris distance divergence, mean luminance, and left/right assignment balance (a session stuck at 95/5 is a mirroring bug, not a finding).

There is no universal "unusable" threshold in the literature — data quality is study-context dependent. Adopt published criteria explicitly and pre-register them.

---

## 6. Ethics and claims

Non-negotiable for a screening instrument deployed by non-clinicians:

1. **Never output a diagnosis, a probability of autism, or the word "autism"** in caregiver-facing results. The outcomes are *refer for developmental assessment* / *no referral indicated at this visit* / *assessment not completed*.
2. **"Assessment not completed" must be first-class.** Expect 30–45% of field sessions to produce no usable result (remote infant eye-tracking attrition is 42% vs 10% in-lab). Silently coercing those to "low risk" would be the single largest source of harm in the app.
3. **A second-stage instrument before referral.** Single-stage PPV for this class is ~14%; M-CHAT-R/F's two-stage follow-up lifted PPV from 13.8% to 47.5%.
4. **Resolve the referral pathway before displaying a positive.** A positive screen with nowhere to refer is a net harm.
5. **Do not show per-metric numbers to non-clinicians** — most of our metrics have no age norms, and a number without a reference invites over-interpretation.
6. **Audit by sex, caste/tribal status and district.** The closest comparator showed specificity of 53.6% for Black children vs 82.7% for White. No phone screener has been validated on an Indian cohort.

USPSTF currently rates universal ASD screening at 18–30 months where no concerns have been raised as **"I" — insufficient evidence** to assess the balance of benefits and harms.

---

## 7. Citation hygiene

An earlier round of work on this module cited four PMIDs. On re-verification only **29581878** (Pierce et al., GeoPref subtype) resolved to a retrievable record. **40438470, 38550425 and 27011784 did not.** That does not make them wrong, but nothing in this document rests on them, and they must be checked before appearing in any submission.

---

## 8. Recommended sequence of work

1. Fix timestamps (§3.1) — correctness, cheap, unblocks every timing measure that survives.
2. Switch to landscape with edge-placed stimuli (§3.3) — free 3.5× signal gain.
3. Age-conditioned anthropometry (§3.4) — removes a 55% distance error on the youngest children.
4. Enable the facial transformation matrix and gate on head pose (§5.2 stages 4) — kills the yaw confound.
5. Unify handedness in one place with a test (§3.6).
6. **Measure achieved frame rate on real target hardware** before anything else is designed around it.
7. GPU delegate with capability gate and single-thread construction (§3.8).
8. Remove or relabel the undefendable metrics (§4.2) and restrict scoring to 12–48 months (§4.3).
9. Implicit calibration harvesting (§5.2 stage 6).
10. Prototype the iCatcher+-style classifier as a cross-check (§5.3).

---

## 9. Sources

Full citation lists with URLs live in the three research reports this document synthesises. Primary anchors:

- Perochon et al., *Nature Medicine* 2023 — SenseToKnow: [PMC10579093](https://pmc.ncbi.nlm.nih.gov/articles/PMC10579093/) · remote validation [PMC12107789](https://pmc.ncbi.nlm.nih.gov/articles/PMC12107789/)
- Erel et al., *AMPPS* 2023 — iCatcher+: [doi:10.1177/25152459221147250](https://doi.org/10.1177/25152459221147250)
- Pierce et al. — GeoPref subtype: [PubMed 29581878](https://pubmed.ncbi.nlm.nih.gov/29581878/) · large-scale validation [PMC8917231](https://pmc.ncbi.nlm.nih.gov/articles/PMC8917231/)
- Elsabbagh et al., *Biol Psychiatry* 2013 — disengagement: [PMC3715700](https://pmc.ncbi.nlm.nih.gov/articles/PMC3715700/)
- Sampling rate and saccade detection: [PMC7141092](https://www.ncbi.nlm.nih.gov/pmc/articles/PMC7141092/)
- Salvucci & Goldberg, ETRA 2000 — I-DT / I-VT: [PDF](https://www.cs.drexel.edu/~dds26/publications/Salvucci-ETRA00.pdf)
- Casiez et al., CHI 2012 — One Euro filter: [PDF](https://gery.casiez.net/publications/CHI2012-casiez.pdf)
- MediaPipe 3D face transform: [blog](https://developers.googleblog.com/mediapipe-3d-face-transform/) · hardcoded camera in [face_geometry_from_landmarks_graph.cc](https://github.com/google-ai-edge/mediapipe/blob/master/mediapipe/tasks/cc/vision/face_geometry/face_geometry_from_landmarks_graph.cc)
- MediaPipe Iris depth: [research.google](https://research.google/blog/mediapipe-iris-real-time-iris-tracking-depth-estimation/)
- Toddler tracking accuracy: [Front Psychol 2018;9:803](https://www.frontiersin.org/journals/psychology/articles/10.3389/fpsyg.2018.00803/full)
- MacLachlan & Howland 2002 — paediatric IPD: [doi:10.1046/j.1475-1313.2002.00023.x](https://onlinelibrary.wiley.com/doi/abs/10.1046/j.1475-1313.2002.00023.x)
- USPSTF ASD screening (I statement): [link](https://www.uspreventiveservicestaskforce.org/uspstf/recommendation/autism-spectrum-disorder-in-young-children-screening)

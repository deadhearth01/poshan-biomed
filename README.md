# Poshan

**Offline-first child health screening for ages 0–5, built for Anganwadi and ASHA settings in India.**

An Android app that screens a child across three domains in about fifteen minutes, on a mid-range phone, with no network. Everything is computed on the device. Everything shows its working.

---

## Contents

- [What it does](#what-it-does)
- [What it deliberately does not do](#what-it-deliberately-does-not-do)
- [How the app works](#how-the-app-works)
- [Module 1 — Growth and everyday skills](#module-1--growth-and-everyday-skills)
- [Module 2 — Looking and attention](#module-2--looking-and-attention)
- [Module 3 — Sunlight and vitamin D](#module-3--sunlight-and-vitamin-d)
- [On-device machine learning](#on-device-machine-learning)
- [Offline-first architecture](#offline-first-architecture)
- [Reports and export](#reports-and-export)
- [Design principles](#design-principles)
- [Research and references](#research-and-references)
- [Building](#building)
- [Repository layout](#repository-layout)

---

## What it does

| Module | Measures | Method |
|---|---|---|
| **1. Growth and everyday skills** | Developmental milestones, anthropometry, feeding, vaccines | Caregiver questionnaire + WHO growth standards |
| **2. Looking and attention** | Social attention, response to name, joint attention, motor | On-device gaze tracking + caregiver questionnaire |
| **3. Sunlight and vitamin D** | Skin type, sun exposure adequacy, deficiency risk | Camera colorimetry + documented risk factors |

Each module produces a plain-language result, a full question-and-answer transcript, and the arithmetic behind every number.

## What it deliberately does not do

It **does not diagnose**, and it never names a condition — not in the UI, not in the report, not in the exports.

Three specific restraints are worth stating because they cost capability:

- **No saccade metrics.** A phone camera runs at 15–30 fps. Saccades are not resolvable at that rate, and neither is eye-region versus mouth fixation on a 6″ screen. The module reports only slow, robust quantities.
- **No vitamin D level.** There is no validated non-invasive biomarker for vitamin D status. The 7-dehydrocholesterol reaction is driven by UV-B near 295 nm, which a phone sensor cannot see; circulating 25(OH)D sits four orders of magnitude below optical detectability. Module 3 reports *risk from documented factors*, not a concentration.
- **No dose figures on screen.** Supplementation preparations differ by an order of magnitude, so printing "400 IU" invites a caregiver to convert wrongly. The app says "the daily drops"; the bottle and the health worker supply the amount. `MAX_SAFE_DAILY_IU = 400` is enforced in code and never exceeded.

---

## How the app works

### The visit

```
Sign in  ──►  Select or add child  ──►  Home: three module tiles
                                            │
        ┌───────────────────────────────────┼───────────────────────────────┐
        ▼                                   ▼                               ▼
   Module 1                            Module 2                        Module 3
   intro → guided walkthrough → questions → measurements
        │                                   │                               │
        └───────────────────────────────────┴───────────────────────────────┘
                                            ▼
                          Results ──► Report ──► PDF / Markdown / SMS
```

Every module follows the same shape: **module intro → step-by-step walkthrough with pictures → the task itself**. The walkthrough is one instruction per screen with a green tick and a red cross, because a caregiver who reads slowly still recognises symbols — and every instruction in it is a condition the measurement is only valid under.

### A session is a visit, not a module

All three modules write into one `AssessmentSession`, keyed to the calendar day. Answers persist on **every single answer**, not at a submit step, so closing the app mid-questionnaire loses nothing.

Reopening a module does one of three things:

- **Not started** → opens
- **Partly done** → resumes silently, answers intact
- **Complete** → asks whether to view the result or run it again; running again creates a *separate dated assessment* rather than overwriting, so repeats become a trend rather than a replacement

### Catch-up questions

A child first seen at 11 months is asked the 0–3, 4–6 and 7–9 month questions too. A skill missed from a band the child has already passed is the delay signal — they are late for it, not approaching it — so that outranks every other rule in the scoring.

Answers are filed under **the band each question belongs to**, not the band of the visit, so the child's profile shows real coverage across the whole 0–5 road.

---

## Module 1 — Growth and everyday skills

### Milestones

123 questions across nine age bands (0–3, 4–6, 7–9, 10–12, 13–18, 19–24, 25–36, 37–48, 49–60 months), from the AIIMS Delhi developmental milestones chart, each with its own illustration.

Four responses, scored:

| Response | Score | Flag |
|---|---|---|
| Yes | 1.0 | — |
| Maybe | 0.5 | — |
| Not yet | 0.0 | — |
| **Not sure** | 0.0 | **flagged** |

"Not sure" is not a missing value. A caregiver who cannot say whether their child does an everyday thing has, in effect, not observed it — so it scores as zero *and* raises a flag, and it is counted separately from other concerns everywhere it appears.

**Developmental quotient:**

```
DQ = (developmental age ÷ chronological age) × 100
```

Developmental age accumulates band by band: the fraction of credit earned in each band multiplied by that band's width in months.

**Domain status**, in precedence order:

```
any red flag                         → delayed
any missed milestone from an earlier band → delayed
concerns ≥ half the current band     → delayed
concerns ≥ 1                         → some concern
otherwise                            → on track
```

### Growth

WHO Child Growth Standards, real LMS tables from the official spreadsheets, Box-Cox transform:

```
z = ((measurement / M)^L − 1) / (L · S)
```

with the inverse `value = M(1 + L·S·z)^(1/L)` for plotting reference curves.

`S` is a **coefficient of variation**, not an absolute standard deviation — an earlier version divided by it as though it were absolute, which silently produced wrong z-scores for every child.

Weight-for-age, length/height-for-age, BMI-for-age and MUAC-for-age, boys and girls, per month.

**Arm circumference is prompted, not routine.** It is asked for when weight-for-age or height-for-age falls at or below −2 SD. The field stays available on every eligible child regardless, because MUAC is an *independent* criterion for severe acute malnutrition — a tall, wasted child can sit above −2 on both and still be under 115 mm.

### Corrected age for prematurity

```
corrected = chronological − (40 − gestational weeks) × 7 / 30.4375
```

Applied only below **37 weeks** (the definition of term) and only under 24 months. An earlier gate at 40 weeks was subtracting up to three weeks from clinically term babies and scoring them against an easier question set.

---

## Module 2 — Looking and attention

Modelled on the paired-preference paradigm and on Duke's SenseToKnow, which reported AUC 0.90, sensitivity 87.8%, specificity 80.8% on 475 toddlers.

### Age routing

| Age | What runs |
|---|---|
| **Under 9 months** | Caregiver questionnaire only |
| **9–24 months** | Camera + questionnaire, unscored below 12 months |
| **2–5 years** | Full camera protocol + questionnaire |

The camera protocol does not run below 9 months because every probe in it depends on a skill the child does not yet have: gaze following is not reliably present before 9 months, response to name is not a usable probe until near 12, and young infants' attention is "sticky" enough that a paired trial measures whichever side appeared first. Running them anyway produces numbers that look like measurements and carry no information.

### The probes

| Probe | Question | Trials (9–24mo / 2–5y) |
|---|---|---|
| **Paired preference** | Given a social and a non-social option, which is chosen? | 4 / 6 |
| **Response to name** | Does the child turn to their own name? | 3 / 3 |
| **Gaze following** | Does the child follow where a face looks? | 3 / 3 |
| **Disengagement** | Can the child shift from centre to periphery? | — / 3 |
| **Bubble game** | Motor and reaction speed | after the gaze trials |

Side alternates every trial and condition crosses every second trial, so side and condition are never confounded — a child who simply prefers the left of a screen cannot produce a false social-preference score.

### The measurement

```
socialAttentionRatio = time on the social half
                       ───────────────────────────────────────
                       time on social + time on geometric
```

Gaze maps to a side at `x < 0.42` / `x > 0.58`, with a dead zone straddling the panel edges so an ambiguous gaze counts for neither.

**Matching is what makes this mean anything.** Both halves must match on mean luminance, motion energy, colour saturation, spatial frequency and element count. If the geometric half is brighter or busier, every child looks at it and the measurement is of salience, not social preference. See [`docs/04-module2-stimulus-design.md`](docs/04-module2-stimulus-design.md).

### Landscape

The session runs in landscape. In portrait each half of a 6.1″ screen subtends **4.9°** at 40 cm, and a face inside it about 3.4° — below the ~5–8° infant face processing needs. Landscape roughly doubles it to **10.8°**, close to what an iPad at 60 cm achieves. The phone must be **propped, not held**: the AOI thresholds assume a fixed frame, and a hand-held phone makes the operator's movement indistinguishable from the child's.

### Validity gating

Every trial opens a window and counts usable frames. Below 50% usable, the trial is discarded. Below the session threshold, the result is reported as **inconclusive** — explicitly *not* a low score. An inconclusive session says the camera could not see; it says nothing about the child.

### Privacy

**No image of a child is ever stored.** Frames are processed and discarded. Only derived numbers persist — nothing reaches the disk or the network that could reconstruct a face.

---

## Module 3 — Sunlight and vitamin D

Two layers, kept deliberately apart because they carry different weight.

### Layer 1 — Skin typing (measured)

```
camera frame (RGBA_8888, AE + AWB locked, effects off)
  → rotate once to display orientation
  → on-device segmentation → skin mask
  → grey-card illuminant gains, applied per pixel
  → specular rejection (dichromatic model)
  → skin/non-skin rejection
  → per-channel median
  → sRGB linearise → CIE XYZ (D65) → L*a*b*
  → ITA° = arctan((L* − 50) / b*) × 180/π
  → Fitzpatrick type I–VI
```

`a*` is computed and **discarded** — it carries the haemoglobin signal (perfusion, crying, warmth), which is not melanin. Dropping it is what makes ITA° a melanin proxy rather than a general colour measure.

**Why the grey card is mandatory.** A white shirt looks yellow under a bulb and blue in shade; the shirt did not change, the light did. The card is known to be neutral, so whatever colour it shows belongs to the room. Measured on a real patch: the same skin reads **6.5° of ITA different** before and after correction — an entire skin-type band. No card, no saved reading.

**Acquire-and-freeze.** The reading locks when 12 consecutive frames hold within **±1.5°** with a usable card, ≥50% skin, and `b* > 0.5`. It saves the *median* frame of the window, not the last — the last is the one most likely to catch the start of a movement — and reports its own spread ("steady to ±0.4°"). Without this, whatever number happened to be on screen when a thumb landed would be the saved one, and the press itself shakes the phone.

No contrast enhancement of any kind. CLAHE and similar rewrite `L*` per tile while leaving `b*` alone, corrupting the arctan ratio — the most common defect in published phone-ITA pipelines.

### Layer 2 — Deficiency risk (modelled)

A weighted additive score over documented risk factors: skin type, time outdoors, covering, air quality, solar altitude for the latitude and date, supplementation adherence, prematurity, maternal status, dietary calcium and vitamin D. One interaction term — dark skin combined with little sun, because required exposure rises as available exposure falls.

**Banding is a fraction of each child's own achievable maximum**, not an absolute count:

```
LOW       < 0.30
MODERATE  0.30 – 0.55
HIGH      ≥ 0.55
```

Absolute thresholds were tried and failed twice: the top tier saturated (realistic urban cases scored 13–23 against a threshold of 6, so everything above the line was indistinguishable), and skin type alone reached Moderate — flagging a rural dark-skinned infant who was outdoors daily and reliably taking the drops, for whom nothing was actionable and nothing was wrong.

**Stated assumptions** (in the KDoc, so they can be attacked directly): weights are ordinal from literature effect directions, *not* fitted coefficients — no cohort of under-fives with paired skin readings and serum results exists to fit them to. Tiers rank children against each other; they are not probabilities.

`assess()` accepts external `RiskContribution`s, so a future model plugs in without touching the banding, the UI or the explanation screens.

---

## On-device machine learning

Everything runs locally. No inference leaves the phone.

| Model | Size | Purpose |
|---|---|---|
| **MediaPipe Face Landmarker** | `face_landmarker.task` | 478 landmarks including iris, blendshapes, facial transformation matrix — drives gaze, head pose, blink |
| **MediaPipe Selfie Multiclass** | 16 MB `.tflite` | Skin segmentation for module 3 |

### Face and gaze

Face Landmarker in `LIVE_STREAM` mode with a GPU delegate (gated on GLES 3.1, built lazily on the analysis thread because the delegate must be created and used on the same thread). Four correctness fixes are documented in `FaceGazeAnalyzer`: monotonic timestamps from `imageProxy.imageInfo.timestamp`, explicit rotation and mirroring via a single `Matrix`, one mirroring point only, and `setOutputFacialTransformationMatrixes(true)` for real head pose rather than a landmark proxy.

**Distance** comes from age-conditioned anthropometry — interpupillary distance runs from 40.5 mm at birth to ~63 mm in adults, and using the adult figure for a six-month-old overestimates distance by 55%. Iris diameter provides an independent cross-check; divergence between the two is a quality flag.

**Head-yaw confound:** apparent gaze ≈ `g − 0.83ψ`. Outside roughly ±15° of yaw, head rotation injects more error than the gaze signal carries, so those frames are gated out.

### Skin segmentation

The selfie multiclass segmenter replaced fixed YCbCr chrominance thresholds (Chai & Ngan 1999), which remain as a fallback. The thresholds were derived under neutral light, so under a warm bulb genuine skin drifts outside them and the whole patch is discarded as "not skin" — the operator sees a refusal and the arm plainly is an arm. The model decides from shape and context, and correctly rejects wooden furniture and tan cloth, which sit inside the skin chrominance box.

The model decides when available; the colour rule is a **fallback, not a second vote** — requiring both to agree would reinherit the exact warm-light failure the model was brought in to fix.

### What is not used

No cloud inference, no face recognition, no identity matching, no emotion classification. The face model produces geometry, and the geometry produces numbers.

---

## Offline-first architecture

Designed for a village with no signal, on a phone with no spare storage.

### The local store is the record

Not a cache of one. Every write goes to disk first; sync is a separate concern that may never happen.

`JsonFileStore` handles durability explicitly:

1. Serialise to a temp file, `fsync` it — without the flush, the rename can reach disk before the bytes do, leaving a file that exists and is empty
2. Move the current file aside as `.bak`
3. Rename the temp into place

Every intermediate state is recoverable. An earlier version did `delete()` then `rename()`, so a crash in the gap destroyed the record it was updating.

Reads try the backup before giving up. A parse failure previously fell through to an empty list — the app showed no children, and the next write persisted that emptiness, turning a recoverable read error into permanent loss.

### Sync

Appwrite, when a network appears. WorkManager for periodic sync plus a 3-second foreground loop while the app is open. Writes are local-then-remote, never the reverse.

The client grants only roles its own session holds — an earlier version included `Permission.read(Role.label("admin"))`, which a client may not grant, and every write returned 401. Admin access is a collection-level policy.

### Everything works without a network

Question banks, WHO LMS tables, the immunisation schedule, solar altitude, both ML models, all scoring, PDF and Markdown export. The only network-dependent features are cloud backup and sending a report.

---

## Reports and export

| Format | For |
|---|---|
| **On-screen** | Plain sentence per module, numbers behind drawers |
| **Plain text** | Pasting into a message |
| **PDF** | Printing and carrying to a clinic |
| **Markdown** | Machine-readable — tables parse, renders anywhere |
| **SMS** | Pre-filled to a doctor's number the parent saved |

The results screen carries **no raw numbers on its front**. A parent cannot act on "engagement 43%" and will read any percentage as a mark out of a hundred for their child. Each module gets one plain sentence and a status colour; every number that produced it is one tap away, including the arithmetic.

Tapping any module card opens the full transcript: every question, what was answered, and a tally of answered / to watch / not sure.

In the PDF, flagged answers carry a `!` marker **as well as** colour — a report that only works in colour does not work.

SMS uses `ACTION_SENDTO`, which opens the messaging app pre-filled and stops. The parent presses send. The app never transmits a child's health data on its own.

---

## Design principles

**Show the working.** Every screen that produces a number has a button that explains it — plain language first, then the technical basis, then the evidence, then the stated limitation. A screening tool that hides its limits is not trustworthy.

**Refuse rather than guess.** Unusable readings are reported as unavailable, not shown with a caveat. A number on screen gets believed.

**Written for someone who reads slowly.** No medical vocabulary in the interface: not *developmental*, *milestone*, *gaze*, *prophylaxis*, *deficiency*, *melanin*, *rickets*. The real terms live one tap away behind the explanation buttons, where somebody who wants them will look.

**Dynamic haptics.** Feedback varies with what is happening — progress through a walkthrough firms up step by step, a result's severity is felt before it is read, and the skin capture pulses the moment a reading becomes usable, so the operator can watch the child's arm instead of the screen.

---

## Research and references

### Growth

- **WHO Child Growth Standards (2006)** — LMS tables for weight, length/height, BMI and MUAC, 0–60 months
- **AIIMS Delhi** — developmental milestones chart; the question bank's source
- **AIIMS Child Neurology** — red-flag chart
- **National Immunization Schedule (NIS)** and Indian Academy of Paediatrics schedule

### Attention and gaze

- **Jones & Klin (2013)**, *Nature* — decline in eye fixation between 2 and 6 months. Explicitly **not** reproduced here: it measures the eye region specifically, on a calibrated corneal-reflection tracker. The eye region subtends 2–4° on a phone while uncalibrated RGB gaze error is 2–6°. Building the infant phase on the one metric this hardware cannot measure would produce confident numbers with nothing underneath.
- **Pierce et al.** — GeoPref paradigm. Preference for dynamic geometric patterns over social video; highly specific, with sensitivity substantially improved when the Complex Social condition is included.
- **Duke SenseToKnow** — *Early detection of autism using digital behavioral phenotyping*, [Nature Medicine (2023)](https://www.nature.com/articles/s41591-023-02574-3). 475 toddlers aged 17–36 months. AUC 0.90, sensitivity 87.8%, specificity 80.8%, NPV 97.8%, PPV 40.6% against 14.6% for M-CHAT-R/F. 23 digital phenotypes (19 computer-vision, 4 touch) from real filmed movies on a tablet at 60 cm. Its bubble-popping motor task, blink rate, head movement and response-to-name are all mirrored here.
- **2026 eye-tracking screening meta-analysis** — the ≤18 / 19–36 month subgroup split used for the age phases
- **2025 systematic review of automated infant eye tracking** (793 articles) — found the literature too heterogeneous in paradigm, dependent variable and age for meta-analysis; the reason the infant window here is observation rather than scoring
- **Gazefinder** — gaze-following joint-attention probe design
- *Comprehensive Developmental Guide for Early Autism Identification in the Indian Population* — the early-marker question bank

### Skin colorimetry and vitamin D

- **Chardon, Cretois & Hourseau (1991)** — ITA° definition and its six category boundaries
- **Del Bino & Bernerd (2013)** — ITA° validated against extracted melanin content across the full human range
- **Shafer (1985); Klinker, Shafer & Kanade (1988)** — dichromatic reflection model, the basis for specular rejection
- **Chai & Ngan (1999)** — YCbCr skin-colour thresholds (77 ≤ Cb ≤ 127, 133 ≤ Cr ≤ 173)
- **Von Kries** — chromatic adaptation, the grey-card correction
- **Indian Academy of Paediatrics (2021)** and the **Global Consensus on Prevention and Management of Nutritional Rickets (2016)** — universal infant supplementation
- **RBSK** screening card item B5 — the rickets sign list
- **Hyper-Skin-2023** and **Meta BioSkin** — evaluated and rejected for status estimation: they cover 380–1000 nm and 400–1000 nm respectively, while 7-dehydrocholesterol absorbs near 295 nm

### Negative results worth recording

Three things were investigated and deliberately **not** built, each because the evidence did not support it:

1. **Hyperspectral vitamin D estimation** — wrong wavelength range, and 25(OH)D at ~50 nmol/L is four orders of magnitude below optical detectability
2. **A vitamin D risk questionnaire as a screening instrument** — no validated under-5 questionnaire exists, IAP explicitly recommends against screening healthy children, and serum testing is unavailable below district hospital level. The real gap is delivery: correct administration of prescribed drops has been reported below 10%.
3. **Saccade, gap-overlap and eye-region fixation metrics** — not defensible at 15–30 Hz on a 6″ screen

---

## Building

```bash
git lfs install          # large binaries are LFS-tracked
git clone https://github.com/deadhearth01/poshan-biomed.git
cd poshan-biomed/android
```

Create `android/local.properties` (gitignored):

```properties
sdk.dir=/path/to/Android/Sdk

poshan.appwrite.endpoint=https://<your-appwrite-host>/v1
poshan.appwrite.projectId=<your-project-id>
poshan.appwrite.databaseId=poshan_db
poshan.appwrite.enabled=true
```

Then:

```bash
./gradlew assembleDebug
```

### Configuration precedence

```
-P command line  →  environment variable  →  local.properties / secrets.properties  →  default
```

Environment variables uppercase the key and replace dots: `poshan.appwrite.projectId` → `POSHAN_APPWRITE_PROJECTID`. **No committed file carries a backend**, so CI configures a build without writing one and a fork points at its own Appwrite with nothing to edit.

Requires JDK 17, `compileSdk` 36, `minSdk` 24.

---

## Repository layout

```
android/app/src/main/java/com/teamposhan/biomed/
├── data/
│   ├── local/          JsonFileStore — durable offline persistence
│   ├── model/          Serializable domain models
│   ├── remote/         Appwrite sync
│   ├── repo/           Single source of truth
│   ├── reminder/       Vaccine due/overdue notifications
│   └── sync/           WorkManager sync
├── domain/
│   ├── age/            Chronological and corrected age
│   ├── attention/      Protocol, recorder, gaze, head pose, pursuit target
│   ├── growth/         WHO LMS tables, z-scores, MUAC trigger
│   ├── immunization/   NIS + IAP schedule
│   ├── markers/        Early-marker question bank
│   ├── milestones/     Question bank, age bands, band coverage
│   ├── nutrition/      Feeding practice and household background
│   ├── progress/       Per-module completion
│   ├── report/         Text, Markdown, PDF, answer transcript
│   ├── risk/           Composite risk, vitamin D module and risk model
│   ├── scoring/        DQ and domain status
│   └── skin/           Colorimetry, filters, segmentation, typing, lock
└── ui/
    ├── components/     Design system, guided steps, haptics
    ├── navigation/     Routes
    ├── screens/        All screens
    └── theme/          Material 3 Expressive

docs/           Architecture, module design, grey-card method, vitamin D model
design/         Artwork prompt files for every illustrated question set
appwrite/       Backend schema provisioning
growth-charts/  WHO reference data
tools/          Build and release scripts
```

Deeper detail lives in [`docs/`](docs/) — in particular [`04-module2-stimulus-design.md`](docs/04-module2-stimulus-design.md) for why stimulus matching decides whether the measurement means anything, [`05-grey-card.md`](docs/05-grey-card.md) for the colour correction in plain language, and [`06-vitamin-d-module.md`](docs/06-vitamin-d-module.md) for the risk model and its stated assumptions.

---

## Licence

Not yet specified.

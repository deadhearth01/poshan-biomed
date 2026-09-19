# Module 3 — Skin Typing & Vitamin D Risk

What this module claims, what it refuses to claim, and how every number is produced.

Companion to `05-grey-card.md` (the colour-correction step in plain language).

---

## 1. The claim, stated precisely

This module does **two** things, and keeping them apart is the whole design:

| | What it is | Standing |
|---|---|---|
| **Skin typing** | ITA° from a camera reading → Fitzpatrick type I–VI | A **measurement**, on published ground |
| **Deficiency risk** | Skin type + exposure + supplementation + background → Low / Moderate / High | A **model**, on stated assumptions |

They are separate cards in the UI and separate blocks in the report, deliberately. Collapsing
them would let the model borrow the measurement's credibility.

### What it does not claim

It does not measure vitamin D. Not approximately, not as a trend. There is currently **no
validated non-invasive biomarker for vitamin D status** — nothing optical, nothing in sweat,
saliva or breath that has cleared validation. Any tool in this space is doing risk
stratification, not measurement. That is a fact about the field, not a limitation of this app.

Three physical walls, for the record:

1. **Wrong wavelength.** The reaction that makes vitamin D is driven by UV-B near 295 nm. A
   phone sensor does not see below ~400 nm, and the lens glass absorbs UV-B before it
   arrives. The relevant light never reaches the sensor.
2. **Wrong concentration.** Circulating 25(OH)D sits near 50 nmol/L — roughly four orders of
   magnitude below where a chromophore such as bilirubin becomes detectable through skin.
   There is no optical signal to find.
3. **Wrong effect size.** Skin tone, which the camera *can* measure, explains about 5% of the
   variance in measured blood levels (~0.6 ng/mL per 10° of ITA). Across the full Indian skin
   range that is ~2 ng/mL, against an individual SD near 8.8 ng/mL. The signal is a quarter
   the size of the noise.

A model trained to emit "low/normal" from a photograph would be fitting the demographics of
its training set, and would be most confident exactly where it is most wrong.

---

## 2. Skin typing — the measured half

### The pipeline

```
camera frame (RGBA_8888, AE+AWB locked, effects off)
  → rotate once to display orientation
  → on-device segmentation (MediaPipe selfie multiclass) → skin mask
  → grey-card illuminant gains applied per pixel      ← see 05-grey-card.md
  → specular rejection (dichromatic model)
  → skin/non-skin rejection (model mask, YCbCr fallback)
  → per-channel median
  → sRGB linearise → CIE XYZ (D65) → L*a*b*
  → ITA° = arctan((L* − 50) / b*) × 180/π
  → Fitzpatrick type I–VI
```

Each rejection stage removes pixels the next stage would be misled by. Order is not
arbitrary — white balance precedes segmentation because the segmentation thresholds assume a
neutral illuminant.

### Why ITA° and not something else

ITA° is what the dermatology literature uses for skin typing, and it is validated for exactly
that: Del Bino & Bernerd (2013) measured it against extracted melanin content across the full
human range and found a strong monotonic relationship. Chardon, Cretois & Hourseau (1991)
defined the angle and its category boundaries for this purpose.

`a*` is computed and **discarded**. It carries the haemoglobin signal — blood perfusion,
flushing, a crying child — which has nothing to do with melanin. Dropping it is what makes
ITA° a melanin proxy rather than a general colour measure.

### Type boundaries (published, unmodified)

| Type | ITA° | Label | Exposure multiplier | Risk points |
|---|---|---|---|---|
| I | > 55 | Very light | 0.7× | 0 |
| II | 41 – 55 | Light | 1.0× | 0 |
| III | 28 – 41 | Intermediate | 1.5× | 1 |
| IV | 10 – 28 | Tan | 2.0× | 2 |
| V | −30 – 10 | Brown | 3.0× | 3 |
| VI | < −30 | Dark | 4.0× | 4 |

All six kept, not collapsed into three. Indian children cluster Fitzpatrick IV–V (roughly
10–35°), so a three-band scheme would put nearly everyone in one bucket and discriminate
nothing.

### Known weaknesses of ITA°

Documented because they bound what the number is worth.

**Error budget is tight.** Sensitivity of ITA° to L\* error, and distance to the nearest band
edge:

| Skin | ITA° | °/unit L\* | L\* error to flip band |
|---|---|---|---|
| Light | 35.2 | 2.25 | 2.6 |
| Intermediate | 24.0 | 2.66 | **1.5** |
| Brown (typical Indian) | −14.0 | 2.70 | 5.9 |
| Dark | −33.7 | 2.20 | **1.7** |

One-and-a-half units of L\* flips a band — within what a specular highlight or a slightly-off
white balance will do. The filter chain exists to buy back that margin; it is not polish.
Readings within 4° of a boundary are flagged **borderline** in the UI and report.

**The arctan blows up silently at b\* ≤ 0.**

```
L*=45  b*= 0.5  →  ITA −84.3   Dark
L*=45  b*= 0.0  →  UNDEFINED
L*=45  b*=−2.0  →  ITA +68.2   Very light   ← same dark skin
```

A sign flip in b\* throws the angle into the opposite quadrant and reports dark skin as very
light, confidently, with no error raised. `itaFrom()` refuses outright at `b* ≤ 0.5`.

**Band widths are uneven** — Intermediate 13°, Tan 18°, but Brown 40° and Dark 60°. Precision
is worst where most users sit, and the coarse bands lump visibly different children together.

**Not injective.** ITA° is a 2D projection; different (L\*, b\*) pairs give the same angle.

**Colour space.** The pipeline assumes sRGB. Devices emitting Display P3 would skew this and
that is currently unhandled.

---

## 3. Deficiency risk — the modelled half

A **screening prior**: how likely is this child to be deficient, given factors whose
association with the outcome is documented. Same class of instrument as a cardiovascular risk
score — a way of deciding who warrants attention first.

### Factors and weights

| Factor | Points | Source of the effect direction |
|---|---|---|
| Skin type | 0–4 | Melanin absorbs UV-B over the same 290–315 nm band as previtamin D3 synthesis |
| Dark skin **×** little sun (interaction) | 2 | Required exposure rises while available exposure falls — compounds rather than adds |
| Mostly indoors | 2 | Strongest modifiable predictor in paediatric cohorts |
| Skin covered outdoors | 2 | Established independent predictor |
| Heavy smoke / dust / haze | 1 | Polluted Delhi areas ≈ half the 25(OH)D of cleaner areas nearby |
| Sun too low this season | 2 | Below ~45° solar altitude, atmospheric path removes effectively all UV-B |
| Daily drops not started | 3 | IAP 2021; Global Consensus on Nutritional Rickets 2016 |
| Drops missed some days | 1 | Correct delivery of prescribed drops reported below 10% |
| Breastfed without drops | 2 | Unsupplemented human milk ≈ 25 IU/L against a 400 IU/day requirement |
| Born early | 1 | Third-trimester transfer builds most neonatal stores |
| Mother likely low | 2 | Cord blood 25(OH)D tracks maternal status closely |
| Little dietary calcium | 1 | Low calcium causes rickets even where vitamin D is adequate |
| Little dietary vitamin D | 1 | Minor contributor in unfortified diets |

### Banding — and why it is a fraction

Tiers are a **share of the worst score this particular child could have scored**, not an
absolute point count.

```
LOW       < 0.30
MODERATE  0.30 – 0.55
HIGH      ≥ 0.55
```

Absolute thresholds were tried first and failed twice. Both failures are easy to reintroduce,
so they are recorded here:

- **The top tier saturated.** Realistic urban cases scored 13–23 against a threshold of 6, so
  everything above the line was indistinguishable. Where indoor living and darker skin are
  the norm, nearly every child would read High — the same band-compression failure that makes
  a three-band ITA scheme useless.
- **Skin type alone reached Moderate.** A rural dark-skinned infant, outdoors daily and
  reliably taking the drops, scored 4 on pigmentation alone. That child is doing everything
  available to them; flagging them tells the health worker nothing actionable and tells the
  parent something untrue.

Scoring against each child's own ceiling fixes both, and keeps an infant and a four-year-old
comparable even though different factors apply to them.

Verified behaviour:

```
rural infant, dark, outdoors, ON drops         4/23  0.17  LOW
light infant, outdoors, on drops               1/23  0.04  LOW
urban infant, brown, indoors, misses some      9/23  0.39  MODERATE
no skin reading, urban infant, no drops        8/17  0.47  MODERATE
urban infant, brown, indoors, NO drops        15/23  0.65  HIGH
preterm infant, dark, indoors, mother low     14/23  0.61  HIGH
worst realistic                               23/23  1.00  HIGH
```

### Stated assumptions

This is a proof of concept. Each assumption is listed so it can be attacked directly rather
than reverse-engineered from the arithmetic.

1. **Additive, with one exception** — the dark-skin × low-sun interaction. Every other pair is
   assumed independent, which is certainly wrong in detail.
2. **Weights are ordinal, not fitted.** They encode direction and rough relative magnitude
   from the literature. They are **not** regression coefficients, because no cohort of
   under-fives with paired skin readings and serum results exists to fit them to.
3. **Tiers are relative, not probabilities.** "High" means this child carries most of the risk
   factors that could apply to them — not "X% chance of deficiency".
4. **Skin type treated as constitutive.** The reading is taken on the inner forearm precisely
   to approximate unexposed baseline pigmentation rather than tan.
5. **Self-reported exposure taken at face value.** Recall bias is well known and unmeasured.

### Designed to be superseded

`VitaminDRiskModel.assess()` accepts `extra: List<RiskContribution>`. A second optical
channel, a fitted coefficient set, or an actual biomarker adds its contributions and the
banding, the UI and the explanation screen pick them up unchanged. Nothing hard-codes the
factor set.

---

## 4. What the module does regardless of any of this

The risk score is the interesting part. It is not the useful part.

**Every infant under twelve months needs 400 IU/day, breastfed or not, whatever their skin
type, wherever they live.** That is universal guidance and does not depend on any
measurement this app makes. The module leads with that action, then asks whether the drops
are actually being given — because the documented gap is not diagnosis, it is delivery.

Hard limits, enforced in code:

- `MAX_SAFE_DAILY_IU = 400`. The app will never suggest a higher dose
- Never suggests 60,000 IU units or injections
- Never prompts a blood test for a healthy child — IAP explicitly recommends against
  screening healthy children
- No output ever states or implies a vitamin D level

Alongside: the **RBSK item B5** sign check (wrist widening, rachitic rosary, bowing, unable to
walk, leg pain, seizure). Any single sign triggers referral. There is no weighting, because
no published study reports the accuracy of these signs as a screening test in a general
population and inventing a weighting would be false precision. A present sign outranks the
risk score entirely — a sign is something seen on the child; the score is a prior.

---

## 5. Files

| File | Contains |
|---|---|
| `domain/skin/SkinColorimetry.kt` | sRGB → L\*a\*b\*, von Kries gains, ITA°, refusal conditions |
| `domain/skin/SkinFilters.kt` | Specular rejection, segmentation, median, full pipeline |
| `domain/skin/SkinSegmenter.kt` | On-device MediaPipe skin mask, YCbCr fallback |
| `domain/skin/SkinTyping.kt` | ITA° → Fitzpatrick type, borderline margin |
| `domain/skin/SkinPatchSampler.kt` | Reticle → pixels, in display orientation |
| `domain/risk/VitaminDRiskModel.kt` | Risk factors, weights, fraction banding |
| `domain/risk/VitaminDModule.kt` | Prophylaxis advice, RBSK signs, sun window, counselling |
| `ui/screens/SkinCaptureScreen.kt` | Capture, AE/AWB lock, live working drawer |
| `ui/screens/VitaminDCalculation.kt` | Per-child working, risk card, skin type card |

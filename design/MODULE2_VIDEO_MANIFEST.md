# Module 2 — Video Manifest

Every video the assessment needs, what goes in it, and what the app measures from it.

Organised by the **5 assessment categories + 1 quality gate**. One category can reuse a
video; one video can feed more than one category. The scoring column is what actually gets
computed, so if a video does not satisfy its layout rules, that score is invalid.

**Global rules for every file** (details in `MASTER_PROMPT_MODULE2_STIMULI.md`):
landscape, ≥1920 px long edge, 30 fps, H.264 MP4, no text, no logos, Indian cast and
settings, paired panels matched on luminance and motion energy, natural contrast polarity on
faces.

At 40 cm on a 6″ phone: **1° ≈ 118 px**.

---

## Category 0 — Quality gate (not a child-behaviour score)

### V0.1 `getter_<01..06>` — Attention-getters
| | |
|---|---|
| Length | 3 s, seamless loop |
| Count | **6 variants**, visibly different in colour, form and sound |
| Content | One bold shape, centred, expanding–contracting or spinning, **ending in a shrink to a point ≤1.5° (~180 px)** |
| Audio | Soft chime, rising then resolving, synced to the contraction |
| Measures | Face detected, both irises tracked, head pose in envelope, achieved fps, distance. Also harvests the **implicit calibration point** |

Six, not one: a target that varies between appearances measurably raises data quality; an
identical repeat stops working after a few presentations.

---

## Category 1 — Face & Eye Gaze

### V1.1 `face_still_<01..03>` — Single face, direct gaze
| | |
|---|---|
| Length | 10 s |
| Content | One Indian adult, front-facing, eyes open and clearly lit, small natural movements (blink, slight smile, tiny head shift). **No speech.** Face fills the central third |
| Layout | Face centred; nothing else salient in frame |
| Measures | **Face fixation %** = time on face AOI ÷ total valid looking time. Fixation count and dwell runs |

### V1.2 `face_speaking_<01..02>` — Face with speech
| | |
|---|---|
| Length | 15 s |
| Content | Same framing, adult speaks warmly to camera in Hindi/Telugu/English variants — nursery talk, no instructions |
| Measures | Face fixation % under auditory load; comparison against V1.1 |

> **The honest limit.** Your list includes *eye-region fixation %* (eyes ÷ face). The eye
> region subtends **2–4°**; uncalibrated phone gaze error is **2–6°**, and even calibrated
> infrared tracking of toddlers averages 1.3°. **Eyes-versus-mouth is not separable on this
> hardware.** Film these videos so the AOI exists — if a better gaze model lands later the
> footage is ready — but the app must report face-level only. A number for eye-region
> fixation would be fabricated.

---

## Category 2 — Social vs Non-Social Attention

The strongest-evidence category. Paired panels, **edge-placed**, landscape.

### V2.1 `pair_simple_<L|R>_<01..02>` — Original GeoPref
| | |
|---|---|
| Length | 30 s (trim from a 90 s master) |
| Left/right | Two mirrored versions for counterbalancing |
| Social panel | 1–2 Indian children doing simple whole-body actions — clapping, stretching, peek-a-boo, yoga-like moves |
| Geometric panel | Rotating kaleidoscope / pulsing concentric rings / drifting lattice. Rhythmic, repetitive, no narrative |
| Measures | **Social fixation %**, **non-social fixation %**, **social preference ratio** = social ÷ (social + non-social) |

### V2.2 `pair_complex_<L|R>_<01..02>` — Complex Social
| | |
|---|---|
| Length | 30 s |
| Social panel | **2–3 children interacting with each other and with an object** — passing a ball, sharing a toy, building together |
| Geometric panel | As above |
| Measures | Same three, reported separately from V2.1 |

> Run **both**. Combining the original and Complex Social conditions roughly **doubles**
> GeoPref sensitivity — about 35% at 94% specificity, versus roughly half that for the
> original alone. This is the single highest-value content decision in the module.

**Matching is the measurement.** If the geometric panel is brighter, busier or faster, you
are measuring salience, not social preference. Match mean luminance, colour energy and
quantity of motion between panels, and verify side by side before acceptance.

---

## Category 3 — Attention Shifting

### V3.1 `shift_<01..04>` — Centre-to-edge capture
| | |
|---|---|
| Length | 8 s per trial, 4 trials |
| Content | Central animated character holds attention, then a second animated target appears at one edge while the centre **keeps playing** |
| Timing | Centre-only 2.5 s → peripheral target appears → both run 4 s |
| Edge target | ~6°, appears left or right, counterbalanced |
| Measures | **Shift latency** (frames from onset to gaze crossing the midline), **successful shift rate**, **return rate** |

> **What this is and is not.** This is *not* the published gap-overlap disengagement
> paradigm — that places the target at **15° eccentricity**, and a 6″ phone at 30 cm reaches
> a maximum of 13.0° (7.9° at 50 cm). It is geometrically impossible here.
>
> What we can measure is a coarser **shift between screen halves**. Latency is quantised to
> the frame interval — 33 ms at 30 fps — so report it as a **within-session relative index**,
> never against published disengagement norms, whose discriminating effect is ~130 ms, i.e.
> 2–4 frames.

---

## Category 4 — Gaze Following / Joint Attention

The one category where **composition is the measurement**.

### V4.1 `gazefollow_<L|R>_<01..04>`
```
  [ TARGET A ]        [ ADULT FACE ]        [ TARGET B ]
    edge-left            centre               edge-right
```
| | |
|---|---|
| Length | 6 s per trial, 4 trials |
| Beat 1 (0–1.5 s) | Adult faces camera, makes eye contact,小 warm expression |
| Beat 2 (1.5–2.3 s) | Adult turns **head and eyes** decisively to one target — turn completes in **≤800 ms**, exaggerated |
| Beat 3 (2.3–6 s) | Adult holds the look at the target |
| Targets | **Both visible throughout**, identical size and salience, mirrored, ~6°, at the outer edges |
| Measures | **Gaze-following success** (first look after cue lands on cued side), **response latency**, **person→object→person alternation** |

Deliver left-cue and right-cue versions and two target pairs, for counterbalancing.

> The AOI that matters is the **shared-focus region** — the target the adult is attending to
> — not the adult's face. Keep it visually clean and unambiguous.

### V4.2 `name_call_<01..02>` — Response to name backdrop
| | |
|---|---|
| Length | 20 s |
| Content | **Deliberately low-demand.** Gently moving neutral scene — drifting shapes, a slow mobile. No faces, no speech, no strong social pull |
| Measures | Head/gaze turn toward the caller, response rate and **consistency across calls**, coarse latency |

Low-demand on purpose: anything competing with the auditory probe makes a failure to turn
uninterpretable.

---

## Category 5 — Visual Engagement

Computed **across all of the above** rather than from a dedicated video — engagement is a
property of the whole session.

| Measure | Definition |
|---|---|
| Total engaged time | Usable frames with gaze on screen ÷ session length |
| Sustained engagement | Longest continuous on-screen run |
| Interruptions | Count of complete disengagements |

Optionally one dedicated clip:

### V5.1 `engagement_long_<01>`
| | |
|---|---|
| Length | 45 s |
| Content | A gentle narrative — a child building a tower, a simple sequence with a beginning and end. Enough structure to hold attention without a task demand |
| Measures | Sustained engagement over a longer window than any trial provides |

---

## Phase A additions (0–11 months) — different rules entirely

Acuity at birth is ~20/400. The style used everywhere else in the app is **invisible** to a
1-month-old.

| Asset | Age | Content |
|---|---|---|
| `infant_facelike_<01..03>` | 0–3 mo | **Schematic**: three solid black blobs in a top-heavy triangle on white; checkerboards; bullseyes. Feature size **≥5° (~590 px)**, so 2–3 features max |
| `infant_face_<01..03>` | 3–11 mo | **Real faces.** By 3 months the top-heavy-pattern preference has gone and only actual faces hold attention |
| `infant_pursuit_<01..02>` | 0–11 mo | One high-contrast shape drifting slowly across frame, **< 10°/s (~1180 px/s)**. Above that an infant tracks in saccades, not smooth pursuit |

Maximum contrast only — black/white, or black/white plus one saturated accent. No fine
detail: it cannot be resolved and only adds noise.

---

## Summary — what to shoot

| Priority | Files | Why |
|---|---|---|
| **1** | `getter_01..06` | Nothing works without fixation capture |
| **2** | `pair_simple_L/R`, `pair_complex_L/R` | Strongest evidence; complex variant doubles sensitivity |
| **3** | `face_still_01..03` | Most fundamental category |
| **4** | `gazefollow_L/R_01..04` | Composition-critical, hardest to reshoot |
| **5** | `name_call_01..02`, `shift_01..04` | Supporting |
| **6** | `infant_*` | Only needed once the 0–11 month phase is exercised |
| **7** | `engagement_long_01` | Optional |

**Minimum viable set: priorities 1–3** — 6 getters, 4 paired videos, 3 face videos. That
alone supports categories 0, 1, 2 and 5, which is a legitimate screener.

---

## When you send me videos

Tell me, per file: the filename, its category, and for paired videos **which side the social
panel is on**. I'll wire them into `AttentionProtocol`, define the AOIs in normalised
coordinates, and map each to its category's scoring. If a video's layout does not match the
rules above I'll say so rather than scoring it anyway — a misplaced AOI produces a confident
number that means nothing.

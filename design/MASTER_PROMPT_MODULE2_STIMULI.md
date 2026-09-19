# Poshan — Module 2 Stimulus & UX Asset Prompt File

Companion to `MASTER_PROMPT.md`. That file governs the app's *illustration* style. This one
governs **assessment stimuli** — the things a child actually looks at while the camera
measures them — plus the UI assets for the new explanation and report surfaces.

**The distinction matters.** An illustration decorates a screen. A stimulus *is the
measurement instrument*. If its contrast, size, speed or layout is wrong, the number the app
produces is wrong, and no amount of visual polish fixes that. Every constraint below comes
from a measurable requirement, not from taste.

---

## 0. Non-negotiable constraints

| Constraint | Value | Why |
|---|---|---|
| Orientation | **Landscape** | Portrait gives only ±2.25° of separation between screen halves at 40 cm; landscape with edge-placed targets gives ±7.8°. Portrait left/right is a coin flip |
| Paired-stimulus placement | **Outer edges**, not half-centroids | Maximises angular separation |
| Safe margin | 4% of the short axis | `PreviewView` and notches crop the extremes |
| Colour space | sRGB, no ICC profile | Budget devices render profiles inconsistently |
| Faces | **Never invert contrast polarity** | Negative-polarity faces disrupt the newborn face preference — the stimulus stops working |
| Cast | **Indian** faces, clothing, settings | No phone screener has been validated on an Indian cohort; the closest comparator showed specificity of 53.6% for Black children vs 82.7% for White. Cheap to get right now, expensive to retrofit |

At 40 cm on a 6″ 1080×2340 screen: **1° ≈ 118 px**. Use this to convert every size below.
Recompute per device from `DisplayMetrics` at runtime; the figures here are the design target.

---

## 1. Attention-getters — the highest-leverage asset in the module

Used at the start of every session and between trials. This is what earns usable data; a
session that never captures fixation produces nothing regardless of how good the stimuli are.

**Specification**
- Duration **3 seconds**, looping
- Motion: **expand–contract (looming)** and/or slow spin, ending in a **shrink to a point**
  — a shrinking target reliably pulls fixation to a *location*, which is what calibration needs
- Final point ≤ **1.5°** (≈180 px) so the harvested calibration point is precise
- Paired sound: soft chime, rising then resolving, synchronised to the contraction
- **Produce a SET of at least 6**, visibly different in colour, form and sound

> Varying the target between appearances measurably raises data quality; a repeated identical
> target stops working after a few presentations. This is why six, not one.

**Reve prompt template**

```
A single [FORM] centred on a plain [BACKGROUND] background, children's app style,
bold flat vector, thick clean outlines, high colour saturation, no gradients,
no text, no drop shadows, perfectly centred, generous empty margin,
square composition, sRGB.
FORM options: concentric bullseye / five-point star / spiral pinwheel /
  smiling sun / rounded flower / bouncing ball with highlight
BACKGROUND options: white / deep navy / warm cream
```

Deliver each as a **PNG sequence or Lottie**, 30 fps, 3 s loop, alpha background.

---

## 2. Phase A (0–11 months) — Infant observation

Acuity at birth is ~20/400 and focus sits at 20–40 cm, which matches our viewing distance.
The pastel illustration style used elsewhere in the app is **invisible** to a 1-month-old.

**Specification**
- **Maximum contrast only.** Black/white, or black/white with one saturated accent
- **Feature size ≥ 5°** (≈590 px at 40 cm) → **2–3 features maximum per screen**
- Motion **< 10°/s** (≈1180 px/s) — above this an infant tracks in saccades, not smooth
  pursuit, and you are measuring a different thing
- No fine detail anywhere: it cannot be resolved and only adds noise

**Two sub-sets, because the preference changes at ~3 months**

| Sub-set | Age | Content |
|---|---|---|
| A1 | 0–3 mo | **Schematic face-like patterns**: three high-contrast blobs in a top-heavy triangle (two "eyes" above one "mouth"), plus checkerboards and bullseyes |
| A2 | 3–11 mo | **Real faces.** By 3 months the top-heavy-pattern preference has gone and only actual faces hold attention |

```
A1: Three solid black circles arranged in an inverted triangle on pure white,
    two upper circles side by side, one lower circle centred, extremely high
    contrast, bold flat shapes, no facial detail, no outline, no texture,
    landscape composition, large simple forms filling the frame.

A2: Photorealistic warm portrait of an Indian mother's face, front-facing,
    neutral-to-smiling expression, eyes clearly visible and open, even soft
    frontal lighting, plain uncluttered background, no props, no text,
    face filling the central third of a landscape frame, natural contrast
    polarity, sRGB.
```

---

## 3. Phases B & C (12–36 months) — the scored phases

The finding that should drive every asset here: **dynamic stimuli beat static**, and the
regions that discriminate are *the target and activity of characters' shared focus* — not the
faces themselves.

**That reframes the brief.** A portrait is the wrong stimulus. What is needed is a character
**attending to something, with the thing they are attending to also visible in frame.**

### 3.1 Social vs geometric (the GeoPref pair)

Two panels side by side, each occupying an outer edge of a landscape frame.

| Panel | Content |
|---|---|
| Geometric | Dynamic abstract motion — rotating kaleidoscope, pulsing concentric rings, drifting lattice. Rhythmic, repetitive, no narrative |
| Simple social | One or two Indian children doing simple whole-body actions: clapping, stretching, peek-a-boo, yoga-like movements |
| **Complex social** | A busier scene: 2–3 children **interacting with each other and with an object** — passing a ball, sharing a toy, building together |

> Run **both** social variants. Combining the original and Complex Social conditions roughly
> doubles GeoPref sensitivity (35% vs ~17%, at 94% specificity).

**Match the two panels on luminance, colour energy and motion quantity.** If the geometric
panel is simply brighter or busier, you are measuring salience, not social preference.

```
Geometric: Seamless looping animation of brightly coloured rotating geometric
  patterns, kaleidoscope of triangles and circles, continuous smooth rotation,
  flat vector style, saturated primary colours, no characters, no faces,
  no text, black background, vertical portrait panel composition.

Social: Two young Indian children playing together on a plain mat, clapping
  and laughing, warm natural lighting, full upper bodies visible, faces clearly
  lit and front-facing, simple uncluttered background, no text, no logos,
  vertical portrait panel composition, documentary style, natural colours.
```

### 3.2 Gaze following / joint attention

The one asset where composition *is* the measurement.

```
Frame layout (landscape):

  [ TARGET A ]        [ ADULT FACE ]        [ TARGET B ]
    edge-left            centre               edge-right
```

- Adult begins facing the camera, makes eye contact, **then turns head and eyes** decisively
  toward one target
- Head turn completes in **≤ 800 ms**, exaggerated enough to be unmissable
- **Both targets visible throughout**, identical in size and salience, mirrored left/right
- Target subtends ~6°, placed at the outer edge
- The cued target's **shared-focus region is the AOI** — it must be clean and unambiguous

Produce **left-cue and right-cue versions**, and two target pairs, for counterbalancing.

### 3.3 Response to name

Deliberately **low-demand**: a gently moving neutral scene, no speech, no strong social pull.
Nothing on screen should compete with the auditory probe, or a failure to turn is
uninterpretable.

---

## 4. Phase D (37–60 months) — Preschool

Preschoolers show different patterns to complex versus simple dynamic social stimuli, so
scenes get more socially demanding: multiple characters, a visible interaction to parse,
turn-taking, a narrative thread. Still landscape, still edge-placed pairs.

---

## 5. UI assets for the new surfaces

Ordinary app illustration style (`MASTER_PROMPT.md`), not stimulus rules.

| Asset | Use | Prompt seed |
|---|---|---|
| `illus_phase_overview` | Module 2 phase overview header | Parent holding phone at child's eye level, child on lap looking at screen, warm flat illustration, calm palette |
| `illus_stage_setup` | Quality-gate stage intro | Phone propped on a stand at child's eye level with a soft light source, showing correct distance |
| `illus_stage_baseline` | Baseline stage intro | Child watching screen calmly, parent silent and slightly behind |
| `illus_why_sheet` | Explanation sheet header | Simple open-book-and-magnifier motif, friendly, non-clinical |
| `illus_not_completed` | Session-not-completed result | Reassuring, non-failure imagery — a "come back another day" feeling, never a sad child |
| `illus_trajectory` | Trend/timeline section | Simple rising-and-falling line over months, abstract, no numbers |

---

## 6. Delivery checklist

- [ ] Landscape, 16:9 or 20:9, minimum 1920 px on the long edge
- [ ] Animations as Lottie JSON or PNG sequence @30 fps; loops seamless
- [ ] Paired panels colour- and luminance-matched, verified side by side
- [ ] Left/right mirrored versions for every lateralised stimulus
- [ ] Faces: natural contrast polarity, eyes open and clearly visible
- [ ] Cast, clothing and settings recognisably Indian
- [ ] No text of any kind in a stimulus — it is a distractor and untranslatable
- [ ] File names match the drawable ids the protocol builder expects
- [ ] Every stimulus reviewed at arm's length **on a real phone, outdoors**, before acceptance

---

## 7. What this file deliberately does not specify

Exact trial counts, durations and thresholds. Those belong to the protocol
(`domain/attention/AttentionProtocol.kt`) and to `docs/03-module2-attention-gaze.md`, and
they will change as the module is validated. Asset specs should outlive them.

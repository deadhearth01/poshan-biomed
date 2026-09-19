# Module 2 — Stimulus Design

What the child is shown, why each choice is made, and which choices are free versus which
would invalidate the measurement.

Companion to `03-module2-attention-gaze.md` (which covers the tracking pipeline) and
`design/MODULE2_VIDEO_MANIFEST.md` (which is the production shot-list).

---

## 1. The measurement, in one paragraph

Module 2's headline number is:

```
socialAttentionRatio = time looking at the social half
                       ─────────────────────────────────────────
                       time looking at social + time on geometric
```

It is a **forced choice between two things shown at the same moment**. Everything in this
document follows from that single sentence. The child is not being asked "do you like this?"
— they are being asked, implicitly, "given these two, which do you choose?", and the answer
only means something if the two options differ in exactly one respect: whether the content is
social.

---

## 2. The probes

Four scored probes, one quality gate, one non-scored observation.

| Probe | Question it asks | Trials (9–24mo) | Trials (2–5y) | Output |
|---|---|---|---|---|
| **Pair** | Given a social and a non-social option, which is chosen? | 4 | 6 | `socialAttentionRatio` |
| **NameCall** | Does the child turn to their own name? | 3 | 3 | `responseToNameRatio` |
| **GazeFollow** | Does the child follow where a face looks? | 3 | 3 | `gazeFollowRatio`, latency |
| **Disengage** | Can the child shift from centre to periphery? | — | 3 | `disengagementLatencyMs` |
| *Attention-getter* | *(quality gate)* Is tracking good enough to proceed? | between trials | between trials | valid-frame ratio |
| *Movement observation* | *(under 9 months)* — **not used**; that age is the caregiver questionnaire | — | — | — |

Pair trials alternate **side** every trial and **condition** every second trial, so side and
condition are crossed rather than confounded. A child who simply prefers the left side of a
screen cannot produce a false social-preference score.

### Why two pair conditions

`PairCondition` has `SIMPLE_SOCIAL` and `COMPLEX_SOCIAL`. This is not decoration. Reported
sensitivity for the geometric-preference paradigm is substantially higher when the complex
social condition is included than for the simple condition alone (~35% combined). Dropping
Complex Social during production would quietly halve what the module can detect.

- **Simple social** — one child, calm, direct gaze, minimal background
- **Complex social** — several children interacting, more motion, richer scene

---

## 3. Matching — the rule that makes the number mean anything

**Both halves of a pair must be matched on:**

| Property | Why it matters if unmatched |
|---|---|
| **Mean luminance** | A brighter half attracts looking regardless of content. You would be measuring brightness preference |
| **Motion energy** | More movement pulls the eye bottom-up. The busier half wins for every child |
| **Colour saturation** | Saturated colour is more salient. Same failure mode |
| **Spatial frequency** | Fine detail versus coarse changes how easily each half is resolved at 30–45 cm |
| **Number of moving elements** | Six moving things beat one, whatever the one is |
| **Onset timing** | If one half starts moving first it captures attention first |

If these are matched, a difference in looking time is attributable to the **social content**,
which is the thing being measured. If they are not matched, the difference is attributable to
salience, and the number is meaningless — while looking exactly as precise as a valid one.

This is the single most important constraint in module 2. A beautiful, engaging, unmatched
pair produces confident numbers that measure nothing.

---

## 4. Can we use CoComelon (or similar)?

Asked directly, so answered directly.

### As the measured stimulus: no

Not "suboptimal" — structurally broken, for six separate reasons:

1. **It is a single full-screen stream.** There is no second simultaneous option, so
   `socialAttentionRatio` has no denominator. The primary output is undefined, not merely
   degraded.

2. **Ceiling effect — this is the fatal one.** Content of that kind is engineered to maximise
   attention in *every* child; that is its commercial purpose. The paradigm works *because*
   typical children prefer the social side while a minority strongly prefer geometric — that
   between-child variance **is** the signal. Show something every child stares at and the
   variance collapses to zero. You would get excellent-looking 95% engagement numbers that
   discriminate nobody from anybody.

3. **Content mixes constantly.** Social and non-social alternate second by second, so no half
   of the frame can be labelled at any given timestamp.

4. **Rapid cuts and music drive the wrong attention system.** ~1–2 second cuts plus a musical
   score recruit *exogenous* (bottom-up, stimulus-driven) attention. The social preference
   being measured is *endogenous*. The first swamps the second.

5. **Audio confounds the name-call probe**, which needs a quiet window to detect a turn to
   the child's own name.

6. **Copyright.** It cannot be shipped in the APK.

### But the underlying instinct is correct

"Children actually watch it" is a real and important point, not a naive one. A child who
will not look at the screen produces `inconclusive = true`, and an inconclusive result is
worth nothing to anybody. **Appeal is a prerequisite for the measurement, not a competing
concern.**

So the resolution is not "use boring stimuli". It is:

### Where high-appeal content legitimately belongs

A pair trial is 11 seconds. Even in the preschool band that is 6 × 11 = **66 seconds of
actual measurement** inside a session of roughly two minutes. The rest of the session is not
measured, and there the only goal is keeping the child engaged and facing the phone.

| Slot | Measured? | Appeal rules |
|---|---|---|
| Warm-up, before trial 1 | No | **Free.** Use the most engaging content available |
| Attention-getter between trials | Quality gate only | Bold, loud, fun — it exists to pull fixation back |
| **Pair trial (11 s)** | **Yes** | **Matched pair, strictly.** No exceptions |
| Re-engagement after a lost trial | No | **Free** |
| Name-call window | Yes | Quiet, low-motion neutral scene |
| Gaze-follow cue | Yes | Face must be the salient thing |

This gets what you want — a child who is enjoying the session and looking at the phone —
without touching the 66 seconds that carry the result.

### And make the matched pairs genuinely appealing

Matching constrains the *relationship* between the two halves. It says nothing about how
lively either half is, as long as they are lively in the same measure. A matched pair can be:

- bright, saturated, high-contrast
- continuously moving
- children laughing and dancing on the social side
- a vivid rotating kaleidoscope on the geometric side

What it cannot be is **lopsided**. Both halves bright, or both halves calm — never one of
each.

---

## 5. Production specification

### Global

```
landscape · ≥1920 px long edge · 30 fps · H.264 MP4
no text · no logos · Indian cast and settings
paired panels matched per §3
natural contrast polarity on faces (never inverted)
```

At 40 cm on a 6″ phone, **1° of visual angle ≈ 118 px**. Any feature that must be resolvable
needs to exceed a few degrees.

### Social half

- 2–3 Indian children playing, dancing or interacting
- Faces clearly lit and visible, direct gaze to camera at intervals
- **No speech** (it would confound the name-call probe and add an uncontrolled audio channel)
- Natural domestic or courtyard setting

### Geometric half

- Dynamic geometric patterns — rotating, morphing, kaleidoscopic
- Same palette, same mean luminance, same rate of movement as its paired social half
- No recognisable objects, no faces, nothing face-like

### Attention-getter

- 3 s seamless loop, **6 visibly different variants**
- One bold centred shape, expanding–contracting or spinning
- Ends by shrinking to a point ≤1.5° (~180 px) — this is what harvests the implicit
  calibration point
- Soft chime synced to the contraction

Six variants rather than one repeated: a target that varies between appearances measurably
improves infant eye-tracking data quality, while an identical repeat stops working after a
few presentations.

---

## 6. Current state and what is outstanding

| Item | State |
|---|---|
| Probe structure, trial counts, side/condition crossing | Implemented |
| Recorder, scoring, validity gate | Implemented |
| Age-band routing (under 9mo → questionnaire) | Implemented |
| Attention-getters | Static images, 6 variants |
| **Pair stimuli** | **Static images, not matched** — this is the gap |
| Video playback in `StimulusStage` | Not implemented — currently `painterResource` |

### Priority order

1. **Match the existing stills.** Cheapest fix by a wide margin, and it makes
   `socialAttentionRatio` interpretable today. A paired-preference measure does not strictly
   require video — static is weaker but valid. Unmatched is not valid either way, so this is
   the blocking issue, not video.
2. Produce matched video pairs per `MODULE2_VIDEO_MANIFEST.md`.
3. Switch `StimulusStage` from `painterResource` to video playback.
4. Add free-content warm-up and re-engagement slots per §4.

---

## 7. Verification checklist for a delivered pair

Before a pair goes into the app:

- [ ] Both halves same duration, seamless loop
- [ ] Mean luminance within ±5% between halves
- [ ] Comparable motion energy (no half visibly busier)
- [ ] Same palette and saturation range
- [ ] Neither half starts moving before the other
- [ ] Social half: faces visible and lit, no speech
- [ ] Geometric half: no faces, no face-like arrangements, no recognisable objects
- [ ] No text, no logos anywhere
- [ ] Plays correctly at the target size without letterboxing

A pair that fails any line invalidates every trial it appears in — and does so silently,
because the app cannot detect an unmatched pair. The checklist is the only guard.

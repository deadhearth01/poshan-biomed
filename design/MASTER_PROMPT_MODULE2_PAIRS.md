# Module 2 — Split-Screen Stimulus Prompt File (Portrait)

Regeneration brief for the paired-preference stimuli, written for the **portrait** layout the
app actually uses.

Supersedes the pair entries in `MODULE2_VIDEO_MANIFEST.md`, which assumed landscape. Read
`docs/04-module2-stimulus-design.md` for why matching decides whether the measurement means
anything at all.

---

## 1. The constraint these are designed against

Portrait, 6.1″ phone. Each half of the split is genuinely small:

| | Half width | Face inside it | Eye separation |
|---|---|---|---|
| **@30cm** (infant band) | 6.5° | 5.5° | 2.2° |
| **@45cm** (preschool band) | 4.3° | 3.7° | 1.5° |

Infant face processing wants a face around **5–8°**. At 30cm with a tight crop we reach
**5.5°** — workable. At 45cm we do not, which is why the preschool stimuli lean on colour and
shape contrast rather than facial detail.

Two consequences drive every rule below:

1. **Crop brutally tight.** A "scene with children in it" puts the faces at 1–2°, which is
   below threshold. One face, filling the frame.
2. **Low spatial frequency, high contrast.** Infant contrast sensitivity peaks near
   0.5–2 cycles/degree, far below adult. Fine detail is not merely wasted — it actively
   breaks matching, because texture density differs between a photo-real face and a
   geometric pattern.

---

## 2. Global rules

```
1024 x 1024 PNG, square (each half is roughly square in portrait)
flat vector illustration, bold shapes, high contrast
NO text, NO logos, NO numerals
Indian children, Indian clothing
subject fills 85-90% of the frame
plain background, single flat colour, no texture, no pattern
no fine detail anywhere: nothing finer than 1/8 of the frame width
```

### The matching rules — non-negotiable

Each social image is paired with a specific geometric image. **The pair must match on:**

| Property | How to hit it |
|---|---|
| **Mean luminance** | Within ±5%. Check by blurring both heavily — they should be the same grey |
| **Palette** | Same 4–5 colours, same saturation range |
| **Element count** | One face ↔ one shape. Two children ↔ two shapes. Count literally |
| **Element size** | The shape occupies the same fraction of frame as the face |
| **Background** | Identical flat colour in both halves of a pair |
| **Edge density** | Comparable amount of outline/contour. A busy pattern against a simple face invalidates the trial |

If the geometric half is more eye-catching, every child looks at it and you have measured
salience, not social preference. If the social half is more eye-catching, the same in reverse.
**Matched is the only valid state.**

---

## 3. The images

### 3.1 Pair — simple social

| Drawable | Prompt |
|---|---|
| `stim_geopref_social_simple` | Head-and-shoulders portrait of one smiling Indian toddler, facing directly forward, eyes wide open and clearly defined, warm medium skin tone, simple dark hair, plain teal background with no texture, face fills 88% of the square frame, bold flat vector illustration, thick clear features, no fine detail, no text, square 1024x1024 |

**Its matched geometric partner:**

| Drawable | Prompt |
|---|---|
| `stim_geopref_geometric` | One large bold concentric-ring target, four thick rings in warm orange, cream and soft coral, centred and filling 88% of the square frame, plain teal background with no texture, identical background colour to its paired social image, flat vector illustration, thick even lines, no fine detail, no text, square 1024x1024 |

> One face ↔ one shape. Same teal background. Same warm palette. Same fill fraction.

### 3.2 Pair — complex social

Keep this condition. Reported sensitivity for the geometric-preference paradigm is
substantially higher with complex social included than for the simple condition alone.

| Drawable | Prompt |
|---|---|
| `stim_geopref_social_complex` | Two Indian toddlers close together facing forward, heads touching, both laughing with eyes open and clearly defined, one warm-light and one deeper skin tone, plain mustard background with no texture, the two faces together fill 88% of the square frame, bold flat vector illustration, thick clear features, no fine detail, no text, square 1024x1024 |

**Its matched geometric partner** — note this is a *second* geometric image, because it must
match the complex social one, not the simple one:

| Drawable | Prompt |
|---|---|
| `stim_geopref_geometric_complex` | Two large bold interlocking shapes side by side — one hexagon, one many-pointed star — in warm orange, cream and soft coral, together filling 88% of the square frame, plain mustard background with no texture, identical background colour to its paired social image, flat vector illustration, thick even lines, no fine detail, no text, square 1024x1024 |

> Two faces ↔ two shapes. Mustard background in both.

### 3.3 Gaze-following cue

For the joint-attention probe. The child must be able to see **which way the eyes are
pointing** — so the eyes have to be oversized and unambiguous, not naturalistic.

| Drawable | Prompt |
|---|---|
| `stim_gaze_cue_left` | Head-and-shoulders portrait of a friendly Indian adult facing forward, head turned slightly to their right (viewer's left) and eyes looking clearly and unmistakably to the viewer's left, eyes drawn large with strong dark irises well off-centre toward the left, plain cream background, face fills 85% of the square frame, bold flat vector illustration, no text, square 1024x1024 |
| `stim_gaze_cue_right` | Identical to the above in every respect — same person, same clothing, same cream background, same crop — but head turned slightly to their left (viewer's right) and eyes looking clearly to the viewer's right, eyes drawn large with strong dark irises well off-centre toward the right, bold flat vector illustration, no text, square 1024x1024 |

> **These two must be mirror-consistent.** Same face, same size, same background. Only the
> gaze direction differs, or the child's response measures the change of person rather than
> the change of direction.

### 3.4 Name-call neutral scene

Shown while the caregiver says the child's name. It must be **calm and boring** — anything
engaging competes with the name for attention and depresses the measured response.

| Drawable | Prompt |
|---|---|
| `stim_response_to_name` | A simple still scene of three softly coloured wooden toy blocks resting on a plain floor, muted low-contrast palette, no faces, no motion implied, nothing visually striking, plain pale background, flat vector illustration, no text, square 1024x1024 |

### 3.5 Under-3-month stimuli — regenerate only if you re-enable that band

The module now routes under-9-months to the caregiver questionnaire, so these are not on the
live path. Listed for completeness.

| Drawable | Prompt |
|---|---|
| `stim_a1_face_pattern` | Three solid black blobs arranged in a top-heavy triangle — two side by side above, one below — on a plain white square background, very high contrast, no detail whatsoever, flat vector, square 1024x1024 |
| `stim_a1_checkerboard` | A bold black and white checkerboard of large squares, eight by eight, maximum contrast, no detail, flat vector, square 1024x1024 |
| `stim_a1_bullseye_bw` | Concentric black and white rings, five thick rings, maximum contrast, centred, no detail, flat vector, square 1024x1024 |
| `stim_a2_mother_face` | Head-and-shoulders portrait of a calm smiling Indian mother facing directly forward, eyes open and clearly defined, plain warm background, face fills 88% of the square frame, bold flat vector illustration, no fine detail, no text, square 1024x1024 |

---

## 4. Verification before these go in

Per pair, not per image:

- [ ] Blur both halves heavily — same apparent grey?
- [ ] Same background colour, exactly?
- [ ] Same number of distinct elements?
- [ ] Element occupies the same fraction of frame in both?
- [ ] Same palette and saturation?
- [ ] Neither half has finer detail than the other?
- [ ] Subject fills 85–90%?
- [ ] No text, numerals or logos anywhere?
- [ ] Gaze cues: identical apart from direction?

A pair failing any line invalidates **every trial it appears in** — silently, because the app
cannot detect an unmatched pair. This checklist is the only guard there is.

---

## 5. What this does not fix

Portrait caps the social half at 5.5° at 30cm and 3.7° at 45cm. That is workable for the
infant band and marginal for preschool. Tight cropping and bold rendering buy back most of
it; they do not eliminate it.

The complementary measure is the **drawn moving target** (`PursuitTarget`), which has no such
ceiling — a high-contrast shape can be a sixth of the screen regardless of layout, and its
position is known to the millisecond, which no image can offer. Those two probes are meant to
carry each other: the pairs measure *what* the child prefers, the moving target measures *how
well they can follow*, and the target doubles as the calibration source that makes the pair
measurement more accurate.

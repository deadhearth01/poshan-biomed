# Why the grey card?

Plain explanation first. The technical version is at the bottom.

---

## The problem in one sentence

**The camera does not know what colour the light is, so it cannot tell you what colour the
skin is.**

---

## The everyday version

Think of a white shirt.

- Under a yellow bulb at night, it looks yellowish
- In the shade outdoors, it looks slightly blue
- At midday in the sun, it looks white

The shirt never changed. The **light** changed.

Your eyes correct for this automatically and you barely notice — you still call it a white
shirt. A camera does not. It records exactly what arrives at the sensor, yellow cast and all.

Now the same thing with skin. Photograph one child indoors under a bulb and again outside in
the shade, and the camera produces two genuinely different colours. Without help, the app
would calculate two different skin types for the same child and present both with equal
confidence.

## What the grey card does

The card is a flat, plain grey with no colour of its own.

So when the camera photographs it:

> **Whatever colour the card appears to be, that colour came from the light in the room.**

That is the whole trick. The card is a known answer. The camera's guess is compared against
it, and the difference is the lighting.

Then:

1. Work out what shift makes the card look properly grey again
2. Apply that **same shift** to the skin next to it

Now the skin reading means the same thing indoors as outdoors, at 8am as at 4pm.

## Why it has to be in the same photo

Not before, not after, not "a card like that one". In the **same frame, next to the skin**.

Light changes between one moment and the next — a cloud passes, someone moves, the phone
auto-adjusts. A correction from a card photographed thirty seconds earlier corrects for
lighting that no longer exists.

## Why not just use white paper?

Paper is usually **brighter than grey and often not neutral** — most printer paper has
optical brighteners that make it slightly blue on purpose, to look "whiter". Correcting
against it would push a blue tint onto the skin.

Grey also sits in the middle of the camera's range, where it measures most accurately. White
tends to blow out to pure 255 and stops carrying any information at all.

## Why we refuse to save a reading without it

The app will not store a skin type if no card was found. It says so and asks for a retake.

That is deliberate. We measured it: **the same skin patch under a warm indoor bulb reads
6.5° of ITA different before and after correction — an entire skin-type band.** A reading
without a card is not "slightly less accurate". It can be a whole category wrong, and there
is no way to tell from the number itself which it is.

A wrong number that looks precise is worse than no number.

## How to use it

1. Wipe the skin — no oil, no powder, no cream
2. Stand in the shade, or indoors near an open door (not direct sun)
3. Lay the card **flat against the inner forearm**, near the elbow
4. Fill the left box with skin, the right box with the card
5. Hold steady until **both boxes turn green**, then press the button

Keep the card clean and flat. A dirty, creased or faded card is no longer a known answer, and
it stops being useful the moment it stops being neutral grey.

---

## The technical version

Standard **von Kries chromatic adaptation**.

The card is a neutral reflector, so its recorded RGB is a direct sample of the illuminant.
Per-channel gains are computed to drive it to neutral:

```
grey  = (cardR + cardG + cardB) / 3
gainR = grey / cardR
gainG = grey / cardG
gainB = grey / cardB
```

Those gains are applied to **every pixel before segmentation**, not to the patch average
afterwards. Order matters: the Chai & Ngan skin-detection thresholds were derived under a
neutral illuminant, so under a strong tungsten cast genuine skin drifts outside them and the
whole patch is discarded as "not skin". Correcting first means the segmentation sees what it
was designed to see.

Guards:

- Card below 20 or above 245 in any channel → gains forced to 1.0 (blown or black card cannot
  be trusted to scale anything), and the reading is flagged as uncorrected
- No card in frame → `usedCard = false`, `trustworthy = false`, reading not saved

Also pinned, because they would otherwise fight the correction:

- **AE and AWB locked** after a settling delay. Left on, the phone silently re-neutralises
  every frame, so the card correction lands on top of a correction the device already made
  and the reading drifts as the operator pans
- `CONTROL_EFFECT_MODE_OFF` — scene modes and HDR tone curves rewrite luminance, which is the
  numerator of ITA
- **No contrast enhancement of any kind.** CLAHE and similar rewrite L\* per tile while
  leaving b\* untouched, corrupting the arctan ratio. This is the most common defect in
  published phone-ITA pipelines

Downstream: sRGB is linearised properly before the XYZ transform, conversion is to CIE
L\*a\*b\* under D65, and

```
ITA° = arctan((L* − 50) / b*) × 180 / π
```

a\* is computed and deliberately discarded — it carries the haemoglobin signal (perfusion,
flushing, crying), which is not melanin.

See `SkinColorimetry.kt`, `SkinFilters.kt` and `docs/06-vitamin-d-module.md`.

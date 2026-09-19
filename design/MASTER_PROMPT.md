# Poshan — Image Master Prompt File (Reve Image 2.x)

Purpose: a single source of truth for generating every artwork asset in the Poshan app
(`com.teamposhan.biomed`) with **Reve Image** (`app.reve.com`, model `latest` / Reve 2.1).

Reve is **layout-first** and **natural-language first**. It plans a layout between the
prompt and the pixels, so the more explicitly you name *placement*, *regions*,
*exact text* and *per-region colour*, the more faithful the output.

---

## 0. How to use this file

1. Copy the **Global Style Block** (§1) verbatim into every prompt.
2. Add the **asset prompt** from §4–§10.
3. Paste into Reve with the stated aspect ratio.
4. For any asset that must appear as a *series* (illustrations, icons), generate the
   **anchor asset first**, then attach it as a **style reference image** for every
   subsequent asset in the series.
5. Never rely on Reve for final UI text — use **Live Layers → Add Text** for pixel-exact
   labels. Treat all generated medical text as a visual draft.

**Rules that matter for Reve specifically**

| Rule | Why |
|---|---|
| Write full sentences, not comma keyword salad | Reve 2.x is trained on structured descriptions |
| Name placement for every element | Reve plans a layout before rendering |
| Quote exact strings in 'single quotes' | Its typography engine renders quoted text most reliably |
| State the aspect ratio in words **and** in the UI | Reve honours ratios, max 4096 |
| Positive framing only (no "no background") | There is no negative-prompt parameter — use region edit / Select to remove |
| Edit one region at a time | Multi-instruction edits degrade fidelity |
| Reuse an anchor image as a reference | Holds palette + stroke style across a set |

---

## 1. Global Style Block (paste into every prompt)

```
Style: flat vector illustration, thick rounded strokes, simple geometric shapes,
no gradients, no texture, limited palette.
Palette: deep teal #1F6B5A, mint #9BE7D2, warm amber #F5A623,
soft coral #FF8A65, sun yellow #FFD166, warm cream background #FFF6E9.
Characters: friendly, rounded, age-appropriate, gentle happy expressions,
Indian children and caregivers, simple dot eyes, no visible teeth, no realism.
Composition: generous negative space, single clear focal subject, centred,
plain cream background #FFF6E9, tidy alignment, no clutter, no text unless quoted.
```

Optional regional variants (append when needed):

- **Telugu/Hindi onboarding**: same style, `traditional Indian clothing in teal and
  amber`, mother in a simple saree, child in a kurta.
- **Health-worker scenes**: `anganwadi centre setting, simple folded-hands posture,
  clipboard, no lab equipment`.

---

## 2. Placeholder tokens

| Token | Replace with |
|---|---|
| `{{ACTION}}` | the milestone activity, e.g. `a baby lifting their head while lying on their tummy` |
| `{{AGE}}` | age label, e.g. `0–3 months` |
| `{{CATEGORY}}` | Gross Motor / Fine Motor / Communication / Social-Emotional / Nutrition |
| `{{NUMBER}}` | category number 1–5 |
| `{{SIZE}}` | `square 1:1` / `vertical 9:16` / `wide 16:9` / `portrait 2:3` |

---

## 3. Global constraints

- Aspect ratios used by the app: **1:1** (icons, illustrations), **9:16** (splash,
  onboarding), **16:9** (report header), **2:3** (posters, infographics).
- Readability distance: this app is used in **Anganwadi homes and health centres**, often
  on a low-end phone at arm's length, sometimes in bright sunlight. So: **big shapes,
  high contrast, no fine detail, no thin lines**.
- Cultural context: India, community health, ASHA workers, Anganwadi centres.
- Nothing clinical, frightening, or that implies diagnosis.

---

## 4. App identity

### 4.1 App icon — `{{SIZE}}=square 1:1`
```
A mobile app icon for a children's health app called 'Poshan', square 1:1.
Centred: a stylised flat-vector mother holding a chubby happy baby, both simplified
into rounded geometric shapes, teal and amber, on a plain deep-teal #1F6B5A background
that fills the whole square.
The mother's silhouette and the baby's body together form a soft upward curve like a
growing sprout.
Thick rounded strokes, no gradients, no text, no outline border, generous padding around
the subjects so the icon reads clearly at small size.
```

### 4.2 Splash / onboarding hero — `{{SIZE}}=vertical 9:16`
```
A vertical 9:16 mobile app splash illustration for the children's health app 'Poshan'.
Top third: the app name 'Poshan' centred in a bold friendly rounded sans-serif, deep teal.
Middle: a flat-vector scene of an Indian mother sitting cross-legged with her toddler on
her lap, an ASHA health worker kneeling beside them with a simple clipboard, all three
smiling gently, placed centre-frame.
Bottom third: a single wide rounded pill button shape (empty, no label) centred, mint
green, with generous margin around it.
Cream background #FFF6E9, thick rounded strokes, limited palette of teal, mint, amber
and coral, plenty of empty space, no clutter, no extra text.
```

---

## 5. Onboarding illustrations (one per step)

Each: `{{SIZE}}=square 1:1`, centred subject, no text.

1. **Language**
```
Flat vector illustration of a smiling Indian mother and her toddler sitting together,
the mother holding a simple phone showing two speech bubbles, one with a Devanagari
character and one with a Telugu character, both as shapes only.
Both characters rounded and friendly, teal and amber clothing, plain cream background
#FFF6E9, thick rounded strokes, no gradients, no readable text, generous negative space.
```

2. **Offline / community use**
```
Flat vector illustration of an ASHA community health worker with a shoulder bag standing
in front of a simple Anganwadi building with a small veranda, holding a phone. A mother
and child stand beside her. A small crossed-out cloud icon floats in the top right corner
made of simple rounded shapes. Cream background #FFF6E9, teal/mint/amber palette,
thick rounded strokes, no gradient, no text.
```

3. **Screening, not diagnosis**
```
Flat vector illustration of a friendly pediatrician with a stethoscope kneeling at eye
level with a toddler, both smiling, with a simple rounded shield containing a checkmark
floating between them. Reassuring, calm, gentle. Cream background #FFF6E9, teal and mint
palette with one amber accent, thick rounded strokes, no gradients, no text.
```

---

## 6. Category illustrations (5 assets, used on category cards + reports)

Series: **generate 6.1 as the anchor, then reuse it as a style reference for 6.2–6.5.**
Each: `{{SIZE}}=square 1:1`.

6.1 **Gross Motor**
```
Flat vector illustration of a happy Indian toddler mid-jump with both feet off the
ground, arms raised, and a baby below them lying on their tummy lifting their head.
Simple rounded bodies, tiny dot eyes, big smiles.
Cream background #FFF6E9, teal and amber clothing, thick rounded strokes, no gradients,
centred composition, no text.
```

6.2 **Fine Motor**
```
Flat vector illustration of a toddler sitting at a low table stacking three rounded
colourful blocks into a tower, holding one block in a pincer grip, tongue slightly out in
concentration, smiling.
Simple block shapes in teal, amber and coral, cream background #FFF6E9, thick rounded
strokes, no gradients, centred, no text.
```

6.3 **Communication**
```
Flat vector illustration of a mother and toddler facing each other talking, two large
rounded speech bubbles between them filled with simple abstract sound shapes (a wave, a
dot, a curve) — no readable letters.
Warm and engaged expressions, cream background #FFF6E9, teal and coral palette, thick
rounded strokes, no gradients, centred, no text.
```

6.4 **Social / Emotional**
```
Flat vector illustration of two toddlers sitting opposite each other playing a clapping
game, hands raised together, one laughing, a small heart shape floating above them made
of simple rounded geometry.
Cream background #FFF6E9, teal, coral and sun-yellow palette, thick rounded strokes, no
gradients, centred, no text.
```

6.5 **Nutrition**
```
Flat vector illustration of a toddler at a low table with a round plate divided into
four coloured sections, a small bowl of dal, a glass of milk, and a banana, all rendered
as simple rounded geometric shapes.
Cream background #FFF6E9, teal, amber, coral and leaf-green palette, thick rounded
strokes, no gradients, centred, no text.
```

---

## 7. Milestone question illustrations (the in-question card art)

Template — one asset per milestone question. Replace `{{ACTION}}`:

```
Flat vector illustration for a child development question card:
{{ACTION}}.
One Indian baby or toddler, simple rounded body, dot eyes, gentle happy expression,
centred and large in the frame so it reads on a small phone screen.
Cream background #FFF6E9, teal and amber clothing, thick rounded strokes, no gradients,
no text, generous negative space around the child.
```

Worked examples (the first 12 questions of each band):

| Age band | `{{ACTION}}` |
|---|---|
| 0–3 mo | `a baby lying on their tummy lifting their head momentarily` |
| 0–3 mo | `a baby lying on their back moving both arms and legs actively` |
| 0–3 mo | `a baby bringing their hands toward their mouth` |
| 0–3 mo | `a baby looking up at their mother's face` |
| 0–3 mo | `a baby smiling responsively at a caregiver` |
| 4–6 mo | `a baby holding their head steadily while upright in a caregiver's hands` |
| 4–6 mo | `a baby pushing up on their arms while lying on their tummy` |
| 4–6 mo | `a baby rolling from tummy to back on a mat` |
| 4–6 mo | `a baby grasping a small soft toy` |
| 7–9 mo | `a baby sitting upright without support` |
| 7–9 mo | `a baby passing a toy from one hand to the other` |
| 7–9 mo | `a baby playing peek-a-boo with a caregiver` |
| 10–12 mo | `a baby pulling up to stand holding onto a low stool` |
| 10–12 mo | `a baby picking up a small object with thumb and finger` |
| 13–18 mo | `a toddler walking unsupported across a room` |
| 13–18 mo | `a toddler stacking two blocks one on top of the other` |
| 19–24 mo | `a toddler kicking a soft ball` |
| 19–24 mo | `a toddler turning pages of a board book` |
| 25–36 mo | `a toddler jumping with both feet off the floor` |
| 25–36 mo | `a toddler scribbling with a thick crayon on paper` |
| 37–48 mo | `a preschooler drawing a circle with a crayon` |
| 37–48 mo | `a preschooler standing on one foot briefly` |
| 49–60 mo | `a preschooler hopping on one foot` |
| 49–60 mo | `a preschooler cutting paper with round safety scissors` |

> Generate these in **batches of the same age band** using one anchor as the reference
> image, so stroke weight and palette stay identical across the card set.

---

## 8. Empty states and utility art

### 8.1 No children yet — `{{SIZE}}=square 1:1`
```
Flat vector illustration of an empty simple cradle with a soft folded blanket inside and
a single small star floating above it.
Cream background #FFF6E9, mint and amber palette, thick rounded strokes, no gradients,
centred, no text.
```

### 8.2 Assessment complete — `{{SIZE}}=square 1:1`
```
Flat vector illustration of a happy Indian toddler raising both arms in celebration, with
three simple rounded confetti pieces and a small teal ribbon badge floating around them.
Cream background #FFF6E9, teal, amber, coral palette, thick rounded strokes, no
gradients, centred, no text.
```

### 8.3 Offline — `{{SIZE}}=square 1:1`
```
Flat vector illustration of a simple rounded phone with a small crossed-out cloud shape
beside it made of basic geometry.
Cream background #FFF6E9, teal and grey-mint palette, thick rounded strokes, no
gradients, centred, no text.
```

### 8.4 Camera / attention module — `{{SIZE}}=square 1:1`
```
Flat vector illustration of a toddler's face in three-quarter view looking at a large
rounded phone screen, with a wide open cartoon eye and a short dotted line showing the
line of sight from the eye to the screen.
Cream background #FFF6E9, teal and amber palette, thick rounded strokes, no gradients,
no text.
```

---

## 9. 3D icon set (optional, for report + dashboard accents)

Keep material, camera angle and lighting wording **identical** across the set.
Pattern:

```
A 3D icon of {{SUBJECT}}, centred, single subject, clean warm-cream background,
soft even studio lighting, subtle contact shadow beneath it, three-quarter view,
glossy matte clay material, teal and mint with one amber accent, minimal and rounded.
Square 1:1.
```

Subjects: `a weighing scale`, `a growth chart ribbon`, `a syringe-free vaccine shield`,
`an eye with a soft glow`, `a sun with a small droplet`, `a heart with a checkmark`,
`a bowl of food`, `a magnifying glass over a child's footprint`.

---

## 10. Report / infographic assets

### 10.1 Growth chart header — `{{SIZE}}=wide 16:9`
```
A wide 16:9 header banner for a child growth report.
Left: a flat-vector Indian toddler standing next to a simple vertical measuring ruler with
rounded tick marks, seen from behind.
Right: three simple rounded stat-card shapes stacked vertically, empty, teal outlines on
cream.
Cream background #FFF6E9, teal/mint/amber palette, thick rounded strokes, no gradients,
no readable text.
```

### 10.2 "Five screening categories" infographic — `{{SIZE}}=portrait 2:3`
```
A vertical 2:3 health infographic titled 'Five Screening Categories', flat vector style,
cream background #FFF6E9.
Title 'Five Screening Categories' at the top centre in bold rounded sans-serif, deep teal.
Below: five numbered rounded panels stacked top to bottom, each with a small flat-vector
icon on the left and a short quoted label on the right:
'1 Gross Motor', '2 Fine Motor', '3 Communication', '4 Social and Emotional',
'5 Nutrition'.
A teal downward arrow between each panel.
Muted teal / mint / amber palette, rounded corners, generous spacing, no clutter.
```

### 10.3 Referral flow infographic — `{{SIZE}}=portrait 2:3`
```
A vertical 2:3 infographic showing a three-step referral flow, flat vector, cream
background #FFF6E9.
Three rounded panels stacked top to bottom, each with a simple flat icon and one short
quoted label: '1 Screening', '2 Review', '3 Referral'.
A teal downward arrow between panels. A small rounded note card at the bottom with the
quoted text 'Screening, not diagnosis'.
Teal, mint and amber palette, rounded corners, generous negative space.
```

---

## 11. Reproducing the set consistently (workflow)

1. Generate **4.1 app icon** and **6.1 Gross Motor** first — these are your anchors.
2. Save both. In Reve, attach the relevant anchor as a **reference image** and state:
   `Match the reference image exactly for palette, stroke weight, character style and
   background. Change only the subject as described.`
3. Generate each series in one session so the seed and style stay close; log the seed.
4. For any asset where a label must be pixel-exact (app icon wordmark, infographic
   titles), render the artwork **without** text and add the text with
   **Live Layers → Add Text**.
5. To fix a single element, use **Select / Spotlight annotation** and edit that region
   only — do not rewrite the whole prompt.
6. Keep a `design/reve-seeds.md` log: asset name → prompt version → seed → date.

---

## 12. Known Reve limitations and workarounds

| Limitation | Workaround |
|---|---|
| Ignores part of a compositional instruction | Restate the missed element as its own sentence/region; use Spotlight annotation |
| Small in-scene text is illegible or misspelled | Use Live Layers → Add Text |
| Multi-instruction edits misplace or duplicate elements | Edit one region per request |
| Proportion/scale of people drifts | Use one character per asset; state relative scale |
| No negative prompts | Positive framing + region removal |
| Medical/factual content can hallucinate | Treat as a visual draft; verify before publishing |
| Free tier may publish to a public gallery | Use a paid plan for anything client-facing |

---

## 13. Asset checklist (what the app needs)

| # | Asset | Size | Section |
|---|---|---|---|
| 1 | App icon | 1:1 | 4.1 |
| 2 | Splash / onboarding hero | 9:16 | 4.2 |
| 3 | Onboarding ×3 | 1:1 | 5 |
| 4 | Category illustrations ×5 | 1:1 | 6 |
| 5 | Milestone illustrations (~30 used, scalable to the full bank) | 1:1 | 7 |
| 6 | Empty states ×4 | 1:1 | 8 |
| 7 | 3D icon set ×8 | 1:1 | 9 |
| 8 | Report headers/infographics ×3 | 16:9, 2:3 | 10 |

Place generated files in:

```
android/app/src/main/res/drawable-nodpi/     # 1:1 and raster illustrations
android/app/src/main/res/drawable/           # vectorised simple shapes
```
Use lowercase, underscore-separated names (`illus_cat_gross_motor.png`).

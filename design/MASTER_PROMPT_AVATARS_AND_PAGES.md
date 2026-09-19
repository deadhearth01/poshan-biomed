# Poshan — Avatars, Measurement How-To & Page Artwork Prompt File

Second companion to `MASTER_PROMPT.md`, alongside `MASTER_PROMPT_GUIDES.md`. Covers what the
walkthrough file does not:

1. **Child avatars** (28 images) — a stand-in picture for every child, by sex and age.
2. **Measurement how-to** (4 images) — weight, length, height, arm tape.
3. **Page artwork** (8 images) — vaccines, growth charts, report, child profile.

House style, delivery rules and the "no text of any kind" rule are inherited from
`MASTER_PROMPT_GUIDES.md` section 0. Read that first; only the differences are repeated here.

---

## 1. Child avatars — 28 images

### Why these exist

The app never photographs a child, so every list and header showed the same generic icon and
one child's page looked exactly like another's. These are illustrated stand-ins.

The app picks one automatically from the child's recorded sex and age, and keeps the same one
for that child forever — chosen from a hash of the child's id, so it never changes between
screens, sessions or devices.

### Naming — this must be exact

```
avatar_<sex>_<stage>_<variant>.png

sex     : boy | girl | child       (`child` = sex not recorded)
stage   : baby | infant | toddler | child
variant : 1 | 2 | 3
```

So: `avatar_girl_toddler_2.png`, `avatar_boy_baby_1.png`, `avatar_child_infant_3.png`.

That is 3 sexes × 4 stages × 3 variants = **36 slots**, but the `child` (unrecorded) set can
be generated last — **28 images** covers boy and girl fully (24) plus one neutral variant per
stage (4). Missing files are safe: the app falls back to the child's initial in a coloured circle.

### Age stages

| Stage | Age | What it must look like |
|---|---|---|
| `baby` | under 6 months | Newborn to small infant. Round face, little or no hair, often swaddled or in a simple cotton vest. Not sitting up |
| `infant` | 6–17 months | Sitting or just standing. More hair, alert expression, maybe one or two teeth |
| `toddler` | 18–35 months | Standing confidently, fuller face, simple everyday clothes |
| `child` | 3 years and up | Clearly a young child, not a baby. Taller proportions, expressive face |

### Rules specific to avatars

| Rule | Why |
|---|---|
| **Head and shoulders only, centred, facing forward** | Displayed in a circle as small as 48 px. Anything wider disappears |
| **The three variants of each set must differ clearly** | Different skin tone, hair, and clothing colour. If two variants look alike the whole feature is pointless |
| **Vary skin tone deliberately across the set** | Light, medium and deep across the three variants of every stage. Not three shades of the same |
| **Plain soft background, single flat colour** | It sits inside a circle. A detailed background becomes visual noise at small size |
| **Happy or neutral, never sad, never crying** | This picture represents somebody's child on their own page |
| **No medical props** | No stethoscope, no thermometer, no clinic. This is the child, not the appointment |
| **Indian children** | Clothing, features and hair should be recognisably Indian |

### Prompts

Use this template and substitute:

```
Head-and-shoulders portrait of a happy Indian <STAGE DESCRIPTION>, <SKIN TONE> skin,
<HAIR>, wearing <CLOTHING>, facing forward, centred, plain soft <BACKGROUND COLOUR>
background, flat vector children's-book illustration, no text, square, 512x512
```

| Drawable | Substitutions |
|---|---|
| `avatar_boy_baby_1` | newborn baby boy about 3 months / light warm / almost no hair / plain white cotton vest / soft mint |
| `avatar_boy_baby_2` | baby boy about 4 months / medium brown / short dark tuft of hair / pale yellow cotton vest / soft peach |
| `avatar_boy_baby_3` | baby boy about 5 months / deep brown / soft black curls / light blue cotton vest / soft lavender |
| `avatar_boy_infant_1` | baby boy about 10 months, sitting up and alert / light warm / short dark hair / small green kurta / soft mint |
| `avatar_boy_infant_2` | baby boy about 12 months / medium brown / thicker dark hair / orange t-shirt / soft peach |
| `avatar_boy_infant_3` | baby boy about 15 months / deep brown / short curly hair / yellow t-shirt / soft lavender |
| `avatar_boy_toddler_1` | toddler boy about 2 years / light warm / neat dark hair / blue checked shirt / soft mint |
| `avatar_boy_toddler_2` | toddler boy about 2 and a half / medium brown / slightly messy hair / red t-shirt / soft peach |
| `avatar_boy_toddler_3` | toddler boy about 3 years / deep brown / short curls / green kurta / soft lavender |
| `avatar_boy_child_1` | boy about 4 years, bright smile / light warm / neat side-parted hair / school-style blue shirt / soft mint |
| `avatar_boy_child_2` | boy about 4 and a half / medium brown / short dark hair / mustard shirt / soft peach |
| `avatar_boy_child_3` | boy about 5 years / deep brown / short curls / white shirt / soft lavender |
| `avatar_girl_baby_1` | newborn baby girl about 3 months / light warm / almost no hair / plain white cotton vest / soft rose |
| `avatar_girl_baby_2` | baby girl about 4 months / medium brown / fine dark hair with a tiny clip / pale green vest / soft mint |
| `avatar_girl_baby_3` | baby girl about 5 months / deep brown / soft black curls / pale yellow vest / soft lavender |
| `avatar_girl_infant_1` | baby girl about 10 months, sitting up / light warm / short hair with one small clip / pink frock / soft rose |
| `avatar_girl_infant_2` | baby girl about 12 months / medium brown / two tiny tufts tied up / teal frock / soft mint |
| `avatar_girl_infant_3` | baby girl about 15 months / deep brown / short curls / orange frock / soft lavender |
| `avatar_girl_toddler_1` | toddler girl about 2 years / light warm / two small plaits with ribbons / yellow frock / soft rose |
| `avatar_girl_toddler_2` | toddler girl about 2 and a half / medium brown / short bob with a clip / purple frock / soft mint |
| `avatar_girl_toddler_3` | toddler girl about 3 years / deep brown / curly hair tied back / green frock / soft lavender |
| `avatar_girl_child_1` | girl about 4 years, bright smile / light warm / two neat plaits / blue school pinafore / soft rose |
| `avatar_girl_child_2` | girl about 4 and a half / medium brown / ponytail with a ribbon / pink kurta / soft mint |
| `avatar_girl_child_3` | girl about 5 years / deep brown / curly hair with a hairband / white shirt / soft lavender |
| `avatar_child_baby_1` | baby about 4 months, sex not shown / medium brown / fine dark hair / plain cream vest / soft grey-green |
| `avatar_child_infant_1` | baby about 12 months, sex not shown / medium brown / short dark hair / plain cream kurta / soft grey-green |
| `avatar_child_toddler_1` | toddler about 2 and a half, sex not shown / medium brown / short dark hair / plain cream shirt / soft grey-green |
| `avatar_child_child_1` | child about 4 years, sex not shown / medium brown / short dark hair / plain cream shirt / soft grey-green |

The neutral set needs **one variant per stage only** — the app never asks for `_2` or `_3`
of it, because a child whose sex was not recorded is rare enough that spreading them across
three pictures buys nothing.

---

## 2. Measurement how-to — 4 images

These replace simple line drawings that are already in the app, so anything delivered here is
an upgrade rather than a dependency. Shown small, about 88 px square, beside three numbered
lines of text.

**These must be technically correct.** A picture showing the wrong technique will be copied.

| Drawable | What it must show | Prompt |
|---|---|---|
| `guide_measure_weight` | A child on a scale, wearing one light layer | Small Indian child in a single light cotton vest standing on a simple flat digital floor scale, shoes and sweater set aside on the floor nearby, adult crouching beside without touching the child, no numbers visible on the display, flat vector illustration, square |
| `guide_measure_length` | Under two years, measured lying down | Indian baby lying flat on their back on a wooden length board, one adult holding the head gently against the fixed top end while a second adult presses the knees flat and brings the sliding footboard to the heels, side view, no numbers on the board, flat vector illustration, square |
| `guide_measure_height` | Two years and over, measured standing | Indian child about four years old standing barefoot with heels together against a wall-mounted height board, back and head touching the board, looking straight ahead, an adult lowering the flat headpiece onto the crown, no numbers on the scale, flat vector illustration, square |
| `guide_measure_muac` | The arm tape, at the correct point | Close view of an adult wrapping a flat measuring tape around the middle of a small child's left upper arm, halfway between shoulder and elbow, the arm hanging relaxed at the child's side, tape flat and snug against the skin, no markings or colours readable on the tape, flat vector illustration, square |

Common errors to avoid, because each produces a wrong measurement:
- Length board shown **without** someone holding the head — the head tips and the child reads short.
- Arm tape shown on an arm held **up** or bent — the muscle changes shape and the number changes.
- Standing height shown with the child **looking up** — adds a centimetre or more.
- Weight shown with the child **held** by an adult — weighs the adult's arm too.

---

## 3. Page artwork — 8 images

Wide 4:3 headers and section pictures for the pages that currently have none.

| Drawable | Page | Prompt |
|---|---|---|
| `page_vaccines_hero` | Vaccine schedule | Warm scene of an Indian health worker placing a sticker on a smiling toddler's hand after a vaccination, mother holding the child calmly, a simple immunisation card in her other hand with no readable text, reassuring not clinical, flat vector illustration |
| `page_vaccines_due` | A vaccine is due | A soft rounded calendar shape with a gentle bell beside it and a small syringe resting flat, warm amber, friendly and not urgent, no numerals on the calendar, flat vector illustration |
| `page_vaccines_late` | A vaccine is overdue | The same soft calendar and bell, now in warm coral with a gentle upward-pointing arrow beside it, concerned but not alarming, no numerals, flat vector illustration |
| `page_growth_hero` | Growth charts | Warm scene of an Indian child standing beside a doorframe with pencil height marks rising up it, a parent smiling beside them, a soft rising curve echoed in the background, no numbers or letters on the marks, flat vector illustration |
| `page_report_hero` | Report | Warm scene of an Indian health worker and a mother looking together at a phone, both calm and engaged, the child playing on the floor beside them, flat vector illustration |
| `page_child_hero` | Child profile | Warm scene of an Indian child of about two standing in a sunlit courtyard with arms open, growth-ring shapes echoing softly behind them, generous negative space, flat vector illustration |
| `page_child_timeline` | The road from birth to five | A soft winding path across the frame with five gentle milestone dots along it, a small child at the near end and a taller child at the far end, no text or numbers on the markers, flat vector illustration |
| `page_report_shared` | Report sent | A rounded document shape with a soft tick, and a gentle arc showing it moving toward a simple clinic building, calm and finished, flat vector illustration |

---

## 4. Cross-check before sending

The app resolves every one of these by **exact file name**. A typo means a silent fallback,
not an error, so nothing will tell you it is wrong — it will just never appear.

- [ ] All lowercase, underscores only, `.png` extension.
- [ ] Avatars **512 × 512**; measurement how-tos **512 × 512**; page artwork **1024 × 768**.
- [ ] No letters, words or numerals anywhere in any image, including on scales, tapes, charts and calendars.
- [ ] Skin tones genuinely varied across each avatar set.
- [ ] Measurement images checked against the four common errors listed in section 2.
- [ ] Dropped into `android/app/src/main/res/drawable-nodpi/`.

Priority if generating in stages:

1. **Measurement how-tos** (4) — they change whether a number is right.
2. **Avatars, boy and girl** (24) — the biggest visible change per image.
3. Page artwork (8).
4. Neutral `avatar_child_*` variants (4).

---

## 5. Dynamic home images — 8 images

The home screen header currently never changes. These let it respond to where the family
actually is in the visit, so opening the app says something rather than nothing.

The app picks one automatically from the state of the current visit. Same 1024 × 768 size and
the same rules as the page artwork.

| Drawable | Shown when | Prompt |
|---|---|---|
| `home_state_nothing_done` | No check started yet today | Warm wide scene of an Indian mother and small child sitting together on a floor mat in morning light, unhurried, a phone resting face-down beside them, a sense of about to begin, generous negative space, flat vector illustration |
| `home_state_in_progress` | One or two modules done | Warm wide scene of an Indian health worker and a mother mid-conversation over a phone, the child playing contentedly nearby, a sense of work underway, generous negative space, flat vector illustration |
| `home_state_all_done` | All three modules complete | Warm wide scene of an Indian mother lifting her laughing toddler up in bright sunlight, relief and warmth, a soft tick shape echoed faintly in the background, generous negative space, flat vector illustration |
| `home_state_review_needed` | Something needs a doctor | Warm wide scene of an Indian mother and child walking together toward a simple health centre doorway in soft daylight, calm and purposeful, not frightened, generous negative space, flat vector illustration |
| `home_state_due_soon` | Next check is due | Warm wide scene of an Indian child sitting by a window watching soft rain or sunlight outside, a gentle calendar shape floating faintly in the background with no numerals, patient waiting, generous negative space, flat vector illustration |
| `home_state_vaccine_late` | A vaccine is overdue | Warm wide scene of an Indian health worker at a doorway holding a small immunisation card, greeting a mother with a baby, welcoming rather than reproachful, no readable text on the card, generous negative space, flat vector illustration |
| `home_state_offline` | Not synced for a while | Warm wide scene of an Indian village courtyard at dusk with a phone glowing softly on a table and a faint cloud shape far above, calm and unworried, generous negative space, flat vector illustration |
| `home_state_new_child` | No child added yet | Warm wide scene of an empty sunlit courtyard with a small folded blanket and a wooden toy waiting on a mat, inviting rather than lonely, generous negative space, flat vector illustration |

Rules specific to these:

| Rule | Why |
|---|---|
| **Never show distress, illness or medical equipment** | This is the first thing a parent sees when they open the app, every time |
| **`home_state_review_needed` must not look like bad news** | It means "worth a closer look". A frightening picture turns a routine referral into a crisis |
| **`home_state_vaccine_late` must not look accusing** | The caregiver is already behind; shaming them is how they stop opening the app |
| **All eight must feel like one set** | Same palette, same light, same level of detail. They swap in the same slot, so a mismatch reads as a glitch |

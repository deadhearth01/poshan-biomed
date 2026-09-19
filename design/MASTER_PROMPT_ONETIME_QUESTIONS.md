# Poshan — One-Time Question Artwork Prompt File

The question sets that are asked once per child and currently have **no illustration at all**:

1. **Nutrition background** (19) — household and history, asked at enrolment
2. **Vitamin D sign check** (6) — the physical look
3. **Vitamin D background** (8) — sunlight, drops, diet

House style, delivery rules and the no-text rule are inherited from
`MASTER_PROMPT_GUIDES.md` section 0. Only the differences are repeated here.

---

## 0. The rule that matters most here

These 19 background questions are **the most sensitive content in the app**. They ask about
poverty, toilets, cooking fuel, a mother's education, a mother's weight. A careless
illustration turns a screening question into a judgement, and a caregiver who feels judged
stops answering honestly — which destroys the data as surely as a wrong threshold would.

| Rule | Why |
|---|---|
| **Never depict a distressed, dirty, or visibly poor-looking family** | The question asks about circumstances, not worth. Show the *thing* being asked about, not a pitiable person |
| **Prefer the object over the person** | "No toilet" is a simple latrine structure, not a squatting figure. "Unclean fuel" is a chulha with a wisp of smoke, not a coughing woman |
| **Everyone depicted is calm, clean and dignified** | Always. No exceptions |
| **No thin, sad, or sick-looking children anywhere** | Not in this set, not ever |
| **Neutral, never cautionary** | These are not warnings. A red cross or a frowning face makes the question accusatory |

Where a question is genuinely hard to draw kindly, **draw the neutral object** and let the
question text carry the meaning.

### Delivery

```
768 x 768 PNG, square
flat vector illustration, warm muted palette, bold simple shapes
no text, no numerals, no logos
Indian homes, clothing, utensils, settings
plain background, low contrast, no texture
```

Shown at roughly 64–96 px beside the question, so: one clear subject, no fine detail.

Drop into `android/app/src/main/res/drawable-nodpi/`.

---

## 1. Nutrition background — 19 images

### Birth and early history

| Drawable | Question | Prompt |
|---|---|---|
| `nut_bg_1` | 2nd–4th born? | Three small Indian children of stepped heights standing together in a row, calm and neatly dressed, plain warm background, flat vector, square |
| `nut_bg_2` | 5th born or later? | A row of five or six small Indian children of stepped heights, calm and evenly spaced, plain warm background, flat vector, square |
| `nut_bg_3` | Small at birth? | A newborn wrapped in a soft cotton cloth resting in two cupped adult hands, gentle and protective, no scale or measurement shown, flat vector, square |
| `nut_bg_4` | Breastfeeding not started immediately? | A simple clock face with no numerals beside a soft mother-and-newborn silhouette, quiet and neutral, flat vector, square |
| `nut_bg_5` | Postnatal checkup done? | An Indian health worker holding a simple clipboard, smiling gently at a mother holding her newborn, no readable text on the clipboard, flat vector, square |

### Feeding history

| Drawable | Question | Prompt |
|---|---|---|
| `nut_bg_6` | Currently breastfed? | Indian mother seated on a floor mat breastfeeding her baby, calm and warm, soft daylight, flat vector, square |
| `nut_bg_7` | Receives formula milk? | A feeding bottle and a plain tin standing on a clean table beside a steel tumbler, no baby in frame, entirely neutral presentation, flat vector, square |
| `nut_bg_8` | Receives fresh animal milk? | A steel tumbler of milk beside a small brass pot, a calm cow standing softly in the far background, flat vector, square |

### Mother's health

> Handle with the most care of any group. Each asks about the mother. None should make her
> look sick, sad or inadequate.

| Drawable | Question | Prompt |
|---|---|---|
| `nut_bg_9` | Mother younger than 20? | A young Indian woman holding a baby, calm and composed, plain warm background, she looks young but capable and unworried, flat vector, square |
| `nut_bg_10` | Mother underweight? | A simple flat weighing scale standing alone on a floor, no person and no reading visible, entirely neutral object, flat vector, square |
| `nut_bg_11` | Mother has anaemia? | A soft open palm shown alone with a gentle drop shape beside it, quiet and clinical-free, no blood, no needle, no face, flat vector, square |
| `nut_bg_12` | Mother had no formal education? | A closed simple book and a folded pair of reading glasses resting on a table, object only, no person, flat vector, square |
| `nut_bg_13` | Delivered by caesarean? | A simple hospital bed shape with a soft folded blanket, calm and empty, no surgical imagery of any kind, flat vector, square |

### Household and surroundings

| Drawable | Question | Prompt |
|---|---|---|
| `nut_bg_14` | Poorer wealth category? | A simple modest Indian village house with a tiled roof and an open doorway, tidy and well-kept, warm daylight, dignified not shabby, flat vector, square |
| `nut_bg_15` | No toilet? | A simple standalone latrine structure with a closed door, drawn plainly as a building, no person, flat vector, square |
| `nut_bg_16` | Unimproved water source? | A hand pump and a filled steel pot standing beside it, plain and neutral, no person, flat vector, square |
| `nut_bg_17` | Father had no formal education? | A closed simple book resting beside a folded cotton shirt, object only, no person, flat vector, square |
| `nut_bg_18` | Cooks with unclean fuel? | A traditional clay chulha with a small stack of firewood beside it and a thin wisp of smoke, shown plainly as a cooking arrangement, no coughing, no person, flat vector, square |
| `nut_bg_19` | Lives in a rural area? | A wide calm view of an Indian village — a few tiled roofs, a tree, fields behind — peaceful and pleasant, flat vector, square |

---

## 2. Vitamin D sign check — 6 images

These must be **anatomically clear**, because the caregiver is being asked to look for the
thing in the picture. But none should look alarming — most children have none of these.

| Drawable | Question | Prompt |
|---|---|---|
| `vd_sign_wrist` | Wrists or ankles swollen? | Close view of a small child's forearm and wrist held gently by an adult hand, the wrist area drawn slightly fuller than the arm above it, calm and clinical-free, flat vector, square |
| `vd_sign_rosary` | Bumps in a row down the chest? | Front view of a small child's bare chest, a soft row of small evenly spaced bumps visible along each side of the breastbone, child calm and content, flat vector, square |
| `vd_sign_legs` | Legs curve in or out? | Front view of a standing toddler with feet together, legs drawn with a gentle outward curve leaving a visible gap at the knees, child standing happily, flat vector, square |
| `vd_sign_walk` | Cannot stand or walk? | A toddler seated on a floor mat watching another child of the same size walking nearby, neutral and unworried, flat vector, square |
| `vd_sign_pain` | Leg pain or tires easily? | A small child sitting down on a step resting a hand on their own shin, other children playing softly in the background, calm not distressed, flat vector, square |
| `vd_sign_seizure` | Fits or spasms? | A soft open adult hand supporting a resting child's head, protective and calm, **no depiction of a seizure**, flat vector, square |

> `vd_sign_seizure` deliberately does not illustrate the event. A drawing of a convulsing
> child is frightening, unhelpful for recognition, and would be the single most upsetting
> image in the app.

---

## 3. Vitamin D background — 8 images

| Drawable | Question | Prompt |
|---|---|---|
| `vd_bg_bf` | Breastfed without drops? | Indian mother breastfeeding her baby with a small dropper bottle resting unopened on a shelf nearby, calm and non-judgemental, flat vector, square |
| `vd_bg_sun` | Mostly indoors? | A child playing contentedly on a floor mat indoors, warm sunlight visible through a doorway behind them, neutral not sad, flat vector, square |
| `vd_bg_cover` | Skin covered outdoors? | A toddler outdoors in long sleeves and full-length trousers with a cap, standing happily in sunshine, flat vector, square |
| `vd_bg_air` | Smoke, dust or haze? | A view of rooftops under a soft hazy sky with the sun showing dimly through, no people, flat vector, square |
| `vd_bg_preterm` | Born early? | A very small newborn resting in a simple cot with a soft blanket, calm and peaceful, no medical equipment, flat vector, square |
| `vd_bg_mother` | Mother low or rarely outdoors? | An Indian woman standing just inside a doorway looking out at sunlight, calm and composed, flat vector, square |
| `vd_bg_calcium` | Rarely eats calcium foods? | A steel plate holding curd, a bowl of ragi porridge and a small heap of green leafy vegetables, appetising and plain, flat vector, square |
| `vd_bg_diet` | Rarely eats eggs, fish, fortified foods? | A steel plate holding a boiled egg cut in half, a small fish, and a glass of milk, appetising and plain, flat vector, square |

---

## 4. Check before sending

- [ ] 768 × 768 PNG, exact drawable names, lowercase with underscores
- [ ] **No image makes a family look pitiable, dirty, or judged**
- [ ] No thin, sad, or unwell-looking children anywhere
- [ ] No text, numerals or logos — including on clocks, scales and clipboards
- [ ] Sensitive questions use the neutral object, not a person
- [ ] `vd_sign_seizure` shows a supporting hand, never a seizure
- [ ] Sign images are anatomically clear enough to actually recognise the sign

Priority if generating in stages:

1. **Vitamin D signs** (6) — the caregiver is being asked to *recognise* something, so the picture does real work
2. **Vitamin D background** (8)
3. **Nutrition background** (19) — largest set, and the one to take most care over

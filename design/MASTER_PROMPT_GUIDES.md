# Poshan — Guided Walkthrough & Micro-Element Artwork Prompt File

Companion to `MASTER_PROMPT.md`. Covers three things the app now needs:

1. **Module walkthrough steps** (25 images) — one per instruction screen, all three modules.
2. **Module hero images** (3 images) — the top of each module intro.
3. **Micro-elements** (18 images) — small reusable pictures used across the app for status,
   empty states and result feedback.

Everything here is **instructional**. Unlike the milestone and nutrition artwork, which
illustrates a question, these pictures tell somebody what to physically do. That changes the
rules, so read section 0 before generating anything.

---

## 0. House style

Inherited from `MASTER_PROMPT.md`:

```
Flat vector children's-book illustration, soft rounded shapes, warm and friendly,
generous negative space, no outlines heavier than the shapes themselves,
muted natural palette with one warm accent, no text of any kind,
no logos, no brand marks, sRGB.
```

### Rules specific to instructional artwork

| Rule | Why |
|---|---|
| **No text, no numbers, no arrows with labels** | These screens are for people who may not read. A picture that needs a caption has failed. Plain arrows and simple motion lines are fine; anything alphabetic is not |
| **The action must be the largest thing in frame** | If the step is "hold the phone one forearm away", the distance is the subject, not the room |
| **One action per image** | A picture showing three things teaches none of them |
| **Show the correct action, never the wrong one** | The app pairs each picture with a separate green tick / red cross line. An image showing the mistake gets remembered as the instruction |
| **Indian families, homes, clothing** | Courtyards, charpais, steel utensils, cotton clothing, tube lights and open doorways — not a Western nursery |
| **Anganwadi setting where an adult is working** | Simple painted room, floor mat, wall chart shapes (no readable text), daylight |
| **Faces calm and content** | Never a crying child, never a worried parent. These are everyday actions |
| **Phones drawn as a plain rounded rectangle** | No brand, no visible UI, no notch. A blank grey screen unless the step is about the screen |
| **Skin tones varied across the set** | The user base spans the full Indian range. Do not default every child to the lightest tone |

### Delivery

- **1024 × 768 PNG** (4:3 landscape) for walkthrough steps and heroes.
- **512 × 512 PNG** (square) for micro-elements.
- Named **exactly** as the `Drawable` column. Lowercase, underscores, no extension in the
  table, `.png` on the file.
- Drop into `android/app/src/main/res/drawable-nodpi/`.

Missing images are safe: the app resolves them by name at runtime and shows a plain
placeholder block for anything not yet delivered. So partial batches can be dropped in and
tested as they are made.

---

## 1. Module 1 walkthrough — growth and everyday skills (6 images)

| Drawable | Step | Prompt |
|---|---|---|
| `guide_m1_sit_together` | Sit together somewhere calm | Indian mother sitting cross-legged on a floor mat in a sunlit home, toddler playing with a wooden toy beside her, a plain phone resting in her hand, unhurried and calm, flat vector illustration |
| `guide_m1_answer_honestly` | Answer what you have really seen | Indian mother thoughtfully watching her toddler take a few steps across a courtyard, she is remembering rather than testing, soft warm daylight, flat vector illustration |
| `guide_m1_not_sure` | "Not sure" is a real answer | Indian mother with an open, relaxed shrug gesture, toddler on her lap, a gentle question-mark shape drawn as a soft cloud above (no letters), reassuring and unembarrassed, flat vector illustration |
| `guide_m1_measure_weight` | Weight and height | Indian health worker weighing a toddler in light clothes on a simple floor scale, mother steadying the child nearby, no numbers visible on the dial, flat vector illustration |
| `guide_m1_measure_arm` | The arm tape | Close view of an Indian health worker placing a measuring tape around a small child's relaxed left upper arm, midway between shoulder and elbow, tape flat against skin, no readable markings, flat vector illustration |
| `guide_m1_no_internet` | It works without internet | A plain phone on a wooden table with a soft glowing cloud shape floating away from it and a simple tick inside the phone, calm reassurance, no text, flat vector illustration |

---

## 2. Module 2 walkthrough — looking and attention (9 images)

These are the ones the app is weakest on today. Each one is a setup condition the
measurement is only valid under, so the picture carries real weight.

| Drawable | Step | Prompt |
|---|---|---|
| `guide_m2_what_happens` | Child will watch a short video | Indian toddler sitting on a floor mat looking at a plain phone propped in front of them, on the phone screen two simple soft shapes — a friendly round face on one side and a coloured spinning toy on the other, child is interested and calm, flat vector illustration |
| `guide_m2_no_photos` | No pictures are kept | A plain phone with a simple eye shape on its screen and a soft shield or lock shape beside it, a crossed-out camera-roll square floating away and dissolving, reassuring not alarming, no text, flat vector illustration |
| `guide_m2_distance` | One forearm away | Side view of an Indian parent holding a plain phone in front of a seated toddler's face, with the parent's own forearm drawn lightly alongside as the measure of the gap, phone level with the child's eyes, clear horizontal spacing is the subject of the image, flat vector illustration |
| `guide_m2_lighting` | Light on the face, not behind | Indian parent and toddler seated facing each other, a bright window behind the parent so warm light falls onto the child's lit face, the phone between them, light direction shown with soft rays, flat vector illustration |
| `guide_m2_steady` | Keep the phone still | A plain phone propped upright against a small stack of books on a wooden table, perfectly steady, a toddler seated in front of it, simple stability lines at the base, flat vector illustration |
| `guide_m2_quiet_room` | Quiet room, nobody behind | A simple Indian room with one adult and one child, a switched-off television on the wall, a closed door, older siblings outside beyond the doorway, calm and uncluttered, flat vector illustration |
| `guide_m2_your_role` | Stay quiet until asked | Indian parent seated behind a propped-up phone, out of the child's view, one hand gently near their own mouth in a quiet gesture, toddler in front looking at the phone, flat vector illustration |
| `guide_m2_green_box` | Watch for the green box | A plain phone screen showing a small camera window with a soft rounded green rectangle around a simplified child's face, the green frame is the brightest element in the picture, flat vector illustration |
| `guide_m2_stop_anytime` | You can stop any time | Indian parent calmly lifting a plain phone away as a toddler turns to look elsewhere, parent's expression relaxed and untroubled, a soft rounded stop shape on the phone, flat vector illustration |

---

## 3. Module 3 walkthrough — sunlight, drops and skin tone (10 images)

The capture steps are the important half. Every one of them is a condition that decides
whether the reading means anything.

| Drawable | Step | Prompt |
|---|---|---|
| `guide_m3_what_this_is` | Sunlight and drops | Indian mother holding a baby in a sunlit doorway, warm morning light falling on the baby's bare arms and legs, a small dropper bottle resting on a shelf nearby, flat vector illustration |
| `guide_m3_not_a_blood_test` | NOT a blood test | A plain phone beside a crossed-out blood-test vial and needle, the crossing-out is soft and clear, phone shown as the calm alternative, no alarm, no blood, flat vector illustration |
| `guide_m3_daily_drops` | Every baby needs the drops | Indian mother giving a single drop from a small dropper into a contented baby's mouth, the baby is relaxed in her arms, one clear drop is the focus, flat vector illustration |
| `guide_m3_grey_card` | You need the grey card | A plain flat neutral-grey rectangular card held between two fingers against a warm household background, the card is unmistakably grey and matte, nothing printed on it, flat vector illustration |
| `guide_m3_shade` | Go into the shade | Indian mother and baby standing in the soft shade of a doorway or a tree, bright sunlight visible beyond them but not falling on them, even gentle light on the baby, flat vector illustration |
| `guide_m3_inner_arm` | Use the inside of the arm | Close view of a small child's arm turned palm-upward, the soft inner forearm near the elbow clearly presented, an adult's hand gently supporting the wrist, flat vector illustration |
| `guide_m3_clean_skin` | Wipe the skin first | An adult's hand gently wiping a baby's inner forearm with a soft dry cloth, a small tin of powder and a bottle of oil set aside and pushed away, flat vector illustration |
| `guide_m3_two_boxes` | Fill both boxes | A plain phone held above a baby's inner forearm with a flat grey card laid on the skin beside it, on the phone screen two soft rounded outline boxes — the left over the arm, the right over the grey card, flat vector illustration |
| `guide_m3_wait_green` | Wait for green | A plain phone screen showing the same two rounded boxes, both now glowing soft green, an adult hand holding the phone very steady, green is the brightest element, flat vector illustration |
| `guide_m3_what_you_get` | What you get | Indian mother and toddler sitting together in gentle morning sunlight in a courtyard, a small sun shape and a soft clock shape floating nearby as friendly symbols of time outdoors, no numerals on the clock, flat vector illustration |

---

## 4. Module heroes (3 images)

Wide, calm, atmospheric. These sit at the top of each module intro and set the tone before
any instruction is read. Less literal than the step images.

| Drawable | Module | Prompt |
|---|---|---|
| `hero_module_1` | Growth and everyday skills | Warm wide scene of an Indian toddler mid-stride across a sunlit courtyard toward an outstretched parent, a few growth-ring shapes echoing softly in the background, generous negative space, flat vector illustration |
| `hero_module_2` | Looking and attention | Warm wide scene of an Indian infant and parent sharing eye contact very close together, soft concentric circles radiating gently from the meeting of their gaze, generous negative space, flat vector illustration |
| `hero_module_3` | Sunlight and vitamin D | Warm wide scene of an Indian mother holding a baby in an open sunlit doorway, long soft morning light across the floor, a large gentle sun shape low in the frame, generous negative space, flat vector illustration |

---

## 5. Micro-elements (18 images)

Small square pictures reused all over the app — result cards, empty states, sync status,
progress. They replace bare icons in the places where a caregiver needs reassurance rather
than information.

These must read clearly at **48 px**, so: one subject, bold silhouette, no fine detail, no
faces smaller than a quarter of the frame.

### Status and feedback

| Drawable | Used for | Prompt |
|---|---|---|
| `micro_on_track` | A result that is fine | A small green sprout with two rounded leaves growing from soft ground, gentle and reassuring, flat vector, square, bold simple silhouette |
| `micro_watch` | Something to keep an eye on | A pair of soft open eyes in a rounded amber circle, attentive not anxious, flat vector, square, bold simple silhouette |
| `micro_look_closer` | Refer for a check | A rounded magnifying glass over a soft heart shape, caring and calm, warm amber, flat vector, square, bold simple silhouette |
| `micro_see_doctor` | Priority referral | A friendly stethoscope curved into a soft rounded shape, warm and unfrightening, flat vector, square, bold simple silhouette |
| `micro_not_done` | Not assessed yet | A soft dotted rounded square outline with a gentle plus sign in the middle, quiet grey, flat vector, square |
| `micro_complete` | Module finished | A rounded green tick inside a soft filled circle, satisfying and clear, flat vector, square, bold simple silhouette |

### Modules and navigation

| Drawable | Used for | Prompt |
|---|---|---|
| `micro_module_milestones` | Module 1 tile | A small child's footprint beside a taller one, warm terracotta, flat vector, square, bold simple silhouette |
| `micro_module_attention` | Module 2 tile | A single soft open eye with a small radiating arc in front of it, warm violet, flat vector, square, bold simple silhouette |
| `micro_module_vitamind` | Module 3 tile | A simple round sun with soft rays falling onto a small rounded arm shape, warm amber, flat vector, square, bold simple silhouette |
| `micro_growth_chart` | Growth section | A gentle rising curve over two soft axis lines, no numbers or gridlines, warm green, flat vector, square |
| `micro_immunization` | Vaccines section | A small rounded syringe lying flat beside a soft shield shape, friendly and blunt-tipped, flat vector, square, bold simple silhouette |
| `micro_report` | Report section | A rounded document shape with three soft content lines (no letters) and a small tick in the corner, flat vector, square |

### States and reassurance

| Drawable | Used for | Prompt |
|---|---|---|
| `micro_offline_saved` | Saved on this phone | A rounded phone shape with a small soft tick inside it and a gentle enclosing arc, calm and safe, flat vector, square |
| `micro_syncing` | Sending to the cloud | A soft rounded cloud with a gentle upward arrow beneath it, light and unhurried, flat vector, square |
| `micro_no_child_yet` | Empty child list | A soft empty rounded cradle or basket with a small blanket folded in it, warm and inviting, flat vector, square |
| `micro_privacy` | No photos kept | A soft rounded shield with a small closed eye inside it, quiet and protective, flat vector, square |
| `micro_how_it_works` | Explanation buttons | A soft lightbulb with three small radiating lines, warm amber, friendly not clinical, flat vector, square |
| `micro_sun_window` | Best time for sunlight | A small sun sitting low between two soft rounded hill shapes with a gentle clock arc above, warm, flat vector, square |

---

## 6. What to send back

Zip the PNGs with their exact drawable names. Partial batches are fine and can be dropped in
as they are made — the app shows a neutral placeholder for anything not yet present, so
nothing breaks and nothing has to wait for the full set.

Priority order if the set is being generated in stages:

1. **Module 3 capture steps** (`guide_m3_grey_card` → `guide_m3_wait_green`) — without these
   the skin reading is being taken by people guessing at the method.
2. **Module 2 setup steps** (`guide_m2_distance`, `guide_m2_lighting`, `guide_m2_steady`,
   `guide_m2_green_box`) — the four conditions that decide whether a session is usable.
3. The remaining walkthrough steps.
4. Micro-elements.
5. Heroes.

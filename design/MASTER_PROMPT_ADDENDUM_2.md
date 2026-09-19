# Poshan — Image Prompt File · Addendum 2

**Coverage:** every remaining questionnaire input, the growth-measurement demonstrations,
the growth-chart visuals and the home module tiles.
Read alongside `design/MASTER_PROMPT.md` — §1 (Global Style Block) applies to **every**
prompt below and is not repeated.

**Current asset status**

| Group | Have | Needed |
|---|---|---|
| Category illustrations | 5 | 0 |
| Milestone illustrations | 18 | **105** (full 123-item bank) |
| Nutrition / feeding questions | 0 | **14** |
| Growth measurement demos | 0 (schematic vectors in app) | **3** |
| Growth chart visuals | 0 | **6** |
| Module tiles | 0 | **3** |
| Report / referral | 3 | 0 |
| **Total new** | | **131** |

File naming for everything below: `lowercase_with_underscores.webp`, dropped into
`android/app/src/main/res/drawable-nodpi/`.
Workflow: generate §A anchor first, reuse it as a **style reference image** for every
later asset, then Live-Layers the text for anything with labels (§B, §C, §E).

---

## A. Remaining milestone illustrations (105)

Template — fill `{{ACTION}}`, one asset per question.

```
Flat vector illustration for a child development question card: {{ACTION}}.
One Indian baby or toddler, simple rounded body, dot eyes, gentle happy expression,
centred and large in the frame so it reads on a small phone screen.
Cream background #FFF6E9, teal and amber clothing, thick rounded strokes, no gradients,
no text, generous negative space around the child.
```

> **Anchor:** regenerate `ms_0_3_lift_head` once with the new style block, then use it as
> the reference image for all 105 so the set is visually identical.

### A1 · 0–3 months (12 new; 1 already have)

| # | Question | `{{ACTION}}` | File |
|---|---|---|---|
| 1 | active arm & leg movement | a baby lying on their back kicking both legs and waving both arms | `ms_0_3_move_arms_legs` ✓ |
| 2 | head control improving | a baby held upright against a caregiver's chest holding their head steady | `ms_0_3_head_control` |
| 3 | opens hands briefly | a baby lying on their back with both tiny hands open and relaxed | `ms_0_3_open_hands` |
| 4 | hands toward mouth | a baby bringing both hands up to their mouth | `ms_0_3_hands_to_mouth` ✓ |
| 5 | holds an object placed in hand | a caregiver placing a soft rattle into a baby's open palm | `ms_0_3_holds_object` |
| 6 | noises other than crying | a baby lying happily making cooing sounds with a small sound-wave shape near the mouth | `ms_0_3_cooing` |
| 7 | reacts to familiar voices | a baby turning slightly toward a caregiver who is speaking softly | `ms_0_3_reacts_voice` |
| 8 | pays attention when others speak | a baby lying still and alert, eyes wide, while a caregiver speaks | `ms_0_3_listens` |
| 9 | looks at your face | a baby on a caregiver's lap gazing up at the caregiver's face | `ms_0_3_look_at_face` ✓ |
| 10 | smiles responsively | a baby smiling back at a smiling caregiver | `ms_0_3_responsive_smile` ✓ |
| 11 | shows interest in interaction | a baby brightening and wriggling while a caregiver plays with them | `ms_0_3_interest_interaction` |
| 12 | turns head toward activity or sound | a baby turning their head toward a small hanging toy that is moving | `ms_0_3_turn_to_sound` |
| 13 | lifts head on tummy | a baby lying on their tummy lifting their head momentarily | `ms_0_3_lift_head` ✓ |

### A2 · 4–6 months (9 new)

| # | `{{ACTION}}` | File |
|---|---|---|
| 1 | a baby held upright with their head completely steady | `ms_4_6_steady_head` ✓ |
| 2 | a baby on their tummy pushing up on straight arms, chest lifted | `ms_4_6_push_up_arms` ✓ |
| 3 | a baby rolling from their tummy onto their back on a mat | `ms_4_6_roll_tummy_back` ✓ |
| 4 | a caregiver supporting a baby standing so their legs take some weight | `ms_4_6_bear_weight_legs` |
| 5 | a baby reaching both hands toward a dangling soft toy | `ms_4_6_grasp_toy` ✓ |
| 6 | a baby successfully grabbing a small cloth ball with one hand | `ms_4_6_grab_object` |
| 7 | a baby putting a safe teething ring toward their mouth | `ms_4_6_mouthing` |
| 8 | a baby babbling with a small speech bubble of abstract shapes beside them | `ms_4_6_babble` |
| 9 | a baby turning toward a caregiver who is talking to them | `ms_4_6_responds_speech` |
| 10 | a baby making a loud sound to get a caregiver's attention | `ms_4_6_sounds_attention` |
| 11 | a baby laughing while a caregiver tickles them | `ms_4_6_laughs` |
| 12 | a baby staring intently at a caregiver's face | `ms_4_6_stares_faces` |
| 13 | a baby reaching curiously toward a colourful toy | `ms_4_6_curious` |

### A3 · 7–9 months (10 new)

| # | `{{ACTION}}` | File |
|---|---|---|
| 1 | a baby sitting upright unsupported on a mat, hands free | `ms_7_9_sit_unsupported` ✓ |
| 2 | a baby rolling over completely on a floor mat | `ms_7_9_rolls_both_ways` |
| 3 | a baby shifting from sitting to lying and back | `ms_7_9_changes_position` |
| 4 | a baby on hands and knees starting to crawl forward | `ms_7_9_attempts_crawl` |
| 5 | a baby accurately reaching out and closing their hand on a small block | `ms_7_9_reach_grasp` |
| 6 | a baby passing a small toy from one hand to the other | `ms_7_9_pass_toy` ✓ |
| 7 | a baby picking a small piece of food up with thumb and fingers | `ms_7_9_thumb_fingers` |
| 8 | a baby babbling different syllables with abstract sound shapes nearby | `ms_7_9_babble_sounds` |
| 9 | a baby turning toward a caregiver who is calling their name | `ms_7_9_responds_name` |
| 10 | a baby copying a sound a caregiver just made | `ms_7_9_imitates_sounds` |
| 11 | a baby smiling at a familiar grandparent | `ms_7_9_recognises_people` |
| 12 | a baby reaching toward their mother instead of a stranger | `ms_7_9_prefers_caregiver` |
| 13 | a caregiver and baby playing peek-a-boo with a cloth | `ms_7_9_peek_a_boo` ✓ |

### A4 · 10–12 months (13 new)

| # | `{{ACTION}}` | File |
|---|---|---|
| 1 | a baby sitting on the floor completely unsupported | `ms_10_12_sits_alone` |
| 2 | a baby crawling across the floor toward a toy | `ms_10_12_crawls` |
| 3 | a baby pulling up to stand holding the edge of a low stool | `ms_10_12_pull_up_stand` ✓ |
| 4 | a baby standing while holding onto a sofa | `ms_10_12_stands_support` |
| 5 | a baby picking up a tiny object with a neat pincer grip | `ms_10_12_pincer_grip` ✓ |
| 6 | a baby deliberately dropping a toy from a high chair tray | `ms_10_12_releases_object` |
| 7 | a baby pointing a finger toward a ball across the room | `ms_10_12_points` |
| 8 | a caregiver asking the baby to come and the baby moving toward them | `ms_10_12_follows_instruction` |
| 9 | a baby waving bye-bye with an open hand | `ms_10_12_waves` |
| 10 | a baby saying a first clear word, shown as an abstract speech shape | `ms_10_12_first_words` |
| 11 | a baby turning when their name is called | `ms_10_12_responds_name` |
| 12 | a baby copying a caregiver clapping their hands | `ms_10_12_imitates_actions` |
| 13 | a baby and caregiver playing with a ball together on the floor | `ms_10_12_plays_caregiver` |

### A5 · 13–18 months (15 new)

| # | `{{ACTION}}` | File |
|---|---|---|
| 1 | a toddler standing alone in the middle of a room | `ms_13_18_stands_alone` |
| 2 | a toddler walking unsupported with arms out for balance | `ms_13_18_walk_unsupported` ✓ |
| 3 | a toddler squatting down to pick up a toy then standing again | `ms_13_18_squats` |
| 4 | a toddler climbing onto a low cushioned stool with a caregiver nearby | `ms_13_18_climbs_furniture` |
| 5 | a toddler picking up a crumb with a precise pincer grip | `ms_13_18_pincer` |
| 6 | a toddler stacking two blocks one on top of the other | `ms_13_18_stack_blocks` ✓ |
| 7 | a toddler scooping food with a spoon, some help from a caregiver | `ms_13_18_uses_spoon` |
| 8 | a toddler scribbling with a thick crayon on paper | `ms_13_18_scribbles` |
| 9 | a toddler saying a single meaningful word, abstract speech shape | `ms_13_18_meaningful_words` |
| 10 | a toddler following a simple request from a caregiver | `ms_13_18_understands_simple` |
| 11 | a toddler pointing at a bottle to ask for it | `ms_13_18_points_wants` |
| 12 | a toddler copying a word a caregiver says | `ms_13_18_imitates_words` |
| 13 | a toddler hugging a familiar grandparent | `ms_13_18_shows_affection` |
| 14 | a caregiver and toddler playing a simple hand game together | `ms_13_18_interactive_games` |
| 15 | a toddler looking at and approaching another small child | `ms_13_18_interest_children` |

### A6 · 19–24 months (13 new)

| # | `{{ACTION}}` | File |
|---|---|---|
| 1 | a toddler walking confidently across a room | `ms_19_24_walks` |
| 2 | a toddler running with both feet briefly off the ground | `ms_19_24_runs` |
| 3 | a toddler climbing a step while a caregiver holds their hand | `ms_19_24_climbs_stairs` |
| 4 | a toddler kicking a soft ball | `ms_19_24_kick_ball` ✓ |
| 5 | a toddler building a tower of four blocks | `ms_19_24_builds_tower` |
| 6 | a toddler turning the thick pages of a board book | `ms_19_24_turn_pages` ✓ |
| 7 | a toddler making simple crayon marks on paper | `ms_19_24_scribbles` |
| 8 | a toddler using several single words, abstract speech shapes | `ms_19_24_several_words` |
| 9 | a toddler joining two words together, two linked speech shapes | `ms_19_24_two_words` |
| 10 | a toddler following a two-part request from a caregiver | `ms_19_24_two_step` |
| 11 | a toddler reaching up and pointing to ask to be picked up | `ms_19_24_communicates_wants` |
| 12 | a toddler putting on their own simple chappals | `ms_19_24_independence` |
| 13 | a toddler playing beside another child with a shared toy | `ms_19_24_interacts` |

### A7 · 25–36 months (14 new)

| # | `{{ACTION}}` | File |
|---|---|---|
| 1 | a child running steadily without falling | `ms_25_36_runs_steadily` |
| 2 | a child jumping with both feet leaving the ground | `ms_25_36_jumps` |
| 3 | a child walking down a step while holding a caregiver's hand | `ms_25_36_stairs` |
| 4 | a child throwing a soft ball overarm | `ms_25_36_throws` |
| 5 | a child copying a straight line with a crayon | `ms_25_36_copies_lines` |
| 6 | a child stacking a tower of six blocks | `ms_25_36_stack_blocks` |
| 7 | a child turning the pages of a picture book one at a time | `ms_25_36_turns_pages` |
| 8 | a child speaking a short three-word sentence, abstract speech shapes | `ms_25_36_short_sentences` |
| 9 | a stranger and a caregiver both listening to a child speak | `ms_25_36_unfamiliar_understand` |
| 10 | a child naming everyday objects on a table | `ms_25_36_names_objects` |
| 11 | a child answering a simple question from a caregiver | `ms_25_36_answers_questions` |
| 12 | a child pointing excitedly at a bird to show a caregiver | `ms_25_36_shows_interest` |
| 13 | two children playing a simple game together | `ms_25_36_plays_children` |
| 14 | a child feeding a doll with a spoon in pretend play | `ms_25_36_pretend_play` |

### A8 · 37–48 months (13 new)

| # | `{{ACTION}}` | File |
|---|---|---|
| 1 | a child running and changing direction without falling | `ms_37_48_runs_turns` |
| 2 | a child jumping forward with both feet together | `ms_37_48_jumps_forward` |
| 3 | a child walking down stairs without holding on | `ms_37_48_stairs_alone` |
| 4 | a child standing on one foot with arms out | `ms_37_48_stands_one_foot` |
| 5 | a child drawing a simple circle with a crayon | `ms_37_48_draws_circle` |
| 6 | a child holding a pencil correctly | `ms_37_48_holds_pencil` |
| 7 | a child buttoning a large button on a shirt | `ms_37_48_buttons` |
| 8 | a child speaking a full sentence, abstract speech shapes | `ms_37_48_sentences` |
| 9 | a child telling a caregiver about something that happened | `ms_37_48_tells_story` |
| 10 | a child answering a who / what / where question | `ms_37_48_answers_wh` |
| 11 | a group of children playing a circle game together | `ms_37_48_group_play` |
| 12 | two children taking turns rolling a ball | `ms_37_48_takes_turns` |
| 13 | a child waving happily as a caregiver leaves the room | `ms_37_48_separates` |

### A9 · 49–60 months (16 new)

| # | `{{ACTION}}` | File |
|---|---|---|
| 1 | a child hopping on one foot across the floor | `ms_49_60_hops` |
| 2 | a child running and dodging sideways easily | `ms_49_60_runs_changes` |
| 3 | a child catching a large soft ball with both hands | `ms_49_60_catches_ball` |
| 4 | a child climbing the steps of a small playground slide | `ms_49_60_climbs_playground` |
| 5 | a child drawing a stick figure person with head, arms and legs | `ms_49_60_draws_person` |
| 6 | a child copying a square shape with a crayon | `ms_49_60_copies_shapes` |
| 7 | a child cutting a strip of paper with round safety scissors, supervised | `ms_49_60_scissors` |
| 8 | a child zipping up their own jacket | `ms_49_60_fasteners` |
| 9 | a child speaking clearly to an unfamiliar adult | `ms_49_60_speaks_clearly` |
| 10 | a child telling a simple story with a small audience | `ms_49_60_tells_story` |
| 11 | a child answering questions about a picture book | `ms_49_60_story_questions` |
| 12 | a child following three spoken instructions in a row | `ms_49_60_multi_step` |
| 13 | four children building something together cooperatively | `ms_49_60_cooperative` |
| 14 | a child waiting in line and following a game rule | `ms_49_60_follows_rules` |
| 15 | a child looking calm and naming an emotion | `ms_49_60_expresses_emotions` |
| 16 | a child brushing their teeth and dressing with minimal help | `ms_49_60_routines` |

---

## B. Nutrition & feeding illustrations (14)

All `{{SIZE}}=square 1:1`, same style block, same anchor as §A.

| # | Input | Prompt |
|---|---|---|
| 1 | Feeding status · breastfeeding | `Flat vector illustration of an Indian mother breastfeeding her baby in a calm home setting, a soft cloth draped for privacy, both relaxed and content. Cream background #FFF6E9, teal and coral palette, thick rounded strokes, no gradients, no text, centred, dignified and warm.` — `nut_breastfeeding` |
| 2 | Feeding status · mixed feeding | `Flat vector illustration of a caregiver spoon-feeding a baby in a high chair while a bottle sits on the tray, both foods shown as simple rounded shapes. Cream background #FFF6E9, teal and amber palette, no text.` — `nut_mixed_feeding` |
| 3 | Feeding status · family food | `Flat vector illustration of a toddler sitting on the floor with a small round plate of rice, dal and vegetables, eating with their hands, happy. Cream background #FFF6E9, teal, amber and leaf-green palette, no text.` — `nut_family_food` |
| 4 | Food group · grains | `Flat vector illustration of a small bowl of rice with two chapatis beside it, drawn as simple rounded shapes. Cream background #FFF6E9, warm beige and amber palette, thick rounded strokes, no gradients, no text, centred.` — `nut_group_grains` |
| 5 | Food group · dal / legumes | `Flat vector illustration of a small bowl of yellow dal with a few whole lentils beside it as rounded dots. Cream background #FFF6E9, amber and leaf-green palette, no text.` — `nut_group_dal` |
| 6 | Food group · milk | `Flat vector illustration of a small glass of milk with a simple milk-drop shape beside it. Cream background #FFF6E9, soft blue-white and mint palette, no text.` — `nut_group_milk` |
| 7 | Food group · eggs | `Flat vector illustration of a boiled egg cut in half on a small plate with a spoon. Cream background #FFF6E9, cream-yellow and amber palette, no text.` — `nut_group_eggs` |
| 8 | Food group · meat / fish | `Flat vector illustration of a small plate with a piece of fish and a drumstick drawn as simple rounded shapes. Cream background #FFF6E9, coral and amber palette, no text.` — `nut_group_meat_fish` |
| 9 | Food group · vegetables | `Flat vector illustration of a small basket with a carrot, a green leafy vegetable and a tomato as rounded shapes. Cream background #FFF6E9, leaf-green and coral palette, no text.` — `nut_group_vegetables` |
| 10 | Food group · fruits | `Flat vector illustration of a banana, a guava and a small mango arranged together as rounded shapes. Cream background #FFF6E9, sun-yellow and coral palette, no text.` — `nut_group_fruits` |
| 11 | Food group · fats / oil | `Flat vector illustration of a small glass bottle of cooking oil with a single ghee drop beside it. Cream background #FFF6E9, amber and gold palette, no text.` — `nut_group_fats` |
| 12 | Meals per day | `Flat vector illustration of three small round plates in a neat row representing breakfast, lunch and dinner, each with a simple food shape. Cream background #FFF6E9, teal and amber palette, no text.` — `nut_meals_per_day` |
| 13 | Feeding difficulty | `Flat vector illustration of a caregiver gently offering a spoon to a toddler who is turning their head away, both calm and not distressed. Cream background #FFF6E9, teal and coral palette, no text.` — `nut_feeding_difficulty` |
| 14 | Dietary diversity | `Flat vector illustration of a round thali divided into five coloured sections, each holding a different simple food shape, with a small tick badge floating above. Cream background #FFF6E9, multi-accent palette, no text.` — `nut_dietary_diversity` |

---

## C. Growth measurement demonstrations (3)

These replace the schematic vectors currently in the app. `{{SIZE}}=wide 4:3`.
Use **Live Layers → Add Text** for the numbered callouts so labels are exact.

### C1 · Length (under 2 years, lying with an infantometer)
```
A wide 4:3 instructional health illustration, flat vector style, cream background #FFF6E9.
An Indian health worker measuring a baby's length on a flat infantometer board.
Left: the baby lies on their back, head against the fixed headboard, legs gently
straightened by the worker's hand, feet flat against the sliding footboard.
Right: a small magnified detail circle showing the headboard contact point.
Three teal numbered circles '1', '2', '3' float beside the head, knees and heel.
A teal measuring tape with rounded tick marks runs along the board edge.
Clean, uncluttered, instructional, no other text, no gradients.
```

### C2 · Weight (calibrated scale)
```
A wide 4:3 instructional health illustration, flat vector style, cream background #FFF6E9.
An Indian health worker placing a baby on a flat digital baby scale on a low table.
The scale display is a large rounded rectangle with a blank readout panel (no digits).
The baby wears a single light cloth layer. A small towel and a spare nappy lie folded
on the table beside the scale. Two teal numbered circles '1' and '2' float beside the
scale display and the baby.
Clean, uncluttered, instructional, no other text, no gradients.
```

### C3 · MUAC (mid-upper arm circumference)
```
A wide 4:3 instructional health illustration, flat vector style, cream background #FFF6E9.
A close view of a young Indian child's left arm hanging relaxed at their side.
A health worker's hands wrap a slim non-stretch MUAC tape around the mid-point of the
upper arm. A small dashed vertical guide line marks the mid-point between the shoulder
tip and the elbow tip, with a tiny arrow between them.
Three floating tape segments beside the arm show the colour bands: green, yellow, red,
each as a simple rounded rectangle with no text.
Two teal numbered circles '1' and '2' float near the mid-point and the tape.
Clean, instructional, no other text, no gradients.
```

---

## D. Growth chart visuals (6)

The app embeds the official WHO charts. These prompts are only for the **helper
graphics** that sit beside them — do not redraw the WHO curves.

| # | Purpose | Prompt |
|---|---|---|
| 1 | Weight-for-age header | `A wide 16:9 flat vector banner, cream background #FFF6E9. Left: a simple rounded weighing scale with a baby. Right: a small stylised growth curve rising smoothly across a rounded grid with three horizontal guide lines in green, yellow and red. Thick rounded strokes, teal and amber palette, no text, no numbers.` — `chart_banner_weight` |
| 2 | Length/height-for-age header | `A wide 16:9 flat vector banner, cream background #FFF6E9. Left: a toddler standing beside a rounded measuring ruler with tick marks. Right: a stylised growth curve rising across a rounded grid with green, yellow and red guide lines. Teal, mint and amber palette, thick rounded strokes, no text.` — `chart_banner_height` |
| 3 | BMI-for-age header | `A wide 16:9 flat vector banner, cream background #FFF6E9. Left: a small rounded balance scale with a fruit shape on one side and a child's shoe on the other. Right: a stylised curve with a gentle peak across a rounded grid, green, yellow and red guide lines. Violet and mint palette, thick rounded strokes, no text.` — `chart_banner_bmi` |
| 4 | MUAC band legend | `A horizontal flat vector legend strip on a cream background #FFF6E9. Three rounded pill shapes side by side in green, yellow and red, each with plenty of empty space to the right for a label to be added later. Thick rounded strokes, no text, no gradients, generous padding.` — `chart_legend_muac` |
| 5 | Normal vs concern zones | `A square 1:1 flat vector diagram, cream background #FFF6E9. A rounded vertical bar divided into three soft zones — green at the top, amber in the middle, red at the bottom. A small smiling child icon sits in the green zone, a neutral face in the amber, and a small alert icon in the red. No text, no numbers, thick rounded strokes, teal and amber palette.` — `chart_zones` |
| 6 | Growth monitoring visit card | `A square 1:1 flat vector illustration, cream background #FFF6E9. A health worker writing a child's weight on a small clipboard beside a simple growth chart pinned to a wall. A mother holds a toddler nearby. Calm, encouraging, no text, teal and amber palette, thick rounded strokes.` — `chart_visit` |

---

## E. Home module tiles (3)

`{{SIZE}}=square 1:1`. Only the **icon glyph** is needed — the tile colour is applied in
code, so keep the artwork on a transparent or cream background and let it sit centred with
generous padding.

| # | Module | Prompt |
|---|---|---|
| 1 | Development | `A square 1:1 flat vector icon of a child building a tower of three rounded blocks, single subject, centred with generous padding, teal and mint palette, thick rounded strokes, no gradients, no text, plain cream background #FFF6E9.` — `tile_development` |
| 2 | Attention & gaze | `A square 1:1 flat vector icon of a toddler's face in three-quarter view with one wide open eye and a short dotted line from the eye to a small rounded screen shape. Single subject, centred, generous padding, violet and mint palette, thick rounded strokes, no text, plain cream background #FFF6E9.` — `tile_attention` |
| 3 | Nutrition & Vitamin D | `A square 1:1 flat vector icon of a small round sun above a bowl of food with a single leaf, combining both ideas in one simple mark. Single subject, centred, generous padding, amber and leaf-green palette, thick rounded strokes, no text, plain cream background #FFF6E9.` — `tile_nutrition_vitd` |

---

## F. Generation checklist

1. Generate `ms_0_3_lift_head` and `tile_development` first — they are the style anchors.
2. Work **one table at a time**, attaching the anchor as a reference image.
3. Log every asset in `design/reve-seeds.md` (asset → prompt version → seed → date).
4. Export at **900 px** for milestone/nutrition squares, **1280 px** for wide banners.
5. Drop the files into `android/app/src/main/res/drawable-nodpi/` using the **exact file
   names** in the tables — the app already resolves them by name.
6. Before committing, run the background-removal pass
   (`tools/remove_bg.py` logic used earlier) so the cream does not show as a box on cards
   that are not cream.

> Reminder: Reve has no negative-prompt parameter. If an unwanted element appears, use
> **Select/Spotlight → remove that region** rather than adding "no …" to the prompt.

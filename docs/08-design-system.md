# 08 — Design System

Material 3 **Expressive**, on alpha/beta Compose artifacts (`material3 1.5.0-alpha18`,
`compose 1.11.0-beta02`) because the Expressive APIs — emphasized type, wavy progress — are
not in a stable release yet. Pin exact versions; never use dynamic ones.

Everything here is used by caregivers and Anganwadi workers, frequently outdoors in bright
sun, often by people who are not young and not confident with phones. That drives the whole
system: large type, large targets, high contrast, and no interaction whose meaning depends
on noticing something subtle.

---

## 1. Interaction rules

### 1.1 Clip before clickable — always

**This is the single easiest visual bug to introduce in this codebase, and it was present in
three places.**

Compose draws the press ripple in the bounds of the modifier chain *at the point
`clickable` is applied*. If the shape is established after that — by `Card(shape = …)`, by
`background(color, shape)`, or by a later `clip` — the ripple is not clipped to it, and a
**hard-edged rectangle flashes around the rounded component** on every tap.

```kotlin
// WRONG — sharp rectangular ripple around a rounded card
Modifier
    .background(container, RoundedCornerShape(24.dp))
    .clickable(onClick = onClick)

// RIGHT — ripple follows the corners
Modifier
    .clip(RoundedCornerShape(24.dp))
    .background(container)
    .clickable(onClick = onClick)
```

The `Card` case is subtler and worth calling out separately, because the code *looks*
correct: `Card(shape = shape)` clips the card's **content**, not the modifier chain handed
to it. A `clickable` in that chain still ripples rectangularly.

```kotlin
// WRONG
Card(modifier = modifier.clickable { … }, shape = shape)

// RIGHT
Card(modifier = modifier.clip(shape).clickable { … }, shape = shape)
```

Fixed in `PoshanCard` (`ui/components/Components.kt`), `ModuleTile`
(`ui/screens/ModuleTile.kt`) and the GPS row (`ui/screens/OnboardingScreens.kt`).

> **Rule for review:** any diff that adds `.clickable` must have a `.clip` above it in the
> same chain, or use a component that already does (`PoshanCard`, `PoshanPrimaryButton`).

### 1.2 The whole card is the target

Touch targets are **48dp minimum**. Prefer making an entire card clickable over placing a
small button inside it — a thin row is hard to hit for anyone whose hands are not steady.
The Home quick-actions are a 2×2 grid of full cards for exactly this reason.

### 1.3 Every tap has a physical counterpart

`ui/haptics/AppHaptics.kt` defines a semantic vocabulary rather than raw vibration calls.
On API 31+ it composes `VibrationEffect` primitives and falls back gracefully below.

| Call | Use for |
|---|---|
| `tick()` | Tab changes, toggles, card taps, list scrolling |
| `click()` | Selecting a response card |
| `confirm()` | Answer saved, category completed, long-press accepted |
| `reject()` | Red flag surfaced, invalid input |
| `success()` | Assessment submitted (slow-rise + click composition) |

`PoshanCard` fires `tick()` itself, so call sites must not fire a second one.

### 1.4 Motion

Card press: spring, `dampingRatio = 0.5f`, `stiffness = 1000f`, scaling to `0.975f` —
fast and barely perceptible, enough to confirm the touch registered.
Chart plotting: spring, `dampingRatio = 0.72f`, `stiffness = 90f` — slow enough to read.
Question transitions: slide + fade, ~200 ms in / ~180 ms out.

Animation is confirmation, never decoration. Anything that delays a field worker by more
than a beat is wrong.

---

## 2. Colour

### 2.1 Risk palette — clinical meaning, not decoration

`ui/theme/Color.kt`, exposed as `RiskColors` via a CompositionLocal (`riskColors`).

| Token | Light | Soft (container) | Dark theme | Meaning |
|---|---|---|---|---|
| `low` | `#1E7B4D` | `#D7F2E3` | `#7BDBAA` | On track / within normal |
| `monitor` | `#B07500` | `#FFEFCF` | `#F3C567` | Borderline — watch at next visit |
| `review` | `#C4561A` | `#FFE0CD` | `#F3A171` | Outside normal — needs follow-up |
| `priority` | `#B3231F` | `#FFD9D7` | `#F5A09C` | Severe — refer |

These map directly onto WHO z-score bands and `DomainStatus`, so a colour is never chosen
for looks. Green is not "good", it is "inside −2 to +2 SD".

**Never encode clinical state in colour alone** — every risk colour is paired with a label
(`RiskChip`, `DomainChip`, `StatusChip`). Outdoor glare and colour vision deficiency both
defeat colour-only encoding.

### 2.2 Accent palette — surfaces, not status

`AccentColors`: mint `#DDF3EC`, amber `#FFF0D8`, coral `#FFE4DA`, violet `#E9E5FF`. Pale
tints for module tiles, avatars and quick-action icons. **They carry no clinical meaning**
and must never substitute for a risk colour.

---

## 3. Typography

`ui/theme/Type.kt`.

### 3.1 The font pairing, and why it is a pairing

**`Baloo 2`** (Latin + Devanagari) + **`Baloo Tammudu 2`** (Telugu), selected by locale in
`poshanTypography()`.

No single Google font covers Latin, Devanagari and Telugu. The rounded fonts most common in
children's apps — Nunito, Quicksand, Fredoka — are **Latin-only**, so Hindi and Telugu would
silently fall back to the system font and the UI would visibly break mid-screen. The Baloo
family are stylistic siblings, so switching language does not change the app's voice.

Selection is by active locale at composition time, not per-glyph fallback, which would drop
Telugu into the system font.

### 3.2 The scale is deliberately larger than stock M3

Default M3 is tuned for consumer apps read at arm's length by young eyes. This app is read
by grandparents and by health workers outdoors in bright sun.

| Style | Size / line height |
|---|---|
| displayLarge / Medium / Small | 60/68 · 48/56 · 38/46 |
| headlineLarge / Medium / Small | 34/42 · 30/38 · 26/34 |
| titleLarge / Medium / Small | 24/30 · 19/26 · 17/24 |
| bodyLarge / Medium / Small | 17/26 · 15/22 · 14/20 |
| labelLarge / Medium / Small | 16/22 · 14/18 · **13/17** |

**13sp is a hard floor.** Nothing in the app may be smaller — the one exception is the
developer mesh overlay HUD, which is diagnostic and never seen by a caregiver.

Headings use the M3 Expressive *emphasized* variants for a heavier voice.

### 3.3 Shape

`PoshanShapes`: extraSmall 8dp, small 14dp, medium 22dp, large 28dp, extraLarge 36dp.
`FieldCorner` = 20dp for every input. Generous rounding is a signature of the expressive
scale — and see §1.1 about clipping it properly.

---

## 4. Component catalogue

`ui/components/Components.kt`.

| Component | Use |
|---|---|
| `PoshanScaffold(title, onBack, actions, bottomBar, …)` | Standard screen frame; handles `safeDrawing` insets |
| `PoshanSurface` | Full-bleed screen without a top bar |
| `PoshanPrimaryButton(text, icon, enabled, onClick)` | The one main action on a screen |
| `PoshanTonalButton` | Secondary action |
| `PoshanOutlinedButton` | Tertiary / cancel |
| `PoshanTextField(value, onChange, label, keyboardType, supportingText, filter)` | Free text; `filter` constrains input (e.g. digits, max length) |
| `PoshanDropdownField(value, onSelect, label, options, allowOther)` | Fixed-set input. `allowOther` reveals a free-text field — a relationship list must not trap someone whose situation is not listed |
| `PoshanCard(onClick, container, shape, contentPadding)` | The universal container. Handles press-scale, haptics and ripple clipping |
| `RiskChip(band)` / `DomainChip(status)` / `StatusChip(label, color)` | Labelled status — colour plus text, always |
| `PoshanProgress(value, label, showPercent)` | Wavy linear progress (Expressive) |
| `PoshanLoader(text)` | Indeterminate wait |
| `StatTile(label, value, accent)` | Compact metric |
| `InfoBanner(text)` | Non-blocking context or caveat |
| `TextLink`, `SmallIconButton` | Inline affordances |
| `PoshanBottomBar(items, selectedRoute, onSelect)` | Home + Report only |
| `Gap(n)`, `HSpacer(n)`, `SectionLabel(text)` | Layout primitives |

**Do not use raw `Button`, `Card` or `OutlinedTextField` directly.** They bypass the
haptics, the press motion and the ripple clipping.

---

## 5. Navigation shell

Bottom bar carries **two** destinations: Home and Report. The child switcher is an **app-bar
action on Home** (`ChildSwitcherAction`) showing the active child's initial, because
switching child is a context change, not a destination — and as a third tab it was taking a
third of the bar for something used rarely.

Home layout, top to bottom: hero child card (avatar, age band, risk chip, key measurements)
→ growth z-score chips → next-visit card → the sequential 3-module plan with wavy progress
and one unmistakable "Continue" button → 2×2 quick-action grid → sync row.

---

## 6. Artwork

`design/MASTER_PROMPT.md` and `MASTER_PROMPT_ADDENDUM_2.md` hold the Reve image-generation
master prompt system: one source of truth for style, palette and framing so assets generated
weeks apart still belong together.

Filenames map directly to drawable names. `ui/screens/MilestoneArt.kt` maps question ids to
illustrations (45 of 123 questions have their own; the rest fall back to category art).
`ui/screens/ModuleTile.kt` carries the module tile art.

Launcher icon is a proper adaptive icon: legacy PNGs at all five densities plus
`ic_launcher_foreground.png` on 108dp canvases with the artwork inside the 66% safe zone.
The earlier hand-made vector in `mipmap-anydpi-v26` overrode the real icon on Android 8+ and
has been deleted.

---

## 7. Checklist for a new screen

- [ ] Uses `PoshanScaffold` or `PoshanSurface` (insets handled)
- [ ] Every clickable has a `.clip` above it, or uses a Poshan component (§1.1)
- [ ] Touch targets ≥ 48dp
- [ ] No text below 13sp
- [ ] Every user-facing string is a `stringResource`, present in `values/`, `values-hi/` and `values-te/`
- [ ] Status shown as colour **and** label
- [ ] Icons that convey meaning have a `contentDescription`; decorative ones pass `null`
- [ ] Haptics on interactive elements, not doubled with `PoshanCard`'s own
- [ ] Reads correctly at the system's largest font scale

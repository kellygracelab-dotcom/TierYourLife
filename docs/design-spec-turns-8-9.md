# TierYourLife — design spec extract (turns 8 and 9 snapshot)

Source: Claude Design, project `TierYourLife app UI designs`
(`50b81681-3405-4a68-8015-cf5817e72484`), file `TierYourLife.dc.html`, read in
full via MCP on **2026-08-04**. File size at the time of extraction — **261,623
characters** (matches the size recorded in previous sessions — i.e. the file
hasn't changed since last time; turns just kept accumulating and evicting
earlier ones).

## Why this document exists

The design file only holds on to the last few turns — adding a new turn
evicts the oldest ones, which then become unreadable via MCP. At the time of
extraction, the file had **only turns 8 and 9 left** (11 "options": 8a–8d,
9a–9g). Everything that used to be in turns 1–7 (typography, palette, the data
schema, tiers, adding movies, the list title, and so on) is already gone from
the file — references to them (`#2a`, `#4c`, `#5a`, `#5m`, `«turn 1 never
said…»`) only survive as broken anchors inside the text of turns 8–9.

This document is whatever could still be salvaged: the literal content of
turns 8 and 9, plus — separately, and clearly flagged — whatever made its way
into the code and still carries a comment naming its origin as an evicted
turn. Nothing has been invented or reconstructed from memory beyond what is
either literally present in `.dc.html`, or literally written in the current
repository code with an explicit source note.

The full list of what's no longer in the design file is in section
[9. Missing / evicted](#9-missing--evicted). A later addition, section
[10. Search sheet](#10-search-sheet-turn-11--read-live-2026-08-05), is not
part of the original turns-8–9 extraction — see its own note on that.

---

## 1. How to use this document

- Sections 2–4 (typography, narrow screens, palette) — what to build the
  visuals from: sizes, colours, spacing.
- Section 5 — for every option in turns 8 and 9: what it shows, what's
  already implemented in code as of the extraction date (verified by
  reading, not from memory), and what's flagged by the design as an open
  question.
- Section 6 — every interface string and content description, verbatim from
  the mock, ready to move into `strings.xml` without retyping from a
  screenshot.
- Section 7 — a consolidated list of what the design invented on its own
  (from the "What I had to invent" tables in 8a and 9a).
- Section 8 — open questions left for a decision.
- Section 9 — what's evicted, and the list to use when re-requesting it from
  the design if it's ever needed.
- Section 10 — the search sheet, read live from the design tool in a later
  session, after this document's own turns-8–9 snapshot had already gone
  stale on that exact point.

---

## 2. Typography

**There is no explicit "type role table" and no named roles
(`headlineSmall`, `bodyLarge`, etc.) anywhere in the design file itself** —
that naming and the table it presumably lived in were, by the looks of it, in
the evicted turn 1. Below are all the unique font declarations (`font: weight
size/line-height family`) that actually occur in the turn 8 and 9 mocks, with
a role determined from context of use (not invented — determined by which
element it sits on).

| Weight | Size | Line height | Font | Where it's used (verbatim from the mock) |
|---|---|---|---|---|
| 400 | 20px | 28px | Roboto | Top app bar title (`List settings`, `Sci-fi films`) |
| 400 | 24px | 32px | Roboto | Sheet title (`Edit tier`) |
| 500 | 24px | 28px | Roboto | Large tier letter in a row's colour band (`S`, `A`, …) |
| 500 | 20px | 24px | Roboto | Tier letter on the 40dp swatch in the move-to-tier sheet (`S`, `A`, `B`, `C`, `D`) |
| 500 | 14px | 20px | Roboto | Section header (`Display`) |
| 400 | 16px | 24px | Roboto | Input field text (e.g. the hex value `7A3F8C`) — matches what's already defined in code, `core/theme/Type.kt` → `Typography.bodyLarge` (16sp/24sp) |
| 400 | 16px | 22px | Roboto | List row title (`Wrapped rows`, a row's title in the move-to-tier sheet) |
| 400 | 13px | 18px | Roboto | Caption/supporting text under a row title (`Every item visible, rows grow taller`) |
| 500 | 13px | 18px | Roboto | Tab label (`Light` / `Dark` in the colour editor) |
| 500 | 13px | 16px | Roboto | Chip text (`Add`) |
| 400 | 12px | 16px | Roboto | Counter line under the title (`10 ranked · best to worst`); caption on a poster placeholder |
| 500 | 12px | 16px | Roboto | Tier letter in the 24dp badge of the flat ranked list (`S`, `A`, …) |
| 400 | 10px | 12px | Roboto | Tier caption under the large letter in the colour band (`Masterpiece`) |
| 500 | 10px | 12px | Roboto | `Remove` label under the trash icon while hovering during a drag |
| 400 | 14px | 20px | Roboto | Snackbar text (`Stalker moved to the trash`) |

Housekeeping entries (not part of the app's UI — they belong to the spec page
itself, set in monospace `ui-monospace,Menlo,monospace`): table column
headers (`FEATURE`), string-resource names in the left column
(`list_settings_title`). Don't carry these into the app — they're the design
document's own markup.

**Takeaway:** there is no canonical, named type scale anywhere in the
project — not in the design (turn 1 is evicted), and not in code either:
`core/theme/Type.kt` defines exactly one value (`Typography.bodyLarge =
16sp/24sp`, letterSpacing 0.5sp), and everything else in the presentation
layer is set per element (`fontSize=`/`lineHeight=` directly on `Text(...)`),
not through `MaterialTheme.typography.*`. This isn't a mistake by earlier
sessions — there was simply nothing to draw named roles from except reading
the mock element by element, which is exactly what happened.

---

## 3. Narrow screens and large system font

Taken verbatim from 8a ("The rule, and what I had to invent" → the Line
capacity / Width rows):

- **The tier tile (44×64dp)** doesn't change size depending on how many
  neighbours it has. 44dp is already the floor for a drag target.
- **The item area** is a `FlowRow`, 10dp padding on all sides, 8dp horizontal
  gap and 8dp vertical gap (the same in both directions).
- **Line capacity** isn't hardcoded — it's computed from the actual width:
  `contentWidth − band width − 20dp padding`, divided by `44 + 8dp` (rounded
  down to however many fit).
  - At 412dp: 5 or 6 tiles per line **depending on the width of the tier's
    own band** — a band at its 56dp minimum (short caption) leaves 312dp →
    6 tiles; a band at ~74dp (long caption) leaves 294dp → 5 tiles. Because
    of this, tiers in the same list with captions of different lengths don't
    line up on the same tile count per row — that's expected behaviour, not
    a bug.
  - At 320dp: 4 tiles per line (`320 − 24 − 56 − 20 = 220dp`).
  - The device-framed mocks (8c, 8d) lay out 5 per line rather than 6 —
    that's an artefact of the mock's bezel (it costs 16px of layout width),
    not of Android. The un-framed strips in 8b are the accurate ones.
- **Row height:** `10 + n×64 + (n−1)×8 + 10`, floored at 84dp. One line is
  84dp, two is 156dp, three is 228dp, four is 300dp. There is deliberately no
  cap on the number of lines (open question, see section 8).
- **The tier's colour band (the "tier letter" column):**
  - Stretches to the full row height (not a square, not a fixed height).
  - Top-aligned, 10dp from the top — **not vertically centred**. On a 1-line
    row this is visually indistinguishable from centring (which is why every
    earlier screen still holds up); on a 4-line row it keeps the letter next
    to the first item in the row and next to the row above it, so a column
    of tier letters stays scannable. Centring would drop the "B" 130dp down
    its own row.
  - Width: **wrap-content, minimum 56dp, maximum 33% of the row width**,
    caption in a single line with an ellipsis.
  - **The caption disappears entirely at a system font scale of ≥1.5×.**
    The large tier letter always stays.
- Between tier rows: 8dp (unchanged).

**Divergence from the current code** (see also the "Report" section of the
agent's message): `TierRow.kt` currently gives the colour band a **fixed**
`.width(66.dp)`, with no min/max-from-row-width logic and no response to
`fontScale` — the rule "the caption disappears at scale ≥1.5×" is not
implemented anywhere in `feature/tier/presentation` (checked: `fontScale`
doesn't appear anywhere in the module). The app does not currently follow
this part of the spec literally.

---

## 4. Palette

### 4.1 Material 3 theme roles

**No Material 3 role is named literally anywhere in the design file itself
(turns 8–9)** — only hex values in inline styles are used. The full, named
role table was, by the looks of it, in the evicted turn 2a — but its values
**survive verbatim in the code**, with the source explicitly cited:

> `core/theme/Color.kt`, line 5: «Values come from the project design system
> (screen 2a), named after Material 3 roles.»

The table below is transcribed from that file (not invented — transcribed
from code whose own source comment points at the evicted turn 2a):

| M3 role | Light | Dark |
|---|---|---|
| primary | `#4A5BAA` | `#BAC3FF` |
| onPrimary | `#FFFFFF` | `#1B2678` |
| primaryContainer | `#DEE0FF` | `#333E8F` |
| onPrimaryContainer | `#2C3A80` | `#DDE1FF` |
| surface | `#FBF8FF` | `#121318` |
| surfaceContainerLow | `#F3F0F9` | `#1F1F25` |
| surfaceContainer | `#EFEDF4` | `#26262D` |
| surfaceContainerHigh | `#F5F2FA` | `#2A2A31` |
| surfaceContainerHighest | `#E7E4ED` | `#35353C` |
| onSurface | `#1B1B21` | `#E4E1E9` |
| onSurfaceVariant | `#46464F` | `#C7C5D0` |
| outline | `#77767F` | `#91909A` |
| outlineVariant | `#E4E1E9` | `#46464F` |
| scrim | `#000000` @32% | `#000000` @60% |

Plus the app's non-M3 roles (also from `Color.kt`, same source):

| Role | Light | Dark |
|---|---|---|
| tilePlaceholder | `#DAD7E0` | `#35353C` |
| tilePlaceholderAlt | `#E7E4ED` | `#3E3E46` |
| tileLabel (text on a poster placeholder) | `#5D5C66` | `#A8A6B0` |
| unrankedRibbon | `#DAD7E0` | `#46464F` |
| onTierBand (text/letter on the tier's colour band) | `#FFFFFF` | `#201F26` |

The values in this table match exactly the hex codes actually found in the
turn 8–9 mocks (e.g. `#FBF8FF`/`#121318` — screen background,
`#46464F`/`#C7C5D0` — secondary text, `#4A5BAA`/`#BAC3FF` —
accent/selection) — i.e. code and design are in sync at this level, no
divergence found.

### 4.2 Tier colours

Two different sets — don't confuse them with each other, they're close but
not identical:

**A. Default tier colours** (created when a new list is created, S–D +
pool). Source — `feature/tier/data/.../DefaultTierColors.kt`; the
originating turn (presumably 1) is evicted, and the code carries no explicit
provenance comment, so treat these as confirmed by code, not by design:

| Tier | Light | Dark |
|---|---|---|
| S | `#B03A32` | `#F1948C` |
| A | `#C06A25` | `#E9A867` |
| B | `#A98B1F` | `#D8C05A` |
| C | `#3F7F55` | `#7FC393` |
| D | `#3C6E99` | `#86B8DE` |
| Pool (Unranked) | `#DAD7E0` | `#46464F` |

**B. The eight presets in the tier editor** (the add/edit-tier sheet —
swatches for picking a new tier's colour). Source — `TierColorPicker.kt`,
line 54: «The eight presets are literal from the mock» — and they do appear
literally in the turn 8–9 mocks (verifiable: the hex values match 1-to-1 with
`colors.txt`, extracted from the `.dc.html`):

| # | Light | Dark |
|---|---|---|
| 1 | `#B03A32` | `#F1948C` |
| 2 | `#C06A25` | `#E9A867` |
| 3 | `#A98B1F` | `#D8C05A` |
| 4 | `#3F7F55` | `#8FD3A3` |
| 5 | `#3C6E99` | `#8FC3E8` |
| 6 | `#6B4E9E` | `#C9A9F0` |
| 7 | `#2F7D7D` | `#7ED4D4` |
| 8 | `#A63A66` | `#F09BB9` |

The ninth swatch isn't a colour but a control: a conic rainbow ring icon
with a `tune` glyph in a surface-coloured centre — it reads as a control
rather than a colour — and opens the custom picker (HSL sliders + hex, see
9e below).

**Noticed:** preset #4 (green) in the editor (`#3F7F55`/`#8FD3A3`) differs
from tier C's default colour (`#3F7F55`/`#7FC393`) — the light variant
matches, the dark one doesn't. This isn't necessarily a bug (two different
sets, designed at different times), but it's worth keeping in mind for
future colour work.

The default value for the custom colour (the first time the picker opens,
before any slider is touched) is also «literally from the mock» per the
code comment (`TierColorPicker.kt`, «Mock's own example custom color»):
light `#7A3F8C`, dark `#D9A2E6`.

### 4.3 Row fill rule

Taken verbatim from the code (`common/TierRowColors.kt`, comment on the
constant):

> «Design-system rule: a row fill is the band color at 12% over the
> surface.»

In other words, a tier row's body fill isn't a separate hardcoded colour —
it's the tier's own band colour composited over the surface at 12% alpha
(`ROW_TINT_ALPHA = 0.12f`). Separately there's `ROW_HOVER_TINT_ALPHA =
0.24f` — the fill used when a tile is hovering over the row during a drag
(matches, in spirit, the row highlight in the 8d mock: `outline:1px solid
#B03A3252` — the `52` at the end of the hex is an alpha channel of ≈32% —
plus a separate `#F7E4E2` fill that reads as noticeable but not a solid,
block-style background).

---

## 5. Screens and states

For each option: what the mock literally shows, and its status in the
current code (verified by reading the code as of the extraction date, not
from memory of past sessions).

### Turn 8 — «Tier rows wrap and grow downward — every item in a tier visible without scrolling sideways»

- **8a — the rule, and what had to be invented.** Geometry (see section 3)
  plus the "What I had to invent" list: an 8dp vertical gap (no multi-line
  row ever existed before — invented by analogy with the horizontal gap);
  top-aligning the band instead of centring it — the single most debatable
  decision here; the screen is now scrollable as a whole (it used to be a
  fixed five-row column with each row scrolling sideways on its own); drag
  auto-scroll within 72dp of the top/bottom at up to 600dp/s; **no cap on
  row height was deliberately set** — open question, see section 8;
  insertion order across multiple lines is reading order (left to right,
  then down), not just left to right.
  **Status:** wrap mode is implemented (`TierRow.kt`, `FlowRow`); drag
  auto-scroll and the exact band-width logic were not checked separately
  beyond section 3 (which already records the band-width divergence).
- **8b — a tier holding 3, 8, and 20 items at 412dp.** A purely visual row
  (S=3, A=8, B=4 — not 20 as the option's title says; the 20-item case was
  presumably shown off-screen/didn't make it into the extract). Tiles are
  44×64 throughout.
- **8c — in context, the editor with rows of different heights.** Same as
  above, but inside the device bezel (5 per line — a bezel artefact, see
  section 3), with the pool docked at the bottom.
- **8d — dragging over a row that is now an area.** The target row is
  highlighted with a `#F7E4E2` fill and a `outline:1px solid #B03A3252`
  outline across its **whole** area (not just a thin strip, as it would be
  in the single-line version); at the potential drop position in the pool
  sits a 52×76 placeholder tile with a dashed outline, `border:2px dashed
  rgba(74,91,170,.55)`.
  **Status:** the row-hover highlight exists in code (`isHovered →
  dashedBorder`, `ROW_HOVER_TINT_ALPHA`); this wasn't specifically checked
  against the dashed placeholder in the pool.

### Turn 9 — «Per-list display mode and a ranked list · arbitrary tier colours · move without dragging · a trash target mid-drag»

- **9a — strings, content descriptions, and the schema gaps.** At the time
  of this turn, the "Data the app does not currently hold" table listed as
  missing: `displayMode` on the list (enum WRAPPED/STRIP/RANKED, defaulting
  to WRAPPED), item order within a tier for the flat list (an item's
  position within its tier — «turn 1 never said either way», so the schema
  needed checking beforehand), and marked as **not** requiring new data:
  custom colour (the tier already has two hex fields), double-tap (writes
  the same thing a drag writes), drag-to-trash (the same soft-delete a long
  press already performs).
  **Status:** both items marked "Yes" (new data needed) exist in the schema
  now — `displayMode` on `TierListEntity`, an item's position within its
  tier on `TierItemEntity.position`. The gap noted in turn 9a is closed in
  the current code. The full strings/content-description tables from this
  option are in section 6.
- **9b — the list settings screen, the three-way display choice.** An M3
  radio list, 20dp circles, the selected row filled with `#EDEBFA`/`#2E2F45`,
  a 24dp mode icon on the left (`grid_view` / `view_carousel` /
  `format_list_numbered`), a caption under the section header spelling out
  the scope in words ("Other lists are unaffected"). Below that, `Rename
  list` and `New tier` as ordinary rows with a chevron; `more_vert` now
  opens this screen directly instead of the old menu.
  **Status:** implemented (`TierListSettingsScreen.kt`) — the `Rename list`
  row is still present both in code and in the mock (removing it is the
  subject of a separate, not-yet-completed task about editing the title in
  the header — see the aside below).
- **9c — the ranked list, 10 items across three tiers.** A row: rank number
  (28dp, right-aligned, tabular figures), a 32×48 poster, a single-line
  title with an ellipsis, a 24dp/6dp-radius tier badge on the right in the
  band colour. The screen title is a count ("10 ranked · best to worst")
  instead of tier rows. The pool is a collapsed 60dp bar at the bottom with
  a counter and an `Add` chip.
  **Status:** implemented (`RankedList.kt`, including the expandable pool —
  added later; the mock never drew that state, see the previous report).
- **9d — the ranked list, long titles wrap to two lines.** Two lines
  maximum, then an ellipsis; the row grows to 86dp, and the poster and
  badge stay vertically centred. **The only place in the app where a title
  wraps rather than truncating.**
  **Status:** implemented (`RankedItemRow`, `maxLines = 2`).
- **9e — arbitrary colour: presets plus a picker for both themes.** See
  section 4.2 (eight presets + a ninth control swatch). Light/Dark tabs
  with a 24dp preview circle of the current value; Hue/Saturation/Lightness
  sliders + a hex field; a contrast indicator for the caption against the
  band (advisory, never blocks Save). The live row previews at the bottom
  of the sheet sit in the same place they did in the evicted turn 5m.
  **Status:** implemented (`TierEditorSheet.kt`, `TierColorPicker.kt`,
  `ColorMath.kt` — the hue slider, the contrast indicator, the presets).
- **9f — double-tap a poster: a chooser instead of a drag.** Double-tap (not
  a long press — so it can't be confused with a drag). The sheet names the
  poster it's acting on (a 32×48 preview + title), and its own title is
  "Move to a tier". Every tier appears as a 40dp swatch in its band colour
  plus its label; the current tier is highlighted, ticked, and labelled
  "Currently here" (not hidden or disabled — the list of destinations is
  always the same length in the same order). Below a divider, two non-tier
  actions: "Remove from list" (error-container, a round icon) and "Back to
  the pool" (a neutral circle, `south` glyph). The distinction is visual:
  square swatches are destinations, round icons are actions.
  **Status:** implemented (`MoveItemSheet.kt`), confirmed by a literal
  match between the strings ("Move to a tier", "Currently here", "Back to
  the pool"/"Unranked, stays in the list", "Remove from list"/"Goes to the
  trash") and the existing tests.
- **9g — drag to the trash: resting, hovering, and after the drop.** Three
  states:
  1. *Resting* — the target fades in at the bottom-right the moment a tile
     lifts, in the FAB's position and size (56dp, 16dp radius,
     error-container fill, `delete_outline` glyph in on-error-container).
     It sits above the pool sheet, 16dp apart from it.
  2. *Hovering* — grows to 72dp/24dp radius, fills with the solid error
     colour, the glyph switches to the filled `delete`, and the word
     "Remove" appears under it. Elevation goes to 8dp, and one haptic tick
     fires on entry.
  3. *After the drop* — the tile disappears, the row/pool closes its gap,
     and the snackbar appears at 88dp: "%1$s moved to the trash" + Undo.
     The same soft delete as a long-press removal. A second removal while
     the first snackbar is still showing **replaces** the message
     immediately (it isn't queued) — a deliberate decision, matching the
     behaviour from turn 3.
  **Status:** implemented (`TRASH_TARGET`, `DELETED_ITEM_SNACKBAR`,
  `dragTileIntoTrash`; the snackbar-replacement behaviour is confirmed by
  the `rapidSuccessiveDeletions_showOnlyLatestMessage_notQueued` test).

**Aside — why this session's original task fell through.** The task
referenced «turn 5: editing the list title in the header» — that turn no
longer exists in the file (it existed at least up through this session; the
`#5a`/`#5m` reference shows up in 9a/9e as a broken anchor). This loss is
exactly what prompted this extraction. The `Rename list` row in 9b is part
of the **old** path (through list settings) that the abandoned task was
meant to replace with editing directly in the header; the new screen/state
itself isn't described anywhere in the remaining turns 8–9.

---

## 6. Interface text and content descriptions

Verbatim from option 9a ("Display mode — strings", "Ranked list — strings",
"Colour picker — strings", "Move chooser — strings", "Drag to trash —
strings", "Content descriptions"). Format: `key` → value(s).

### Strings

| Key | Value |
|---|---|
| `list_settings_title` | “List settings” |
| `display_section` | “Display” |
| `display_section_caption` | “How tiers are shown in this list. Other lists are unaffected.” |
| `mode_wrapped` / `_sub` | “Wrapped rows” · “Every item visible, rows grow taller” |
| `mode_strip` / `_sub` | “Single strip” · “One line per tier, scrolls sideways” |
| `mode_ranked` / `_sub` | “Ranked list” · “One column, best to worst, no tiers” |
| `list_tier_count` (plural) | one: “1 tier” · other: “%1$d tiers” |
| `ranked_header` (plural) | one: “1 ranked · best to worst” · other: “%1$d ranked · best to worst” |
| `ranked_header_empty` | “Nothing ranked yet” — shown when every item is still in the pool |
| `pool_collapsed` (plural) | one: “Pool · 1 unranked” · other: “Pool · %1$d unranked” |
| `colour_custom` | “Custom · set the light and dark colour separately” |
| `picker_light` / `_dark` | “Light” · “Dark” |
| `picker_hue` / `_sat` / `_light` | “Hue” · “Saturation” · “Lightness” |
| `picker_hex` | “Hex” |
| `picker_contrast` | “%1$s:1” — the contrast of the caption against the band; advisory, never blocking |
| `chooser_subtitle` | “Move to a tier” |
| `chooser_current` | “Currently here” |
| `chooser_no_caption` | “No caption” — when a destination tier has no caption; the label is already on the swatch |
| `action_remove_from_list` | “Remove from list” · sub “Goes to the trash” |
| `action_back_to_pool` | “Back to the pool” · sub “Unranked, stays in the list” |
| `drag_trash_label` | “Remove” — appears only while hovering during a drag |
| `snack_item_trashed` | “%1$s moved to the trash” · action “Undo” |

### Content description

| Key | Value |
|---|---|
| `cd_more` | “More options” — now opens List settings |
| `cd_mode_option` | Radio buttons carry their visible label; the group is “How tiers are shown” |
| `cd_ranked_row` | “Rank %1$d, %2$s, tier %3$s.” → «Rank 3, Blade Runner 2049, tier A.» |
| `cd_pool_collapsed` | “Pool, %1$s. Expand.” |
| `cd_colour_swatch` | “%1$s. Light and dark version.” |
| `cd_colour_custom` | “Custom colour. Opens a picker for light and dark.” |
| `cd_picker_tab` | “Editing the light colour” / “Editing the dark colour” |
| `cd_picker_slider` | Sliders are real sliders: “Hue”, “Saturation”, “Lightness”, each announcing its value 0–360 or 0–100 |
| `cd_chooser_dest` | “Move to tier %1$s, %2$s.” · current tier: “Tier %1$s, %2$s. Currently here.” |
| `cd_drag_trash` | “Trash. Drop here to remove.” — a live region announcing “Over the trash” on hover |

### Other literal text from the mocks (not in 9a's tables, but visible on the screens themselves)

- The tier editor screen's title: “Edit tier”.
- The demo list's name across every mock: “Sci-fi films”.
- Icons (`material symbols` glyph names, verbatim from the markup):
  `arrow_back`, `note_add`, `more_vert`, `add`, `expand_less`, `edit`,
  `chevron_right`, `grid_view`, `view_carousel`, `format_list_numbered`,
  `check`, `tune`, `contrast`, `delete_outline`, `delete`, `south`.

---

## 7. What the design invented on its own

Compiled from the "What I had to invent" tables in options 8a and 9a —
verbatim, one item per line:

**From 8a (wrapping tier rows):**
- An 8dp vertical gap between lines inside a multi-line tier — no
  multi-line tier ever existed before, so nothing specified a line spacing.
- Top-aligning the band instead of centring it — the single most debatable
  decision here: centring looks better in isolation and worse in a column.
- The screen became scrollable as a whole (a consequence, not a decision) —
  the pool sheet is now permanently docked over a scrolling list, rather
  than sitting under a fixed column.
- Drag auto-scroll (72dp from the edge, up to 600dp/s) — an unavoidable
  consequence of the list now being taller than the screen.
- No cap on row height — a 60-item tier is 12 lines and 780dp. Explicitly
  flagged as an open question (section 8).
- Insertion order across multiple lines is reading order, not just
  left-to-right — it was never ambiguous before.

**From 9a:**
- A dedicated list settings screen — the overflow menu used to hold Rename
  list and New tier as direct items; a three-way display choice is too big
  for a menu item, so the menu was replaced by a screen entirely.
- The rank number column's width — 28dp, right-aligned, tabular figures, so
  1/10/100 line up in the same column.
- A 32×48 poster in the flat list — a third tile size (besides the tier's
  44×64 and the pool's 52×76); a 64dp row can't fit either of the existing
  sizes.
- A two-line title in the flat list — the only place in the app where a
  title wraps rather than truncating.
- The collapsed pool — a 60dp bar with a counter and an Add chip; the pool
  never had a collapsed state before.
- The contrast indicator — not requested; it exists only because arbitrary
  colour is now allowed, and without it nothing in the app would warn about
  unreadable white text on a pale band. Advisory, never blocks Save.
- HSL sliders instead of a 2D field — three labelled sliders are
  announceable by TalkBack and reachable by keyboard; a 2D gradient field
  is neither.
- "Back to the pool" in the chooser sheet — not a destination and not
  "Remove", but a third thing a poster can do, and dragging already
  supports it — without this item the sheet would be a strictly weaker path
  than dragging.
- The trash target's position — bottom-right, over the pool sheet, in the
  same spot the FAB sits on Home. It only exists during a drag, so it never
  competes with anything.
- The two-snackbar sequence — a second removal while the first is still
  showing replaces the message immediately rather than queuing it (M3
  itself supports a queue, but then the Undo you're reaching for would
  belong to a removal two actions ago).

---

## 8. Open design questions

Literally flagged in the mock as decisions left open ("say the word",
explicit forks):

- **A cap on tier row height.** Currently — no limit (a 60-item tier =
  780dp). The design says outright: «Say the word if you want a cap» — a
  decision on capping it was explicitly not made and left for later.
- **Top-aligning the tier band vs. centring it** is called the single most
  debatable decision in 8a — not an open question in the sense of
  "undecided", but flagged as a decision worth revisiting if objections
  come up.

---

## 9. Missing / evicted

A list of what was no longer in `TierYourLife.dc.html` at the time of
extraction (2026-08-04, 261,623 characters), even though — judging by the
references inside turns 8–9 themselves — it used to exist. Everything on
this list, if it's ever needed, has to be **re-requested from the design**,
not guessed at:

- **Turn 5 entirely** — editing the list title in the screen header
  (rest/edit states, cursor position, empty-name behaviour). This is
  exactly what derailed this session's original task and is why this
  document exists. **Stale as of 2026-08-05 — see the note at the top of
  section 10.** Turn 5 was evicted on 2026-08-04; it wasn't gone a session
  later. The window's contents move in both directions, not just forward.
- **Turn 1** — judging by a line in 9a: «If the tier row is currently
  backed by an unordered set — and turn 1 never said either way» — turn 1
  apparently set the original schema/basic rules of the tier-list screen.
  It probably also held the original type scale (section 2) and the
  original default tier colours (section 4.2, table A) — there's no direct
  proof, only that neither table is redefined from scratch anywhere in the
  remaining turns, and the code treats them as a given.
- **Turn 2a** — the Material 3 theme palette (section 4.1). Proof — the
  comment in `core/theme/Color.kt`: «Values come from the project design
  system (screen 2a)». The values themselves survived in the code; the
  turn itself didn't.
- **Turn 3** — the delete/trash behaviour, which 9g refers to directly
  («matches the deletion flow in turn 3»). The mechanics themselves (soft
  delete, snackbar, replace instead of queue) are described again in 9g for
  drag-to-trash, but where "the same thing" originally came from is the
  evicted turn 3.
- **Turn 4c** — the width/behaviour rule for the tier band on narrow
  screens and at large font sizes, which 8a refers to directly («Unchanged
  from 4c»). Section 3 of this document is reconstructed from 8a's quote,
  not from turn 4c itself — and it's also where it's recorded that the
  code doesn't currently follow this rule.
- **Turn 5a** — the original storage of two hex values (light/dark) on a
  tier, referenced by both 9a and this document's "tier colours" section
  (`«The tier already stores two hex values (5a)»`). The schema itself
  survived in code (`TierEntity.colorLight`/`colorDark`); the turn itself
  didn't.
- **Turn 5m** — the original eight colour presets plus the live row
  previews at the bottom of the editor sheet, which 9e references twice
  («The eight presets from 5m are untouched», «The live row previews from
  5m stay exactly where they were»). The eight presets themselves survive
  verbatim in code (`TierColorPicker.kt`, section 4.2 table B) with an
  explicit «literal from the mock» note — but turn 5m itself can no longer
  be read.
- **Official type-role names** (`headlineSmall`, `bodyLarge`, and so on —
  if the design ever had them under those names at all) — section 2 is
  assembled from actual usage, not from an official role registry.

---

## 10. Search sheet (turn 11 — read live, 2026-08-05)

The task that produced the rest of this document assumed sections 11a and
11d already existed in *this* file. They didn't — the rest of this document
is a static snapshot of turns 8–9 taken on 2026-08-04; turn 11 didn't exist
yet at that time. Turn 11 exists only in the live, continuously-evicting
design file itself (`TierYourLife.dc.html`, same project). This section was
read from there directly, live, via the `DesignSync` tool, on **2026-08-05**
— not extracted into the turns-8–9 sweep above.

While there, one claim in section 9 above turned out to be stale: it says
*"Turn 5 entirely"* was evicted as of 2026-08-04. It wasn't, as of
2026-08-05 — turn 5 (options 5a, 5g, 5i, 5k, 5m, 5n, 5o) was present, sitting
alongside turns 9, 10 and 11 (turns 6–8 were the ones evicted by then). The
window's contents change in both directions between sessions — new turns
evict old ones, but which old ones, and whether a turn "gone" today is really
gone, isn't something one extraction can answer for a later one. Re-check
live rather than trusting a prior session's "missing" list at face value.

Read live via `DesignSync` (`get_file`) from project `TierYourLife app UI
designs` (`50b81681-3405-4a68-8015-cf5817e72484`), file
`TierYourLife.dc.html` — 260,778 characters at read time. The tool's own
response is capped at 256 KiB; the file exceeds that, so its content arrives
as a saved local file rather than inline — read in full from there, not
truncated. Options read: **11a** ("The sheet, field, rows and action bar —
implementable from this text alone"), **11b** ("Searching, nothing found,
and a failed request"), **11c** ("The copy change — 'films' to items, and
which strings move"), **11d** ("Every value, measured off the mock — the
numbers to build against"). All four describe the same screen from different
angles — 11a is prose and rationale, 11d is the same content as hard numbers
("nothing is redesigned... where 11a was vague, this is the value"), 11b
covers the three states 5i's mockup never drew, 11c is the string diff. The
drawn mockup itself is **5i**, confirmed present and unchanged; 11d is this
document's transcription of its measurements, not a redrawing.

### 10.1 Sheet container

| Part | Light | Dark |
|---|---|---|
| Top edge | 76dp below the screen top; fills to the bottom | Same |
| Corners | 28dp top corners only, 0 at the bottom | Same |
| Fill | `#F5F2FA` (= `surfaceContainerHigh`) | `#2A2A31` (= `surfaceContainerHigh`) |
| Elevation | `0 −8px 32px rgba(0,0,0,.28)` | Same |
| Scrim | `rgba(0,0,0,.32)` | Same |
| Own padding | **0.** The sheet has no horizontal padding of its own — every child sets its own inset, which is why the selected-row tint can reach the edge | Same |
| Drag handle | 32×4dp, 2dp radius, centred, 6dp from the top, 8dp gap below. `#C7C5D0` | `#46464F` |

### 10.2 Search field

A filled, fully-rounded field — **not outlined**.

| Part | Light | Dark |
|---|---|---|
| Height | 56dp | Same |
| Corner radius | 28dp — fully rounded | Same |
| Container fill | `#EFEDF4` (no single matching M3 role — light half matches `surfaceContainer`, dark half matches `surfaceContainerHigh`'s dark; treat as a mock-literal pair, same as the move-chooser's current-tier tint) | `#2A2A31` |
| Margin around it | 12dp left/right, 0 above (the drag handle's 8dp gap serves), 8dp below | Same |
| Leading control | 48×48dp icon button, 24dp `close` glyph, 4dp from the field's left edge. On-surface-variant `#46464F`. CD "Close search". Dismisses the whole sheet | `#C7C5D0` |
| Query text | bodyLarge 16/24, on-surface `#1B1B21`, 12dp from the leading button. Caret primary `#4A5BAA` | `#E4E1E9`; caret `#BAC3FF` |
| Placeholder | "Search TMDB" (`"Search %1$s"`, source name parameterised), outline colour `#77767F` | `#91909A` |
| Trailing control | 48×48dp icon button, 24dp `cancel` glyph, **present only when the query is non-empty**. Same on-surface-variant colour. CD "Clear search". Clears the text and returns to the empty state — it does not close the sheet | `#C7C5D0` |
| Triggering a search | **Debounced, 300ms after the last keystroke, minimum 2 characters. No submit button.** The keyboard's enter key is not required — pressing it just re-runs the current query immediately, bypassing the debounce. Each new query replaces the result list; **selections already made are kept** — search "villeneuve", pick two, search "kubrick", pick two more, confirm all four at once. This is why the selected count lives outside the list, not inside it. | — |

### 10.3 Caption line

Always present — including in the loading, empty and error states — because it states what the sheet is doing, not what it currently shows.

- Copy: `"Results from %1$s · adding to %2$s pool"` → "Results from TMDB · adding to Sci-fi films pool". Both the source name and the list name are parameters.
- Type: labelMedium 12/16, `#46464F` / `#C7C5D0`, with the **list name run in primary** (`#4A5BAA` / `#BAC3FF`) — the only coloured run in the sheet.
- Spacing: 4dp under the field (12dp of clear space total, counting the field's own 8dp bottom margin), 8dp to the first result row, 20dp side padding — 4dp wider than a result row's 16dp, deliberately, so it reads as a caption and not as a row.

### 10.4 Result row

| Part | Value |
|---|---|
| Total height | **84dp = 10dp top + 64dp poster + 10dp bottom** |
| Horizontal padding | 16dp both sides |
| Poster | **44×64dp**, 6dp radius, `flex: none`. Missing artwork falls back to the title, centred, ellipsised, up to 2 lines |
| Gap poster → text | 16dp |
| Title | bodyLarge 16/22, on-surface. 1 line, ellipsis |
| Secondary line | bodySmall 13/18, on-surface-variant, directly under the title. Holds **the release year, and the original title when it differs from the displayed one** — "2024" or "2010 · Des hommes et des dieux". Omitted entirely when there's no year and no differing original title (title then centres against the poster) |
| Divider between rows | **None** — poster edges carry the rhythm |

**Director dropped, corrected in 11a/11c:** the mockup (5i) shows "2024 · Denis Villeneuve", but a TMDB *search* response carries no crew — the director needs a `/credits` request per result, which means N extra calls on a screen whose entire premise is results appearing 300ms after the user stops typing. The secondary line now holds only what one search response already contains: year, and `original_title` when it disambiguates a remake or a same-year same-title collision. This matches the decision already made earlier in this session, from the other direction (found by reading the mapper, not the mock) — the mock and the code now agree for the same reason.

### 10.5 Selection control

| State | Light | Dark |
|---|---|---|
| What it is | An M3 checkbox — a **square**, not a radio, not a bare trailing check glyph | |
| Size | **24×24dp, 4dp corner radius** | Same |
| Position | 16dp from the row's trailing edge — the row's own padding, not a further inset | Same |
| Unselected | 2dp border, outline colour, no fill | `#77767F` / `#91909A` |
| Selected | Filled primary, no border, 18dp `check` glyph in on-primary | `#4A5BAA`/`#FFFFFF` · `#BAC3FF`/`#201F26` |
| Hit target | The whole 84dp row toggles. The checkbox is not separately tappable | |

### 10.6 A selected row, as a whole

- Fill: **`#EDEBFA` light / `#2E2F45` dark** — the same tint already used for the current tier in the move-to-tier chooser (`MoveItemSheet.kt`) and for Home's selection mode.
- Shape: **full width, edge to edge, no inset, no corner radius.** It is the row container's own background, spanning the sheet's whole width — it runs *under* the row's 16dp padding and touches both edges. Not a rounded pill or card sitting inside the row.
- Why: a rounded inset shape would read as a card in a list of non-cards, and would fight the poster's own 6dp radius 16dp away.
- Everything else in the row (title, secondary line, poster) keeps its colour — only the background and the checkbox differ. Both signals matter: the tint alone would read as "highlighted"; the *empty* checkbox on every other row is what makes the list read as a set of checkboxes rather than one oddly-tinted row.

### 10.7 Bottom action bar

| Part | Light | Dark |
|---|---|---|
| Height | **64dp = 12dp + 40dp button + 12dp** | Same |
| Background | `#EFEDF4` — same fill as the search field, so field and bar bracket the list | `#2A2A31` |
| Divider vs. elevation | **A 1dp top divider, no elevation, no shadow.** `#E4E1E9` (= `outlineVariant`) | `#46464F` (= `outlineVariant`) |
| Padding | 12dp vertical, 16dp horizontal | Same |
| Left: count | bodyMedium 14/20, on-surface-variant. "3 selected"; **"Nothing selected"** (not "0 selected") when empty | |
| Right: button | Filled, 40dp tall, 100dp radius, 24dp horizontal padding, primary fill, labelLarge 14/20 on-primary | |
| Button label, with a selection | Plural-aware: "Add 1 item" / "Add %1$d items" | |
| Button label, empty selection | Plain **"Add"**, and the button is **disabled** — on-surface at 12% fill, on-surface at 38% label (M3's standard disabled treatment). Same geometry, same position — not hidden, so the target stays where the thumb expects it | |
| Presence | **Always**, from the moment the sheet opens — loading, empty and failed states included. It does not slide in with the first selection, which would change the list's height under a finger that just tapped something | |

**Pinned above the keyboard, and why the button was disappearing:** the bar sits directly on top of the IME and moves with it; it is opaque and does **not** overlay the list — the scrollable region ends exactly where the bar begins, so the last row is never covered and needs no bottom clearance. **This is the one screen in the app that does not use the standard 88dp snackbar-clearance pattern for its bottom content padding — the padding here is 0**, because the bar is real layout, not floating chrome sitting on top of the list.

### 10.8 Confirming and dismissing

- Confirming closes the sheet immediately; every selected item is created in the list's pool, unranked, in selection order. A snackbar on the tier-list screen underneath reports the count ("3 items added to the pool", plural-aware, standard 88dp placement) with **no Undo** — the items are visibly in the pool and removing one is a double-tap away.
- `close`, the scrim, a downward drag, or system back all dismiss without adding anything; **selections are discarded**, there is no draft to return to.

### 10.9 Loading, empty and error (11b — never drawn in 5i, new)

The field, caption and action bar are present and unchanged in all three; only the list area between them swaps.

- **While searching:** a 4dp indeterminate linear progress indicator, full sheet width, pinned directly under the field, above the caption line. Primary track on a primary-container trace.
- **Nothing found:** title `"Nothing found for "%1$s""`, body `"Check the spelling, or add it by hand instead."`.
- **Request failed:** title `"Couldn't reach %1$s"`, body `"Check your connection and try again."`.
- Below 2 characters: `"Type at least two letters to search."`.

### 10.10 Copy — "films" become "items" (11c)

The app isn't a films app; TMDB is one source of several planned. Every user-facing string drops "film"; the word survives only as the source's own proper noun.

| String | Was | Now |
|---|---|---|
| Add button | "Add 1 film" / "Add %1$d films" | "Add 1 item" / "Add %1$d items" |
| Added snackbar | "1 film added to the pool" / "%1$d films added to the pool" | "1 item added to the pool" / "%1$d items added to the pool" |
| Caption | "Results from TMDB · adding to %1$s pool" | "Results from %1$s · adding to %2$s pool" — source becomes a parameter |
| Field hint | *(never specified)* | "Search TMDB" → "Search %1$s" |
| Empty title | *(sheet had no empty state)* | "Nothing found for "%1$s"" |
| Error title | *(sheet had no error state)* | "Couldn't reach %1$s" |
| Result secondary line | "%1$s · %2$s" — year and director | "%1$s" — year alone, or "%1$s · %2$s" with the original title when it differs. No crew. |

Unchanged, and why: `"%1$d selected"` / `"Nothing selected"` never named a type; `"Add"` (disabled label), `"Try again"`, `"Add by hand"`, `"Type at least two letters to search."`, `"Check the spelling, or add it by hand instead."`, `"Check your connection and try again."`, and the two selection content descriptions (`"%1$s, selected. Tap to remove."` / `"%1$s, not selected. Tap to add."`) are all already generic.

**Status:** implemented in this session — sheet layout, search field, caption line, result row, selection control, selected-row tint, bottom bar and its keyboard pinning, and the "films" → "items" copy pass. **Not implemented:** the "Add by hand" action in the empty state (opens a manual-entry flow that does not exist yet — out of scope, same reason it was excluded from the multi-select task) and the two result-selection content descriptions (`cd_result_selected`/`cd_result_unselected`) — the row's selection semantics come from a plain toggle, not from per-row content-description text, and adding one without the other would be half a fix; noted as open rather than done partially. The "Try again" action **was** wired, since it costs nothing beyond calling the search that already exists.

---

*End of document. Compiled without touching any code — see the repository
diff, only the `docs/` directory is affected.*

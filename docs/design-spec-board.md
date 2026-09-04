# TierYourLife — design spec: the board

The tier list screen: rows, colours, the three ways a board can be displayed, moving a card
without dragging it, dragging one to the trash, and the catalogue search sheet.

Condensed from the design conversation of August 2026. The verbatim transcripts are not in
the repository; what is here is each decision and the reason it was made, and where the
built app diverged from the design, it says so and why. Companion to
[`design-spec-home.md`](design-spec-home.md), which covers everything the board is reached
from. Baseline width 412dp; dimensions in dp unless marked sp.

---

## 1. Tier rows wrap and grow downward

Every item in a tier is visible without scrolling sideways. The screen scrolls as a whole,
and the pool sheet is docked over it.

| Rule | Value |
|---|---|
| Tile | 44×64dp, never resized by how many neighbours it has — 44dp is already the floor for a drag target |
| Item area | `FlowRow`, 10dp padding on every side, 8dp gap in both directions |
| Tiles per line | Computed from width, not hardcoded: `(content width − band width − 20dp) ÷ 52dp`, rounded down. At 412dp that is 5 or 6 depending on the tier's own band width — tiers with captions of different lengths do not line up on the same count, and that is expected |
| Row height | `10 + n×64 + (n−1)×8 + 10`, floored at 84dp. No cap — a 60-item tier is 12 lines and 780dp. Left open on purpose; see §7 |
| The band | Stretches to the full row height and is **top-aligned, 10dp from the top, not centred**. On a one-line row the two are indistinguishable; on a four-line row centring would drop the letter 130dp into its own row and break the column of tier letters a reader scans down. The single most debatable decision here |
| Band width | Wrap-content, min 56dp, max 33% of the row; caption on one line with an ellipsis. Disappears at a system font scale of 1.5× or more; the letter always stays |
| Insertion order across lines | Reading order — left to right, then down. It was never ambiguous before rows could wrap |
| Drag auto-scroll | Within 72dp of the top or bottom edge, up to 600dp/s — unavoidable once the list is taller than the screen |
| Dragging over a row | The whole row highlights, not a strip: the band colour at 24% over the surface, plus a 1dp outline |

**Built differently, then reconciled.** The first build gave the band a fixed 66dp width and
ignored font scale. The rule that later replaced it — one band width per board, measured from
the longest caption, so a caption never moves the column of tier letters — lives in `TierRow`,
and the tier editor's preview takes the same width from outside rather than promising a size
that changes on save.

## 2. Colour

The Material 3 roles live in `core/theme/Color.kt`, named after the roles and with their
origin in the design system noted on the file. Three rules matter beyond the palette:

- **A row's fill is its band colour at 12% over the surface** (`ROW_TINT_ALPHA`), never a
  separate colour. Hovering during a drag raises that to 24%.
- **Every tier carries two colours, light and dark, set separately.** A band readable on white
  is a glowing slab at night. The defaults for S–D and the pool live in `DefaultTierColors`;
  the eight presets in the tier editor are their own set and are not the defaults — preset 4's
  dark green differs from tier C's, on purpose or not, and nothing depends on their agreeing.
- **The ninth swatch is a control, not a colour**: a rainbow ring that opens the custom picker.

The custom picker is three labelled HSL sliders and a hex field, not a two-dimensional
gradient — three sliders are announceable by TalkBack and reachable by keyboard, a gradient
field is neither. A contrast figure for the caption against the band is shown and is
advisory: it never blocks Save.

## 3. Three ways to show a board

A per-list choice, on a settings screen reached from the overflow rather than in the overflow
itself — a three-way choice is too big for a menu item, so the menu became a screen.

| Mode | What it is | What it needed |
|---|---|---|
| Wrapped rows | §1. Every item visible, rows grow taller | Nothing new |
| Single strip | One line per tier, scrolls sideways | Nothing new |
| Ranked list | One column, best to worst, no tiers: rank number (28dp, right-aligned, tabular figures so 1/10/100 line up), a 32×48 poster — a third tile size, because a 64dp row fits neither of the others — a title that may wrap to two lines (the only place in the app a title wraps rather than truncating), and a tier badge in the band colour | A position for each item within its tier, and a display mode on the list |

The pool under a ranked list is a collapsed 60dp bar with a count and an Add chip; it had
never had a collapsed state before.

## 4. Moving a card without dragging it

Double-tap a poster — not a long press, which would be confused with the start of a drag —
and a sheet names the poster it is acting on and lists every tier as a 40dp swatch in its band
colour. The current tier is **shown, ticked and labelled "Currently here"** rather than hidden
or disabled: the list of destinations is always the same length in the same order, so it can
be learned.

Below a divider, two things a poster can do that are not tiers: "Remove from list" and "Back
to the pool". Square swatches are destinations, round icons are actions. "Back to the pool"
exists because dragging can already do it — without it the sheet would be a strictly weaker
path than dragging.

## 5. Dragging a card to the trash

The trash target exists only during a drag, at the bottom-right where the corner button sits
on Home, so it never competes with anything.

| State | What is drawn |
|---|---|
| Resting | Fades in the moment a tile lifts: 56dp, 16dp radius, error-container fill, outlined bin |
| Hovering | Grows to 72dp, fills solid error, the bin fills, the word "Remove" appears under it, one haptic tick on entry |
| After the drop | The tile is gone, the row closes its gap, and a snackbar offers Undo. The same soft delete as a long press |

A second removal while the first snackbar is still showing **replaces** it rather than
queuing — a queue means the Undo you are reaching for belongs to a removal two actions ago.

## 6. The catalogue search sheet

| Rule | Value, and why |
|---|---|
| Triggering | Debounced 300ms after the last keystroke, minimum two characters, no submit button. Enter re-runs the query immediately |
| Selections survive a new query | Search "villeneuve", pick two, search "kubrick", pick two more, confirm all four. This is why the selected count lives in the bottom bar and not in the list |
| Result row | 84dp: a 44×64 poster, the title on one line, and a second line holding the year and the original title when it differs — "2010 · Des hommes et des dieux" — because that is how a remake or a same-year collision is told apart |
| No director | The mock showed "2024 · Denis Villeneuve". A search response carries no crew; the director costs one extra request per result on a screen whose whole premise is results 300ms after you stop typing. Dropped |
| Selection | An M3 checkbox, 24dp square, on every row — the empty box on every other row is what makes the list read as a set of checkboxes rather than one oddly tinted row. The whole row toggles it |
| The bottom bar | 64dp, always present from the moment the sheet opens — loading, empty and failed states included — so the list's height never changes under a finger that just tapped. Pinned above the keyboard, opaque, real layout rather than floating chrome: the only bottom content in the app with zero snackbar clearance |
| The button | "Add 3 items" with a selection; plain "Add", disabled, without one — same size, same place, so the target stays where the thumb expects it |
| Confirming | Closes at once; the items land in the pool in selection order; a snackbar reports the count with no Undo, because the items are visibly there and removing one is a double-tap away |
| Dismissing | Close, scrim, drag down or back all discard the selection. There is no draft |
| Words | "Films" became "items" everywhere but the source's own name — this is not a films app, and TMDB is one source of several |

## 7. Left open

- **A cap on row height.** None today. The design said outright "say the word if you want a
  cap", and nobody has.
- **Top-aligning the band** is called the most debatable decision above. It has not been
  challenged on a device; it stays until it is.

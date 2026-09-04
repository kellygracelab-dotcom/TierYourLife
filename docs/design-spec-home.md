# TierYourLife — design spec: Home and everything it leads to

The list of boards, searching it, selecting and deleting from it, making a new board, the
trash, settings, and export.

Condensed from the design conversation of August 2026. The verbatim transcripts are not in
the repository; what is here is each decision and the reason it was made, and where the
built app diverged from the design after being used on a phone, it says so and why.
Companion to [`design-spec-board.md`](design-spec-board.md). Baseline width 412dp; dimensions
in dp unless marked sp.

---

## 1. The top bar

Two actions, both of which navigate: search and settings. No drawer — there is nothing to
put in one, the app is one list of lists. No theme toggle in the bar — a gear leads
somewhere, an icon that silently flips a colour scheme does not; theme is a setting. Under
the title, a summary line — "3 lists · 24 ranked" — hidden entirely when there are no lists,
because "0 lists · 0 ranked" under a heading that already says so is noise.

Since then the bar has gained a sort control, filters and a picture/row toggle, and on a wide
window the whole bar gives its job to the rail; both are described in the README rather than
here.

## 2. Searching your boards

The search field **replaces** the top bar; it does not appear under the title. The heading is
redundant beside a field, and the summary describes the library rather than the results. The
corner button hides — there is nothing to create from a search. The field is the same
fully-rounded 56dp shape as the catalogue search sheet, so search looks like search everywhere.

Matching is case-insensitive and anywhere in the name — "piz" finds "Pizza in Lisbon" — as you
type, with no submit. It searches names only, not the cards inside. The count line above the
results never changes shape between states, so the list does not jump while typing. No
"create it" escape in the empty state: the corner button is one gesture away once search
closes.

## 3. Selecting and deleting

Long press a card and it is selected at once — the long press does not open a menu. From then
on a tap selects instead of opening; tapping a selected card deselects it, and **deselecting
the last one leaves the mode**, because an empty contextual bar with a delete button that does
nothing is a dead end. The bar offers one action, delete. No select-all, no share, no rename —
renaming happens inside a board and multi-rename means nothing.

Delete is immediate, with no dialog: the cards are gone from the list at once and the snackbar
carries the Undo (§5).

**Changed on a device, twice.** The design first marked a selected card with a full-width
tint behind it and nothing on the card itself. Built, that read as a ripple — the platform's
own vocabulary says "pressed", not "chosen" — and gave no sign that a tap had been re-bound,
so the owner concluded multi-select was broken, which was the correct inference from what was
on screen. The design's own verdict: it had already written the rule that fixes this, for the
search sheet — *the empty checkbox on every other row is what makes the list read as a set of
checkboxes* — and failed to apply it on Home. A checkbox now takes the chevron's slot
(the chevron says "tap goes deeper", which stops being true the moment selection starts, so
the two never coexist), and the unselected cards grow empty boxes at the same instant the
pressed one fills. Then the tint went too: its square corners stuck out past a 16dp-radius
card and read as something underneath rather than about it, and two signals where one is
unambiguous is one too many.

The design also had the count replace the heading. Built, entering selection mode yanked the
whole list upward under the finger still resting on the card. The heading stays; only the bar
above it changes.

## 4. Making a board — there is no dialog

A create dialog existed early and was removed. The corner button **creates the board and
opens it**, with the title in edit mode and the keyboard up, so the first keystroke names it.
No form, no OK, and no validation by decision: nothing is blocked, nothing turns red, and an
empty name saves as "Untitled list" — an unnamed board is a recoverable state, not an error.
Five tiers exist from the start, because an empty screen with nothing to drag onto gives a
first-time user nothing to do; their empty rows are 60dp rather than 84dp so all five fit
above the pool.

**The escape hatch.** On a brand-new board the leading control is a cross, and pressing it
throws the board away — a hard delete, no trash, no dialog, the only unrecoverable action in
the app. The moment the board is touched, the cross becomes the ordinary back arrow and the
board is kept. "Touched" flips on the first keystroke and is one-way: typing a character and
deleting it again has already flipped it, because a control that oscillates between "discard"
and "back" under the thumb is worse than one that commits. Opening the search sheet and
adding nothing does not count.

Three things recorded because they are real: system back keeps the board, so a mis-tap plus a
back gesture still strands an Untitled list; the cross only helps people who see it; and
`close` now means discard here and cancel-renaming on an established board — the weakest seam
in the design. The design proposed an Undo snackbar after discarding; **none is shown**. An
untouched board has no name and no cards by definition, so there is nothing to restore, and
"undo" would create a fresh empty board — which is what pressing + does.

## 5. The undo snackbar

88dp from the bottom on Home: the corner button is 56dp with a 16dp inset, so it occupies the
bottom 72dp, and 88dp clears it with 16dp to spare. On the board screen it is 208dp, clearing
the pool sheet instead. Long duration, about ten seconds, because it is the only route back for
something that just vanished — and **replaced** rather than queued if another delete happens.
Undo restores everything at once, in the previous positions, and removes it from the trash.

The cards really are gone the moment delete is tapped; the snackbar reports something that has
already happened. If everything was deleted, the empty state shows under it.

## 6. The trash

**Nothing expires.** The design once promised 72 hours; every trace of that is gone — no
countdown, no remaining-time text, no clause in the Settings row. The subtitle says the rule
in words: "Deleted lists and items stay here until you remove them." It is reached from one
place, Settings, whose row carries the count — a recovery surface, not a destination.

Rows are 72dp, which dropping the time text made possible. A deleted card's row names the
board it came from, because a card out of its board is otherwise unidentifiable. The time is
relative and coarse — "2 h ago", "yesterday" — and is provenance, not a deadline; nothing acts
on it. Two text buttons: **Restore** in primary nearer the thumb, **Remove** in error furthest
from it. Restore puts the thing back where it came from with no confirmation and no snackbar —
the row leaving the trash is the confirmation.

Removing one entry for good, and emptying the trash, are **two of the only dialogs in the
app**. Neither has a warning icon — the title already asks the question and a triangle would
make it shout — and the destructive button is error-coloured but not filled, because filled
would make it the default and the default should be Cancel. Empty-trash lives behind an
overflow menu with one item: one extra tap is the right price for the only action that can
destroy everything at once, on a screen people reach while trying to rescue something. The
dialog says "4 entries", not "4 items", because "item" already means one card inside a board.
After emptying there is **no snackbar** — an Undo after an irreversible action would be a lie.
When the trash is empty the overflow button is not composed at all: not disabled, not greyed,
not reachable by TalkBack. That absence is the whole design of the empty state.

## 7. Settings

One scrolling column: theme, language, then the rows that lead somewhere.

**Theme** is chosen inline, three segments, because three options fit. The design gave a rule
for stacking the segments into rows at a large font scale and named three languages that
would stack at normal size too. On a device the built control overflowed in six. The third
label was the problem — "Follow system" was a phrase where the other two were words, and a
third of a 360dp row is about twelve characters — so it is one word in every language now
(System, Системная, Systemowy…), and **whether the three fit is measured, not guessed**: the
control measures its widest label against the width a segment would get, so the rule holds
for a language nobody has thought of yet. Both layouts are tested by geometry, not wording.

**Language** opens a bottom sheet over the same screen: eleven 48dp rows are 528dp, which
inline pushed Export off the bottom of the design, and 48dp is the touch-target floor and
must not shrink. English first as the default; the rest in the order they were added, not
alphabetical. Arabic's row says "right-to-left" beside it.

**Trash** shows its count as the row's supporting line — "Empty", not "0 items", when there
is nothing. **Export** is last.

## 8. Export is a readable listing, not JSON

Never drawn; the design only ever had a row labelled "Save every list as a JSON file". Since
there is no import, the file is for a person — something to open, read, mail to yourself or
paste into a note — so it is plain text. Not JSON, and not CSV, which would flatten the tiers
into a column and lose the thing the app is for.

The file: the app name and date and totals, then each board in the order Home shows them —
name, a rule of `=` under it, counts — then each tier with its caption and its cards numbered
within the tier, `(empty)` for a tier left empty (a tier you made and left empty is
information; dropping it would make the file disagree with the screen), and the pool last,
**unnumbered**, because the pool has no order worth exporting and numbering would imply one.
No pictures, no history, no trash, no settings.

It goes through the system create-document picker, so the app writes nowhere on its own and
needs no storage permission. Nothing is shown while writing: a few kilobytes are written
faster than a spinner can appear, and a progress dialog for it would be theatre. Success is a
short snackbar with a Share action; failure is a snackbar with Try again; cancelling the picker
shows nothing at all. The file's own headings are translated with the app; the filename keeps
the product's name in every language.

## 9. Decided from the device, not the design

- **Nothing is seeded.** The first build created two demo boards whenever it found none, which
  made them impossible to delete — they came back on the next read — and meant the empty
  state could never appear. An empty library is a legitimate state and the empty state says
  so.
- **Icons mirror by opting in.** Arabic ships, so layout mirrors; every icon here is a
  hand-drawn path, which Compose does not mirror, and the first build flipped the screen while
  the back arrow kept pointing the English way. The flip lives in the one place icons are
  drawn, behind a flag each icon opts into — because most must not move: a plus, a bin, a
  check and a gear mean the same thing either way, and a magnifying glass is deliberately
  among them, as in Material. Eight do flip: the back arrow, both chevrons, the two list
  glyphs, the tune sliders, the photo stack and the empty-trash sweep. A test captures an
  icon's pixels in both directions and checks where the ink sits.

# TierYourLife — design spec: Home and everything it leads to

Turn 12 of the design conversation, transcribed verbatim on 2026-08-05, plus a
reality check at the end written against the code rather than the design.

Companion to `design-spec-turns-8-9.md`, which covers the tier detail screen. Sections
2 (type), 3 (narrow screens and large system font) and 4 (palette) of that document
apply to every screen described here.

Android · Material 3 · TierYourLife. Baseline width 412dp, all dimensions dp unless
marked sp.

**Two things to read first.** There is no create-list dialog — it was removed and
replaced by create-and-open, so §4 describes the FAB and the screen it opens, not a
form. And Export has never been drawn: all of §8 is new and should be treated as a
proposal.

---

## 1. Home top bar — the final arrangement

Drawn in 1a and revised in turn 3. The built screen is behind the design in three ways:
it still has a navigation drawer icon (dropped — there is nothing to put in a drawer,
the app is one list of lists), it uses three dots to toggle the theme (dropped — theme
is a setting, not a top-level action), and it has no way to reach Settings.

### What the bar holds, in order

| Part | Value |
| --- | --- |
| Structure | M3 `LargeTopAppBar`. Collapsed height 56dp; the title block below it is part of the same component and scrolls away with it. No navigation icon at all — Home is the root, so the leading 48dp slot is empty and the title starts at 16dp. |
| 1 · Title | "Your lists" at headlineLarge 32/40 in onSurface. 16dp side padding, 8dp above. |
| 2 · Summary line | Directly under the title at bodyMedium 14/20 in onSurfaceVariant, 2dp gap: "3 lists · 24 ranked", both halves plural-aware. Hidden entirely when there are no lists — "0 lists · 0 ranked" under a heading that already says there are none is noise. |
| 3 · `search` | 48×48dp icon button, 24dp glyph in onSurfaceVariant, first of the two trailing actions. CD "Search your lists". |
| 4 · `settings` | 48×48dp icon button, 24dp glyph in onSurfaceVariant, at the trailing edge, 4dp from the screen edge. CD "Settings". This replaced the theme toggle — a gear leads somewhere, an icon that silently flips a colour scheme does not. |
| Nothing else | No drawer, no overflow, no theme button, no sort or filter control. Two actions, both of which navigate. |

| Part | Light | Dark |
| --- | --- | --- |
| Bar background | surface #FBF8FF, no elevation while at rest | #121318 |
| Title / summary | #1B1B21 / #46464F | #E4E1E9 / #C7C5D0 |
| Action glyphs | #46464F | #C7C5D0 |

### Strings

| Key | Value |
| --- | --- |
| `home_title` | "Your lists" |
| `home_subtitle` | "%1$s · %2$s" from `list_count` + `ranked_count`; hidden at zero |
| `list_count` | one "1 list" · other "%1$d lists" |
| `ranked_count` | one "1 ranked" · other "%1$d ranked" |
| `cd_search_home` | "Search your lists" |
| `cd_settings` | "Settings" |

---

## 2. Searching tier lists on Home

Drawn at 5l in three states.

### Entering, and what happens to the header

| Part | Value |
| --- | --- |
| Trigger | The `search` action. The whole top bar — title, summary line and both actions — is replaced by the search field. It is **not** a field that appears below the title. |
| Title and summary | **Both gone** while searching. The heading is redundant next to a field, and the summary describes the library rather than the results. |
| The FAB | **Hidden** while searching. There is nothing to create from a search. |

### The field

| Part | Value |
| --- | --- |
| Geometry | 56dp tall, **28dp radius** (fully rounded), surfaceContainerLow fill, 12dp side margins, 8dp above and 4dp below. Same shape as the TMDB search field, so search looks like search everywhere in the app. |
| Leading | 48×48dp `arrow_back` in onSurfaceVariant. CD "Close search". **This is how the user leaves search** — it restores the title, the summary and the FAB, and clears the query. |
| Query | bodyLarge 16/24 in onSurface, primary caret. Placeholder "Search your lists" in outline. Keyboard up on entry. |
| Trailing | 48×48dp `cancel` in onSurfaceVariant, **only when the query is non-empty**. CD "Clear search". It clears the text and returns to the empty state; it does **not** leave search. |
| Also leaves search | System back — once, and it exits search entirely rather than only dismissing the keyboard. |

### Results

| Part | Value |
| --- | --- |
| Count line | labelMedium 12/16 in onSurfaceVariant, 20dp side padding, 8dp above / 6dp below: "3 lists" with an empty query, "1 list" with one match. It doubles as the results header and **never changes shape between states**, so the list does not jump as you type. |
| The cards | The **unmodified** Home cards — same 16dp radius, 16dp padding, 14dp internal gap, same distribution bar and footnote row. A result is tappable exactly as it is on Home. 12dp apart, 16dp side padding. |
| Matching | Case-insensitive, matches **anywhere** in the name and not only at the start — "piz" finds "Pizza in Lisbon". Results appear as you type; there is no submit. |
| Scope | **List names only.** It does not search item titles. |

### No results

| Part | Value |
| --- | --- |
| Layout | The standard empty state, centred with 120dp of bottom offset: 56dp surfaceContainerLow circle with a 28dp `search_off` in outline, 8dp, headline, 8dp, body. |
| Headline | headlineSmall 24/32 in onSurface, `text-wrap: pretty`: “No lists match “sushi”” — the query verbatim in curly quotes. |
| Body | bodyMedium 14/20 in onSurfaceVariant, max-width 264dp: "Try a shorter word, or check the spelling." |
| No action button | Unlike the TMDB sheet's empty state, there is no "create it" escape here — the FAB is the way to make a list and it is one gesture away once search is closed. |

### Strings

| Key | Value |
| --- | --- |
| `home_search_hint` | "Search your lists" |
| `search_results_count` | one "1 list" · other "%1$d lists" |
| `search_no_results_title` | “No lists match “%1$s”” |
| `search_no_results_body` | "Try a shorter word, or check the spelling." |
| `cd_close_search` / `cd_clear_query` | "Close search" · "Clear search" |

---

## 3. Selection mode on Home

Drawn at 3c (one selected) and 3d (three selected).

### Entering and leaving

| Part | Value |
| --- | --- |
| Enter | Long press a card, ~400ms, one haptic tick. The card selects immediately — the long press does **not** open a menu first. |
| Add to the selection | A single tap on any other card, once selection mode is active. Tap is re-bound while in this mode: it selects instead of opening the list. |
| Remove from the selection | A single tap on a card that is already selected. **Deselecting the last remaining card exits selection mode automatically** — an empty contextual bar with a delete button that does nothing is a dead end. |
| Leave | The close button in the contextual bar, system back, or deselecting everything. Leaving restores the normal top bar, the title, the summary line and the FAB. |

### The contextual bar

| Part | Value |
| --- | --- |
| Geometry | Replaces the top bar in place. **56dp tall** — the collapsed app-bar height, not the large one: the title block collapses away, because in this mode the count is the title. |
| Background | surfaceContainerLow — #F3F0F9 light, #1F1F25 dark. A filled bar against the plain surface behind it is what says the screen is in a different mode. |
| Leading | 48×48dp `close`, 24dp glyph in onSurfaceVariant. CD "Exit selection". |
| Title | The count at titleLarge 20/28 in onSurface, plural-aware: "1 selected" / "3 selected". It replaces "Your lists" and the summary line entirely. |
| Actions | **One** — 48×48dp `delete_outline`, 24dp glyph in onSurfaceVariant, at the trailing edge. CD "Delete 3 lists", taking the same plural as the count. Nothing else: no select-all, no share, no rename. Renaming happens inside a list, and multi-rename is meaningless. |
| The FAB | Hidden while selecting. |

### The selected card

| Part | Value |
| --- | --- |
| Treatment | The standard row tint — #EDEBFA light, #2E2F45 dark — behind the card, run **full width, edge to edge, no inset and no radius**: the tint bleeds past the card's 16dp side margins to both screen edges, so it reads as a selected row of the list rather than a second card behind the card. |
| The card itself | Unchanged — same surfaceContainerLow fill, same 16dp radius, same contents and colours. No checkbox, no check overlay, no scale or elevation change. The tint is the whole signal. |
| Unselected cards | Completely unchanged. They do not dim. |

### Deleting

| Part | Value |
| --- | --- |
| What happens | Tapping `delete_outline` deletes **immediately — no confirmation dialog**. The cards are gone from the list at once and the snackbar carries the Undo (§5). Selection mode ends on the same tap. |
| Where they go | The trash, as a soft delete, recoverable there as well as through the snackbar. |

### Strings

| Key | Value |
| --- | --- |
| `selection_count` | one "1 selected" · other "%1$d selected" |
| `cd_close_selection` | "Exit selection" |
| `cd_delete_selected` | "Delete %1$s" from `list_count` → "Delete 3 lists" |

---

## 4. Creating a list — there is no dialog, and that was deliberate

> The premise of the question does not match the design. Reading it back rather than
> building to it.

**The + button does not open a dialog.** A create-list dialog existed in turn 1 and was
removed in turn 5 (5h). Tapping the FAB **creates the list immediately and pushes the
tier list screen**, with the title already in edit mode and the keyboard up, so the
first keystroke names it. There is no name dialog, no OK button, and no validation to
describe — because there is no form.

### The button

| Part | Value |
| --- | --- |
| Geometry | FAB, 56×56dp, **16dp corner radius** (M3 medium FAB shape, not a circle), bottom-right, 16dp from both the right and bottom edges. |
| Colours | primaryContainer fill — #DEE0FF light, #333E8F dark — with a 24dp `add` glyph in onPrimaryContainer (#2C3A80 / #DEE0FF). Elevation `0 3px 8px rgba(0,0,0,.18)`. |
| Content description | "New list". |
| Hidden when | Search is active, or selection mode is active. |

### What the new list opens as

| Part | Value |
| --- | --- |
| The screen | The tier list screen, pushed onto the stack. Back returns to Home. |
| Title | **In edit mode**: `close` replaces the back arrow, the field carries the placeholder "Untitled list" in outline with a primary caret and a 2dp primary underline, and a `check` in primary sits at the trailing edge. Counter "0/60" right-aligned on its own line below, with the helper "Left empty, this list stays Untitled list." at bodySmall in onSurfaceVariant beside it. |
| "Validation" | **None, by decision.** Nothing is blocked, nothing turns red, and the confirm is never disabled. An empty name saves as "Untitled list". Blocking would be the app's first piece of validation and an unnamed list is a recoverable state, not an error. 60 characters is the only limit. |
| Tiers | Five exist from the start — S / A / B / C / D with the seeded captions — because an empty screen with no tiers gives a first-time user nothing to drag onto. |
| Empty rows | **60dp tall, not 84dp.** A row grows to 84dp once it holds a poster; 60dp is what lets all five fit above the pool without scrolling. |
| The pool | Header "Pool · nothing yet", and instead of a tile strip one line at bodySmall in onSurfaceVariant: "Add films with the Add button, or write one in by hand." — it names both ways in. |
| Confirming | The `check`, or the keyboard's Done. The list is already saved before this — the check only commits the name — so back or close leaves a list called Untitled list and nothing is lost and nothing needs confirming. |
| On return to Home | The new list appears as a card **at the top** of the list, with metadata "0 in pool · nothing ranked yet", an all-unranked distribution bar (a single flat segment in the unranked colour, #DAD7E0 / #46464F, 4dp radius) and the footnote "Start dragging to rank these" with a 16dp `drag_indicator` in outline. **Never drawn:** whether the list order on Home is newest-first or by last edit was never specified — this assumes newest first, which is the same thing on a fresh list. |

### Strings

| Key | Value |
| --- | --- |
| `cd_new_list` | "New list" |
| `list_title_hint` | "Untitled list" |
| `list_title_empty_note` | "Left empty, this list stays Untitled list." |
| `list_title_counter` | "%1$d/60" |
| `pool_header_empty` | "Pool · nothing yet" |
| `pool_empty_body` | "Add films with the Add button, or write one in by hand." |

---

## 5. The undo snackbar on Home

Drawn at 5f. 3e drew it too but showed the deleted cards still on screen, which was
wrong and is corrected in 5f.

### Geometry and placement

| Part | Value |
| --- | --- |
| Container | M3 snackbar: **4dp radius** (not rounded), min height 48dp, 16dp left / 8dp right inner padding, 8dp vertical. Elevation `0 4px 12px rgba(0,0,0,.28)`. |
| Colours | inverseSurface fill — #303036 light, #E4E1E9 dark; label in onInverseSurface (#F3F0F7 / #1B1B21); action in inversePrimary (#BAC3FF light, #4A5BAA dark). |
| Position | 16dp from each side, **88dp from the bottom**. The 88dp is the whole point: the FAB is 56dp tall with a 16dp inset, so it occupies the bottom 72dp — 88dp clears it with 16dp to spare, and the snackbar never sits under the button or pushes it up. |
| Elsewhere in the app | 208dp on the tier list screen, where it has to clear the pool sheet instead. Home is 88dp. |
| Label | bodyMedium 14/20: "3 lists deleted", plural-aware — "1 list deleted" for one. |
| Action | "Undo" at labelLarge 14/20 in inversePrimary, 10dp/12dp padding inside a 48dp target. |
| Duration | **M3 long — about 10 seconds**, not the 4-second short form. It is the only route back for something that just vanished, so it gets the longer window. It also dismisses on swipe, and is **replaced** immediately if another delete happens (never queued: a queue means the Undo you are reaching for belongs to an earlier action). |
| Undo restores | All the deleted lists at once, in their previous positions, and removes them from the trash. |

### What the screen behind it shows

| Part | Value |
| --- | --- |
| The lists really are gone | The cards are removed the moment delete is tapped. The snackbar reports something that has already happened. |
| If everything was deleted | Home shows its empty state under the snackbar — 56dp circle with a 28dp `format_list_bulleted` in outline, "No lists yet" at headlineSmall, "Tap + to rank something." at bodyMedium, max-width 264dp, centred with 80dp of bottom offset so it clears the FAB. The summary line is hidden. Undo brings the lists back and the empty state disappears. |

### Strings

| Key | Value |
| --- | --- |
| `snack_lists_deleted` | one "1 list deleted" · other "%1$d lists deleted" |
| `action_undo` | "Undo" |
| `home_empty_title` / `_body` | "No lists yet" · "Tap + to rank something." |

---

## 6. The Trash screen in full

Drawn at 5b–5d and 6b–6e. **Nothing expires.** Every trace of the old 72-hour rule is
gone: no remaining-time text, no countdown bar, and no promise of a time limit in the
header, the empty state or the Settings subtitle.

### Reached from

| Part | Value |
| --- | --- |
| One route only | **Settings → Trash.** There is no trash icon on Home, no entry in a menu, and no swipe. It is a recovery surface, not a destination — and the Settings row carries the count, which is the only reason to go there. |

### Header

| Part | Value |
| --- | --- |
| App bar | 56dp, `arrow_back` at the leading edge, title "Trash" at titleLarge, and one trailing action: 48×48dp `more_vert`. CD "More options". |
| Subtitle | labelMedium 12/16 in onSurfaceVariant, 20dp side padding, 8dp below: "Deleted lists and items stay here until you remove them." It states the new rule in words — that nothing disappears on its own is the thing a user needs told. |

### A row — both kinds

| Part | Value |
| --- | --- |
| Geometry | **72dp tall**, 12dp vertical / 16dp horizontal padding, 16dp gaps. No divider between rows. Dropping the time text is what let the row come back to 72dp — it was the only reason it ever needed three stacked lines. |
| Deleted list thumb | 40×40dp, 8dp radius, surfaceContainerLow fill, 6dp padding, holding three 4dp bars at 2dp radius in the S band, A band and unranked colours — a miniature of the distribution bar, so a list is recognisable as a list. |
| Deleted item thumb | 40×40dp, 8dp radius, the poster placeholder fill. In the app this is the item's poster, cropped square. |
| Title | bodyLarge 16/22 in onSurface, 1 line, ellipsis. "Pizza in Lisbon" / "Interstellar". |
| Meta — list | labelMedium 12/16 in onSurfaceVariant: "List · 5 items · deleted 2 h ago". The item count is plural-aware. |
| Meta — item | "Item · from Sci-fi films · deleted yesterday" — the source list is named, because an item out of its list is otherwise unidentifiable. |
| Time wording | Relative and coarse: "2 h ago", "yesterday", "4 days ago", "6 days ago". It is provenance, not a deadline — nothing acts on it. |
| Actions | Two text buttons, `flex: none`, never truncating, at labelLarge 14/20: **Restore** in primary, then **Remove** in error. Order is deliberate — the safe action sits nearer the thumb and the destructive one furthest from the drag of the thumb. |
| At large text | At ≥1.5× font, or under 320dp, both buttons drop to a second row, right-aligned below the thumb and text. Two side-by-side text buttons cannot survive 26sp in 288dp. |
| Restore | Puts the list or item back where it came from. No confirmation, no snackbar — the row leaving the trash is the confirmation. |

### Removing one entry for good — one of only two dialogs in the app

| Part | Value |
| --- | --- |
| Dialog | M3 AlertDialog: 28dp radius, 24dp padding with 18dp to the button row, 24dp side inset, surfaceContainerHigh fill, scrim `rgba(0,0,0,.32)`. |
| Title | "Remove permanently?" at headlineSmall 24/32. |
| Body | bodyMedium 14/20 in onSurfaceVariant. List: "Pizza in Lisbon and its 5 items will be deleted from this device. This can't be undone." Item: "Interstellar will be deleted from this device. This can't be undone." The name is inline in the sentence rather than quoted in the title, so it stays readable when long. |
| Buttons | Cancel, then **Remove** in error. Bottom-right, 8dp apart, both text buttons. |
| Deliberately not | No warning icon — the title already asks the question and a triangle would make it shout. Remove is error-coloured but **not** a filled button: filled would make it the default, and the default here should be Cancel. |

### Emptying the trash — the other dialog

| Part | Value |
| --- | --- |
| Where it lives | Behind `more_vert`, in a menu holding one item. It matches the app's existing pattern for destructive list-level actions, and one extra tap is the right price for the only action that can destroy everything at once. It is not a bare icon: this is a screen people reach while trying to rescue something. |
| The menu | 212dp wide, 12dp radius, surfaceContainerHigh, 8dp inset from the right, top at 52dp, 8dp vertical padding. One 48dp item: 20dp `delete_sweep` glyph and a bodyMedium label, both in the error colour. |
| Dialog | Identical in every dimension to the one above. Title "Empty the trash?", body "4 entries will be deleted from this device. This can't be undone.", buttons Cancel / **Empty trash** (error). |
| "entries", not "items" | "Item" already means one film inside a list — the rows themselves say "List · 5 items". "4 items will be deleted" would read as four films when it is in fact three lists and a film. |
| After confirming | Every entry is removed and the screen drops straight to the empty state. **No snackbar** — there is nothing left to undo, and an Undo after an irreversible action would be a lie. |
| When the trash is empty | The `more_vert` button is **not composed at all** — not disabled, not greyed. It cannot be focused by TalkBack or reached by keyboard. Compare the two bars: that absence is the whole design of the empty state. |
| Count accuracy | The number in the dialog body is read when the dialog opens, so it can never disagree with the list behind it. |

### Empty state

| Part | Value |
| --- | --- |
| The bar | `arrow_back` and nothing else. |
| Body | 56dp surfaceContainerLow circle with a 28dp `delete_outline` in outline, 8dp, "Nothing in the trash" at headlineSmall 24/32, 8dp, "Lists and items you delete appear here. They stay until you remove them." at bodyMedium 14/20, max-width 264dp, vertically centred with 40dp side padding. |

### Strings

| Key | Value |
| --- | --- |
| `trash_title` | "Trash" |
| `trash_subtitle` | "Deleted lists and items stay here until you remove them." |
| `trash_row_list_meta` | "List · %1$s · deleted %2$s" |
| `trash_row_item_meta` | "Item · from %1$s · deleted %2$s" |
| `list_items_count` | one "1 item" · other "%1$d items" |
| `action_restore` / `action_remove` | "Restore" · "Remove" |
| `remove_dialog_title` | "Remove permanently?" |
| `remove_dialog_body_list` | "%1$s and its %2$s will be deleted from this device. This can't be undone." |
| `remove_dialog_body_item` | "%1$s will be deleted from this device. This can't be undone." |
| `menu_empty_trash` / `action_empty_trash` | "Empty trash" |
| `empty_trash_dialog_title` | "Empty the trash?" |
| `empty_trash_dialog_body` | "%1$s will be deleted from this device. This can't be undone." |
| `trash_entry_count` | one "1 entry" · other "%1$d entries" |
| `trash_empty_title` / `_body` | "Nothing in the trash" · "Lists and items you delete appear here. They stay until you remove them." |
| `cd_more` | "More options" |
| `cd_restore` / `cd_remove` | "Restore %1$s" · "Remove %1$s permanently" |
| menu state description | "Deletes %1$s permanently" — announced before the dialog opens; the item needs no CD, its label is visible text |

---

## 7. App Settings as it now stands

Drawn at 3k, revised at 5e (Trash subtitle), and revised again in the language work.

### Shape

| Part | Value |
| --- | --- |
| App bar | 56dp, `arrow_back`, title "Settings" at titleLarge. No actions. |
| The screen | One scrolling column. Nothing is pinned. Order: **Theme, Language, Trash, Export**. |
| Dividers | 1dp in outlineVariant (#E4E1E9 / #46464F), inset 16dp from both sides, between each block. |

### 1 · Theme — inline, not a row

| Part | Value |
| --- | --- |
| Label | "Theme" at bodyLarge 16/22 in onSurface, with "Follow system uses your Android setting." at bodySmall 13/18 in onSurfaceVariant under it. 12dp/16dp padding, 12dp to the control. |
| Control | A three-segment `SingleChoiceSegmentedButtonRow`, 40dp tall, three equal segments. 1dp outline border (#77767F / #91909A), 100dp radius on the outer ends only and shared inner edges. Selected segment: primaryContainer fill with an 18dp `check` and the label at titleSmall 13/18 in onPrimaryContainer; unselected labels in onSurfaceVariant. |
| Labels | "Light" · "Dark" · "Follow system". It is chosen here inline rather than on a sub-screen because three options fit. |
| At large text | Stacks at ≥1.3× into three 48dp rows with the check on the left, keeping the outline and the 100dp end radii on the first and last row. Ellipsising "Follow syst…" would be worse than stacking. In German, Polish and Turkish it will stack at 100% too — "Follow system" becomes "Systemeinstellung folgen". |

### 2 · Language

| Part | Value |
| --- | --- |
| The row | A standard settings row — 24dp `translate` glyph in onSurfaceVariant, title "Language" at bodyLarge, the current language as its supporting line at bodySmall in onSurfaceVariant ("English"), 20dp `chevron_right` in outline at the trailing edge. Row min 56dp, 14dp/16dp padding, 16dp gaps. |
| What it opens | A **bottom sheet over the same screen**, not a separate screen: 28dp top corners, surfaceContainerHigh, top at 186dp, 32×4dp handle. Title "Language" at headlineSmall with "Changes the app's own text. Names you typed stay as they are." at bodySmall under it. Then eleven 48dp radio rows, 20dp side padding: 20dp radio, the language written in itself at bodyLarge, and its English name at bodySmall on the trailing edge. The selected row takes the standard row tint. |
| The eleven | English (trailing label "Default"), Українська, Русский, Español, Português (Brasil), Deutsch, Français, Polski, Türkçe, 日本語, العربية (trailing label "Arabic · right-to-left"). English first as the default; the rest in the order you gave — not alphabetical and not by speaker count. |
| Why a sheet | Eleven 48dp rows are 528dp; inline, with the theme control plus Trash and Export, the column came to ~903dp against an 812dp viewport, so the fold cut through the Trash row and pushed Export out of the design. 48dp is the touch-target floor and must not shrink, so the list moved into a sheet where all eleven fit at once. |

### 3 · Trash

| Part | Value |
| --- | --- |
| The row | 24dp `delete_outline`, title "Trash", 20dp chevron. Min 56dp. |
| On the right | Nothing on the right but the chevron — the count is the row's **supporting line**, under the title, at bodySmall in onSurfaceVariant: "4 items", plural-aware. |
| When empty | "Empty" — not "0 items". The row stays present and tappable; the Trash screen's own empty state explains itself. |
| What it dropped | "4 items · kept 72 hours" → "4 items". The clause was the last promise of a time limit anywhere in the app. |

### 4 · Export

| Part | Value |
| --- | --- |
| The row | 24dp `file_download`, title "Export data", supporting line, 20dp chevron. Min 56dp. Last in the column. |
| Supporting line | Changed — see §8. It read "Save every list as a JSON file"; it now reads "Save a readable copy of every list". |

### Strings

| Key | Value |
| --- | --- |
| `settings_title` | "Settings" |
| `settings_theme` / `_sub` | "Theme" · "Follow system uses your Android setting." |
| `theme_light` / `_dark` / `_system` | "Light" · "Dark" · "Follow system" |
| `settings_language` | "Language" |
| `settings_language_caption` | "Changes the app's own text. Names you typed stay as they are." |
| `language_default` | "Default" |
| `settings_trash` | "Trash" |
| `settings_trash_subtitle` | plural `list_items_count` · "Empty" at zero |
| `settings_export` | "Export data" |
| `settings_export_sub` | "Save a readable copy of every list" |

---

## 8. Export — a readable listing, not JSON

**Never drawn. Everything in this section is new** — the design has only ever had a
Settings row labelled "Save every list as a JSON file". Nothing here repeats an earlier
decision, so treat all of it as a proposal.

Since there is no import, the file is for a person: something you can open, read, mail
to yourself, or paste into a note. So it is plain text, not JSON and not CSV — CSV
would flatten the tiers into a column and lose the thing the app is for.

### The file

| Part | Value |
| --- | --- |
| Format | `.txt`, UTF-8, no BOM, LF line endings. Wrapped at no fixed column — let the reader's app wrap. |
| Name | `TierYourLife 2026-08-05.txt` — app name, then an ISO date. Sortable, unambiguous across locales, and safe on every filesystem (no colons, no slashes). If a file of that name exists the system picker handles the collision, as Android's create-document flow already does. |
| Where | Through the system create-document picker (SAF), so the user chooses the folder and can send it straight to Drive or a mail app. The app writes nowhere on its own and needs no storage permission. |

### What it contains, in order

**1 · Header** — three lines: the app name, the export date in the user's locale format,
and the totals. Then a blank line.

```
TierYourLife
Exported 5 August 2026
3 lists · 24 ranked · 15 unranked
```

**2 · Each list** — in the same order as Home, so the file matches what the user just
looked at. The list name, then a rule of `=` the width of the name, then a count line,
then a blank line.

```
Sci-fi films
============
7 ranked · 6 unranked
```

**3 · Each tier** — in tier position order, S first, worst last. The label, then the
caption in brackets when the tier has one, then the items in rank order, numbered within
the tier, two-space indent.

```
S — Masterpiece
  1. Interstellar
  2. Arrival
```

| Part | Value |
| --- | --- |
| Empty tiers | Included, with `(empty)` in place of items. A tier the user made and left empty is information — silently dropping it would make the file disagree with the screen. |
| 4 · The pool, last | Under the heading `Unranked`, items **unnumbered** — the pool has no order worth exporting, and numbering it would imply one. Omitted entirely when the pool is empty. |
| 5 · Between lists | One blank line, then the next list. No page breaks, no separators beyond the `=` rule. |
| Deliberately excluded | No item images (a text file cannot carry them, and a zip would make this a backup rather than something readable). No change history. No trash. No settings. No dates on items. |

### What the user sees

| Part | Value |
| --- | --- |
| Tap | The Export row opens the system create-document picker straight away, pre-filled with the filename. No intermediate options screen — there is nothing to choose. |
| While writing | **Nothing.** A local file of a few kilobytes is written faster than a spinner can appear, and a progress dialog for it would be theatre. If a future export ever grows large enough to matter, the pattern is the 4dp indeterminate linear indicator under the app bar, matching the search sheet — not a blocking dialog. |
| On success | A snackbar on the Settings screen: "Exported 3 lists", plural-aware, at the standard 16dp/88dp position, with a single action "Share" in inversePrimary that hands the file to the system share sheet. Short duration (~4s) — nothing is lost if it is missed. |
| If it fails | A snackbar "Couldn't save the file" with "Try again". Not a dialog: nothing has been destroyed and the retry is one tap. The two failure causes — the user cancelled the picker, or the write failed — are not distinguished; **cancelling shows nothing at all**. |

### Strings

| Key | Value |
| --- | --- |
| `settings_export` | "Export data" |
| `settings_export_sub` | "Save a readable copy of every list" |
| `export_filename` | "TierYourLife %1$s.txt" with an ISO date |
| `export_file_header` | "TierYourLife" |
| `export_file_date` | "Exported %1$s" |
| `export_file_totals` | "%1$s · %2$s · %3$s" from `list_count`, `ranked_count`, `unranked_count` |
| `unranked_count` | one "1 unranked" · other "%1$d unranked" |
| `export_list_counts` | "%1$s · %2$s" from `ranked_count` and `unranked_count` |
| `export_tier_with_caption` | "%1$s — %2$s" → "S — Masterpiece" |
| `export_tier_plain` | "%1$s" — label alone when the tier has no caption |
| `export_tier_empty` | "(empty)" |
| `export_unranked_heading` | "Unranked" |
| `snack_export_done` | one "Exported 1 list" · other "Exported %1$d lists" |
| `action_share` | "Share" |
| `snack_export_failed` | "Couldn't save the file" |
| `action_try_again` | "Try again" |

**Note on translation:** the file's own headings ("Exported", "Unranked", "(empty)")
are translated with the app, so an export made in Ukrainian reads in Ukrainian. The
filename stays "TierYourLife" in every language, because it is the product name.

---

## 9. Invented here, and data the app may not hold

### Invented — never drawn before

| Thing | Note |
| --- | --- |
| The whole of Export | §8. Format, filename, ordering, headings, the success and failure snackbars, and the decision to show nothing while writing. The design has only ever had a row label. |
| Export row copy | "Save every list as a JSON file" → "Save a readable copy of every list". |
| Home list order | §4 assumes a new list appears at the top. Whether Home is ordered newest-first, by last edit, or by name has never been decided. |
| The new-list card's appearance | The all-unranked distribution bar (one flat segment) and the "Start dragging to rank these" footnote were drawn for an existing empty list, not specified as the state of a brand-new one. Same treatment, stated for the first time. |
| Deselect-to-exit | That removing the last selection exits selection mode was implied by 3c/3d but never stated. |
| Snackbar duration | ~10s on Home for the delete undo, ~4s for export. No duration was ever specified anywhere. |
| Trash time wording | "2 h ago", "yesterday", "4 days ago" — the coarse buckets were drawn as examples; the rule that they are provenance and nothing acts on them is stated here. |

### Data these screens need — as the design saw it

| Thing | Note |
| --- | --- |
| Item order inside a tier | The file numbers items 1, 2, 3 within each tier, which needs a position on the item. |
| Tier caption | Export prints "S — Masterpiece", so it reads the caption too. |
| Deletion timestamp | The Trash rows show "deleted 2 h ago", which needs a `deletedAt` on the soft-deleted row. |
| Trash entry count | The Settings subtitle and the empty-trash dialog both need a single count across two kinds of soft-deleted row — lists and items together. |
| Unranked count | The export header prints a total unranked figure across all lists; Home only ever showed it per list. |
| Restore target | Restoring a deleted item puts it back "where it came from" — that needs the item to remember its list **and its tier** at deletion time. If the tier it came from has since been deleted, nothing is designed for that. Genuinely open. |
| Needs nothing new | The top bar, search, selection mode, the FAB behaviour, the snackbar, the theme setting and the language setting. |

---

## 10. Reality check against the code — written 2026-08-05, not by the design

The design does not read the repository, so its data-gap list is written from the
outside. Checked against `feature/tier/data` and `feature/tier/domain` on
`feature/m7-delete-and-recovery`:

| The design's concern | What the code actually has |
| --- | --- |
| Item order inside a tier | **Already there.** `tier_items.position`, maintained transactionally by `TierDao.moveItem`, which compacts the source tier and shifts the target. Export can number items and the numbers will be stable. |
| Tier caption | **Already there.** `tiers.caption TEXT` added by `MIGRATION_1_2`, which also backfills the five literals ("Masterpiece", "Great", "Good", "Watchable", "No"). `Tier.caption: String?` is on the domain model. |
| Deletion timestamp | **Already there.** `deletedAt INTEGER` on both `tier_lists` and `tier_items` (`MIGRATION_2_3`), and `TrashEntry.deletedAtMillis` carries it into the domain. |
| Trash entry count | **Already there in one call.** `TierRepository.getTrashEntries()` returns lists and items merged and sorted newest-first; its `size` is the count both the Settings subtitle and the empty-trash dialog need. |
| Unranked count | Derivable — sum of `tiers.first { isPool }.items.size` across lists. No storage needed. |
| Restore target | `restoreTierItem(id)` restores the row in place, so the item keeps whatever `tierId` it had. The unhandled case is real but narrower than the design thinks: it only arises if the tier is deleted **after** the item was trashed, and it is tracked separately — do not try to fix it in this task. |

**No new migration and no new DAO method are needed for anything in this document.**

Two things the design assumes that the code does *not* do, and which are decided here:

- **Home list order.** `TierDao.getAllTierLists()` is `ORDER BY id ASC` — oldest first.
  It stays that way for now, so a newly created list appears at the **bottom**, not the
  top as §4 says. Changing the ordering means changing a query that the data module's
  instrumentation tests assert against, and those tests need a device. Danylo decides
  later; nothing else in this document depends on it, because creating a list navigates
  straight into it rather than back to Home.
- **Language.** §7 puts a Language row second in Settings. The app currently ships one
  `values/strings.xml` and no translations, so the row would open a chooser over a
  single language. It is left out until the localization pass; Settings is Theme,
  Trash, Export, in that order, with the same dividers and geometry.

---

# Turn 13 — the two device fixes (transcribed 2026-08-05)

Both come from using the built screen on a real phone. Turn 13 supersedes the named
lines in §3 and §4 above; where the two disagree, turn 13 wins.

## 11. Selection mode gets a checkbox (supersedes §3 "The selected card")

The design's own verdict, quoted so the reasoning survives:

> The tint alone was a mistake, and the build is faithful to it. A full-bleed tint
> behind a card that already has its own fill is a weak signal — the card's
> `surfaceContainerLow` is doing the visual work, and a wash behind it reads as
> *underneath* rather than *about* the card. Worse, a transient full-width tint is
> exactly what a ripple looks like on Android, so the platform's own vocabulary says
> "pressed", not "chosen".
>
> But the real failure isn't aesthetic. Selection mode **re-binds what a tap does**,
> and I put no affordance on the card to signal that. The owner concluding multi-select
> was broken is the correct inference from what's on screen. And I'd already written the
> rule that fixes it, for the TMDB sheet: *"the empty checkbox on every other row is what
> makes the list read as a set of checkboxes."* I failed to apply my own rule on Home.

| Part | Value |
| --- | --- |
| Where it goes | **The checkbox takes the chevron's slot.** The chevron says "tap goes deeper", which stops being true the moment selection starts, so the two never coexist. |
| The box | 24dp square, 4dp radius. Unchecked: outlined, no fill. Checked: filled with primary and an 18dp check glyph on it. |
| Nothing shifts | Chevron 20dp → box 24dp; the text column absorbs the 4dp. No layout jump when the mode starts. |
| The tint | **Kept**, but demoted — it is now for scanning the list at a glance. The checkbox is what states the selection. |
| Hit target | The whole card stays one target. Give the card `Role.Checkbox` and silence the box itself so TalkBack does not announce it twice. |
| The frame that matters | On entering the mode, the **unselected** cards grow empty boxes at the same instant as the pressed one fills. That simultaneous appearance is what teaches the user that a plain tap now selects. |

## 12. Creating a list gets an escape hatch (supersedes §4 "Confirming")

The leading control on a brand-new list is a close cross; pressing it throws the list
away. Once the list has been touched it becomes the ordinary back arrow and the list is
kept.

| Part | Value |
| --- | --- |
| "Untouched" means | Empty trimmed title, no items, five untouched seeded tiers, Wrapped display mode. |
| Typing a character and deleting it again | **The latch has already flipped.** The flag goes on the first keystroke and is one-way. A leading control that oscillates between "discard" and "back" under the user's thumb is worse than one that commits. |
| Opening the TMDB sheet and adding nothing | **Not touched.** Same for cancelling any dialog or sheet. |
| Discarding | No confirmation dialog, and it does **not** go to the trash — a hard delete. |
| An existing list | **Never shows a cross.** The flag is session state and is dropped when the screen is left. |

Three things the design pushed back on, recorded because they are real:

- Discard is a hard delete with no dialog — the only genuinely unrecoverable action in
  the app.
- **System back keeps the list.** A mis-tap plus a back gesture still strands an
  "Untitled list". The cross only helps people who see the cross.
- `close` now means two things — discard here, cancel-renaming on an established list.
  The cancel-renaming affordance was removed on new lists to keep them apart, but this
  is the weakest seam in the design.

### Decided here, against the design

The design proposes an Undo snackbar after discarding. **We show none.** An untouched
list has no name and no items by definition, so there is nothing to restore — "undo"
would create a fresh empty list, which is precisely what pressing + does. A snackbar
offering to recover nothing is theatre, and the design itself admits the undo is gone
the moment it times out.

---

# Decided against the design, from using it on a device (2026-08-05)

## The selected card carries a checkbox and nothing else (against §11)

§11 kept the full-width tint behind the selected card, demoted to "a scanning aid".
On a device it does not survive that demotion. The tint is a square-cornered band the
full width of the screen sitting behind a card with a 16dp radius, so its corners stick
out past the card on all four sides and it reads as something showing through from
underneath rather than as a property of the card. The checkbox already states the
selection without ambiguity, and two signals where one is unambiguous is one too many.

The tint is gone. A selected card differs from an unselected one only by its checkbox.

## The heading stays while selecting (against §3 and §11)

§3 says the count "replaces 'Your lists' and the summary line entirely". Built, that
made entering selection mode yank the whole list upward — under the finger still
resting on the card it had just long-pressed — and left the first card flush against
the contextual bar with nothing between them.

The heading and summary now stay put in selection mode. Only the bar above them
changes: close on the left, the count as its title, delete at the trailing edge. The
FAB still hides.

Search keeps the design's behaviour, because there the field genuinely replaces the bar
and a heading beside a search field is redundant. The difference is that search swaps
one control for another, while selection only re-labels what is already there.

## Nothing is seeded (against nothing — this was never designed, only built)

`loadTierListsForPresentation` used to create "Sci-fi films" and "Every A24 film"
whenever it found no lists. That made the two demo lists impossible to remove — delete
them and they returned on the next read — and it meant the empty state in §5 could
never appear, because there was never a moment with an empty database. The seeding is
gone. An empty library is a legitimate state and the empty state says so.

Existing installs still hold whichever demo lists they created earlier; deleting them
now makes them stay deleted.

## The theme control measures instead of assuming (extends §7)

§7 gives the theme choice as a three-segment row and adds a rule: stack it into three
48dp rows at a font scale of 1.3× or more, "and in German, Polish and Turkish it will
stack at 100% too".

Two changes to that, both from the device.

**The third label is one word in every language.** It was a phrase — "Follow system",
"Как в системе", "Systemeinstellung folgen" at twenty-four characters — while the other
two segments were single words. A third of a 360dp row is about 109dp, less 18dp for the
check, which is roughly twelve characters at 13sp; the phrase never had a chance. It is
now System, Системная, Systemowy, Système, Sistema, Sistem, and so on: parallel with
Light and Dark, and the caption under the heading already explains what it means. The
caption was reworded to match, since it quoted the old label.

**Whether the three fit is measured, not guessed.** A hardcoded font-scale threshold is
a bet on the length of eleven translations plus every one added later, and the design
lost that bet already — it named three languages that would stack at 100% and the built
app overflowed in six. The control measures the widest label against the width a segment
would actually get and lays itself out accordingly, so the rule holds for a language
nobody has thought of yet.

The stacked form is as §7 describes: three rows, minimum 48dp, check moved to the
leading edge, the outline and rounded outer ends kept so it still reads as one control.
Both branches are covered by tests that assert the geometry — side by side at the
default scale, stacked at 2× — rather than the wording.

## Arabic is not offered, and the app is left-to-right (against §7)

§7 lists eleven languages, Arabic among them, labelled "right-to-left". It shipped and it
looked wrong: every icon in this project is a hand-drawn `Canvas` path, and Compose mirrors
layout and `Painter`-backed icons for RTL but not raw drawing commands. The screen flipped
while the back arrow and every chevron kept pointing the way they do in English.

Arabic is removed rather than half-supported. Ten languages remain.

`android:supportsRtl` goes to `false` with it, which is the part worth writing down: layout
direction follows the **device locale**, not the app's own resources. Dropping `values-ar`
alone would still have left a phone set to Arabic mirroring the whole app while showing
English text — the same broken screen, reached a different way. Off is the honest state.

Both flip back together when the icons learn to mirror: `VectorIcon` is the single place
that draws them, so a layout-direction-aware flip belongs there, opted into per icon, since
a plus or a trash can must not mirror.

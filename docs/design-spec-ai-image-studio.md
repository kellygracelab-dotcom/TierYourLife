# Design spec addendum — AI image generation

Consolidated from the design canvas (`TierYourLife.dc.html`, turn 14: 14a–14k).
Supersedes the AI placement-suggestion concept formerly sketched in 1h, which is deleted.

Behind a stub generator at first build (§7). Everything below reads without live generation.

## 1. Entry point (canvas 14a, 14b)

The AI produces one thing: a pool item with a photo. So the control sits where pool items already come from — the pool header, not the app bar.

The pool header gains a second chip to the right of the existing Add chip:

| | Add (existing) | Generate (new) |
|---|---|---|
| Style | Filled assist chip, primaryContainer | Outlined assist chip, transparent, 1dp `outline` |
| Glyph | 18dp `add` in onPrimaryContainer | 18dp `auto_awesome` in onSurfaceVariant |
| Label | 13/16 w500 in onPrimaryContainer | 13/16 w500 in onSurfaceVariant |
| Height / radius | 32dp / 100dp | 32dp / 100dp |
| Padding / gap | 10dp leading, 12dp trailing, 6dp glyph→label | same |
| Measured width | 69dp | 101dp |
| Opens | TMDB search sheet | AI image studio, full screen |

- 8dp between the two chips. Header padding 16dp sides, 10dp below.
- The count (titleMedium) takes the remaining width and truncates first; both chips hold their intrinsic widths.
- At ≥1.3× font scale the two chips wrap to a second line under the count; the header grows to 96dp.
- The collapsed pool (ranked display mode, 60dp bar) carries both chips as well, with the count ellipsised.
- The app bar is unchanged: `arrow_back`, editable title, `note_add`, `more_vert`.

Why not a sparkle in the app bar: 360dp − 8dp bar padding − 48dp back − 2 × 48dp trailing actions − 4dp gaps = ~192dp of title; a third trailing action leaves ~144dp — a 48dp loss. A bare `auto_awesome` in an app bar reads as "do something clever to this list" — the autofill concept this feature replaced. One entry, not two. What survives from the bar idea: the provenance badge (§6).

## 2. AI image studio — screen anatomy (canvas 14c–14f)

Full screen, pushed. Not a bottom sheet.

| Part | Value |
|---|---|
| App bar | 56dp, `arrow_back` only (48dp), title titleLarge, no trailing actions |
| Caption | labelMedium in onSurfaceVariant, 16dp side padding, 10dp below the bar |
| Conversation | flex 1, 16dp side padding, 12dp between messages, scrolls, anchored to the bottom |
| Composer | insets 8dp top / 12dp sides / 12dp bottom |
| Composer field | min 56dp, 28dp radius, surfaceContainerLow fill, padding 16dp leading / 8dp trailing, input bodyLarge in onSurface, primary caret, hint in `outline` |
| Send button | 40dp circle. Has text: primary fill, 20dp `arrow_upward` in onPrimary. Empty or request in flight: surfaceContainerHighest fill, glyph in `outline` |

`arrow_back` is the only exit and it is safe — nothing is committed on this screen.

### 2.1 User prompt bubble
primaryContainer fill, radius 20 / 20 / 4 / 20, padding 12dp / 16dp, max-width 76%, text bodyLarge in onPrimaryContainer, right-aligned.

### 2.2 AI slot — a card, never prose
| Part | Value |
|---|---|
| Container | surfaceContainerLow, radius 20 / 20 / 20 / 4, padding 12dp, left-aligned |
| Image area | 192 × 256dp (3:4), 12dp radius |
| Meta line | labelMedium in onSurfaceVariant, 10dp above |
| Action row | 8dp above; Add to list filled tonal (primaryContainer / onPrimaryContainer) + Regenerate text button (primary), 4dp apart |
| Buttons | 40dp visual height, 100dp radius, 48dp touch target; Add to list 20dp side padding, text button 12dp |

### 2.3 Generating (shimmer)
Same card, same 192 × 256dp block at 12dp radius, shimmer instead of the image; label "Generating…" at bodyMedium in onSurfaceVariant, 10dp under the block. Shimmer: sweep between surfaceContainerHighest and surfaceContainerHigh, 1200ms loop, ease-in-out. ANIMATOR_DURATION_SCALE == 0 → flat surfaceContainerHighest fill. The composer stays visible but send is disabled while a request is in flight. No cancel button.

### 2.4 Empty state
| Part | Value |
|---|---|
| Icon | 56dp surfaceContainerLow circle, 28dp `auto_awesome` in `outline` |
| Headline | headlineSmall in onSurface |
| Body | bodyMedium in onSurfaceVariant, max-width 264dp, centred |
| Side padding | 24dp; block vertically centred |
| Hints label | labelLarge in onSurface, 28dp below the body block, 10dp above the rows |
| Hint rows | min 48dp, 24dp radius, 1dp outlineVariant border, 16dp padding, bodyMedium in onSurface, full width, 8dp apart |

The three hints are static strings. Tapping one fills the composer and focuses it; it does not send.

### 2.5 Error card
| Part | Value |
|---|---|
| Container | errorContainer, radius 20 / 20 / 20 / 4, padding 14dp top / 16dp sides / 8dp bottom, max-width 288dp |
| Glyph | 20dp `error_outline` in onErrorContainer, 12dp gap |
| Title | titleSmall in onErrorContainer |
| Body | bodySmall in onErrorContainer, 2dp below |
| Action | Try again text button, optically aligned to the text block's left edge |

One message for every failure — no network, timeout, 5xx, safety refusal.

## 3. State machine

empty → generating → result | error; error → Try again → generating (same prompt); result → Regenerate → generating (replaces the card, does not append; the discarded image file is deleted immediately); result → Add to list → naming dialog → pool landing (+ snackbar, Undo) | Cancel → result. Back leaves at any state; nothing is committed. The studio opens empty every time.

Invariant: one generation in flight at a time; Regenerate replaces its own card and deletes the discarded file.

## 4. Naming dialog (canvas 14g)

The manual-entry dialog with one changed string.

| Part | Value |
|---|---|
| Container | M3 alert dialog, 28dp radius, surfaceContainerHigh, 24dp padding, 24dp side inset |
| Title | headlineSmall — "Name this card" |
| Supporting | bodySmall in onSurfaceVariant — reused "Goes into the pool. You can drag it into a tier afterwards." |
| Name field | 56dp, outlined 2dp primary, 4dp radius, label "Name", 20dp below the supporting line |
| Photo row | 18dp below: 52 × 76dp thumb at 8dp radius (carrying the provenance badge) + note bodySmall in onSurfaceVariant, 14dp gap — "The generated image becomes the card's photo." |
| Buttons | 18dp below, bottom-right: Cancel then Add, text buttons labelLarge in primary, 8dp apart |

The field opens empty and focused, Add disabled while empty. No "Choose photo" chip.

## 5. Pool landing (canvas 14h)

The studio closes on Add; the user ends on the tier list. The new tile lands at the front of the pool strip, 52 × 76dp, 8dp radius. Pool count increments. Snackbar (clears the pool sheet): "1 item added to the pool", Undo. Undo deletes the item and its image copy.

## 6. Provenance badge

16dp circle, primaryContainer fill, 10dp `auto_awesome` in onPrimaryContainer, tile top-right, offset −3dp/−3dp. Applies to pool tiles, ranked tiles, and the naming dialog thumb. Persists forever — backed by a column, not a session flag.

## 7. Copy

New keys: ai_chip "Generate"; ai_title "Generate an image"; ai_caption "New cards go into the “%1$s” pool."; ai_empty_title "Describe an image"; ai_empty_body "The AI draws it. Keep the ones you like as cards."; ai_hints_label "Try one of these"; ai_hint_1 "A neon-lit Tokyo street in the rain"; ai_hint_2 "A lone figure on a red desert planet"; ai_hint_3 "A retro VHS cover with bold type"; ai_field_hint "Describe an image"; ai_generating "Generating…"; ai_not_saved "Not saved yet"; action_add_to_list "Add to list"; action_regenerate "Regenerate"; ai_error_title "Couldn't generate that image"; ai_error_body "Check your connection, or try a different description."; ai_name_title "Name this card"; ai_name_photo_note "The generated image becomes the card's photo.".
Content descriptions: cd_ai_chip "Generate an image with AI"; cd_ai_send "Generate"; cd_ai_badge "Generated with AI".
Reused: manual dialog body, "Name", "Add", "Cancel", "Try again", "%d item(s) added to the pool", "Undo".
The three hints are translatable prose, not prompt templates.

## 8. The stub, and the schema (canvas 14k)

Request: send → ~1200ms delay → one of three placeholder images, cycled by Regenerate. No network call, no key, no API surface committed. Error path: airplane mode returns the error state — reachable without a debug switch. Storage: the kept image is copied into internal storage exactly like a picked photo. New column: `source: enum(TMDB, MANUAL, GENERATED)` on the item — the only schema change this feature asks for.

Deliberately not designed yet: editing a prompt; conversation history (the studio opens empty every time); multiple keeps; cost/quota/consent; per-cause error messages.

The seam worth watching: delete-on-regenerate assumes the studio holds exactly one uncommitted image per exchange; a grid of candidates would break it.

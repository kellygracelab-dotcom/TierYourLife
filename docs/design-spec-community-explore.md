# Community: Explore, sections, Following — implementation spec

Written by Claude Design on 26.10.2026 from the file **TierYourLife App Community.dc.html** in Danylo's
Claude Design project, and approved by Danylo the same day ("Claude Design did it great, do the same").
Four parts, kept here in full so nothing lives only in the frames. Sections 9–11 do not exist: the
numbering skips from 8 to 12.

The list screens of the same file (§5, §6.2–6.5, §7.2–7.5) are built separately with the landscape
work; they are kept here for completeness.

Every colour is written as `light / dark`. The colour values come from the app palette; where the app
spec lists a conflict, the screen values win. The drag trash target in the landscape and tablet frames
was drawn with the web's error colours; in the app, use the app's error tokens (§0.1).

---

## 0. Foundations

### 0.1 Colour tokens

| Token | Light | Dark |
|---|---|---|
| surface | #FBF8FF | #121318 |
| surfaceContainerLow (feed cards) | #F3F0F9 | #1F1F25 |
| surfaceContainer (pool, menus, sheets) | #EFEDF4 | #2A2A31 |
| surfaceContainerHighest (placeholders, skeletons, offline strip) | #E7E4ED | #35353C |
| onSurface | #1B1B21 | #E4E1E9 |
| onSurfaceVariant | #46464F | #C7C5D0 |
| outline | #77767F | #91909A |
| outlineVariant (dividers, quiet borders) | #E4E1E9 | #46464F |
| primary | #4A5BAA | #BAC3FF |
| primaryContainer | #DEE0FF | #333E8F |
| onPrimaryContainer | #2C3A80 | #DDE1FF |
| authorChip (container) | #EDEBFA | #23242D |
| errorContainer | #F9DEDC | #601410 |
| onErrorContainer | #8C1D18 | #F9DEDC |
| success | #3F7F55 | #8FD3A3 |
| scrim (sheets, dialogs) | #000000 at 32% | same |

On section art, the same in both themes:

- Tile scrim: a vertical gradient from #000000 at 5% to #000000 at 62%.
- Tile label: #FFFFFF.
- "new this week" line: #FFFFFF at 88%.

### 0.2 Type (Roboto, sp)

| Name | Size/line | Weight | Used for |
|---|---|---|---|
| titleLarge | 22/28 | 400 | top app bars |
| sectionTitle | 28/36 | 400 | section header title |
| titleMedium | 16/24 | 500 | feed headings, error titles |
| titleSmall | 14/20 | 500 | tabs, buttons |
| bodyLarge | 16/24 | 400 | Manage row titles |
| bodyMedium | 14/20 | 400 | body text, menu items |
| body13 | 13/18 | 400 | counts, offline strip |
| tileName | 15/20 | 500 | section tile label |
| cardTitle | 14/18 | 500 | feed card title |
| chipLabel | 13/16 | 500 | topic chips |
| meta | 11/14 | 400 | card author and meta |
| caption | 11/15 | 400 | reason line, "new this week", strip labels |

### 0.3 Motion and haptics

- Cross-fade from skeleton or placeholder to content: 150 ms, FastOutSlowInEasing.
- Skeleton pulse: alpha 1 → 0.55 → 1 over 1400 ms, infinite. One shared InfiniteTransition per screen.
- Pill and strip enter/exit: slide plus fade, 200 ms.
- Long-press: HapticFeedbackType.LongPress.
- Honour the system animator scale. With reduced motion: cross-fades take 90 ms; the pulse becomes a static alpha of 0.75.

### 0.4 Numbers and plurals

- Thousands grouped by locale with NumberFormat (English: narrow no-break space, `2 140`).
- Plurals with pluralStringResource: 1 card / N cards, 1 ranking / N rankings, 1 follower / N followers, 1 list / N lists.
- "rankings" on a card is always takeCount. A list republished with the same cards is never its own card; it only adds to the original's count.

## 1. Shared components

### 1.1 App-level tabs (existing, unchanged)

PrimaryTabRow, h48: Your lists · Community, with a 1 dp outlineVariant divider under it.

### 1.2 Community sub-tabs (new)

- SecondaryTabRow, h48, two equal tabs: **Explore · Following**.
- Label titleSmall; selected onSurface, unselected onSurfaceVariant.
- Indicator 2 dp primary, full tab width. Divider 1 dp outlineVariant.
- Cold start opens Explore. The last tab is restored within a session (rememberSaveable).
- The two tabs are swipeable via a HorizontalPager.

### 1.3 Section tile

- Surface with combinedClickable(onClick, onLongClick), shape 16 dp, clipped.
- Layers, bottom to top:
  1. Base fill: the section's tint (§12). It shows before the art loads, and stays as the placeholder until the art ships.
  2. Art: `Image(painterResource(R.drawable.section_<id>))`, ContentScale.Crop, Alignment.TopEnd. Bundled, no network.
  3. Scrim: as in §0.1.
  4. Label, bottom-start, padding start 12 / bottom 10 / end 10: a row with the Material Symbol (18 dp #FFFFFF), a 7 dp gap and the name (tileName #FFFFFF); under it the caption **N new this week** in #FFFFFF at 88%, hidden when N is 0 or unknown.
- Pressed: standard ripple.
- Long-pressed: scale 0.97, plus a ring made of a 3 dp gap in surface and a 2 dp primary line. It holds while the menu is open.
- contentDescription: "Food, 22 new this week", or "Food" when the line is hidden.
- Tap opens the Section page (§3).

### 1.4 Feed list card

- Card, shape 16 dp, surfaceContainerLow, elevation 0, onClick.
- Cover: 132 dp on phones in 2 columns (other sizes per frame). One of three kinds:
  - One image: AsyncImage, Crop.
  - Collage: a 3 × 2 grid of the first 6 images, 1 dp outlineVariant gaps; used when there are 6 or more images and no cover is set.
  - Tier stripes: five equal horizontal bands in the list's tier colours; used when the list has no images.
  - While loading: flat surfaceContainerHighest, then a 150 ms fade-in. Collage cells load independently.
- Body, padding 10 top / 12 sides / 12 bottom:
  - Optional reason line: caption onSurfaceVariant, 3 dp bottom gap, on the Following feed only (§4.1).
  - Title: cardTitle onSurface, at most 2 lines with ellipsis, min height 36.
  - Author row, 8 dp above, min height 24: avatar 20 dp circle (placeholder surfaceContainerHighest), 6 dp gap, name in meta style onSurfaceVariant on one line.
  - Meta, 4 dp above, meta style in outline: `34 cards · 2 140 rankings`.
- Tap opens the community list. Long-press opens the app's existing list menu (Report / Hide).

### 1.5 Skeleton card

- Same outer size and shape as 1.4. Cover: a 132 dp block in surfaceContainerHighest.
- Body padding 10/12/12, column gap 7: bar 85% × 13 (r4); bar 50% × 13 (r4); row with 3 dp above: circle 20, 6 dp gap, bar 56 × 10 (r4); bar 110 × 10 (r4).
- Every block uses surfaceContainerHighest, all pulse together (§0.3). No shimmer and no spinner.

### 1.6 Buttons

- **Follow**: FilledTonalButton, colours primaryContainer / onPrimaryContainer. h40, horizontal padding 20, titleSmall, minWidth 120.
- **Following**: OutlinedButton, 1 dp outlineVariant border, content onSurface, leading check 18 onSurfaceVariant, minWidth 120.
- Compact variants: h36 (topic row, minWidth 104, label 13/18); h32 (collapsed header and Manage rows, padding 12, label 13/18, check 16).
- Follow tap: optimistic. The button and the follower count update in the same frame. On failure both revert, with the snackbar **Couldn't follow. Try again.**
- Following tap: unfollows immediately, no dialog. Snackbar **Unfollowed Food · Undo**, 6 s.

### 1.7 Topic chip

- FilterChip, h32, shape 8 dp, chipLabel.
- Unselected: 1 dp outline border, label onSurfaceVariant.
- Selected: container primaryContainer, label onPrimaryContainer, leading check 16.
- Waiting chip (custom): like the unselected chip but with a dashed 1 dp outline border; leading schedule 16 outline; label **Shiny hunts · waiting**.
- Suggest chip: AssistChip look, outline border, leading add 16 primary, label in primary: **Suggest a topic**.

### 1.8 Snackbar

Standard M3 SnackbarHost, at the bottom above the nav bar. One at a time; a newer one replaces the current one.

## 2. Explore (phone portrait, 390 × 844)

### 2.1 Explore: default

Top to bottom:

- Status bar 24.
- TopAppBar h56: title **TierYourLife** in titleLarge onSurface, start padding 12; actions are IconButtons (48 dp touch, 24 dp icons, onSurfaceVariant): search and settings.
- App tabs (1.1), h48.
- Community sub-tabs (1.2), h48, Explore selected.
- One LazyVerticalGrid(GridCells.Fixed(2)) holding everything below:
  - Tiles: content padding 16 top / 16 sides, 10 dp spacing both ways. Each tile fills its column (about 173 wide) at height 96. Order: Anime, Film & TV, Games, Music, Books, Food, Sport, People. Then **Other** spanning both columns (GridItemSpan(2)) at height 56, with no "new" line and the label vertically centred.
  - Heading (span 2): **Popular this week**, titleMedium onSurface, padding 22 top / 16 sides / 10 bottom.
  - Feed: cards (1.4), 12 dp spacing, padding 0 16 16. Paging with page size 20 and prefetch distance 6; the next page's covers are prefetched with Coil enqueue.
- Nav handle.

Sizing check: the tiles end at about y 672, so the heading and the top of the first card row show above the fold.

Taps: tile → Section page (§3); card → community list; search → Community search; settings → Settings. Pull to refresh is a PullToRefreshBox around the grid (§13).

### 2.2 Explore: scrolled

- The TopAppBar uses `TopAppBarDefaults.enterAlwaysScrollBehavior()`: it scrolls off and comes back on the first upward scroll.
- Both tab rows stay pinned, outside the scrolling grid, under the collapsing app bar.
- The tiles scroll away with the content.

### 2.3 Explore: long-press on a tile

- The tile goes into its long-pressed state (1.3), with haptics.
- A DropdownMenu opens, anchored under the tile and start-aligned: container surfaceContainer, shape 12 dp, tonal elevation 3, 4 dp vertical padding, width 190; items h48, leading icon 22 dp onSurfaceVariant, label bodyMedium onSurface.
- Items: **Follow Food** (add) and **Open Food** (arrow_forward). When followed, the first item reads **Unfollow Food** (remove).
- Follow applies optimistically, then shows the snackbar **Following Food**. Unfollow shows **Unfollowed Food · Undo**. Open does the same as a tap.
- On dismiss the tile returns to normal over 120 ms.

### 2.4 Explore: first open, no cache

- Bars, tabs and tiles paint on the first frame (bundled). The "new" lines are hidden until counts arrive.
- Under Popular this week: 6 skeleton cards (1.5).
- When data arrives each skeleton is replaced in place with a 150 ms cross-fade; covers fade in as they load; tile "new" lines fade in when their counts arrive.
- No spinner anywhere.

### 2.5 Explore: back to the tab, held update (New lists pill)

- The cache shows immediately (§13).
- If the refresh brings new data and the person has scrolled (firstVisibleItemIndex > 0), the new data is held and a pill appears: overlay, horizontally centred, 12 dp below the pinned tab rows; h40, shape 20 dp, primaryContainer, shadow elevation 3; padding 12 start / 16 end; arrow_upward 18 dp, 6 dp gap, **New lists** (titleSmall), all onPrimaryContainer; enters with a slide down from −8 dp plus fade, 200 ms.
- Tap: animateScrollToItem(0), apply the held data, hide the pill.
- If the person scrolls back to the top on their own, the update applies and the pill hides.
- If they had not scrolled, no pill: the data swaps in place.

### 2.6 Explore: offline with cache

- All content stays.
- A strip directly under the sub-tabs, pinned with them: h40, full width, surfaceContainerHighest, padding 16 start / 4 end; cloud_off 18 onSurfaceVariant, 10 dp gap; body13 onSurface, one line with ellipsis: **Offline · showing lists from 2 hours ago** (DateUtils.getRelativeTimeSpanString); TextButton **Retry** in primary.
- Shown only when the last fetch failed and the cache is more than 1 h old. Slide in/out 200 ms.
- Retry fetches again; disabled while the request runs, no spinner.
- Clears on any successful fetch, including the automatic retry when the network comes back.
- A failed pull-to-refresh (any cache age) shows the snackbar **Couldn't refresh. Showing lists from 14:20. · Retry** instead of the strip.

### 2.7 Explore: failed, no cache

- Tiles work as normal, without the "new" lines. A section opened from here shows this same state.
- The heading and feed are replaced by a block, padding 20 top / 16 sides: a row with cloud_off 20 outline, 10 dp gap, **Couldn't load community lists** (titleMedium onSurface); 6 dp below **Check your connection. Your own lists are on this device.** (body13 onSurfaceVariant); 8 dp below an OutlinedButton with refresh 18 and **Try again**.
- Retries automatically on ConnectivityManager.NetworkCallback.onAvailable.
- An App Check or token failure shows this same state; the user never sees the term.

## 3. Section page (phone portrait)

### 3.1 Section: Games, default

- Status bar 24, drawn over the header image (edge-to-edge, light status-bar icons).
- Collapsing header, expanded height 200 dp counted from the top of the status bar:
  - Background: a collage of the section's top 4 list covers by takeCount, as 4 equal columns with 1 dp gaps. Only lists with ≥ 5 rankings and no open reports qualify. If fewer than 4 qualify, the section's tile art (§12) with Crop.
  - Scrim: vertical gradient over the whole header, #000000 at 35% → 5% (at 35% height) → 68%.
  - Action row at the top, 4 dp inset: arrow_back · spacer · search · more_vert; 48 dp touch, icons 24 dp #FFFFFF.
  - Bottom block, 16 dp from start, end and bottom, a row aligned to the bottom with a 12 dp gap: a column with **Games** (28/36 400 #FFFFFF) and **3 410 followers · 812 lists** (body13 #FFFFFF at 92%); the Follow button (1.6, minWidth 120), theme colours.
- Topic chips row: LazyRow, padding 12 top / 16 sides / 4 bottom, 8 dp spacing. Order: All (selected) · approved topics by follower count · the viewer's own waiting topics (dashed, 1.7) · Suggest a topic last.
- Sort row, h44, padding 16 start / 4 end: start **812 lists** (body13 onSurfaceVariant); end a TextButton **Popular** (titleSmall onSurface) with trailing arrow_drop_down 22 dp onSurfaceVariant, opening a DropdownMenu with Popular and Newest, a check on the current one. Popular = takeCount (last 30 days on the server); Newest = publish date. The choice is remembered per section for the session.
- Feed: 2 columns, cards from 1.4, spacing 12, padding 0 16 16. No reason lines.

Collapse: on scroll the header shrinks to a 64 dp bar on surface: arrow_back · **Games** (titleLarge onSurface) · a compact h32 Follow or Following · more_vert. Icons change from #FFFFFF to onSurfaceVariant; status-bar icons turn dark in light theme. The image and scrim fade out across the last 40% of the collapse. The chips row is part of the scrolling content, not pinned.

Taps: Follow → 1.6; a chip filters the feed in place (3.2) — it pushes `community/section/games?topic=pokemon` with launchSingleTop, so Back clears the filter first; Suggest a topic → the existing Suggest-topic sheet; more_vert → Share section · Report a problem; search → Community search scoped to the section; card → community list. Deep links: `tyl.app/s/games`, `tyl.app/s/games/t/pokemon`.

### 3.2 Section with a topic selected (Pokémon)

- The header is shown collapsed (64 dp bar).
- Chips row: Pokémon selected; the row keeps the selected chip fully visible (animateScrollToItem with offset −48 dp).
- 8 dp spacer, then the topic row: h56, padding 0 16, 12 dp gap, 1 dp outlineVariant divider below. Topic square 36 × 36, r10, filled with the section's tint (§12) holding the section icon 18 dp #FFFFFF; a column with **Pokémon** (titleSmall onSurface) and **210 followers · 64 lists** (bodySmall 12/16 onSurfaceVariant); Follow or Following compact h36 (1.6) — this follows the topic, not the section.
- Sort row: **64 lists · Popular ▾**. Feed: only lists in this topic.
- Tapping All removes the topic row with an animated height collapse (200 ms) and restores the section feed with its scroll position.

### 3.3 Waiting topic: the proposer's sheet

- Trigger: the proposer taps their own waiting chip. Nobody else sees the chip.
- ModalBottomSheet, container surfaceContainer, top corners 28 dp, drag handle 32 × 4 in outline at 60%, scrim §0.1. Content padding 24 sides / 40 bottom.
- Title **Shiny hunts**, titleLarge onSurface, 18 dp below the handle.
- Status pill, 10 dp above / 14 below: h24, r12, surfaceContainerHighest, padding 0 10; schedule 14 plus **Waiting for a moderator** (11/16 500 onSurfaceVariant).
- Body bodyMedium onSurfaceVariant: **A moderator will look at it. Your lists show here once it's approved; until then they're in Games.**
- 10 dp below, bodySmall outline: **Only you see this topic until then.**
- Actions end-aligned, 22 dp above: a filled-tonal **OK** that dismisses.
- Approved: the chip turns normal on the next load; the backend moves the lists. Declined: the chip disappears; the inbox line **"Shiny hunts" wasn't added as a topic. Your lists stay in Games.** (no system push).

## 4. Following

### 4.1 Following: feed

- Top app bar and app tabs as in 2.1; sub-tabs with Following selected.
- One LazyColumn or grid. First item, the followed strip: LazyRow, padding 14 top / 16 sides / 12 bottom, 12 dp spacing, 1 dp outlineVariant divider under it. Items 64 dp wide: a 56 × 56 visual, 6 dp gap, label caption onSurface, one line, centred.

| Kind | Visual | Tap opens |
|---|---|---|
| Section | r14, section art (or tint fill), section icon 22 dp #FFFFFF over a 30% black scrim | Section page |
| Topic | r14, topicBg (§12) with the section icon 22 dp in topicIcon | Section page, topic selected |
| Waiting topic (proposer only) | r14, dashed 1.5 dp outline border, transparent, schedule 22 outline | 3.3 sheet |
| Person | circle 56, their face, or the letter on primaryContainer (24 500 onPrimaryContainer) | Author profile |
| Manage (always last) | circle 56, 1 dp outline ring, tune 22 primary; label **Manage** in primary | 4.3 |

- Order: sections, topics (waiting after approved), people by recent activity, Manage.
- 14 dp gap, then the merged feed: 2 columns, cards from 1.4, newest first. Each card carries one reason line above the title: **In Food** (followed section); **Pokémon** (followed topic, the name only); nothing for a followed person. Priority when several match: person, topic, section.
- Loading and failure follow §13 with this tab's own cache; pull to refresh as on Explore.

### 4.2 Following: empty (follows nothing)

- Block, padding 28 top / 24 sides, 8 dp gap: group 36 dp outline; **You aren't following anything yet** (20/26 400 onSurface, 4 dp above); **Follow a section, a topic or a person and their new lists arrive here first.** (bodyMedium onSurfaceVariant).
- Mini tiles, 20 dp above, padding 0 24, 3-column grid, 8 dp spacing; tiles 84 tall, r14, label 13/18 500, icon 16, 10 dp padding, no "new" line. In the top-end corner (8 dp inset) a 28 dp circle in surface with add 18 primary. Tap follows the section: the + becomes check in success, no snackbar. Long-press opens the section.
- Helper, 14 dp above, padding 0 24, bodySmall onSurfaceVariant: **Tap + to follow a section. Topics are inside each section; people, on their profile.**
- After the first follow the tab switches to 4.1; the strip shows the section, the feed shows skeletons until it loads.
- Second empty case (follows things, nothing published): the strip as normal; where the feed would be, centred bodyMedium onSurfaceVariant with 28 dp padding: **Nobody you follow has published anything yet.**, then a TextButton **Go to Explore** that switches the sub-tab.

### 4.3 Manage following

- TopAppBar h60: arrow_back · **Following** (titleLarge) · more_vert.
- LazyColumn in three blocks. Headings padding 16 top / 16 sides / 4 bottom, titleSmall primary, with the count: **Sections · 2**, **Topics · 2**, **People · 2**.
- Rows: M3 ListItem, two lines, h72, padding 0 16, 16 dp gap.

| Row | Leading (48 × 48) | Headline (bodyLarge onSurface) | Supporting (bodyMedium onSurfaceVariant) | Trailing |
|---|---|---|---|---|
| Section | r12, tint fill + section icon 20 #FFFFFF | Food | 4 120 followers | compact Following (h32) |
| Topic | r12, topicBg + icon 20 topicIcon | Pokémon | In Games · 210 followers | compact Following |
| Waiting topic | r12, dashed outline, schedule 20 | Shiny hunts | In Games · waiting for a moderator | none |
| Person | circle 48, face | mariia | 6 lists · 380 followers | compact Following |

- Tapping Following unfollows and shows **Unfollowed Food · Undo**. The row stays, its button changed to Follow, until the screen is left, so Undo has something to point at.
- Tapping a row opens the section, topic or person. An empty block hides its heading.

## 5. Board metrics and tier colours (list screens; built with the landscape work)

### 5.1 Board metrics by window class

| Metric | Compact height (< 480) | Phone portrait (existing) | 600+ width, height ≥ 480 |
|---|---|---|---|
| Band width | 64 | 74 | 80 (72 with a list pane beside the board) |
| Occupied row min height | 72 | 84 | 96 |
| Empty row min height | 56 | 84 | 72 |
| Card in a row | 40 × 58, r6 | 44 × 64 | 52 × 76, r6 |
| Card gap / row padding | 6 / 7 | 8 / 8 | 8 / 10 |
| Gap between rows | 6 | 8 | 8 |
| Row shape | r12 | r12 | r12 |
| Band letter | 20/24 500 | existing | 26/30 500 |
| Band caption | 9/11, hidden on empty rows at 56 | existing | 10/13 |
| Pool card | 52 × 76, r6 | existing sheet | 52 × 76 |

Text-only cards: container surfaceContainerHighest, 1 dp outlineVariant inset border, 4 dp padding; label 8/10 500 on 40 dp cards and 10/12 500 on 52 dp cards, onSurfaceVariant, centred, at most 3 lines; break only between words with hyphenation on (Hyphens.Auto, LineBreak.Paragraph).

Empty row body: **Drop items here**, bodySmall 12/16 onSurfaceVariant at 70%, 9 dp start padding; only on rows that are empty while the pool has cards.

### 5.2 Tier colours

| Tier | Caption | Band light | Row light | Band dark | Row dark |
|---|---|---|---|---|---|
| S | Masterpiece | #B03A32 | #F7E4E2 | #F1948C | #3A1F1D |
| A | Great | #C06A25 | #F8EBDC | #E9A867 | #382518 |
| B | Good | #A98B1F | #F5EFD9 | #D8C05A | #332F1B |
| C | Watchable | #3F7F55 | #E1EFE5 | #8FD3A3 | #1C2E22 |
| D | No | #3C6E99 | #E0EAF3 | #8FC3E8 | #1B2733 |

Text on the band: light theme letter #FFFFFF, caption #FFFFFF at 85%; dark theme letter and caption #121318, caption at 80%.

## 6. Phone landscape (844 × 390, compact height)

Shell: status bar 24; NavigationRail 80 dp at the start on surface (FAB New list 56, r16, primaryContainer, add; then Lists · Community · Settings; active indicator 56 × 32 pill in primaryContainer); content fills the remaining 764 dp; nav handle 16.

### 6.1 Explore

- One bar, h48, 1 dp outlineVariant divider, scrolls away with enterAlways. Padding 20 start / 4 end: **Community** (20/28 400 onSurface) · 12 dp gap · inline SecondaryTabRow Explore · Following (each tab hugs its label, 16 dp padding, indicator inset 12) · spacer · search.
- Tiles: one horizontal LazyRow, padding 12 top / 16 sides, 10 dp spacing, tiles 160 × 88, all 9 in the usual order.
- Heading **Popular this week**, padding 16 top / 16 sides / 8 bottom.
- Feed: 4 columns, cover 96, spacing 12, padding 0 16 16.

### 6.2 Community list (someone else's list, ranking a copy)

- Bar h48, 6 dp gap, 1 dp divider; scrolls away with the rows and returns on the first upward scroll: arrow_back (48 touch); title 16/22 500 onSurface, max width 200, ellipsis; author chip h28, full radius, authorChip, padding 3 start / 8 end, avatar 16, `danylo · Film & TV` (bodySmall primary), chevron_right 14 primary, tap opens the profile; spacer; segmented button h32 Theirs · Mine (selected primaryContainer with check 16, unselected onSurfaceVariant, border outline); FilledTonalButton h36 with bookmark_add 18 and **Save to my lists** (13/18); more_vert (40 touch).
- Body: the board (weight 1) and the pool column. Board padding 6 top / 8 end / 12 start, compact metrics, vertical scroll. Pool column width 216, margin 8 top / 8 end / 8 bottom, surfaceContainer, r16, padding 10 / 12, 8 dp gap: head **Pool · 22** (titleSmall) · spacer · right_panel_close 20 (collapses); status line caption onSurfaceVariant **Your copy · saved on this phone only**; cards 52 × 76 in a 3-column flow, 8 dp gap, own vertical scroll.
- Theirs shows the author's arrangement read-only and hides the pool; Mine is the visitor's copy. Save to my lists copies the list; the button becomes **Saved** (check) and the status line **Saved to your lists**. Tap a pool card to select it (2 dp primary ring), then tap a row; dragging also works. more_vert → Share · Report this list · Hide this list.

### 6.3 Own board, editing

- Bar: arrow_back · title (max width 320) · spacer · undo · note_add · more_vert (40 touch, 22 dp icons).
- Pool column as 6.2 without the status line; under the head an **Add a card** field: h40, r20, surface, 1 dp outline border, padding 12 start / 10 end; add 20 onSurfaceVariant · Add a card (body13 outline) · auto_awesome 18 primary. Tapping opens the existing add flow as a full-height side sheet (width 360) from the end edge; auto_awesome opens the AI studio.

### 6.4 Own board, dragging

- Lifted card: scale 1.08, rotation −4°, shadow elevation 8. Target row: 2 dp primary inset border with a dashed slot (2 dp primary, r6, card-sized) where the card will land.
- Pool head becomes the trash target: h48, r12, errorContainer, delete 20 + **Drop to delete** (13/18 500 onErrorContainer); caption **Drop on the cards below to unrank**.
- Drop: on a row moves; on the trash deletes with **Card deleted · Undo**; on the pool's cards unranks; elsewhere springs back in 160 ms. Auto-scroll 48 dp zones; haptic tick on pickup and on each row change.

### 6.5 Own board, pool empty

- The column collapses to a 56 dp strip: margin 8, r16, surfaceContainer, padding top 8, 10 dp gap, centred: right_panel_open (40 touch), check_circle 20 success, **0** (12/16 500), add (40 touch, primary).
- Collapses automatically at 0, reopens when a card is added or on expand. A manual choice is saved per device (poolColumnCollapsed) and wins until the pool count changes. Width animates 200 ms.

## 7. Tablets

### 7.1 Explore, 1280 × 800

- Rail 80. Top bar h64, padding 24 start / 12 end: **Community** (titleLarge) · spacer · search. Sub-tabs inline under it, padding start 12, 1 dp divider.
- Content max width 1080, centred, padding 20 top / 24 sides: tiles in a row of 5 (Anime, Film & TV, Games, Music, Books) then 4 (Food, Sport, People, Other), height 112, 12 dp spacing, Other without "new" line; heading **Popular this week**, padding 26 top / 12 bottom; feed 4 columns, cover 140, spacing 16.

### 7.2–7.5 List screens on tablets

- 7.2 Community list 1280 × 800: no list pane; header h64 (arrow_back · titleLarge · author chip h32 · spacer · Theirs/Mine h40 · Save to my lists · ios_share · more_vert); board padding 12 / 16 / 24, tablet metrics; pool card width 300, as tall as its content, head Pool · 22 (titleMedium), status **Your copy · saved on this tablet only**, cards 52 × 76 in 4 columns, 10 dp gap.
- 7.3 Own board 1280 × 800: rail, Your lists pane 320 (header h64, rows h64 r16 with 44 × 44 thumbnails, selected row primaryContainer), board band 72, pool card width 268; while dragging its head is the trash target with **Drop on the pool to unrank**.
- 7.4 Community list 800 × 1280: header h72 with title (20/26) and author text stacked; board full width; pool docked at the bottom (surfaceContainer, top corners 24, padding 12 / 24 / 18, handle 32 × 4; head Pool · 22 + status; cards wrapped, at most 2 rows (162 dp); the handle resizes 1/2/4 rows, saved per device).
- 7.5 Own board 800 × 1280: as 7.4 with header h64 (arrow_back · title · undo · note_add · more_vert); docked pool head Pool · 6 · spacer · Add a card field at 300; dragging turns the head into the trash target.

## 8. Flip6 cover screen (360 × 380)

### 8.1 Sections on the cover

- No status or nav bar; display corners r44, keep content in the safe area.
- Title row h36, padding 0 16: **Community** (15/20 500 onSurface).
- Grid padding 0 12, 3 × 3, 6 dp spacing; tiles about 108 × 84, r14, padding 9, label 12/16 500, icon 16, no "new" line. No scrolling. Tap opens 8.2; long-press does nothing.

### 8.2 A section on the cover

- Bar h44, padding 0 4: arrow_back (40 touch, 22 dp) · **Games** (15/20 500).
- LazyColumn of compact rows, h72, padding 0 14, 12 dp gap: cover 48 × 64 r8 (the three kinds of 1.4), title 13/18 500 one line, author 11/15 onSurfaceVariant, meta 11/15 outline.
- After the first 3 rows (6 dp above, padding 0 14): tonal button h40 with open_in_full 18 and **Open on the main screen** (13/18). It sends the section to the main display (ActivityOptions.setLaunchDisplayId, or the app's handoff), then shows **Open the phone to continue**. Tapping a row also hands off and opens that list. Ranking never happens on the cover.

## 12. The nine sections

The order is fixed everywhere (tiles, Manage, search filters). The id is the backend category key; the name is a localised string.

| # | id | Name (EN) | Material Symbol |
|---|---|---|---|
| 1 | anime | Anime | animation |
| 2 | film_tv | Film & TV | movie |
| 3 | games | Games | sports_esports |
| 4 | music | Music | music_note |
| 5 | books | Books | menu_book |
| 6 | food | Food | restaurant |
| 7 | sport | Sport | sports_soccer |
| 8 | people | People | person |
| 9 | other | Other | category |

Four colours per section, computed in OKLab from one tint: **tint** (tile base and placeholder in light theme, section square fill in Following and Manage); **tint dark** (the tint mixed 78% with #121318); **topicBg** (light 22% tint over #FBF8FF; dark 32% tint over #121318); **topicIcon** (light the tint itself; dark the tint lifted to L 0.82).

| id | tint | tint dark | topicBg light | topicBg dark | topicIcon light | topicIcon dark |
|---|---|---|---|---|---|---|
| anime | #7E4E74 | #64405E | #DFD0DF | #322533 | #7E4E74 | #E7B0DA |
| film_tv | #3E4A6C | #343D58 | #CECFDD | #1F2431 | #3E4A6C | #B4C3EC |
| games | #3D6B58 | #345649 | #CFD8D8 | #202D2B | #3D6B58 | #A0D1BB |
| music | #62508A | #4F426F | #D7D1E5 | #292539 | #62508A | #CAB8FA |
| books | #7A5B3F | #614A37 | #DED4D2 | #302925 | #7A5B3F | #E0BD9E |
| food | #9A5638 | #794732 | #E7D3D1 | #3A2824 | #9A5638 | #FBAF8F |
| sport | #2F6A76 | #2A5560 | #CDD7DF | #1D2D34 | #2F6A76 | #94D0DD |
| people | #76586A | #5E4857 | #DDD3DC | #2F2830 | #76586A | #DBB9CD |
| other | #55566A | #454656 | #D4D2DC | #262730 | #55566A | #C0C2D9 |

Art does not change with the theme. The tile label is always #FFFFFF over the scrim; only the placeholder base under the art switches between tint and tint dark.

### 12.1 Art files

- Generate at 1440 × 900 (16:10). Ship as WebP quality 82 in `res/drawable-nodpi/section_<id>.webp`, about 60–90 KB each.
- The tile crops with ContentScale.Crop anchored at top-end, so the busy top-right part stays visible and the quiet bottom-left sits under the label.
- Until the art is generated: the tint fill with a hatch overlay, 45° stripes of #FFFFFF at 7%, 4 dp wide, every 10 dp.

### 12.2 AI-studio prompts

Shared style, prefixed to every prompt verbatim:

> Flat editorial illustration with soft paper grain, three or four muted colours and one warm accent, gentle light from the top right. Objects and places only: no people's faces, no text, no logos, no brand characters, no recognisable products. Composition weighted to the top and right; the bottom-left 40% calm and low in detail (plain floor, wall or sky). 16:10.

| id | Subject |
|---|---|
| anime | A rooftop at dusk with laundry lines, a bicycle and a distant train on a viaduct, cel-shaded clouds. Muted plum and dusk blue, warm accent on the train windows. |
| film_tv | A dark cinema seen from the back row, the projector beam crossing the room, a paper popcorn box on a seat. Deep navy, warm accent in the beam. |
| games | A desk at night lit by a CRT glow, a generic rounded controller, a stack of unlabelled cartridges. Dark green and teal, warm accent from a desk lamp. |
| music | A record player on a low cabinet, a sleeve with abstract shapes, a speaker and a plant. Violet and grey, warm accent on the record label. |
| books | A tall shelf of mixed-colour spines, a library ladder, an open book on an armchair. Brown and cream, warm accent from a reading lamp. |
| food | Top-down table: a bowl of noodles, dumplings, chopsticks, half a lemon, steam. Terracotta and linen, warm accent on the lemon. |
| sport | An empty pitch at golden hour, white line markings, a plain ball at rest near the bottom right. Teal and grass green, warm accent in the low sun. |
| people | Overlapping silhouettes as soft coloured shapes under a single spotlight, no faces. Dusty rose and grey, warm accent in the spotlight. |
| other | A cabinet of curiosities: a globe, a shell, a brass key, a teacup, a small cactus. Slate and sand, warm accent on the brass. |

Before approving an image: put all nine into the 2-column phone grid at 173 × 96 and check them as one set; the label must stay legible at 96 dp tall in both themes; reject any face, logo, readable text, recognisable character or product; the bottom-left 40% must be calm.

## 13. Loading and cache checklist

1. Bundled UI paints on the first frame: bars, tabs, rail, the nine tiles (art and names), section names. None waits for the network.
2. No spinners, except the pull-to-refresh indicator.
3. Cache: the last good response per feed (Explore, Following, each section, each section-plus-topic) in Room with a fetchedAt timestamp. Keep the first page (20 items) per feed; evict feeds not opened in 14 days.
4. Images: Coil with memory and disk cache (disk 150 MB). Placeholder flat surfaceContainerHighest with a 150 ms fade-in; no per-image shimmer. Prefetch the next page's covers while scrolling.
5. Warm-up at app start: in the background (expedited, network-constrained), fetch the App Check token and make one cheap request to the feed function.
6. First open ever: real tiles plus 6 skeleton cards; data replaces them in place with a 150 ms cross-fade; tile "new" lines appear when counts arrive.
7. Coming back to the tab or a section: show the cache immediately. If fetchedAt is more than 5 min old, refresh silently. Not scrolled: apply in place. Scrolled: hold and show the New lists pill.
8. Pull to refresh: PullToRefreshBox, cached content kept visible. Success replaces the content. Failure: snackbar **Couldn't refresh. Showing lists from 14:20. · Retry**.
9. Failure with cache: content stays; the offline strip (2.6) only when the cache is older than 1 h; it clears on success.
10. Failure without cache: the error block (2.7); tiles still work.
11. Automatic retry on NetworkCallback.onAvailable, for any feed in a failed state, at most once per 10 s.
12. App Check or token failure is a network failure (states 9 and 10); log it, never show the term.
13. Optimistic writes: follow, unfollow and topic follow apply locally first; on failure they revert with the snackbar from 1.6. Queue while offline, send on reconnect.

Optional, Danylo's decision: `minInstances: 1` on the feed Cloud Function in europe-west1 removes the 2–5 s cold start for a small monthly fee. Rules 1–13 hide most of the wait without it.

## 14. Window-size checklist

1. Decide layout by window size class, never by device model; re-read on every configuration change, including fold and unfold.
2. Navigation: width < 600 uses the bottom app tabs; width ≥ 600 or height < 480 uses the 80 dp rail.
3. Compact height (< 480 dp): pin only the rail and the pool column; all headers become one 48 dp bar that scrolls with the content and returns on the first upward scroll; the author is a chip in the bar, the status line goes under the pool title; rows use compact metrics.
4. Pool in compact height: a 216 dp column at the end edge, open while cards are unranked, collapsing to a 56 dp strip at 0 or by hand; no widths in between; the manual choice saved per device.
5. Pool at width ≥ 840 and height ≥ 480: a column card 300 wide (268 beside the Your lists pane), as tall as its content.
6. Pool at width 600–839 and height ≥ 480: docked at the bottom, at most 2 rows, resizable to 1, 2 or 4 rows.
7. Pool in phone portrait: the existing bottom sheet.
8. Own boards at width ≥ 840: list-detail (Your lists pane 320 + board). Community lists never get a list pane. Below 840 the pane is a separate screen.
9. Editing: Add a card always in the pool's head; the add flow as a 360 side sheet in column layouts, the existing full-screen flow otherwise.
10. Dragging at all sizes: lifted card 1.08 and −4°; target row 2 dp primary ring with a dashed slot; pool head becomes Drop to delete; dropping on the pool's cards unranks; 48 dp auto-scroll zones.
11. Community Explore: phone portrait 2-column tiles (Other spans both) and a 2-column feed; compact height one bar with inline tabs, one row of tiles at 160 × 88 and a 4-column feed; width ≥ 840 content max 1080, tiles 5 + 4 at 112, 4-column feed; width 600–839 3 × 3 tiles at 120 and a 3-column feed.
12. Section page: collapsing 200 dp header in phone portrait; in compact height the header starts collapsed into the single 48 dp bar; at width ≥ 840 a 200 dp header with content capped at 1080.
13. Flip6 cover (about 360 × 380): Community shows only the 3 × 3 tiles and compact section rows; every list and every ranking action hands off to the main screen.
14. Insets: WindowInsets.safeDrawing on all edges; the rail respects the cutout in landscape; the docked pool and bottom sheets add navigationBars padding; the cover keeps content inside the r44 corners.
15. State survives size changes: selected tab, scroll positions, the picked card, the open pool or strip, the held update with its pill, and an in-progress drag (cancelled cleanly). rememberSaveable and the ViewModel.

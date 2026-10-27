# Walkthrough notes, 25.10.2026

Danylo went through build 355 on his Flip6 and wrote down what to change. One
row per remark, in his order. "Kind" says who decides the shape: a fix is a
code change with an obvious answer; design means Claude Design is asked first.

The designs for 4, 5, 6, 9, 10, 11 and 12 are on the Claude Design canvas
"TierYourLife: прогон 25.10" (nine boards, private), approved the same evening; the
decisions and what is built are in `design-spec-community.md`.

| # | Remark | Kind | Status |
|---|---|---|---|
| 1 | A card should answer the finger at once when dragging; there is no single-tap action, so the long-press delay only costs. Double tap should feel quicker too. | fix (keep a short delay: a vertical scroll and a drag both start with a finger on a card) | done, #154 |
| 2 | Default tier captions (Best, Great, …) go away: a new board shows the letter alone. When a row wraps to two or more lines, the letter sits centred in the band, not top-left. | fix (reverses the default-caption part of #105; captions stay editable) | done, #151 |
| 3 | The system back gesture first shrinks the screen, then leaves; the in-app back button leaves at once. The shrink is the system's predictive-back animation. | fix: opt out of predictive back, or make the pop immediate | done, #149 |
| 4 | Opening a community list shows black for about half a second; "Save to my list" flashes too. Transitions across the whole app should be smooth and not catch the eye. | design: a transition language for the app; fix for the black frame (window background, screen root surface, NavHost transitions) | done, #161: the skeleton from the card, one set of transitions, saving stays on the screen |
| 5 | The bottom panel on a list (pool; Their ranking / Mine; Save to my list) should collapse like a sheet and expand on demand, on own boards and community lists alike. | design + fix | done, #164 (the sheet) and #169 (the switch and the save in its strip) |
| 6 | Cover art: if the person picked 1 picture, show it alone; 2 to 6, a collage of exactly those; 6 is the maximum. With no choice, a collage of up to 6 *distinct* card pictures, fewer when fewer exist, never repeats; 1 picture shows alone; 0 shows the tier-colour stripes. | design (layouts for 2–5; the picker) + fix (no repeats) | done: no repeats #156, the picker and the collage #163, the collage to the feed tieryourlife-proxy#28 and to the account #168 |
| 7 | Catalogue search returns many results without a poster; anything without a picture is dropped from the results. | fix | done, #150 |
| 8 | Where the cover is chosen, add a picture search (the same catalogue search), not only the board's own cards. | feature | done, #163: a search tab in the picker |
| 9 | The Following tab should present either the new rankings of people you follow, or the people themselves, in a way worth looking at. | design | done: people #166, topics #173 |
| 10 | Categories as on TierMaker: people create their own; a new one starts under Other and is lifted into the shared set when an admin approves it; Community opens with the categories a person already taps, then the lists in that category; the category is asked at creation and required when a board goes public (with wording in every language). TierMaker guards category misuse by keyword checks at publish time, user reports, and moderators moving or removing lists. | design (Community, creation, picking) + proxy + web parity | done: proxy tieryourlife-proxy#30 (deployed 26.10), the app #173; the 3×3 section grid and "new list here" on a topic page are left for later |
| 11 | Follow a category, not only a person (design). Bugs: the Following tab flashes before the people appear; coming back from an author's page with the system back, the author blinks out and in. | design + fix (the flashes: the feed reloads on every arrival) | the Following flash fixed, #152; following a topic #173; the author blink is to be checked on the phone |
| 12 | There is no Unfollow. And no choice of what the Following feed shows: followed people, followed categories, or both. | fix (Unfollow) + design (the switch, with #10 and #11) | Unfollow #153, the manage list #166, the People / Topics switch #173 |
| 13 | A poster chosen as the avatar is not scaled to fill the circle, so the circle has empty sides. Add choosing an avatar from the gallery. | fix (crop to fill) + feature (gallery) | crop #155, gallery #160 |
| 14 | The coloured band of a tier must not grow with its letter or caption: one fixed width, text wraps or is cut inside it. Reverses the August rule "one band width per board, from the longest caption". Goes with #2. | fix | done, #151 |

## Order proposed

1. Fixes with an obvious shape, one PR each: 7, 3, 2, 1, 12 (Unfollow), 11 (the two flashes), 13 (crop), 6 (no repeats), 4 (the black frame).
2. Claude Design: transitions (4), the collapsible panel (5), cover collages and picker (6), Following (9, 11, 12), categories (10). One brief per topic, decisions recorded in `design-decisions-log` memory and the design specs here.
3. Features after the designs: 8, 13 (gallery avatar), 5, 6 (picker), 9, 11, 12, then 10 with the proxy and the site.

## Decisions on 10, taken with Danylo on 25.10

- Two levels: the nine fixed, localised **sections** stay; people create **topics** (Pokémon, Marvel, Kharkiv restaurants). A topic's name is user text, not translated, searched as typed.
- At publish a person picks an existing topic (its section follows), types a new one, or picks a section alone. A new topic waits for approval; until then its lists sit under **Other** and the topic shows on them as a label everyone can see. On approval the moderator assigns the section (or renames, merges, rejects) and the lists move.
- Approval and reports live in the same moderation queue. Danylo approves; he alone can appoint other moderators, who then approve and handle reports too. Today the proxy knows one moderator by environment variable, so this needs a roles record.
- At most **3** pending topics per account.
- Guards, in TierMaker's spirit but softer: a suggestion at publish when the title looks like an existing topic (never a block), a report reason "wrong category", moderators can move any list.
- Community opens with the person's topics (followed, recently opened), then sections, then the feed. A topic page sorts recent/popular and can be followed; Following shows people and topics with a switch (#11, #12).
- Needs the proxy (topics collection, topic on a list, indexes, approval endpoints, roles) and the site to follow.

## Remarks of the evening, 25.10, after the first builds

| # | Remark | Kind | Status |
|---|---|---|---|
| 15 | The tick in the Their / Mine switch is one thing too many; the fill says which. | fix | done, #171 |
| 16 | Opening somebody's list, the posters show as squares and then jump to rectangles. | fix | done, #171: the skeleton uses the board's own tile size |
| 17 | Save to my lists should ask whose ranking to keep, theirs or mine, in a proper dialog. | design + fix | done, #171 |
| 18 | Going back from a list, own or community, blinks. | fix | done, #171: screens slide opaque, nothing fades |
| 19 | Settings open with the text a beat late. | fix | done, #172: the account and the backup row are known on the first frame |
| 20 | Duplicates as TierMaker fights them: somebody takes a list and publishes it one to one. Decided: a copy published with the same cards becomes a *ranking* of the original, not a second list. | design + proxy + app | proxy tieryourlife-proxy#31 and #32 (await deploy); the app in `rankings-of-a-list` |
| 21 | Landscape is unusable: the title eats the height and the section row does not scroll; tablets and the Flip's small screen need the whole thing comfortable. | design | drawn on the canvas, row 3 (landscape board, landscape community, the flip cover); awaits Danylo's look before building |

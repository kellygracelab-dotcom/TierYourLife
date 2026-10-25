# Walkthrough notes, 25.10.2026

Danylo went through build 355 on his Flip6 and wrote down what to change. One
row per remark, in his order. "Kind" says who decides the shape: a fix is a
code change with an obvious answer; design means Claude Design is asked first.

| # | Remark | Kind | Status |
|---|---|---|---|
| 1 | A card should answer the finger at once when dragging; there is no single-tap action, so the long-press delay only costs. Double tap should feel quicker too. | fix (keep a short delay: a vertical scroll and a drag both start with a finger on a card) | open |
| 2 | Default tier captions (Best, Great, …) go away: a new board shows the letter alone. When a row wraps to two or more lines, the letter sits centred in the band, not top-left. | fix (reverses the default-caption part of #105; captions stay editable) | open |
| 3 | The system back gesture first shrinks the screen, then leaves; the in-app back button leaves at once. The shrink is the system's predictive-back animation. | fix: opt out of predictive back, or make the pop immediate | open |
| 4 | Opening a community list shows black for about half a second; "Save to my list" flashes too. Transitions across the whole app should be smooth and not catch the eye. | design: a transition language for the app; fix for the black frame (window background, screen root surface, NavHost transitions) | open |
| 5 | The bottom panel on a list (pool; Their ranking / Mine; Save to my list) should collapse like a sheet and expand on demand, on own boards and community lists alike. | design + fix | open |
| 6 | Cover art: if the person picked 1 picture, show it alone; 2 to 6, a collage of exactly those; 6 is the maximum. With no choice, a collage of up to 6 *distinct* card pictures, fewer when fewer exist, never repeats; 1 picture shows alone; 0 shows the tier-colour stripes. | design (layouts for 2–5; the picker) + fix (no repeats) | open |
| 7 | Catalogue search returns many results without a poster; anything without a picture is dropped from the results. | fix | open |
| 8 | Where the cover is chosen, add a picture search (the same catalogue search), not only the board's own cards. | feature | open |
| 9 | The Following tab should present either the new rankings of people you follow, or the people themselves, in a way worth looking at. | design | open |
| 10 | Categories as on TierMaker: people create their own; a new one starts under Other and is lifted into the shared set when an admin approves it; Community opens with the categories a person already taps, then the lists in that category; the category is asked at creation and required when a board goes public (with wording in every language). TierMaker guards category misuse by keyword checks at publish time, user reports, and moderators moving or removing lists. | design (Community, creation, picking) + proxy + web parity | open |
| 11 | Follow a category, not only a person (design). Bugs: the Following tab flashes before the people appear; coming back from an author's page with the system back, the author blinks out and in. | design + fix (the flashes: the feed reloads on every arrival) | open |
| 12 | There is no Unfollow. And no choice of what the Following feed shows: followed people, followed categories, or both. | fix (Unfollow) + design (the switch, with #10 and #11) | open |
| 13 | A poster chosen as the avatar is not scaled to fill the circle, so the circle has empty sides. Add choosing an avatar from the gallery. | fix (crop to fill) + feature (gallery) | open |
| 14 | The coloured band of a tier must not grow with its letter or caption: one fixed width, text wraps or is cut inside it. Reverses the August rule "one band width per board, from the longest caption". Goes with #2. | fix | open |

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


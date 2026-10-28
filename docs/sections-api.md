# Sections as data, and anonymous lists — the API as merged

Status: merged and implemented. Proxy: TierYourLifeProxy #38 (sections) and #39 (anonymous lists, the admin's calls, the audit log, `play` on boards). App: TierYourLife #189 (sections, slice 1) and the anonymous-lists PR. This file started as the draft both clients built against (#187). It now says what the code does.

Danylo's decisions this rests on:
- sections are no longer a fixed nine;
- people propose new ones, and he approves them;
- production data may be deleted;
- no code converts old data. An additive Room auto-migration (new columns with defaults) is not conversion; it is how the app adds a column without wiping a phone's library.

**Deploy** (proxy): `firebase deploy --only functions,storage,firestore:indexes`.
- Storage rules: approved section pictures answer 403 without the rule from #38.
- Indexes: `GET lists/anonymous` answers 503 until its index from #39 has built.

## Roles and refusals

- **owner**: `MODERATOR_EMAIL` / `MODERATOR_UID`.
- **moderator**: appointed, `roles/{uid}.moderator`.
- **staff**: either.
- A moderator calling an owner-only route gets `403 NOT_YOURS {detail: "owner_only"}`; anyone else gets `403 NOT_YOURS`.
- Every refusal is `{error, code, detail?}`. Extra fields from the draft ride along: `id`, `why`, `mergedInto`.

## 1. What a section is

A Firestore document `sections/{id}`.

| Field | Who sees it | Notes |
|---|---|---|
| `name`, `names` | everyone | `names` holds translations, keyed by the app's locales: `en uk ru es pt-BR de fr pl tr ja ar`. A label is `names[locale] ?? name`. |
| `icon` | everyone | One of `SECTION_ICONS` (below). Default `category`. |
| `picture` | everyone, once approved | `{url, width, height}` or null. Public copy at `sections/{id}/{pictureId}`. Always null for built-ins. |
| `tint`, `topicBg`, `topicIcon` | everyone | `{light, dark}` hex (§5). |
| `status` | everyone for approved | `approved`, `pending`, `declined` or `merged`. |
| `builtIn` | everyone | Built-ins can't be declined, merged, deleted or given a picture, and Other can't be hidden. |
| `hidden` | staff (and see below) | Hidden sections get no tile, can't be published into, used as `fileUnder` or used as a topic's section. Their lists stay reachable by link, search and `?category=`, and `GET lists/sections/{id}` still answers. Non-staff never see hidden sections in the list. |
| `rank` | everyone | The owner's pin. |
| `fileUnder`, `mergedInto`, `declineNote`, `waitingLists` | the proposer on their own non-approved section; staff | |
| `withdrawn` | staff | `true` on a proposal its proposer withdrew. It stays `declined`. |
| `proposerUid`, `proposerName`, `palette`, `createdAt`, `decidedAt`, `decidedBy`, `unfinished` | staff (`proposerUid` also to the proposer) | `unfinished: true`: a decision is written but its lists are not all moved yet. The same call again finishes the job. |
| `approvedAt` | everyone | Built-ins count as 0. |

**Built-ins** are seeded on first read with `create()`. Nothing is converted. Each gets `builtIn: true, hidden: false, approvedAt: 0`, `names` in all 11 locales from the app's `category_*` strings (`name` is the English one), and Claude Design's colours as given. Icons: anime=animation, film_tv=movie, games=sports_esports, music=music_note, books=menu_book, food=restaurant, sport=sports_soccer, people=person, other=category.

**`SECTION_ICONS`** (24, the built-ins' first): category, movie, animation, sports_esports, music_note, menu_book, restaurant, sports_soccer, person, pets, flight, directions_car, palette, checkroom, science, computer, local_cafe, park, theater_comedy, mic, photo_camera, sports_basketball, celebration, star.

**Ids** are read with `^[a-z0-9][a-z0-9_-]{1,39}$`, because `film_tv` keeps its underscore. New ones are made with `^[a-z0-9][a-z0-9-]{1,39}$`:
- a slug of the name (`street-food`), with `-2`, `-3` on a clash;
- `s-` plus 8 base36 characters when the name gives no slug.

Ids never change, and these are reserved: `queue proposals order name-check lists`.

**Names** are 2–24 characters (characters, not UTF-16 units) and must contain a letter or digit. Before comparing or slugging they are NFKC-normalised and lose format characters (`\p{Cf}`). Every name a section claims (its proposal name, renames, translations) is reserved in `sectionNames/{sha256(key)}`, in the same transaction as the section. A reservation lapses once its section is declined, merged, renamed or deleted.

**A list's section** is `publishedLists.category`. It may be:
- any open section (approved, not hidden);
- the caller's own pending id, stored as `category = fileUnder` plus `proposedSection`;
- a merged id, followed to its target.

A republish keeps its section even once that is hidden. A topic still decides the section, so a list with a topic never carries `proposedSection`. Anything else is `400 INVALID {detail: "category"}`. `GET lists/mine` adds `proposedSection` and `proposedSectionName`.

## 2. Reading

| Call | Who | Success | Refusals |
|---|---|---|---|
| `GET lists/sections` | anyone | `200 {sections, updatedAt, version, role}`. `version` is null except for staff. | |
| `GET lists/sections/name-check?name=` | anyone | `200 {free: true}`, `{free: false, id}` or `{free: false, code: "BAD_NAME"}` | |
| `GET lists/sections/{id}` | anyone | `200` section (bare) | `404 NOT_FOUND`. `410 MERGED {detail, mergedInto}` goes to everybody if the section was approved before it was merged, otherwise only to its proposer and staff; the rest get 404. |
| `POST lists/sections/{id}/follow` | signed in | `200 {following: true}` | `403 NOT_SIGNED_IN`, `404`, `410 MERGED`, `409 TOO_MANY_FOLLOWING` (ceiling 200) |
| `DELETE lists/sections/{id}/follow` | anyone | `200 {following: false}` | `404` for a bad id |

A section in the list carries `id, name, names, icon, pictureUrl, tint, topicBg, topicIcon, status, builtIn, hidden, rank, approvedAt, listCount, lists30d, newThisWeek, followerCount, following, collage`, plus the fields the caller may see (§1).
- `pictureUrl` of a pending section is a download-token address, sent only to its proposer and staff.
- `collage` is four pictures from lists enough people ranked, otherwise empty.
- After the ordered sections come the caller's own proposals: pending, and declined or merged until dismissed, oldest first. Staff get every pending, declined and merged section there, and hidden ones in the list.
- Ordinary screens draw only `status == approved && !hidden`.

The feed (`GET lists?category={id}`, `&topic=`) works for any id. `GET lists/following` takes `followerCount` from the cached counts, up to 10 minutes old.

## 3. Writing sections

| Call | Who | Body | Success | Refusals |
|---|---|---|---|---|
| `POST lists/sections/proposals` | signed in, unbanned | `{name, icon?, pictureId, fileUnder}` | `201` pending section | `403 NOT_SIGNED_IN`, `403 BANNED {until}`, `400 BAD_NAME`, `400 NAME_TAKEN {detail, id}`, `400 BAD_ICON`, `400 BAD_FILE_UNDER`, `400 BAD_PICTURE {detail, why: missing\|not_image\|too_small\|not_16_10}`, `400 TOO_MANY_PENDING` (3), `429 TOO_MANY_PROPOSALS` (10 per rolling 24 h, however they ended) |
| `DELETE lists/sections/proposals/{id}` | the proposer, unbanned | | `204`. A pending proposal is withdrawn and its lists stay in `fileUnder`. On a declined or merged one, the notice is dismissed. | `403 NOT_SIGNED_IN`, `403 BANNED`, `404`, `409 SETTLED` |
| `GET lists/sections/queue` | staff | | `200 {sections}`, oldest first, with `proposerUid, proposerName, pictureUrl, palette, waitingLists, fileUnder, createdAt, unfinished` | `403` |
| `POST lists/sections/{id}/approve` | staff (`rank`: owner only) | `{name?, names?, icon?, tint?, rank?}` | `200` section | `403`, `404`, `409 SETTLED`, `400 BAD_NAME / NAME_TAKEN / BAD_ICON / INVALID / BAD_PICTURE` |
| `POST lists/sections/{id}/decline` | staff | `{note?}` | `200` section | `403`, `404`, `409 SETTLED`, `409 BUILT_IN` |
| `POST lists/sections/{id}/merge` | owner | `{into}` | `200` section | `403`, `404`, `409 SETTLED`, `409 BUILT_IN`, `400 INVALID {detail: "into"}` |
| `POST lists/sections` | owner | `{name, names?, icon?, pictureId?, tint?, rank?}` | `201` approved section | `403`, `400 BAD_NAME / NAME_TAKEN / BAD_ICON / BAD_PICTURE / INVALID` |
| `PATCH lists/sections/{id}` (approved) | owner | `{name?, names?, icon?, rank?, hidden?, pictureId?, tint?}` | `200` section | `403`, `404`, `409 BUILT_IN` (a picture on a built-in, or hiding Other), `400 …` as above |
| `PATCH lists/sections/{id}` (pending) | owner | `{name?, names?, icon?, pictureId?, tint?}` | `200` section, still pending | `403` (moderators too), `400 INVALID {detail: "rank"\|"hidden"}`, `400 BAD_NAME / NAME_TAKEN / BAD_ICON / BAD_PICTURE`, `409 SETTLED` |
| `PUT lists/sections/order` | owner | `{ids, hidden, version}` | `200`, the same answer as `GET lists/sections`, with the version bumped | `403`, `400 BAD_ORDER` (only approved ids; Other can't be hidden), `409 CONFLICT {detail: "version", version}` |
| `DELETE lists/sections/{id}` | owner | | `204`. Pending proposals filed under it move to Other, and its follows go. | `403`, `404`, `409 BUILT_IN`, `409 NOT_EMPTY {detail: "lists"\|"topics", lists, topics}` |

**Proposal rules**:
- The picture comes from the proposer's own `users/{uid}/pictures/{id}`. It must be JPEG, PNG or WebP, measured from its header, at least 960 px wide, and 16:10 within ±2%.
- At proposal time it is copied to `proposals/{id}/{pictureId}`, out of reach of the daily sweep, with a download token.
- Every proposal also reads and writes `sectionProposers/{uid}`. That keeps the daily cap and serialises one account's proposals.

**Decisions are transactions.** Approve, decline, merge and withdraw each re-read the section and write the decision there. Of two at once, one stands and the other gets `409 SETTLED`.
- Lists move after the decision, a page at a time, with `unfinished: true` until they all have.
- **Approve** publishes the picture to `sections/`, computes the colours (§5), sets `approvedAt`, moves the `proposedSection == id` lists in, and deletes the proposal copy.
- **Decline** and **withdraw** clear `proposedSection`. A withdrawn proposal is kept, as `declined` with `withdrawn: true`, so its id is never reused.
- **Merge** moves filed and waiting lists, topics, follows, older merges and proposals to `into`.

On a waiting section:
- a `tint` chosen while waiting is stored and used at approval;
- a new picture replaces the private copy and takes the palette again;
- renames release the names the section gave up.

## 4. Order

Applied by the proxy per caller. Clients redraw by the same rule after a follow made on the device:
1. followed sections;
2. `rank` ascending;
3. `lists30d` descending;
4. `approvedAt` ascending, with built-ins at 0, so they keep their old order;
5. `other` always last, and on Home only when every section fits.

Every owner change that alters the order screen bumps `config/sections.version`: approve, merge, create, delete, PATCH and order. A decline doesn't.

Home slots (Claude Design):
- phone: 8 tiles plus the full-width row;
- 600–839: 9;
- ≥840: 10.

With more sections than slots, the last slot is "All sections": a screen in the app, the page `/s` on the web. It has a count, sorting by Most lists or A–Z, and "Find a section" once there are more than 20. It ends with "Propose a section".

## 5. Colours of a user section

The proxy computes them at approval, so every client draws the same thing. This is Claude Design's derivation, exactly:
- Convert sRGB to linear, then to OKLab. `mix(x, base, t) = x·t + base·(1−t)` on L, a and b. Convert back to sRGB, clipping each channel.
- `tint.light`: the dominant colour with L clamped to 0.40–0.48.
- `tint.dark`: `mix(light, #121318, 0.78)`.
- `topicBg.light`: `mix(light, #FBF8FF, 0.22)`; `topicBg.dark`: `mix(light, #121318, 0.32)`.
- `topicIcon.light`: `light`; `topicIcon.dark`: `light` with L set to 0.82.

The test vector (#9A5638) and all nine built-in rows reproduce exactly from their light tint.

**The dominant colour.** Vision's `IMAGE_PROPERTIES` colours, weighed by `pixelFraction`, go through 4-bit RGB buckets. Near-black and near-white are ignored unless nothing else is left, and the fullest bucket's mean wins. Vision's clusters stand in for shrinking the picture to 32×20, because the proxy has no image decoder.

**Sources and fallbacks:**
- `palette` holds Vision's top 5, taken once at proposal time. If Vision fails at approval, the first palette colour is used. With no palette either, the call answers 503.
- A moderator's `tint` on approve or PATCH goes through the same clamp (not for a built-in) and the same derivation.
- A waiting section keeps Other's colours until it is approved.

## 6. Clients

- **Cache.** The last `GET lists/sections` answer is kept on the device and drawn first. It refreshes when Community opens, at most every 10 minutes. With no cache, the nine built-ins are drawn from the client's own art.
- **Unknown id.** It is drawn with Other's look and the `category` icon. The client asks `GET lists/sections/{id}` once and redraws. A 410 remaps the id.
- **Built-ins** keep the client's own art (tint plus hatch, their icons). Only user sections use `picture`.
- **The app's `ListCategory` enum** stays as the built-ins' art and fallback. Everything that names a section carries a section id string.

## 7. Anonymous lists

**Publishing.**
- `POST lists` and `POST lists/{id}` take `anonymous: true|false`, stored as `anonymous`. An explicit boolean decides.
- When the field is missing or isn't a boolean, a republish **keeps the stored value**, and a first publish is not anonymous. That rule is what stops an older app from turning an anonymous list public.
- The app always sends the field, true or false.

**What everybody else sees.**
- Every public answer sends an anonymous list as `authorUid: "", authorName: "", authorPhotoUrl: null, anonymous: true`. Other lists carry `anonymous: false`.
- That covers the feed (every sort, `category`, `topic`, `q`), `GET lists/{id}` (where the rankings sheet's author row comes from), the following feed, and section collages.
- **`basedOn`** is `{id, title, authorUid, authorName}`. Both author fields are `""` when the original is anonymous, and `authorUid` is `""` on copies made before uids travelled. It is never null.
- Every republish rewrites the copies whose credit disagrees with the list's current flag.

**What stays hidden from public pages.**
- Anonymous lists are kept off `GET lists?author=` (the author page and its "N public lists").
- They never reach a follower's Following feed through the author. Through a followed topic or section they still appear, by nobody.
- `GET lists/following` never takes a name or face from an anonymous list.
- Suggested authors skip them.
- Pages filter in code and read on past hidden lists (up to 5 reads a page), and always end on a list they show.
- Lists without the field read as not anonymous.

**What the owner and the staff still see.**
- The owner check stays on the stored uid. `GET lists/mine` returns the real author plus `anonymous: true`.
- The reports queue keeps the real author, because bans need it.
- `GET lists/anonymous` (owner) lists every anonymous list with `publishedBy`.

**On screen.**
- Anonymous lists show "Anonymous" with a generic face where the author would be: not a link, no follow.
- "Based on “X”" drops "by Y" when the name is empty.
- Your public lists marks an anonymous list "Anonymous".

## 8. Boards (the account's copy)

- **`anonymous`**: a board may carry `anonymous: true`, so "Publish anonymously" survives a change of device. It is stored and returned only when true. A board without it round-trips byte for byte. The app sends it only when true.
- **`play`**: an optional saved Play game `{kind, count, seed, refine, ids, history, round, startedAt}`, validated strictly:
  - kind `swiss|hill|bracket`;
  - 2–64 distinct `ids`, with `count` equal to their number;
  - int32 `seed` and boolean `refine`;
  - up to 2000 `history` entries `[first, second, winner|null]`;
  - `round` 0–2000 and a positive `startedAt`.

  `scope` (tier uids) and `keepTier` are being added in a small proxy PR.

  Anything invalid drops `play` and keeps the board. Each write that carries a valid game stamps it with `touchedAt` (stored, never returned). A game not stored for 30 days is left out of reads. A board without `play` has no `play` key at all. The app keeps the JSON as an opaque column and sends it back exactly as it came.
- The account stores a board as it is written, with no merge. A client that leaves either field out wipes it.
- **Fingerprint** (app `BoardFingerprint`, the site matches it): SHA-256 over fields written as `{length}:{value}|`, or `~|` for null, encoded as UTF-8. The length counts UTF-16 code units, as JavaScript's `.length` does.
  - The board's own fields, in order: `uid, title, displayMode, category, coverImageUrl, coverImageUrls (one address per line), authorName, deletedAt`.
  - Then each tier by uid, `uid, position, label, caption, colorLight, colorDark, isPool`, each followed by its cards by uid: `uid, position, title, picture id or http(s) address, source, deletedAt`.
  - After the last card come two more fields, each only when there is something to say. A board with neither keeps its old fingerprint.
    1. `anonymous`, when the board is anonymous.
    2. The canonical play JSON, when the board has a game. Without it, a game saved on an untouched board would never sync. The canonical form is the stored game parsed and written again, never the server's raw text:
       - no whitespace;
       - keys in this order: `kind, count, seed, refine, ids, history, round, startedAt`, then `scope` only when present, then `keepTier` only when true;
       - `history` entries as `[first,second,winner]`, with a skip's winner as `null`.
  - Vector (a): board uid `u`, title `T`, displayMode `WRAP`, every other field null, no tiers, anonymous. That is the SHA-256 of `1:u|1:T|4:WRAP|~|~|~|~|~|9:anonymous|`, `b7c68568ab4c48be25a724e26ef8f14ac5f940178b556a1cb4afb7183c717394`. Without anonymous: `1:u|1:T|4:WRAP|~|~|~|~|~|`, `8e5ded7251916b6817519d19b768e298cb2e38b195ef08173f4e22c50189b27f`.
  - Vector (b): the same board with the game `{"kind":"swiss","count":4,"seed":7,"refine":false,"ids":["a","b","c","d"],"history":[[0,1,0],[2,3,null]],"round":1,"startedAt":1793146000000,"scope":["t-s"],"keepTier":true}` (173 characters). That is the SHA-256 of `1:u|1:T|4:WRAP|~|~|~|~|~|9:anonymous|173:{…}|`, `b97bfcf2595276d8399cacd16b85ec5341c62ae5ea9916330b394bf61c9cea89`.

## 9. Topics, moderators, counts, audit

| Call | Who | Body / query | Success | Refusals |
|---|---|---|---|---|
| `POST lists/topics` | staff | `{name, section}` | `201` topic | `403`, `400 INVALID {detail: "name"\|"section"}`, `409 NAME_TAKEN {detail, id}` |
| `PATCH lists/topics/{id}` | staff | `{name?, names?}` | `200` topic | `403`, `400 INVALID`, `404`, `409 SETTLED` (rejected or merged), `409 NAME_TAKEN {detail, id}` |
| `POST lists/topics/{id}/rename` | staff | `{name}` | `200`, as PATCH | as PATCH |
| `POST lists/topics/{id}/approve` | staff | `{section}` (any open section) | `200` topic | |
| `POST lists/topics/{id}/reject` | staff | | `200` topic | |
| `POST lists/topics/{id}/merge` | **owner** | `{into}` | `200` topic | `403 {detail: "owner_only"}` for a moderator |
| `DELETE lists/topics/{id}` | owner | | `204` | `403`, `400`, `404`, `409 NOT_EMPTY {detail: "lists", lists}` |
| `GET lists/moderators` | staff | | `200 {moderators: [{uid, name, photoUrl, email?, appointedAt}]}`, `email` for the owner only | `403` |
| `GET lists/moderators/candidates` | owner | `?q=` | `200 {people: [{uid, name, email, photoUrl}]}`: at most 10, and `[]` under 2 characters | `403` |
| `POST lists/moderators/{uid}` | owner | `{name?, photoUrl?}` | `200 {uid, moderator: true}` | `403`, `400 INVALID` (`detail: "uid"` for a guest or a disabled account), `404 NOT_FOUND`, `409 YOURSELF` |
| `DELETE lists/moderators/{uid}` | owner | | `204` | `403`, `400` |
| `GET lists/admin/counts` | staff | | `200 {reports, proposals}` | `403` |
| `GET lists/anonymous` | owner | `?after=` (last id) | `200 {lists: [summary + {publishedBy: {uid, name}}], nextCursor}`, newest first, pages of 20 | `403`, `409 CURSOR_GONE` |
| `GET lists/{id}/rankings` | anyone | `?after=` | `200 {rankings, nextCursor}`. Rankings without an owner uid are left out, and the caller's own leads the first page. | `409 CURSOR_GONE` |

**Topics.**
- Topic answers carry `names`: translations in the same 11 locales, 2–40 characters each.
- `createdBy` goes only to the staff and to its own proposer.
- Topic names are reserved in `topicNames/{sha256(nameLower)}`, in the transaction that creates or renames.

**Moderator candidates** come from an index of signed-in, enabled Auth accounts:
- it is kept for 5 minutes and capped at 10 000 accounts read;
- matching is case-insensitive on the start of the name, of any word in it, or of the address.

**Audit log** `auditLog/{auto} {who, role, what, target, before, after, at}`:
- `what` is one of `section.approve|decline|merge|create|edit|order|delete`, `topic.create|edit|merge|delete|approve|reject`, `moderator.appoint|remove`, `report.takedown|dismiss`, `ban.set`, `list.move`.
- `before` and `after` hold only the fields that changed.
- Each entry is written in the same transaction or batch as its change.
- There is no read endpoint yet.

## 10. Data

- Stored covers of `"null"`, `""` or `"undefined"` are refused on write. Since proxy #37 they go out as no cover on read.
- **Account erase:**
  - deletes the account's pending or declined proposals;
  - takes `proposerUid` and `proposerName` off its approved or merged ones;
  - deletes its proposal pictures and `sectionProposers/{uid}`;
  - blanks `basedOn.authorUid` and `authorName` on other people's copies that credit it;
  - removes `name` from the audit lines about it.
- **Indexes:** `publishedLists (anonymous ASC, updatedAt DESC)` serves `GET lists/anonymous`. No other composite index was added; feeds filter anonymous lists in code.
- Production test data may be deleted rather than converted. Nothing here converts old documents.

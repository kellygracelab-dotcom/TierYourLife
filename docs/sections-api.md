# Sections as data, and anonymous lists — API draft

Status: draft agreed between the app and web sessions on 28.10.2026, while Danylo was away.
Owner of the proxy side: the app session (TierYourLifeProxy). The web builds its admin against this.

Danylo's decisions this rests on (relayed by the web session):
- sections are no longer a fixed nine;
- people propose new ones, and he approves them;
- production data may be deleted;
- no migration code.

## 1. What a section is

A section is a Firestore document `sections/{id}`. The nine built-in ones are ordinary documents with `builtIn: true`. The proxy creates them on first read if they are missing. That is seeding, not migration: nothing old is converted.

| Field | Type | Who sees it | Notes |
|---|---|---|---|
| `name` | string, 2–24 chars | everyone | As typed by the proposer, or as edited by a moderator. Base language: whatever was typed. |
| `names` | map, locale → string | everyone | Optional translations added by a moderator. Keys are the app's locales: `en uk ru es pt-BR de fr pl tr ja ar`. |
| `icon` | string | everyone | A Material Symbols name from `SECTION_ICONS` (the 24 offered to proposers, plus the built-ins' own nine). Default `category`. |
| `picture` | `{url, width, height}` or null | everyone, once approved | Public Storage copy at `sections/{id}/{pictureId}`. Always null for built-ins: clients draw their own art. |
| `tint` | `{light, dark}` hex | everyone | For user sections, computed at approval (§5). For built-ins, the app's current tints. |
| `topicBg`, `topicIcon` | `{light, dark}` hex | everyone | Derived like the tint (§5). |
| `status` | `approved`, `pending`, `declined` or `merged` | everyone for approved; otherwise proposer and moderators | |
| `builtIn` | bool | everyone | Built-ins can't be declined, merged or deleted. |
| `rank` | int or null | everyone | A moderator's pin. Pinned sections come first, in ascending rank. |
| `fileUnder` | section id or null | proposer, moderators | While pending: where the proposer's lists live in the meantime. |
| `mergedInto` | section id or null | everyone who asks for that id | |
| `proposerUid`, `proposerName` | | proposer, moderators | Never sent to anybody else. |
| `proposalPicture` | Storage path | proxy only | The proposer's own upload. Private until approval. |
| `declineNote` | string or null | proposer | Shown once in the proposer's Following strip, then dismissed. |
| `createdAt`, `approvedAt`, `decidedAt`, `decidedBy` | timestamps / uid | `approvedAt` everyone; the rest moderators | |

**Ids** match `^[a-z0-9][a-z0-9-]{1,39}$` and never change, even when the section is renamed. The nine built-ins keep their ids: `anime film_tv games music books food sport people other`. A new section's id is an ASCII slug of its name (`street-food`), with `-2`, `-3` added on a clash. If the name gives no usable slug (Cyrillic, Japanese and so on), the id is `s-` plus 8 random base36 characters.

**A list's section** is still the field `category` on `publishedLists`, now holding any approved section id. A list filed under a pending proposal stores `category = fileUnder` and `proposedSection = <pending id>`.

## 2. Reading

### `GET lists/sections`

Returns approved sections plus the caller's own pending and declined ones. The order is already applied (§4).

```json
{
  "sections": [
    {
      "id": "street-food",
      "name": "Street food",
      "names": { "uk": "Вулична їжа" },
      "icon": "ramen_dining",
      "pictureUrl": "https://firebasestorage.googleapis.com/…",
      "tint": { "light": "#7A4A33", "dark": "#3A2A26" },
      "topicBg": { "light": "#F3E6DF", "dark": "#2B2220" },
      "topicIcon": { "light": "#7A4A33", "dark": "#E8C3B0" },
      "status": "approved",
      "builtIn": false,
      "rank": null,
      "approvedAt": 1793146000000,
      "listCount": 41,
      "lists30d": 12,
      "newThisWeek": 3,
      "followerCount": 9,
      "following": true,
      "collage": ["https://…", "https://…", "https://…", "https://…"]
    }
  ],
  "updatedAt": 1793146553000
}
```

Fields beyond the common ones:
- On a pending or declined section, only for its proposer: `fileUnder`, `proposerUid`, `declineNote`.
- `pictureUrl` of a pending section, for its proposer and for moderators, is an unguessable download-token URL. It is never listed anywhere public.
- A client picks the label as `names[locale] ?? name`.
- `collage` keeps today's rule: four pictures from lists enough people ranked, otherwise empty.

### `GET lists/sections/{id}`

Returns one section, for deep links (`/s/:id`) and for ids a client meets before its cached list knows them.
- `404 NOT_FOUND`: no such section, or a pending one the caller didn't propose.
- `410 MERGED` with `{ "mergedInto": "food" }`: clients follow it and remember the mapping.

### Feed

`GET lists?category={id}` keeps working for any id, so does `&topic=`, and section follows work as today.

The following feed carries no reason field. Clients derive the reason as the app does today (`FollowingFeedViewModel.reasonFor`):
- author followed → no line;
- else a followed topic → the topic's name;
- else a followed section → "In {section}";
- else nothing.

## 3. Writing

All writes need a signed-in, non-anonymous, unbanned account.

### Propose: `POST lists/sections/proposals`

```json
{ "name": "Street food", "icon": "ramen_dining", "pictureId": "<id>", "fileUnder": "food" }
```

1. The client uploads the picture first, to its own private folder `users/{uid}/pictures/{pictureId}`. That is the existing Storage rule; no new opening.
2. The proxy checks:
   - the name is 2–24 characters after trimming and collapsing spaces;
   - it doesn't clash, case- and diacritics-insensitively, with any approved or pending name or translation;
   - the icon is in `SECTION_ICONS`;
   - `fileUnder` is an approved section (Other included);
   - the picture exists, is an image, is at least 960 px wide, and has a 16:10 aspect within ±2%. The client crops it.
   - the account has at most 3 pending proposals.

`201` returns the pending section. Refusals are `400` with one of these codes:
- `BAD_NAME`
- `NAME_TAKEN`, with `{ "id": <the existing section> }`
- `BAD_ICON`
- `BAD_FILE_UNDER`
- `BAD_PICTURE`, with `{ "why": "missing" | "not_image" | "too_small" | "not_16_10" }`
- `TOO_MANY_PENDING`

Account refusals: `403 NOT_SIGNED_IN` and `403 BANNED`.

### Withdraw or dismiss: `DELETE lists/sections/proposals/{id}`

Withdraws a pending proposal (its lists stay in `fileUnder`), or dismisses a declined one's note. Only the proposer can do this.

### Publishing into a proposal

The publish body's `category` may be an approved id, or the caller's own pending id. With a pending id, the list is stored under `fileUnder` and `proposedSection` is set. `GET lists/mine` returns `proposedSection` and its name, so the app and the web can say "Moves to Street food once it's approved".

### Moderators (`MODERATOR_EMAIL` / moderator uids, as for topics)

| Call | Does |
|---|---|
| `GET lists/sections/queue` | Pending proposals, oldest first, with proposer, picture URL and how many lists wait in each. |
| `POST lists/sections` `{name, names?, icon, pictureId?, rank?}` | Creates an approved section directly. Picture from the moderator's own upload folder. |
| `POST lists/sections/{id}/approve` `{name?, names?, icon?, rank?}` | Copies the picture to `sections/{id}/…` (public), computes `tint`, `topicBg` and `topicIcon`, sets `approved` and `approvedAt`, and moves every list with `proposedSection == id` into it. |
| `POST lists/sections/{id}/decline` `{note?}` | Sets `declined`. Lists stay where they were filed, and `proposedSection` is cleared. The proposer sees the note. |
| `POST lists/sections/{id}/merge` `{into}` | Sets `merged`. Lists, topics and follows move to `into`, and later reads of the old id answer `410`. |
| `PATCH lists/sections/{id}` `{name?, names?, icon?, rank?, pictureId?}` | Edits an approved section. A new picture recomputes the tints. |

A section is never deleted: an unwanted approved one is merged into another. Topics keep a `section` field, which now holds any approved id.

## 4. Order

The order is decided by the proxy, and clients redraw by the same rule after a local follow or unfollow:

1. sections the caller follows, in the order below;
2. pinned sections (`rank` not null), ascending;
3. the rest by `lists30d`, descending;
4. ties by `approvedAt`, earlier first; built-ins count as approved at 0, so they keep their old order among themselves;
5. `other` always last, and on Home only when every section fits.

Home slots (from Claude Design):
- phone: 8 tiles plus the full-width row;
- 600–839: 9;
- ≥840: 10.

With more sections than slots, the last slot is "All sections", a screen (the app) or page (`/s`, the web). It has a count, sorting by Most lists or A–Z, and "Find a section" once there are more than 20. It ends with "Propose a section".

## 5. Colours of a user section

Computed by the proxy at approval, so every client draws the same thing:
1. Shrink the picture to 32×20.
2. Bucket the pixels by 4-bit RGB, ignoring near-black and near-white unless that leaves nothing.
3. Take the fullest bucket's mean colour as the dominant colour.
4. `tint.light` is that colour in OKLab with lightness clamped to 0.40–0.48.
5. `tint.dark` is `tint.light` mixed 78% with `#121318`.
6. `topicBg` and `topicIcon` follow Claude Design's derivation. The exact numbers will be filled in from its spec before this is implemented.

A moderator can override any of these in `PATCH`.

## 6. Clients

- **Cache.** The last `GET lists/sections` answer is kept on the device and drawn first, so tiles don't jump. It refreshes when Community opens, at most every 10 minutes. With no cache at all, the nine built-ins are drawn from the app's own art.
- **Unknown id.** A list or link with an id the cache doesn't know is drawn with Other's look and `category` icon. The client asks `GET lists/sections/{id}` once and redraws. A `410` remaps the id.
- **Built-ins** keep the app's own art (tint plus hatch, its icons). Only user sections use `picture`.
- **The app's `ListCategory` enum** stays as the built-ins' art and fallback. Everything that names a section — board category, filters, publish, topics, follows — carries a section id string. A board's category is already stored as a string, so the board database needs no change.

## 7. Anonymous lists

- The publish body takes `anonymous: true`, which the proxy stores.
- Every public answer omits the author of an anonymous list: `authorUid: ""`, `authorName: ""`, `authorPhotoUrl: null`, `anonymous: true`. That covers the feed, list, search, section, following feed, the rankings sheet's author row and `basedOn`, which becomes `{id, title, authorName: null}`.
- Not counted on the author page or in its "N public lists". Never in followers' Following feeds.
- The owner check stays on the stored uid. `GET lists/mine` returns the owner's anonymous lists with `anonymous: true`, and the owner can update or unpublish them as usual.
- Moderators still see the author in the report queue, because bans need it.
- Republishing can switch the flag either way.

## 8. Data

- Stored covers of `"null"`, `""` or `"undefined"` are already refused on write (`keepableImageUrl`).
- Since proxy #37, they go out as no cover on read. The five old editorial documents need no backfill for clients to be right. If one is wanted, it's a one-off script run by Danylo, since this session has no direct Firestore access.
- Production test data may be deleted rather than converted. Nothing in this design converts old documents.

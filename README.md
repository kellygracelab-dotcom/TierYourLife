# TierYourLife

> A ranking journal for Android. Put anything you care about on an **S / A / B / C / D**
> board — films, games, dishes, albums — and keep it. Not a one-off tier-list generator:
> boards live on the device, stay editable, follow you to the next phone, and any one of them
> can be published for other people to rank for themselves.

[![CI](https://github.com/kellygracelab-dotcom/TierYourLife/actions/workflows/ci.yml/badge.svg)](https://github.com/kellygracelab-dotcom/TierYourLife/actions/workflows/ci.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-24-3DDC84?logo=android&logoColor=white)

Built from scratch, in the open, as a learning flagship — the commit history is the story.
Heading for Google Play; the listing link goes here when it exists.

---

<p align="center">
  <img src="docs/screenshots/drag.gif" width="300" alt="Dragging a poster into a tier, then reordering the tiers themselves">
</p>

<p align="center">
  <sub>Drag an item into a tier, then drag the tier itself — neighbours move out of the way while your finger is still down.</sub>
</p>

## The app

| Your boards | A board | Settings |
|:---:|:---:|:---:|
| <img src="docs/screenshots/home-light.png" width="240"> | <img src="docs/screenshots/board-light.png" width="240"> | <img src="docs/screenshots/settings-light.png" width="240"> |

Light and dark are designed separately — each tier carries two colours, so a band that is readable
on white is not a glowing slab at night.

| A board, dark | Nothing yet | Settings, dark |
|:---:|:---:|:---:|
| <img src="docs/screenshots/board-dark.png" width="240"> | <img src="docs/screenshots/home-empty-dark.png" width="240"> | <img src="docs/screenshots/settings-dark.png" width="240"> |

## The community

A board you publish becomes something other people can open and rank themselves. They get your
cards and your tiers and an empty board to fill; their arrangement and yours are two tabs on the
same board, and saving keeps whichever one is on screen. The ranking is the part you never hand
over.

| The feed | Long-press a card | Someone else's board |
|:---:|:---:|:---:|
| <img src="docs/screenshots/community-light.png" width="240"> | <img src="docs/screenshots/list-actions-dark.png" width="240"> | <img src="docs/screenshots/community-list-light.png" width="240"> |

Nothing is moderated automatically. Reports go to one person who reads them by hand, and the app
says so before the button is pressed rather than implying a moderation team. Hiding sits next to
reporting because most of the time "I would rather not see this" is not an accusation; both can be
undone from Settings.

| Reporting one | The report queue | Their profile |
|:---:|:---:|:---:|
| <img src="docs/screenshots/report-light.png" width="240"> | <img src="docs/screenshots/moderation-light.png" width="240"> | <img src="docs/screenshots/author-dark.png" width="240"> |

The queue leads with the cover the feed showed, because most of the time the complaint *is* the
picture. Taking a list down and how long its author waits before publishing again are one sheet,
because they are one decision — a week, a month, three, six, or for good, and only the last asks
twice.

## On a tablet, and on a folded phone

| A board at 1280×800dp | The feed | Settings |
|:---:|:---:|:---:|
| <img src="docs/screenshots/tablet/board-light.png" width="300"> | <img src="docs/screenshots/tablet/community-light.png" width="300"> | <img src="docs/screenshots/tablet/settings-dark.png" width="300"> |

Filling a tablet's width is not the same as using it. Content has a measure and the window keeps
the rest — except the feed, where width buys more cards rather than bigger ones. Past 600dp the
tabs, the settings icon and the corner button all move into a rail on the left, because they were
one job and two navigation systems on one screen is what makes tablet layouts fall apart. Past
840dp the list of boards stands beside the open board.

The window is classified by what a layout may do, not by device: a phone in split view is not a
tablet, and a folding phone is three different windows within a second. Its cover screen is its
own case — it shows a board and refuses everything else, because a drag across 46dp rows with a
camera cutout in the corner is a worse version of a gesture that works one fold away.

## What it does

- **Boards.** Rename, reorder tiers, edit each tier's label, caption and colour — light and dark
  shades set separately. Drag an item between tiers or within one, in reading order, right-to-left
  included; drag a tier row and the board reorders live under your finger.
- **Add cards** from a catalogue search — TMDB and Wikidata behind one field, merged, deduplicated,
  ranked, and each still answering when the other is down — or by name, or from your photos.
- **Sort, filter, star.** Newest or oldest, by category, public or private, and a star that pins a
  board to the top. Starring is a time rather than a flag, so several starred boards keep an order
  among themselves.
- **Boards that follow you.** Sign in and they are kept in your account. Two versions of one board
  are never merged — the order of the cards *is* the content — so the account's copy arrives as a
  second board that says where it came from, and you decide.
- **Trash, not deletion**, with a one-tap restore. Deleting a tier does not take its trashed items
  with it.
- **The community.** Browse by category, search across it, follow authors, open anyone's profile,
  save a copy of any board.
- **Share a board as a picture.** Drawn, not screenshotted — the whole board at one width with a
  line at the foot saying where it was made — and sent with a caption that carries the link, so
  the friend who gets it in a chat can tap their way to the app. Export writes every list to a
  text file through the same system sheet.
- **Eleven languages**, switchable inside the app without a restart. Arabic is fully right-to-left:
  layout, icon mirroring and drag arithmetic.

Boards are private until you publish one, and publishing sends a snapshot: the cards and the tier
definitions, never the ranking. Photos from your gallery stay on the phone; a published list shows
its web-hosted pictures and nothing else. Signing in is Google or nothing — no password to store,
no email shown to anyone — and is needed only to publish or to keep boards in an account.

An **AI image studio** is built, tested and switched off for the first release: it draws cards
for things that have no poster anywhere, through a proxy that holds the key, and a stub that draws
placeholder art when there is no proxy. One flag in `core:settings` hides the entrance and the
server refuses the request regardless, because hiding a button does not stop one being sent.

## Architecture

```
app                          ← Application, Activity, theme + locale bootstrap
navigation                   ← the NavHost, and the rail that replaces the tabs on wide windows
core:settings                ← theme, language, feature flags, what this phone has hidden
core:theme                   ← Material 3 colour scheme and typography; WindowShape
core:logging                 ← Timber, and the only module that knows a crash reporter exists
core:network                 ← App Check and ID-token interceptors shared by every caller
core:ui                      ← the pieces more than one feature draws
feature:tier:domain          ← models, repository ports, pure decision logic (no Android)
feature:tier:data            ← Room, Retrofit, image store, board sync, Hilt wiring
feature:tier:presentation    ← Compose screens, view models, strings
feature:account:*            ← who is signed in, as three states rather than a nullable user
feature:aistudio:*           ← generation and library ports, Gemini client, stub, credits
build-logic                  ← convention plugins: library, compose, hilt, room, network, navigation
```

Dependencies point inward: `presentation` and `data` depend on `domain` and never on each other.
Features never reference another feature's screens — each offers `NavGraphBuilder` extensions and
`navigation` wires them together, so `app` is bootstrap only. Module build files are a handful of
lines because the repeated setup lives in `build-logic` as typed convention plugins.

The backend is Firebase — Auth, App Check with Play Integrity, Crashlytics — with Cloud Functions
in front of Firestore, in a [separate repository](https://github.com/kellygracelab-dotcom/tieryourlife-proxy).
The app never holds a TMDB or Gemini key; the proxy attaches them. The reasoning behind the
screens is in [`docs/`](docs): the presentation-layer rules every screen follows and the design
specs, including the decisions reversed after using a feature on a real phone.

## Five places worth reading

Decisions, not just results — each is a small file with its reasoning written on it.

- [`WindowShape`](core/theme/src/main/kotlin/com/artiuillab/tieryourlife/core/theme/layout/WindowShape.kt)
  — three window shapes named after what a layout may do, and why "is this a tablet" was the
  wrong question.
- [`BoardOrder`](feature/tier/domain/src/main/kotlin/com/artiuillab/tieryourlife/feature/tier/domain/lists/BoardOrder.kt)
  — how your boards are ordered and filtered, pure, so the rules are argued with in a unit test.
- [`BoardSyncEngine`](feature/tier/data/src/main/kotlin/com/artiuillab/tieryourlife/feature/tier/data/sync/BoardSyncEngine.kt)
  — changes noticed by a fingerprint of the board rather than a dirty flag, and why two versions
  become two boards instead of a merge.
- [`AppCheckInterceptor`](core/network/src/main/kotlin/com/artiuillab/tieryourlife/core/network/AppCheckInterceptor.kt)
  — a request sent *without* the header rather than not sent, so the refusal is the server's and
  names its reason.
- [`moderation.ts`](https://github.com/kellygracelab-dotcom/tieryourlife-proxy/blob/main/functions/src/moderation.ts)
  and [`bans.ts`](https://github.com/kellygracelab-dotcom/tieryourlife-proxy/blob/main/functions/src/bans.ts)
  in the proxy — when a reported list hides, and for how long its author waits, as pure modules
  with the Firestore adapter kept thin around them.

## Tests

Every push runs the whole suite: JVM tests for the pure logic, and the full instrumentation
suite on emulators at API 24 and 34.

| Where | What they cover |
|---|---|
| JVM | search merging, board order, the sync plan and fingerprint, which window gets which layout, DTO parsing, repository behaviour against fakes |
| Instrumented — data | Room DAOs, transactions, cascades, migrations, the image store, pictures going up and coming back |
| Instrumented — presentation | Compose screens, view models, drag arithmetic, RTL layout; every screen rendered in all eleven languages |
| Instrumented — navigation | the rail: what is lit, what a tap chooses, what the new-board button does not choose |
| Instrumented — the README | the pictures above, drawn rather than photographed ([how](docs/screenshots.md)) |

Some of these exist because a bug got through first: a locale change that quietly broke three
things visible in no other language, a tier deletion that took recoverable items with it, a drag
preview that ran away from the finger in Arabic, a sync fingerprint that hashed a file path so two
phones would never agree they held the same board — and a CI script that reported every red run
green, which is why "green" here means the log was read, not the badge.

## How this was built

AI agents were part of the workflow, and the commit history reflects that. The division of
labour: specifications, architectural decisions, code review and on-device testing are mine; a
large share of the code was written by agents against those specifications. Screens were designed
before they were built, and the reasoning is in [`docs/`](docs).

Stating it plainly because it is visible in the history either way, and because how a codebase
was produced is a reasonable thing to want to know.

## Building

```bash
git clone https://github.com/kellygracelab-dotcom/TierYourLife.git
```

Open in Android Studio and run the `app` configuration. minSdk 24, targetSdk 37, JDK 21.

Everything local works from a clean clone: boards, drag and drop, trash, export, languages, and
catalogue search through Wikidata. The online half — TMDB, the community, publishing, boards kept
in an account, image generation — runs through the proxy behind Firebase App Check, which admits
only builds it has been told about. A build from your machine is not one of them, so those parts
answer "this copy could not be verified" until you point the app at your own Firebase project and
your own deployment of the [proxy](https://github.com/kellygracelab-dotcom/tieryourlife-proxy):

```properties
# local.properties
PROXY_BASE_URL=https://europe-west1-your-project.cloudfunctions.net/
```

Requests carry two tokens. App Check proves the caller is a build the project knows; an anonymous
Firebase ID token says which install, which is what the proxy meters against — App Check alone is
satisfied by every copy equally and cannot count.

## What's next

- Native-speaker review of the Arabic, Japanese and Turkish translations
- Ranking history: what moved between tiers and when
- A link to a published list that opens it in the app — needs the Play signing key, for App Links

---

<sub>Built with Kotlin & Jetpack Compose · learning flagship, summer 2026</sub>

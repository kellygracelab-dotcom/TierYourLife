# How the Android app is built

Modules, the decisions worth reading, the tests, and how the work was divided. Building it yourself is in [building.md](building.md).

## Architecture

```
app                          ← Application, Activity, theme + locale bootstrap
navigation                   ← the NavHost, and the rail that replaces the tabs on wide windows
core:settings                ← theme, language, feature flags, what this phone has hidden
core:theme                   ← Material 3 colour scheme and typography; WindowShape; the icons, bars and messages every screen shares
core:logging                 ← Timber, and the only module that knows a crash reporter exists
core:network                 ← App Check and ID-token interceptors, and the proxy client every caller shares
feature:tier:domain          ← models, repository ports, pure decision logic (no Android)
feature:tier:data            ← Room, Retrofit, image store, board sync, Hilt wiring
feature:tier:board           ← the board itself: rows, drag, sheets, the share picture — drawn for your lists and for strangers'
feature:tier:presentation    ← your lists, the board's view model, catalogue, trash, cover; strings
feature:community:*          ← the published feed, authors, moderation: what other people see
feature:settings:presentation ← settings, hidden lists, language, theme, backup, export
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
screens is in [`docs/`](.): the presentation-layer rules every screen follows and the design
specs, including the decisions reversed after using a feature on a real phone.

## Five places worth reading

Decisions, not just results — each is a small file with its reasoning written on it.

- [`WindowShape`](../core/theme/src/main/kotlin/com/artiuillab/tieryourlife/core/theme/layout/WindowShape.kt)
  — three window shapes named after what a layout may do, and why "is this a tablet" was the
  wrong question.
- [`BoardOrder`](../feature/tier/domain/src/main/kotlin/com/artiuillab/tieryourlife/feature/tier/domain/lists/BoardOrder.kt)
  — how your boards are ordered and filtered, pure, so the rules are argued with in a unit test.
- [`BoardSyncEngine`](../feature/tier/data/src/main/kotlin/com/artiuillab/tieryourlife/feature/tier/data/sync/BoardSyncEngine.kt)
  — changes noticed by a fingerprint of the board rather than a dirty flag, and why two versions
  become two boards instead of a merge.
- [`AppCheckInterceptor`](../core/network/src/main/kotlin/com/artiuillab/tieryourlife/core/network/AppCheckInterceptor.kt)
  — a request sent *without* the header rather than not sent, so the refusal is the server's and
  names its reason.
- [`moderation.ts`](https://github.com/kellygracelab-dotcom/tieryourlife-proxy/blob/main/functions/src/moderation.ts)
  and [`bans.ts`](https://github.com/kellygracelab-dotcom/tieryourlife-proxy/blob/main/functions/src/bans.ts)
  in the proxy — when a reported list hides, and for how long its author waits, as pure modules
  with the Firestore adapter kept thin around them.

## Tests

Every push runs the whole suite: JVM tests for the pure logic, and the full instrumentation
suite on emulators at API 26 and 34.

| Where | What they cover |
|---|---|
| JVM | search merging, board order, the sync plan and fingerprint, which window gets which layout, DTO parsing, repository behaviour against fakes |
| Instrumented — data | Room DAOs, transactions, cascades, migrations, the image store, pictures going up and coming back |
| Instrumented — presentation | Compose screens, view models, drag arithmetic, RTL layout; every screen rendered in all eleven languages |
| Instrumented — navigation | the rail: what is lit, what a tap chooses, what the new-board button does not choose |
| Instrumented — pictures | the README's and the store's pictures, drawn rather than photographed ([how](screenshots.md)) |

Some of these exist because a bug got through first: a locale change that quietly broke three
things visible in no other language, a tier deletion that took recoverable items with it, a drag
preview that ran away from the finger in Arabic, a sync fingerprint that hashed a file path so two
phones would never agree they held the same board — and a CI script that reported every red run
green, which is why "green" here means the log was read, not the badge.

### End-to-end, against the proxy

`:e2e` runs the app's real network layer against the proxy's own code in the Firebase emulators:
the Retrofit services, DTOs, JSON and error mapping, publishing, and sync between two phones on
one account, conflicts included. Only App Check (not asked for there) and the ID token (from the
Auth emulator) are swapped. The proxy's [CONTRACT.md](https://github.com/kellygracelab-dotcom/tieryourlife-proxy/blob/main/CONTRACT.md)
is the API both clients follow; its E2E section describes the emulators. CI runs this as its own job.

```sh
git clone https://github.com/kellygracelab-dotcom/tieryourlife-proxy ../tieryourlife-proxy
npm --prefix ../tieryourlife-proxy/functions ci
npm --prefix ../tieryourlife-proxy/functions run emulators:e2e:exec -- "cd \"$PWD\" && ./gradlew :e2e:testDebugUnitTest -Pe2e"
```

It needs Java 11+, Node 22 and `firebase-tools`; no login or keys. Without `-Pe2e` the tests are
compiled and skipped, so `./gradlew test` never needs the emulators. With another suite already on
the standard ports, pass the emulators a copy of `firebase.json` with other ports (`--config`), give
the run its own `TEMP`/`TMP`, and set `E2E_FUNCTIONS_URL` to the functions address
(`http://127.0.0.1:<port>/demo-tieryourlife/europe-west1`); the Auth address comes from
`FIREBASE_AUTH_EMULATOR_HOST`, which the emulators set.

## How this was built

AI agents were part of the workflow, and the commit history reflects that. The division of
labour: specifications, architectural decisions, code review and on-device testing are mine; a
large share of the code was written by agents against those specifications. Screens were designed
before they were built, and the reasoning is in [`docs/`](.).

Stating it plainly because it is visible in the history either way, and because how a codebase
was produced is a reasonable thing to want to know.

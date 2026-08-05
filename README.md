# TierYourLife

> A private ranking journal for Android. Put anything you care about on an **S / A / B / C / D**
> board — films, games, restaurants, albums, people's cooking — and keep it. Not a one-off
> tier-list generator: your boards live on the device, stay editable, and are yours alone.

🚧 **Work in progress** — built from scratch, in the open, as a learning flagship. The commit history *is* the story.

---

## What it does today

- **Boards.** Create lists, rename them, reorder tiers, edit each tier's label, caption and
  colour — light and dark shades set separately, by HSL sliders or hex.
- **Drag and drop.** Move an item between tiers or within one; the insertion point follows the
  pointer in reading order, right-to-left included.
- **Add items three ways.** Search a catalogue, type a name by hand, or pick photos from the
  gallery — several at once, one item per photo. Gallery images are downscaled on the way in,
  so a board of 200 items stays inside Android's 25 MB auto-backup quota.
- **Catalogue search.** One field, two sources behind it — TMDB and Wikidata — merged,
  deduplicated and ranked into a single list. If one source is down the other still answers.
- **Trash, not deletion.** Deleted lists and items go to a trash screen with the time they were
  removed and a one-tap restore. Deleting a tier does not take its trashed items with it.
- **Export.** Any list to a text file, then share it through the system sheet.
- **Eleven languages** — English, Ukrainian, Russian, Spanish, Portuguese (BR), German, French,
  Polish, Turkish, Japanese, Arabic — switchable inside the app without a restart or a flash of
  the old language. Arabic is fully right-to-left: layout, icon mirroring and drag arithmetic.
- **Light, dark, or follow the system**, applied before the first frame is drawn.

Everything is local. No account, no server, no analytics. The only network calls are the two
catalogue search sources and the images they point at.

## Architecture

```
app                     ← Activity, navigation host, theme + locale bootstrap
core:theme              ← Material 3 colour scheme and typography
feature:tier:domain     ← models, repository interfaces, pure search-merge logic (no Android)
feature:tier:data       ← Room, SharedPreferences, Retrofit, image store, Hilt wiring
feature:tier:presentation ← Compose screens, view models, strings
build-logic             ← convention plugins: library, compose, hilt, room, network, navigation
```

Dependencies point inward: `presentation` and `data` both depend on `domain`, and never on each
other. Each module's Gradle file is a handful of lines because the repeated setup lives in
`build-logic` as typed convention plugins rather than in copied blocks.

## Tech stack

- **Language:** Kotlin · Coroutines · Flow
- **UI:** Jetpack Compose · Material 3 · Navigation Compose (type-safe routes)
- **Architecture:** clean architecture · multi-module · unidirectional data flow
- **DI:** Hilt
- **Data:** Room (entities, views, transactions, exported schemas) · SharedPreferences
- **Network:** Retrofit · OkHttp · kotlinx.serialization · TMDB · Wikidata SPARQL
- **Images:** Coil 3
- **Build:** Gradle Kotlin DSL · version catalog · convention plugins · KSP
- **Tests:** JUnit4 · Compose UI tests · Room in-memory and migration tests · hand-written fakes
- **CI:** GitHub Actions — build, unit tests and lint, plus the full instrumentation suite on
  emulators at API 24 and 34

## Tests

313 tests, all of them run on every push:

| Where | Count | What they cover |
|---|---|---|
| JVM | 60 | domain logic, search merging, DTO mapping, repository behaviour against fakes |
| Instrumented — data | 80 | Room DAOs, transactions, cascades, migrations, the image store |
| Instrumented — presentation | 171 | Compose screens, view models, drag arithmetic, RTL layout |
| Instrumented — app | 2 | manifest contract (RTL support, config-change handling) |

Some of these exist because a bug got through first: a locale change that quietly broke three
things visible in no other language, a tier deletion that took recoverable items with it, a
floating drag preview that ran away from the finger in Arabic.

## Building

```bash
git clone https://github.com/kellygracelab-dotcom/TierYourLife.git
```

Open in Android Studio and run the `app` configuration. minSdk 24, targetSdk 37, JDK 21.

Catalogue search works out of the box through Wikidata. To enable the TMDB source as well, add
your own read access token to `local.properties` (which is not in the repository):

```properties
TMDB_READ_ACCESS_TOKEN=your_token_here
```

## What's next

- [ ] An AI feature: suggest where an item belongs on a board and explain the reasoning
- [ ] Screenshots and a short demo recording in this README
- [ ] Static analysis (detekt / ktlint) in CI

---

<sub>Built with Kotlin & Jetpack Compose · learning flagship, summer 2026</sub>

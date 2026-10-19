# Presentation layer — architecture rules

How screens are built in this codebase. These rules are enforced by review, not tooling;
when a rule and a file disagree, one of them is wrong and it is usually the file.

## Modules

Every feature is three Gradle modules: `feature:X:domain` (pure JVM — models and ports),
`feature:X:data` (implementations, storage, DI bindings), `feature:X:presentation`
(Compose UI, ViewModels, resources). `feature:tier` has a fourth, `board`: the renderer of a
board, which its own screens and the community's draw alike. Presentation depends on domain
and on `core:theme`, which holds the icons, bars and generic strings every screen shares.
Features never depend on each other's presentation; the `:navigation` module composes them
into a graph, and `app` is bootstrap — Application, Activity, DI wiring.

`./gradlew checkModuleBoundaries` (run in CI) enforces this from the build files: `app`
reaches screens only through `navigation`; a domain depends only on domains; `core` sees no
feature; data sees neither screens nor another feature's data; screens never see data,
`navigation`, or another feature's screens (the shared `board` excepted). A new rule goes
there, not into a reviewer's memory.

## The screen package law

One screen surface = one package (`tierdetail`, `tierlists`, `trash`, `settings`,
`catalogue`, `aistudio`). The package root holds **only the screen's skeleton**:

```
tierdetail/
├── TierDetailScreen.kt      the screen (stateful entry + stateless Content + previews)
├── TierDetailViewModel.kt
├── TierDetailUiState.kt
├── TierDetailActions.kt     (only when the callback count earns it — see below)
├── TierDetailTestTags.kt
└── components/              everything the screen is built from
```

*In the root — what defines the screen; in `components/` — what it is built from.*
No exceptions for chrome: top bars, preview fixtures, and non-visual helpers such as
`RelativeTime` are building blocks and live in `components/`.

## A screen is three layers

1. **`XScreen(onBack, ..., viewModel = hiltViewModel())`** — the only place that touches
   the ViewModel. Collects state with `collectAsStateWithLifecycle`, owns UI-local state
   that does not deserve a ViewModel (`rememberSaveable` field text, dialog visibility),
   translates ViewModel methods into callbacks.
2. **`XScreenContent(state, callbacks...)`** — stateless, a pure function of its
   arguments. No Hilt, no ViewModel. Previews and UI tests render this layer, which is
   why every screen state is reachable from a fixture.
3. **Components** — stateless by the same rule; state is hoisted. Nothing below Content
   reaches for a ViewModel.

## State and ViewModel

- One `StateFlow<XUiState>` per screen. Load-gated screens use a sealed hierarchy
  (`Loading / Success / Error`); accumulative screens use a data class with defaults.
- ViewModel methods are user intentions (`send`, `regenerate`, `addToList`): launch,
  call a domain port, refresh state. No Compose imports, no `Context`, no `R.string`
  below the presentation layer — user-facing strings live in resources, plurals for
  quantities, all locales kept in parity.

## Actions

When a Content signature outgrows a glance (more than ~5–7 callbacks), the callbacks are
bundled into a data class of defaulted lambdas (`TierDetailActions`, born when the screen
hit 20 parameters). Below that threshold, pass callbacks individually (`aistudio` does).
The type doubles as the screen's verb inventory.

## Eviction rules — where code lives

Place code where its audience is:

| Signal | Destination |
|---|---|
| Unit is used by 2+ files | its own file, named after the unit |
| Distinct visual block of the screen body | the screen's `components/` |
| Code↔test contract (test tags) | `XTestTags.kt` in the screen package |
| Helper consumed across screen packages | the module's `common/` |
| Preview fixtures | one `XPreviewData.kt` per surface, in `components/` |
| Fake of a port that tests in 2+ modules need | `testFixtures` of the module that owns the port (`FakeAppPreferences` in `core:settings`, `RecordingTierRepository` in `feature:tier:domain`, `FakeCommunityRepository` in `feature:community:domain`, `FakeAccountRepository` in `feature:account:domain`) |

A screen file may keep: the entry, Content, `private` pieces used once, and previews.

## Test tags

An `internal object` per screen, in that screen's package, values prefixed with the
screen name, parametrized tags as functions. Tags belong to the screen that owns the
nodes — a tag consumed by `catalogue` does not live in `tierdetail`'s object. The string
values are a contract with the instrumented tests: renaming a constant is free, changing
a value means updating the tests in the same commit. Tags are the test channel;
`contentDescription` is the accessibility channel — never conflate them.

## Tests

- **ViewModels run on the JVM**, in `src/test`, with `MainDispatcherRule` from
  `core:testing` standing one thread in for the main thread. Real time, no virtual
  clock: the same `runBlocking` test reads as it would on a phone.
- **Routes without `Bundle`:** a ViewModel reads its route with the reader next to the
  route type (`savedStateHandle.tierDetailRoute()`), not `toRoute()`, which goes through
  Android's `Bundle` and would keep the test on an emulator. Each feature's
  `RouteFromHandleTest` (instrumented, real `NavHost`) holds the reader equal to
  `toRoute()`; add a case there when a route gains an argument.
- **Waiting for state** uses `firstWithin { }` from `core:testing`, never a bare
  `first { }`: a state that never comes fails that one test with the last value seen,
  instead of hanging the instrumentation job for an hour.
- **Fakes** of shared ports are open classes with quiet defaults; a test overrides only
  the calls it is about (`object : FakeCommunityRepository() { ... }`).
- **Screenshots on the JVM:** `TierDetailScreenshotTest` draws the board with Robolectric
  and Roborazzi and compares it with `feature/tier/board/src/test/screenshots/`. CI runs
  `verifyRoborazziDebug`; a deliberate visual change re-records with
  `./gradlew recordRoborazziDebug` and commits the new pictures with it.
- `androidTest` is for what needs a device: Compose UI, Room, the file system,
  pictures.

## Previews

- **Screens:** `@TierYourLifeDevicePreviews` (five devices including large-font) in both
  themes for the primary state, plus single-device previews for the remaining states of
  the screen's state machine. Every fixture defined must be rendered by some preview.
- **Components:** a light and a dark `@Preview` for components with a visual identity or
  states of their own. Trivial wrappers get none — preview noise is a cost.
- Dialog/sheet bodies that cannot render inside their window wrapper get an extracted
  `...Content` composable — extraction must not change behavior.

## Subpackages inside components/

Flat until ~12–15 files **and** real clusters exist. A cluster earns a subpackage when it
is a coherent subsystem with its own vocabulary (`drag/` — the drag engine; `sheets/` —
modal surfaces; `rows/` — the tier/tile rendering), not an alphabetical bucket. Small
`components/` folders stay flat.

## Navigation

Each feature owns its routes: `@Serializable` route types, `NavGraphBuilder.xScreen(...)`
destination extensions, `NavController.navigateToX(...)` offers — in the feature's
`navigation/` package. Features know their own screens and never another feature's
routes; cross-feature transitions are callbacks wired in `:navigation`'s host. Results
travel back through the previous back-stack entry's `SavedStateHandle`.

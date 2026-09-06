# Architecture plan: from one big feature to three

Written 2026-09-05. The layering is sound (three pure `domain` modules, `data`
never reached from `presentation`, one-way dependencies), but `feature/tier`
has grown into the whole app: 112 presentation files against 18 in `account`
and 16 in `aistudio`, holding the community feed, authors, moderation, settings,
the trash, the catalogue, the cover screen and sharing. This plan pulls two
features out of it. Behaviour does not change at any step and the Room schema
is not touched.

## Target

```
feature/tier        domain, data, board (the renderer), presentation (your lists, catalogue, trash, cover)
feature/community   feed, author, someone else's list, my published, reports, moderation, bans
feature/settings    settings, hidden lists, language, theme, backup, account row
feature/account     unchanged
feature/aistudio    unchanged
navigation          HomeScreen (tabs/rail) + graph; the every-screen tests live here
core/theme          theme, layout, messages, and the icons, bars and generic strings every screen shares
core/…              without core/ui
```

Dependencies stay one-way: `community.domain -> tier.domain` (a published list
is built from a `TierList`), `community.presentation -> tier.board` (a
stranger's list is drawn by `TierDetailScreenContent`),
`settings.presentation -> tier/account/community.domain`. No `data` module
reaches another feature's `data`, and no feature's `presentation` reaches
another's: only `navigation` sees them all.

## Steps

One PR per step, CI green by log, squash-merged, only `main` left. Steps 1, 3,
4 and 5 are pure moves: the diff is imports and paths, `git diff -M` shows
renames. Step 2 is the only one that changes logic.

| # | Step | Status |
|---|------|--------|
| 1 | Cheap moves inside existing modules: `ModerationScreen`, `ModerationViewModel`, `TakeDownSheet` from the `settings` package to `community`; `core/ui` (one file) folded into `core/theme`; the one-file domain packages `lists`, `ordering`, `search`, `export` reduced to two | done, #89 |
| 2 | Split the home screen: `CommunityFeedScreen` + `CommunityFeedViewModel` out of `TierListsScreen` (880 lines) and `TierListsViewModel` (530); the feed, `FeedControlsRow` and `SuggestedAuthors` go with them; `HomeScreen` in `navigation` owns which half is open and hands each half the tab row (`HomeTabs` stays with its strings); the `tierlists` tests split the same way | done |
| 3 | New modules `feature/community/{domain,data,presentation}`: the models (`PublishedList`, `CommunityPage`, `Following`, `ModerationReport`, `BanLength`, `ReportReason`, `PublishError`, `CommunityRepository`), `CommunityApi` + DTOs + `RetrofitCommunityRepository`, the whole `community` package and the feed from step 2. The shared network client stays in `core/network`; the repository binding travels. Seven androidTests move | done; the proxy `Retrofit` moved into `core/network`, publishing reaches this phone's pictures through the `OwnPictures` port in `tier.domain`, and `PublishFingerprint` moved to `tier.domain` with it |
| 4 | `feature/settings/presentation`: `SettingsScreen`, its sections, `HiddenScreen`; four tests with them | done; 70 strings and the TMDB logo went with it, and the rail now lights a destination by its route's last name rather than a substring, since `SettingsRoute.Hidden` carries "Settings" too |
| 5 | `EveryScreenInEveryLanguageTest` and `ReadmeScreenshotTest` from `tier/presentation/common` to `navigation/androidTest`, the only module that sees every screen. They call `internal` screen contents, which become public on the way; README screenshots regenerated | done with step 3, since the tests could no longer see the community screens from the tier module |
| 6 | Optional, later: the board renderer into `feature/tier/board` so `community.presentation` no longer depends on `tier.presentation` | done; the icons, section label, home bars, deleted-item snackbar and the generic strings went to `core/theme` on the way, so `settings.presentation` dropped the dependency too. Only `navigation` still sees `tier.presentation` |

Afterwards, one more pass laid the two new modules out by the screen-package
law of `docs/architecture-presentation.md`: `community` has `feed/`, `list/`,
`author/`, `published/` and `moderation/`, `settings` has `hidden/`, every
test-tag object has its own file, and the icon files in `core/theme/ui` are
named by what they hold.

Before step 3: if `RetrofitCommunityRepository` or `BoardSyncEngine` writes
`publishedId` straight into `TierDao`, add a method on `TierRepository` first,
or `community.data` would pull in `tier.data`.

Order is fixed: 2 before 3, because a module is easier to move once the feed is
already apart from the list of boards.

## Checks at every step

- Full local gate: `assembleDebug testDebugUnitTest ktlintCheck lint`, plus the
  instrumented tests of the touched modules on an emulator.
- CI read from the log (`FAILED` count, `BUILD SUCCESSFUL` count), not the badge.
- After steps 2 and 3 a debug build on the phone over the debug build already
  there: home screen, feed, open a stranger's list, Mine, save, settings,
  moderation.
- Hilt: a duplicated binding fails at app start, not at compile time, so the
  app is launched after every module move.
- Test tags (`TierListsTestTags`, `CommunityTestTags`) move with their screens,
  or the neighbouring modules' tests stop compiling.

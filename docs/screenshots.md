# The pictures in the README

They are drawn by a test, not taken by hand.

`ReadmeScreenshotTest` renders each screen from invented content and writes a PNG per screen per
theme. Two reasons it works that way:

- **Nobody's real boards end up in a public README.** The old ones were photographs of whoever was
  holding the phone — their lists, their name, their half-finished test data. A public README is
  the last place a real person's name should arrive by accident, and it did.
- **They can be redrawn.** Every change to a colour, a type scale or a layout used to leave the
  README slowly wrong, because refreshing it meant somebody re-taking twelve screenshots by hand.
  Now it is one command.

## Redrawing them

```bash
ANDROID_SERIAL=<phone> ./gradlew :feature:tier:presentation:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.artiuillab.tieryourlife.feature.tier.presentation.common.ReadmeScreenshotTest
```

The files land under
`feature/tier/presentation/build/outputs/connected_android_test_additional_output/debugAndroidTest/connected/<device>/readme/`.
Copy them into `docs/screenshots/`.

The picture is the size of whatever drew it, so use a device with an ordinary phone window — the
committed set came from a 1080×2640 phone at 3x. Run it a second time against a tablet and copy
`board-light`, `community-light` and `settings-dark` into `docs/screenshots/tablet/` for the
tablet section.

Gradle clears that output directory at the start of each run, so copy the phone set out before
starting the tablet one.

## Two things it depends on

**Artwork comes over the network.** The cards use real TMDB poster URLs, so the device needs to be
online or the tiles fall back to their titles. That is a worse picture, not a failing test —
nothing here asserts, and a screenshot generator that can fail the build would be a bad trade.

**Dialogs are captured from their own window.** `list-actions` and `report` draw in a separate
window, so they are captured through `isDialog()`; the feed is still drawn behind them, because a
scrim over nothing is not what anybody sees.

## What is still taken by hand

`drag.gif`, and the three AI-studio pictures. The gif is a recording, and the studio needs a live
generator. If those are ever re-taken, take them on a device signed into an account that is not a
real person's.

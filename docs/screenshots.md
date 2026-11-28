# The pictures in the README

Since 28.11.2026 the README shows two kinds of picture, both in `docs/screenshots/`.

**The strip at the top**, `play-gallery.jpg`, is the Play listing's gallery: the six frames uploaded
on 20.11.2026, side by side, 2000 px wide. When the listing's frames change, replace it with the
new preview.

**The eight phone screens** are drawn by `StoreListingScreenshotTest`, the test that draws the
Play listing's phone pictures; [store-listing.md](store-listing.md) says how to run it. It uses
invented content and only pictures made for the app, so no poster or real person's list reaches a
public page. Run it on an ordinary phone window (the committed set came from 1080×2400 at 420 dpi),
copy the eight files below and shrink them to 540 px wide.

| In the README | The test's file |
|---|---|
| `board-dark.png` | `dark-16-hero-board.png` |
| `duel-dark.png` | `dark-18-game.png` |
| `section-dark.png` | `dark-19-section.png` |
| `community-dark.png` | `dark-24-community.png` |
| `rankings-light.png` | `light-4-rankings.png` |
| `pool-light.png` | `light-11-pool.png` |
| `lists-light.png` | `light-12-lists.png` |
| `settings-light.png` | `light-14-settings.png` |

## The store icon

Google Play wants 512x512 and the largest icon in the app is 192x192, so it is
drawn rather than exported:

```bash
ANDROID_SERIAL=<device> ./gradlew :app:connectedDebugAndroidTest   -Pandroid.testInstrumentationRunnerArguments.class=com.artiuillab.tieryourlife.StoreIconTest
```

The file lands under
`app/build/outputs/connected_android_test_additional_output/debugAndroidTest/connected/<device>/store/icon-512.png`
and is uploaded to Play by hand -- it is not committed, because nothing in the
repository reads it.

`StoreIconTest` draws `R.mipmap.ic_launcher`, so the store icon and the launcher
icon cannot disagree. Drawing an adaptive icon applies the device's mask, which
is why the test draws it oversized and keeps the middle: what comes back from
the whole 108dp canvas has the corners cut out, and Play rejects transparency.

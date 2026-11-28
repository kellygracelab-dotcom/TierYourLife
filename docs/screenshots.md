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

## The older tests

`ReadmeScreenshotTest` and `ReadmeTabletScreenshotTest` drew the README's pictures before that,
with TMDB posters on the cards. The README no longer uses their output; the rest of this page
describes them.

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
ANDROID_SERIAL=<phone> ./gradlew :navigation:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.artiuillab.tieryourlife.navigation.ReadmeScreenshotTest
```

The files land under
`navigation/build/outputs/connected_android_test_additional_output/debugAndroidTest/connected/<device>/readme/`.

Copy over **only the ones the README points at** — the test draws every screen in both themes and
the README uses about half of them, and an unread PNG is weight in a repository that nobody ever
removes. Then shrink them: they are displayed at 240px (300 for the tablet), so 480px wide is
already twice what any screen needs, and the full-size set is four times the bytes for no
difference at all.

```bash
python -c "from PIL import Image; import sys; [Image.open(p).convert('RGB').resize((480, round(Image.open(p).height*480/Image.open(p).width))).save(p, optimize=True) for p in sys.argv[1:]]" docs/screenshots/*.png
```

The picture is the size of whatever drew it, so use a device with an ordinary phone window — the
committed set came from a 1080×2400 phone at 420dpi.

## The tablet pictures

They come from a different test in a different module, because the rail does:

```bash
ANDROID_SERIAL=<tablet> ./gradlew :navigation:connectedDebugAndroidTest   -Pandroid.testInstrumentationRunnerArguments.class=com.artiuillab.tieryourlife.navigation.ReadmeTabletScreenshotTest
```

`ReadmeTabletScreenshotTest` draws the same composition `TierYourLifeNavHost` makes — the rail,
then the screen beside it — with fixtures where the view models would be. Both tests live in
`navigation`, the one module that sees every feature's screens; a feature module cannot see the
rail and should not, since a screen does not know what the app puts beside it. For a while the
tablet section showed phone pictures stretched to tablet width, under a paragraph about a rail
they did not contain.

The files land under
`navigation/build/outputs/connected_android_test_additional_output/debugAndroidTest/connected/<device>/readme-tablet/`.
Copy `board-light`, `community-light` and `settings-dark` into `docs/screenshots/tablet/` and
shrink them to 600px wide. The committed set came from a 2560×1600 tablet at 320dpi.

Run on a phone, the test measures the window, finds no rail, logs that and draws nothing — rather
than writing phone-shaped pictures into a folder called tablet.

## Two things it depends on

**Artwork comes over the network.** The cards use real TMDB poster URLs, so the device needs to be
online or the tiles fall back to their titles. That is a worse picture, not a failing test —
nothing here asserts, and a screenshot generator that can fail the build would be a bad trade.

**Dialogs are captured from their own window.** `list-actions` and `report` draw in a separate
window, so they are captured through `isDialog()` — which yields the dialog alone, cropped to its
own bounds, without the feed behind it.

That is also why the take-down sheet is not here. A `ModalBottomSheet`'s window is the whole
screen, so capturing it the same way gives a grey field with the sheet clipped along the bottom
edge rather than a picture of a sheet.

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

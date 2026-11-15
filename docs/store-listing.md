# The phone pictures in the Play listing

Drawn by `StoreListingScreenshotTest`. In the light theme: a new board's five tiers, all full; the
same board with a card held up over the top row; a game's duel; a list's Rankings sheet; the Food
section with its topics; one list as its author placed it and as a reader did (`rankings-a`,
`rankings-b`, with the bar naming whose rows, and `-rows` without it, all five tiers in frame); then a
board, a board with its pool, Your lists, Community and Settings.
The dark theme repeats the last eight for comparison. Files are named `light-N-…` and `dark-N-…`.

Only pictures made for the app go on a card: the fruit and vegetables in
`navigation/src/androidTest/assets/listing/`, drawn by `docs/store-listing/draw_pictures.py`, and
plain text cards. No posters, covers, stills, logos or characters, because a store review reads
them as somebody else's work. That is the difference from the README pictures, which use TMDB
artwork.

## Redrawing them

Play wants 9:16. For pictures that can be cropped, set the emulator to 1440×2560 at density 640
(the same 360 dp as 1080×1920 at 480) and put it back afterwards:

```bash
adb -s <emulator> shell wm size 1440x2560
adb -s <emulator> shell wm density 640
ANDROID_SERIAL=<emulator> ./gradlew :navigation:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.artiuillab.tieryourlife.navigation.StoreListingScreenshotTest
adb -s <emulator> shell wm size reset
adb -s <emulator> shell wm density reset
```

The files land under
`navigation/build/outputs/connected_android_test_additional_output/debugAndroidTest/connected/<device>/listing/`.
They are not kept in the repository; the copy uploaded to Play Console lives with the release
materials outside it.

To change a picture, edit the script and run `python draw_pictures.py <folder>`; it writes 400×600
PNGs, and for the topics 1280×800 ones (`topic-*.png`) with three whole drawings each, since a topic's
tile is 16:10 and crops a single drawing to a slice.

# The phone pictures in the Play listing

Drawn by `StoreListingScreenshotTest`, dark theme, eight screens: a board, Settings, Your lists, a
board with its pool, Community, a game's duel, the Food section with its topics, and a list's
Rankings sheet.

Only pictures made for the app go on a card: the fruit and vegetables in
`navigation/src/androidTest/assets/listing/`, drawn by `docs/store-listing/draw_pictures.py`, and
plain text cards. No posters, covers, stills, logos or characters, because a store review reads
them as somebody else's work. That is the difference from the README pictures, which use TMDB
artwork.

## Redrawing them

Play wants 9:16, so set the emulator's window to 1080×1920 first and put it back afterwards:

```bash
adb -s <emulator> shell wm size 1080x1920
ANDROID_SERIAL=<emulator> ./gradlew :navigation:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.artiuillab.tieryourlife.navigation.StoreListingScreenshotTest
adb -s <emulator> shell wm size reset
```

The files land under
`navigation/build/outputs/connected_android_test_additional_output/debugAndroidTest/connected/<device>/listing/`.
They are not kept in the repository; the copy uploaded to Play Console lives with the release
materials outside it.

To change a picture, edit the script and run `python draw_pictures.py <folder>`; it writes 400×600
PNGs.

package com.artiuillab.tieryourlife.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.core.theme.layout.LocalWindowShape

/**
 * A cover screen gets an app of its own rather than the app with most of its
 * controls refusing. The two meet here: what the cover hands over, and the
 * board the main screen was on, are kept above both and outlive the
 * activity being made again each time the phone folds or opens.
 */
@Composable
fun TierYourLifeContent(
    themeChoice: ThemeChoice,
    onThemeChoiceChange: (ThemeChoice) -> Unit,
    languageTag: String?,
    onLanguageTagChange: (String?) -> Unit,
) {
    var handOff by rememberSaveable(stateSaver = HandOffSaver) { mutableStateOf<HandOff?>(null) }
    var boardOnMain by rememberSaveable { mutableStateOf<Long?>(null) }
    val modes = rememberSaveableStateHolder()

    if (LocalWindowShape.current.isGlanceable) {
        modes.SaveableStateProvider(COVER_STATE) {
            CoverHost(
                handOff = handOff,
                onHandOff = { subject -> handOff = HandOff(subject, System.currentTimeMillis()) },
                onCancelHandOff = { handOff = null },
                boardOnMain = boardOnMain,
            )
        }
    } else {
        // The cover starts afresh each time the phone shuts: where it was last
        // time is not where the person is now. The main screen keeps its place.
        LaunchedEffect(Unit) { modes.removeState(COVER_STATE) }
        val wanted = HandOffRules.stillWanted(handOff, System.currentTimeMillis())
        // Too late is dropped here, not shown again the next time the phone shuts.
        if (handOff != null && wanted == null) LaunchedEffect(handOff) { handOff = null }
        TierYourLifeNavHost(
            themeChoice = themeChoice,
            onThemeChoiceChange = onThemeChoiceChange,
            languageTag = languageTag,
            onLanguageTagChange = onLanguageTagChange,
            // The phone opening is the signal: the window stops being a cover.
            handOff = wanted,
            onHandOffDone = { handOff = null },
            onBoardShown = { boardOnMain = it },
        )
    }
}

private const val COVER_STATE = "cover"

package com.artiuillab.tieryourlife.feature.tier.presentation.cover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.ui.CoverType
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.core.theme.R as ThemeR

/** What the main screen opens once the phone is opened, with the title the cover names it by while it waits. */
sealed interface CoverHandOffSubject {
    /** One of your lists; [tierId] is the tier the cover had open or in view. */
    data class OwnList(val listId: Long, val title: String, val tierId: Long?) : CoverHandOffSubject

    /** One card, on its list's board, at its tier. */
    data class Card(val listId: Long, val tierId: Long, val title: String) : CoverHandOffSubject

    data class CommunityList(val publishedId: String, val title: String) : CoverHandOffSubject

    /** Nothing in particular: the app itself, from the cover's empty lists. */
    data object App : CoverHandOffSubject
}

/**
 * The hand-off's wait (§9): the cover says what will open and that opening
 * the phone is the way. Cancel, Back or the time running out return to where
 * the cover was, untouched underneath.
 */
@Composable
fun CoverHandOffWait(subject: CoverHandOffSubject, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            // It stands over the cover it will return to: nothing beneath takes a tap.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            .testTag(CoverTestTags.HAND_OFF),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DevicesFoldIcon(40.dp, MaterialTheme.colorScheme.primary)
            Text(
                stringResource(R.string.cover_hand_off_title),
                modifier = Modifier.padding(top = 4.dp),
                style = CoverListType.HandOffTitle,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                body(subject),
                style = CoverListType.HandOffBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.padding(top = 8.dp).height(36.dp).testTag(CoverTestTags.HAND_OFF_CANCEL),
            ) { Text(stringResource(ThemeR.string.action_cancel), style = CoverType.Button) }
        }
    }
}

@Composable
private fun body(subject: CoverHandOffSubject): String = when (subject) {
    is CoverHandOffSubject.OwnList -> stringResource(R.string.cover_hand_off_list, subject.title)
    is CoverHandOffSubject.Card -> stringResource(R.string.cover_hand_off_card, subject.title)
    is CoverHandOffSubject.CommunityList -> stringResource(R.string.cover_hand_off_community, subject.title)
    CoverHandOffSubject.App -> stringResource(R.string.cover_hand_off_app)
}

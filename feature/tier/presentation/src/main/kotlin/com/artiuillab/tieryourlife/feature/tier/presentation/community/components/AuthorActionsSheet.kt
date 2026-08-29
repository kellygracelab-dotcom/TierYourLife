package com.artiuillab.tieryourlife.feature.tier.presentation.community.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.community.AuthorTestTags
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.CategoryIcon

/**
 * What a reader can do about a person rather than a list. Reporting is not
 * offered here on purpose: a complaint needs something to look at, and the
 * lists below carry that on a long press.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AuthorActionsSheet(
    name: String,
    photoUrl: String?,
    onDismiss: () -> Unit,
    onHideAuthor: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            Modifier
                .navigationBarsPadding()
                .testTag(AuthorTestTags.ACTIONS_SHEET),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AuthorFace(photoUrl = photoUrl, name = name, size = 32.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            HorizontalDivider(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onHideAuthor)
                    .testTag(AuthorTestTags.ACTION_HIDE_AUTHOR)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryIcon(20.dp, MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.community_action_hide_author, name),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

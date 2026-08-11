package com.artiuillab.tieryourlife.feature.tier.presentation.trash.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.DeleteSweepIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.common.MoreIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.BackIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.trash.TrashTestTags

@Composable
internal fun TrashTopBar(
    isEmpty: Boolean,
    menuExpanded: Boolean,
    onBack: () -> Unit,
    onMoreClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onEmptyTrashClick: () -> Unit,
) {
    val backDescription = stringResource(R.string.cd_trash_back)
    val moreDescription = stringResource(R.string.cd_more)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = backDescription }
                    .testTag(TrashTestTags.BACK),
            ) { BackIcon() }

            if (!isEmpty) {
                Text(
                    text = stringResource(R.string.trash_title),
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Box {
                    IconButton(
                        onClick = onMoreClick,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = moreDescription }
                            .testTag(TrashTestTags.MORE),
                    ) { MoreIcon() }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.menu_empty_trash),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                            leadingIcon = { DeleteSweepIcon(20.dp, MaterialTheme.colorScheme.error) },
                            onClick = onEmptyTrashClick,
                            modifier = Modifier.testTag(TrashTestTags.MENU_EMPTY_TRASH),
                        )
                    }
                }
            }
        }

        if (!isEmpty) {
            Text(
                text = stringResource(R.string.trash_subtitle),
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

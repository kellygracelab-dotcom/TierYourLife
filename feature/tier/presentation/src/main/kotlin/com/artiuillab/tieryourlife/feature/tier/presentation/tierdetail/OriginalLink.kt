package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.theme.ui.ClearIcon
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.presentation.R

/** The id of the Community list this board is a copy of, if it is one. */
internal fun TierList.original(): String? = rankingOf ?: takenFrom

/** [list]'s link to its original when it is somebody's copy and the link was not hidden on this device. */
@Composable
internal fun originalLinkOf(list: TierList?, links: OriginalLinkViewModel, onOpenOriginal: (String) -> Unit): (@Composable () -> Unit)? {
    val hidden by links.hidden.collectAsStateWithLifecycle()
    val original = list?.original() ?: return null
    if (list.uid in hidden) return null
    return { OriginalLink(onOpen = { onOpenOriginal(original) }, onHide = { links.hide(list.uid) }) }
}

/**
 * Under the title of a copy, as the website has it: a plain link to the list
 * it came from, naming nobody. Hidden, it stays hidden on this device; the
 * list settings still open the original.
 */
@Composable
internal fun OriginalLink(onOpen: () -> Unit, onHide: () -> Unit, modifier: Modifier = Modifier) {
    val hide = stringResource(R.string.tier_detail_hide_original_list)
    Row(modifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onOpen, contentPadding = PaddingValues(horizontal = 12.dp)) {
            Text(stringResource(R.string.tier_detail_original_list), style = MaterialTheme.typography.labelLarge)
        }
        IconButton(onClick = onHide, modifier = Modifier.size(40.dp)) {
            Box(Modifier.semantics { contentDescription = hide }) {
                ClearIcon(16.dp, MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

package com.artiuillab.tieryourlife.feature.tier.board.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.layout.AdaptiveSheet
import com.artiuillab.tieryourlife.core.theme.ui.CheckCircleIcon
import com.artiuillab.tieryourlife.core.theme.ui.ShareIcon
import com.artiuillab.tieryourlife.feature.tier.board.R
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags

/** What the sheet after a first publish does; [onOpen] is null where Community cannot be reached from here. */
class LiveSheetCalls(
    val onShare: () -> Unit,
    val onCopy: () -> Unit,
    val onImage: () -> Unit,
    val onOpen: (() -> Unit)?,
    val onDone: () -> Unit,
)

/**
 * "Your list is live": what a first publish turns into instead of a passing
 * "Published", with the link and the ways to send it. Share leads, since on a
 * phone the system's sheet already holds every chat and Copy. [link] is null
 * for the frame or two before the new id reaches the board.
 */
@Composable
fun LiveSheet(section: String, link: String?, calls: LiveSheetCalls) {
    AdaptiveSheet(onDismiss = calls.onDone, modifier = Modifier.testTag(TierDetailTestTags.LIVE_SHEET)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CheckCircleIcon(48.dp, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.size(12.dp))
            Text(
                stringResource(R.string.list_live_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(6.dp))
            Text(
                stringResource(R.string.list_live_body, section),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (link != null) {
                Spacer(Modifier.size(16.dp))
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        link.removePrefix("https://"),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.size(16.dp))
            Button(
                onClick = calls.onShare,
                enabled = link != null,
                modifier = Modifier.fillMaxWidth().testTag(TierDetailTestTags.LIVE_SHARE),
            ) {
                ShareIcon(18.dp, MaterialTheme.colorScheme.onPrimary.takeIf { link != null } ?: MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.share_button))
            }
            Spacer(Modifier.size(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = calls.onCopy,
                    enabled = link != null,
                    modifier = Modifier.weight(1f).testTag(TierDetailTestTags.LIVE_COPY),
                ) { Text(stringResource(R.string.list_live_copy), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                OutlinedButton(
                    onClick = calls.onImage,
                    modifier = Modifier.weight(1f).testTag(TierDetailTestTags.LIVE_IMAGE),
                ) { Text(stringResource(R.string.list_live_image), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            Spacer(Modifier.size(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val open = calls.onOpen
                if (open != null && link != null) {
                    TextButton(onClick = open, modifier = Modifier.testTag(TierDetailTestTags.LIVE_OPEN)) {
                        Text(stringResource(R.string.list_live_open))
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = calls.onDone, modifier = Modifier.testTag(TierDetailTestTags.LIVE_DONE)) {
                    Text(stringResource(R.string.list_live_done))
                }
            }
        }
    }
}

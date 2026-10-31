package com.artiuillab.tieryourlife.feature.community.presentation.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.ui.ChevronRightIcon
import com.artiuillab.tieryourlife.core.theme.ui.GridViewIcon
import com.artiuillab.tieryourlife.feature.community.presentation.R

/**
 * Home's last slot when there are more sections than slots: no art, the
 * grid sign, "All sections" and how many. On a phone it is the full-width
 * row, the label and the count on one line and a chevron at the end.
 */
@Composable
internal fun AllSectionsTile(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    row: Boolean = false,
    style: TileStyle = TileStyle(),
) {
    val title = stringResource(R.string.all_sections_title)
    val counted = countText(R.plurals.all_sections_count, count)
    val ink = MaterialTheme.colorScheme.onSurface
    val dim = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier
            .clip(RoundedCornerShape(style.corner))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$title, $counted" }
            .testTag(ExploreTestTags.ALL_SECTIONS_TILE),
    ) {
        if (row) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(start = style.start, end = style.end),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                GridViewIcon(style.icon, dim)
                Text(title, style = style.label, color = ink, maxLines = 1)
                Text(counted, modifier = Modifier.weight(1f), style = CaptionStyle, color = dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                ChevronRightIcon(20.dp, dim)
            }
        } else {
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = style.start, end = style.end, bottom = style.bottom),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GridViewIcon(style.icon, dim)
                    Spacer(Modifier.width(7.dp))
                    Text(title, style = style.label, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (style.showsSubline) {
                    Text(counted, style = CaptionStyle, color = dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

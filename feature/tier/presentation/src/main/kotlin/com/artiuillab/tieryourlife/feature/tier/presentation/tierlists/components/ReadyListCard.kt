package com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.titleIn
import com.artiuillab.tieryourlife.feature.tier.board.common.currentLanguageTag
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.TierListsTestTags

/** "New here? Try a ready list": a popular list to rank before making one's own (artboard 50). */
@Composable
internal fun ReadyListCard(list: PublishedListSummary, onTry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().testTag(TierListsTestTags.READY_LIST)) {
        Text(
            text = stringResource(R.string.ready_list_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
        )
        Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TierBands(list.tierColors)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(
                            text = list.titleIn(currentLanguageTag()),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = pluralStringResource(R.plurals.ready_list_cards, list.itemCount, list.itemCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Button(
                    onClick = onTry,
                    modifier = Modifier.fillMaxWidth().testTag(TierListsTestTags.READY_LIST_TRY),
                ) { Text(stringResource(R.string.ready_list_try)) }
            }
        }
    }
}

/** The list's own tier colours, stacked like the bands of a board. */
@Composable
private fun TierBands(colors: List<String>) {
    val bands = colors.mapNotNull { runCatching { Color(it.toColorInt()) }.getOrNull() }.take(MAX_BANDS)
        .ifEmpty { DEFAULT_BANDS }
    Column(Modifier.width(40.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        bands.forEach { band ->
            Box(Modifier.fillMaxWidth().height(10.dp).background(band, RoundedCornerShape(3.dp)))
        }
    }
}

private const val MAX_BANDS = 5
private val DEFAULT_BANDS = listOf(Color(0xFFE57373), Color(0xFFFFB74D), Color(0xFFFFF176), Color(0xFF81C784), Color(0xFF64B5F6))

package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.domain.model.SearchNotice
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.CatalogueSearchTestTags

@Composable
internal fun CaptionLine(listTitle: String) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val fullText = stringResource(R.string.item_search_source_caption, listTitle)
    val listNameStart = fullText.indexOf(listTitle).takeIf { listTitle.isNotEmpty() && it >= 0 }
    val annotated = buildAnnotatedString {
        append(fullText)
        listNameStart?.let { start ->
            addStyle(SpanStyle(color = primary), start, start + listTitle.length)
        }
    }
    Text(
        text = annotated,
        color = onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 0.dp).padding(bottom = 8.dp),
    )
}

/** Why the results are fewer than they could be: one quiet line above them, never an error. */
@Composable
internal fun NoticeLine(notice: SearchNotice?) {
    val text = when (notice) {
        SearchNotice.WebDailyLimit -> stringResource(R.string.item_search_notice_web_daily)
        SearchNotice.WebOff -> stringResource(R.string.item_search_notice_web_off)
        null -> return
    }
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(CatalogueSearchTestTags.ITEM_SEARCH_NOTICE),
    )
}

@Composable
internal fun CenteredMessage(
    text: String,
    body: String? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    actionTag: String = CatalogueSearchTestTags.ITEM_SEARCH_TRY_AGAIN,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        body?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clickable(onClick = onAction)
                    .testTag(actionTag)
                    .padding(8.dp),
            )
        }
    }
}

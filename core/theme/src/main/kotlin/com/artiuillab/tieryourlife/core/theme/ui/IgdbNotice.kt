package com.artiuillab.tieryourlife.core.theme.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import com.artiuillab.tieryourlife.core.theme.R

const val IGDB_NOTICE_TAG = "igdb_notice"

/** IGDB's terms ask for a visible "IGDB.com" in a place that stays; kept in English, like TMDB's notice beside it. */
@Composable
fun IgdbNotice(modifier: Modifier = Modifier) {
    val words = stringResource(R.string.igdb_attribution)
    val name = "IGDB.com"
    val start = words.indexOf(name)
    val text = buildAnnotatedString {
        if (start < 0) {
            append(words)
            return@buildAnnotatedString
        }
        append(words.substring(0, start))
        withLink(LinkAnnotation.Url(IGDB_URL, TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary)))) { append(name) }
        append(words.substring(start + name.length))
    }
    Text(
        text = text,
        modifier = modifier.testTag(IGDB_NOTICE_TAG),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.Center,
    )
}

private const val IGDB_URL = "https://www.igdb.com"

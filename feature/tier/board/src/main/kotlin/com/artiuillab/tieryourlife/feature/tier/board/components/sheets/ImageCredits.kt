package com.artiuillab.tieryourlife.feature.tier.board.components.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.board.R
import com.artiuillab.tieryourlife.feature.tier.board.TierDetailTestTags

@Composable
internal fun ImageCredits(credit: String, onDismiss: () -> Unit) {
    val text = remember(credit) { linkedCredit(credit) }
    Column(Modifier.heightIn(max = 400.dp).padding(horizontal = 24.dp, vertical = 12.dp)) {
        Text(stringResource(R.string.tier_detail_image_credits), style = MaterialTheme.typography.titleLarge)
        SelectionContainer(Modifier.weight(1f, fill = false).padding(vertical = 16.dp)) {
                Text(
                    text = text,
                    modifier = Modifier.verticalScroll(rememberScrollState()).testTag(TierDetailTestTags.IMAGE_CREDITS_TEXT),
                )
        }
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.tier_detail_credits_close)) }
    }
}

internal fun linkedCredit(credit: String) = buildAnnotatedString {
    append(credit)
    CREDIT_URL.findAll(credit).forEach { match ->
        var url = match.value.trimEnd('.', ',', ';')
        while (url.endsWith(')') && url.count { it == ')' } > url.count { it == '(' }) url = url.dropLast(1)
        addLink(LinkAnnotation.Url(url), match.range.first, match.range.first + url.length)
    }
}

private val CREDIT_URL = Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE)

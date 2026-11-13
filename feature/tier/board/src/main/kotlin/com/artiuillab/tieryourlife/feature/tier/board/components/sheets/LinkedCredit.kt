package com.artiuillab.tieryourlife.feature.tier.board.components.sheets

import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString

internal fun linkedCredit(credit: String) = buildAnnotatedString {
    append(credit)
    CREDIT_URL.findAll(credit).forEach { match ->
        var url = match.value.trimEnd('.', ',', ';')
        while (url.endsWith(')') && url.count { it == ')' } > url.count { it == '(' }) url = url.dropLast(1)
        addLink(LinkAnnotation.Url(url), match.range.first, match.range.first + url.length)
    }
}

private val CREDIT_URL = Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE)

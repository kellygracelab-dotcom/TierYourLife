package com.artiuillab.tieryourlife.feature.aistudio.domain.naming

private const val MAX_LENGTH = 40
private val WHITESPACE_REGEX = Regex("\\s+")
private val LEADING_ARTICLES = listOf("a ", "an ", "the ")
private const val TRAILING_PUNCTUATION = ".,;:!?—–-…"

fun cardTitleFromPrompt(prompt: String): String {
    val normalized = prompt.trim().replace(WHITESPACE_REGEX, " ")
    if (normalized.isEmpty()) return ""

    val withoutArticle = stripLeadingArticle(normalized)
    val truncated = truncateToWordBoundary(withoutArticle, MAX_LENGTH)
    val withoutTrailingPunctuation = truncated.trimEnd { it in TRAILING_PUNCTUATION || it.isWhitespace() }

    return withoutTrailingPunctuation.replaceFirstChar { it.uppercaseChar() }
}

private fun stripLeadingArticle(text: String): String {
    val article = LEADING_ARTICLES.firstOrNull { text.startsWith(it, ignoreCase = true) }
    return if (article != null) text.substring(article.length) else text
}

private fun truncateToWordBoundary(text: String, maxLength: Int): String {
    if (text.length <= maxLength) return text
    val prefix = text.substring(0, maxLength)
    val lastSpace = prefix.lastIndexOf(' ')
    return if (lastSpace > 0) prefix.substring(0, lastSpace) else prefix
}

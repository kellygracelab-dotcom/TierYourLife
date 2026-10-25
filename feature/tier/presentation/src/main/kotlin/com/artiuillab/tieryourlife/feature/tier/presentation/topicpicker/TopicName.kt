package com.artiuillab.tieryourlife.feature.tier.presentation.topicpicker

/** The server's own bounds, applied before asking so a refusal is never the first the author hears of them. */
const val MIN_TOPIC_NAME_LENGTH = 2
const val MAX_TOPIC_NAME_LENGTH = 40

/** A name fit to propose: whitespace collapsed, two to forty characters, at least one letter or digit. Null otherwise. */
fun cleanTopicName(raw: String): String? {
    val collapsed = raw.replace(Regex("\\s+"), " ").trim()
    if (collapsed.length !in MIN_TOPIC_NAME_LENGTH..MAX_TOPIC_NAME_LENGTH) return null
    if (collapsed.none { it.isLetterOrDigit() }) return null
    return collapsed
}

/** The first word of a title with some weight to it: what an existing topic would be looked up by. */
fun topicLookupWord(title: String): String? = title
    .split(Regex("\\s+"))
    .map { it.trim { c -> !c.isLetterOrDigit() } }
    .firstOrNull { it.length >= LOOKUP_WORD_LENGTH }

private const val LOOKUP_WORD_LENGTH = 3

package com.artiuillab.tieryourlife.core.settings

// Unit separator: a title can hold anything a person can type, but not this.
private const val SEPARATOR = '\u001F'

/**
 * How a hidden thing is written down: its id and the name to offer it back
 * by. Apart from the preferences so the awkward cases can be tested without a phone.
 */
internal object HiddenRecords {

    fun ids(records: Set<String>): Set<String> = records.mapTo(mutableSetOf()) { it.substringBefore(SEPARATOR) }

    /**
     * Records written before hiding could be undone are bare ids with no name.
     * An empty label says so; the id itself is not something anyone can act on.
     */
    fun entries(records: Set<String>): List<HiddenEntry> = records
        .map { HiddenEntry(it.substringBefore(SEPARATOR), it.substringAfter(SEPARATOR, "")) }
        .sortedBy { it.label.lowercase() }

    /** Hiding the same thing twice replaces the record rather than adding one. */
    fun with(records: Set<String>, id: String, label: String): Set<String> =
        without(records, id) + "$id$SEPARATOR$label"

    fun without(records: Set<String>, id: String): Set<String> =
        records.filterNot { it.substringBefore(SEPARATOR) == id }.toSet()
}

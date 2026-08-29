package com.artiuillab.tieryourlife.core.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HiddenRecordsTest {

    // The same character the records separate on, written as an escape rather
    // than pasted: a raw control character in source is a thing editors eat.
    private val separator = "\u001F"

    @Test
    fun `a hidden thing keeps both its id and the name to offer it back by`() {
        val records = HiddenRecords.with(emptySet(), "abc", "Sci-fi films")

        assertEquals(setOf("abc"), HiddenRecords.ids(records))
        assertEquals(listOf(HiddenEntry("abc", "Sci-fi films")), HiddenRecords.entries(records))
    }

    // Written before hiding could be undone. They still have to be filtered out
    // of the feed, and they still have to be offerable back.
    @Test
    fun `a record from before names were kept reads as an id with no name`() {
        val records = setOf("abc")

        assertEquals(setOf("abc"), HiddenRecords.ids(records))
        assertEquals(listOf(HiddenEntry("abc", "")), HiddenRecords.entries(records))
    }

    @Test
    fun `hiding the same thing twice replaces the record rather than adding one`() {
        val once = HiddenRecords.with(emptySet(), "abc", "Old title")
        val twice = HiddenRecords.with(once, "abc", "New title")

        assertEquals(1, twice.size)
        assertEquals(listOf(HiddenEntry("abc", "New title")), HiddenRecords.entries(twice))
    }

    @Test
    fun `putting one back leaves the others alone`() {
        val records = HiddenRecords.with(HiddenRecords.with(emptySet(), "a", "First"), "b", "Second")

        val left = HiddenRecords.without(records, "a")

        assertEquals(listOf(HiddenEntry("b", "Second")), HiddenRecords.entries(left))
    }

    @Test
    fun `putting back something that was never hidden changes nothing`() {
        val records = HiddenRecords.with(emptySet(), "a", "First")

        assertEquals(records, HiddenRecords.without(records, "b"))
    }

    // A title is whatever someone typed, so it cannot be trusted to be free of
    // anything -- except the one character a keyboard will not produce.
    @Test
    fun `a title carrying the separator does not swallow the rest of the record`() {
        val records = HiddenRecords.with(emptySet(), "abc", "Films" + separator + "not really")

        assertEquals(setOf("abc"), HiddenRecords.ids(records))
        assertEquals("abc", HiddenRecords.entries(records).single().id)
        assertTrue(HiddenRecords.entries(records).single().label.startsWith("Films"))
    }

    @Test
    fun `the list reads in a settled order rather than the set's own`() {
        var records = emptySet<String>()
        listOf("c" to "Zebra", "a" to "apple", "b" to "Mango").forEach { (id, label) ->
            records = HiddenRecords.with(records, id, label)
        }

        assertEquals(listOf("apple", "Mango", "Zebra"), HiddenRecords.entries(records).map { it.label })
    }

    @Test
    fun `nothing hidden reads as nothing`() {
        assertEquals(emptySet<String>(), HiddenRecords.ids(emptySet()))
        assertEquals(emptyList<HiddenEntry>(), HiddenRecords.entries(emptySet()))
    }
}

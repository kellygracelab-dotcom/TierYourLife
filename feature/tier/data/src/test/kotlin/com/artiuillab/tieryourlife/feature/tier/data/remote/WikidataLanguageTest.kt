package com.artiuillab.tieryourlife.feature.tier.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

// Every expectation here was checked against the live Wikidata API before being written
// down — the accepted and rejected forms are observed, not guessed.
class WikidataLanguageTest {

    @Test
    fun plainLanguage_isPassedThrough() {
        assertEquals("ru", wikidataLanguageCode("ru"))
        assertEquals("ja", wikidataLanguageCode("ja"))
    }

    // The one that matters most: Android hands out region-qualified tags as a matter of
    // course, and Wikidata rejects the upper-case region while accepting the lower-case one.
    @Test
    fun regionQualifiedTag_isLowerCased() {
        assertEquals("pt-br", wikidataLanguageCode("pt-BR"))
        assertEquals("en-gb", wikidataLanguageCode("en-GB"))
    }

    // sr-Latn-RS is rejected by Wikidata in any case, so the primary subtag is the only
    // thing left that can still answer.
    @Test
    fun tagWithMoreThanTwoSubtags_fallsBackToThePrimaryOne() {
        assertEquals("sr", wikidataLanguageCode("sr-Latn-RS"))
    }

    @Test
    fun missingOrUndeterminedTag_fallsBackToEnglish() {
        assertEquals("en", wikidataLanguageCode(null))
        assertEquals("en", wikidataLanguageCode(""))
        assertEquals("en", wikidataLanguageCode("   "))
        assertEquals("en", wikidataLanguageCode("und"))
    }
}

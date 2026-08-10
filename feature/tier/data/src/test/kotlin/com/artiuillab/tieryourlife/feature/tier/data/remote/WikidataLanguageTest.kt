package com.artiuillab.tieryourlife.feature.tier.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class WikidataLanguageTest {

    @Test
    fun plainLanguage_isPassedThrough() {
        assertEquals("ru", wikidataLanguageCode("ru"))
        assertEquals("ja", wikidataLanguageCode("ja"))
    }

    @Test
    fun regionQualifiedTag_isLowerCased() {
        assertEquals("pt-br", wikidataLanguageCode("pt-BR"))
        assertEquals("en-gb", wikidataLanguageCode("en-GB"))
    }

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

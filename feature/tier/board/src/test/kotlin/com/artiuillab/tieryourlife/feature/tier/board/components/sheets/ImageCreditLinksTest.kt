package com.artiuillab.tieryourlife.feature.tier.board.components.sheets

import androidx.compose.ui.text.LinkAnnotation
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageCreditLinksTest {
    @Test
    fun attributionKeepsItsTextAndEveryWholeHttpLink() {
        val credit = "Jane (https://commons.wikimedia.org/wiki/User:Jane) · CC BY 4.0 https://creativecommons.org/licenses/by/4.0/ · https://commons.wikimedia.org/?curid=12345"
        val text = linkedCredit(credit)
        assertEquals(credit, text.text)
        assertEquals(listOf("https://commons.wikimedia.org/wiki/User:Jane", "https://creativecommons.org/licenses/by/4.0/", "https://commons.wikimedia.org/?curid=12345"), text.getLinkAnnotations(0, text.length).map { (it.item as LinkAnnotation.Url).url })
    }

    @Test
    fun balancedParenthesesInsideAUrlArePreserved() {
        val text = linkedCredit("https://example.org/Album_(2026).")
        assertEquals("https://example.org/Album_(2026)", (text.getLinkAnnotations(0, text.length).single().item as LinkAnnotation.Url).url)
    }

    @Test
    fun anExecutableSchemeIsOnlyText() {
        val text = linkedCredit("Jane · javascript:alert(1)")
        assertEquals(emptyList<LinkAnnotation>(), text.getLinkAnnotations(0, text.length).map { it.item })
    }
}

package com.artiuillab.tieryourlife.feature.tier.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PictureScopeTest {

    @Test
    fun aBoardSearchesItsSectionsCatalogues_animalsTooThoughItIsNoBuiltIn() {
        assertEquals(PictureScope.Anime, PictureScope.forCategory(ListCategory.Anime.id))
        assertEquals(PictureScope.Animals, PictureScope.forCategory("animals"))
    }

    @Test
    fun aSectionWithNoCatalogue_orNone_searchesEverything() {
        assertEquals(PictureScope.All, PictureScope.forCategory(ListCategory.Food.id))
        assertEquals(PictureScope.All, PictureScope.forCategory("all"))
        assertEquals(PictureScope.All, PictureScope.forCategory(null))
    }
}

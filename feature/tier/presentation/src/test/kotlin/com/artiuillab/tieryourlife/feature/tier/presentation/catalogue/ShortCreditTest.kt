package com.artiuillab.tieryourlife.feature.tier.presentation.catalogue

import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import com.artiuillab.tieryourlife.feature.tier.presentation.catalogue.components.shortCredit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** A result's credit under it, short, as the site has it (artboard 23). */
class ShortCreditTest {

    @Test
    fun namesTheAuthor_andTheLicenceWithoutItsVersion() {
        assertEquals(
            "Rosino, CC BY-SA",
            item("Rosino, CC BY-SA 2.0, via Wikimedia Commons", PictureAttribution("commons", author = "Rosino", license = "CC BY-SA 2.0")).shortCredit(),
        )
        assertEquals("Ann, CC0", item(null, PictureAttribution("inaturalist", author = "Ann", license = "CC0 1.0")).shortCredit())
        assertEquals("Bo, Public domain", item(null, PictureAttribution("commons", author = "Bo", license = "Public domain")).shortCredit())
        assertEquals("Cy", item(null, PictureAttribution("commons", author = " Cy ")).shortCredit())
    }

    @Test
    fun keepsAPlainCredit_andShowsNoBareAddressAsAName() {
        assertEquals("Photo: Ann", item("Photo: Ann", null).shortCredit())
        assertNull(item("https://example.org/pic", null).shortCredit())
        assertNull(item("//example.org/pic", null).shortCredit())
        assertNull(item(null, PictureAttribution("tmdb")).shortCredit())
        assertNull(item("  ", null).shortCredit())
    }

    private fun item(credit: String?, attribution: PictureAttribution?) =
        CatalogueItem(id = "1", title = "Card", subtitle = null, imageUrl = null, credit = credit, attribution = attribution)
}

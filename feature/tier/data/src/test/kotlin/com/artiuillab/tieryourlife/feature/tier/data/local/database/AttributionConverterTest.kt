package com.artiuillab.tieryourlife.feature.tier.data.local.database

import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AttributionConverterTest {

    private val converter = AttributionConverter()

    @Test
    fun anAttribution_readsBackAsItWasStored() {
        val fox = PictureAttribution("inaturalist", "Jane Doe", "CC BY 4.0", "https://l", "https://p", "https://o")

        assertEquals(fox, converter.fromStored(converter.toStored(fox)))
        assertNull(converter.toStored(null))
    }

    @Test
    fun aRowThatDoesNotRead_isACardWithoutOne() {
        assertNull(converter.fromStored("{not json"))
        assertNull(converter.fromStored(null))
        assertEquals(PictureAttribution("tmdb"), converter.fromStored("""{"source":"tmdb","later":true}"""))
    }
}

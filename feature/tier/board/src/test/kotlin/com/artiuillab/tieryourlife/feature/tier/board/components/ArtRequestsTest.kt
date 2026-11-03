package com.artiuillab.tieryourlife.feature.tier.board.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtRequestsTest {

    @Test
    fun a_cover_is_asked_for_at_the_whole_tile() {
        val requests = artRequests(ArtPictures("cover.jpg", listOf("a.jpg")), width = 400, height = 300)

        assertEquals(listOf(ArtRequest("cover.jpg", 400, 300)), requests)
    }

    @Test
    fun a_mosaic_shares_the_tile_out_the_way_it_is_drawn_and_never_smaller() {
        val requests = artRequests(ArtPictures(null, listOf("a", "b", "a", "c", "d", "e")), width = 400, height = 301)

        // Five distinct pictures: three on top, two under, each row half the height rounded up.
        assertEquals(
            listOf(
                ArtRequest("a", 134, 151),
                ArtRequest("b", 134, 151),
                ArtRequest("c", 134, 151),
                ArtRequest("d", 200, 151),
                ArtRequest("e", 200, 151),
            ),
            requests,
        )
    }

    @Test
    fun a_tile_drawn_in_its_colours_asks_for_nothing() {
        assertEquals(emptyList<ArtRequest>(), artRequests(ArtPictures(null, emptyList()), width = 400, height = 300))
    }
}

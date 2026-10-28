package com.artiuillab.tieryourlife.feature.tier.data.mapper

import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierItemEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.TierListEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.relation.TierListWithTiers
import com.artiuillab.tieryourlife.feature.tier.data.local.relation.TierWithItems
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import org.junit.Assert.assertEquals
import org.junit.Test

class TierListMapperTest {

    @Test
    fun tier_list_relation_maps_to_domain_with_nested_lists_ordered_by_position() {
        val tierList = TierListEntity(
            id = 1,
            title = "Films",
        )

        val tierS = TierEntity(
            id = 1,
            tierListId = tierList.id,
            position = 1,
            label = "S",
            colorLight = "#B03A32",
            colorDark = "#F1948C",
        )
        val tierA = TierEntity(
            id = 2,
            tierListId = tierList.id,
            position = 2,
            label = "A",
            colorLight = "#C06A25",
            colorDark = "#E9A867",
        )

        val firstItem = TierItemEntity(
            id = 1,
            tierId = tierS.id,
            position = 1,
            title = "First",
            imageUrl = "https://example.com/first.jpg",
        )
        val secondItem = TierItemEntity(
            id = 2,
            tierId = tierS.id,
            position = 2,
            title = "Second",
            imageUrl = "https://example.com/second.jpg",
        )

        val source = TierListWithTiers(
            tierList = tierList,
            tiers = listOf(
                TierWithItems(
                    tier = tierA,
                    items = emptyList(),
                ),
                TierWithItems(
                    tier = tierS,
                    items = listOf(secondItem, firstItem),
                ),
            ),
        )

        val actual = source.toDomain()

        assertEquals(1L, actual.id)
        assertEquals("Films", actual.title)
        assertEquals(listOf("S", "A"), actual.tiers.map { it.label })
        assertEquals(
            listOf("First", "Second"),
            actual.tiers.first().items.map { it.title },
        )
    }

    // Play names cards and tiers by uid in the game the account keeps, and reads that game off the board.
    @Test
    fun a_boards_uids_and_its_saved_game_reach_the_domain() {
        val game = """{"kind":"hill","count":2,"seed":1,"refine":false,"ids":["i-1","i-2"],"history":[],"round":0,"startedAt":5}"""
        val tier = TierEntity(id = 3, tierListId = 1, position = 0, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", uid = "t-s")
        val item = TierItemEntity(id = 4, tierId = 3, position = 0, title = "Dune", imageUrl = null, uid = "i-1")

        val board = TierListWithTiers(
            tierList = TierListEntity(id = 1, title = "Films", play = game),
            tiers = listOf(TierWithItems(tier = tier, items = listOf(item))),
        ).toDomain()

        assertEquals(game, board.play)
        assertEquals("t-s", board.tiers.single().uid)
        assertEquals("i-1", board.tiers.single().items.single().uid)
        assertEquals(game, TierListEntity(id = 1, title = "Films", play = game).toDomain().play)
    }

    @Test
    fun tier_list_entity_with_known_display_mode_maps_to_the_matching_enum_constant() {
        val entity = TierListEntity(id = 1, title = "Films", displayMode = "HORIZONTAL_SCROLL")

        val actual = entity.toDomain()

        assertEquals(TierListDisplayMode.HORIZONTAL_SCROLL, actual.displayMode)
    }

    @Test
    fun tier_list_entity_with_unrecognized_display_mode_falls_back_to_wrap() {
        val entity = TierListEntity(id = 1, title = "Films", displayMode = "SOME_FUTURE_MODE")

        val actual = entity.toDomain()

        assertEquals(TierListDisplayMode.WRAP, actual.displayMode)
    }

    @Test
    fun tier_list_with_tiers_relation_carries_display_mode_from_the_embedded_list() {
        val entity = TierListEntity(id = 1, title = "Films", displayMode = "FLAT_RANKED")
        val source = TierListWithTiers(tierList = entity, tiers = emptyList())

        val actual = source.toDomain()

        assertEquals(TierListDisplayMode.FLAT_RANKED, actual.displayMode)
    }

    @Test
    fun a_collage_comes_back_as_the_same_pictures_in_the_same_order() {
        val pictures = listOf("https://example.com/a.jpg", "https://example.com/b.jpg", "https://example.com/c.jpg")
        val entity = TierListEntity(id = 1, title = "Films", coverImageUrls = pictures.toStoredCoverImageUrls())

        assertEquals(pictures, entity.toDomain().coverImageUrls)
    }

    @Test
    fun a_section_this_phone_has_not_heard_of_is_kept_as_it_was_stored() {
        val entity = TierListEntity(id = 1, title = "Ramen", category = "street-food")

        assertEquals("street-food", entity.toDomain().category)
        assertEquals("street-food", TierListWithTiers(tierList = entity, tiers = emptyList()).toDomain().category)
    }

    @Test
    fun one_picture_is_a_cover_and_not_a_collage() {
        assertEquals(null, listOf("https://example.com/a.jpg").toStoredCoverImageUrls())
        assertEquals(emptyList<String>(), TierListEntity(id = 1, title = "Films").toDomain().coverImageUrls)
    }
}

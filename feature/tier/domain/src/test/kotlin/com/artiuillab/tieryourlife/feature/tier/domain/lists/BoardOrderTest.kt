package com.artiuillab.tieryourlife.feature.tier.domain.lists

import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardOrderTest {

    private fun board(
        id: Long,
        editedAt: Long? = id,
        favouritedAt: Long? = null,
        category: ListCategory? = null,
        publishedId: String? = null,
    ) = TierList(
        id = id,
        title = "Board $id",
        tiers = emptyList(),
        editedAt = editedAt,
        favouritedAt = favouritedAt,
        category = category,
        publishedId = publishedId,
    )

    private fun ids(lists: List<TierList>) = lists.map { it.id }

    @Test
    fun `newest first means most recently touched first`() {
        val arranged = BoardOrder.arrange(
            listOf(board(1, editedAt = 100), board(2, editedAt = 300), board(3, editedAt = 200)),
            BoardSort.Newest,
        )

        assertEquals(listOf(2L, 3L, 1L), ids(arranged.rest))
    }

    @Test
    fun `oldest first is the same order upside down`() {
        val arranged = BoardOrder.arrange(
            listOf(board(1, editedAt = 100), board(2, editedAt = 300), board(3, editedAt = 200)),
            BoardSort.Oldest,
        )

        assertEquals(listOf(1L, 3L, 2L), ids(arranged.rest))
    }

    // A board made before the app started recording the time still has to sit
    // somewhere, and the id is the order they were made in.
    @Test
    fun `a board with no recorded time falls back on the order it was made`() {
        val arranged = BoardOrder.arrange(
            listOf(board(1, editedAt = null), board(2, editedAt = null), board(3, editedAt = 50)),
            BoardSort.Newest,
        )

        assertEquals(listOf(3L, 2L, 1L), ids(arranged.rest))
    }

    @Test
    fun `starred boards come first, most recently starred at the top`() {
        val arranged = BoardOrder.arrange(
            listOf(
                board(1, editedAt = 900),
                board(2, editedAt = 100, favouritedAt = 10),
                board(3, editedAt = 200, favouritedAt = 20),
            ),
            BoardSort.Newest,
        )

        assertEquals(listOf(3L, 2L), ids(arranged.favourites))
        assertEquals(listOf(1L), ids(arranged.rest))
    }

    // Sorting is about the boards nobody pinned. If the sort could reorder the
    // pinned ones, pinning would not be pinning.
    @Test
    fun `the sort does not disturb what was pinned`() {
        val lists = listOf(board(1, editedAt = 100, favouritedAt = 10), board(2, editedAt = 200, favouritedAt = 20))

        val newest = BoardOrder.arrange(lists, BoardSort.Newest).favourites
        val oldest = BoardOrder.arrange(lists, BoardSort.Oldest).favourites

        assertEquals(ids(newest), ids(oldest))
    }

    @Test
    fun `a category filter keeps only that category`() {
        val arranged = BoardOrder.arrange(
            listOf(
                board(1, category = ListCategory.Games),
                board(2, category = ListCategory.Food),
                board(3, category = null),
            ),
            BoardSort.Newest,
            BoardFilters(category = ListCategory.Food),
        )

        assertEquals(listOf(2L), ids(arranged.all))
    }

    @Test
    fun `published and private are the two halves of the same question`() {
        val lists = listOf(board(1, publishedId = "a"), board(2, publishedId = null))

        assertEquals(
            listOf(1L),
            ids(BoardOrder.arrange(lists, BoardSort.Newest, BoardFilters(published = PublishedFilter.Public)).all),
        )
        assertEquals(
            listOf(2L),
            ids(BoardOrder.arrange(lists, BoardSort.Newest, BoardFilters(published = PublishedFilter.Private)).all),
        )
    }

    @Test
    fun `two filters both have to be satisfied`() {
        val arranged = BoardOrder.arrange(
            listOf(
                board(1, category = ListCategory.Food, publishedId = "a"),
                board(2, category = ListCategory.Food, publishedId = null),
                board(3, category = ListCategory.Games, publishedId = "b"),
            ),
            BoardSort.Newest,
            BoardFilters(category = ListCategory.Food, published = PublishedFilter.Public),
        )

        assertEquals(listOf(1L), ids(arranged.all))
    }

    // A starred board is still a board: a filter it does not match hides it
    // like any other, or "Public" would quietly show private boards.
    @Test
    fun `a filter applies to starred boards too`() {
        val arranged = BoardOrder.arrange(
            listOf(board(1, favouritedAt = 10, publishedId = null), board(2, publishedId = "a")),
            BoardSort.Newest,
            BoardFilters(published = PublishedFilter.Public),
        )

        assertEquals(emptyList<Long>(), ids(arranged.favourites))
        assertEquals(listOf(2L), ids(arranged.rest))
    }

    // Without headings the top two cards read as the two newest, and the sort
    // control directly above them says exactly that.
    @Test
    fun `the groups get headings only when there are two of them`() {
        val both = BoardOrder.arrange(listOf(board(1, favouritedAt = 5), board(2)), BoardSort.Newest)
        val onlyStarred = BoardOrder.arrange(listOf(board(1, favouritedAt = 5)), BoardSort.Newest)
        val noneStarred = BoardOrder.arrange(listOf(board(1)), BoardSort.Newest)

        assertTrue(BoardOrder.shouldGroup(both, narrowed = false))
        assertFalse(BoardOrder.shouldGroup(onlyStarred, narrowed = false))
        assertFalse(BoardOrder.shouldGroup(noneStarred, narrowed = false))
    }

    // A filtered or searched screen is an answer to a question. Pinning
    // something above the answer is a second question nobody asked.
    @Test
    fun `narrowing the screen turns the headings off`() {
        val both = BoardOrder.arrange(listOf(board(1, favouritedAt = 5), board(2)), BoardSort.Newest)

        assertFalse(BoardOrder.shouldGroup(both, narrowed = true))
    }

    @Test
    fun `filters know whether any of them is on`() {
        assertFalse(BoardFilters().any)
        assertTrue(BoardFilters(category = ListCategory.Food).any)
        assertTrue(BoardFilters(published = PublishedFilter.Private).any)
    }
}

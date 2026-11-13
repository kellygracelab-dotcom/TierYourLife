package com.artiuillab.tieryourlife.feature.tier.presentation.share

import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.tier.domain.RecordingTierRepository
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureAttribution
import com.artiuillab.tieryourlife.feature.tier.domain.model.PoolItemDraft
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListSummary
import com.artiuillab.tieryourlife.feature.tier.domain.repository.LinkPicture
import com.artiuillab.tieryourlife.feature.tier.domain.repository.PictureLinks
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AddToListViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val totoro = LinkPicture.Found("/files/p_1", PictureAttribution("web"), null)

    @Test
    fun theListsComeNewestFirst_andTheNewestIsChosen() = runBlocking {
        val viewModel = AddToListViewModel(ShareRepository(), FakeLinks(totoro))

        withContext(Dispatchers.Main) { viewModel.start(Shared(text = "https://i.example.net/a.jpg")) }
        val shown = viewModel.state.firstWithin { it.lists.isNotEmpty() && it.picture != null }

        assertEquals(listOf(2L, 1L), shown.lists.map { it.id })
        assertEquals(2L, shown.chosen)
        assertEquals(totoro, shown.picture)
    }

    @Test
    fun aSharedAddress_isFetched_andTheCardGoesIntoTheChosenPool() = runBlocking {
        val repository = ShareRepository()
        val links = FakeLinks(totoro)
        val viewModel = AddToListViewModel(repository, links)
        withContext(Dispatchers.Main) { viewModel.start(Shared(text = "Totoro https://i.example.net/a.jpg", subject = "Totoro")) }
        viewModel.state.firstWithin { it.picture != null && it.lists.isNotEmpty() }
        withContext(Dispatchers.Main) { viewModel.choose(1L) }

        val added = CompletableDeferred<String>()
        withContext(Dispatchers.Main) { viewModel.add("Totoro", "Untitled list", added::complete) }

        assertEquals("Studio Ghibli, ranked", withTimeout(5_000) { added.await() })
        assertEquals(listOf("https://i.example.net/a.jpg"), links.asked)
        assertEquals(listOf(1L to PoolItemDraft("Totoro", "/files/p_1", attribution = PictureAttribution("web"))), repository.added)
    }

    @Test
    fun aSharedFile_isKeptOnThePhone_andANewListIsMadeForIt() = runBlocking {
        val repository = ShareRepository()
        val viewModel = AddToListViewModel(repository, FakeLinks(totoro))
        withContext(Dispatchers.Main) { viewModel.start(Shared(imageUri = "content://browser/1")) }
        assertEquals(LinkPicture.Found("/files/kept-content://browser/1", null, null), viewModel.state.firstWithin { it.picture != null }.picture)
        withContext(Dispatchers.Main) { viewModel.choose(null) }

        val added = CompletableDeferred<String>()
        withContext(Dispatchers.Main) { viewModel.add("", "Posters", added::complete) }

        assertEquals("Posters", withTimeout(5_000) { added.await() })
        assertEquals(listOf("Posters"), repository.created)
        assertEquals(listOf(99L to PoolItemDraft("", "/files/kept-content://browser/1")), repository.added)
    }

    @Test
    fun textWithoutAnAddress_isACardWithoutAPicture() = runBlocking {
        val links = FakeLinks(totoro)
        val viewModel = AddToListViewModel(ShareRepository(), links)

        withContext(Dispatchers.Main) { viewModel.start(Shared(text = "Spirited Away")) }
        val shown = viewModel.state.firstWithin { !it.hasPicture }

        assertNull(shown.picture)
        assertFalse(shown.hasPicture)
        assertEquals(emptyList<String>(), links.asked)
    }

    @Test
    fun whatABrowserShares_isReadForItsAddressAndHost() {
        assertEquals("https://www.ghibli.jp/works/totoro/", Shared(text = "Totoro https://www.ghibli.jp/works/totoro/").link)
        assertEquals("ghibli.jp", Shared(text = "https://www.ghibli.jp/works/totoro/").host)
        assertNull(Shared(imageUri = "content://x", text = "https://www.ghibli.jp/").link)
    }
}

private class ShareRepository(private val base: RecordingTierRepository = RecordingTierRepository()) : TierRepository by base {
    val added = mutableListOf<Pair<Long, PoolItemDraft>>()
    val created = mutableListOf<String>()

    override suspend fun getTierListSummaries(): List<TierListSummary> = listOf(
        TierListSummary(1, "Studio Ghibli, ranked", tiers = emptyList(), editedAt = 100),
        TierListSummary(2, "Мультфильмы детства", tiers = emptyList(), editedAt = 200),
    )

    override suspend fun createTierList(title: String, captions: List<String>): Long {
        created += title
        return 99L
    }

    override suspend fun addItemsToPool(tierListId: Long, items: List<PoolItemDraft>) {
        items.forEach { added += tierListId to it }
    }

    override suspend fun keepPicture(sourceUri: String): String = "/files/kept-$sourceUri"
}

private class FakeLinks(private val answer: LinkPicture) : PictureLinks {
    val asked = mutableListOf<String>()

    override suspend fun fetch(url: String): LinkPicture {
        asked += url
        return answer
    }
}

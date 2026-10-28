package com.artiuillab.tieryourlife.feature.community.domain.repository

import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityClock
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.FakeSectionStore
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionDirectory
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionLookup
import com.artiuillab.tieryourlife.feature.community.domain.sectionCatalogFor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class KeptSectionCatalogTest {

    private val clock = FakeCommunityClock(now = 1_000_000L)
    private val cars = Section(id = "cars", name = "Cars", lists30d = 9)

    private open class Community(
        var answer: Result<List<Section>> = Result.success(Section.BUILT_IN),
        val lookups: Map<String, Result<SectionLookup>> = emptyMap(),
        val followFails: Boolean = false,
    ) : FakeCommunityRepository() {
        var reads = 0
        val asked = mutableListOf<String>()
        val followed = mutableListOf<Pair<String, Boolean>>()

        override suspend fun sections(): Result<List<Section>> {
            reads++
            return answer
        }

        override suspend fun section(id: String): Result<SectionLookup> {
            asked += id
            return lookups[id] ?: Result.success(SectionLookup.Unknown)
        }

        override suspend fun followSection(id: String): Result<Unit> = follow(id, true)

        override suspend fun unfollowSection(id: String): Result<Unit> = follow(id, false)

        private fun follow(id: String, now: Boolean): Result<Unit> {
            followed += id to now
            return if (followFails) Result.failure(IOException("offline")) else Result.success(Unit)
        }
    }

    @Test
    fun withNothingKept_theNineAreDrawn_untilTheServerAnswers() = runBlocking {
        val community = Community(answer = Result.success(Section.BUILT_IN + cars))
        val catalog = sectionCatalogFor(community, clock = clock)

        assertEquals(Section.BUILT_IN, catalog.directory.value.sections)
        catalog.refresh()

        assertEquals(Section.BUILT_IN + cars, catalog.directory.value.sections)
        assertEquals(clock.now, catalog.directory.value.fetchedAtMillis)
    }

    @Test
    fun whatWasKept_isWhatOpens_andAFreshCopyIsNotAskedForAgain() = runBlocking {
        val kept = SectionDirectory(sections = listOf(cars), fetchedAtMillis = clock.now - 60_000L)
        val community = Community()
        val catalog = sectionCatalogFor(community, FakeSectionStore(kept), clock)

        catalog.refresh()

        assertEquals(listOf(cars), catalog.directory.value.sections)
        assertEquals(0, community.reads)
    }

    @Test
    fun aCopyOlderThanTenMinutes_isReadAgain_andKept() = runBlocking {
        val store = FakeSectionStore(SectionDirectory(sections = listOf(cars), fetchedAtMillis = clock.now - 11 * 60_000L))
        val community = Community()
        val catalog = sectionCatalogFor(community, store, clock)

        catalog.refresh()

        assertEquals(1, community.reads)
        assertEquals(Section.BUILT_IN, store.kept?.sections)
    }

    @Test
    fun aForcedRefresh_asksEvenWhenFresh() = runBlocking {
        val community = Community()
        val catalog = sectionCatalogFor(community, FakeSectionStore(SectionDirectory(fetchedAtMillis = clock.now)), clock)

        catalog.refresh(force = true)

        assertEquals(1, community.reads)
    }

    @Test
    fun aFailedRead_leavesWhatWasThere() = runBlocking {
        val kept = SectionDirectory(sections = listOf(cars), fetchedAtMillis = 1L)
        val catalog = sectionCatalogFor(Community(answer = Result.failure(IOException("offline"))), FakeSectionStore(kept), clock)

        assertTrue(catalog.refresh().isFailure)
        assertEquals(kept, catalog.directory.value)
    }

    @Test
    fun anIdNobodyKnows_isAskedAboutOnce_andDrawnOnceFound() = runBlocking {
        val community = Community(lookups = mapOf("cars" to Result.success(SectionLookup.Found(cars))))
        val catalog = sectionCatalogFor(community, clock = clock)

        catalog.lookUp("cars")
        catalog.lookUp("cars")

        assertEquals(listOf("cars"), community.asked)
        assertEquals(cars, catalog.directory.value.find("cars"))
    }

    @Test
    fun aMergedId_isRemembered_andWhatItBecameIsAskedAboutToo() = runBlocking {
        val community = Community(
            lookups = mapOf(
                "autos" to Result.success(SectionLookup.Merged("cars")),
                "cars" to Result.success(SectionLookup.Found(cars)),
            ),
        )
        val store = FakeSectionStore()
        val catalog = sectionCatalogFor(community, store, clock)

        catalog.lookUp("autos")

        assertEquals(listOf("autos", "cars"), community.asked)
        assertEquals(cars, catalog.directory.value.find("autos"))
        assertEquals(mapOf("autos" to "cars"), catalog.directory.value.merged)
    }

    @Test
    fun anIdTheServerHasNeverHeardOf_isNotAskedAboutAgain_untilTheNextAnswer() = runBlocking {
        val community = Community()
        val catalog = sectionCatalogFor(community, clock = clock)

        catalog.lookUp("ghost")
        catalog.lookUp("ghost")
        assertEquals(listOf("ghost"), community.asked)

        catalog.refresh(force = true)
        catalog.lookUp("ghost")
        assertEquals(listOf("ghost", "ghost"), community.asked)
    }

    @Test
    fun aLookupThatFails_mayBeTriedAgain() = runBlocking {
        val community = Community(lookups = mapOf("cars" to Result.failure(IOException("offline"))))
        val catalog = sectionCatalogFor(community, clock = clock)

        catalog.lookUp("cars")
        catalog.lookUp("cars")

        assertEquals(listOf("cars", "cars"), community.asked)
        assertNull(catalog.directory.value.find("cars"))
    }

    @Test
    fun aKnownId_isNeverAskedAbout() = runBlocking {
        val community = Community()
        val catalog = sectionCatalogFor(community, clock = clock)

        catalog.lookUp("food")

        assertTrue(community.asked.isEmpty())
    }

    @Test
    fun following_redrawsAtOnce_andIsKept() = runBlocking {
        val community = Community()
        val store = FakeSectionStore(SectionDirectory(sections = listOf(cars, Section(id = "boats")), fetchedAtMillis = clock.now))
        val catalog = sectionCatalogFor(community, store, clock)

        assertTrue(catalog.setFollowing("boats", now = true).isSuccess)

        assertEquals(listOf("boats", "cars"), catalog.directory.value.sections.map { it.id })
        assertEquals(listOf("boats" to true), community.followed)
        assertTrue(store.kept?.find("boats")?.following == true)
    }

    @Test
    fun aRefusedFollow_isPutBack() = runBlocking {
        val catalog = sectionCatalogFor(Community(followFails = true), FakeSectionStore(SectionDirectory(sections = listOf(cars), fetchedAtMillis = 1L)), clock)

        assertTrue(catalog.setFollowing("cars", now = true).isFailure)

        val after = catalog.directory.value.find("cars")
        assertFalse(after?.following == true)
        assertEquals(0, after?.followerCount)
    }

    @Test
    fun aFollowOfAMergedId_goesToTheSectionItLivesOnAs() = runBlocking {
        val community = Community()
        val kept = SectionDirectory(sections = listOf(cars), merged = mapOf("autos" to "cars"), fetchedAtMillis = 1L)
        val catalog = sectionCatalogFor(community, FakeSectionStore(kept), clock)

        catalog.setFollowing("autos", now = true)

        assertEquals(listOf("cars" to true), community.followed)
    }

    @Test
    fun aRefresh_waitsForWhatWasKept_andTheAnswerIsWhatStays() = runBlocking {
        val opened = CompletableDeferred<Unit>()
        val slowStore = object : SectionStore {
            override suspend fun read(): SectionDirectory? {
                opened.await()
                return SectionDirectory(sections = listOf(cars), fetchedAtMillis = 1L)
            }

            override suspend fun write(directory: SectionDirectory) = Unit
        }
        val catalog = sectionCatalogFor(Community(), slowStore, clock)

        val refreshing = launch { catalog.refresh(force = true) }
        yield()
        assertEquals(Section.BUILT_IN, catalog.directory.value.sections)
        opened.complete(Unit)
        refreshing.join()

        assertEquals(Section.BUILT_IN, catalog.directory.value.sections)
        assertEquals(clock.now, catalog.directory.value.fetchedAtMillis)
    }
}

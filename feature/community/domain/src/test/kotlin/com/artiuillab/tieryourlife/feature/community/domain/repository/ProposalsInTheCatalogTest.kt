package com.artiuillab.tieryourlife.feature.community.domain.repository

import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.FakeSectionStore
import com.artiuillab.tieryourlife.feature.community.domain.model.ProposalError
import com.artiuillab.tieryourlife.feature.community.domain.model.ProposalRefused
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionDirectory
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionProposal
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionStatus
import com.artiuillab.tieryourlife.feature.community.domain.model.Withdrawal
import com.artiuillab.tieryourlife.feature.community.domain.sectionCatalogFor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProposalsInTheCatalogTest {

    private val proposal = SectionProposal(name = "Street food", icon = "local_cafe", pictureId = "p-1", fileUnder = "food")
    private val sent = Section(id = "street-food", name = "Street food", status = SectionStatus.Pending, fileUnder = "food")

    private class Community(val answer: Result<Section>, val withdrawal: Withdrawal = Withdrawal.Withdrawn, val onTheServer: List<Section> = Section.BUILT_IN) :
        FakeCommunityRepository() {
        val proposed = mutableListOf<SectionProposal>()
        val withdrawn = mutableListOf<String>()
        var reads = 0

        override suspend fun proposeSection(proposal: SectionProposal): Result<Section> {
            proposed += proposal
            return answer
        }

        override suspend fun withdrawProposal(id: String): Result<Withdrawal> {
            withdrawn += id
            return Result.success(withdrawal)
        }

        override suspend fun sections(): Result<List<Section>> {
            reads++
            return Result.success(onTheServer)
        }
    }

    @Test
    fun aProposalTheServerTook_waitsFirstOnThisPersonsScreens_andIsKept() = runBlocking {
        val store = FakeSectionStore(SectionDirectory(fetchedAtMillis = 1L))
        val catalog = sectionCatalogFor(Community(Result.success(sent)), store)

        assertTrue(catalog.propose(proposal).isSuccess)

        assertEquals(listOf("street-food"), catalog.directory.value.waiting.map { it.id })
        assertTrue(catalog.directory.value.shown.none { it.id == "street-food" })
        assertEquals(listOf("street-food"), store.kept?.waiting?.map { it.id })
    }

    @Test
    fun aRefusedProposal_leavesNothingWaiting() = runBlocking {
        val catalog = sectionCatalogFor(Community(Result.failure(ProposalRefused(ProposalError.TooManyPending))))

        val result = catalog.propose(proposal)

        assertEquals(ProposalError.TooManyPending, (result.exceptionOrNull() as ProposalRefused).error)
        assertTrue(catalog.directory.value.waiting.isEmpty())
    }

    @Test
    fun withdrawing_takesTheProposalAway() = runBlocking {
        val community = Community(Result.success(sent))
        val kept = SectionDirectory(sections = Section.BUILT_IN + sent.copy(mine = true), fetchedAtMillis = 1L)
        val catalog = sectionCatalogFor(community, FakeSectionStore(kept))

        assertNull(catalog.withdraw("street-food").getOrThrow())

        assertEquals(listOf("street-food"), community.withdrawn)
        assertTrue(catalog.directory.value.waiting.isEmpty())
        assertEquals(0, community.reads)
    }

    @Test
    fun aProposalApprovedMeanwhile_saysSo_isASectionNow_andTheSectionsAreReadAgain() = runBlocking {
        val approved = sent.copy(status = SectionStatus.Approved)
        val community = Community(Result.success(sent), Withdrawal.Settled(SectionStatus.Approved), onTheServer = Section.BUILT_IN + approved)
        val kept = SectionDirectory(sections = Section.BUILT_IN + sent.copy(mine = true), fetchedAtMillis = 1L)
        val catalog = sectionCatalogFor(community, FakeSectionStore(kept))

        assertEquals(SectionStatus.Approved, catalog.withdraw("street-food").getOrThrow())

        assertTrue(catalog.directory.value.waiting.isEmpty())
        assertTrue(catalog.directory.value.shown.any { it.id == "street-food" })
        assertEquals(1, community.reads)
    }

    @Test
    fun aWaitingProposalDeclinedMeanwhile_saysSo_andIsGone() = runBlocking {
        val community = Community(Result.success(sent), Withdrawal.Dismissed(SectionStatus.Merged))
        val kept = SectionDirectory(sections = Section.BUILT_IN + sent.copy(mine = true), fetchedAtMillis = 1L)
        val catalog = sectionCatalogFor(community, FakeSectionStore(kept))

        assertEquals(SectionStatus.Merged, catalog.withdraw("street-food").getOrThrow())

        assertTrue(catalog.directory.value.waiting.isEmpty())
        assertTrue(catalog.directory.value.settledAgainst.isEmpty())
        assertEquals(1, community.reads)
    }

    @Test
    fun aDeclinedOrMergedProposal_isANotice_untilDismissed() = runBlocking {
        val declined = Section(id = "kites", name = "Kites", status = SectionStatus.Declined, mine = true, fileUnder = "sport", declineNote = "Too close to Sport")
        val merged = Section(id = "noodles", name = "Noodles", status = SectionStatus.Merged, mine = true, mergedInto = "food")
        val someoneElses = Section(id = "boats", status = SectionStatus.Pending)
        val community = Community(Result.success(sent), Withdrawal.Dismissed(SectionStatus.Declined))
        val catalog = sectionCatalogFor(community, FakeSectionStore(SectionDirectory(sections = listOf(declined, merged, someoneElses), fetchedAtMillis = 1L)))

        assertEquals(listOf("kites", "noodles"), catalog.directory.value.settledAgainst.map { it.id })
        assertTrue(catalog.directory.value.waiting.isEmpty())

        assertNull("putting a notice away says nothing more", catalog.withdraw("kites").getOrThrow())

        assertEquals(listOf("noodles"), catalog.directory.value.settledAgainst.map { it.id })
        assertEquals(0, community.reads)
    }
}

package com.artiuillab.tieryourlife.feature.community.presentation.propose

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.FakeSectionPictures
import com.artiuillab.tieryourlife.feature.community.domain.FakeSectionStore
import com.artiuillab.tieryourlife.feature.community.domain.model.PictureCrop
import com.artiuillab.tieryourlife.feature.community.domain.model.PictureProblem
import com.artiuillab.tieryourlife.feature.community.domain.model.ProposalError
import com.artiuillab.tieryourlife.feature.community.domain.model.ProposalRefused
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionDirectory
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionNameCheck
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionProposal
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionStatus
import com.artiuillab.tieryourlife.feature.community.domain.repository.PictureSize
import com.artiuillab.tieryourlife.feature.community.domain.sectionCatalogFor
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ProposeSectionViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private class Community(
        val nameAnswer: Result<SectionNameCheck> = Result.success(SectionNameCheck.Free),
        val proposeAnswer: (SectionProposal) -> Result<Section> = { Result.success(pending(it)) },
    ) : FakeCommunityRepository() {
        val checked = mutableListOf<String>()
        val proposed = mutableListOf<SectionProposal>()

        override suspend fun checkSectionName(name: String): Result<SectionNameCheck> {
            synchronized(checked) { checked += name }
            return nameAnswer
        }

        override suspend fun proposeSection(proposal: SectionProposal): Result<Section> {
            proposed += proposal
            return proposeAnswer(proposal)
        }
    }

    private val signedIn = Account.SignedIn(email = "ann@example.com", photoUrl = null)
    private val made = mutableListOf<ProposeSectionViewModel>()

    @After
    fun clear() {
        made.forEach { it.viewModelScope.cancel() }
    }

    private fun viewModel(
        community: Community = Community(),
        account: Account = signedIn,
        pictures: FakeSectionPictures = FakeSectionPictures(size = Result.success(PictureSize(2000, 1250))),
        store: FakeSectionStore = FakeSectionStore(),
        fileUnder: String? = null,
    ) = ProposeSectionViewModel(
        savedStateHandle = SavedStateHandle(fileUnder?.let { mapOf("fileUnder" to it) } ?: emptyMap()),
        accounts = FakeAccountRepository(account),
        community = community,
        catalog = sectionCatalogFor(community, store),
        pictures = pictures,
    ).also { made += it }

    /** Name free, picture in: ready to send. */
    private suspend fun ready(viewModel: ProposeSectionViewModel): ProposeSectionUiState {
        viewModel.updateName("Street food")
        viewModel.pickPicture("content://picture")
        return viewModel.state.firstWithin { it.canPropose && it.nameStanding == NameStanding.Free }
    }

    @Test
    fun aGuest_isAskedToSignInFirst_andSomebodySignedIn_seesTheForm() = runBlocking {
        assertEquals(false, viewModel(account = Account.Guest).state.firstWithin { it.signedIn != null }.signedIn)
        assertEquals(true, viewModel(account = signedIn).state.firstWithin { it.signedIn != null }.signedIn)
    }

    @Test
    fun theName_isAskedAboutOnce_whenTheTypingStops() = runBlocking {
        val community = Community()
        val viewModel = viewModel(community)

        viewModel.updateName("St")
        viewModel.updateName("Stre")
        viewModel.updateName("Street food ")

        assertEquals(NameStanding.Free, viewModel.state.firstWithin { it.nameStanding == NameStanding.Free }.nameStanding)
        assertEquals(listOf("Street food"), synchronized(community.checked) { community.checked.toList() })
    }

    @Test
    fun aNameTooShort_isNotAskedAbout_andALongOneIsCutAt24() = runBlocking {
        val community = Community()
        val viewModel = viewModel(community)

        viewModel.updateName("S")
        delay(600)
        viewModel.updateName("A name that goes on far too long")

        assertEquals("A name that goes on far ", viewModel.state.firstWithin { it.name.length > 1 }.name)
        assertTrue(synchronized(community.checked) { community.checked.none { it == "S" } })
    }

    @Test
    fun aTakenName_pointsAtTheSectionThatHasIt_andCannotBeProposed() = runBlocking {
        val viewModel = viewModel(Community(nameAnswer = Result.success(SectionNameCheck.Taken("street-food"))))
        viewModel.pickPicture("content://picture")

        viewModel.updateName("Street food")

        val state = viewModel.state.firstWithin { it.nameStanding is NameStanding.Taken && it.picture != null }
        assertEquals(NameStanding.Taken("street-food"), state.nameStanding)
        assertFalse(state.canPropose)
    }

    @Test
    fun aNameTheServerWontTake_saysSo() = runBlocking {
        val viewModel = viewModel(Community(nameAnswer = Result.success(SectionNameCheck.Bad)))

        viewModel.updateName("!!")

        assertEquals(NameStanding.Bad, viewModel.state.firstWithin { it.nameStanding == NameStanding.Bad }.nameStanding)
    }

    @Test
    fun aNameCheckThatFails_isNoRefusal() = runBlocking {
        val viewModel = viewModel(Community(nameAnswer = Result.failure(IOException("offline"))))
        viewModel.pickPicture("content://picture")

        viewModel.updateName("Street food")
        delay(800)

        val state = viewModel.state.firstWithin { it.picture != null }
        assertEquals(NameStanding.Unasked, state.nameStanding)
        assertTrue(state.canPropose)
    }

    @Test
    fun aPictureNarrowerThan960_isTurnedAwayBeforeAnythingIsSent() = runBlocking {
        val pictures = FakeSectionPictures(size = Result.success(PictureSize(900, 600)))
        val viewModel = viewModel(pictures = pictures)

        viewModel.pickPicture("content://small")

        val state = viewModel.state.firstWithin { it.pictureNotice != null }
        assertEquals(PictureNotice.TooSmall(900), state.pictureNotice)
        assertNull(state.picture)
        assertTrue(pictures.uploads.isEmpty())
    }

    @Test
    fun aWidePanorama_isMeasuredByTheWidest16By10CutItHolds() = runBlocking {
        val viewModel = viewModel(pictures = FakeSectionPictures(size = Result.success(PictureSize(4000, 500))))

        viewModel.pickPicture("content://panorama")

        assertEquals(PictureNotice.TooSmall(800), viewModel.state.firstWithin { it.pictureNotice != null }.pictureNotice)
    }

    @Test
    fun aPictureThatCannotBeRead_asksForAnother() = runBlocking {
        val viewModel = viewModel(pictures = FakeSectionPictures(size = Result.failure(IOException("gone"))))

        viewModel.pickPicture("content://gone")

        assertEquals(PictureNotice.Unreadable, viewModel.state.firstWithin { it.pictureNotice != null }.pictureNotice)
    }

    @Test
    fun proposing_uploadsTheCrop_sendsTheProposal_andTheFormCloses() = runBlocking {
        val community = Community()
        val pictures = FakeSectionPictures(size = Result.success(PictureSize(2000, 1250)))
        val viewModel = viewModel(community, pictures = pictures, fileUnder = "food")
        ready(viewModel)
        viewModel.chooseIcon("pets")
        viewModel.state.firstWithin { it.icon == "pets" }

        viewModel.propose()

        withTimeout(5_000) { viewModel.proposed.first() }
        val (uri, crop) = pictures.uploads.single()
        assertEquals("content://picture", uri)
        assertCrop(PictureCrop(0f, 0f, 1f, 1f), crop)
        assertEquals(listOf(SectionProposal("Street food", "pets", "picture-1", "food")), community.proposed)
        assertEquals(listOf("street-food"), viewModel.state.firstWithin { it.waiting.isNotEmpty() }.waiting.map { it.id })
    }

    @Test
    fun anIconOutsideTheTwentyFour_isNotTaken() = runBlocking {
        val viewModel = viewModel()

        viewModel.chooseIcon("rocket_launch")
        viewModel.chooseIcon("flight")

        assertEquals("flight", viewModel.state.firstWithin { it.icon == "flight" }.icon)
    }

    @Test
    fun listsWaitUnderTheBoardsSection_onlyWhenItTakesLists() = runBlocking {
        assertEquals("food", viewModel(fileUnder = "food").state.value.fileUnder)
        assertEquals("other", viewModel(fileUnder = "kites").state.value.fileUnder)
        assertEquals("other", viewModel().state.value.fileUnder)
    }

    @Test
    fun threeWaiting_theFormSaysSoInstead() = runBlocking {
        val mine = (1..3).map { Section(id = "mine-$it", name = "Mine $it", status = SectionStatus.Pending, mine = true) }
        val store = FakeSectionStore(SectionDirectory(sections = Section.BUILT_IN + mine, fetchedAtMillis = 1L))

        val state = viewModel(store = store).state.firstWithin { it.waiting.size == 3 }

        assertTrue(state.tooManyWaiting)
    }

    private fun refusedWith(failure: Throwable, pictures: FakeSectionPictures = FakeSectionPictures(size = Result.success(PictureSize(2000, 1250)))) =
        runBlocking {
            val viewModel = viewModel(Community(proposeAnswer = { Result.failure(failure) }), pictures = pictures)
            ready(viewModel)
            viewModel.propose()
            viewModel.state.firstWithin { !it.sending && (it.error != null || it.nameStanding != NameStanding.Free || it.pictureNotice != null) }
        }

    @Test
    fun aTakenName_goesUnderTheName_withTheSectionToOpen() {
        val state = refusedWith(ProposalRefused(ProposalError.NameTaken("street-food")))

        assertEquals(NameStanding.Taken("street-food"), state.nameStanding)
        assertNull(state.error)
    }

    @Test
    fun aBadName_goesUnderTheName() {
        assertEquals(NameStanding.Bad, refusedWith(ProposalRefused(ProposalError.BadName)).nameStanding)
    }

    @Test
    fun aPictureNotCropped_keepsThePicture_andAsksForAnotherCrop() {
        val state = refusedWith(ProposalRefused(ProposalError.BadPicture(PictureProblem.Not16By10)))

        assertEquals(PictureNotice.NotCropped, state.pictureNotice)
        assertEquals("content://picture", state.picture?.uri)
    }

    @Test
    fun aPictureTooSmall_asksForAnotherPicture() {
        val state = refusedWith(ProposalRefused(ProposalError.BadPicture(PictureProblem.TooSmall)))

        assertEquals(PictureNotice.TooSmall(2000), state.pictureNotice)
        assertNull(state.picture)
    }

    @Test
    fun aPictureTheServerCouldNotRead_asksForAnotherPicture() {
        listOf(PictureProblem.Missing, PictureProblem.NotImage).forEach { why ->
            val state = refusedWith(ProposalRefused(ProposalError.BadPicture(why)))

            assertEquals(PictureNotice.Unreadable, state.pictureNotice)
            assertNull(state.picture)
        }
    }

    @Test
    fun everyOtherRefusal_isShownAboveTheActions_andTheFormIsKept() {
        val formWide = listOf(
            ProposalError.BadIcon,
            ProposalError.BadFileUnder,
            ProposalError.TooManyPending,
            ProposalError.TooManyToday,
            ProposalError.Banned(untilMillis = 1_800_000_000_000L),
            ProposalError.Banned(untilMillis = null),
            ProposalError.NotSignedIn,
            ProposalError.Failed,
        )
        formWide.forEach { error ->
            val state = refusedWith(ProposalRefused(error))

            assertEquals(error, state.error)
            assertEquals("Street food", state.name)
            assertEquals("content://picture", state.picture?.uri)
        }
    }

    @Test
    fun aFailureWithNoAnswer_isAFailureToTryAgain() {
        assertEquals(ProposalError.Failed, refusedWith(IOException("offline")).error)
    }

    @Test
    fun anUploadRefused_sendsNoProposal() = runBlocking {
        val community = Community()
        val pictures = FakeSectionPictures(
            size = Result.success(PictureSize(2000, 1250)),
            uploadAnswer = Result.failure(ProposalRefused(ProposalError.NotSignedIn)),
        )
        val viewModel = viewModel(community, pictures = pictures)
        ready(viewModel)

        viewModel.propose()

        assertEquals(ProposalError.NotSignedIn, viewModel.state.firstWithin { it.error != null }.error)
        assertTrue(community.proposed.isEmpty())
    }

    private companion object {
        fun assertCrop(expected: PictureCrop, actual: PictureCrop) {
            assertEquals(expected.left, actual.left, 1e-4f)
            assertEquals(expected.top, actual.top, 1e-4f)
            assertEquals(expected.width, actual.width, 1e-4f)
            assertEquals(expected.height, actual.height, 1e-4f)
        }

        fun pending(proposal: SectionProposal) =
            Section(id = "street-food", name = proposal.name, status = SectionStatus.Pending, fileUnder = proposal.fileUnder, mine = true)
    }
}

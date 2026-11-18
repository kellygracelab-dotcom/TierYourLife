package com.artiuillab.tieryourlife.feature.account.presentation.account

import com.artiuillab.tieryourlife.core.settings.CardFace
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.settings.ThePerson
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.FakeFacePhotos
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.FaceCrop
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.OwnLists
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * "Your photo" (FACE-PHOTO-2026-11-19): one slot for the uploaded photo, kept on this phone by account;
 * the letter or a card never wipes it, and a card, cut or not, never takes it.
 */
class FaceSlotTest {

    @get:Rule
    val main = MainDispatcherRule()

    // Danylo's bug of 19.11: the letter wiped the uploaded photo, and there was no way back to it.
    @Test
    fun theLetter_leavesTheUploadedPhotoInItsSlot_andATapWearsItAgain_withoutGoingUp() = runBlocking {
        val repository = FakeAccountRepository(initial = FakeAccountRepository.signedIn)
        val photos = FakeFacePhotos()
        val preferences = FakeAppPreferences()
        val viewModel = viewModel(repository, facePhotos = photos, preferences = preferences)
        viewModel.face.position("content://photos/1", fromGallery = true)
        viewModel.face.confirmPosition(null)
        viewModel.state.firstWithin { !it.makingFace && repository.photoUrls.isNotEmpty() }

        viewModel.face.wearLetter()
        viewModel.state.firstWithin { !it.makingFace && repository.photoUrls.size == 2 }
        assertEquals(FakeCommunityRepository.FACE_URL, viewModel.state.value.ownFace)
        assertEquals(FakeCommunityRepository.FACE_URL, preferences.ownFace(SIGNED_IN_KEY))

        viewModel.face.wearOwnPhoto()
        viewModel.state.firstWithin { !it.makingFace && repository.photoUrls.size == 3 }

        assertEquals(listOf(FakeCommunityRepository.FACE_URL, null, FakeCommunityRepository.FACE_URL), repository.photoUrls)
        assertEquals(1, photos.uploaded.size)
        assertEquals(FakeCommunityRepository.FACE_URL, viewModel.state.value.shownFace)
    }

    @Test
    fun aCard_isWornWithoutTakingThePhotosSlot() = runBlocking {
        val repository = FakeAccountRepository(initial = FakeAccountRepository.signedIn)
        val preferences = FakeAppPreferences().apply { setOwnFace(SIGNED_IN_KEY, OWN_PHOTO) }
        val viewModel = viewModel(repository, publishedLists = FakeOwnLists(cards = listOf(POSTER)), preferences = preferences)
        viewModel.state.firstWithin { it.ownFace == OWN_PHOTO }

        viewModel.face.position(POSTER, fromGallery = false)
        viewModel.face.confirmPosition(null)
        viewModel.state.firstWithin { !it.makingFace && repository.photoUrls.isNotEmpty() }

        assertEquals(POSTER, viewModel.state.value.shownFace)
        assertEquals(OWN_PHOTO, viewModel.state.value.ownFace)
    }

    // Put on from another device, or before the slot was kept: a worn face that is none of the cards is the photo.
    @Test
    fun aWornFaceThatIsNoCard_takesThePhotosSlot_aWornCardDoesNot() = runBlocking {
        val worn = FakeAccountRepository(initial = FakeAccountRepository.signedIn.withFace(OLD_FACE))
        val preferences = FakeAppPreferences()
        viewModel(worn, publishedLists = FakeOwnLists(cards = listOf(POSTER)), preferences = preferences)
            .state.firstWithin { it.ownFace == OLD_FACE }
        assertEquals(OLD_FACE, preferences.ownFace(SIGNED_IN_KEY))

        val wearingACard = FakeAccountRepository(initial = FakeAccountRepository.signedIn.withFace(POSTER))
        val untouched = FakeAppPreferences()
        val viewModel = viewModel(wearingACard, publishedLists = FakeOwnLists(cards = listOf(POSTER)), preferences = untouched)
        viewModel.state.firstWithin { it.faceChoices.isNotEmpty() }
        assertNull(viewModel.state.value.ownFace)
        assertTrue(untouched.ownFaces.isEmpty())
    }

    // A card moved in the circle is cut and copied, yet stays a card: the check is on its tile, the photo keeps its slot.
    @Test
    fun aCardCutAndCopied_isCheckedOnItsTile_andLeavesThePhotoBe() = runBlocking<Unit> {
        val repository = FakeAccountRepository(initial = FakeAccountRepository.signedIn)
        val preferences = FakeAppPreferences().apply { setOwnFace(SIGNED_IN_KEY, OWN_PHOTO) }
        val viewModel = viewModel(repository, publishedLists = FakeOwnLists(cards = listOf(POSTER)), preferences = preferences)
        viewModel.state.firstWithin { it.ownFace == OWN_PHOTO && it.faceChoices.isNotEmpty() }

        viewModel.face.position(POSTER, fromGallery = false)
        viewModel.face.confirmPosition(FaceCrop(0f, 0.1f, 1f, 0.66f))
        viewModel.state.firstWithin { !it.makingFace && repository.photoUrls.isNotEmpty() }

        assertEquals(OWN_PHOTO, viewModel.state.value.ownFace)
        assertEquals(CardFace(POSTER, FakeCommunityRepository.FACE_URL), preferences.cardFace(SIGNED_IN_KEY))
        viewModel.state.firstWithin { it.faceInSheet == POSTER }
    }

    // A card of the account's own pictures, kept with us, is copied by the server as it is: still a card.
    @Test
    fun aCardKeptWithUs_isCopiedByTheServer_andStaysACard() = runBlocking {
        val repository = FakeAccountRepository(initial = FakeAccountRepository.signedIn)
        val kept = "/data/pictures/0123456789abcdef0123456789abcdef"
        val viewModel = viewModel(repository, publishedLists = FakeOwnLists(cards = listOf(kept)))
        viewModel.state.firstWithin { it.faceChoices.isNotEmpty() }

        viewModel.face.position(kept, fromGallery = false)
        viewModel.face.confirmPosition(null)
        viewModel.state.firstWithin { !it.makingFace && it.faceInSheet == kept }

        assertNull(viewModel.state.value.ownFace)
    }

    // The copy of a card worn on this phone is not mistaken for the photo when the screen opens again.
    @Test
    fun aWornCopyOfACard_isNotTakenForThePhoto() = runBlocking {
        val repository = FakeAccountRepository(initial = FakeAccountRepository.signedIn.withFace(OLD_FACE))
        val preferences = FakeAppPreferences().apply { setCardFace(SIGNED_IN_KEY, CardFace(POSTER, OLD_FACE)) }
        val viewModel = viewModel(repository, publishedLists = FakeOwnLists(cards = listOf(POSTER)), preferences = preferences)
        viewModel.state.firstWithin { it.faceChoices.isNotEmpty() }

        assertNull(viewModel.state.value.ownFace)
        assertEquals(POSTER, viewModel.state.value.faceInSheet)
    }

    // The slot is the account's: another account on this phone does not see it.
    @Test
    fun thePhotosSlot_isKeptPerAccount() = runBlocking {
        val preferences = FakeAppPreferences().apply { setOwnFace("someone-else", OWN_PHOTO) }
        val viewModel = viewModel(FakeAccountRepository(initial = FakeAccountRepository.signedIn), preferences = preferences)
        viewModel.state.firstWithin { it.account is Account.SignedIn }

        assertNull(viewModel.state.value.ownFace)
    }

    private fun viewModel(
        repository: AccountRepository,
        facePhotos: FakeFacePhotos = FakeFacePhotos(),
        publishedLists: OwnLists = FakeOwnLists(),
        preferences: FakeAppPreferences = FakeAppPreferences(),
    ) = AccountViewModel(
        repository,
        FakeGenerationCredits(),
        publishedLists,
        FakeCommunityForAccount(),
        RecordingMerge(guestBoards = 0),
        NoErasure,
        facePhotos,
        ThePerson(preferences, emptySet()),
        preferences,
    )
}

private const val OWN_PHOTO = "https://example.test/avatars/own.jpg"
private const val SIGNED_IN_KEY = FakeAccountRepository.SIGNED_IN_EMAIL

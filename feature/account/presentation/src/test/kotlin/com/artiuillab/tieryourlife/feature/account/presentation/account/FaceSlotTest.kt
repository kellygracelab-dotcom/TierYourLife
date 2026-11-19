package com.artiuillab.tieryourlife.feature.account.presentation.account

import com.artiuillab.tieryourlife.core.settings.CardFace
import com.artiuillab.tieryourlife.core.settings.FakeAppPreferences
import com.artiuillab.tieryourlife.core.settings.ThePerson
import com.artiuillab.tieryourlife.core.testing.DEFAULT_WAIT
import com.artiuillab.tieryourlife.core.testing.MainDispatcherRule
import com.artiuillab.tieryourlife.core.testing.firstWithin
import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.FakeFacePhotoSlot
import com.artiuillab.tieryourlife.feature.account.domain.FakeFacePhotos
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.FaceCrop
import com.artiuillab.tieryourlife.feature.account.presentation.account.face.sameFace
import com.artiuillab.tieryourlife.feature.community.domain.FakeCommunityRepository
import com.artiuillab.tieryourlife.feature.tier.domain.repository.OwnLists
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * "Your photo" (FACE-PHOTO-2026-11-19, Proxy "The account's photo"): one slot with the account; the letter
 * or a card never wipes it, a card, cut or not, never takes it, and "Remove your photo" has its Undo.
 */
class FaceSlotTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val repository = FakeAccountRepository(initial = FakeAccountRepository.signedIn)
    private val preferences = FakeAppPreferences()
    private val slot = FakeFacePhotoSlot(worn = { (repository.current() as? Account.SignedIn)?.photoUrl })

    /** As the server: an upload from the device fills the slot. */
    private val community = object : FakeCommunityRepository() {
        override suspend fun makeFace(pictureId: String, upload: Boolean): Result<String> =
            super.makeFace(pictureId, upload).onSuccess { if (upload) slot.photo = it }
    }

    // QA D18, 587 and 589: after an upload "Your photo" was not ticked, not even after the screen was opened again.
    @Test
    fun anUpload_isTicked_asItIsWorn_andAfterTheScreenOpensAgain() = runBlocking {
        val answered = "https://tieryourlife.web.app/img/original/avatars/ann/${FakeFacePhotos.PICTURE_ID}"
        val kept = "https://firebasestorage.googleapis.com/v0/b/tieryourlife.firebasestorage.app/o/avatars%2Fann%2F${FakeFacePhotos.PICTURE_ID}?alt=media&token=t"
        val server = object : FakeCommunityRepository() {
            override suspend fun makeFace(pictureId: String, upload: Boolean): Result<String> {
                if (upload) slot.photo = kept
                return Result.success(answered)
            }
        }
        val viewModel = viewModel(facePhotos = FakeFacePhotos(), community = server)
        viewModel.face.position("content://photos/1", fromGallery = true)
        viewModel.face.confirmPosition(null)
        viewModel.state.firstWithin { !it.makingFace && repository.photoUrls.isNotEmpty() }
        assertTrue(viewModel.state.value.let { sameFace(it.faceInSheet, it.ownFace) })

        viewModel.face.load()
        val reopened = viewModel.state.firstWithin { it.ownFace == kept }

        assertTrue(sameFace(reopened.faceInSheet, reopened.ownFace))
    }

    // Danylo's bug of 19.11: the letter wiped the uploaded photo, and there was no way back to it.
    @Test
    fun anUpload_takesTheSlot_theLetterLeavesIt_andATapWearsItAgain_withoutGoingUp() = runBlocking {
        val photos = FakeFacePhotos()
        val viewModel = viewModel(facePhotos = photos)
        viewModel.face.position("content://photos/1", fromGallery = true)
        viewModel.face.confirmPosition(null)
        viewModel.state.firstWithin { !it.makingFace && repository.photoUrls.isNotEmpty() }
        assertEquals(listOf(FakeFacePhotos.PICTURE_ID to true), community.facesMade)

        viewModel.face.wearLetter()
        viewModel.state.firstWithin { !it.makingFace && repository.photoUrls.size == 2 }
        assertEquals(FakeCommunityRepository.FACE_URL, viewModel.state.value.ownFace)

        viewModel.face.wearOwnPhoto()
        viewModel.state.firstWithin { !it.makingFace && repository.photoUrls.size == 3 }

        assertEquals(listOf(FakeCommunityRepository.FACE_URL, null, FakeCommunityRepository.FACE_URL), repository.photoUrls)
        assertEquals(1, photos.uploaded.size)
    }

    @Test
    fun theSlot_isTheAccounts_readWhenTheScreenOpens() = runBlocking {
        slot.photo = OWN_PHOTO
        val viewModel = viewModel()

        viewModel.state.firstWithin { it.ownFace == OWN_PHOTO }
        assertEquals(OWN_PHOTO, preferences.ownFace(SIGNED_IN_KEY))
    }

    // Offline, this phone's copy of the slot stands in.
    @Test
    fun offline_thePhonesCopyOfTheSlotIsShown() = runBlocking {
        preferences.setOwnFace(SIGNED_IN_KEY, OWN_PHOTO)
        val viewModel = viewModel(slot = FakeFacePhotoSlot(unreachable = true))

        viewModel.state.firstWithin { it.ownFace == OWN_PHOTO }
        assertEquals(OWN_PHOTO, viewModel.state.value.ownFace)
    }

    // A card moved in the circle is cut and copied, yet stays a card: the check is on its tile, the slot is left be.
    @Test
    fun aCardCutAndCopied_goesUpAsACard_andIsCheckedOnItsTile() = runBlocking<Unit> {
        slot.photo = OWN_PHOTO
        val viewModel = viewModel(publishedLists = FakeOwnLists(cards = listOf(POSTER)))
        viewModel.state.firstWithin { it.ownFace == OWN_PHOTO && it.faceChoices.isNotEmpty() }

        viewModel.face.position(POSTER, fromGallery = false)
        viewModel.face.confirmPosition(FaceCrop(0f, 0.1f, 1f, 0.66f))
        viewModel.state.firstWithin { !it.makingFace && repository.photoUrls.isNotEmpty() }

        assertEquals(listOf(FakeFacePhotos.PICTURE_ID to false), community.facesMade)
        assertEquals(OWN_PHOTO, viewModel.state.value.ownFace)
        assertEquals(CardFace(POSTER, FakeCommunityRepository.FACE_URL), preferences.cardFace(SIGNED_IN_KEY))
        viewModel.state.firstWithin { it.faceInSheet == POSTER }
    }

    // A card of the account's own pictures, kept with us, is copied by the server as it is: still a card.
    @Test
    fun aCardKeptWithUs_isCopiedByTheServer_andStaysACard() = runBlocking<Unit> {
        val kept = "/data/pictures/0123456789abcdef0123456789abcdef"
        val viewModel = viewModel(publishedLists = FakeOwnLists(cards = listOf(kept)))
        viewModel.state.firstWithin { it.faceChoices.isNotEmpty() }

        viewModel.face.position(kept, fromGallery = false)
        viewModel.face.confirmPosition(null)
        viewModel.state.firstWithin { !it.makingFace && it.faceInSheet == kept }

        assertEquals(listOf("0123456789abcdef0123456789abcdef" to false), community.facesMade)
        assertNull(viewModel.state.value.ownFace)
    }

    // QA D16, 587: worn under another token, the photo was not seen as worn and stayed in the header.
    @Test
    fun removingTheWornPhoto_underAnotherToken_wearsTheLetterAtOnce() = runBlocking {
        slot.photo = "$OWN_PHOTO?token=slot"
        repository.setPhotoUrl("$OWN_PHOTO?token=profile")
        val viewModel = viewModel()
        viewModel.state.firstWithin { it.ownFace != null && it.shownFace != null }

        viewModel.face.removeOwnPhoto()

        assertNull(viewModel.state.value.shownFace)
        assertEquals(AccountNotice.PhotoRemoved, viewModel.state.value.notice)
    }

    @Test
    fun removingTheWornPhoto_wearsTheLetterAtOnce_andUndoPutsItBackOn() = runBlocking {
        slot.photo = OWN_PHOTO
        repository.setPhotoUrl(OWN_PHOTO)
        val viewModel = viewModel()
        viewModel.state.firstWithin { it.ownFace == OWN_PHOTO && it.shownFace == OWN_PHOTO }

        viewModel.face.removeOwnPhoto()
        assertNull(viewModel.state.value.shownFace)
        assertEquals(AccountNotice.PhotoRemoved, viewModel.state.value.notice)
        viewModel.state.firstWithin { !it.makingFace && repository.photoUrls.size == 2 }
        assertEquals(1, slot.removals)
        assertNull(viewModel.state.value.ownFace)

        viewModel.face.undoRemove()
        viewModel.state.firstWithin { !it.makingFace && repository.photoUrls.size == 3 }

        assertEquals(listOf(OWN_PHOTO, null, OWN_PHOTO), repository.photoUrls)
        assertEquals(OWN_PHOTO, viewModel.state.value.ownFace)
        assertEquals(OWN_PHOTO, slot.photo)
    }

    @Test
    fun removingAPhotoNotWorn_leavesTheFaceBe() = runBlocking {
        slot.photo = OWN_PHOTO
        val viewModel = viewModel()
        viewModel.state.firstWithin { it.ownFace == OWN_PHOTO }

        viewModel.face.removeOwnPhoto()
        until { slot.removals == 1 && viewModel.state.value.ownFace == null }

        assertEquals(emptyList<String?>(), repository.photoUrls)
    }

    // Undo after a new photo went up elsewhere: that one stays, and is shown.
    @Test
    fun undo_afterANewUpload_showsTheNewPhoto() = runBlocking {
        slot.photo = OWN_PHOTO
        val viewModel = viewModel()
        viewModel.state.firstWithin { it.ownFace == OWN_PHOTO }
        viewModel.face.removeOwnPhoto()
        until { slot.removals == 1 }
        slot.uploadedSince = OTHER_PHOTO

        viewModel.face.undoRemove()

        viewModel.state.firstWithin { it.ownFace == OTHER_PHOTO }
        assertEquals(emptyList<String?>(), repository.photoUrls)
    }

    // The removal never reached the account: the photo is back on screen, and it is said.
    @Test
    fun aRemovalThatFails_putsThePhotoBack_andSaysSo() = runBlocking {
        preferences.setOwnFace(SIGNED_IN_KEY, OWN_PHOTO)
        val viewModel = viewModel(slot = FakeFacePhotoSlot(unreachable = true))
        viewModel.state.firstWithin { it.ownFace == OWN_PHOTO }

        viewModel.face.removeOwnPhoto()

        viewModel.state.firstWithin { it.notice == AccountNotice.FaceNotSaved }
        assertEquals(OWN_PHOTO, viewModel.state.value.ownFace)
    }

    // The copy of a card worn on this phone is checked on its card when the screen opens again.
    @Test
    fun aWornCopyOfACard_isCheckedOnItsCard() = runBlocking<Unit> {
        repository.setPhotoUrl(OLD_FACE)
        preferences.setCardFace(SIGNED_IN_KEY, CardFace(POSTER, OLD_FACE))
        val viewModel = viewModel(publishedLists = FakeOwnLists(cards = listOf(POSTER)))

        viewModel.state.firstWithin { it.faceChoices.isNotEmpty() && it.faceInSheet == POSTER }
    }

    /** A removal that changes nothing on screen sends no new state to wait for: it is looked for. */
    private suspend fun until(condition: () -> Boolean) = withTimeout(DEFAULT_WAIT) {
        while (!condition()) delay(POLL)
    }

    private fun viewModel(
        facePhotos: FakeFacePhotos = FakeFacePhotos(),
        publishedLists: OwnLists = FakeOwnLists(),
        slot: FakeFacePhotoSlot = this.slot,
        community: FakeCommunityRepository = this.community,
    ) = AccountViewModel(
        repository,
        FakeGenerationCredits(),
        publishedLists,
        community,
        RecordingMerge(guestBoards = 0),
        NoErasure,
        facePhotos,
        slot,
        ThePerson(preferences, emptySet()),
        preferences,
    )
}

private const val OWN_PHOTO = "https://example.test/avatars/own.jpg"
private const val OTHER_PHOTO = "https://example.test/avatars/other.jpg"
private const val POLL = 10L
private const val SIGNED_IN_KEY = FakeAccountRepository.SIGNED_IN_EMAIL

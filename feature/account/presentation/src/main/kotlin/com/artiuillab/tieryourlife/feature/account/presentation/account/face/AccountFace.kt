package com.artiuillab.tieryourlife.feature.account.presentation.account.face

import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.settings.CardFace
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.FaceCrop
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.repository.FacePhotos
import com.artiuillab.tieryourlife.feature.account.presentation.account.AccountNotice
import com.artiuillab.tieryourlife.feature.account.presentation.account.AccountUiState
import com.artiuillab.tieryourlife.feature.account.presentation.account.FaceToPosition
import com.artiuillab.tieryourlife.feature.account.presentation.account.PendingFace
import com.artiuillab.tieryourlife.feature.community.domain.model.FaceRefused
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Changing the account's face. The chosen picture is on screen at once and
 * stays there while it goes up and the server looks at it; a refusal puts the
 * old face back and says so. Catalogue art left where it is needs no copy;
 * anything cut by the person, or from their phone, is cut here and sent up.
 *
 * A photo uploaded here is the account's own and keeps its slot whatever is
 * worn next: the letter or a card never wipes it. A card, cut or not, never
 * takes the slot (FACE-PHOTO-2026-11-19).
 */
internal class AccountFace(
    private val state: MutableStateFlow<AccountUiState>,
    private val scope: CoroutineScope,
    private val repository: AccountRepository,
    private val community: CommunityRepository,
    private val facePhotos: FacePhotos,
    private val preferences: AppPreferences,
    private val afterChange: suspend () -> Unit,
) {

    /** The slot of the account now on screen. */
    fun load() {
        val key = (state.value.account as? Account.SignedIn)?.recordKey
        state.update {
            it.copy(
                ownFace = key?.let(preferences::ownFace),
                cardFace = key?.let(preferences::cardFace),
            )
        }
    }

    /**
     * A face worn that is none of the cards is the account's photo: put on from
     * another device, or before the slot was kept. Asked only once the cards are known.
     */
    fun takeWornPhoto() {
        val now = state.value
        val worn = (now.account as? Account.SignedIn)?.photoUrl ?: return
        val known = now.faceChoices + listOfNotNull(now.cardFace?.face, now.ownFace)
        if (now.pendingFace == null && worn !in known) keepOwn(worn)
    }

    /** The photo, worn again as it is: nothing goes up. */
    fun wearOwnPhoto() {
        val photo = state.value.ownFace ?: return
        if (state.value.makingFace || photo == state.value.shownFace) return
        wearAtOnce(photo)
    }

    private fun keepOwn(photo: String) {
        val account = state.value.account as? Account.SignedIn ?: return
        preferences.setOwnFace(account.recordKey, photo)
        state.update { it.copy(ownFace = photo) }
    }

    private fun keepCardFace(cardFace: CardFace) {
        val account = state.value.account as? Account.SignedIn ?: return
        preferences.setCardFace(account.recordKey, cardFace)
        state.update { it.copy(cardFace = cardFace) }
    }

    /** A picture to place in the circle before it becomes the face. */
    fun position(sourceUri: String, fromGallery: Boolean) {
        if (state.value.makingFace) return
        state.update { it.copy(positioning = FaceToPosition(sourceUri, fromGallery)) }
    }

    fun cancelPositioning() {
        state.update { it.copy(positioning = null) }
    }

    /** [crop] null: nothing was moved, the middle square stands. */
    fun confirmPosition(crop: FaceCrop?) {
        val chosen = state.value.positioning ?: return
        state.update { it.copy(positioning = null) }
        when {
            chosen.fromGallery || crop != null -> cutAndSend(chosen.sourceUri, crop, card = chosen.sourceUri.takeUnless { chosen.fromGallery })
            chosen.sourceUri.startsWith("https://") -> wearAtOnce(chosen.sourceUri)
            else -> copyOwnPicture(chosen.sourceUri)
        }
    }

    /** No picture: the initial stands in. */
    fun wearLetter() {
        if (state.value.makingFace) return
        wearAtOnce(null)
    }

    private fun wearAtOnce(address: String?) {
        state.update { it.copy(makingFace = true, pendingFace = PendingFace(shown = address)) }
        scope.launch { wear(address, chosen = address != null) }
    }

    /**
     * A generated picture lives in a folder only its owner may read, so the
     * server copies it somewhere the community can see -- after looking at
     * it, as with a published board.
     */
    private fun copyOwnPicture(path: String) {
        state.update { it.copy(makingFace = true, pendingFace = PendingFace(shown = path)) }
        scope.launch {
            val made = community.makeFace(path.substringAfterLast('/'))
            wear(made.getOrNull(), chosen = true, failure = made.failureNotice(), card = path)
        }
    }

    /**
     * Shown the moment Done is pressed, as the chosen square of the photo
     * itself; then cut on the phone, up to the person's own folder and
     * through the server's look, all behind it.
     */
    private fun cutAndSend(sourceUri: String, crop: FaceCrop?, card: String?) {
        state.update { it.copy(makingFace = true, pendingFace = PendingFace(shown = sourceUri, crop = crop)) }
        scope.launch {
            val prepared = facePhotos.prepare(sourceUri, crop)
                .onFailure { Timber.w(it, "The chosen picture could not be cut") }
                .getOrNull()
            if (prepared == null) {
                wear(null, chosen = true)
                return@launch
            }
            state.update { it.copy(pendingFace = PendingFace(shown = prepared)) }
            val made = facePhotos.upload(prepared)
                .onFailure { Timber.w(it, "The chosen photo did not go up") }
                .fold(onSuccess = { pictureId -> community.makeFace(pictureId) }, onFailure = { Result.failure(it) })
            wear(made.getOrNull(), chosen = true, failure = made.failureNotice(), card = card, own = card == null)
        }
    }

    /**
     * [chosen] and no [address]: the picture was refused or lost on the way, [failure] says which, and the old face comes back.
     * [own]: an uploaded photo, so it takes the photo's slot; [card]: the card a copy was made of.
     */
    private suspend fun wear(
        address: String?,
        chosen: Boolean,
        failure: AccountNotice = AccountNotice.FaceNotMade,
        card: String? = null,
        own: Boolean = false,
    ) {
        if (chosen && address == null) {
            state.update { it.copy(makingFace = false, pendingFace = null, notice = failure) }
            return
        }
        if (address != null && own) keepOwn(address)
        if (address != null && card != null) keepCardFace(CardFace(card, address))
        val saved = repository.setPhotoUrl(address)
        state.update {
            it.copy(
                makingFace = false,
                pendingFace = if (saved) it.pendingFace else null,
                notice = if (saved) it.notice else AccountNotice.NameNotSaved,
            )
        }
        if (saved) afterChange()
    }
}

private fun Result<String>.failureNotice(): AccountNotice =
    if (exceptionOrNull() is FaceRefused) AccountNotice.FaceNotMade else AccountNotice.FaceNotSaved

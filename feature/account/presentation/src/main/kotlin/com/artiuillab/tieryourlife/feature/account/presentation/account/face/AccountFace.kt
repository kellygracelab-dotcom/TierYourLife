package com.artiuillab.tieryourlife.feature.account.presentation.account.face

import com.artiuillab.tieryourlife.feature.account.domain.model.FaceCrop
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.account.domain.repository.FacePhotos
import com.artiuillab.tieryourlife.feature.account.presentation.account.AccountNotice
import com.artiuillab.tieryourlife.feature.account.presentation.account.AccountUiState
import com.artiuillab.tieryourlife.feature.account.presentation.account.FaceToPosition
import com.artiuillab.tieryourlife.feature.account.presentation.account.PendingFace
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
 */
internal class AccountFace(
    private val state: MutableStateFlow<AccountUiState>,
    private val scope: CoroutineScope,
    private val repository: AccountRepository,
    private val community: CommunityRepository,
    private val facePhotos: FacePhotos,
    private val afterChange: suspend () -> Unit,
) {

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
            chosen.fromGallery || crop != null -> cutAndSend(chosen.sourceUri, crop)
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
            wear(community.makeFace(path.substringAfterLast('/')).getOrNull(), chosen = true)
        }
    }

    /**
     * Shown the moment Done is pressed, as the chosen square of the photo
     * itself; then cut on the phone, up to the person's own folder and
     * through the server's look, all behind it.
     */
    private fun cutAndSend(sourceUri: String, crop: FaceCrop?) {
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
            val address = facePhotos.upload(prepared)
                .onFailure { Timber.w(it, "The chosen photo did not go up") }
                .getOrNull()
                ?.let { pictureId -> community.makeFace(pictureId).getOrNull() }
            wear(address, chosen = true)
        }
    }

    /** [chosen] and no [address]: the picture was refused or lost on the way, and the old face comes back. */
    private suspend fun wear(address: String?, chosen: Boolean) {
        if (chosen && address == null) {
            state.update { it.copy(makingFace = false, pendingFace = null, notice = AccountNotice.FaceNotMade) }
            return
        }
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

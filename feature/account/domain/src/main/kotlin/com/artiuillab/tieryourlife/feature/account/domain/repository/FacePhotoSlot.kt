package com.artiuillab.tieryourlife.feature.account.domain.repository

/**
 * The account's photo (Proxy CONTRACT.md, lists, "The account's photo"): the one uploaded for the face,
 * one slot with the account, the same on the phone and the website. Wearing the letter or a card leaves it;
 * only a new upload or [remove] changes it.
 */
interface FacePhotoSlot {

    /** The photo's address, or null when the account has none. */
    suspend fun photo(): Result<String?>

    /** Takes it away at once and everywhere it shows; [RemovedPhoto.wasWorn]: the account's face was it. */
    suspend fun remove(): Result<RemovedPhoto>

    /** Undo of [remove], while the server still keeps the copy. */
    suspend fun restore(): Result<RestoredPhoto>
}

data class RemovedPhoto(val wasWorn: Boolean)

sealed interface RestoredPhoto {
    /** Back in the slot; worn again when it was worn. */
    data class Back(val url: String, val wasWorn: Boolean) : RestoredPhoto

    /** A new photo was uploaded since; that one stays. */
    data class Replaced(val url: String) : RestoredPhoto

    /** Too late, or nothing to put back. */
    data object Gone : RestoredPhoto
}

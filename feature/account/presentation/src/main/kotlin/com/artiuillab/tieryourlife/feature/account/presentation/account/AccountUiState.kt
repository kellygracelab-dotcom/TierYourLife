package com.artiuillab.tieryourlife.feature.account.presentation.account

import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.FaceCrop

data class AccountUiState(
    val account: Account = Account.Unknown,
    val signingIn: Boolean = false,
    val savingName: Boolean = false,
    /** Generations left, or null where nothing is counted. */
    val credits: Int? = null,
    val publicListCount: Int = 0,
    /** Card pictures the reader can wear as a face. */
    val faceChoices: List<String> = emptyList(),
    /** True while a picture of their own is being made into a face. */
    val makingFace: Boolean = false,
    /** The face just chosen, shown before the account wears it. */
    val pendingFace: PendingFace? = null,
    /** A picture being placed in the circle, before it is chosen. */
    val positioning: FaceToPosition? = null,
    val notice: AccountNotice? = null,
    /** True while the account is being removed; nothing else can be pressed. */
    val deleting: Boolean = false,
    /** True while signing out waits for the call a sync is in to end. */
    val signingOut: Boolean = false,
    /**
     * Set only while a question is open: somebody has signed in while this
     * phone has boards made without an account, or held for one it was signed
     * out of, and nothing has been moved or sent yet.
     */
    val merge: GuestBoardsQuestion? = null,
) {
    /**
     * The face to draw: the one just chosen, from the moment it is chosen and
     * on after the account wears it (the same face, already on this phone),
     * otherwise the account's own.
     */
    val shownFace: String?
        get() = pendingFace.let { pending -> if (pending != null) pending.shown else (account as? Account.SignedIn)?.photoUrl }

    /** The square to show of [shownFace] while the picture is still the whole photo, before it is cut. */
    val shownFaceCrop: FaceCrop?
        get() = pendingFace?.crop
}

/** What is on screen at once: the picture cut on this phone, a card's own address, or null for the letter. */
data class PendingFace(val shown: String?, val crop: FaceCrop? = null)

data class FaceToPosition(val sourceUri: String, val fromGallery: Boolean)

/**
 * [boards] made without an account, and the account they could move [into], by
 * its address. [fromAnotherAccount]: instead the boards of an account this phone
 * was signed out of, asked about on their own and first.
 */
data class GuestBoardsQuestion(val boards: Int, val into: String?, val fromAnotherAccount: Boolean = false)

enum class AccountNotice {
    SignInFailed,
    NoGoogleAccount,
    SignInUnavailable,
    SignedInToExistingAccount,
    NameNotSaved,

    /** The picture was refused, or could not be read. The old face stays. */
    FaceNotMade,

    /** The picture did not reach the server, or the server did not answer. The old face stays. */
    FaceNotSaved,

    /** The account is still there. Saying nothing would read as "it worked". */
    NotDeleted,

    /** Nothing was copied; they stay on this phone and are offered again. */
    BoardsNotCopied,
}

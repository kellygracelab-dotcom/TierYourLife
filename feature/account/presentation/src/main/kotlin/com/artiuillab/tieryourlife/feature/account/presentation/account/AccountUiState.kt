package com.artiuillab.tieryourlife.feature.account.presentation.account

import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.model.FaceCrop
import com.artiuillab.tieryourlife.feature.tier.domain.sync.MergeChoice

data class AccountUiState(
    val account: Account = Account.Unknown,
    val signingIn: Boolean = false,
    val savingName: Boolean = false,
    /** Generations left, or null where nothing is counted. */
    val credits: Int? = null,
    val publicListCount: Int = 0,
    /** Boards on this phone, so the offer can say what it would be keeping. */
    val boardCount: Int = 0,
    val backUpBoards: Boolean = true,
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
    /**
     * Set only while the question is open: somebody has signed into an account
     * that already holds boards, and nothing has been written either way yet.
     */
    val merge: MergeChoice? = null,
    val mergeKeep: MergeKeep = MergeKeep.Everything,
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

/** Which set of boards is in use afterwards. Neither answer deletes anything. */
enum class MergeKeep { Everything, AccountOnly }

enum class AccountNotice {
    SignInFailed,
    NoGoogleAccount,
    SignInUnavailable,
    SignedInToExistingAccount,
    NameNotSaved,

    /** The picture was refused, or would not copy. The old face stays. */
    FaceNotMade,

    /** The account is still there. Saying nothing would read as "it worked". */
    NotDeleted,
}

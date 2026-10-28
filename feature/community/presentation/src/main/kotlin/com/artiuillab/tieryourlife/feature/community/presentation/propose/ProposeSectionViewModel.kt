package com.artiuillab.tieryourlife.feature.community.presentation.propose

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.MAX_PENDING_SECTIONS
import com.artiuillab.tieryourlife.feature.community.domain.model.MAX_SECTION_NAME
import com.artiuillab.tieryourlife.feature.community.domain.model.PictureProblem
import com.artiuillab.tieryourlife.feature.community.domain.model.ProposalError
import com.artiuillab.tieryourlife.feature.community.domain.model.ProposalRefused
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionNameCheck
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionPictureSize
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionProposal
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionSymbols
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.repository.PictureSize
import com.artiuillab.tieryourlife.feature.community.domain.repository.SectionCatalog
import com.artiuillab.tieryourlife.feature.community.domain.repository.SectionPictures
import com.artiuillab.tieryourlife.feature.community.presentation.navigation.proposeSectionRoute
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the server said of the name typed so far. */
sealed interface NameStanding {
    data object Unasked : NameStanding

    data object Asking : NameStanding

    data object Free : NameStanding

    data class Taken(val id: String?) : NameStanding

    data object Bad : NameStanding
}

/** A picture chosen from the phone, with its size upright. */
data class PickedPicture(val uri: String, val size: PictureSize)

/** Why the picture area is asking for another picture, or for another crop. */
sealed interface PictureNotice {
    /** Narrower than a section's picture may be; [width] is the widest 16:10 cut it holds. */
    data class TooSmall(val width: Int) : PictureNotice

    data object NotCropped : PictureNotice

    data object Unreadable : PictureNotice
}

/** The proposal form (Claude Design, design-spec-admin §2): name, picture, icon, where lists wait, and how it went. */
data class ProposeSectionUiState(
    /** Null until the account has answered. */
    val signedIn: Boolean? = null,
    val name: String = "",
    val nameStanding: NameStanding = NameStanding.Unasked,
    val picture: PickedPicture? = null,
    val pictureNotice: PictureNotice? = null,
    val frame: PictureFrame = PictureFrame(),
    val icon: String = SectionSymbols.DEFAULT,
    val fileUnder: String = ListCategory.Other.id,
    /** This person's own proposals still waiting. */
    val waiting: List<Section> = emptyList(),
    val sending: Boolean = false,
    /** A refusal that belongs to the whole form, shown above its actions. */
    val error: ProposalError? = null,
) {
    val tooManyWaiting: Boolean get() = waiting.size >= MAX_PENDING_SECTIONS

    /** A cropped picture and a name the server has not turned down. */
    val canPropose: Boolean
        get() = !sending &&
            picture != null &&
            name.trim().length >= MIN_NAME &&
            nameStanding !is NameStanding.Taken &&
            nameStanding != NameStanding.Bad

    /** Something worth asking about before it is thrown away. */
    val edited: Boolean get() = picture != null || name.isNotBlank()
}

private const val MIN_NAME = 2
private const val NAME_CHECK_DELAY_MILLIS = 400L

@HiltViewModel
class ProposeSectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    accounts: AccountRepository,
    private val community: CommunityRepository,
    private val catalog: SectionCatalog,
    private val pictures: SectionPictures,
) : ViewModel() {

    private val form = MutableStateFlow(
        ProposeSectionUiState(fileUnder = startingFileUnder(savedStateHandle.proposeSectionRoute().fileUnder)),
    )

    val state: StateFlow<ProposeSectionUiState> = combine(form, accounts.account, catalog.directory) { now, account, directory ->
        now.copy(
            signedIn = when (account) {
                Account.Unknown -> null
                Account.Guest -> false
                is Account.SignedIn -> true
            },
            waiting = directory.waiting,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, form.value)

    private val sent = Channel<Unit>(Channel.CONFLATED)

    /** Once, when the server has the proposal: the form closes. */
    val proposed: Flow<Unit> = sent.receiveAsFlow()

    private var checking: Job? = null

    /** Only an open section takes lists; anything else, or nothing, starts on Other. */
    private fun startingFileUnder(wanted: String?): String =
        wanted?.takeIf { id -> catalog.directory.value.shown.any { it.id == id } } ?: ListCategory.Other.id

    /** At most 24 characters; the server is asked about it once the typing has stopped for a moment. */
    fun updateName(typed: String) {
        val name = typed.take(MAX_SECTION_NAME)
        form.update { it.copy(name = name, nameStanding = NameStanding.Unasked, error = null) }
        checking?.cancel()
        val trimmed = name.trim()
        if (trimmed.length < MIN_NAME) return
        checking = viewModelScope.launch {
            delay(NAME_CHECK_DELAY_MILLIS)
            form.update { it.copy(nameStanding = NameStanding.Asking) }
            val standing = community.checkSectionName(trimmed).fold(
                onSuccess = { answer ->
                    when (answer) {
                        SectionNameCheck.Free -> NameStanding.Free
                        is SectionNameCheck.Taken -> NameStanding.Taken(answer.id)
                        SectionNameCheck.Bad -> NameStanding.Bad
                    }
                },
                // Not knowing is not a refusal: the proposal itself will say.
                onFailure = { NameStanding.Unasked },
            )
            form.update { if (it.name.trim() == trimmed) it.copy(nameStanding = standing) else it }
        }
    }

    /** Checked here first: a picture narrower than 960 px at 16:10 is refused before anything is sent. */
    fun pickPicture(uri: String) {
        viewModelScope.launch {
            pictures.measure(uri).fold(
                onSuccess = { size ->
                    val widest = PictureFrame.widestCut(size)
                    form.update {
                        if (widest < SectionPictureSize.MIN_WIDTH) {
                            it.copy(picture = null, pictureNotice = PictureNotice.TooSmall(widest), frame = PictureFrame())
                        } else {
                            it.copy(picture = PickedPicture(uri, size), pictureNotice = null, frame = PictureFrame(), error = null)
                        }
                    }
                },
                onFailure = { form.update { it.copy(picture = null, pictureNotice = PictureNotice.Unreadable) } },
            )
        }
    }

    fun moveFrame(frame: PictureFrame) {
        form.update { it.copy(frame = frame, pictureNotice = it.pictureNotice.takeUnless { notice -> notice == PictureNotice.NotCropped }) }
    }

    fun chooseIcon(icon: String) {
        if (icon in SectionSymbols.ALL) form.update { it.copy(icon = icon) }
    }

    fun chooseFileUnder(sectionId: String) {
        form.update { it.copy(fileUnder = sectionId, error = null) }
    }

    fun propose() {
        val now = state.value
        val picture = now.picture ?: return
        if (!now.canPropose) return
        form.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            val result = pictures.upload(picture.uri, now.frame.crop(picture.size)).mapCatching { pictureId ->
                catalog.propose(SectionProposal(now.name.trim(), now.icon, pictureId, now.fileUnder)).getOrThrow()
            }
            result.fold(
                onSuccess = {
                    form.update { it.copy(sending = false) }
                    sent.send(Unit)
                },
                onFailure = { failure -> refused((failure as? ProposalRefused)?.error ?: ProposalError.Failed) },
            )
        }
    }

    /** Each refusal where it belongs: under the name, on the picture, or above the actions. */
    private fun refused(error: ProposalError) {
        form.update { now ->
            val stopped = now.copy(sending = false)
            when (error) {
                is ProposalError.NameTaken -> stopped.copy(nameStanding = NameStanding.Taken(error.id))
                ProposalError.BadName -> stopped.copy(nameStanding = NameStanding.Bad)
                is ProposalError.BadPicture -> when (error.why) {
                    PictureProblem.Not16By10 -> stopped.copy(pictureNotice = PictureNotice.NotCropped)
                    PictureProblem.TooSmall -> stopped.copy(
                        picture = null,
                        pictureNotice = PictureNotice.TooSmall(now.picture?.size?.let(PictureFrame::widestCut) ?: 0),
                    )

                    else -> stopped.copy(picture = null, pictureNotice = PictureNotice.Unreadable)
                }

                else -> stopped.copy(error = error)
            }
        }
    }
}

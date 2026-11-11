package com.artiuillab.tieryourlife.feature.community.presentation.list.components

import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.core.theme.messages.UserMessages
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedList
import com.artiuillab.tieryourlife.feature.community.domain.model.RankingRefusal
import com.artiuillab.tieryourlife.feature.community.domain.model.RankingRefused
import com.artiuillab.tieryourlife.feature.community.domain.model.ShownRanking
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.presentation.list.CommunityListUiState
import com.artiuillab.tieryourlife.feature.community.presentation.list.RankingNotice
import com.artiuillab.tieryourlife.feature.community.presentation.list.arrangementOf
import com.artiuillab.tieryourlife.feature.community.presentation.list.rowsOf
import com.artiuillab.tieryourlife.feature.community.presentation.list.tierLabelsOf
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/** Consent names one account and one private arrangement, including across the refresh before PUT. */
internal class RankingPublisher(
    private val listId: String,
    private val community: CommunityRepository,
    private val accounts: AccountRepository,
    private val state: MutableStateFlow<CommunityListUiState>,
    private val scope: CoroutineScope,
    private val messages: UserMessages,
    private val reread: suspend () -> PublishedList?,
    private val ready: () -> Boolean,
    private val visibilityChanged: suspend (Boolean) -> Unit,
    private val shown: (ShownRanking, PublishedList) -> Unit,
    private val tell: suspend (RankingNotice) -> Unit,
) {
    private val epoch = AtomicLong()
    private var identity = identityOf(accounts.current())
    private var inFlight: Job? = null

    fun accountChanged(account: Account) {
        val next = identityOf(account)
        if (next == identity) return
        identity = next
        epoch.incrementAndGet()
        inFlight?.cancel()
        stopSending()
    }

    fun show() {
        val current = state.value as? CommunityListUiState.Success ?: return
        if (current.arriving || !current.canShowRanking || current.sendingRanking) return
        val account = accounts.current() as? Account.SignedIn
        if (!current.signedIn || account == null) {
            scope.launch { tell(RankingNotice.SignInFirst) }
            return
        }
        if (!ready()) return
        val lease = RankingConsent(account.recordKey, account.uid ?: account.recordKey, epoch.incrementAndGet(), current.version, current.mine)
        state.value = current.copy(sendingRanking = true)
        inFlight = scope.launch {
            try {
                send(lease)
            } finally {
                if (epoch.get() == lease.epoch) stopSending()
            }
        }
    }

    private suspend fun send(lease: RankingConsent) {
        val answer = reread()
        if (!sameOwner(lease)) return
        if (answer != null && answer.summary.updatedAtMillis != lease.version) {
            tell(RankingNotice.ListChanged)
            return
        }
        if (answer == null) {
            messages.send(UserMessage.ActionFailed)
            return
        }
        if (!sameSnapshot(lease)) {
            messages.send(UserMessage.ActionFailed)
            return
        }
        val result = community.showMyRankingVersioned(
            listId, arrangementOf(answer, lease.mine), rowsOf(answer, lease.mine), tierLabelsOf(answer, lease.mine),
            answer.summary.updatedAtMillis, expectedOwnerUid = lease.uid,
        )
        if (!sameSnapshot(lease)) return
        result.fold(
            onSuccess = { publication ->
                visibilityChanged(true)
                if (!sameSnapshot(lease)) return@fold
                shown(publication, answer)
                tell(RankingNotice.Shown)
            },
            onFailure = { failure ->
                val refusal = noticeFor((failure as? RankingRefused)?.reason)
                if (refusal == null) messages.send(UserMessage.ActionFailed) else tell(refusal)
            },
        )
    }

    private fun sameOwner(lease: RankingConsent): Boolean =
        epoch.get() == lease.epoch && (accounts.current() as? Account.SignedIn)?.recordKey == lease.owner && ready()

    private fun sameSnapshot(lease: RankingConsent): Boolean {
        val current = state.value as? CommunityListUiState.Success ?: return false
        return sameOwner(lease) && !current.arriving && current.version == lease.version && current.mine == lease.mine
    }

    private fun stopSending() {
        state.update { if (it is CommunityListUiState.Success) it.copy(sendingRanking = false) else it }
    }
}

private data class RankingConsent(val owner: String, val uid: String, val epoch: Long, val version: Long, val mine: TierList)

private fun identityOf(account: Account): String = when (account) {
    is Account.SignedIn -> "user:${account.recordKey}"
    Account.Unknown -> "unknown"
    Account.Guest -> "guest"
}

private fun noticeFor(refusal: RankingRefusal?): RankingNotice? = when (refusal) {
    RankingRefusal.NotSignedIn -> RankingNotice.SignInFirst
    RankingRefusal.Banned -> RankingNotice.Banned
    RankingRefusal.OwnList -> RankingNotice.OwnList
    RankingRefusal.ListGone -> RankingNotice.ListGone
    RankingRefusal.ListChanged -> RankingNotice.ListChanged
    RankingRefusal.BadTierNames -> RankingNotice.BadTierNames
    RankingRefusal.CardsDontMatch -> RankingNotice.CardsDontMatch
    RankingRefusal.NotAccepted -> RankingNotice.NotAccepted
    RankingRefusal.TooManyWebPictures -> RankingNotice.TooManyWebPictures
    RankingRefusal.WebPicturesToday -> RankingNotice.WebPicturesToday
    else -> null
}

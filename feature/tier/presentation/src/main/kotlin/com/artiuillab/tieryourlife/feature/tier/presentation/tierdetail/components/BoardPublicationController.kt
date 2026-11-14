package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.community.domain.model.PublicationKind
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.repository.Published
import com.artiuillab.tieryourlife.feature.tier.domain.model.RankingChoice
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.repository.TierRepository
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.BoardPublicationState
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.PublicationPreview
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.RankingOffer
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.publishErrorOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A server decision belongs to one immutable board snapshot and one account, never to the next edit. */
internal class BoardPublicationController(
    private val repository: TierRepository,
    private val community: CommunityRepository,
    private val accounts: AccountRepository,
    private val scope: CoroutineScope,
    private val current: () -> TierList?,
    private val busy: (Boolean, Boolean?) -> Unit,
    private val error: (PublishError?) -> Unit,
    private val wantsCategory: () -> Unit,
    private val takeDown: suspend (TierList) -> Result<Published?>,
    private val recorded: suspend (TierList, Published?) -> Unit,
    private val reload: suspend () -> Unit,
) {
    private val mutable = MutableStateFlow(BoardPublicationState())
    val state = mutable.asStateFlow()
    private var generation = 0
    private var offerJob: Job? = null
    private var actionJob: Job? = null
    private var checked: TierList? = null

    private fun owner(): String? = (accounts.current() as? Account.SignedIn)?.recordKey
    private fun same(left: TierList, right: TierList): Boolean =
        left.copy(rankingChoice = null, editedAt = null, favouritedAt = null) == right.copy(rankingChoice = null, editedAt = null, favouritedAt = null)
    private fun present(list: TierList, who: String?): Boolean = who == owner() && current()?.let { same(it, list) } == true

    fun accountChanged() {
        generation++
        offerJob?.cancel()
        actionJob?.cancel()
        checked = null
        error(null)
        mutable.value = BoardPublicationState()
        busy(false, null)
    }

    fun loaded(list: TierList) {
        if (mutable.value.preview?.let { !same(it.list, list) } == true) dismissPreview()
        if (mutable.value.offer?.let { !same(it.list, list) } == true) dismissOffer()
        if (!canOffer(list)) return
        val total = list.tiers.sumOf { it.items.size }
        val placed = list.tiers.filterNot { it.isPool }.sumOf { it.items.size }
        if (total == 0 || placed < total) return
        offerJob?.cancel()
        val who = owner()
        val ticket = generation
        offerJob = scope.launch {
            runSuspendCatching { offer(list, who, ticket, placed, total) }
                .onFailure { if (active(ticket, who)) checked = null }
        }
    }

    private fun canOffer(list: TierList): Boolean {
        if (list.takenFrom == null || list.isPublic || list.rankingChoice != null) return false
        if (mutable.value.busy || accounts.current() == Account.Unknown) return false
        return checked?.let { same(it, list) } != true
    }

    private fun active(ticket: Int, who: String?): Boolean = ticket == generation && owner() == who

    private suspend fun offer(list: TierList, who: String?, ticket: Int, placed: Int, total: Int) {
        val kind = community.classifyPublication(list).getOrNull() ?: return
        if (!active(ticket, who) || !present(list, who)) return
        checked = list
        if (kind is PublicationKind.Ranking) rememberOffer(list, kind, who, ticket, placed, total)
    }

    private suspend fun rememberOffer(list: TierList, kind: PublicationKind.Ranking, who: String?, ticket: Int, placed: Int, total: Int) {
        if (list.originVersion == null) repository.setRankingOrigin(list.id, kind.version)
        repository.setRankingChoice(list.id, RankingChoice.Offered)
        if (!active(ticket, who)) return
        val saved = repository.getTierListById(list.id) ?: return
        val unchanged = same(saved, list.copy(originVersion = kind.version))
        if (active(ticket, who) && unchanged && present(list, who)) {
            mutable.value = mutable.value.copy(offer = RankingOffer(saved, kind, who, placed, total))
            reload()
        }
    }

    fun setPublic(public: Boolean) {
        val list = current() ?: return
        if (mutable.value.busy) return
        error(null)
        offerJob?.cancel()
        mutable.value = BoardPublicationState()
        if (public && list.tiers.none { it.items.isNotEmpty() }) {
            fail(PublishError.NothingToPublish)
            return
        }
        if (accounts.current() !is Account.SignedIn) {
            fail(PublishError.NotSignedIn)
            return
        }
        if (!public || list.publishedId != null) {
            val who = owner()
            action(pending = public) { perform(list, public, null, who, it) }
        } else {
            review(list)
        }
    }

    private fun review(list: TierList) {
        val who = owner()
        action {
            val result = community.classifyPublication(list).getOrThrow()
            if (active(it, who) && present(list, who)) mutable.value = mutable.value.copy(preview = PublicationPreview(list, result, who))
        }
    }

    fun dismissPreview() {
        generation++
        actionJob?.cancel()
        mutable.value = mutable.value.copy(preview = null, busy = false)
        busy(false, null)
    }
    fun dismissError() {
        error(null)
        mutable.value = mutable.value.copy(error = null)
    }
    fun dismissOffer() {
        mutable.value = mutable.value.copy(offer = null)
    }

    fun shareOffer(onSignIn: () -> Unit) {
        val offer = mutable.value.offer ?: return
        if (mutable.value.busy) return
        action { ticket ->
            if (!present(offer.list, offer.owner)) {
                dismissOffer()
                return@action
            }
            repository.setRankingChoice(offer.list.id, RankingChoice.Ranking)
            if (!active(ticket, offer.owner)) return@action
            dismissOffer()
            if (accounts.current() !is Account.SignedIn) {
                reload()
                if (active(ticket, offer.owner)) onSignIn()
            } else {
                perform(offer.list, true, offer.kind, offer.owner, ticket)
            }
        }
    }

    fun confirmPreview() {
        val preview = mutable.value.preview ?: return
        if (mutable.value.busy) return
        mutable.value = mutable.value.copy(preview = null)
        action { ticket ->
            val fresh = repository.getTierListById(preview.list.id)
            if (!active(ticket, preview.owner)) return@action
            if (fresh == null || !present(preview.list, preview.owner) || !same(fresh, preview.list)) {
                reload()
                if (active(ticket, preview.owner)) current()?.let(::review)
                return@action
            }
            if (preview.kind is PublicationKind.NewList && fresh.category == null) wantsCategory()
            else perform(fresh, true, preview.kind, preview.owner, ticket)
        }
    }

    fun recheckOriginal() {
        val list = current() ?: return
        val sourceId = list.takenFrom ?: return
        if (mutable.value.busy) return
        val who = owner()
        mutable.value = BoardPublicationState()
        action(refusal = PublishError.SourceChanged) { ticket ->
            val original = community.open(sourceId).getOrThrow()
            if (!active(ticket, who) || !present(list, who)) return@action
            if (original.summary.id != sourceId) return@action
            repository.setRankingOrigin(list.id, original.summary.updatedAtMillis)
            if (!active(ticket, who)) return@action
            error(null)
            reload()
            if (active(ticket, who)) current()?.let(::review)
        }
    }

    /** One cancellable operation owns each busy state and its error, even across an account round trip. */
    private fun action(pending: Boolean? = null, refusal: PublishError? = null, block: suspend (Int) -> Unit) {
        val ticket = ++generation
        val who = owner()
        mutable.value = mutable.value.copy(busy = true, error = null)
        busy(true, pending)
        actionJob = scope.launch {
            try {
                if (active(ticket, who)) runSuspendCatching { block(ticket) }.onFailure {
                    if (active(ticket, who)) fail(refusal ?: publishErrorOf(it, pending != false))
                }
            } finally {
                finish(ticket)
            }
        }
    }

    private suspend fun perform(list: TierList, public: Boolean, kind: PublicationKind?, who: String?, ticket: Int) {
        if (!present(list, who)) return
        val fresh = repository.getTierListById(list.id)
        if (!active(ticket, who)) return
        if (fresh != null && same(fresh, list) && present(list, who)) send(list, public, kind, who, ticket)
        else reload()
    }

    private suspend fun send(list: TierList, public: Boolean, kind: PublicationKind?, who: String?, ticket: Int) {
        if (!public && list.rankingOf != null) repository.setRankingChoice(list.id, RankingChoice.Ranking)
        if (!active(ticket, who) || !present(list, who)) return
        val sending = publicationRequest(list, kind)
        val published = (if (public) community.publishOwned(sending, requireNotNull(who)) else takeDown(list)).getOrThrow()
        if (active(ticket, who)) {
            rememberPublication(sending, published, kind, ticket, who)
            if (active(ticket, who)) {
                recorded(sending, published)
                finish(ticket)
                if (active(ticket, who)) reload()
            }
        }
    }

    private suspend fun rememberPublication(list: TierList, published: Published?, kind: PublicationKind?, ticket: Int, who: String?) {
        if (published == null || list.takenFrom == null) return
        val version = when (kind) {
            is PublicationKind.Ranking -> kind.version
            is PublicationKind.NewList -> kind.sourceVersion
            null -> null
        }
        if (version != null) repository.setRankingOrigin(list.id, version)
        if (active(ticket, who)) repository.setRankingChoice(list.id, RankingChoice.Ranking)
    }

    private fun publicationRequest(list: TierList, kind: PublicationKind?): TierList = when (kind) {
        is PublicationKind.Ranking -> list.copy(originVersion = kind.version, category = list.category ?: "other")
        is PublicationKind.NewList -> list.copy(originVersion = kind.sourceVersion ?: list.originVersion)
        null -> list
    }

    private fun finish(ticket: Int) {
        if (ticket == generation) {
            mutable.value = mutable.value.copy(busy = false)
            busy(false, null)
        }
    }
    private fun fail(reason: PublishError) {
        error(reason)
        mutable.value = mutable.value.copy(error = reason)
    }
}

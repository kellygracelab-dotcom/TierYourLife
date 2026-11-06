package com.artiuillab.tieryourlife.feature.community.domain.repository

import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionDirectory
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionLookup
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionProposal
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionStatus
import com.artiuillab.tieryourlife.feature.community.domain.model.Withdrawal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The sections as this phone last heard of them (docs/sections-api.md §6):
 * what was kept is drawn first, so tiles do not jump, and the list is read
 * again when Community opens and the last answer is older than ten minutes.
 * What somebody follows and proposed is theirs: when [person] (who is signed
 * in, by uid) changes, the directory starts over and is read again for them.
 */
class KeptSectionCatalog(
    private val community: CommunityRepository,
    private val store: SectionStore,
    private val clock: CommunityClock,
    private val scope: CoroutineScope,
    person: Flow<String?> = emptyFlow(),
) : SectionCatalog {

    private val state = MutableStateFlow(SectionDirectory())
    override val directory: StateFlow<SectionDirectory> = state.asStateFlow()

    private val reading = Mutex()
    private val asking = mutableSetOf<String>()

    /** An answer that came first wins over what was kept from an older one. */
    private val kept: Job = scope.launch {
        store.read()?.let { found -> state.update { now -> if (now.counted) now else found } }
    }

    init {
        scope.launch {
            person.distinctUntilChanged().drop(1).collect { uid ->
                reading.withLock { state.value = SectionDirectory() }
                if (uid != null) refresh(force = true)
            }
        }
    }

    override suspend fun refresh(force: Boolean): Result<Unit> {
        kept.join()
        return reading.withLock {
            val last = state.value.fetchedAtMillis
            if (!force && last != null && clock.nowMillis() - last < FRESH_FOR_MILLIS) {
                Result.success(Unit)
            } else {
                community.sections().map { answer ->
                    // Ids nobody had heard of may have been approved since: they may be asked about again.
                    state.update { it.copy(sections = answer, unknown = emptySet(), fetchedAtMillis = clock.nowMillis()) }
                    keep()
                }
            }
        }
    }

    override fun lookUp(id: String) {
        if (state.value.settled(id) || !synchronized(asking) { asking.add(id) }) return
        scope.launch {
            kept.join()
            if (!state.value.settled(id)) {
                community.section(id).onSuccess { learn(id, it) }
            }
            // A failure leaves the id unsettled: the next screen that meets it asks again.
            synchronized(asking) { asking.remove(id) }
        }
    }

    private suspend fun learn(id: String, found: SectionLookup) {
        when (found) {
            is SectionLookup.Found -> state.update { now ->
                val renamed = if (found.section.id != id) now.merged + (id to found.section.id) else now.merged
                now.copy(extra = now.extra + (found.section.id to found.section), merged = renamed)
            }

            is SectionLookup.Merged -> state.update { it.copy(merged = it.merged + (id to found.into)) }
            SectionLookup.Unknown -> state.update { it.copy(unknown = it.unknown + id) }
        }
        keep()
        if (found is SectionLookup.Merged) lookUp(found.into)
    }

    override suspend fun setFollowing(id: String, now: Boolean): Result<Unit> {
        val target = state.value.resolve(id)
        val was = state.value.find(target)?.following
        state.update { it.withFollowing(target, now) }
        val result = if (now) community.followSection(target) else community.unfollowSection(target)
        if (result.isSuccess) {
            keep()
        } else if (was != null) {
            state.update { it.withFollowing(target, was) }
        }
        return result
    }

    override suspend fun propose(proposal: SectionProposal): Result<Section> =
        community.proposeSection(proposal).onSuccess { sent ->
            state.update { it.withProposal(sent) }
            keep()
        }

    override suspend fun withdraw(id: String): Result<SectionStatus?> {
        val waited = state.value.find(id)?.waiting == true
        return community.withdrawProposal(id).map { answer ->
            // A notice put away was asked for; a waiting proposal found already decided was not.
            val decided = when (answer) {
                Withdrawal.Withdrawn -> null
                is Withdrawal.Dismissed -> answer.status.takeIf { waited }
                is Withdrawal.Settled -> answer.status
            }
            state.update { if (decided == SectionStatus.Approved) it.withApproved(id) else it.withoutProposal(id) }
            keep()
            if (decided != null) refresh(force = true)
            decided
        }
    }

    private suspend fun keep() = store.write(state.value)

    companion object {
        const val FRESH_FOR_MILLIS: Long = 10 * 60 * 1000L
    }
}

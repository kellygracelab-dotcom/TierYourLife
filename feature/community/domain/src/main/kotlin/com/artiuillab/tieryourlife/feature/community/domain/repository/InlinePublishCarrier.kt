package com.artiuillab.tieryourlife.feature.community.domain.repository

import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishRefused
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Carried for as long as [scope] lives, with no retrying: what a screen does with no background carrier given it. */
class InlinePublishCarrier(
    private val scope: CoroutineScope,
    private val community: CommunityRepository,
    private val record: RecordPublished,
) : PublishCarrier {
    private val mutable = MutableStateFlow(emptyMap<Long, Carrying>())
    override val going: StateFlow<Map<Long, Carrying>> = mutable.asStateFlow()
    private val events = MutableSharedFlow<Carried>(extraBufferCapacity = EVENTS)
    override val ended: SharedFlow<Carried> = events.asSharedFlow()
    private val trips = mutableMapOf<Long, Job>()

    override fun send(request: CarryRequest) {
        val id = request.board.id
        if (id in mutable.value) return
        val update = request.board.publishedId != null
        mutable.update { it + (id to Carrying(update)) }
        trips[id] = scope.launch {
            try {
                events.emit(carry(request, update))
            } finally {
                trips.remove(id)
                mutable.update { it - id }
            }
        }
    }

    private suspend fun carry(request: CarryRequest, update: Boolean): Carried {
        val id = request.board.id
        val published = community.publishOwned(request.sending, request.owner).getOrElse { failure ->
            return Carried.Refused(id, update, (failure as? PublishRefused)?.error ?: PublishError.Unknown)
        }
        // Out all the same if the board could not take it down: the next check of what is published catches it up.
        runCatching { record(request.sending, published, request.originVersion) }
            .exceptionOrNull()
            ?.let { if (it is CancellationException) throw it }
        return Carried.Out(id, request.sending, published)
    }

    override suspend fun stop(boardId: Long) {
        trips[boardId]?.join()
    }

    private companion object {
        const val EVENTS = 8
    }
}

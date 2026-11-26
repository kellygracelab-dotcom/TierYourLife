package com.artiuillab.tieryourlife.feature.community.domain.repository

import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * A tap on Publish or Update, carried on while the board stays open and
 * editable, and after it is left (Designer, approved by Danylo 26.11.2026).
 */
interface PublishCarrier {

    /** Boards on their way out, by board id. */
    val going: StateFlow<Map<Long, Carrying>>

    /** How each one ended, as it ends: a board nobody is looking at says nothing. */
    val ended: Flow<Carried>

    /** [CarryRequest.board] goes out as it is now; returns at once. One at a time per board. */
    fun send(request: CarryRequest)

    /** Nothing more goes out for [boardId]; a request already on its way is waited for, as Take it down must. */
    suspend fun stop(boardId: Long)
}

/**
 * [board] as the tap found it, to go out as it is: an edit after the tap waits
 * for the next one. [originVersion]: the version of the list a copy answers,
 * remembered once it is out. [categoryIfNone]: where a copy with no section goes.
 */
data class CarryRequest(
    val board: TierList,
    val owner: String,
    val originVersion: Long? = null,
    val categoryIfNone: String? = null,
) {
    /** What goes out. */
    val sending: TierList
        get() = board.copy(originVersion = originVersion ?: board.originVersion, category = board.category ?: categoryIfNone)
}

/** [slow]: over a minute, and still going. [offline]: tried and failed for want of a connection, and waiting for one. */
data class Carrying(val update: Boolean, val slow: Boolean = false, val offline: Boolean = false)

sealed interface Carried {
    val boardId: Long

    /** Out, and written onto the board: [sent] is the board as it went. */
    data class Out(override val boardId: Long, val sent: TierList, val published: Published) : Carried

    /** Refused for good: nothing was lost, the board says Update again. */
    data class Refused(override val boardId: Long, val update: Boolean, val reason: PublishError) : Carried
}

package com.artiuillab.tieryourlife.feature.community.domain

import com.artiuillab.tieryourlife.feature.community.domain.model.CommunityPage
import com.artiuillab.tieryourlife.feature.community.domain.repository.CachedFeed
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityClock
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityFeedCache
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityNetwork
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** A cache in memory: what a test puts in is what the screen opens with. */
open class FakeCommunityFeedCache(initial: Map<String, CachedFeed> = emptyMap()) : CommunityFeedCache {
    val kept = initial.toMutableMap()

    override suspend fun read(key: String): CachedFeed? = kept[key]

    override suspend fun write(key: String, page: CommunityPage, fetchedAtMillis: Long) {
        kept[key] = CachedFeed(page, fetchedAtMillis)
    }
}

/** A clock a test moves by hand. */
class FakeCommunityClock(var now: Long = 0L) : CommunityClock {
    override fun nowMillis(): Long = now
}

/** A network a test switches on by hand. */
class FakeCommunityNetwork : CommunityNetwork {
    private val signals = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val available: Flow<Unit> = signals

    fun comeBack() {
        signals.tryEmit(Unit)
    }
}

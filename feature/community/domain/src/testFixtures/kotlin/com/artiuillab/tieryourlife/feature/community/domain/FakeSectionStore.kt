package com.artiuillab.tieryourlife.feature.community.domain

import com.artiuillab.tieryourlife.feature.community.domain.model.SectionDirectory
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityClock
import com.artiuillab.tieryourlife.feature.community.domain.repository.CommunityRepository
import com.artiuillab.tieryourlife.feature.community.domain.repository.KeptSectionCatalog
import com.artiuillab.tieryourlife.feature.community.domain.repository.SectionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** The phone's copy of the sections, in memory: what a test puts in is what screens open with. */
class FakeSectionStore(var kept: SectionDirectory? = null) : SectionStore {
    var writes = 0
        private set

    override suspend fun read(): SectionDirectory? = kept

    override suspend fun write(directory: SectionDirectory) {
        kept = directory
        writes++
    }
}

/**
 * The real catalog over a fake community, working in the caller's thread: a
 * screen's test sees what the phone would, one step at a time.
 */
fun sectionCatalogFor(
    community: CommunityRepository,
    store: SectionStore = FakeSectionStore(),
    clock: CommunityClock = FakeCommunityClock(),
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
): KeptSectionCatalog = KeptSectionCatalog(community, store, clock, scope)

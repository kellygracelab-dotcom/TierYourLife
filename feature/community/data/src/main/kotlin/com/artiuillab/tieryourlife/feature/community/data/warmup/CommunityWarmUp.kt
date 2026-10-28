package com.artiuillab.tieryourlife.feature.community.data.warmup

import com.artiuillab.tieryourlife.feature.community.domain.repository.SectionCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One cheap request when the app starts: the App Check token is fetched and
 * the function woken while nobody is waiting, so Community opens on an answer
 * rather than on a cold start. The request is the sections list, which every
 * screen that names a section draws from.
 */
@Singleton
class CommunityWarmUp @Inject constructor(
    private val sections: SectionCatalog,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        scope.launch {
            sections.refresh().onFailure { Timber.i(it, "Warming the community up failed") }
        }
    }
}

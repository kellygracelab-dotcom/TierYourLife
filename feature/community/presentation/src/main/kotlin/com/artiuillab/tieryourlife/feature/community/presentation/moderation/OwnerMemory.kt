package com.artiuillab.tieryourlife.feature.community.presentation.moderation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What this process has learned about who decides here. The owner is the one
 * account the moderators list answers to; the app knows it is the owner only
 * after that answer, so an author page offers "make moderator" only then.
 * Nothing is stored: the next launch asks again.
 */
@Singleton
class OwnerMemory @Inject constructor() {

    private val _moderators = MutableStateFlow<Set<String>?>(null)

    /** The moderators' uids once the owner has read them; null while nobody has, or the reader is not the owner. */
    val moderators: StateFlow<Set<String>?> = _moderators.asStateFlow()

    fun learned(moderatorUids: Set<String>) {
        _moderators.value = moderatorUids
    }

    fun appointed(uid: String) {
        _moderators.value = _moderators.value.orEmpty() + uid
    }

    fun removed(uid: String) {
        _moderators.value = _moderators.value?.minus(uid)
    }
}

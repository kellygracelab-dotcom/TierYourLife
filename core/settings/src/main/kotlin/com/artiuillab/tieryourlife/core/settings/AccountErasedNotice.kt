package com.artiuillab.tieryourlife.core.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** The account was deleted and this phone signed out of it: said once, until read. */
@Singleton
class AccountErasedNotice @Inject constructor(private val preferences: AppPreferences) {

    enum class Where { ThisDevice, AnotherDevice }

    private val pendingNow = MutableStateFlow(preferences.accountErasedNotice()?.let { stored -> Where.entries.find { it.name == stored } })

    val pending: StateFlow<Where?> = pendingNow.asStateFlow()

    /** One already waiting keeps its words: a leave done again must not change them. */
    fun raise(where: Where) {
        if (pendingNow.value != null) return
        preferences.setAccountErasedNotice(where.name)
        pendingNow.value = where
    }

    fun dismiss() {
        preferences.setAccountErasedNotice(null)
        pendingNow.value = null
    }
}

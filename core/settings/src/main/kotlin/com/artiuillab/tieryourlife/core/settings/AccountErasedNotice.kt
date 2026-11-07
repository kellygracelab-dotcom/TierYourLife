package com.artiuillab.tieryourlife.core.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** The account was deleted on another phone and this one signed out of it: said once, until read. */
@Singleton
class AccountErasedNotice @Inject constructor(private val preferences: AppPreferences) {

    private val pendingNow = MutableStateFlow(preferences.accountErasedNoticePending())

    val pending: StateFlow<Boolean> = pendingNow.asStateFlow()

    fun raise() {
        preferences.setAccountErasedNoticePending(true)
        pendingNow.value = true
    }

    fun dismiss() {
        preferences.setAccountErasedNoticePending(false)
        pendingNow.value = false
    }
}

package com.artiuillab.tieryourlife.feature.account.data.repository

import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A face or name changed on the site shows when the app is back on screen, as the colour does: the profile is
 * read again at most once a [REREAD_AFTER_MS].
 */
@Singleton
class ProfileOnReturn internal constructor(
    private val accounts: AccountRepository,
    private val scope: CoroutineScope,
    private val now: () -> Long,
) {

    @Inject
    constructor(accounts: AccountRepository) : this(accounts, CoroutineScope(SupervisorJob() + Dispatchers.IO), System::currentTimeMillis)

    private val lastRead = AtomicLong(Long.MIN_VALUE / 2)

    fun backOnScreen() {
        val at = now()
        val last = lastRead.get()
        if (at - last < REREAD_AFTER_MS || !lastRead.compareAndSet(last, at)) return
        scope.launch { accounts.refreshProfile() }
    }
}

private const val REREAD_AFTER_MS = 30_000L

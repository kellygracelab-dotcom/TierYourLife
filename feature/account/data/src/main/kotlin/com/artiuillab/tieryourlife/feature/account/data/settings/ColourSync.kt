package com.artiuillab.tieryourlife.feature.account.data.settings

import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.core.settings.ColourChoice
import com.artiuillab.tieryourlife.core.settings.ColourSetting
import com.artiuillab.tieryourlife.feature.account.data.remote.ColourDto
import com.artiuillab.tieryourlife.feature.account.data.remote.ColourWriteDto
import com.artiuillab.tieryourlife.feature.account.data.remote.SettingsApi
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The colour follows the account (CONTRACT.md, boards, Settings). At start and after each sign-in the account's
 * colour is read: taken when it changed later than this device's, else this device's goes up. Each choice made
 * here goes up at once; when the account kept a later one, that one is shown. A guest's stays on the device.
 */
@Singleton
class ColourSync internal constructor(
    private val api: SettingsApi,
    private val colour: ColourChoice,
    private val accounts: AccountRepository,
    private val signIns: Flow<String>,
    private val scope: CoroutineScope,
) {

    @Inject
    constructor(api: SettingsApi, colour: ColourChoice, accounts: AccountRepository) : this(
        api,
        colour,
        accounts,
        accounts.account.filterIsInstance<Account.SignedIn>().map { it.recordKey }.distinctUntilChanged(),
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )

    private val mutex = Mutex()

    fun start() {
        scope.launch { signIns.collect { quietly("Reading the account's colour") { meet() } } }
        scope.launch { colour.chosen.collect { chosen -> quietly("Keeping the colour") { keep(chosen) } } }
    }

    /** Whichever of the account's colour and this device's changed later becomes both. */
    internal suspend fun meet() = mutex.withLock {
        if (accounts.current() !is Account.SignedIn) return@withLock
        val theirs = api.settings().colour?.toSetting()
        val mine = colour.current.value
        when {
            theirs != null && (mine == null || theirs.changedAt > mine.changedAt) -> colour.adopt(theirs)
            mine != null && mine != theirs -> write(mine)
        }
    }

    internal suspend fun keep(chosen: ColourSetting) = mutex.withLock {
        if (accounts.current() is Account.SignedIn) write(chosen)
    }

    private suspend fun write(setting: ColourSetting) {
        val answer = api.keepColour(ColourWriteDto(setting.name, setting.hue, setting.changedAt))
        if (!answer.landed) colour.adopt(answer.toSetting())
    }

    private suspend fun quietly(what: String, block: suspend () -> Unit) {
        runSuspendCatching { block() }.onFailure { e ->
            when (e) {
                // Offline: the next sign-in or start meets again, and the device keeps what it shows.
                is IOException -> Timber.i(e, "$what waits for the network")
                // A guest's 403, an erased account's 401: the colour stays on the device.
                is HttpException -> Timber.w(e, "$what: the account did not take it")
                else -> Timber.w(e, what)
            }
        }
    }
}

private fun ColourDto.toSetting() = ColourSetting(name = name, hue = hue, changedAt = changedAt)

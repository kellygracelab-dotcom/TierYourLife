package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.BoardSyncDao
import com.artiuillab.tieryourlife.feature.tier.data.local.image.TierImageStore
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.BoardsApi
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BackupSettings
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardBackup
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** The switch is the signed-in account's own: off for one says nothing about the next. */
@Singleton
class RoomBoardBackup @Inject constructor(
    private val preferences: AppPreferences,
    private val dao: BoardSyncDao,
    private val images: TierImageStore,
    private val api: BoardsApi,
    private val accounts: AccountRepository,
) : BoardBackup {

    override suspend fun settings(): BackupSettings {
        val bytes = storedBytes()
        preferences.setLastKnownStoredBytes(bytes)
        return settingsNow().copy(storedBytes = bytes)
    }

    override fun settingsNow() = BackupSettings(
        on = signedInAs()?.let(preferences::backUpBoards) ?: false,
        picturesOnWifiOnly = preferences.picturesOnWifiOnly(),
        storedBytes = preferences.lastKnownStoredBytes(),
        lastSyncedAtMs = preferences.lastSyncedAtMs(),
    )

    override fun setPicturesOnWifiOnly(wifiOnly: Boolean) {
        preferences.setPicturesOnWifiOnly(wifiOnly)
    }

    override fun start() {
        signedInAs()?.let { account -> preferences.setBackUpBoards(account, true) }
    }

    /**
     * Off means gone: every board becomes a marker and every picture leaves
     * the sent record. Nothing local is touched, and turning it back on sends
     * everything again: with the records gone, a marker is written over. The
     * switch goes off only once the copy is gone: off stops every sync, so
     * nothing would ever finish a delete left halfway. Only the signed-in
     * account's rows: the rest are other accounts' boards.
     */
    override suspend fun stopAndDelete(): Boolean {
        val account = signedInAs() ?: return false
        return try {
            dao.syncRecordsOf(account).forEach { record ->
                api.forget(record.listUid)
                dao.forget(account, record.listUid)
            }
            dao.forgetPicturesOf(account)
            preferences.setLastSyncedAtMs(null)
            preferences.setBackUpBoards(account, false)
            true
        } catch (e: IOException) {
            Timber.w(e, "Could not delete every kept board; backup stays on")
            false
        } catch (e: HttpException) {
            Timber.w(e, "The account would not delete every kept board; backup stays on")
            false
        }
    }

    private fun signedInAs(): String? = (accounts.current() as? Account.SignedIn)?.recordKey

    /** Measured from the files this phone sent, not asked of Storage: same answer, no connection needed. */
    private suspend fun storedBytes(): Long {
        val account = signedInAs() ?: return 0L
        return dao.sentPictureIds(account).sumOf { pictureId -> images.sizeOf(pictureId) }
    }
}

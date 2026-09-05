package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.BoardSyncDao
import com.artiuillab.tieryourlife.feature.tier.data.local.image.TierImageStore
import com.artiuillab.tieryourlife.feature.tier.data.remote.api.BoardsApi
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BackupSettings
import com.artiuillab.tieryourlife.feature.tier.domain.sync.BoardBackup
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomBoardBackup @Inject constructor(
    private val preferences: AppPreferences,
    private val dao: BoardSyncDao,
    private val images: TierImageStore,
    private val api: BoardsApi,
) : BoardBackup {

    override suspend fun settings() = BackupSettings(
        on = preferences.backUpBoards(),
        picturesOnWifiOnly = preferences.picturesOnWifiOnly(),
        storedBytes = storedBytes(),
        lastSyncedAtMs = preferences.lastSyncedAtMs(),
    )

    override fun setPicturesOnWifiOnly(wifiOnly: Boolean) {
        preferences.setPicturesOnWifiOnly(wifiOnly)
    }

    override fun start() {
        preferences.setBackUpBoards(true)
    }

    /**
     * Off means gone: every board becomes a marker and every picture leaves
     * the sent record, so turning it back on sends everything again. Nothing
     * local is touched.
     */
    override suspend fun stopAndDelete(): Boolean {
        preferences.setBackUpBoards(false)
        return runCatching {
            dao.allSyncRecords().forEach { record ->
                api.forget(record.listUid)
                dao.forget(record.listUid)
            }
            dao.forgetEveryPicture()
            preferences.setLastSyncedAtMs(null)
        }.onFailure { failure ->
            // The switch is already off; what is still up there goes with the next attempt.
            Timber.w(failure, "Could not delete every kept board")
        }.isSuccess
    }

    /** Measured from the files this phone sent, not asked of Storage: same answer, no connection needed. */
    private suspend fun storedBytes(): Long =
        dao.sentPictureIds().sumOf { pictureId -> images.sizeOf(pictureId) }
}

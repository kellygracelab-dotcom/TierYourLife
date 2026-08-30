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
     * Off means gone. Every board is turned into a marker and every picture is
     * dropped from the record of what has been sent, so turning it back on
     * later sends everything again rather than trusting a ledger about files
     * that are no longer there.
     *
     * Nothing local is touched, and that is the whole promise: the boards stay
     * exactly where they were, minus somebody else's computer.
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
            // The switch is already off, so nothing more goes up. What is
            // still up there will be taken down by the next attempt, and the
            // account holds nothing anybody else can see either way.
            Timber.w(failure, "Could not delete every kept board")
        }.isSuccess
    }

    /**
     * Measured from the files this phone sent rather than asked of Storage.
     * The answer is the same, costs nothing, and works with no connection --
     * and a number that only appears when the network is up is worse than a
     * number that is occasionally a few kilobytes stale.
     */
    private suspend fun storedBytes(): Long =
        dao.sentPictureIds().sumOf { pictureId -> images.sizeOf(pictureId) }
}

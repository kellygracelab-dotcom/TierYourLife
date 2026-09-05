package com.artiuillab.tieryourlife.feature.tier.data.sync

import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.tier.data.local.dao.BoardSyncDao
import com.artiuillab.tieryourlife.feature.tier.data.local.entity.PictureSyncEntity
import com.artiuillab.tieryourlife.feature.tier.data.local.image.TierImageStore
import com.artiuillab.tieryourlife.feature.tier.domain.sync.PictureRestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A card's own picture, to the account and back. Neither direction keeps a
 * queue: what still has to go is read off what is already there, so a run
 * that dies halfway leaves nothing to repair.
 */
@Singleton
class PictureSync @Inject constructor(
    private val dao: BoardSyncDao,
    private val images: TierImageStore,
    private val vault: Pictures,
    private val preferences: AppPreferences,
    private val connection: Connection,
) : PictureRestore {

    private val _restoring = MutableStateFlow(PictureRestore.Progress.Idle)
    override val restoring: StateFlow<PictureRestore.Progress> = _restoring.asStateFlow()

    /** A picture that will not go stays unrecorded, so the next run tries again; recording it would lose it quietly. */
    suspend fun push() {
        // Pictures cost data allowance; boards are text and go regardless.
        if (preferences.picturesOnWifiOnly() && !connection.unmetered()) {
            Timber.d("Pictures are waiting for Wi-Fi")
            return
        }
        val sent = dao.sentPictureIds().toSet()
        val here = dao.allImageUrls().mapNotNull(images::pictureIdOf).distinct()

        here.filterNot { it in sent }.forEach { pictureId ->
            val bytes = images.read(pictureId) ?: return@forEach
            if (vault.put(pictureId, bytes)) {
                dao.rememberPicture(PictureSyncEntity(pictureId, System.currentTimeMillis()))
            } else {
                Timber.d("Picture %s did not go up; it will be tried again", pictureId)
            }
        }
    }

    /**
     * Now, whatever the Wi-Fi rule says: publishing is a button somebody
     * pressed, and blank tiles in the feed would be a failure with no visible
     * reason. Answers with what is up.
     */
    suspend fun sendNow(pictureIds: List<String>): Set<String> {
        if (pictureIds.isEmpty()) return emptySet()
        val sent = dao.sentPictureIds().toSet().toMutableSet()

        pictureIds.filterNot { it in sent }.forEach { pictureId ->
            val bytes = images.read(pictureId) ?: return@forEach
            if (vault.put(pictureId, bytes)) {
                dao.rememberPicture(PictureSyncEntity(pictureId, System.currentTimeMillis()))
                sent += pictureId
            } else {
                Timber.d("Picture %s did not go up before publishing", pictureId)
            }
        }
        return pictureIds.filterTo(mutableSetOf()) { it in sent }
    }

    /**
     * What a new phone looks like: boards arrive in one request, pictures take
     * as long as they take, and until then a tile shows its title.
     */
    suspend fun pull(wanted: Map<String, String>) {
        if (preferences.picturesOnWifiOnly() && !connection.unmetered()) {
            _restoring.value = PictureRestore.Progress.Idle
            return
        }
        val missing = wanted.filterValues { pictureId -> !images.holds(pictureId) }
        if (missing.isEmpty()) {
            _restoring.value = PictureRestore.Progress.Idle
            return
        }

        var done = 0
        _restoring.value = PictureRestore.Progress(done = 0, total = missing.size)
        missing.forEach { (itemUid, pictureId) ->
            val bytes = vault.get(pictureId)
            if (bytes != null) {
                dao.setItemImage(itemUid, images.write(pictureId, bytes))
                // Already up there, so it never needs sending back.
                dao.rememberPicture(PictureSyncEntity(pictureId, System.currentTimeMillis()))
            }
            done++
            _restoring.value = PictureRestore.Progress(done = done, total = missing.size)
        }
        _restoring.value = PictureRestore.Progress.Idle
    }
}

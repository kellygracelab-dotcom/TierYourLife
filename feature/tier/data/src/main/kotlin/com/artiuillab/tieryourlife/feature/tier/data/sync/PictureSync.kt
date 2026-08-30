package com.artiuillab.tieryourlife.feature.tier.data.sync

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
 * Getting a card's own picture to the account and back.
 *
 * Neither direction keeps a queue. What still has to go up is every picture on
 * the phone minus the ones already sent; what still has to come down is every
 * picture a board names and the phone does not hold. Both are read off what is
 * already there, so a run that dies halfway leaves nothing to repair -- the
 * next one asks the same question and gets a shorter answer.
 */
@Singleton
class PictureSync @Inject constructor(
    private val dao: BoardSyncDao,
    private val images: TierImageStore,
    private val vault: Pictures,
) : PictureRestore {

    private val _restoring = MutableStateFlow(PictureRestore.Progress.Idle)
    override val restoring: StateFlow<PictureRestore.Progress> = _restoring.asStateFlow()

    /**
     * Sends what the account does not have yet.
     *
     * A picture that will not go stays unrecorded, so the next run tries it
     * again. Recording it anyway would lose it quietly, which is the one
     * outcome this whole thing exists to prevent.
     */
    suspend fun push() {
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
     * Fetches what a board says should be here and is not.
     *
     * The gap is what a new phone looks like: the boards arrive in one request
     * and the pictures take as long as they take. Until each one lands the card
     * shows its title on a plain tile, which is what a card with no picture has
     * always looked like -- not an error, just not finished.
     */
    suspend fun pull(wanted: Map<String, String>) {
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

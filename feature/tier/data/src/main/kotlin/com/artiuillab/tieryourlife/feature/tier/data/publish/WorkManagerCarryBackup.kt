package com.artiuillab.tieryourlife.feature.tier.data.publish

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One piece of work per trip, run only if the trip outlives [WAIT_SECONDS]: by
 * then a trip in a living app has ended and taken it back, so what runs is
 * mostly a trip the app was closed on.
 */
@Singleton
class WorkManagerCarryBackup @Inject constructor(
    @ApplicationContext private val context: Context,
) : CarryBackup {

    private val work by lazy { WorkManager.getInstance(context) }

    override fun keep(note: BackupNote) {
        val request = OneTimeWorkRequestBuilder<CarryWorker>()
            .setInputData(note.toData())
            .setInitialDelay(WAIT_SECONDS, TimeUnit.SECONDS)
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .build()
        // Kept, not replaced: the work that resumed a trip keeps it while it goes.
        work.enqueueUniqueWork(nameOf(note.boardId), ExistingWorkPolicy.KEEP, request)
    }

    override fun drop(boardId: Long) {
        work.cancelUniqueWork(nameOf(boardId))
    }

    companion object {
        const val WAIT_SECONDS = 30L

        fun nameOf(boardId: Long) = "carry-$boardId"
    }
}

/** Finishes a trip the app was closed on, or waits for one still going here. */
class CarryWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val note = inputData.toNote() ?: return Result.success()
        EntryPointAccessors.fromApplication(applicationContext, CarrierEntryPoint::class.java).carrier().resume(note)
        return Result.success()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CarrierEntryPoint {
    fun carrier(): BackgroundPublishCarrier
}

private const val BOARD = "board"
private const val OWNER = "owner"
private const val FINGERPRINT = "fingerprint"
private const val ORIGIN_VERSION = "originVersion"
private const val CATEGORY_IF_NONE = "categoryIfNone"

internal fun BackupNote.toData(): Data = workDataOf(
    BOARD to boardId,
    OWNER to owner,
    FINGERPRINT to fingerprint,
    ORIGIN_VERSION to originVersion,
    CATEGORY_IF_NONE to categoryIfNone,
)

internal fun Data.toNote(): BackupNote? {
    val board = getLong(BOARD, 0L)
    val owner = getString(OWNER)
    val fingerprint = getString(FINGERPRINT)
    return if (board == 0L || owner == null || fingerprint == null) {
        null
    } else {
        BackupNote(board, owner, fingerprint, keyValueMap[ORIGIN_VERSION] as? Long, getString(CATEGORY_IF_NONE))
    }
}

package com.artiuillab.tieryourlife.core.theme.messages

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber

sealed interface UserMessage {
    data object ActionFailed : UserMessage

    /**
     * A published list was left alone rather than deleted, because the copy in
     * the community could not be taken down.
     */
    data object PublishedListStillPublic : UserMessage

    /** Out from the board: first time or [updated] over the same link, as a list or beside one as a [ranking]. */
    data class Published(
        val updated: Boolean,
        val ranking: Boolean = false,
        val droppedPictures: Int = 0,
        /** Out by its link, in the feed only once approved. */
        val awaitingApproval: Boolean = false,
    ) : UserMessage

    /** The list was kept, but it waits for a moderator before anyone else can see it. */
    data object PublishedUnderReview : UserMessage

    /** A moderator reported a list by staff, which only the owner may judge. */
    data object TargetIsStaff : UserMessage

    /** As many reports today as one may send in a day. */
    data object ReportsToday : UserMessage

    /** More own pictures today than the account may send up; they wait on this phone for the next day. */
    data object UploadsToday : UserMessage

    /** Published, but [count] pictures from other sites could not be added. */
    data class PicturesDropped(val count: Int) : UserMessage

    /** An Update over a minute on its way: no error yet, and nothing to wait for. */
    data object StillUpdating : UserMessage

    /** An Update the server turned down: the list is safe here, and Try again sends it once more. */
    data object CouldNotUpdate : UserMessage
}

/** Null when nothing was left out, which is also what an answer without the field means. */
fun picturesDroppedMessage(count: Int): UserMessage? = count.takeIf { it > 0 }?.let { UserMessage.PicturesDropped(it) }

class UserMessages {

    private val channel = Channel<UserMessage>(Channel.BUFFERED)

    val flow: Flow<UserMessage> = channel.receiveAsFlow()

    suspend fun send(message: UserMessage) {
        channel.send(message)
    }
}

suspend fun UserMessages.guard(operation: String, block: suspend () -> Unit): Boolean {
    return try {
        block()
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "$operation failed")
        send(UserMessage.ActionFailed)
        false
    }
}

suspend fun logFailures(operation: String, block: suspend () -> Unit) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "$operation failed")
    }
}

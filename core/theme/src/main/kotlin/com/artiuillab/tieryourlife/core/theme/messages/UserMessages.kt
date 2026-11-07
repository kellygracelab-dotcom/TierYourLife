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

    /** The copy went out as this person's ranking of the list it was taken from, not as a second list. */
    data object PublishedAsRanking : UserMessage

    /** The list was kept, but it waits for a moderator before anyone else can see it. */
    data object PublishedUnderReview : UserMessage

    /** A moderator reported a list by staff, which only the owner may judge. */
    data object TargetIsStaff : UserMessage

    /** Published, but [count] pictures from other sites could not be added. */
    data class PicturesDropped(val count: Int) : UserMessage
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

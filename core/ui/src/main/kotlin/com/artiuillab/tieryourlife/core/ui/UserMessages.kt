package com.artiuillab.tieryourlife.core.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber

enum class UserMessage {
    ActionFailed,
}

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

package com.artiuillab.tieryourlife.feature.community.presentation.follow

import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/** What a guest tapped Follow on. */
sealed interface FollowTarget {
    data class Author(val uid: String) : FollowTarget

    data class Section(val id: String) : FollowTarget

    data class Topic(val id: String) : FollowTarget
}

/** Signing in for longer than this, and the follow asked for before it is forgotten. */
const val FOLLOW_AFTER_SIGN_IN_MILLIS = 10 * 60 * 1000L

/**
 * The follow a guest asked for, done once they have signed in, as the site does.
 * One at a time and in memory only: a sign-in that outlives the app is not the
 * same errand any more.
 */
@Singleton
class FollowAfterSignIn internal constructor(
    private val accounts: AccountRepository,
    private val now: () -> Long,
) {
    @Inject constructor(accounts: AccountRepository) : this(accounts, System::currentTimeMillis)

    private data class Waiting(val target: FollowTarget, val atMillis: Long)

    private val waiting = AtomicReference<Waiting?>(null)

    /** True when [target] can be followed now; for a guest it waits for the sign-in instead. */
    fun followNowOrWait(target: FollowTarget): Boolean {
        if (accounts.current() !is Account.Guest) return true
        waiting.set(Waiting(target, now()))
        return false
    }

    /** The waiting target, once, when a sign-in finds one [accepts] takes and it waited no longer than allowed. */
    fun signedIn(accepts: (FollowTarget) -> Boolean): Flow<FollowTarget> =
        accounts.account.filterIsInstance<Account.SignedIn>().mapNotNull { take(accepts) }

    private fun take(accepts: (FollowTarget) -> Boolean): FollowTarget? {
        val found = waiting.get()?.takeIf { accepts(it.target) } ?: return null
        if (!waiting.compareAndSet(found, null)) return null
        return found.target.takeIf { now() - found.atMillis <= FOLLOW_AFTER_SIGN_IN_MILLIS }
    }
}

/** One screen's follow buttons as a guest meets them: a tap asks for a sign-in, and the sign-in follows. */
internal class GuestFollows(private val afterSignIn: FollowAfterSignIn, private val scope: CoroutineScope) {

    private val asks = Channel<Unit>(Channel.CONFLATED)

    /** The screen opens the sign-in for each. */
    val signInRequests: Flow<Unit> = asks.receiveAsFlow()

    /** False when a guest tapped: [target] waits, and the screen is asked to sign in. */
    fun mayFollow(target: FollowTarget): Boolean {
        if (afterSignIn.followNowOrWait(target)) return true
        asks.trySend(Unit)
        return false
    }

    /** [follow] is what a sign-in does about a waiting target this screen [accepts]. */
    fun afterSignIn(accepts: (FollowTarget) -> Boolean, follow: (FollowTarget) -> Unit) {
        scope.launch { afterSignIn.signedIn(accepts).collect(follow) }
    }
}

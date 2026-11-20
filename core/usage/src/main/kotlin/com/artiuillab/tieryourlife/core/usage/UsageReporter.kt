package com.artiuillab.tieryourlife.core.usage

import android.content.Context
import android.content.res.Resources
import com.artiuillab.tieryourlife.core.logging.runSuspendCatching
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The app's version as the statistics name it: the build number, the one Crashlytics and Play show.
 * [fromPlay]: only the build Play ships counts. Debug, fast and test runs are ours and would count us.
 */
data class UsageInstall(val version: String, val fromPlay: Boolean)

/**
 * Keeps the counts on the phone and sends them in one batch a session, when
 * the app goes to the background, and what was left at the next start.
 */
@Singleton
class UsageReporter internal constructor(
    private val store: LedgerStore,
    private val preferences: AppPreferences,
    private val api: UsageApi,
    private val install: UsageInstall,
    private val today: () -> LocalDate,
    private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) : Usage {

    @Inject
    internal constructor(
        @ApplicationContext context: Context,
        preferences: AppPreferences,
        api: UsageApi,
        install: UsageInstall,
    ) : this(
        store = LedgerStore(context.getSharedPreferences(LedgerStore.FILE, Context.MODE_PRIVATE), firstOf(context) { install.fromPlay && preferences.sendUsage() }),
        preferences = preferences,
        api = api,
        install = install,
        today = { LocalDate.now() },
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )

    private val sending = Mutex()
    private var inSession = false
    private var endedAt: Long? = null

    private fun on() = install.fromPlay && preferences.sendUsage()

    override fun step(step: UsageStep) {
        val on = on()
        store.update { it.stepped(step, on) }
    }

    override fun count(feature: UsageFeature) {
        if (on()) store.update { it.counted(feature.wire) }
    }

    /** At the app's start: counts a crash from now on, and sends what the last session left. */
    fun start() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { crashed(error) }
            previous?.uncaughtException(thread, error)
        }
        send()
    }

    /**
     * The app came to the front. A screen turned or unfolded is the same
     * session, and so is a return within half an hour: from the share sheet,
     * the photo picker or a glance at another app.
     */
    fun sessionStarted() {
        if (inSession) return
        inSession = true
        val back = endedAt?.let { now() - it < SESSION_GAP_MS } == true
        if (back || !on()) return
        store.update { it.counted(UsageFeature.Session.wire).copy(sessions = it.sessions + 1) }
    }

    fun sessionEnded() {
        inSession = false
        endedAt = now()
        send()
    }

    internal fun crashed(error: Throwable) {
        if (!on()) return
        val crash = crashOf(error)
        // One that fell before any screen showed is a session too, and the shortest.
        val opened = !inSession
        store.update(now = true) { ledger ->
            val counted = if (opened) ledger.counted(UsageFeature.Session.wire).copy(sessions = ledger.sessions + 1) else ledger
            counted.copy(crashedSessions = counted.crashedSessions + 1, crashes = (counted.crashes + crash).takeLast(MAX_CRASHES))
        }
        // The process is going: the next start sends it, as a session that ended in a crash.
        inSession = false
    }

    internal fun send() = scope.launch { sending.withLock { sendNow() } }

    internal suspend fun sendNow() {
        if (!on()) return store.update { it.dropped() }
        val day = today()
        if (store.quietOn() == day.toEpochDay()) return
        val batch = batchOf(store.read(), day, install.version, localeTagOf(preferences)) ?: return
        val answer = runSuspendCatching { api.send(batch) }.getOrElse {
            // Offline or the server away: the counts wait for the next session.
            Timber.i("Usage statistics wait: %s", it.javaClass.simpleName)
            return
        }
        when {
            answer.isSuccessful -> store.update { it.after(batch, day) }
            answer.code() == HTTP_TOO_MANY -> store.quietUntilTomorrow(day.toEpochDay())
            // Refused as it is: sending it again would be refused again.
            answer.code() == HTTP_INVALID -> store.update { it.after(batch, day) }
            else -> Timber.i("Usage statistics wait: HTTP %d", answer.code())
        }
    }

    private companion object {
        const val HTTP_INVALID = 400
        const val HTTP_TOO_MANY = 429
        const val SESSION_GAP_MS = 30 * 60 * 1000L

        /**
         * The first launch is the install's: an install from before the
         * statistics already took its first steps, so none is sent for it.
         */
        fun firstOf(context: Context, on: () -> Boolean): () -> Ledger = {
            val info = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
            val installedAt = info?.firstInstallTime ?: System.currentTimeMillis()
            val fresh = info == null || info.firstInstallTime == info.lastUpdateTime
            val firstDay = Instant.ofEpochMilli(installedAt).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
            when {
                !fresh -> Ledger(firstDay = firstDay, stepsDone = UsageStep.entries.mapTo(mutableSetOf()) { it.wire })
                else -> Ledger(firstDay = firstDay).stepped(UsageStep.FirstLaunch, on())
            }
        }
    }
}

/** The language the app is read in and the region set on the phone; never from the address. */
internal fun localeTagOf(preferences: AppPreferences): String? {
    val system = Resources.getSystem().configuration.locales[0]
    val language = preferences.languageTag()?.substringBefore('-')?.takeIf { it.isNotBlank() } ?: system.language
    if (language.isNullOrBlank()) return null
    return if (system.country.isNullOrBlank()) language else "$language-${system.country}"
}

/** The root cause by its type and the top line of its stack: no message, nothing the person typed. */
internal fun crashOf(error: Throwable): UsageCrash {
    val root = generateSequence(error) { it.cause }.take(MAX_CAUSES).last()
    val top = root.stackTrace.firstOrNull()
    val place = top?.let { frame ->
        val file = frame.fileName?.takeUnless { it == "SourceFile" || it == "Unknown Source" }
        if (file != null) "$file:${frame.lineNumber} ${frame.methodName}" else "${frame.className}.${frame.methodName}"
    } ?: "unknown"
    return UsageCrash(type = root.javaClass.name.takeLast(MAX_TYPE), place = place.take(MAX_PLACE))
}

private const val MAX_CAUSES = 8
private const val MAX_TYPE = 80
private const val MAX_PLACE = 120

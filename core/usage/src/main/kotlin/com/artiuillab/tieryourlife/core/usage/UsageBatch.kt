package com.artiuillab.tieryourlife.core.usage

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.util.Locale

@Serializable
internal data class UsageBatch(
    val version: String,
    val locale: String? = null,
    val active: Active? = null,
    val steps: List<String> = emptyList(),
    val features: Map<String, Int> = emptyMap(),
    val sessions: Int = 0,
    val crashedSessions: Int = 0,
    val crashes: List<UsageCrash> = emptyList(),
) {
    @Serializable
    data class Active(val gap: Int, val sinceFirst: Int, val firstWeek: String)
}

@Serializable
internal data class UsageCrash(val type: String, val place: String)

/** What the phone keeps between batches; days are its own local days, as epoch days. */
@Serializable
internal data class Ledger(
    val firstDay: Long,
    val lastActiveDay: Long? = null,
    /** Sent, or passed while statistics were off: never sent again. */
    val stepsDone: Set<String> = emptySet(),
    val stepsWaiting: Set<String> = emptySet(),
    val counts: Map<String, Int> = emptyMap(),
    val sessions: Int = 0,
    val crashedSessions: Int = 0,
    val crashes: List<UsageCrash> = emptyList(),
)

/** The batch for [ledger] on [today], or null when there is nothing to say. */
internal fun batchOf(ledger: Ledger, today: LocalDate, version: String, locale: String?): UsageBatch? {
    val day = today.toEpochDay()
    val active = if (ledger.lastActiveDay == day) {
        null
    } else {
        UsageBatch.Active(
            gap = ledger.lastActiveDay?.let { last -> (day - last).takeIf { it in 1..MAX_GAP } }?.toInt() ?: FIRST_OR_LONG_GAP,
            sinceFirst = (day - ledger.firstDay).coerceAtLeast(0).toInt(),
            firstWeek = isoWeekOf(LocalDate.ofEpochDay(ledger.firstDay)),
        )
    }
    val features = ledger.counts.filterValues { it > 0 }.mapValues { (_, n) -> n.coerceAtMost(MAX_COUNT) }
    val nothing = active == null && ledger.stepsWaiting.isEmpty() && features.isEmpty() && ledger.sessions == 0 && ledger.crashes.isEmpty()
    if (nothing) return null
    return UsageBatch(
        version = version,
        locale = locale,
        active = active,
        steps = UsageStep.entries.map { it.wire }.filter { it in ledger.stepsWaiting },
        features = features,
        sessions = ledger.sessions.coerceAtMost(MAX_COUNT),
        crashedSessions = ledger.crashedSessions.coerceAtMost(ledger.sessions).coerceAtMost(MAX_COUNT),
        crashes = ledger.crashes.take(MAX_CRASHES),
    )
}

/** What is left once [sent] went through: anything recorded meanwhile, or past what one batch carries, stays for the next. */
internal fun Ledger.after(sent: UsageBatch, today: LocalDate): Ledger = copy(
    lastActiveDay = if (sent.active != null) today.toEpochDay() else lastActiveDay,
    stepsDone = stepsDone + sent.steps,
    stepsWaiting = stepsWaiting - sent.steps.toSet(),
    counts = counts.mapValues { (name, n) -> n - (sent.features[name] ?: 0) }.filterValues { it > 0 },
    sessions = (sessions - sent.sessions).coerceAtLeast(0),
    crashedSessions = (crashedSessions - sent.crashedSessions).coerceAtLeast(0),
    crashes = crashes.drop(sent.crashes.size),
)

internal fun isoWeekOf(day: LocalDate): String =
    String.format(Locale.ROOT, "%d-W%02d", day.get(IsoFields.WEEK_BASED_YEAR), day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))

/** Statistics turned off: what waited is not sent, and the steps it held are spent. */
internal fun Ledger.dropped(): Ledger = Ledger(firstDay = firstDay, lastActiveDay = lastActiveDay, stepsDone = stepsDone + stepsWaiting)

internal fun Ledger.stepped(step: UsageStep, on: Boolean): Ledger = when {
    step.wire in stepsDone || step.wire in stepsWaiting -> this
    on -> copy(stepsWaiting = stepsWaiting + step.wire)
    else -> copy(stepsDone = stepsDone + step.wire)
}

/** Counts since the last batch are kept per name, so one recorded meanwhile is a second count of it. */
internal fun Ledger.counted(name: String): Ledger = copy(counts = counts + (name to (counts[name] ?: 0) + 1))

internal const val MAX_GAP = 30L
internal const val FIRST_OR_LONG_GAP = 31
internal const val MAX_COUNT = 200
internal const val MAX_CRASHES = 5

package com.artiuillab.tieryourlife.feature.tier.presentation.trash

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.artiuillab.tieryourlife.feature.tier.presentation.R

private const val MINUTE_MILLIS = 60_000L
private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
private const val DAY_MILLIS = 24 * HOUR_MILLIS

// Relative and coarse — "2 h ago", "yesterday", "4 days ago" — because this is
// provenance, not a deadline, and nothing in the app acts on it
// (docs/design-spec-home.md, section 6).
//
// The bucket is a value rather than a string so the arithmetic stays pure and testable
// off-device (RelativeTimeTest in this module's src/test) while the words stay in
// strings.xml with every other piece of user-visible text. The design's strings table
// has no key for these, but it gave them as example wording rather than as a decision
// to leave them untranslated — and this screen is otherwise fully resourced, so a
// hard-coded English string here is precisely what a localization pass would miss.
internal sealed interface RelativeTime {
    data object JustNow : RelativeTime
    data class Minutes(val count: Int) : RelativeTime
    data class Hours(val count: Int) : RelativeTime
    data object Yesterday : RelativeTime
    data class Days(val count: Int) : RelativeTime
}

internal fun relativeTimeFrom(
    deletedAtMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
): RelativeTime {
    // A row deleted "in the future" — a clock change between the delete and this read —
    // reads as just now rather than as a negative count.
    val elapsed = (nowMillis - deletedAtMillis).coerceAtLeast(0)
    return when {
        elapsed < MINUTE_MILLIS -> RelativeTime.JustNow
        elapsed < HOUR_MILLIS -> RelativeTime.Minutes((elapsed / MINUTE_MILLIS).toInt())
        elapsed < DAY_MILLIS -> RelativeTime.Hours((elapsed / HOUR_MILLIS).toInt())
        elapsed < 2 * DAY_MILLIS -> RelativeTime.Yesterday
        else -> RelativeTime.Days((elapsed / DAY_MILLIS).toInt())
    }
}

@Composable
internal fun relativeTimeText(deletedAtMillis: Long): String =
    when (val time = relativeTimeFrom(deletedAtMillis)) {
        RelativeTime.JustNow -> stringResource(R.string.trash_time_just_now)
        is RelativeTime.Minutes ->
            pluralStringResource(R.plurals.trash_time_minutes_ago, time.count, time.count)

        is RelativeTime.Hours ->
            pluralStringResource(R.plurals.trash_time_hours_ago, time.count, time.count)

        RelativeTime.Yesterday -> stringResource(R.string.trash_time_yesterday)
        is RelativeTime.Days ->
            pluralStringResource(R.plurals.trash_time_days_ago, time.count, time.count)
    }

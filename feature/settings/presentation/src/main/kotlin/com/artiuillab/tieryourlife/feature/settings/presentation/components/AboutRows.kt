package com.artiuillab.tieryourlife.feature.settings.presentation.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.artiuillab.tieryourlife.core.settings.AppLinks
import com.artiuillab.tieryourlife.core.theme.ui.ShareIcon
import com.artiuillab.tieryourlife.core.theme.ui.StarIcon
import com.artiuillab.tieryourlife.feature.settings.presentation.LeadsTo
import com.artiuillab.tieryourlife.feature.settings.presentation.R
import com.artiuillab.tieryourlife.feature.settings.presentation.SettingsRow
import com.artiuillab.tieryourlife.feature.settings.presentation.SettingsTestTags
import timber.log.Timber

/**
 * How it works, then rate, share, write to us: what the testers asked for, each by the way Android
 * already knows.
 */
@Composable
internal fun AboutRows(onHowItWorks: () -> Unit, onRate: () -> Unit, onShare: () -> Unit, onFeedback: () -> Unit) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    SettingsRow(
        icon = { HelpIcon(24.dp, tint) },
        title = stringResource(R.string.settings_how_it_works),
        subtitle = stringResource(R.string.settings_how_it_works_sub),
        onClick = onHowItWorks,
        testTag = SettingsTestTags.HOW_IT_WORKS_ROW,
    )
    SettingsGroupDivider()
    SettingsRow(
        icon = { StarIcon(on = false, size = 24.dp, colorOverride = tint) },
        title = stringResource(R.string.settings_rate),
        subtitle = stringResource(R.string.settings_rate_sub),
        onClick = onRate,
        testTag = SettingsTestTags.RATE_ROW,
        leadsTo = LeadsTo.Outside,
    )
    SettingsGroupDivider()
    SettingsRow(
        icon = { ShareIcon(24.dp, tint) },
        title = stringResource(R.string.settings_share),
        subtitle = stringResource(R.string.settings_share_sub),
        onClick = onShare,
        testTag = SettingsTestTags.SHARE_ROW,
        leadsTo = LeadsTo.Nowhere,
    )
    SettingsGroupDivider()
    SettingsRow(
        icon = { MailIcon(24.dp, tint) },
        title = stringResource(R.string.settings_feedback),
        subtitle = stringResource(R.string.settings_feedback_sub),
        onClick = onFeedback,
        testTag = SettingsTestTags.FEEDBACK_ROW,
        leadsTo = LeadsTo.Outside,
    )
}

/**
 * The app's page in Play, in the Play app where there is one and else in the browser. A tap on a
 * button goes straight there: Play's own review sheet keeps a quota and may show nothing, which a
 * button must not do.
 */
internal fun rateTheApp(context: Context) {
    val opened = open(context, Intent(Intent.ACTION_VIEW, AppLinks.PLAY_APP.toUri()).setPackage(PLAY_STORE)) ||
        open(context, Intent(Intent.ACTION_VIEW, AppLinks.PLAY_PAGE.toUri()))
    if (!opened) Timber.w("Nothing on this phone opens the app's page in Play")
}

/** Until the app is in production its Play page is not open to all, so the line carries the site only. */
internal fun shareTheApp(context: Context) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_app_text, AppLinks.SITE))
    open(context, Intent.createChooser(send, context.getString(R.string.share_app_chooser)))
}

/** False when no app here sends mail: the screen then says the address instead. */
internal fun writeToUs(context: Context, version: String): Boolean {
    val mail = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
        .putExtra(Intent.EXTRA_EMAIL, arrayOf(AppLinks.FEEDBACK_EMAIL))
        .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.feedback_subject, version, androidVersion()))
    return open(context, mail)
}

/** "Android 14 (API 34)": what the device is, and nothing about the person. */
internal fun androidVersion(release: String = Build.VERSION.RELEASE, sdk: Int = Build.VERSION.SDK_INT): String =
    "$release (API $sdk)"

/** "1.0 (575)": the name and the build, as Play counts them. */
internal fun appVersion(context: Context): String = runCatching {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
    "${info.versionName} ($code)"
}.getOrDefault("")

private fun open(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: ActivityNotFoundException) {
    false
}

private const val PLAY_STORE = "com.android.vending"

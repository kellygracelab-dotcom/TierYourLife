package com.artiuillab.tieryourlife.feature.tier.board.share

import android.content.ClipData
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.artiuillab.tieryourlife.core.usage.LocalUsage
import com.artiuillab.tieryourlife.core.usage.UsageFeature
import com.artiuillab.tieryourlife.core.usage.shared
import com.artiuillab.tieryourlife.feature.tier.board.R
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import kotlinx.coroutines.launch
import timber.log.Timber

/** What a shared link says above itself: the top tier's label, or none, and up to three named cards. */
internal data class ShareHook(val tier: String?, val names: List<String>)

/**
 * The cards people argue about: the top tier's named ones, three at most;
 * failing those, the first named cards ranked anywhere. A picture without a
 * name says nothing in a message, so it is skipped. Null when no ranked card
 * has a name.
 */
internal fun shareHookOf(list: TierList): ShareHook? {
    val ranked = list.tiers.filterNot { it.isPool }
    val top = ranked.firstOrNull { it.items.isNotEmpty() } ?: return null
    val topNames = top.items.map { it.title.trim() }.filter { it.isNotEmpty() }.take(HOOK_NAMES)
    if (topNames.isNotEmpty() && top.label.isNotBlank()) return ShareHook(top.label.trim(), topNames)
    val anyNames = ranked.flatMap { it.items }.map { it.title.trim() }.filter { it.isNotEmpty() }.take(HOOK_NAMES)
    return anyNames.takeIf { it.isNotEmpty() }?.let { ShareHook(null, it) }
}

private const val HOOK_NAMES = 3

/** "My S tier: Hades, Celeste, Inside. What's yours?" and the link under it; the title in its place when no card has a name. */
fun shareLinkText(resources: Resources, list: TierList, link: String): String {
    val hook = shareHookOf(list)?.let { hook ->
        val names = hook.names.joinToString(", ")
        if (hook.tier != null) {
            resources.getString(R.string.share_hook_tier, hook.tier, names)
        } else {
            resources.getString(R.string.share_hook_top, names)
        }
    } ?: list.title
    return "$hook\n$link"
}

/**
 * The system's share sheet with the hook line and the published list's link:
 * one tap from the board, its page, or the moment it goes live. Nothing
 * happens for a board that is not published.
 */
@Composable
fun rememberShareLink(): (TierList) -> Unit {
    val context = LocalContext.current
    val resources = LocalResources.current
    val usage = LocalUsage.current
    return share@{ list ->
        val id = list.publishedId ?: return@share
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareLinkText(resources, list, publishedListLink(id)))
        }
        runCatching { context.startActivity(Intent.createChooser(send, null)) }
            .onSuccess { usage.shared(UsageFeature.ShareLink) }
            .onFailure {
                Timber.w(it, "Nothing to share the link with")
                Toast.makeText(context, resources.getString(R.string.share_failed), Toast.LENGTH_SHORT).show()
            }
    }
}

/** The published list's link on the clipboard; Android 13 and later say so themselves, earlier ones get a toast. */
@Composable
fun rememberCopyListLink(): (publishedId: String) -> Unit {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val usage = LocalUsage.current
    return { id ->
        scope.launch {
            val link = publishedListLink(id)
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(link, link)))
            usage.shared(UsageFeature.ShareLink)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(context, resources.getString(R.string.share_link_copied), Toast.LENGTH_SHORT).show()
            }
        }
    }
}

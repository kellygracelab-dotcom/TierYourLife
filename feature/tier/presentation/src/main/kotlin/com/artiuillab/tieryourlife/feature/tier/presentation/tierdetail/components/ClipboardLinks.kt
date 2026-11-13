package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.textclassifier.TextClassifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import com.artiuillab.tieryourlife.feature.tier.domain.repository.linkIn

/**
 * Whether the clipboard may hold an address, told from its description
 * alone: Android says so to the person when an app reads the text, so it is
 * read only when they tap the chip.
 */
@Stable
internal class ClipboardLinks(private val manager: ClipboardManager?) {
    var offered by mutableStateOf(false)
        private set

    fun look() {
        offered = runCatching { manager?.primaryClipDescription?.mayHoldLink() == true }.getOrDefault(false)
    }

    /** The address on the clipboard, or whatever text is there to search for. */
    fun paste(): String? {
        val text = runCatching { manager?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString() }.getOrNull()
        offered = false
        return text?.let { linkIn(it) ?: it.trim().takeIf { typed -> typed.isNotEmpty() } }
    }
}

private fun ClipDescription.mayHoldLink(): Boolean {
    val text = hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
        hasMimeType(ClipDescription.MIMETYPE_TEXT_URILIST) ||
        hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
    if (!text) return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && classificationStatus == ClipDescription.CLASSIFICATION_COMPLETE) {
        return getConfidenceScore(TextClassifier.TYPE_URL) > 0f
    }
    return true
}

/** Looked at again whenever the window has focus: Android tells an app about its clipboard only then. */
@Composable
internal fun rememberClipboardLinks(): ClipboardLinks {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager }
    val links = remember(manager) { ClipboardLinks(manager) }
    val focused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(focused) { if (focused) links.look() }
    DisposableEffect(manager) {
        val listener = ClipboardManager.OnPrimaryClipChangedListener { links.look() }
        manager?.addPrimaryClipChangedListener(listener)
        onDispose { manager?.removePrimaryClipChangedListener(listener) }
    }
    return links
}

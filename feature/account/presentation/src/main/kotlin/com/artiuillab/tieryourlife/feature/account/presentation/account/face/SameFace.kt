package com.artiuillab.tieryourlife.feature.account.presentation.account.face

import java.net.URLDecoder

/**
 * One picture however its address is written, told apart by the Storage object it names (QA D16, D18; the site
 * after D13). The slot comes as Storage's own address (`…/o/avatars%2Fuid%2Fid?alt=media&token=…`), while the
 * community's answers, the face an upload made among them, give the site's public one (`…/img/original/avatars/uid/id`).
 */
internal fun sameFace(a: String?, b: String?): Boolean = a == b || (a != null && b != null && objectOf(a) == objectOf(b))

private fun objectOf(address: String): String {
    val bare = address.substringBefore('?').substringBefore('#')
    val decoded = runCatching { URLDecoder.decode(bare, Charsets.UTF_8.name()) }.getOrDefault(bare)
    return when {
        decoded.startsWith(STORAGE) && STORAGE_OBJECT in decoded -> decoded.substringAfter(STORAGE_OBJECT)
        PUBLIC_ORIGINAL in decoded -> decoded.substringAfter(PUBLIC_ORIGINAL)
        else -> decoded
    }
}

private const val STORAGE = "https://firebasestorage.googleapis.com/v0/b/"
private const val STORAGE_OBJECT = "/o/"
private const val PUBLIC_ORIGINAL = "/img/original/"

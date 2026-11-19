package com.artiuillab.tieryourlife.feature.account.presentation.account.face

import java.net.URLDecoder

/**
 * One picture however its address is written: the profile and the account's slot hand out the same Storage file
 * under other tokens and encodings, so faces are told apart by the file's path (QA D16, as the site after D13).
 */
internal fun sameFace(a: String?, b: String?): Boolean = a == b || (a != null && b != null && pathOf(a) == pathOf(b))

private fun pathOf(address: String): String =
    runCatching { URLDecoder.decode(address.substringBefore('?'), Charsets.UTF_8.name()) }.getOrDefault(address.substringBefore('?'))

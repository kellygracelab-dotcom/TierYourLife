package com.artiuillab.tieryourlife.core.network

/** The one place that knows where the proxy lives. Blank in a checkout with no proxy configured. */
object ProxyAddress {

    val isConfigured: Boolean get() = BuildConfig.PROXY_BASE_URL.isNotBlank()

    /** Every call to the placeholder fails as unreachable, which is what a checkout without a proxy should see. */
    fun resolve(path: String = ""): String =
        BuildConfig.PROXY_BASE_URL.ifBlank { PLACEHOLDER_BASE_URL } + path

    private const val PLACEHOLDER_BASE_URL = "https://example.invalid/"
}

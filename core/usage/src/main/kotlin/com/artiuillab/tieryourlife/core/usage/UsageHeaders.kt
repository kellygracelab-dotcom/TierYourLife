package com.artiuillab.tieryourlife.core.usage

import com.artiuillab.tieryourlife.core.network.ProxyHeaders
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import javax.inject.Inject

/**
 * The version, language and region on every call to the proxy, which counts
 * what it answers by them. Statistics off, none of the three is sent.
 */
class UsageHeaders internal constructor(
    private val preferences: AppPreferences,
    private val install: UsageInstall,
    private val locale: () -> String?,
) : ProxyHeaders {

    @Inject
    constructor(preferences: AppPreferences, install: UsageInstall) : this(preferences, install, { localeTagOf(preferences) })

    override fun headers(): Map<String, String> {
        if (!preferences.sendUsage()) return emptyMap()
        return buildMap {
            put(CLIENT, "android/${install.version}")
            locale()?.let { put(LOCALE, it) }
        }
    }

    internal companion object {
        const val CLIENT = "X-TYL-Client"
        const val LOCALE = "X-TYL-Locale"
    }
}

package com.artiuillab.tieryourlife.feature.tier.data.sync

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What to call this phone when a board arrives from it.
 *
 * The name is never the main thing telling two copies of a board apart -- the
 * times do that, "ten minutes ago here" against "yesterday there". It only
 * helps when it happens to be human, which is why an unreadable one is
 * dropped rather than shown.
 */
@Singleton
class DeviceName @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * The name someone gave the phone themselves if there is one, the model if
     * it reads like a name, and otherwise nothing.
     */
    fun current(): String? = chosenName() ?: Build.MODEL?.takeIf(::readable)

    private fun chosenName(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return null
        return runCatching { Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME) }
            .getOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private companion object {

        /**
         * "Pixel 7" is a name; "SM-F741B" is a part number, and showing it
         * would be worse than saying nothing. One space and no run of letters
         * glued to digits separates the two, which is cheap and covers almost
         * everything. A table of marketing names would be out of date by the
         * next phone.
         */
        fun readable(model: String): Boolean {
            val trimmed = model.trim()
            if (trimmed.isEmpty()) return false
            return trimmed.contains(' ') || trimmed.none(Char::isDigit)
        }
    }
}

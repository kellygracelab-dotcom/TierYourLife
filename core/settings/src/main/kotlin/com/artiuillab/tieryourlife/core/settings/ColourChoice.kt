package com.artiuillab.tieryourlife.core.settings

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The colour the app is drawn in, as the account keeps it (Proxy CONTRACT.md, boards, Settings): a [name]
 * (lavender, matcha, peach, sky, phone, custom, or one added later), the [hue] the last own colour had, and
 * when the person chose it here or elsewhere.
 */
data class ColourSetting(val name: String, val hue: Int, val changedAt: Long)

/**
 * This device's colour: what the person chose, and what the account said when that was later. Everything
 * that draws reads [current]; the account's sync hears [chosen], each choice made here.
 */
@Singleton
class ColourChoice internal constructor(
    private val preferences: AppPreferences,
    private val now: () -> Long,
) {

    @Inject
    constructor(preferences: AppPreferences) : this(preferences, System::currentTimeMillis)

    private val state = MutableStateFlow(preferences.colourSetting())
    val current: StateFlow<ColourSetting?> = state.asStateFlow()

    private val choices = MutableSharedFlow<ColourSetting>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val chosen: Flow<ColourSetting> = choices

    /** The hue an own colour starts from: the last one, or lavender's. */
    val lastHue: Int get() = state.value?.hue ?: LAVENDER_HUE

    /** A colour named here; the hue is kept with every name, so one's own colour comes back as it was. */
    fun choose(name: String, hue: Int = lastHue) {
        val setting = ColourSetting(name, hue, now())
        keep(setting)
        choices.tryEmit(setting)
    }

    /** The account's colour, when it changed later than this device's. */
    fun adopt(setting: ColourSetting) {
        if (setting != state.value) keep(setting)
    }

    private fun keep(setting: ColourSetting) {
        preferences.setColourSetting(setting)
        state.value = setting
    }

    companion object {
        /** About the hue of today's lavender: where one's own colour starts. */
        const val LAVENDER_HUE = 265
    }
}

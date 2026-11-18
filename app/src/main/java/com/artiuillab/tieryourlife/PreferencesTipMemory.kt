package com.artiuillab.tieryourlife

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.theme.tips.Tip
import com.artiuillab.tieryourlife.core.theme.tips.TipMemory

/** The tips as this device last left them: read once, written through, observed by the board as state. */
internal class PreferencesTipMemory(private val preferences: AppPreferences) : TipMemory {
    private var seenTips by mutableStateOf(preferences.tipsSeen().mapNotNull { name -> Tip.entries.firstOrNull { it.name == name } }.toSet())
    private var isOff by mutableStateOf(preferences.tipsOff())

    override val off: Boolean get() = isOff

    override fun seen(tip: Tip): Boolean = tip in seenTips

    override fun markSeen(tip: Tip) {
        if (tip in seenTips) return
        seenTips = seenTips + tip
        preferences.setTipsSeen(seenTips.mapTo(mutableSetOf()) { it.name })
    }

    override fun turnOff() {
        isOff = true
        preferences.setTipsOff(true)
    }

    override fun showAgain() {
        seenTips = emptySet()
        isOff = false
        preferences.setTipsSeen(emptySet())
        preferences.setTipsOff(false)
    }
}

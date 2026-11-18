package com.artiuillab.tieryourlife.core.theme.tips

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** The three tips a newcomer meets on a board (artboards 47–50, Danylo 18.11). */
enum class Tip {
    /** On the first board with cards in its pool: tap a card, then its tier. */
    Place,

    /** After the first card placed, on a list of eight or more: Play fills the tiers in. */
    Play,

    /** In one's own first empty list: a name brings its picture. */
    Add,
}

/**
 * Which tips this device has already shown, and whether the person turned them all off. Each tip
 * shows once, and never to someone who has done what it explains. Kept on the device; read
 * through [LocalTipMemory] so the board needs no view model for it. The app provides one backed
 * by its preferences.
 */
interface TipMemory {
    /** "No more tips". */
    val off: Boolean

    fun seen(tip: Tip): Boolean

    fun markSeen(tip: Tip)

    fun turnOff()

    /** Settings → How it works → Show the tips again. */
    fun showAgain()
}

/**
 * For previews, tests and anything drawn outside the app. Off unless asked for, so a screen
 * drawn in a test shows what it always showed.
 */
class InMemoryTipMemory(off: Boolean = true, seen: Set<Tip> = emptySet()) : TipMemory {
    private var isOff by mutableStateOf(off)
    private var seenTips by mutableStateOf(seen)

    override val off: Boolean get() = isOff

    override fun seen(tip: Tip): Boolean = tip in seenTips

    override fun markSeen(tip: Tip) {
        seenTips = seenTips + tip
    }

    override fun turnOff() {
        isOff = true
    }

    override fun showAgain() {
        isOff = false
        seenTips = emptySet()
    }
}

val LocalTipMemory = staticCompositionLocalOf<TipMemory> { InMemoryTipMemory() }

package com.artiuillab.tieryourlife.feature.play.domain

/**
 * The one source of chance in Play, written so the website's copy gives the
 * same numbers: 32-bit integer steps only, the same ones `Math.imul` and
 * `>>> 0` make in JavaScript.
 */
class Mulberry32(seed: Int) {
    private var state = seed

    /** The next number as an unsigned 32-bit value, held in a Long. */
    fun nextUInt(): Long {
        state += STEP
        var t = (state xor (state ushr FIRST_SHIFT)) * (1 or state)
        t = (t + ((t xor (t ushr SECOND_SHIFT)) * (SECOND_FACTOR or t))) xor t
        return (t xor (t ushr LAST_SHIFT)).toLong() and UNSIGNED
    }

    /** In [0, 1). */
    fun nextDouble(): Double = nextUInt().toDouble() / TWO_TO_THE_32

    private companion object {
        // The published algorithm's own numbers.
        const val STEP = 0x6D2B79F5
        const val FIRST_SHIFT = 15
        const val SECOND_SHIFT = 7
        const val SECOND_FACTOR = 61
        const val LAST_SHIFT = 14
        const val UNSIGNED = 0xFFFFFFFFL
        const val TWO_TO_THE_32 = 4_294_967_296.0
    }
}

/** Fisher–Yates from the last place down, the order both platforms use. */
fun shuffled(count: Int, seed: Int): List<Int> {
    val order = MutableList(count) { it }
    val random = Mulberry32(seed)
    for (i in count - 1 downTo 1) {
        val j = (random.nextDouble() * (i + 1)).toInt()
        order[i] = order[j].also { order[j] = order[i] }
    }
    return order
}

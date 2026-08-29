package com.artiuillab.tieryourlife.core.settings

/**
 * Something this phone stopped showing, and enough of a name to recognise it
 * by. Hiding is only useful if it can be undone, and an id alone is nothing a
 * person can choose between.
 */
data class HiddenEntry(val id: String, val label: String)

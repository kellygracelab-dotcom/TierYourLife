package com.artiuillab.tieryourlife.feature.settings.presentation.hidden

internal object HiddenTestTags {
    const val SCREEN = "hidden_screen"
    const val BACK = "hidden_back"
    const val EMPTY_STATE = "hidden_empty_state"

    fun listRow(id: String): String = "hidden_list_$id"

    fun personRow(id: String): String = "hidden_person_$id"
}

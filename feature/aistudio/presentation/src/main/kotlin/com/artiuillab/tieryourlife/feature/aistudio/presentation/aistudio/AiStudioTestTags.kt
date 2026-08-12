package com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio

internal object AiStudioTestTags {
    const val SCREEN = "ai_studio_screen"
    const val FIELD = "ai_studio_field"
    const val SEND = "ai_studio_send"
    const val DIALOG = "ai_studio_naming_dialog"
    const val NAME_FIELD = "ai_studio_naming_name_field"
    const val CLEAR_NAME = "ai_studio_naming_clear_name"
    const val ADD = "ai_studio_naming_add"
    const val CANCEL = "ai_studio_naming_cancel"
    fun hint(index: Int): String = "ai_studio_hint_$index"
    fun bubble(id: Long): String = "ai_studio_bubble_$id"
    fun result(id: Long): String = "ai_studio_result_$id"
    fun error(id: Long): String = "ai_studio_error_$id"
}

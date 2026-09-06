package com.artiuillab.tieryourlife.feature.community.presentation.published

internal object MyPublishedTestTags {
    const val SCREEN = "my_published_screen"
    const val BACK = "my_published_back"
    const val MESSAGE = "my_published_message"

    fun row(id: String): String = "my_published_row_$id"

    fun open(id: String): String = "my_published_open_$id"

    fun update(id: String): String = "my_published_update_$id"

    fun takeDown(id: String): String = "my_published_take_down_$id"
}

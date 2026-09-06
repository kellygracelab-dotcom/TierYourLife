package com.artiuillab.tieryourlife.feature.community.presentation.moderation

import com.artiuillab.tieryourlife.feature.community.domain.model.BanLength

internal object ModerationTestTags {
    const val SCREEN = "moderation_screen"
    const val BACK = "moderation_back"
    const val MESSAGE = "moderation_message"
    const val COVERS_TOGGLE = "moderation_covers_toggle"
    const val TAKE_DOWN_SHEET = "moderation_take_down_sheet"
    const val TAKE_DOWN_CONFIRM = "moderation_take_down_confirm"
    const val BAN_NONE = "moderation_ban_none"
    const val BAN_FOREVER = "moderation_ban_forever"
    const val BAN_FOREVER_DIALOG = "moderation_ban_forever_dialog"
    const val BAN_FOREVER_CONFIRM = "moderation_ban_forever_confirm"

    fun cover(listId: String): String = "moderation_cover_$listId"

    fun banChoice(length: BanLength): String = "moderation_ban_${length.id}"

    fun reportCard(listId: String): String = "moderation_report_$listId"

    fun takeDown(listId: String): String = "moderation_take_down_$listId"

    fun dismiss(listId: String): String = "moderation_dismiss_$listId"
}

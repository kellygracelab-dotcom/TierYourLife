package com.artiuillab.tieryourlife.feature.tier.presentation.trash

import com.artiuillab.tieryourlife.feature.tier.domain.model.TrashEntry

internal object TrashTestTags {
    const val BACK = "trash_back"
    const val MORE = "trash_more"
    const val MENU_EMPTY_TRASH = "trash_menu_empty_trash"
    const val LIST = "trash_list"
    const val EMPTY_STATE = "trash_empty_state"
    const val REMOVE_DIALOG = "trash_remove_dialog"
    const val REMOVE_CONFIRM = "trash_remove_confirm"
    const val REMOVE_CANCEL = "trash_remove_cancel"
    const val EMPTY_TRASH_DIALOG = "trash_empty_trash_dialog"
    const val EMPTY_TRASH_CONFIRM = "trash_empty_trash_confirm"
    const val EMPTY_TRASH_CANCEL = "trash_empty_trash_cancel"
    fun row(entry: TrashEntry): String = when (entry) {
        is TrashEntry.DeletedList -> "trash_row_list_${entry.id}"
        is TrashEntry.DeletedItem -> "trash_row_item_${entry.id}"
    }
    fun restoreButton(entry: TrashEntry): String = "${row(entry)}_restore"
    fun removeButton(entry: TrashEntry): String = "${row(entry)}_remove"
}

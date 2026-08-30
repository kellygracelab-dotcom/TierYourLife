package com.artiuillab.tieryourlife.feature.account.presentation.account

internal object AccountTestTags {
    const val SCREEN = "account_screen"
    const val CLOSE = "account_close"
    const val SIGN_IN = "account_sign_in"
    const val NOT_NOW = "account_not_now"
    const val BACK_UP_BOARDS = "account_back_up_boards"
    const val DONE = "account_done"
    const val SIGN_OUT = "account_sign_out"
    const val EMAIL = "account_email"
    const val NAME = "account_name"
    const val EDIT_NAME = "account_edit_name"
    const val NICKNAME_FIELD = "account_nickname_field"
    const val NICKNAME_SAVE = "account_nickname_save"
    const val COMMUNITY_ROW = "account_community_row"
    const val EDIT_FACE = "account_edit_face"
    const val FACE_SHEET = "account_face_sheet"
    const val FACE_GOOGLE = "account_face_google"
    const val FACE_LETTER = "account_face_letter"
    const val CREDITS = "account_credits"
    const val NOTICE = "account_notice"
    fun reason(index: Int): String = "account_reason_$index"

    fun faceChoice(url: String): String = "account_face_choice_${url.hashCode()}"
}

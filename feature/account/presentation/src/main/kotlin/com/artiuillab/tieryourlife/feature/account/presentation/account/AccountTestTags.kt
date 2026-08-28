package com.artiuillab.tieryourlife.feature.account.presentation.account

internal object AccountTestTags {
    const val SCREEN = "account_screen"
    const val CLOSE = "account_close"
    const val SIGN_IN = "account_sign_in"
    const val NOT_NOW = "account_not_now"
    const val DONE = "account_done"
    const val SIGN_OUT = "account_sign_out"
    const val EMAIL = "account_email"
    const val CREDITS = "account_credits"
    const val NOTICE = "account_notice"
    fun reason(index: Int): String = "account_reason_$index"
}

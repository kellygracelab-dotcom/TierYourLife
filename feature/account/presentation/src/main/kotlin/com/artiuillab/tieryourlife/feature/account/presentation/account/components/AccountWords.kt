package com.artiuillab.tieryourlife.feature.account.presentation.account.components

import com.artiuillab.tieryourlife.core.settings.Features
import com.artiuillab.tieryourlife.feature.account.presentation.R

/** Credits and purchases are spoken of only while generation is offered; until then the app is free (19.11). */
internal object AccountWords {
    private val offered = Features.GENERATION_OFFERED

    val reasonKept = if (offered) R.string.account_reason_purchases else R.string.account_reason_settings
    val existingAccount = if (offered) R.string.account_notice_existing_account else R.string.account_notice_existing_account_lists
    val signOutHelper = if (offered) R.string.account_sign_out_helper else R.string.account_sign_out_helper_lists
    val signOutWaiting = if (offered) R.string.account_sign_out_body_2 else R.string.account_sign_out_body_2_lists
}

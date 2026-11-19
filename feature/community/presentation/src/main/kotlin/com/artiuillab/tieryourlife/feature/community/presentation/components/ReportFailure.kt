package com.artiuillab.tieryourlife.feature.community.presentation.components

import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportsToday
import com.artiuillab.tieryourlife.feature.community.domain.model.TargetIsStaff

/** A report that did not go: a list by staff and the day's limit are said; anything else is a failure. */
internal fun reportFailure(error: Throwable): UserMessage = when (error) {
    is TargetIsStaff -> UserMessage.TargetIsStaff
    is ReportsToday -> UserMessage.ReportsToday
    else -> UserMessage.ActionFailed
}

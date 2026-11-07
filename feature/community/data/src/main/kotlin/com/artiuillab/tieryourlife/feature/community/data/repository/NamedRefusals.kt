package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.data.remote.isAppUnverified
import com.artiuillab.tieryourlife.feature.community.data.remote.isTargetStaff
import com.artiuillab.tieryourlife.feature.community.domain.model.AppUnverified
import com.artiuillab.tieryourlife.feature.community.domain.model.PageGone
import com.artiuillab.tieryourlife.feature.community.domain.model.TargetIsStaff

internal fun Throwable.named(): Throwable = when {
    isAppUnverified() -> AppUnverified()
    isCursorGone() -> PageGone()
    isTargetStaff() -> TargetIsStaff()
    else -> this
}

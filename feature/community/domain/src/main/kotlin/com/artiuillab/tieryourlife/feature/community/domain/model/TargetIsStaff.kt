package com.artiuillab.tieryourlife.feature.community.domain.model

/** The list's author is staff, and staff do not judge staff: only the owner may report, take down, keep or move it. */
class TargetIsStaff : Exception("Only the owner may act on a staff member's list")

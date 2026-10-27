package com.artiuillab.tieryourlife.feature.community.presentation.moderation

import com.artiuillab.tieryourlife.feature.community.domain.model.ModerationReport
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportReason
import com.artiuillab.tieryourlife.feature.community.presentation.list.CommunityListUiState
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList

internal val previewReports = listOf(
    ModerationReport(
        listId = "1",
        listTitle = "Every A24 film, ranked",
        authorName = "Olena Marchuk",
        reasons = listOf(ReportReason.Spam, ReportReason.Other),
        notes = listOf("Every card is a link to the same shop."),
        reportCount = 3,
        newestAtMillis = 0,
        hidden = true,
        reviewed = false,
    ),
    ModerationReport(
        listId = "2",
        listTitle = "Ramen places in Kyiv",
        authorName = "Someone Else",
        reasons = listOf(ReportReason.Hate),
        notes = emptyList(),
        reportCount = 1,
        newestAtMillis = 0,
        hidden = false,
        reviewed = true,
    ),
    ModerationReport(
        listId = "3",
        listTitle = "Best goalkeepers of the decade",
        authorName = "Taras",
        reasons = listOf(ReportReason.WrongCategory),
        notes = emptyList(),
        reportCount = 1,
        newestAtMillis = 0,
        hidden = false,
        reviewed = false,
    ),
)

internal val previewLooking = CommunityListUiState.Success(
    list = TierList(
        id = 0,
        title = "Every A24 film, ranked",
        tiers = listOf(
            Tier(id = 1, label = "S", colorLight = "#B03A32", colorDark = "#F1948C", items = listOf(TierItem(1, "Moonlight", null), TierItem(2, "Aftersun", null))),
            Tier(id = 2, label = "A", colorLight = "#C06A25", colorDark = "#F0B27A", items = listOf(TierItem(3, "Lady Bird", null))),
            Tier(id = 3, label = "B", colorLight = "#A98B1F", colorDark = "#E5CF6B", items = listOf(TierItem(4, "Minari", null))),
            Tier(id = 4, label = "C", colorLight = "#3F7F55", colorDark = "#8FD3A5", items = emptyList()),
        ),
    ),
    authorName = "Olena Marchuk",
)

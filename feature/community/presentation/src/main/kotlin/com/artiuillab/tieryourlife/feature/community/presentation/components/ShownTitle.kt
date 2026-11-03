package com.artiuillab.tieryourlife.feature.community.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishedListSummary
import com.artiuillab.tieryourlife.feature.community.domain.model.titleIn
import com.artiuillab.tieryourlife.feature.tier.board.common.currentLanguageTag

/** The title in the language the app is drawn in, where the house has translated it. */
@Composable
@ReadOnlyComposable
internal fun PublishedListSummary.shownTitle(): String = titleIn(currentLanguageTag())

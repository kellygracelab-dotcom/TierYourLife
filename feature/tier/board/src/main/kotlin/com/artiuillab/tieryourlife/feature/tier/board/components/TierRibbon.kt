package com.artiuillab.tieryourlife.feature.tier.board.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.feature.tier.board.common.tierRowColors
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierSummary
@Composable
fun TierRibbon(tiers: List<TierSummary>) {
    val rankedTiers = tiers.filterNot { it.isPool }
    val pool = tiers.firstOrNull { it.isPool }
    val total = tiers.sumOf { it.cards }
    val media = TierYourLifeMedia.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (total == 0) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(media.unrankedRibbon),
            )
        } else {
            rankedTiers.filter { it.cards > 0 }.forEachIndexed { index, tier ->
                val band = tierRowColors(tier.colorLight, tier.colorDark).band
                Box(
                    Modifier
                        .weight(tier.cards.toFloat())
                        .height(8.dp)
                        .clip(
                            if (index == 0) RoundedCornerShape(
                                topStart = 4.dp,
                                bottomStart = 4.dp,
                            ) else RoundedCornerShape(0.dp),
                        )
                        .background(band),
                )
            }
            if (pool != null && pool.cards > 0) {
                Box(
                    Modifier
                        .weight(pool.cards.toFloat())
                        .height(8.dp)
                        .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        .background(media.unrankedRibbon),
                )
            }
        }
    }
}

package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.AutoAwesomeIcon

@Composable
internal fun GenerateChip(onClick: () -> Unit) {
    val description = stringResource(R.string.cd_ai_chip)
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(100.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(100.dp))
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .testTag(TierDetailTestTags.GENERATE_CHIP)
            .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AutoAwesomeIcon(18.dp, MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.ai_chip),
            style = TierYourLifeType.current.chipText,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

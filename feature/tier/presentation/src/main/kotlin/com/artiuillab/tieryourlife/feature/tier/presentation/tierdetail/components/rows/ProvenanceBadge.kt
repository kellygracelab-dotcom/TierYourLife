package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.rows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.AutoAwesomeIcon

@Composable
internal fun GeneratedBadge(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.cd_ai_badge)
    Box(
        modifier = modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        AutoAwesomeIcon(10.dp, MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

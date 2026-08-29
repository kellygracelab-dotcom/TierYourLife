package com.artiuillab.tieryourlife.feature.tier.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.feature.tier.presentation.common.SectionLabel

private val CARD_SHAPE = RoundedCornerShape(20.dp)

@Composable
internal fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(Modifier.height(24.dp))
        SectionLabel(title, Modifier.padding(start = 28.dp, bottom = 8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(CARD_SHAPE)
                .background(
                    if (outlined) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    },
                )
                .then(
                    if (outlined) {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CARD_SHAPE)
                    } else {
                        Modifier
                    },
                ),
            content = { content() },
        )
    }
}

@Composable
internal fun SettingsGroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 64.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 1.dp,
    )
}

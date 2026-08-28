package com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.aistudio.presentation.R
import com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.AiStudioTestTags

@Composable
internal fun AiStudioTopBar(onBack: () -> Unit, credits: Int? = null) {
    val backDescription = stringResource(R.string.cd_back)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = backDescription },
        ) {
            BackIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = stringResource(R.string.ai_title),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (credits != null) {
            CreditsBadge(credits)
        }
    }
}

@Composable
private fun CreditsBadge(credits: Int) {
    val description = String.format(stringResource(R.string.ai_credits_label), credits)
    Text(
        text = credits.toString(),
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(AiStudioTestTags.CREDITS)
            .semantics { contentDescription = description },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@Preview(showBackground = true)
@Composable
private fun AiStudioTopBarLightPreview() = TierYourLifeTheme(false) {
    AiStudioTopBar(onBack = {}, credits = 7)
}

@Preview(showBackground = true)
@Composable
private fun AiStudioTopBarDarkPreview() = TierYourLifeTheme(true) {
    AiStudioTopBar(onBack = {}, credits = 7)
}

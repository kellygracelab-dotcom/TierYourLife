package com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.aistudio.presentation.R

/**
 * Deliberately not the error card. Running out is an expected end to a free
 * allowance, not a fault, so it is not painted red and it offers no "try
 * again" — trying again would refuse in exactly the same way.
 */
@Composable
internal fun OutOfCreditsCard(testTag: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .widthIn(max = 288.dp)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .testTag(testTag)
            .padding(all = 16.dp),
    ) {
        Row {
            AutoAwesomeIcon(20.dp, MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.ai_out_of_credits_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.ai_out_of_credits_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OutOfCreditsCardLightPreview() = TierYourLifeTheme(false) {
    OutOfCreditsCard(testTag = "out_of_credits_preview")
}

@Preview(showBackground = true)
@Composable
private fun OutOfCreditsCardDarkPreview() = TierYourLifeTheme(true) {
    OutOfCreditsCard(testTag = "out_of_credits_preview")
}

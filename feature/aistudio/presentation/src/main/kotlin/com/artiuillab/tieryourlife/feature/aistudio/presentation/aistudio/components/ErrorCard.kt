package com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.aistudio.presentation.R

@Composable
internal fun ErrorCard(testTag: String, onTryAgain: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .widthIn(max = 288.dp)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .testTag(testTag)
            .padding(top = 14.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
    ) {
        Row {
            ErrorOutlineIcon(20.dp, MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.ai_error_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.ai_error_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                TextButton(
                    onClick = onTryAgain,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.action_try_again))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorCardLightPreview() = TierYourLifeTheme(false) {
    ErrorCard(testTag = "error_card_preview", onTryAgain = {})
}

@Preview(showBackground = true)
@Composable
private fun ErrorCardDarkPreview() = TierYourLifeTheme(true) {
    ErrorCard(testTag = "error_card_preview", onTryAgain = {})
}

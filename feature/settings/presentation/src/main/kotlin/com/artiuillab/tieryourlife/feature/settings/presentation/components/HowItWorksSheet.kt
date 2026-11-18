package com.artiuillab.tieryourlife.feature.settings.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.layout.AdaptiveSheet
import com.artiuillab.tieryourlife.core.theme.layout.SheetWidth
import com.artiuillab.tieryourlife.core.theme.ui.PlayArrowIcon
import com.artiuillab.tieryourlife.core.theme.ui.PlusIcon
import com.artiuillab.tieryourlife.core.theme.ui.ShareIcon
import com.artiuillab.tieryourlife.feature.settings.presentation.R
import com.artiuillab.tieryourlife.feature.settings.presentation.SettingsTestTags

/**
 * Settings → How it works (artboard 50): one line for each thing the app does, and Show the tips
 * again, which brings the newcomer's three tips back. Not a manual.
 */
@Composable
internal fun HowItWorksSheet(onDismiss: () -> Unit, onTipsAgain: () -> Unit) {
    AdaptiveSheet(onDismiss = onDismiss, width = SheetWidth.Choosing) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .testTag(SettingsTestTags.HOW_IT_WORKS_SHEET),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_how_it_works),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            HowLine({ size, color -> TierStackIcon(size, color) }, R.string.how_place, R.string.how_place_text)
            HowLine({ size, color -> PlayArrowIcon(size, color) }, R.string.how_play, R.string.how_play_text)
            HowLine({ size, color -> PlusIcon(size, color) }, R.string.how_own, R.string.how_own_text)
            HowLine({ size, color -> ShareIcon(size, color) }, R.string.how_share, R.string.how_share_text)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.how_close)) }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = onTipsAgain, modifier = Modifier.testTag(SettingsTestTags.TIPS_AGAIN)) {
                    Text(stringResource(R.string.how_again))
                }
            }
        }
    }
}

@Composable
private fun HowLine(icon: @Composable (Dp, Color) -> Unit, name: Int, text: Int) {
    val named = stringResource(name)
    val said = stringResource(text)
    Row(verticalAlignment = Alignment.Top) {
        icon(24.dp, MaterialTheme.colorScheme.primary)
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(named) }
                append(" — ")
                append(said)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

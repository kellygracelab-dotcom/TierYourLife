package com.artiuillab.tieryourlife.feature.aistudio.presentation.aistudio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.feature.aistudio.domain.model.GeneratedCardImage
import com.artiuillab.tieryourlife.feature.aistudio.presentation.R

@Composable
internal fun ResultCard(
    image: GeneratedCardImage,
    testTag: String,
    onAddToList: () -> Unit,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .testTag(testTag)
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 192.dp, height = 256.dp)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            AsyncImage(
                model = image.imageUri,
                contentDescription = image.prompt,
                modifier = Modifier.size(width = 192.dp, height = 256.dp),
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.ai_not_saved),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilledTonalButton(
                onClick = onAddToList,
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                contentPadding = PaddingValues(horizontal = 20.dp),
                modifier = Modifier.height(40.dp),
            ) {
                Text(stringResource(R.string.action_add_to_list))
            }
            TextButton(
                onClick = onRegenerate,
                shape = RoundedCornerShape(100.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(40.dp),
            ) {
                Text(stringResource(R.string.action_regenerate))
            }
        }
    }
}

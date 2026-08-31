package com.artiuillab.tieryourlife.feature.account.presentation.account.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.artiuillab.tieryourlife.core.theme.layout.AdaptiveSheet
import com.artiuillab.tieryourlife.core.theme.layout.SheetWidth
import com.artiuillab.tieryourlife.feature.account.presentation.R
import com.artiuillab.tieryourlife.feature.account.presentation.account.AccountTestTags

private val CHOICE_SIZE = 64.dp

/**
 * A face without a file: every choice is an address someone else already hosts,
 * so nothing of anyone's is stored here and the community can show it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FaceSheet(
    current: String?,
    cardImages: List<String>,
    name: String?,
    onDismiss: () -> Unit,
    onChoose: (String?) -> Unit,
) {
    AdaptiveSheet(
        onDismiss = onDismiss,
        width = SheetWidth.Choosing,
        maxHeight = 560.dp,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .testTag(AccountTestTags.FACE_SHEET),
        ) {
            Text(
                text = stringResource(R.string.account_face_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    if (cardImages.isEmpty()) R.string.account_face_no_cards else R.string.account_face_body,
                ),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (cardImages.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(cardImages) { url ->
                        FaceChoice(
                            selected = url == current,
                            onClick = { onChoose(url) },
                            testTag = AccountTestTags.faceChoice(url),
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(CHOICE_SIZE).clip(CircleShape),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            TextButton(
                onClick = { onChoose(null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AccountTestTags.FACE_LETTER),
            ) {
                Text(stringResource(R.string.account_face_letter))
            }
            Text(
                text = stringResource(R.string.account_face_note),
                modifier = Modifier.padding(bottom = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun FaceChoice(
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(CHOICE_SIZE)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier,
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

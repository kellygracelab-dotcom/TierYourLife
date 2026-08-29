package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.sheets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.color.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierListDisplayMode
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.PlusIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.common.labelRes
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.TierDetailTestTags
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.BackIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.CategoryIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.ChevronRightIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.CoverIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.FormatListNumberedIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.GridViewIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.ViewCarouselIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.previewTierList

private val DisplayModeSelectedTintLight = Color(0xFFEDEBFA)
private val DisplayModeSelectedTintDark = Color(0xFF2E2F45)

@Composable
internal fun TierListSettingsScreenContent(
    list: TierList,
    onBack: () -> Unit,
    onAddTier: (label: String, caption: String?, colorLight: String, colorDark: String) -> Unit,
    onSetDisplayMode: (displayMode: TierListDisplayMode) -> Unit,
    signedIn: Boolean = false,
    publishing: Boolean = false,
    publishError: PublishError? = null,
    onSetPublic: (Boolean) -> Unit = {},
    onSetCategory: (ListCategory) -> Unit = {},
    onSetCover: (String?) -> Unit = {},
) {
    var tierEditorVisible by remember { mutableStateOf(false) }
    var categorySheetVisible by remember { mutableStateOf(false) }
    var coverSheetVisible by remember { mutableStateOf(false) }
    val rankedTierCount = list.tiers.count { !it.isPool }

    Column(Modifier.fillMaxSize().testTag(TierDetailTestTags.LIST_SETTINGS_SCREEN)) {
        ListSettingsTopBar(onBack = onBack)

        PublishSection(
            published = list.publishedId != null,
            signedIn = signedIn,
            hasItems = list.tiers.any { it.items.isNotEmpty() },
            busy = publishing,
            error = publishError,
            onSetPublic = onSetPublic,
        )

        // Both sit with the switch rather than inside a publishing wizard, and
        // both stay available to a guest: a cover is what their own card shows,
        // and a category chosen now is one less thing in the way later.
        CategoryRow(category = list.category, onClick = { categorySheetVisible = true })
        CoverRow(coverImageUrl = list.coverImageUrl, onClick = { coverSheetVisible = true })

        DisplayModeSection(selected = list.displayMode, onSelect = onSetDisplayMode)

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        NewTierRow(
            tierCount = rankedTierCount,
            onClick = { tierEditorVisible = true },
        )
    }

    if (categorySheetVisible) {
        CategorySheet(
            selected = list.category,
            onDismiss = { categorySheetVisible = false },
            onSelect = {
                onSetCategory(it)
                categorySheetVisible = false
            },
        )
    }

    if (coverSheetVisible) {
        CoverSheet(
            list = list,
            onDismiss = { coverSheetVisible = false },
            onSelect = {
                onSetCover(it)
                coverSheetVisible = false
            },
        )
    }

    if (tierEditorVisible) {
        TierEditorSheet(
            onDismiss = { tierEditorVisible = false },
            onSave = { label, caption, colorLight, colorDark ->
                onAddTier(label, caption, colorLight, colorDark)
                tierEditorVisible = false
            },
        )
    }
}

@Composable
private fun ListSettingsTopBar(onBack: () -> Unit) {
    val backDescription = stringResource(R.string.tier_detail_content_description_back)
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
        ) { BackIcon() }
        Text(
            text = stringResource(R.string.tier_list_settings_title),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DisplayModeSection(
    selected: TierListDisplayMode,
    onSelect: (TierListDisplayMode) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.tier_list_settings_display_section),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.tier_list_settings_display_section_caption),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            style = TierYourLifeType.current.captionUnderTitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.selectableGroup()) {
            DisplayModeRow(
                selected = selected == TierListDisplayMode.WRAP,
                icon = { size, color -> GridViewIcon(size, color) },
                title = stringResource(R.string.tier_list_settings_mode_wrapped),
                subtitle = stringResource(R.string.tier_list_settings_mode_wrapped_sub),
                onClick = { onSelect(TierListDisplayMode.WRAP) },
                testTag = TierDetailTestTags.LIST_SETTINGS_MODE_WRAP,
            )
            DisplayModeRow(
                selected = selected == TierListDisplayMode.HORIZONTAL_SCROLL,
                icon = { size, color -> ViewCarouselIcon(size, color) },
                title = stringResource(R.string.tier_list_settings_mode_strip),
                subtitle = stringResource(R.string.tier_list_settings_mode_strip_sub),
                onClick = { onSelect(TierListDisplayMode.HORIZONTAL_SCROLL) },
                testTag = TierDetailTestTags.LIST_SETTINGS_MODE_STRIP,
            )
            DisplayModeRow(
                selected = selected == TierListDisplayMode.FLAT_RANKED,
                icon = { size, color -> FormatListNumberedIcon(size, color) },
                title = stringResource(R.string.tier_list_settings_mode_ranked),
                subtitle = stringResource(R.string.tier_list_settings_mode_ranked_sub),
                onClick = { onSelect(TierListDisplayMode.FLAT_RANKED) },
                testTag = TierDetailTestTags.LIST_SETTINGS_MODE_RANKED,
            )
        }
    }
}

@Composable
private fun DisplayModeRow(
    selected: Boolean,
    icon: @Composable (Dp, Color) -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
) {
    val isDark = TierYourLifeMedia.current.isDark
    val background = if (selected) {
        if (isDark) DisplayModeSelectedTintDark else DisplayModeSelectedTintLight
    } else {
        Color.Transparent
    }
    val accent = MaterialTheme.colorScheme.primary
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .testTag(testTag)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioDot(
            selected = selected,
            color = if (selected) accent else MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 2.dp),
        )
        icon(24.dp, if (selected) accent else neutral)
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = TierYourLifeType.current.captionUnderTitle, color = neutral)
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(20.dp)) {
        val strokeWidth = 2.dp.toPx()
        drawCircle(color, radius = (20.dp.toPx() - strokeWidth) / 2f, style = Stroke(strokeWidth))
        if (selected) {
            drawCircle(color, radius = 5.dp.toPx())
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = TierYourLifeType.current.captionUnderTitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ChevronRightIcon(20.dp, MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun CategoryRow(category: ListCategory?, onClick: () -> Unit) {
    SettingsActionRow(
        icon = { CategoryIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant) },
        title = stringResource(R.string.list_settings_category),
        subtitle = category?.let { stringResource(it.labelRes) }
            ?: stringResource(R.string.list_settings_category_none),
        onClick = onClick,
        testTag = TierDetailTestTags.CATEGORY_ROW,
    )
}

@Composable
private fun CoverRow(coverImageUrl: String?, onClick: () -> Unit) {
    SettingsActionRow(
        icon = { CoverIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant) },
        title = stringResource(R.string.list_settings_cover),
        subtitle = stringResource(
            if (coverImageUrl == null) R.string.list_settings_cover_none_set else R.string.list_settings_cover_set,
        ),
        onClick = onClick,
        testTag = TierDetailTestTags.COVER_ROW,
    )
}

@Composable
private fun NewTierRow(tierCount: Int, onClick: () -> Unit) {
    SettingsActionRow(
        icon = { PlusIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant) },
        title = stringResource(R.string.tier_list_settings_new_tier_title),
        subtitle = pluralStringResource(R.plurals.tier_list_settings_tier_count, tierCount, tierCount),
        onClick = onClick,
        testTag = TierDetailTestTags.NEW_TIER_ROW,
    )
}

@TierYourLifeDevicePreviews
@Composable
private fun TierListSettingsScreenLightPreview() = TierYourLifeTheme(false) {
    TierListSettingsScreenContent(
        list = previewTierList,
        onBack = {},
        onAddTier = { _, _, _, _ -> },
        onSetDisplayMode = {},
    )
}

@TierYourLifeDevicePreviews
@Composable
private fun TierListSettingsScreenDarkPreview() = TierYourLifeTheme(true) {
    TierListSettingsScreenContent(
        list = previewTierList,
        onBack = {},
        onAddTier = { _, _, _, _ -> },
        onSetDisplayMode = {},
    )
}

@Composable
private fun PublishSection(
    published: Boolean,
    signedIn: Boolean,
    hasItems: Boolean,
    busy: Boolean,
    error: PublishError?,
    onSetPublic: (Boolean) -> Unit,
) {
    val caption = when {
        !signedIn -> R.string.list_settings_public_needs_account
        !hasItems -> R.string.list_settings_public_needs_items
        else -> R.string.list_settings_public_body
    }

    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.list_settings_public),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = published,
                onCheckedChange = onSetPublic,
                enabled = signedIn && (published || hasItems) && !busy,
                modifier = Modifier.testTag(TierDetailTestTags.PUBLIC_SWITCH),
            )
        }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(error.messageRes()),
                modifier = Modifier.testTag(TierDetailTestTags.PUBLISH_ERROR),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (published) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.list_settings_public_images),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

private fun PublishError.messageRes(): Int = when (this) {
    PublishError.NotSignedIn -> R.string.list_settings_public_needs_account
    PublishError.NothingToPublish -> R.string.list_settings_public_needs_items
    PublishError.NoCategory -> R.string.list_settings_public_needs_category
    PublishError.TooManyLists -> R.string.list_settings_public_too_many
    PublishError.Offline -> R.string.list_settings_public_offline
    PublishError.Unknown -> R.string.list_settings_public_failed
}

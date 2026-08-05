package com.artiuillab.tieryourlife.feature.tier.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artiuillab.tieryourlife.core.theme.TierYourLifeMedia
import com.artiuillab.tieryourlife.core.theme.TierYourLifeType
import com.artiuillab.tieryourlife.feature.tier.domain.export.TierListsExportStrings
import com.artiuillab.tieryourlife.feature.tier.domain.model.ThemeChoice
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.FileDownloadIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.common.VectorIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.BackIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.CheckIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.ChevronRightIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.DeleteOutlineIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.DeletedItemSnackbarHost
import com.artiuillab.tieryourlife.feature.tier.presentation.tierlists.OnResumeEffect
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

internal object SettingsTestTags {
    const val BACK = "settings_back"
    const val THEME_ROW = "settings_theme_row"
    const val THEME_LIGHT = "settings_theme_light"
    const val THEME_DARK = "settings_theme_dark"
    const val THEME_SYSTEM = "settings_theme_system"
    const val LANGUAGE_ROW = "settings_language_row"
    const val LANGUAGE_SHEET = "settings_language_sheet"
    const val TRASH_ROW = "settings_trash_row"
    const val EXPORT_ROW = "settings_export_row"
    fun languageOption(tag: String?) = "settings_language_option_${tag ?: "default"}"
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onTrashClick: () -> Unit,
    themeChoice: ThemeChoice,
    onThemeChoiceChange: (ThemeChoice) -> Unit,
    languageTag: String?,
    onLanguageTagChange: (String?) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val trashCount by viewModel.trashCount.collectAsState()
    OnResumeEffect(onResume = viewModel::loadTrashCount)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val failedMessage = stringResource(R.string.snack_export_failed)
    val tryAgainLabel = stringResource(R.string.action_try_again)
    val shareLabel = stringResource(R.string.action_share)
    val exportStrings = buildExportStrings(context)

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.exportText(exportStrings) { exported ->
            coroutineScope.launch {
                attemptWriteExport(
                    context = context,
                    uri = uri,
                    text = exported.text,
                    listCount = exported.listCount,
                    snackbarHostState = snackbarHostState,
                    failedMessage = failedMessage,
                    tryAgainLabel = tryAgainLabel,
                    shareLabel = shareLabel,
                )
            }
        }
    }

    SettingsScreenContent(
        themeChoice = themeChoice,
        onThemeChoiceChange = onThemeChoiceChange,
        languageTag = languageTag,
        onLanguageTagChange = onLanguageTagChange,
        trashCount = trashCount,
        onBack = onBack,
        onTrashClick = onTrashClick,
        onExportClick = { createDocumentLauncher.launch(defaultExportFileName(context)) },
        snackbarHostState = snackbarHostState,
    )
}

// Retries by writing the same already-formatted text to the same uri — not by reopening
// the picker — so "Try again" really is the one tap the design promises rather than a
// second trip through system UI (docs/design-spec-home.md, section 8).
private suspend fun attemptWriteExport(
    context: Context,
    uri: Uri,
    text: String,
    listCount: Int,
    snackbarHostState: SnackbarHostState,
    failedMessage: String,
    tryAgainLabel: String,
    shareLabel: String,
) {
    val succeeded = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(text.toByteArray(Charsets.UTF_8))
        } ?: error("openOutputStream returned null")
    }.isSuccess

    snackbarHostState.currentSnackbarData?.dismiss()
    if (succeeded) {
        val message = context.resources.getQuantityString(R.plurals.snack_export_done, listCount, listCount)
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = shareLabel,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) {
            shareExportedFile(context, uri)
        }
    } else {
        val result = snackbarHostState.showSnackbar(
            message = failedMessage,
            actionLabel = tryAgainLabel,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) {
            attemptWriteExport(context, uri, text, listCount, snackbarHostState, failedMessage, tryAgainLabel, shareLabel)
        }
    }
}

private fun shareExportedFile(context: Context, uri: Uri) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, null))
}

private fun defaultExportFileName(context: Context): String {
    val isoDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    return context.getString(R.string.export_filename, isoDate)
}

// The strings a pure domain function needs, resolved here from Android resources so the
// domain module never has to know what one is (docs/design-spec-home.md, section 8).
private fun buildExportStrings(context: Context): TierListsExportStrings {
    val exportedOnFormat = context.getString(R.string.export_file_date)
    val localizedDate = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(Date())
    return TierListsExportStrings(
        header = context.getString(R.string.export_file_header),
        exportedOn = String.format(exportedOnFormat, localizedDate),
        listCountText = { count -> context.resources.getQuantityString(R.plurals.tier_lists_count, count, count) },
        rankedCountText = { count ->
            context.resources.getQuantityString(R.plurals.tier_lists_ranked_count, count, count)
        },
        unrankedCountText = { count -> context.resources.getQuantityString(R.plurals.unranked_count, count, count) },
        tierWithCaptionFormat = context.getString(R.string.export_tier_with_caption),
        tierPlainFormat = context.getString(R.string.export_tier_plain),
        tierEmptyLabel = context.getString(R.string.export_tier_empty),
        unrankedHeading = context.getString(R.string.export_unranked_heading),
    )
}

@Composable
internal fun SettingsScreenContent(
    themeChoice: ThemeChoice,
    onThemeChoiceChange: (ThemeChoice) -> Unit,
    languageTag: String?,
    onLanguageTagChange: (String?) -> Unit,
    trashCount: Int,
    onBack: () -> Unit,
    onTrashClick: () -> Unit,
    onExportClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                SettingsTopBar(onBack = onBack)
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ThemeSection(themeChoice = themeChoice, onThemeChoiceChange = onThemeChoiceChange)
                    SettingsDivider()
                    LanguageRow(languageTag = languageTag, onLanguageTagChange = onLanguageTagChange)
                    SettingsDivider()
                    TrashRow(trashCount = trashCount, onClick = onTrashClick)
                    SettingsDivider()
                    ExportRow(onClick = onExportClick)
                }
            }

            DeletedItemSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 88.dp),
            )
        }
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    val backDescription = stringResource(R.string.cd_settings_back)
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
                .semantics { contentDescription = backDescription }
                .testTag(SettingsTestTags.BACK),
        ) { BackIcon() }
        Text(
            text = stringResource(R.string.settings_title),
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSection(themeChoice: ThemeChoice, onThemeChoiceChange: (ThemeChoice) -> Unit) {
    Column(Modifier.testTag(SettingsTestTags.THEME_ROW)) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)) {
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_theme_sub),
                style = TierYourLifeType.current.captionUnderTitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val options = listOf(
            Triple(ThemeChoice.LIGHT, R.string.theme_light, SettingsTestTags.THEME_LIGHT),
            Triple(ThemeChoice.DARK, R.string.theme_dark, SettingsTestTags.THEME_DARK),
            Triple(ThemeChoice.SYSTEM, R.string.theme_system, SettingsTestTags.THEME_SYSTEM),
        )
        val labels = options.map { (_, labelRes, _) -> stringResource(labelRes) }

        // Whether three labels fit side by side is measured, not assumed. Eleven
        // languages and a system font scale that goes past 2x make any hardcoded
        // threshold wrong for somebody: "Follow system" is one word in English and
        // "Zgodnie z systemem" in Polish, and the next translation added will not be
        // consulted about it. So the labels are measured against the width a segment
        // would actually get, and the control lays itself out accordingly.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            val labelStyle = TierYourLifeType.current.tabLabel
            val measurer = rememberTextMeasurer()
            val density = LocalDensity.current
            val segmentWidth = maxWidth / options.size
            val widest = labels.maxOf { label ->
                with(density) {
                    measurer.measure(AnnotatedString(label), labelStyle, maxLines = 1).size.width.toDp()
                }
            }
            // Reserved on every segment, not just the selected one, so the row does not
            // reflow as the selection moves: the 18dp check, the gap after it, and the
            // button's own horizontal padding.
            val fitsSideBySide = widest <= segmentWidth - SEGMENT_RESERVED_WIDTH

            if (fitsSideBySide) {
                ThemeSegmentedRow(options, labels, themeChoice, onThemeChoiceChange)
            } else {
                ThemeStackedRows(options, labels, themeChoice, onThemeChoiceChange)
            }
        }
    }
}

private val SEGMENT_RESERVED_WIDTH = 42.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSegmentedRow(
    options: List<Triple<ThemeChoice, Int, String>>,
    labels: List<String>,
    themeChoice: ThemeChoice,
    onThemeChoiceChange: (ThemeChoice) -> Unit,
) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .height(40.dp),
        ) {
            options.forEachIndexed { index, (choice, labelRes, tag) ->
                val selected = themeChoice == choice
                SegmentedButton(
                    selected = selected,
                    onClick = { onThemeChoiceChange(choice) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    icon = {
                        if (selected) {
                            CheckIcon(18.dp, MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    },
                    modifier = Modifier.testTag(tag),
                ) {
                    Text(text = labels[index], style = TierYourLifeType.current.tabLabel)
                }
            }
        }
}

// The fallback when three labels cannot sit side by side: the same three choices as
// full-width rows, 48dp each so they clear the touch-target floor, with the check moved
// to the leading edge. The outline and the rounded outer ends are kept so it still reads
// as one control rather than as three unrelated rows (docs/design-spec-home.md,
// section 7).
@Composable
private fun ThemeStackedRows(
    options: List<Triple<ThemeChoice, Int, String>>,
    labels: List<String>,
    themeChoice: ThemeChoice,
    onThemeChoiceChange: (ThemeChoice) -> Unit,
) {
    val outline = MaterialTheme.colorScheme.outline
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, outline, shape)
            .clip(shape)
            .selectableGroup(),
    ) {
        options.forEachIndexed { index, (choice, _, tag) ->
            val selected = themeChoice == choice
            if (index > 0) {
                HorizontalDivider(thickness = 1.dp, color = outline)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { onThemeChoiceChange(choice) },
                    )
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag(tag),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(18.dp)) {
                    if (selected) {
                        CheckIcon(18.dp, MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = labels[index],
                    style = TierYourLifeType.current.tabLabel,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 1.dp,
    )
}

// Mock-literal color with no matching theme role — the same selection tint the
// display-mode rows in TierListSettingsScreen.kt and the "currently here" tier row in
// MoveItemSheet.kt already use for "the standard row tint" (docs/design-spec-home.md,
// section 7's own wording for the selected radio row).
private val LanguageOptionSelectedTintLight = Color(0xFFEDEBFA)
private val LanguageOptionSelectedTintDark = Color(0xFF2E2F45)

@Composable
private fun LanguageRow(languageTag: String?, onLanguageTagChange: (String?) -> Unit) {
    var sheetVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val current = remember(languageTag) { currentLanguageOption(context, languageTag) }

    SettingsRow(
        icon = { TranslateIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant) },
        title = stringResource(R.string.settings_language),
        subtitle = current.nativeName,
        onClick = { sheetVisible = true },
        testTag = SettingsTestTags.LANGUAGE_ROW,
    )

    if (sheetVisible) {
        LanguageBottomSheet(
            selectedTag = languageTag,
            onSelect = { tag ->
                onLanguageTagChange(tag)
                sheetVisible = false
            },
            onDismiss = { sheetVisible = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageBottomSheet(
    selectedTag: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.testTag(SettingsTestTags.LANGUAGE_SHEET)) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)) {
                Text(
                    text = stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_language_caption),
                    style = TierYourLifeType.current.captionUnderTitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(Modifier.selectableGroup()) {
                LanguageOptions.forEach { option ->
                    LanguageOptionRow(
                        option = option,
                        selected = option.persistTag == selectedTag,
                        onClick = { onSelect(option.persistTag) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(option: LanguageOption, selected: Boolean, onClick: () -> Unit) {
    val isDark = TierYourLifeMedia.current.isDark
    val background = if (selected) {
        if (isDark) LanguageOptionSelectedTintDark else LanguageOptionSelectedTintLight
    } else {
        Color.Transparent
    }
    val accent = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(background)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .testTag(SettingsTestTags.languageOption(option.persistTag))
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LanguageRadioDot(selected = selected, color = if (selected) accent else MaterialTheme.colorScheme.outline)
        Text(
            text = option.nativeName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(option.englishNameRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LanguageRadioDot(selected: Boolean, color: Color) {
    Canvas(Modifier.size(20.dp)) {
        val strokeWidth = 1.6.dp.toPx()
        drawCircle(color, radius = (size.minDimension - strokeWidth) / 2f, style = Stroke(strokeWidth))
        if (selected) {
            drawCircle(color, radius = size.minDimension / 4.5f)
        }
    }
}

@Composable
private fun TranslateIcon(iconSize: androidx.compose.ui.unit.Dp, color: Color) = VectorIcon(iconSize) { scale ->
    val stroke = 1.5f * scale
    drawCircle(color, radius = 9f * scale, center = Offset(12f * scale, 12f * scale), style = Stroke(stroke))
    drawLine(color, Offset(3f * scale, 12f * scale), Offset(21f * scale, 12f * scale), stroke, StrokeCap.Round)
    drawArc(
        color,
        startAngle = -90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(8f * scale, 3f * scale),
        size = androidx.compose.ui.geometry.Size(8f * scale, 18f * scale),
        style = Stroke(stroke),
    )
    drawArc(
        color,
        startAngle = 90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(8f * scale, 3f * scale),
        size = androidx.compose.ui.geometry.Size(8f * scale, 18f * scale),
        style = Stroke(stroke),
    )
}

@Composable
private fun TrashRow(trashCount: Int, onClick: () -> Unit) {
    val subtitle = if (trashCount == 0) {
        stringResource(R.string.settings_trash_empty)
    } else {
        pluralStringResource(R.plurals.list_items_count, trashCount, trashCount)
    }
    SettingsRow(
        icon = { DeleteOutlineIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant) },
        title = stringResource(R.string.settings_trash),
        subtitle = subtitle,
        onClick = onClick,
        testTag = SettingsTestTags.TRASH_ROW,
    )
}

@Composable
private fun ExportRow(onClick: () -> Unit) {
    SettingsRow(
        icon = { FileDownloadIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant) },
        title = stringResource(R.string.settings_export),
        subtitle = stringResource(R.string.settings_export_sub),
        onClick = onClick,
        testTag = SettingsTestTags.EXPORT_ROW,
    )
}

@Composable
private fun SettingsRow(
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
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = subtitle,
                style = TierYourLifeType.current.captionUnderTitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ChevronRightIcon(20.dp, MaterialTheme.colorScheme.outline)
    }
}

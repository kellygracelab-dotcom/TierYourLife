package com.artiuillab.tieryourlife.feature.tier.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.core.theme.preview.TierYourLifeDevicePreviews
import com.artiuillab.tieryourlife.core.theme.type.TierYourLifeType
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.tier.domain.export.TierListsExportStrings
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import com.artiuillab.tieryourlife.feature.tier.presentation.common.FileDownloadIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.common.OnResumeEffect
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.FlagIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.community.components.HideIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.components.AccountRow
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.components.LanguageRow
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.components.SettingsGroup
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.components.SettingsGroupDivider
import com.artiuillab.tieryourlife.feature.tier.presentation.settings.components.ThemeSection
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.BackIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.ChevronRightIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.DeleteOutlineIcon
import com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail.components.DeletedItemSnackbarHost
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onTrashClick: () -> Unit,
    onHiddenClick: () -> Unit,
    onModerationClick: () -> Unit,
    onAccountClick: () -> Unit,
    themeChoice: ThemeChoice,
    onThemeChoiceChange: (ThemeChoice) -> Unit,
    languageTag: String?,
    onLanguageTagChange: (String?) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val trashCount by viewModel.trashCount.collectAsStateWithLifecycle()
    val pendingReports by viewModel.pendingReports.collectAsStateWithLifecycle()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val credits by viewModel.credits.collectAsStateWithLifecycle()
    OnResumeEffect(onResume = viewModel::loadCredits)
    OnResumeEffect(onResume = viewModel::loadTrashCount)
    OnResumeEffect(onResume = viewModel::loadPendingReports)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val failedMessage = stringResource(R.string.snack_export_failed)
    val actionFailedMessage = stringResource(R.string.snack_action_failed)
    val tryAgainLabel = stringResource(R.string.action_try_again)
    val shareLabel = stringResource(R.string.action_share)
    val exportStrings = buildExportStrings(context)

    LaunchedEffect(Unit) {
        viewModel.userMessages.collect { snackbarHostState.showSnackbar(actionFailedMessage) }
    }

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
        account = account,
        credits = credits,
        onAccountClick = onAccountClick,
        versionName = versionName(context),
        themeChoice = themeChoice,
        onThemeChoiceChange = onThemeChoiceChange,
        languageTag = languageTag,
        onLanguageTagChange = onLanguageTagChange,
        trashCount = trashCount,
        pendingReports = pendingReports,
        onBack = onBack,
        onTrashClick = onTrashClick,
        onHiddenClick = onHiddenClick,
        onModerationClick = onModerationClick,
        onExportClick = { createDocumentLauncher.launch(defaultExportFileName(context)) },
        snackbarHostState = snackbarHostState,
    )
}

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
    }.onFailure { Timber.w(it, "Exporting lists failed") }.isSuccess

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
    account: Account,
    credits: Int?,
    onAccountClick: () -> Unit,
    versionName: String,
    themeChoice: ThemeChoice,
    onThemeChoiceChange: (ThemeChoice) -> Unit,
    languageTag: String?,
    onLanguageTagChange: (String?) -> Unit,
    trashCount: Int,
    pendingReports: Int? = null,
    onBack: () -> Unit,
    onTrashClick: () -> Unit,
    onHiddenClick: () -> Unit,
    onModerationClick: () -> Unit,
    onExportClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                SettingsTopBar(onBack = onBack)
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 32.dp),
                ) {
                    SettingsGroup(
                        title = stringResource(R.string.settings_account),
                        outlined = account is Account.Guest,
                    ) {
                        AccountRow(account = account, credits = credits, onClick = onAccountClick)
                    }
                    SettingsGroup(title = stringResource(R.string.settings_group_appearance)) {
                        ThemeSection(themeChoice = themeChoice, onThemeChoiceChange = onThemeChoiceChange)
                        SettingsGroupDivider()
                        LanguageRow(languageTag = languageTag, onLanguageTagChange = onLanguageTagChange)
                    }
                    SettingsGroup(title = stringResource(R.string.settings_group_data)) {
                        TrashRow(trashCount = trashCount, onClick = onTrashClick)
                        SettingsGroupDivider()
                        HiddenRow(onClick = onHiddenClick)
                        SettingsGroupDivider()
                        ExportRow(onClick = onExportClick)
                        if (pendingReports != null) {
                            SettingsGroupDivider()
                            ModerationRow(waiting = pendingReports, onClick = onModerationClick)
                        }
                    }
                    VersionLine(versionName)
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

@Composable
private fun VersionLine(versionName: String) {
    Text(
        text = String.format(stringResource(R.string.settings_version), versionName),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.Center,
    )
}

private fun versionName(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull().orEmpty()

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
private fun HiddenRow(onClick: () -> Unit) {
    SettingsRow(
        icon = { HideIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant) },
        title = stringResource(R.string.settings_hidden),
        subtitle = stringResource(R.string.settings_hidden_sub),
        onClick = onClick,
        testTag = SettingsTestTags.HIDDEN_ROW,
    )
}

@Composable
private fun ModerationRow(waiting: Int, onClick: () -> Unit) {
    SettingsRow(
        icon = { FlagIcon(24.dp, MaterialTheme.colorScheme.onSurfaceVariant) },
        title = stringResource(R.string.settings_moderation),
        subtitle = if (waiting == 0) {
            stringResource(R.string.settings_moderation_none)
        } else {
            pluralStringResource(R.plurals.moderation_waiting, waiting, waiting)
        },
        onClick = onClick,
        testTag = SettingsTestTags.MODERATION_ROW,
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
internal fun SettingsRow(
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
                style = TierYourLifeType.current.supportingLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ChevronRightIcon(20.dp, MaterialTheme.colorScheme.outline)
    }
}

@TierYourLifeDevicePreviews
@Composable
private fun SettingsScreenLightPreview() = TierYourLifeTheme(false) {
    SettingsScreenContent(
        account = Account.Guest,
        credits = null,
        onAccountClick = {},
        versionName = "1.0",
        themeChoice = ThemeChoice.SYSTEM,
        onThemeChoiceChange = {},
        languageTag = null,
        onLanguageTagChange = {},
        trashCount = 3,
        onBack = {},
        onTrashClick = {},
        onHiddenClick = {},
        onModerationClick = {},
        onExportClick = {},
    )
}

@TierYourLifeDevicePreviews
@Composable
private fun SettingsScreenDarkPreview() = TierYourLifeTheme(true) {
    SettingsScreenContent(
        account = Account.Guest,
        credits = null,
        onAccountClick = {},
        versionName = "1.0",
        themeChoice = ThemeChoice.SYSTEM,
        onThemeChoiceChange = {},
        languageTag = null,
        onLanguageTagChange = {},
        trashCount = 3,
        onBack = {},
        onTrashClick = {},
        onHiddenClick = {},
        onModerationClick = {},
        onExportClick = {},
    )
}

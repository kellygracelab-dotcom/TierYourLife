package com.artiuillab.tieryourlife.feature.tier.presentation.settings

internal object SettingsTestTags {
    const val BACK = "settings_back"
    const val ACCOUNT = "settings_account_row"
    const val ACCOUNT_ACTION = "settings_account_action"
    const val THEME_ROW = "settings_theme_row"
    const val THEME_LIGHT = "settings_theme_light"
    const val THEME_DARK = "settings_theme_dark"
    const val THEME_SYSTEM = "settings_theme_system"
    const val LANGUAGE_ROW = "settings_language_row"
    const val LANGUAGE_SHEET = "settings_language_sheet"
    const val TRASH_ROW = "settings_trash_row"
    const val HIDDEN_ROW = "settings_hidden_row"
    const val EXPORT_ROW = "settings_export_row"
    fun languageOption(tag: String?) = "settings_language_option_${tag ?: "default"}"
}

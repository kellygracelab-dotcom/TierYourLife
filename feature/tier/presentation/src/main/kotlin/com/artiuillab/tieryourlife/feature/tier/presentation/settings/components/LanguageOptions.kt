package com.artiuillab.tieryourlife.feature.tier.presentation.settings.components

import android.content.Context
import androidx.core.os.ConfigurationCompat
import com.artiuillab.tieryourlife.feature.tier.presentation.R

internal data class LanguageOption(
    val persistTag: String?,
    val matchTag: String,
    val nativeName: String,
    val englishNameRes: Int,
)

internal val LanguageOptions = listOf(
    LanguageOption(persistTag = null, matchTag = "en", nativeName = "English", englishNameRes = R.string.language_default),
    LanguageOption(persistTag = "uk", matchTag = "uk", nativeName = "Українська", englishNameRes = R.string.language_name_uk),
    LanguageOption(persistTag = "ru", matchTag = "ru", nativeName = "Русский", englishNameRes = R.string.language_name_ru),
    LanguageOption(persistTag = "es", matchTag = "es", nativeName = "Español", englishNameRes = R.string.language_name_es),
    LanguageOption(
        persistTag = "pt-BR",
        matchTag = "pt-BR",
        nativeName = "Português (Brasil)",
        englishNameRes = R.string.language_name_pt_br,
    ),
    LanguageOption(persistTag = "de", matchTag = "de", nativeName = "Deutsch", englishNameRes = R.string.language_name_de),
    LanguageOption(persistTag = "fr", matchTag = "fr", nativeName = "Français", englishNameRes = R.string.language_name_fr),
    LanguageOption(persistTag = "pl", matchTag = "pl", nativeName = "Polski", englishNameRes = R.string.language_name_pl),
    LanguageOption(persistTag = "tr", matchTag = "tr", nativeName = "Türkçe", englishNameRes = R.string.language_name_tr),
    LanguageOption(persistTag = "ja", matchTag = "ja", nativeName = "日本語", englishNameRes = R.string.language_name_ja),
    LanguageOption(persistTag = "ar", matchTag = "ar", nativeName = "العربية", englishNameRes = R.string.language_name_ar),
)

internal fun currentLanguageOption(context: Context, storedTag: String?): LanguageOption {
    if (storedTag != null) {
        return LanguageOptions.firstOrNull { it.persistTag == storedTag } ?: LanguageOptions.first()
    }
    val resolvedTag = ConfigurationCompat.getLocales(context.resources.configuration)[0]?.toLanguageTag()
        ?: return LanguageOptions.first()
    return LanguageOptions.firstOrNull { option ->
        resolvedTag.equals(option.matchTag, ignoreCase = true) ||
            resolvedTag.startsWith("${option.matchTag}-", ignoreCase = true)
    } ?: LanguageOptions.first()
}

package com.artiuillab.tieryourlife.feature.tier.presentation.settings

import android.content.Context
import androidx.core.os.ConfigurationCompat
import com.artiuillab.tieryourlife.feature.tier.presentation.R

// One row per language the chooser offers (docs/design-spec-home.md, section 7,
// subsection "2 - Language"), in the owner's own order — not alphabetical, not by
// speaker count. `nativeName` is deliberately a Kotlin literal, not a string resource:
// "The language names shown in the chooser are not translated - each language is
// written in its own script in every locale." Only the trailing English name
// (`englishNameRes`) and the "Default"/"Arabic - right-to-left" labels are resources,
// so they translate along with everything else in strings.xml.
internal data class LanguageOption(
    // Value persisted by AppPreferences.setLanguageTag and read back by languageTag().
    // Null only for the first entry: choosing it means "follow the system", not "force
    // English" - the spec gives English no separate "follow system" affordance the way
    // Theme has a third segment, so the row already at the top of the list (English,
    // because it is the default resource set) is the natural place to hang that meaning.
    val persistTag: String?,
    // The BCP-47 tag used to match this option against the device's resolved locale,
    // for the settings row's supporting line when languageTag() is null. The default
    // entry matches "en" so the row's example ("English") is exactly what a device
    // already running in English (and never having overridden anything) will show.
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
    LanguageOption(
        persistTag = "ar",
        matchTag = "ar",
        nativeName = "العربية",
        englishNameRes = R.string.language_name_ar,
    ),
)

// The option the Language row's supporting line should show. An explicit stored tag
// always wins; "follow system" (null) falls back to whatever locale Android actually
// resolved this app's resources to, so the row's own subtitle never contradicts what's
// on screen.
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

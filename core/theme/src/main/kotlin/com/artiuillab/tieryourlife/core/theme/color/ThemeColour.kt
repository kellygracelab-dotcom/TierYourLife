package com.artiuillab.tieryourlife.core.theme.color

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeTonalSpot

/**
 * The colour the app is drawn in (artboards 58–59). Light or dark stays a choice of its own; tier colours
 * S–D are the same in every colour.
 */
sealed interface ThemeColour {
    /** Today's look, by hand: the default. */
    data object Lavender : ThemeColour

    /** The wallpaper's colours, Android 12 and later; lavender before that. */
    data object Phone : ThemeColour

    /** Built from one hue, 0–359: the ready colours and one's own. */
    data class FromHue(val hue: Int) : ThemeColour
}

/**
 * Material Color Utilities as the proxy's vectors have them (CONTRACT.md, boards, Settings): SchemeTonalSpot,
 * spec 2021, contrast 0, from the seed Hct(hue, 36, 50). The site builds the same colours from the same file.
 */
fun hueColorScheme(hue: Int, dark: Boolean): ColorScheme {
    val seed = Hct.fromInt(Hct.from(hue.toDouble(), SEED_CHROMA, SEED_TONE).toInt())
    val scheme = SchemeTonalSpot(seed, dark, 0.0, ColorSpec.SpecVersion.SPEC_2021)
    val roles = MaterialDynamicColors()
    fun DynamicColor.of() = Color(getArgb(scheme))
    return lightColorScheme(
        primary = roles.primary.of(),
        onPrimary = roles.onPrimary.of(),
        primaryContainer = roles.primaryContainer.of(),
        onPrimaryContainer = roles.onPrimaryContainer.of(),
        inversePrimary = roles.inversePrimary.of(),
        secondary = roles.secondary.of(),
        onSecondary = roles.onSecondary.of(),
        secondaryContainer = roles.secondaryContainer.of(),
        onSecondaryContainer = roles.onSecondaryContainer.of(),
        tertiary = roles.tertiary.of(),
        onTertiary = roles.onTertiary.of(),
        tertiaryContainer = roles.tertiaryContainer.of(),
        onTertiaryContainer = roles.onTertiaryContainer.of(),
        error = roles.error.of(),
        onError = roles.onError.of(),
        errorContainer = roles.errorContainer.of(),
        onErrorContainer = roles.onErrorContainer.of(),
        background = roles.background.of(),
        onBackground = roles.onBackground.of(),
        surface = roles.surface.of(),
        onSurface = roles.onSurface.of(),
        surfaceVariant = roles.surfaceVariant.of(),
        onSurfaceVariant = roles.onSurfaceVariant.of(),
        surfaceTint = roles.surfaceTint.of(),
        surfaceDim = roles.surfaceDim.of(),
        surfaceBright = roles.surfaceBright.of(),
        surfaceContainerLowest = roles.surfaceContainerLowest.of(),
        surfaceContainerLow = roles.surfaceContainerLow.of(),
        surfaceContainer = roles.surfaceContainer.of(),
        surfaceContainerHigh = roles.surfaceContainerHigh.of(),
        surfaceContainerHighest = roles.surfaceContainerHighest.of(),
        inverseSurface = roles.inverseSurface.of(),
        inverseOnSurface = roles.inverseOnSurface.of(),
        outline = roles.outline.of(),
        outlineVariant = roles.outlineVariant.of(),
        scrim = roles.scrim.of(),
    )
}

/** The seed a hue makes, as its own colour: the swatch that stands for it. */
fun hueSeed(hue: Int): Color = Color(Hct.from(hue.toDouble(), SEED_CHROMA, SEED_TONE).toInt())

/** The hue as vivid as a screen shows it: the rainbow one's own colour is picked on. */
fun vividHue(hue: Int): Color = Color(Hct.from(hue.toDouble(), VIVID_CHROMA, VIVID_TONE).toInt())

/** A colour's light page, whatever the mode: its swatch looks the same in light and dark (artboard 59). */
fun swatchScheme(colour: ThemeColour, context: Context): ColorScheme = colorSchemeFor(colour, dark = false, context)

internal fun colorSchemeFor(colour: ThemeColour, dark: Boolean, context: Context): ColorScheme = when (colour) {
    ThemeColour.Lavender -> if (dark) DarkColorScheme else LightColorScheme
    is ThemeColour.FromHue -> hueColorScheme(colour.hue, dark)
    ThemeColour.Phone -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) DarkColorScheme else LightColorScheme
    }
}

/**
 * Lavender keeps its own hand-made tiles; a built colour takes them from its scheme, so a card's
 * placeholder and the pool's ribbon belong to the page they lie on.
 */
internal fun mediaColorsFor(colour: ThemeColour, dark: Boolean, scheme: ColorScheme): TierYourLifeMediaColors {
    val lavender = if (dark) DarkMediaColors else LightMediaColors
    if (colour == ThemeColour.Lavender) return lavender
    return lavender.copy(
        tilePlaceholder = if (dark) scheme.surfaceContainerHighest else scheme.surfaceVariant,
        tilePlaceholderAlt = if (dark) scheme.surfaceBright else scheme.surfaceContainerHighest,
        tileLabel = scheme.onSurfaceVariant,
        unrankedRibbon = if (dark) scheme.outlineVariant else scheme.surfaceVariant,
        onTierBand = if (dark) scheme.surface else lavender.onTierBand,
    )
}

private const val SEED_CHROMA = 36.0
private const val SEED_TONE = 50.0
private const val VIVID_CHROMA = 120.0
private const val VIVID_TONE = 60.0

/** The names the account keeps a colour by (CONTRACT.md, boards, Settings), and the ready colours' hues. */
object ReadyColours {
    const val LAVENDER = "lavender"
    const val MATCHA = "matcha"
    const val PEACH = "peach"
    const val SKY = "sky"
    const val PHONE = "phone"
    const val CUSTOM = "custom"

    private val HUES = mapOf(MATCHA to 125, PEACH to 42, SKY to 212)

    fun hueOf(name: String): Int? = HUES[name]

    /** A name this build does not know (one added later) shows as lavender, and the setting is kept as it is. */
    fun colourOf(name: String, hue: Int): ThemeColour = when (name) {
        PHONE -> ThemeColour.Phone
        CUSTOM -> ThemeColour.FromHue(hue.mod(FULL_TURN))
        else -> HUES[name]?.let { ThemeColour.FromHue(it) } ?: ThemeColour.Lavender
    }

    const val FULL_TURN = 360
}

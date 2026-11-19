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
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.dynamiccolor.Variant
import com.materialkolor.hct.Hct
import com.materialkolor.palettes.TonalPalette

/**
 * The colour the app is drawn in (artboards 58–59). Light or dark stays a choice of its own; tier colours
 * S–D are the same in every colour.
 */
sealed interface ThemeColour {
    /** Today's look, by hand: the default. */
    data object Lavender : ThemeColour

    /** The wallpaper's colours, Android 12 and later; lavender before that. */
    data object Phone : ThemeColour

    /** The Designer's ready colours: matcha, peach and sky, fixed tables. */
    data class Ready(val name: String) : ThemeColour

    /** One's own colour, built from one hue, 0–359. */
    data class FromHue(val hue: Int) : ThemeColour
}

/**
 * One's own colour as the proxy's vectors have it (CONTRACT.md, boards, Settings; theme-vectors.json,
 * algorithm.ownColour): a TONAL_SPOT scheme, spec 2021, contrast 0, over five palettes from the hue of the
 * seed Hct(hue, 36, 50), the neutrals a little stronger than SchemeTonalSpot's. The site builds the same.
 */
fun hueColorScheme(hue: Int, dark: Boolean): ColorScheme {
    val seed = Hct.fromInt(Hct.from(hue.toDouble(), SEED_CHROMA, SEED_TONE).toInt())
    val h = seed.hue
    fun palette(hueOf: Double, chroma: Double) = TonalPalette.fromHueAndChroma(hueOf, chroma)
    val scheme = DynamicScheme(
        sourceColorHct = seed,
        variant = Variant.TONAL_SPOT,
        isDark = dark,
        contrastLevel = 0.0,
        specVersion = ColorSpec.SpecVersion.SPEC_2021,
        primaryPalette = palette(h, SEED_CHROMA),
        secondaryPalette = palette(h, SECONDARY_CHROMA),
        tertiaryPalette = palette((h + TERTIARY_TURN).mod(FULL_TURN_DEGREES), TERTIARY_CHROMA),
        neutralPalette = palette(h, NEUTRAL_CHROMA),
        neutralVariantPalette = palette(h, NEUTRAL_VARIANT_CHROMA),
    )
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

/** A ready colour's table, laid out as [hueAppScheme] is. */
fun readyAppScheme(name: String, dark: Boolean): ColorScheme? = readyColorScheme(name, dark)?.laidOut(dark)

/**
 * A built colour as the app and the site lay it out (19.11): in light the page is surfaceContainerHigh and the
 * cards (surfaceContainerLow) lighter than it, and what lay on surfaceContainerHigh a step darker, so it still
 * stands out; in dark the page is surface and the cards surfaceContainer.
 * Lavender keeps its own page.
 */
fun hueAppScheme(hue: Int, dark: Boolean): ColorScheme = hueColorScheme(hue, dark).laidOut(dark)

private fun ColorScheme.laidOut(dark: Boolean): ColorScheme = let { scheme ->
    if (dark) {
        scheme.copy(surfaceContainerLow = scheme.surfaceContainer)
    } else {
        scheme.copy(
            surface = scheme.surfaceContainerHigh,
            background = scheme.surfaceContainerHigh,
            surfaceContainerHigh = scheme.surfaceContainerHighest,
        )
    }
}

/** The seed a hue makes, as its own colour: the swatch that stands for it. */
fun hueSeed(hue: Int): Color = Color(Hct.from(hue.toDouble(), SEED_CHROMA, SEED_TONE).toInt())

/** The hue as vivid as a screen shows it: the rainbow one's own colour is picked on. */
fun vividHue(hue: Int): Color = Color(Hct.from(hue.toDouble(), VIVID_CHROMA, VIVID_TONE).toInt())

/** A colour's light page, whatever the mode: its swatch looks the same in light and dark (artboard 59). */
fun swatchScheme(colour: ThemeColour, context: Context): ColorScheme = colorSchemeFor(colour, dark = false, context)

internal fun colorSchemeFor(colour: ThemeColour, dark: Boolean, context: Context): ColorScheme = when (colour) {
    ThemeColour.Lavender -> if (dark) DarkColorScheme else LightColorScheme
    is ThemeColour.Ready -> readyAppScheme(colour.name, dark) ?: if (dark) DarkColorScheme else LightColorScheme
    is ThemeColour.FromHue -> hueAppScheme(colour.hue, dark)
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
private const val SECONDARY_CHROMA = 16.0
private const val TERTIARY_TURN = 60.0
private const val FULL_TURN_DEGREES = 360.0
private const val TERTIARY_CHROMA = 24.0
private const val NEUTRAL_CHROMA = 8.0
private const val NEUTRAL_VARIANT_CHROMA = 10.0
private const val VIVID_CHROMA = 120.0
private const val VIVID_TONE = 60.0

/** The names the account keeps a colour by (CONTRACT.md, boards, Settings), and the ready colours' main hues. */
object ReadyColours {
    const val LAVENDER = "lavender"
    const val MATCHA = "matcha"
    const val PEACH = "peach"
    const val SKY = "sky"
    const val PHONE = "phone"
    const val CUSTOM = "custom"

    /** Each table's primary palette hue: where one's own colour starts from a ready one. */
    private val HUES = mapOf(MATCHA to 136, PEACH to 41, SKY to 220)

    fun hueOf(name: String): Int? = HUES[name]

    /** A name this build does not know (one added later) shows as lavender, and the setting is kept as it is. */
    fun colourOf(name: String, hue: Int): ThemeColour = when (name) {
        PHONE -> ThemeColour.Phone
        CUSTOM -> ThemeColour.FromHue(hue.mod(FULL_TURN))
        MATCHA, PEACH, SKY -> ThemeColour.Ready(name)
        else -> ThemeColour.Lavender
    }

    const val FULL_TURN = 360
}

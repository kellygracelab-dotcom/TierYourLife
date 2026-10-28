package com.artiuillab.tieryourlife.feature.tier.board.common

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionDirectory
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import java.text.Collator

/**
 * The sections this phone knows, for everything that names one, and the way
 * to ask about an id it does not. Provided once at the top of the app; with
 * no provider, as in a preview or a test, the nine built-ins.
 */
@Stable
class KnownSections(
    val directory: SectionDirectory = SectionDirectory(),
    val lookUp: (String) -> Unit = {},
)

val LocalSections = compositionLocalOf { KnownSections() }

/**
 * The section to draw for [id]. An id nobody here knows is asked about, once,
 * and drawn meanwhile with Other's look and words under its own id, so a tap
 * still goes where it should.
 */
@Composable
fun rememberSection(id: String): Section {
    val known = LocalSections.current
    val found = known.directory.find(id)
    if (found == null && !known.directory.settled(id)) {
        LaunchedEffect(id) { known.lookUp(id) }
    }
    return found ?: Section(id = id)
}

/** `names[locale] ?? name`; while the server has named nothing, the app's own words for a built-in, and Other's for anybody else. */
fun Section.labelWith(resources: Resources, languageTag: String): String =
    nameIn(languageTag) ?: resources.getString((art ?: ListCategory.Other).labelRes)

@Composable
@ReadOnlyComposable
fun Section.label(): String = labelWith(LocalResources.current, currentLanguageTag())

@Composable
fun sectionLabel(id: String): String = rememberSection(id).label()

/**
 * Names by id for words said later, a snackbar's: read when they are said,
 * from whatever the phone knows by then.
 */
@Composable
fun rememberSectionNames(): (String) -> String {
    val known = LocalSections.current
    val resources = LocalResources.current
    val tag = currentLanguageTag()
    val latest by rememberUpdatedState { id: String -> (known.directory.find(id) ?: Section(id = id)).labelWith(resources, tag) }
    return remember { { id -> latest(id) } }
}

/** The language the app is drawn in, as a tag the server's translations are keyed by. */
@Composable
@ReadOnlyComposable
fun currentLanguageTag(): String = LocalConfiguration.current.locales[0].toLanguageTag()

/**
 * What a list may be filed under, in an order that holds still: the built-ins
 * in their old order, then everybody else's by name, Other last. Only what
 * ordinary screens draw.
 */
@Composable
fun choosableSections(): List<Section> {
    val directory = LocalSections.current.directory
    val resources = LocalResources.current
    val tag = currentLanguageTag()
    return remember(directory, resources, tag) {
        val shown = directory.shown.ifEmpty { Section.BUILT_IN }
        val collator = Collator.getInstance(java.util.Locale.forLanguageTag(tag))
        val builtIns = ListCategory.entries.filter { it != ListCategory.Other }.mapNotNull { category -> shown.firstOrNull { it.id == category.id } }
        val theirs = shown.filter { it.art == null }.sortedWith(compareBy(collator) { it.labelWith(resources, tag) })
        builtIns + theirs + shown.filter { it.isOther }
    }
}

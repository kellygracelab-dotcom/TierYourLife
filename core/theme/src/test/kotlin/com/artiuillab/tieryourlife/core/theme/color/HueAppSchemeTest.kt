package com.artiuillab.tieryourlife.core.theme.color

import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A built colour's page and cards, as the site lays them out (coordinator, 19.11). */
class HueAppSchemeTest {

    @Test
    fun light_thePageIsContainerHigh_andTheCardsLighter() {
        val roles = hueColorScheme(MATCHA, dark = false)
        val app = hueAppScheme(MATCHA, dark = false)

        assertEquals(roles.surfaceContainerHigh, app.surface)
        assertEquals(roles.surfaceContainerHigh, app.background)
        assertEquals(roles.surfaceContainerLow, app.surfaceContainerLow)
        assertTrue(app.surfaceContainerLow.luminance() > app.surface.luminance())
        assertTrue(app.surfaceContainerHigh.luminance() < app.surface.luminance())
    }

    @Test
    fun dark_thePageIsSurface_andTheCardsContainer() {
        val roles = hueColorScheme(MATCHA, dark = true)
        val app = hueAppScheme(MATCHA, dark = true)

        assertEquals(roles.surface, app.surface)
        assertEquals(roles.surfaceContainer, app.surfaceContainerLow)
        assertEquals(roles.primary, app.primary)
    }

    @Test
    fun theReadyTables_areLaidOutTheSame() {
        val table = checkNotNull(readyColorScheme(ReadyColours.MATCHA, dark = false))
        val app = checkNotNull(readyAppScheme(ReadyColours.MATCHA, dark = false))

        assertEquals(table.surfaceContainerHigh, app.surface)
        assertEquals(table.surfaceContainerLow, app.surfaceContainerLow)
    }

    private companion object {
        const val MATCHA = 125
    }
}

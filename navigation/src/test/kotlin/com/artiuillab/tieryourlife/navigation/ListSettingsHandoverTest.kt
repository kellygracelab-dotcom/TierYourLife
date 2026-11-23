package com.artiuillab.tieryourlife.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The screens List settings hands over to come in solid, the settings going at once; by the routes navigation really produces. */
class ListSettingsHandoverTest {

    private val prefix = "com.artiuillab.tieryourlife.feature.tier.presentation.navigation.Route."

    @Test
    fun coverTopicAndAdjust_takeOverListSettings() {
        assertTrue(takesOverListSettings(prefix + "CoverPicker/{tierListId}"))
        assertTrue(takesOverListSettings(prefix + "TopicPicker/{tierListId}"))
        assertTrue(takesOverListSettings(prefix + "AdjustCover/{tierListId}?itemId={itemId}"))
    }

    @Test
    fun everythingElse_keepsTheUsualPush() {
        assertFalse(takesOverListSettings(prefix + "TierDetail/{tierListId}"))
        assertFalse(takesOverListSettings(prefix + "Trash"))
        assertFalse(takesOverListSettings(null))
    }
}

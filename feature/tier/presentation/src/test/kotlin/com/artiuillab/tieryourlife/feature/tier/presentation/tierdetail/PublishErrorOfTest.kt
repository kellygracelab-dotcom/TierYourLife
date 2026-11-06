package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishRefused
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class PublishErrorOfTest {

    @Test
    fun aRefusalSaysItsOwnReason_eitherWay() {
        assertEquals(PublishError.ListGone, publishErrorOf(PublishRefused(PublishError.ListGone), public = true))
        assertEquals(PublishError.ListGone, publishErrorOf(PublishRefused(PublishError.ListGone), public = false))
    }

    // Taking a list down that failed used to read as a publish that failed.
    @Test
    fun aTakedownThatFails_saysTheListIsStillPublic() {
        assertEquals(PublishError.NotTakenDown, publishErrorOf(IllegalStateException("500"), public = false))
        assertEquals(PublishError.Offline, publishErrorOf(IOException("offline"), public = false))
    }

    @Test
    fun aPublishThatFailsWithoutAReason_staysUnknown() {
        assertEquals(PublishError.Unknown, publishErrorOf(IllegalStateException("500"), public = true))
    }
}

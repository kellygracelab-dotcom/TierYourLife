package com.artiuillab.tieryourlife.feature.community.presentation.components

import com.artiuillab.tieryourlife.core.theme.messages.UserMessage
import com.artiuillab.tieryourlife.feature.community.domain.model.ReportsToday
import com.artiuillab.tieryourlife.feature.community.domain.model.TargetIsStaff
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class ReportFailureTest {

    // Proxy #173: 10 a day for a guest, 50 for an account.
    @Test
    fun theDaysLimit_isSaid_notAFailure() {
        assertEquals(UserMessage.ReportsToday, reportFailure(ReportsToday()))
        assertEquals(UserMessage.TargetIsStaff, reportFailure(TargetIsStaff()))
        assertEquals(UserMessage.ActionFailed, reportFailure(IOException()))
    }
}

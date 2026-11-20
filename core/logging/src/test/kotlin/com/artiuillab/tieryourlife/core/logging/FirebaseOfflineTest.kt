package com.artiuillab.tieryourlife.core.logging

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.ExecutionException

/** Firebase's own word for no connection; R8 renames it, so it is known by type, not by name. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FirebaseOfflineTest {

    @Test
    fun firebaseFindingNoNetwork_isNoConnection() {
        assertFalse(LogPolicy.isNonFatal(Log.WARN, ExecutionException(FirebaseNetworkException("A network error has occurred."))))
    }
}

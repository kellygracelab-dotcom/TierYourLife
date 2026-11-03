package com.artiuillab.tieryourlife

import android.os.StrictMode

/** Logs, never crashes: a read on the main thread is a stutter to find, not a reason to stop somebody testing. */
object MainThreadChecks {

    fun install() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectNetwork()
                .penaltyLog()
                .build(),
        )
    }
}

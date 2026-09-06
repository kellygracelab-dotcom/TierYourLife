package com.artiuillab.tieryourlife.feature.tier.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun OnResumeEffect(onResume: () -> Unit) {
    val currentOnResume by rememberUpdatedState(onResume)
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/**
 * A token that is new on every arrival and resume of this composition and
 * stays put while it lives. A screen that re-enters composition on a tab
 * switch cannot tell that from arriving; the screen that outlives the switch
 * can, and hands this down. Null until the first resume has happened.
 */
@Composable
fun rememberArrival(): Any? {
    var arrival by remember { mutableStateOf<Any?>(null) }
    OnResumeEffect { arrival = Any() }
    return arrival
}

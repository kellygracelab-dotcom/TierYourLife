package com.artiuillab.tieryourlife.feature.tier.presentation.tierdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.artiuillab.tieryourlife.feature.tier.board.components.rows.Publishing

/**
 * The bar's publish, signed in or not: signed out, it goes to sign in first and
 * carries on by itself once the account is there, as the person asked.
 */
@Composable
internal fun publishAfterSignIn(signedIn: Boolean, onSignIn: () -> Unit, publish: (Publishing) -> Unit): (Publishing) -> Unit {
    var waiting by rememberSaveable { mutableStateOf<Publishing?>(null) }
    val send by rememberUpdatedState(publish)
    LaunchedEffect(signedIn, waiting) {
        val kind = waiting
        if (signedIn && kind != null) {
            waiting = null
            send(kind)
        }
    }
    return { kind ->
        if (signedIn) {
            publish(kind)
        } else {
            waiting = kind
            onSignIn()
        }
    }
}

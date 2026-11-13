package com.artiuillab.tieryourlife.core.theme.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.artiuillab.tieryourlife.core.theme.R

/**
 * TMDB's mark above their wording, both required by the licence their
 * catalogue is read on. The mark is not tinted or themed (the terms forbid
 * recolouring it), and the words stay in English: a translated legal notice
 * is a different legal notice.
 */
@Composable
fun TmdbNotice(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(R.drawable.tmdb_logo),
            contentDescription = "The Movie Database",
            modifier = Modifier.height(16.dp),
        )
        Text(
            text = stringResource(R.string.tmdb_attribution),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )
    }
}

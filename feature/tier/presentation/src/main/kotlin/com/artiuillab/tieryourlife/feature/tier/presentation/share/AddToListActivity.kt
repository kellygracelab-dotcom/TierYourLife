package com.artiuillab.tieryourlife.feature.tier.presentation.share

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.core.settings.ThemeChoice
import com.artiuillab.tieryourlife.core.theme.TierYourLifeTheme
import com.artiuillab.tieryourlife.feature.tier.presentation.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * What the app is in a browser's Share list: a short sheet over the browser,
 * not the board, so the person stays where they were to take the next picture.
 */
@AndroidEntryPoint
class AddToListActivity : ComponentActivity() {

    @Inject
    lateinit var preferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val shared = sharedBy(intent)
        if (shared == null) {
            finish()
            return
        }
        setContent {
            val dark = when (preferences.themeChoice()) {
                ThemeChoice.LIGHT -> false
                ThemeChoice.DARK -> true
                ThemeChoice.SYSTEM -> isSystemInDarkTheme()
            }
            TierYourLifeTheme(darkTheme = dark) {
                AddToListSheet(
                    shared = shared,
                    onClose = ::finish,
                    onAdded = { list ->
                        Toast.makeText(this, getString(R.string.add_to_list_added, list), Toast.LENGTH_SHORT).show()
                        finish()
                    },
                )
            }
        }
    }
}

internal fun sharedBy(intent: Intent?): Shared? {
    if (intent?.action != Intent.ACTION_SEND) return null
    val image = if (intent.type?.startsWith("image/") == true) streamOf(intent)?.toString() else null
    val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
    val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: intent.getStringExtra(Intent.EXTRA_TITLE)
    if (image == null && text == null) return null
    return Shared(imageUri = image, text = text, subject = subject)
}

private fun streamOf(intent: Intent): Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
} else {
    @Suppress("DEPRECATION")
    intent.getParcelableExtra(Intent.EXTRA_STREAM)
}

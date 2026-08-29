package com.artiuillab.tieryourlife.feature.aistudio.data.credits

import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.api.CardImageApi
import com.artiuillab.tieryourlife.feature.aistudio.domain.credits.GenerationCredits
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Losing the count is not worth an error: it disappears from the top bar and
 * generation carries on, because the server checks again before spending.
 */
@Singleton
class ProxyGenerationCredits @Inject constructor(
    private val api: CardImageApi,
    private val preferences: AppPreferences,
) : GenerationCredits {

    override suspend fun remaining(): Int? = try {
        api.credits().credits.also { preferences.setLastKnownCredits(it) }
    } catch (_: IOException) {
        null
    } catch (_: HttpException) {
        null
    }

    override fun lastKnown(): Int? = preferences.lastKnownCredits()
}

package com.artiuillab.tieryourlife.feature.aistudio.data.credits

import com.artiuillab.tieryourlife.feature.aistudio.data.remote.api.CardImageApi
import com.artiuillab.tieryourlife.feature.aistudio.domain.credits.GenerationCredits
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A balance the studio only displays. Losing it is not worth an error: the
 * count disappears from the top bar and generation carries on, because the
 * server checks again before spending anything.
 */
@Singleton
class ProxyGenerationCredits @Inject constructor(
    private val api: CardImageApi,
) : GenerationCredits {

    override suspend fun remaining(): Int? = try {
        api.credits().credits
    } catch (e: IOException) {
        null
    } catch (e: retrofit2.HttpException) {
        null
    }
}

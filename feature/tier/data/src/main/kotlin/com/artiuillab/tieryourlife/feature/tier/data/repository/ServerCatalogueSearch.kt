package com.artiuillab.tieryourlife.feature.tier.data.repository

import com.artiuillab.tieryourlife.feature.tier.data.remote.api.PicturesApi
import com.artiuillab.tieryourlife.feature.tier.data.remote.dto.toDomain
import com.artiuillab.tieryourlife.feature.tier.domain.model.CatalogueSearchPage
import com.artiuillab.tieryourlife.feature.tier.domain.model.PictureScope
import com.artiuillab.tieryourlife.feature.tier.domain.repository.CatalogueSearchRepository
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.util.Locale
import javax.inject.Inject

/**
 * The server asks every catalogue a scope names, and the web; when it cannot
 * be reached, the phone asks the two it always could ([direct]), scope or no
 * scope, so a search still finds films.
 */
class ServerCatalogueSearch @Inject constructor(
    private val api: PicturesApi,
    private val direct: CatalogueSearchRepositoryImpl,
) : CatalogueSearchRepository {

    override suspend fun search(
        query: String,
        languageTag: String?,
        page: Int,
        scope: PictureScope,
        everywhere: Boolean,
    ): Result<CatalogueSearchPage> {
        val typed = query.trim()
        if (typed.length < MIN_QUERY) return direct.search(typed, languageTag, page)
        val language = languageTag ?: Locale.getDefault().toLanguageTag()
        val asked = try {
            Result.success(
                api.search(
                    query = typed,
                    scope = (if (everywhere) PictureScope.All else scope).id,
                    language = language,
                    page = page,
                    web = if (everywhere) "1" else null,
                ).toDomain(),
            )
        } catch (exception: IOException) {
            Timber.i(exception, "The server's picture search failed on the network; asking the catalogues directly")
            Result.failure(exception)
        } catch (exception: HttpException) {
            Timber.w(exception, "The server's picture search refused: HTTP ${exception.code()}; asking the catalogues directly")
            Result.failure(exception)
        } catch (exception: SerializationException) {
            Timber.w(exception, "The server's picture search answered in a shape this app does not read")
            Result.failure(exception)
        }
        return if (asked.isSuccess) asked else direct.search(typed, language, page)
    }

    private companion object {
        const val MIN_QUERY = 2
    }
}

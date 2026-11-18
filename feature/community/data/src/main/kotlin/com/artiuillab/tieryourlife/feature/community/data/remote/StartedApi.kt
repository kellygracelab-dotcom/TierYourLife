package com.artiuillab.tieryourlife.feature.community.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The account's lists begun, on the boards function. Only the caller's own: no uid in the address.
 * Writes answer with the whole response, as a refused one carries what the account holds.
 */
interface StartedApi {
    @GET("boards/started")
    suspend fun index(@Query("since") since: Long? = null): StartedIndexDto

    @PUT("boards/started/{listId}")
    suspend fun keep(@Path("listId") listId: String, @Body body: KeepStartedDto): Response<KeptStartedDto>

    @DELETE("boards/started/{listId}")
    suspend fun remove(
        @Path("listId") listId: String,
        @Query("reason") reason: String,
        @Query("basedOn") basedOn: Long?,
        @Query("changedAt") changedAt: Long,
    ): Response<KeptStartedDto>
}

@Serializable
data class StartedIndexDto(
    val started: List<StartedListDto> = emptyList(),
    val removed: List<StartedListDto> = emptyList(),
    val now: Long = 0,
)

/** A list begun as the account holds it, or, with [removed], the mark of one taken out of Continue. */
@Serializable
data class StartedListDto(
    val listId: String,
    val updatedAt: Long = 0,
    val changedAt: Long = 0,
    val listVersion: Long = 0,
    val itemCount: Int = 0,
    val tierCount: Int = 0,
    val title: String = "",
    val picture: String? = null,
    val pictures: List<String> = emptyList(),
    val category: String? = null,
    val rows: List<List<Int>> = emptyList(),
    val poolOrder: List<Int>? = null,
    val placed: Int = 0,
    val play: JsonElement? = null,
    val removed: String? = null,
)

/** No defaults: every field goes, a null one as null, which [basedOn] and [play] need to say. */
@Serializable
data class KeepStartedDto(
    val basedOn: Long?,
    val changedAt: Long,
    val listVersion: Long,
    val itemCount: Int,
    val tierCount: Int,
    val title: String,
    val picture: String?,
    val pictures: List<String>,
    val category: String?,
    val rows: List<List<Int>>,
    val poolOrder: List<Int>,
    val play: JsonElement?,
)

@Serializable
data class KeptStartedDto(val listId: String = "", val updatedAt: Long = 0, val placed: Int = 0, val removed: String? = null)

@Serializable
data class StartedConflictDto(val code: String = "", val started: StartedListDto? = null)

/** Why a list left Continue, as the account keeps it. */
object StartedRemoval {
    const val DISCARD = "discard"
    const val SAVED = "saved"
    const val DONE = "done"
}

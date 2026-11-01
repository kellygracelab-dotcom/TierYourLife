package com.artiuillab.tieryourlife.feature.community.data.remote.api

import com.artiuillab.tieryourlife.feature.community.data.remote.dto.AppointModeratorRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.FaceDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.FollowStateDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.FollowedAuthorsDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.ModerationQueueDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.ModeratorsDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.MoveListRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PendingTopicsDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishListRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedFeedDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedIdDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.PublishedListDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.RankingsPageDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.ReportRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.SearchMissDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.SectionFollowDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.SectionNameCheckDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.SectionProposalDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.SectionsDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.ShowRankingRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.ShownRankingDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.SuggestedAuthorsDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.TakeDownRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.TopicDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.TopicMergeRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.TopicNameRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.TopicSectionRequestDto
import com.artiuillab.tieryourlife.feature.community.data.remote.dto.TopicsDto
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CommunityApi {

    @GET("lists")
    suspend fun feed(
        @Query("category") category: String? = null,
        @Query("q") query: String? = null,
        @Query("author") author: String? = null,
        @Query("after") after: String? = null,
        @Query("sort") sort: String? = null,
        @Query("following") following: String? = null,
        @Query("topic") topic: String? = null,
    ): PublishedFeedDto

    @GET("lists/follow/{authorUid}")
    suspend fun followState(@Path("authorUid") authorUid: String): FollowStateDto

    @POST("lists/follow/{authorUid}")
    suspend fun follow(@Path("authorUid") authorUid: String)

    @DELETE("lists/follow/{authorUid}")
    suspend fun unfollow(@Path("authorUid") authorUid: String)

    @GET("lists/follow")
    suspend fun suggestedAuthors(): SuggestedAuthorsDto

    @GET("lists/following")
    suspend fun following(): FollowedAuthorsDto

    @GET("lists/sections")
    suspend fun sections(): SectionsDto

    @GET("lists/sections/name-check")
    suspend fun checkSectionName(@Query("name") name: String): SectionNameCheckDto

    @GET("lists/sections/{id}")
    suspend fun section(@Path("id") id: String): JsonObject

    @POST("lists/sections/proposals")
    suspend fun proposeSection(@Body request: SectionProposalDto): JsonObject

    @DELETE("lists/sections/proposals/{id}")
    suspend fun withdrawProposal(@Path("id") id: String)

    @POST("lists/sections/{id}/follow")
    suspend fun followSection(@Path("id") id: String): SectionFollowDto

    @DELETE("lists/sections/{id}/follow")
    suspend fun unfollowSection(@Path("id") id: String): SectionFollowDto

    @GET("lists/topics")
    suspend fun topics(
        @Query("q") query: String? = null,
        @Query("section") section: String? = null,
        @Query("limit") limit: Int? = null,
    ): TopicsDto

    @GET("lists/topics/mine")
    suspend fun myTopics(): TopicsDto

    @GET("lists/topics/pending")
    suspend fun pendingTopics(): PendingTopicsDto

    @GET("lists/topics/{id}")
    suspend fun topic(@Path("id") id: String): TopicDto

    @POST("lists/search-misses")
    suspend fun reportSearchMiss(@Body request: SearchMissDto)

    @POST("lists/topics/{id}/follow")
    suspend fun followTopic(@Path("id") id: String)

    @DELETE("lists/topics/{id}/follow")
    suspend fun unfollowTopic(@Path("id") id: String)

    @POST("lists/topics/{id}/approve")
    suspend fun approveTopic(@Path("id") id: String, @Body request: TopicSectionRequestDto): TopicDto

    @POST("lists/topics/{id}/rename")
    suspend fun renameTopic(@Path("id") id: String, @Body request: TopicNameRequestDto): TopicDto

    @POST("lists/topics/{id}/merge")
    suspend fun mergeTopic(@Path("id") id: String, @Body request: TopicMergeRequestDto): TopicDto

    @POST("lists/topics/{id}/reject")
    suspend fun rejectTopic(@Path("id") id: String): TopicDto

    @POST("lists/{id}/move")
    suspend fun moveList(@Path("id") id: String, @Body request: MoveListRequestDto)

    @GET("lists/moderators")
    suspend fun moderators(): ModeratorsDto

    @POST("lists/moderators/{uid}")
    suspend fun appointModerator(@Path("uid") uid: String, @Body request: AppointModeratorRequestDto)

    @DELETE("lists/moderators/{uid}")
    suspend fun removeModerator(@Path("uid") uid: String)

    @POST("lists/{id}/taken")
    suspend fun noteTaken(@Path("id") id: String)

    @GET("lists/{id}/rankings")
    suspend fun rankings(@Path("id") id: String, @Query("after") after: String? = null): RankingsPageDto

    @DELETE("lists/{id}/rankings/mine")
    suspend fun deleteMyRanking(@Path("id") id: String)

    @PUT("lists/{id}/rankings/mine")
    suspend fun showMyRanking(@Path("id") id: String, @Body request: ShowRankingRequestDto): ShownRankingDto

    @GET("lists/mine")
    suspend fun myPublished(): PublishedFeedDto

    @GET("lists/reports")
    suspend fun reports(): ModerationQueueDto

    @POST("lists/{id}/takedown")
    suspend fun takeDown(@Path("id") id: String, @Body ban: TakeDownRequestDto)

    @POST("lists/{id}/dismiss")
    suspend fun dismissReports(@Path("id") id: String)

    @GET("lists/{id}")
    suspend fun open(@Path("id") id: String): PublishedListDto

    @POST("lists")
    suspend fun publish(@Body request: PublishListRequestDto): PublishedIdDto

    @POST("lists/{id}")
    suspend fun republish(@Path("id") id: String, @Body request: PublishListRequestDto): PublishedIdDto

    @DELETE("lists/{id}")
    suspend fun unpublish(@Path("id") id: String)

    @POST("lists/face/{pictureId}")
    suspend fun makeFace(@Path("pictureId") pictureId: String): FaceDto

    @PATCH("lists")
    suspend fun refreshAuthor()

    @POST("lists/{id}/report")
    suspend fun report(@Path("id") id: String, @Body request: ReportRequestDto)
}

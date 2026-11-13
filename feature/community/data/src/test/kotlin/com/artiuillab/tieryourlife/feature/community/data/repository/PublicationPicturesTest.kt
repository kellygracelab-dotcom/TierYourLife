package com.artiuillab.tieryourlife.feature.community.data.repository

import com.artiuillab.tieryourlife.feature.community.domain.model.PublishError
import com.artiuillab.tieryourlife.feature.community.domain.model.PublishRefused
import com.artiuillab.tieryourlife.feature.tier.domain.model.Tier
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierItem
import com.artiuillab.tieryourlife.feature.tier.domain.model.TierList
import com.artiuillab.tieryourlife.feature.tier.domain.sync.OwnPictures
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PublicationPicturesTest {
    private fun pictures(uploaded: Set<String>) = object : OwnPictures {
        override fun pictureIdOf(imageUrl: String?) = imageUrl?.takeIf { it.startsWith("file:") }?.substringAfterLast('/')
        override suspend fun sendNow(pictureIds: List<String>) = uploaded
    }
    private fun board(url: String) = TierList(
        1, "My copy", listOf(Tier(1, "S", "#111111", "#EEEEEE", listOf(TierItem(1, "Card", url)))),
        takenFrom = "original", originVersion = 123,
    )

    @Test fun anUnuploadedNewCardPhotoRefusesBeforeAChangedCopyCanSilentlyBecomeARanking() = runBlocking {
        val refusal = runCatching { board("file:///new-photo").uploadedPublicationRequest(pictures(emptySet())) }.exceptionOrNull()
        assertEquals(PublishError.PictureRefused, (refusal as PublishRefused).error)
    }

    @Test fun anUploadedCardPhotoKeepsTheExactContentTheDerivativePreviewClassified() = runBlocking {
        val request = board("file:///new-photo").uploadedPublicationRequest(pictures(setOf("new-photo")))
        assertEquals("new-photo", request.items.single().pictureId)
        assertEquals(123L, request.sourceVersion)
    }

    @Test fun anUnavailableCosmeticCoverDoesNotChangeTheCardContentOrBlockAnOrdinaryRanking() = runBlocking {
        val request = board("https://example.test/public.jpg").copy(coverImageUrl = "file:///cover").uploadedPublicationRequest(pictures(emptySet()))
        assertEquals("https://example.test/public.jpg", request.items.single().imageUrl)
        assertNull(request.coverPictureId)
    }

    @Test fun anIndependentNewListKeepsItsExistingPhotoFallbackBehavior() = runBlocking {
        val request = board("file:///new-photo").copy(takenFrom = null, originVersion = null).uploadedPublicationRequest(pictures(emptySet()))
        assertNull(request.items.single().pictureId)
    }
}

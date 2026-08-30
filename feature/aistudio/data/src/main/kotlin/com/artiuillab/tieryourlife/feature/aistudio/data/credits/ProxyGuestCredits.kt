package com.artiuillab.tieryourlife.feature.aistudio.data.credits

import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.account.domain.repository.GuestCredits
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.api.CardImageApi
import com.artiuillab.tieryourlife.feature.aistudio.data.remote.dto.AdoptGuestCreditsRequestDto
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lives here rather than beside the rest of the account because this is the
 * ledger talking, and the ledger has exactly one door: the proxy. The account
 * knows when to ask, not how.
 */
@Singleton
class ProxyGuestCredits @Inject constructor(
    private val api: CardImageApi,
    private val preferences: AppPreferences,
) : GuestCredits {

    // A balance that did not move is not a failure, and neither is a request
    // that never arrived: the guest's credits stay where they are and the next
    // sign-in can try again. Saying so out loud would be an error message
    // about something nobody asked for.
    override suspend fun carryOver(guestIdToken: String): Boolean = try {
        val adopted = api.adoptGuestCredits(AdoptGuestCreditsRequestDto(guestIdToken))
        adopted.credits?.let(preferences::setLastKnownCredits)
        adopted.moved
    } catch (e: IOException) {
        Timber.d(e, "Could not carry the guest balance over")
        false
    } catch (e: HttpException) {
        Timber.d(e, "The server would not carry the guest balance over")
        false
    }
}

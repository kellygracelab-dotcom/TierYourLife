package com.artiuillab.tieryourlife.feature.community.presentation.follow

import com.artiuillab.tieryourlife.feature.account.domain.FakeAccountRepository

/** Follows as a signed-in reader makes them: at once, nothing waiting. */
internal fun signedInFollows(): FollowAfterSignIn = FollowAfterSignIn(FakeAccountRepository(FakeAccountRepository.signedIn))

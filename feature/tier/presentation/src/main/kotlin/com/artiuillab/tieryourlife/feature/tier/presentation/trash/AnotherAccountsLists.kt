package com.artiuillab.tieryourlife.feature.tier.presentation.trash

import com.artiuillab.tieryourlife.core.settings.AppPreferences
import com.artiuillab.tieryourlife.feature.account.domain.model.Account
import com.artiuillab.tieryourlife.feature.account.domain.repository.AccountRepository
import kotlinx.coroutines.flow.first

/**
 * The published ids on this phone another account put out. Only their author
 * can take one down, so deleting the board here leaves it public and theirs,
 * in their account. Empty until somebody is signed in: then nobody is known
 * to be somebody else.
 */
internal suspend fun publishedByAnotherAccount(accounts: AccountRepository, preferences: AppPreferences): Set<String> {
    val account = (accounts.account.first() as? Account.SignedIn)?.recordKey ?: return emptySet()
    return preferences.publishedOwners().filterValues { it != account }.keys
}

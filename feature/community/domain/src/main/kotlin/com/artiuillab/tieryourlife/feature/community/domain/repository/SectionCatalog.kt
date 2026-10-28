package com.artiuillab.tieryourlife.feature.community.domain.repository

import com.artiuillab.tieryourlife.feature.community.domain.model.Section
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionDirectory
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionProposal
import kotlinx.coroutines.flow.StateFlow

/** The sections every screen names and draws from, kept on the phone between runs. */
interface SectionCatalog {

    /** The last answer, or the nine built-ins while there has never been one. */
    val directory: StateFlow<SectionDirectory>

    /** Reads the list again when the last answer is older than ten minutes, or at once with [force]. */
    suspend fun refresh(force: Boolean = false): Result<Unit>

    /** Asks the server about an id the directory does not know: once, however many screens meet it. */
    fun lookUp(id: String)

    /** Redrawn in the same frame and put back if the server refuses. */
    suspend fun setFollowing(id: String, now: Boolean): Result<Unit>

    /** Sends a proposal; once the server has it, it waits first on this person's screens. Fails with [com.artiuillab.tieryourlife.feature.community.domain.model.ProposalRefused]. */
    suspend fun propose(proposal: SectionProposal): Result<Section>

    /** Withdraws a waiting proposal, or dismisses the notice of a declined or merged one. */
    suspend fun withdraw(id: String): Result<Unit>
}

/** Where the last answer lives between runs of the app. */
interface SectionStore {

    /** Null when nothing was ever kept, or what was kept no longer reads. */
    suspend fun read(): SectionDirectory?

    suspend fun write(directory: SectionDirectory)
}

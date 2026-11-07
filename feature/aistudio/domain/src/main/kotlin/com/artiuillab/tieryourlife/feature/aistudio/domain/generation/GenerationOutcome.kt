package com.artiuillab.tieryourlife.feature.aistudio.domain.generation

import com.artiuillab.tieryourlife.feature.aistudio.domain.model.GeneratedCardImage

sealed interface GenerationOutcome {

    /** [creditsRemaining] is null where generation is not metered — the local stub. */
    data class Success(
        val image: GeneratedCardImage,
        val creditsRemaining: Int? = null,
    ) : GenerationOutcome

    data object OutOfCredits : GenerationOutcome

    /** Another picture of this person's is still being made. */
    data object Busy : GenerationOutcome

    /** Generation is switched off or at its daily limit for now. */
    data object Paused : GenerationOutcome

    data object Failed : GenerationOutcome
}

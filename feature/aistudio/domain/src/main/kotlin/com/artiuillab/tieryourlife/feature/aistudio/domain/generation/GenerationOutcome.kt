package com.artiuillab.tieryourlife.feature.aistudio.domain.generation

import com.artiuillab.tieryourlife.feature.aistudio.domain.model.GeneratedCardImage

/**
 * How a generation ended. Running out of credits is not a failure of the same
 * kind as a dropped connection: one is answered by waiting or paying, the other
 * by trying again. The screen has to tell them apart, so the port does too.
 */
sealed interface GenerationOutcome {

    /** [creditsRemaining] is null where generation is not metered — the local stub. */
    data class Success(
        val image: GeneratedCardImage,
        val creditsRemaining: Int? = null,
    ) : GenerationOutcome

    data object OutOfCredits : GenerationOutcome

    data object Failed : GenerationOutcome
}

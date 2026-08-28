package com.artiuillab.tieryourlife.feature.aistudio.domain.generation

import com.artiuillab.tieryourlife.feature.aistudio.domain.model.GeneratedCardImage

sealed interface GenerationOutcome {

    /** [creditsRemaining] is null where generation is not metered — the local stub. */
    data class Success(
        val image: GeneratedCardImage,
        val creditsRemaining: Int? = null,
    ) : GenerationOutcome

    data object OutOfCredits : GenerationOutcome

    data object Failed : GenerationOutcome
}

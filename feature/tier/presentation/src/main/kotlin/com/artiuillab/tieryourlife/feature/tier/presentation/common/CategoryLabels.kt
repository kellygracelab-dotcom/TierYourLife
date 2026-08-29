package com.artiuillab.tieryourlife.feature.tier.presentation.common

import androidx.annotation.StringRes
import com.artiuillab.tieryourlife.feature.tier.domain.model.ListCategory
import com.artiuillab.tieryourlife.feature.tier.presentation.R

@get:StringRes
internal val ListCategory.labelRes: Int
    get() = when (this) {
        ListCategory.Anime -> R.string.category_anime
        ListCategory.FilmTv -> R.string.category_film_tv
        ListCategory.Games -> R.string.category_games
        ListCategory.Music -> R.string.category_music
        ListCategory.Food -> R.string.category_food
        ListCategory.Sport -> R.string.category_sport
        ListCategory.People -> R.string.category_people
        ListCategory.Other -> R.string.category_other
    }

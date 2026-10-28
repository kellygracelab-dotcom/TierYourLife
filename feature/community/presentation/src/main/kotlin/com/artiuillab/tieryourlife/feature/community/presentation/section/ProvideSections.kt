package com.artiuillab.tieryourlife.feature.community.presentation.section

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiuillab.tieryourlife.feature.community.domain.model.SectionDirectory
import com.artiuillab.tieryourlife.feature.community.domain.repository.SectionCatalog
import com.artiuillab.tieryourlife.feature.tier.board.common.KnownSections
import com.artiuillab.tieryourlife.feature.tier.board.common.LocalSections
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** The catalog, for the one place at the top of the app that hands it to every screen. */
@HiltViewModel
class KnownSectionsViewModel @Inject constructor(
    private val catalog: SectionCatalog,
) : ViewModel() {
    val directory: StateFlow<SectionDirectory> = catalog.directory

    fun lookUp(id: String) = catalog.lookUp(id)
}

/**
 * Every screen inside names and draws sections from what the phone knows of
 * them: the kept list at first, the server's once it answers, and an id met
 * for the first time once the server has said what it is.
 */
@Composable
fun ProvideSections(content: @Composable () -> Unit) {
    val model: KnownSectionsViewModel = hiltViewModel()
    val directory by model.directory.collectAsStateWithLifecycle()
    val known = remember(directory, model) { KnownSections(directory, model::lookUp) }
    CompositionLocalProvider(LocalSections provides known, content = content)
}

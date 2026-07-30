package com.khiemnph.simpletracking.ui.runs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.ui.theme.ChayNgayDiTheme
import com.khiemnph.simpletracking.settings.UserPreferences
import com.khiemnph.simpletracking.settings.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Start destination for [com.khiemnph.simpletracking.ui.MainActivity]'s Navigation graph: the
 * session-history feed, plus a Record button that starts a brand-new session (no sessionId yet -
 * [com.khiemnph.simpletracking.ui.record.RecordFragment] fills one in once `StartSessionUseCase`
 * runs).
 *
 * The screen itself is Compose, hosted in a [ComposeView] rather than a Compose NavHost, so
 * Navigation Component and Safe Args keep working unchanged while Record is still a View-based
 * screen. [RunsViewModel] is untouched by the move: it already exposed a `StateFlow` of
 * pre-formatted models, which [collectAsStateWithLifecycle] consumes directly.
 */
@AndroidEntryPoint
class RunsFragment : Fragment() {

    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    private val viewModel: RunsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        // Disposes the composition with the Fragment's view, not the Fragment: without this the
        // composition outlives onDestroyView and leaks on every navigation.
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val preferences by userPreferencesRepository.preferences
                .collectAsStateWithLifecycle(UserPreferences())
            ChayNgayDiTheme(dynamicColour = preferences.dynamicColour) {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                RunsScreen(
                    state = state,
                    onSessionSwipedAway = viewModel::onSessionSwipedAway,
                    onUndoDelete = viewModel::onUndoDelete,
                    onSettingsClick = {
                        findNavController().navigate(R.id.action_runsFragment_to_settingsFragment)
                    },
                    onSessionClick = { sessionId ->
                        findNavController().navigate(
                            RunsFragmentDirections.actionRunsFragmentToSessionDetailFragment(sessionId),
                        )
                    },
                    onRecordClick = {
                        findNavController().navigate(
                            RunsFragmentDirections.actionRunsFragmentToRecordFragment(sessionId = null),
                        )
                    },
                )
            }
        }
    }
}

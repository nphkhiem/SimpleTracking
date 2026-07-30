package com.khiemnph.simpletracking.ui.summary

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
import dagger.hilt.android.AndroidEntryPoint

/**
 * The post-run moment, reached from Stop.
 *
 * Both actions land back on Runs. The navigation action that got here already popped Record off the
 * stack, so a plain `popBackStack` is enough and Back does the same thing as Save, which is the
 * right behaviour: the run is saved either way, and only Delete is destructive.
 */
@AndroidEntryPoint
class SummaryFragment : Fragment() {

    private val viewModel: SummaryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ChayNgayDiTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                SummaryScreen(
                    state = state,
                    onKeep = { backToRuns() },
                    onDiscard = { viewModel.onDiscard(::backToRuns) },
                )
            }
        }
    }

    private fun backToRuns() {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.summaryFragment) navController.popBackStack()
    }
}

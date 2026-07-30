package com.khiemnph.simpletracking.ui.detail

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

/** A recorded run in full, reached by tapping a row in Runs. */
@AndroidEntryPoint
class SessionDetailFragment : Fragment() {

    private val viewModel: SessionDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ChayNgayDiTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                SessionDetailScreen(
                    state = state,
                    onBack = ::leave,
                    onDelete = { viewModel.onDelete(::leave) },
                )
            }
        }
    }

    /** Guarded so a delete that resolves after the screen has already gone cannot pop twice. */
    private fun leave() {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.sessionDetailFragment) navController.popBackStack()
    }
}

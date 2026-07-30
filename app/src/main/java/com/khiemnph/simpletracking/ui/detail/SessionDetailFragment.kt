package com.khiemnph.simpletracking.ui.detail

import android.content.Intent
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

/** A recorded run in full, reached by tapping a row in Runs. */
@AndroidEntryPoint
class SessionDetailFragment : Fragment() {

    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    private val viewModel: SessionDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val preferences by userPreferencesRepository.preferences
                .collectAsStateWithLifecycle(UserPreferences())
            ChayNgayDiTheme(dynamicColour = preferences.dynamicColour) {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                SessionDetailScreen(
                    state = state,
                    onBack = ::leave,
                    onRename = viewModel::onRename,
                    onShare = { (state as? SessionDetailUiState.Ready)?.let(::share) },
                    onDelete = { viewModel.onDelete(::leave) },
                )
            }
        }
    }

    /**
     * Hands the run to the OS share sheet as plain text.
     *
     * Text rather than an image or a file: it needs no storage permission, no FileProvider and no
     * rendering step, and it pastes usefully into every target. The brief rules out a social layer,
     * so sharing is the system's job, not the app's.
     */
    private fun share(state: SessionDetailUiState.Ready) {
        val body = getString(
            R.string.detail_share_body,
            state.distanceKm,
            state.durationLabel,
            state.averagePaceLabel,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.detail_share_subject))
            putExtra(Intent.EXTRA_TEXT, body)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.detail_share)))
    }

    /** Guarded so a delete that resolves after the screen has already gone cannot pop twice. */
    private fun leave() {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.sessionDetailFragment) navController.popBackStack()
    }
}

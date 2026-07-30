package com.khiemnph.simpletracking.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.Text
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.khiemnph.simpletracking.ui.theme.ChayNgayDiTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * A recorded run in full, built in P4.
 *
 * Placeholder. The destination exists so the navigation graph settles in one change, which means
 * the phase that builds this screen never has to touch `nav_graph.xml` and cannot collide with
 * the phases building the other new screens.
 */
@AndroidEntryPoint
class SessionDetailFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { ChayNgayDiTheme { Text("Session detail") } }
    }
}

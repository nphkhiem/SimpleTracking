package com.khiemnph.simpletracking.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.khiemnph.domain.interactor.ObserveActiveSessionUseCase
import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.databinding.ActivityMainBinding
import com.khiemnph.simpletracking.ui.history.HistoryFragmentDirections
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Single-Activity shell hosting the app's Navigation graph. The one piece of real logic here:
 * on every [onStart] - not just [onCreate], since reopening the app from Recents re-triggers
 * onStart on an already-alive instance rather than a fresh onCreate - it takes a one-shot check
 * for an active tracking session and, if one exists and the user isn't already on
 * [com.khiemnph.simpletracking.ui.record.RecordFragment], routes straight there instead of
 * leaving the user stranded on History mid-session. If the user is already on RecordFragment,
 * this deliberately does nothing so it never fights manual back-navigation.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var observeActiveSessionUseCase: ObserveActiveSessionUseCase

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            try {
                val activeSession = observeActiveSessionUseCase().first()
                val navController = navController()
                if (activeSession != null && navController.currentDestination?.id != R.id.recordFragment) {
                    navController.navigate(
                        HistoryFragmentDirections.actionHistoryFragmentToRecordFragment(activeSession.session.id),
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                // Non-fatal: skip the recovery-navigation attempt for this onStart cycle and let
                // the user land on/stay on HistoryFragment, which is always a safe fallback.
                Log.e(TAG, "Failed to check for an active session on startup", error)
            }
        }
    }

    private fun navController(): NavController {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController
    }

    private companion object {
        private const val TAG = "MainActivity"
    }
}

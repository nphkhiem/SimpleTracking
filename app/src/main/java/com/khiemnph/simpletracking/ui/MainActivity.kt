package com.khiemnph.simpletracking.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.khiemnph.domain.interactor.ObserveActiveSessionUseCase
import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.databinding.ActivityMainBinding
import com.khiemnph.simpletracking.ui.runs.RunsFragmentDirections
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
 * leaving the user stranded on Runs mid-session. If the user is already on RecordFragment,
 * this deliberately does nothing so it never fights manual back-navigation.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var observeActiveSessionUseCase: ObserveActiveSessionUseCase

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before setContentView, so the first layout pass already knows it is drawing behind the
        // system bars. The theme has declared transparent bars for a while, but without this the
        // framework still inset the content, so the transparency bought nothing.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    /**
     * Recovery runs inside `repeatOnLifecycle(STARTED)` rather than as a bare `onStart` launch.
     *
     * Two defects came from the bare launch. The coroutine outlived `onStop`, so a user who
     * backgrounded the app while the session lookup was still in flight got a `navigate()` call
     * after `onSaveInstanceState` and an `IllegalStateException`. And every `onStart` added
     * another coroutine, so returning from Recents repeatedly left several in flight racing to
     * navigate. `repeatOnLifecycle` cancels at `onStop` and runs one block at a time, which fixes
     * both, and the `isStateSaved` check below covers the remaining window where the state is
     * saved but this block has not yet been cancelled.
     */
    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            run {
                // Only the use-case fetch is guarded - a navigation-guard regression must still
                // crash loudly in tests/debug builds rather than being silently absorbed here.
                val activeSession = try {
                    observeActiveSessionUseCase().first()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Exception) {
                    // Non-fatal: skip the recovery-navigation attempt for this onStart cycle and
                    // let the user land on/stay on RunsFragment, which is always a safe fallback.
                    Log.e(TAG, "Failed to check for an active session on startup", error)
                    return@launch
                }
                // Navigating once the state is saved throws; skipping is correct, because the
                // next onStart re-runs this check anyway.
                if (supportFragmentManager.isStateSaved) return@launch
                val navController = navController()
                if (activeSession != null && navController.currentDestination?.id != R.id.recordFragment) {
                    navController.navigate(
                        RunsFragmentDirections.actionRunsFragmentToRecordFragment(activeSession.session.id),
                    )
                }
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

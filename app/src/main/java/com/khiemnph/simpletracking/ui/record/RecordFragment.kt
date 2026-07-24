package com.khiemnph.simpletracking.ui.record

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.domain.model.SessionStatus
import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.databinding.FragmentRecordBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlinx.coroutines.launch

private const val METERS_PER_KILOMETER = 1_000.0
private const val MPS_TO_KMH_FACTOR = 3.6f
private const val SECONDS_PER_MILLIS_DIVISOR = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
private const val MAP_INITIAL_ZOOM = 17f
private const val ROUTE_POLYLINE_WIDTH_PX = 8f

/**
 * The live-tracking destination, reached either from
 * [com.khiemnph.simpletracking.ui.history.HistoryFragment]'s Record button (a brand-new session,
 * [args]' `sessionId` is null) or from [com.khiemnph.simpletracking.ui.MainActivity]'s cold-start
 * active-session recovery / an already-active session (`sessionId` is a concrete id, skipping the
 * permission check below entirely since it was already granted when that session started).
 *
 * Pause/Resume/Stop never call `:domain` use cases from here - [RecordViewModel] routes every one
 * of them through [com.khiemnph.simpletracking.service.TrackingService] intents instead, exactly
 * like the Service's own notification actions do.
 */
@AndroidEntryPoint
class RecordFragment : Fragment() {

    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!

    val args: RecordFragmentArgs by navArgs()

    private val viewModel: RecordViewModel by viewModels()

    private var googleMap: GoogleMap? = null
    private var hasCenteredCameraOnce = false

    /**
     * Must be registered unconditionally at construction time - before `onCreate`/`onAttach`
     * complete - regardless of whether this particular screen instance ends up needing to launch
     * it (Android requires the launcher exist before the Fragment reaches `CREATED`).
     */
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (requiredPermissions().all { grants[it] == true }) {
            viewModel.resolveSession(null)
        } else {
            view?.let {
                Snackbar.make(it, R.string.record_location_permission_denied_message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpMap(savedInstanceState)
        setUpBottomSheet()

        binding.recordBackButton.setOnClickListener { findNavController().popBackStack() }
        binding.recordPauseResumeButton.setOnClickListener { viewModel.onPauseOrResumeClicked() }
        binding.recordStopButton.setOnClickListener { handleStopClicked() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> renderUiState(state) }
            }
        }

        resolveSessionRespectingPermissions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        googleMap = null
        _binding = null
    }

    /** Test seam: exposes the configured behavior so a test can assert `isHideable == false`. */
    internal fun bottomSheetBehavior(): BottomSheetBehavior<View> = BottomSheetBehavior.from(binding.recordBottomSheet)

    private fun setUpBottomSheet() {
        bottomSheetBehavior().apply {
            // Pinned/non-dismissable: the sheet must never be swipeable away from the screen.
            isHideable = false
            isDraggable = false
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun setUpMap(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.record_map_container, SupportMapFragment.newInstance())
                .commit()
        }
        val mapFragment = childFragmentManager.findFragmentById(R.id.record_map_container) as? SupportMapFragment
        mapFragment?.getMapAsync { map ->
            googleMap = map
            renderRoute(viewModel.uiState.value.route)
        }
    }

    private fun resolveSessionRespectingPermissions() {
        val existingSessionId = args.sessionId
        if (existingSessionId != null) {
            viewModel.resolveSession(existingSessionId)
            return
        }
        if (hasAllRequiredPermissions()) {
            viewModel.resolveSession(null)
        } else {
            permissionLauncher.launch(requiredPermissions())
        }
    }

    private fun hasAllRequiredPermissions(): Boolean = requiredPermissions().all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requiredPermissions(): Array<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    private fun handleStopClicked() {
        val map = googleMap
        if (map != null) {
            map.snapshot { bitmap -> viewModel.onStopClicked(bitmap) }
        } else {
            viewModel.onStopClicked(null)
        }
        findNavController().popBackStack()
    }

    private fun renderUiState(state: RecordUiState) {
        val isPaused = state.status == SessionStatus.PAUSED

        binding.recordDistanceValue.text = formatDistanceKm(state.distanceMeters)
        binding.recordCurrentSpeedValue.text = formatSpeedKmh(state.currentSpeedMps)
        binding.recordDurationValue.text = formatDuration(state.elapsedDurationMillis)

        binding.recordPausedTag.visibility = if (isPaused) View.VISIBLE else View.GONE
        binding.recordPauseResumeButton.setImageResource(if (isPaused) R.drawable.ic_play else R.drawable.ic_pause)
        binding.recordPauseResumeButton.contentDescription = getString(
            if (isPaused) R.string.record_resume_content_description else R.string.record_pause_content_description,
        )

        renderRoute(state.route)
    }

    /** Clears and redraws Start/Current markers and the route polyline - simplest correct
     * approach for a route that only ever grows, at this app's scale of points per session. */
    private fun renderRoute(route: List<LatLngPoint>) {
        val map = googleMap ?: return
        map.clear()
        if (route.isEmpty()) return

        val latLngRoute = route.map { LatLng(it.latitude, it.longitude) }
        map.addPolyline(
            PolylineOptions()
                .addAll(latLngRoute)
                .color(ContextCompat.getColor(requireContext(), R.color.history_accent))
                .width(ROUTE_POLYLINE_WIDTH_PX),
        )
        map.addMarker(MarkerOptions().position(latLngRoute.first()).title(getString(R.string.record_start_marker_title)))
        map.addMarker(MarkerOptions().position(latLngRoute.last()).title(getString(R.string.record_current_marker_title)))

        val current = latLngRoute.last()
        if (!hasCenteredCameraOnce) {
            hasCenteredCameraOnce = true
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(current, MAP_INITIAL_ZOOM))
        } else {
            map.animateCamera(CameraUpdateFactory.newLatLng(current))
        }
    }

    private fun formatDistanceKm(distanceMeters: Double): String =
        String.format(Locale.US, "%.2f", distanceMeters / METERS_PER_KILOMETER)

    private fun formatSpeedKmh(speedMps: Float): String =
        String.format(Locale.US, "%.1f", speedMps * MPS_TO_KMH_FACTOR)

    private fun formatDuration(durationMillis: Long): String {
        val totalSeconds = durationMillis / SECONDS_PER_MILLIS_DIVISOR
        val hours = totalSeconds / SECONDS_PER_HOUR
        val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }
}

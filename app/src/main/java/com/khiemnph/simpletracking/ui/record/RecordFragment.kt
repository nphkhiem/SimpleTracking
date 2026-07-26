package com.khiemnph.simpletracking.ui.record

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.khiemnph.domain.model.GpsSignal
import com.khiemnph.domain.model.LatLngPoint
import com.khiemnph.domain.model.SessionStatus
import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.databinding.FragmentRecordBinding
import com.khiemnph.simpletracking.location.LocationSettingsChecker
import com.khiemnph.simpletracking.location.LocationSettingsResult
import com.khiemnph.simpletracking.permission.LocationPermissionAskTracker
import com.khiemnph.simpletracking.permission.PermissionRationaleDialogFactory
import com.khiemnph.simpletracking.ui.format.formatDistanceKm
import com.khiemnph.simpletracking.ui.format.formatDuration
import com.khiemnph.simpletracking.ui.route.OfflineRouteCanvas
import com.khiemnph.simpletracking.ui.theme.ChayNgayDiTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

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
 *
 * New-session permission flow: `ACCESS_FINE_LOCATION` is blocking (see [proceedWithLocationPermissionCheck]),
 * `POST_NOTIFICATIONS` is fire-and-forget and never blocks session start (see
 * [requestNotificationPermissionThenStartSession]). Once location permission is granted, the
 * device's Location Service (the GPS toggle) is checked once via [locationSettingsChecker] right
 * before starting; while a session is `RUNNING`, [locationServiceStateReceiver] re-runs that same
 * check on every [LocationManager.PROVIDERS_CHANGED_ACTION] broadcast and auto-pauses if the
 * Location Service is confirmed off - resuming afterwards stays a manual user action. A check that
 * cannot complete is never treated as confirmation: see [LocationSettingsResult.Unresolvable].
 */
@AndroidEntryPoint
class RecordFragment : Fragment() {

    @Inject
    lateinit var locationPermissionAskTracker: LocationPermissionAskTracker

    @Inject
    lateinit var permissionRationaleDialogFactory: PermissionRationaleDialogFactory

    @Inject
    lateinit var locationSettingsChecker: LocationSettingsChecker

    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!

    val args: RecordFragmentArgs by navArgs()

    private val viewModel: RecordViewModel by viewModels()

    private var googleMap: GoogleMap? = null

    private var mapFragment: SupportMapFragment? = null

    private var stopDispatched = false
    private var hasCenteredCameraOnce = false

    /**
     * How much of the fallback is hidden behind the pinned bottom sheet, so the route is fitted to
     * the part of the screen the user can actually see. Measured rather than assumed: the sheet's
     * height depends on font scale and on the metrics it is showing.
     */
    private val bottomSheetHeightDp = mutableStateOf(0.dp)

    /**
     * Must be registered unconditionally at construction time - before `onCreate`/`onAttach`
     * complete - regardless of whether this particular screen instance ends up needing to launch
     * it (Android requires the launcher exist before the Fragment reaches `CREATED`).
     */
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            checkLocationSettingsThenStartSession()
        } else {
            // Persist right when a denial actually happens, mirroring the platform's own
            // shouldShowRequestPermissionRationale signal (false pre-first-ask, true after one
            // denial, false again once permanently denied) - then immediately re-evaluate so a
            // permanently-denied outcome is reflected without waiting for another Record tap.
            locationPermissionAskTracker.markAsked()
            proceedWithLocationPermissionCheck()
        }
    }

    /** Fire-and-forget: `POST_NOTIFICATIONS` is optional and its outcome never gates session
     * start, so there is deliberately nothing to react to here. */
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}

    private val newSessionLocationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) requestNotificationPermissionThenStartSession()
    }

    /** Re-shown mid-session after [locationServiceStateReceiver] detects the Location Service was
     * turned off; resuming afterwards is still a manual action via the Resume button regardless
     * of this resolution's outcome, so there is nothing to do with the result either way. */
    private val midSessionLocationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) {}

    /**
     * Auto-pauses a `RUNNING` session if the device's Location Service stops being satisfied
     * while this screen is active. Registered in [onStart]/unregistered in [onStop] rather than
     * tied to the view lifecycle, since monitoring is a Fragment-level concern independent of
     * whether the view happens to be recreated.
     */
    private val locationServiceStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = onLocationServiceStateChanged()
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

        setUpMap()
        setUpRouteFallback()
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

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            requireContext(),
            locationServiceStateReceiver,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(locationServiceStateReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        googleMap = null
        mapFragment = null
        _binding = null
    }

    /** Test seam: exposes the configured behavior so a test can assert `isHideable == false`. */
    internal fun bottomSheetBehavior(): BottomSheetBehavior<View> = BottomSheetBehavior.from(binding.recordBottomSheet)

    private fun setUpBottomSheet() {
        binding.recordBottomSheet.doOnLayout { sheet ->
            bottomSheetHeightDp.value = (sheet.height / resources.displayMetrics.density).dp
        }
        bottomSheetBehavior().apply {
            // Pinned/non-dismissable: the sheet must never be swipeable away from the screen.
            isHideable = false
            isDraggable = false
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    /** Test seam: exposes the map fragment this screen is driving, or null if it never bound. */
    internal fun mapFragment(): SupportMapFragment? = mapFragment

    /**
     * Binds the map, holding the [SupportMapFragment] this screen drives.
     *
     * The instance must be kept rather than looked up again after committing: `commit()` only
     * *enqueues* the transaction, so a `findFragmentById` on the next line runs before the fragment
     * is in the FragmentManager's store and returns null. That returned null silently (safe call on
     * an optional cast), leaving `googleMap` null for the view's entire lifetime — no route, no
     * camera centering, and a null thumbnail on every stop.
     *
     * On a configuration change the FragmentManager restores the child itself, so the existing
     * instance is adopted instead of replacing it with a second one.
     */
    private fun setUpMap() {
        val fragment = childFragmentManager.findFragmentById(R.id.record_map_container) as? SupportMapFragment
            ?: SupportMapFragment.newInstance().also {
                childFragmentManager.beginTransaction()
                    .replace(R.id.record_map_container, it)
                    .commit()
            }

        mapFragment = fragment
        fragment.getMapAsync { map ->
            googleMap = map
            // Only a map that has actually finished loading may replace the fallback. Binding a
            // GoogleMap is not evidence it can draw: offline it can be handed over and still never
            // render a tile.
            map.setOnMapLoadedCallback { _binding?.recordRouteFallback?.isVisible = false }
            renderRoute(viewModel.uiState.value.route)
        }
    }

    /**
     * Draws the route with no map under it, shown until [setUpMap]'s map reports itself loaded.
     *
     * This is the offline state, and it is not a rare one. The Maps renderer is a Play Services
     * dynamic module fetched at runtime, so on a device that has never cached it and has no
     * connection the map never initialises at all, and it does not recover when the network returns
     * within the same instance. Starting visible rather than being swapped in on failure means
     * there is no moment where the screen shows nothing.
     */
    private fun setUpRouteFallback() {
        binding.recordRouteFallback.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ChayNgayDiTheme {
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    OfflineRouteCanvas(
                        points = state.route,
                        modifier = Modifier.fillMaxSize(),
                        bottomInset = bottomSheetHeightDp.value,
                    )
                }
            }
        }
    }

    private fun resolveSessionRespectingPermissions() {
        val existingSessionId = args.sessionId
        if (existingSessionId != null) {
            viewModel.resolveSession(existingSessionId)
            return
        }
        // The ViewModel survives view recreation (e.g. a configuration change) even though this
        // Fragment doesn't - skip re-running the permission/Location-Service dance once a session
        // has already been resolved for it.
        if (viewModel.hasResolvedSession) return
        proceedWithLocationPermissionCheck()
    }

    private fun proceedWithLocationPermissionCheck() {
        when {
            hasLocationPermission() -> checkLocationSettingsThenStartSession()
            // Standard guidance: never show a rationale before the very first-ever ask.
            !locationPermissionAskTracker.hasAskedBefore() ->
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> showLocationRationaleDialog()
            else -> showLocationPermanentlyDeniedDialog()
        }
    }

    private fun hasLocationPermission(): Boolean = ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    private fun showLocationRationaleDialog() {
        permissionRationaleDialogFactory.locationRationaleDialog(requireContext()) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }.show()
    }

    private fun showLocationPermanentlyDeniedDialog() {
        permissionRationaleDialogFactory.locationPermanentlyDeniedDialog(requireContext()) {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", requireContext().packageName, null),
                ),
            )
        }.show()
    }

    /** Checked once, right before a brand-new session actually starts - not on every permission
     * grant in general - since this is the point where the app is about to request location
     * updates for real. */
    private fun checkLocationSettingsThenStartSession() {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = locationSettingsChecker.check()) {
                LocationSettingsResult.Satisfied -> requestNotificationPermissionThenStartSession()
                is LocationSettingsResult.ResolutionRequired -> newSessionLocationSettingsLauncher.launch(
                    IntentSenderRequest.Builder(result.intentSender).build(),
                )
                // Unknown, not negative: start anyway and say so. Refusing here is what made the
                // app unusable offline, and GPS needs no network.
                LocationSettingsResult.Unresolvable -> {
                    showLocationSettingsUnknownMessage()
                    requestNotificationPermissionThenStartSession()
                }
            }
        }
    }

    private fun showLocationSettingsUnknownMessage() {
        view?.let { Snackbar.make(it, R.string.record_location_settings_unknown_message, Snackbar.LENGTH_LONG).show() }
    }

    /** `POST_NOTIFICATIONS` is optional: this fires the request (API 33+ only, and only if not
     * already granted) without waiting for its outcome, then starts the session unconditionally
     * either way. */
    private fun requestNotificationPermissionThenStartSession() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.resolveSession(null)
    }

    private fun onLocationServiceStateChanged() {
        if (viewModel.uiState.value.status != SessionStatus.RUNNING) return
        lifecycleScope.launch {
            when (val result = locationSettingsChecker.check()) {
                LocationSettingsResult.Satisfied -> Unit
                is LocationSettingsResult.ResolutionRequired -> {
                    viewModel.onPauseOrResumeClicked()
                    midSessionLocationSettingsLauncher.launch(IntentSenderRequest.Builder(result.intentSender).build())
                }
                // Deliberately nothing: a check that could not complete is not evidence the
                // Location Service went off, and pausing a healthy run on that basis loses data.
                LocationSettingsResult.Unresolvable -> Unit
            }
        }
    }

    private fun handleStopClicked() {
        if (stopDispatched) return
        stopDispatched = true

        viewModel.onStopClicked()
        findNavController().popBackStack()
    }

    private fun renderUiState(state: RecordUiState) {
        val isPaused = state.status == SessionStatus.PAUSED

        binding.recordDistanceValue.text = formatDistanceKm(state.distanceMeters)
        binding.recordCurrentSpeedValue.text = formatPaceMinPerKm(state.currentSpeedMps)
        binding.recordDurationValue.text = formatDuration(state.elapsedDurationMillis)

        binding.recordPausedTag.visibility = if (isPaused) View.VISIBLE else View.GONE
        renderGpsSignal(state.gpsSignal)
        binding.recordPauseResumeButton.setImageResource(if (isPaused) R.drawable.ic_play else R.drawable.ic_pause)
        binding.recordPauseResumeButton.contentDescription = getString(
            if (isPaused) R.string.record_resume_content_description else R.string.record_pause_content_description,
        )

        renderRoute(state.route)
    }

    /** Clears and redraws Start/Current markers and the route polyline - simplest correct
     * approach for a route that only ever grows, at this app's scale of points per session. */
    /**
     * Says nothing when the signal is fine. The point of this line is the cases where the numbers
     * above it cannot be trusted, and a permanent "GPS good" badge would train the user to stop
     * reading it. Kept [View.INVISIBLE] rather than gone so the metrics never shift.
     */
    private fun renderGpsSignal(signal: GpsSignal) {
        val message = when (signal) {
            GpsSignal.ACQUIRING -> R.string.record_signal_acquiring
            GpsSignal.WEAK -> R.string.record_signal_weak
            GpsSignal.LOST -> R.string.record_signal_lost
            GpsSignal.GOOD -> null
        }
        binding.recordSignalTag.apply {
            if (message == null) {
                visibility = View.INVISIBLE
            } else {
                setText(message)
                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (signal == GpsSignal.LOST) R.color.md_theme_error else R.color.record_paused_ink,
                    ),
                )
                visibility = View.VISIBLE
            }
        }
    }

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

}

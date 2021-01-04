package com.khiemnph.simpletracking.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.khiemnph.domain.model.ActivityRecord
import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.SimpleTrackingApp
import com.khiemnph.simpletracking.di.component.DaggerRecordComponent
import com.khiemnph.simpletracking.presenter.ActivityRecordPresenter
import com.khiemnph.simpletracking.ui.view.ActivityRecordView
import com.khiemnph.simpletracking.util.CountUpTimer
import com.khiemnph.simpletracking.utils.UIUtil
import com.khiemnph.simpletracking.utils.extension.*
import kotlinx.android.synthetic.main.activity_record.*
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList


/**
 * Created by Khiem Nguyen on 12/28/2020.
 */
class RecordActivity : AppCompatActivity(), ActivityRecordView, OnMapReadyCallback, GoogleMap.SnapshotReadyCallback, View.OnClickListener {

    @Inject
    lateinit var presenter: ActivityRecordPresenter


    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private var totalDistanceInKiloMetre = 0f
    private var avgSpeed = 0f
    private var totalElapsedTime = 0L
    private var currentSpeed = 0f
    private var totalSpeed = 0f
    private var cameraPosition: CameraPosition? = null
    private var lastKnownLocation: Location? = null
    private var trackingRoute: ArrayList<Location> = ArrayList()



    private val countUpTimer = object : CountUpTimer() {
        override fun onTick(displayedTime: String) {
            tvElapsedTime.text = displayedTime
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult?) {
            result?.let {
                onLocationUpdated(it.locations)
            }
        }

        override fun onLocationAvailability(p0: LocationAvailability?) {

        }
    }

    private fun onLocationUpdated(locations: List<Location>) {
        googleMap?.let { map ->
            locations.forEachIndexed { index, lastLocation ->
                totalSpeed += lastLocation.speed
                if (!trackingRoute.contains(lastLocation))
                    trackingRoute.add(lastLocation)
                val size = trackingRoute.size
                avgSpeed = totalSpeed / size
                if (size >= 2) {
                    val from = trackingRoute[size - 2]
                    val to = trackingRoute[size - 1]
                    totalDistanceInKiloMetre += from.distanceTo(to).toKiloMetreUnit()
                    tvTotalDistance.text = totalDistanceInKiloMetre.toTextKilometre()
                    currentSpeed = lastLocation.speed
                    if (currentSpeed.isInfinite() || currentSpeed.isNaN()) currentSpeed = 0f
                    tvSpeed.text = currentSpeed.toTextSpeed()
                    val lineOptions = PolylineOptions().apply {
                        color(Color.BLUE)
                        jointType(JointType.ROUND)
                        startCap(RoundCap())
                        endCap(RoundCap())
                        width(3f.dp)
                        add(LatLng(from.latitude, from.longitude), LatLng(to.latitude, to.longitude))
                    }

                    map.addPolyline(lineOptions)

                    //move the camera following the moving location
                    map.moveCamera(CameraUpdateFactory.newLatLng(LatLng(to.latitude, to.longitude)))
                }

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)
        DaggerRecordComponent.builder().appComponent(SimpleTrackingApp.appComponent).build().inject(this)
        lifecycle.addObserver(presenter)
        presenter.setView(this)
        savedInstanceState?.let {
            lastKnownLocation = it.getParcelable(KEY_LOCATION)
            cameraPosition = it.getParcelable(KEY_CAMERA_POSITION)
        }

        mapContainer.updateLayoutParams<RelativeLayout.LayoutParams> {
            width = UIUtil.screenWidth()
            height = width
        }

        Places.initialize(this, getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        ivStop.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onDestroy() {
        resetEverything()
        super.onDestroy()
    }

    private fun resetEverything() {
        totalSpeed = 0f
        avgSpeed = 0f
        currentSpeed = 0f
        totalElapsedTime = 0L
        totalDistanceInKiloMetre = 0f
        trackingRoute.clear()
        stopLocationUpdates()
        countUpTimer.destroy()
        if (compressBitmapAsync?.isCancelled == false) compressBitmapAsync?.cancel(true)
    }

    @SuppressLint("MissingPermission")
    override fun onSaveInstanceState(outState: Bundle) {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { lastKnownLocation: Location? ->
            lastKnownLocation?.let {
                outState.putParcelable(KEY_LOCATION, it)
            }

        }
        googleMap?.let {
            outState.putParcelable(KEY_CAMERA_POSITION, it.cameraPosition)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onMapReady(map: GoogleMap?) {
        map?.let { myMap ->
            googleMap = myMap
            googleMap!!.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
                override fun getInfoContents(marker: Marker): View {
                    val infoWindow = layoutInflater.inflate(
                        R.layout.custom_info_contents,
                        findViewById<FrameLayout>(R.id.map),
                        false
                    )
                    val title = infoWindow.findViewById<TextView>(R.id.title)
                    title.text = marker.title
                    val snippet = infoWindow.findViewById<TextView>(R.id.snippet)
                    snippet.text = marker.snippet
                    return infoWindow
                }

                override fun getInfoWindow(p0: Marker?): View? = null

            })

            try {
                googleMap!!.isMyLocationEnabled = true
                googleMap!!.uiSettings?.isMyLocationButtonEnabled = true
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        task.result?.let { startingLocation ->
                            googleMap!!.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(startingLocation.latitude, startingLocation.longitude),
                                    18f
                                )
                            )

                            lastKnownLocation = startingLocation
                            trackingRoute.add(startingLocation)
                        }

                        startLocationUpdates()
                        countUpTimer.start()
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    private var isRequested = false

    //100% ACCESS_FINE_LOCATION permission is granted
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (googleMap != null && googleMap!!.isMyLocationEnabled && !isRequested) {
            val locationRequest = LocationRequest.create()?.apply {
                interval = INTERVAL
                fastestInterval = FASTEST_INTERVAL
                maxWaitTime = MAX_WAIT_TIME
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }


            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                mainLooper
            )

            isRequested = true
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        isRequested = false
    }

    companion object {
        const val INTERVAL = 10000L
        const val FASTEST_INTERVAL = 5000L
        const val MAX_WAIT_TIME = INTERVAL * 2
        const val KEY_CAMERA_POSITION = "camera_position"
        const val KEY_LOCATION = "location"
    }

    private var compressBitmapAsync: CompressBitmapAsync? = null

    override fun onSnapshotReady(bitmap: Bitmap?) {
        compressBitmapAsync = CompressBitmapAsync(totalDistanceInKiloMetre, currentSpeed, avgSpeed, countUpTimer.getElapsedTime(), presenter)
        compressBitmapAsync!!.execute(bitmap)
    }

    override fun onClick(v: View?) {
        v?.let {
            when (it.id) {
                R.id.ivStop -> {
                    googleMap?.snapshot(this@RecordActivity)
                }
                else -> {}
            }
        }
    }

    class CompressBitmapAsync(
        val totalDistance: Float,
        val speed: Float,
        val avgSpeed: Float,
        val elapsedTime: Long,
        presenter: ActivityRecordPresenter
    ) : AsyncTask<Bitmap, Void, ByteArray>() {

        private val presenterWeakRef = WeakReference(presenter)

        override fun doInBackground(vararg bitmaps: Bitmap?): ByteArray? {
            bitmaps[0]?.let {
                val stream = ByteArrayOutputStream()
                it.compress(Bitmap.CompressFormat.PNG, 100, stream)
                return stream.toByteArray()
            }

            return null
        }

        override fun onPostExecute(result: ByteArray?) {
            super.onPostExecute(result)
            presenterWeakRef.get()?.let { presenter ->
                result?.let { thumbByteArr ->
                    presenter.saveRecord(
                        ActivityRecord(
                            id = UUID.randomUUID().toString(),
                            recordThumbByteArr = thumbByteArr,
                            totalDistance = totalDistance,
                            speed = speed,
                            avgSpeed = avgSpeed,
                            elapsedTime = elapsedTime)
                    )
                }

            }
        }

    }

    override fun onSaveRecordSuccess(record: ActivityRecord) {
        Toast.makeText(this, "Added to DB successfully", Toast.LENGTH_SHORT).show()
        val returnedIntent = Intent().apply {
            putExtra(MainActivity.EXTRA_RECORD_ID, record.id)
            putExtra(MainActivity.EXTRA_BITMAP_BYTE_ARRAY, record.recordThumbByteArr)
        }
        setResult(Activity.RESULT_OK, returnedIntent)
        finish()
    }


    override fun getContext(): Context? {
        return this
    }
}

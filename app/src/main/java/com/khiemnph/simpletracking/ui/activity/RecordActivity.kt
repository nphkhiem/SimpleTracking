package com.khiemnph.simpletracking.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
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
import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.utils.UIUtil
import com.khiemnph.simpletracking.utils.extension.*
import kotlinx.android.synthetic.main.activity_record.*
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.lang.ref.WeakReference


/**
 * Created by Khiem Nguyen on 12/28/2020.
 */
class RecordActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.SnapshotReadyCallback, View.OnClickListener {

    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private var totalDistanceInKiloMetre = 0f
    private var cameraPosition: CameraPosition? = null
    private var lastKnownLocation: Location? = null
    private var trackingRoute: ArrayList<Location> = ArrayList()
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
            locations.forEach { lastLocation ->
                if (!trackingRoute.contains(lastLocation))
                    trackingRoute.add(lastLocation)
                val size = trackingRoute.size
                if (size >= 2) {
                    val first = trackingRoute[0]
                    val from = trackingRoute[size - 2]
                    val to = trackingRoute[size - 1]
                    totalDistanceInKiloMetre += from.distanceTo(to).toKiloMetreUnit()
                    tvTotalDistance.text = totalDistanceInKiloMetre.toTextKilometre()
                    tvSpeed.text = from.speed.toKiloMetreUnit().toTextKilometrePerHour()
                    tvElapsedTime.text = to.speed.toKiloMetreUnit().toTextKilometrePerHour()

                    val lineOptions = PolylineOptions().apply {
                        color(Color.BLUE)
                        jointType(JointType.ROUND)
                        startCap(RoundCap())
                        endCap(RoundCap())
                        width(5f)
                        add(LatLng(from.latitude, from.longitude), LatLng(to.latitude, to.longitude))
                    }

                    map.addPolyline(lineOptions)
                    try {
                        map.moveCamera(
                            CameraUpdateFactory.newLatLngBounds(
                                LatLngBounds(
                                    LatLng(first.latitude, first.longitude),
                                    LatLng(to.latitude, to.longitude)
                                ), 10
                            )
                        )
                    } catch (e : Exception) {

                    }
                }

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

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

        ivPause.setOnClickListener(this)
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
        trackingRoute.clear()
        stopLocationUpdates()
        if (!compressBitmapAsync.isCancelled) compressBitmapAsync.cancel(true)
        super.onDestroy()
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

    private fun toggleUserActionViews(isPaused: Boolean) {
        if (isPaused) {
            ivResume.visible()
            ivPause.invisible()
            ivStop.visible()
        } else {
            ivResume.invisible()
            ivPause.visible()
            ivStop.invisible()
        }
    }

    override fun onMapReady(map: GoogleMap?) {
        toggleUserActionViews(false)
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

    private val compressBitmapAsync by lazy { CompressBitmapAsync(this) }

    override fun onSnapshotReady(bitmap: Bitmap?) {
        compressBitmapAsync.execute(bitmap)
    }

    override fun onClick(v: View?) {
        v?.let {
            when (it.id) {
                R.id.ivPause -> {
//                    googleMap?.snapshot(this@RecordActivity)
                }
                else -> {}
            }
        }
    }

    class CompressBitmapAsync(activity: Activity) : AsyncTask<Bitmap, Void, ByteArray>() {

        private val activityWeakRef = WeakReference(activity)
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
            activityWeakRef.get()?.let {
                val returnedIntent = Intent().apply {
                    putExtra(MainActivity.EXTRA_BITMAP_BYTE_ARRAY, result)
                }
                it.setResult(Activity.RESULT_OK, returnedIntent)
                it.finish()
            }
        }

    }
}

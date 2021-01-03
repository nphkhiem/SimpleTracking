package com.khiemnph.simpletracking.ui.activity

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.khiemnph.domain.model.ActivityRecord

import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.SimpleTrackingApp
import com.khiemnph.simpletracking.di.component.DaggerMainComponent
import com.khiemnph.simpletracking.presenter.MainPresenter
import com.khiemnph.simpletracking.ui.adapter.RecordAdapter
import com.khiemnph.simpletracking.ui.view.MainView
import com.khiemnph.simpletracking.utils.UIUtil
import com.khiemnph.simpletracking.utils.extension.gone
import com.khiemnph.simpletracking.utils.extension.visible
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject


class MainActivity : AppCompatActivity(), MainView {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE  = 1111
        private const val REQUEST_CODE                      = 1405
        const         val EXTRA_BITMAP_BYTE_ARRAY           = "xBitmapByteArr"
        const         val EXTRA_RECORD_ID                   = "xRecordId"
    }

    @Inject
    lateinit var presenter: MainPresenter
    private lateinit var adapter: RecordAdapter

    private val handleAfterPermissionGranted    : () -> Unit                        = {
        startActivityForResult(Intent(this@MainActivity, RecordActivity::class.java), REQUEST_CODE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        DaggerMainComponent.builder().appComponent(SimpleTrackingApp.appComponent).build().inject(this)
        lifecycle.addObserver(presenter)
        presenter.setView(this)
        initViews()
    }

    private fun initViews() {
        with(rv) {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
            adapter       = RecordAdapter().also { this@MainActivity.adapter = it }
        }
    }

    fun startRecord(v: View) {
        requestLocationPermission()
    }

    override fun onResume() {
        super.onResume()
        presenter.loadAllRecords()
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(SimpleTrackingApp.appContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            handleAfterPermissionGranted.invoke()
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    handleAfterPermissionGranted.invoke()
                }
            }
            else -> {}
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    presenter.reloadAllRecords()
                }
            }
            else -> {}
        }
    }

    override fun toggleLoading(show: Boolean) {
        rv.gone()
        tvNoData.gone()
        loading.visible()
    }

    override fun toggleError(error: Throwable?) {
        rv.visible()
        loading.gone()
        if (error != null) {
            tvNoData.visible()
            tvNoData.text = "Something went wrong \n ${error?.localizedMessage ?: ""}"
        } else {
            tvNoData.gone()
        }
    }

    override fun toggleEmptyView(show: Boolean) {
        if (show) {
            rv.gone()
            tvNoData.visible()
            tvNoData.text = "No data"
        } else {
            rv.visible()
            tvNoData.gone()
        }
    }

    override fun doOnReceiveRecordData(records: ArrayList<ActivityRecord>) {
        adapter.setData(records)
    }

    override fun doOnReceiveSingleRecordData(record: ActivityRecord) {

    }

    override fun getContext(): Context? {
        return this
    }

}
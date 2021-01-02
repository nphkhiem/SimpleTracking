package com.khiemnph.simpletracking.ui.activity

import android.app.Activity
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
import com.bumptech.glide.Glide

import com.khiemnph.simpletracking.R
import com.khiemnph.simpletracking.SimpleTrackingApp
import com.khiemnph.simpletracking.utils.UIUtil
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE  = 1111
        private const val REQUEST_CODE                      = 1405
        const         val EXTRA_BITMAP_BYTE_ARRAY           = "xBitmapByteArr"
    }


    private val handleAfterPermissionGranted    : () -> Unit                        = {
        startActivityForResult(Intent(this@MainActivity, RecordActivity::class.java), REQUEST_CODE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
    }

    private fun initViews() {
        ivRecordThumb.updateLayoutParams<RelativeLayout.LayoutParams> {
            width   = UIUtil.screenWidth()
            height  = width
        }
    }

    fun startRecord(v: View) {
        requestLocationPermission()
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
                    data?.let { intent ->
                        if (intent.hasExtra(EXTRA_BITMAP_BYTE_ARRAY)) {
                            val byteArray = intent.getByteArrayExtra(EXTRA_BITMAP_BYTE_ARRAY)
                            byteArray?.let {
                                val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                                Glide.with(this).load(bitmap).centerCrop().into(ivRecordThumb)
                            }
                        }
                    }
                }
            }
            else -> {}
        }
    }

}
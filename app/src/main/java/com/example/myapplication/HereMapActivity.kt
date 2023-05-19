package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.here.android.mpa.common.GeoCoordinate
import com.here.android.mpa.common.OnEngineInitListener
import com.here.android.mpa.common.PositioningManager
import com.here.android.mpa.mapping.AndroidXMapFragment
import com.here.android.mpa.mapping.Map
import java.util.Arrays
import java.util.EnumSet

class HereMapActivity : FragmentActivity() {

    private val REQUEST_CODE_ASK_PERMISSIONS = 1
    private val REQUIRED_SDK_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    private var map: Map? = null
    private var mapFragment: AndroidXMapFragment? = null
    var longitude:Double? = null
    var latitude:Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions()

    }

    private fun checkPermissions() {
        val missingPermissions: MutableList<String> = ArrayList()
        // check all required dynamic permissions
        // check all required dynamic permissions
        for (permission in REQUIRED_SDK_PERMISSIONS) {
            val result = ContextCompat.checkSelfPermission(this, permission)
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission)
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            val permissions = missingPermissions
                .toTypedArray()
            ActivityCompat.requestPermissions(
                this,
                permissions,
                REQUEST_CODE_ASK_PERMISSIONS
            )
        } else {
            val grantResults =
                IntArray(REQUIRED_SDK_PERMISSIONS.size)
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED)
            onRequestPermissionsResult(
                REQUEST_CODE_ASK_PERMISSIONS,
                REQUIRED_SDK_PERMISSIONS,
                grantResults
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_ASK_PERMISSIONS -> {
                var index = permissions.size - 1
                while (index >= 0) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(
                            this, "Required permission '" + permissions[index]
                                    + "' not granted, exiting", Toast.LENGTH_LONG
                        ).show()
                        finish()
                        return
                    }
                    --index
                }
                initialize()
            }
        }
    }

    private fun initialize() {
        setContentView(R.layout.activity_here_map)


        val manager: FragmentManager = supportFragmentManager
        val m_mapFragment = manager.findFragmentById(R.id.mapfragment) as AndroidXMapFragment
        m_mapFragment.init { error ->
            if (error == OnEngineInitListener.Error.NONE) {
                m_mapFragment.positionIndicator!!.isVisible = true
                map = m_mapFragment.map
                map?.setCenter(GeoCoordinate(40.08062896762472, -103.16956310956208),
                    Map.Animation.NONE)
                onMapLoaded()
            }else {
                Log.e("MapInitialization", "Cannot initialize Map cFragment: " + error.details)
            }
        }


//        mapFragment = getMapFragment()
//        MapSettings.setDiskCacheRootPath(
//            applicationContext.getExternalFilesDir(null).toString() + File.separator + ".here-maps"
//        )
//
//        // Initialize MapEngine
//        mapFragment!!.init { error ->
//            if (error == OnEngineInitListener.Error.NONE) {
//                map = mapFragment!!.map
//                map!!.setCenter(
//                    GeoCoordinate(49.196261, -123.004773, 0.0),
//                    Map.Animation.NONE
//                )
//                map!!.zoomLevel = (map!!.maxZoomLevel + map!!.minZoomLevel) / 2
//            } else {
//                println("ERROR: Cannot initialize Map Fragment")
//                runOnUiThread {
//                    AlertDialog.Builder(this).setMessage(
//                        """
//                    Error : ${error.name}
//
//                    ${error.details}
//                    """.trimIndent()
//                    )
//                        .setTitle(R.string.engine_init_error)
//                        .setNegativeButton(android.R.string.cancel,
//                            DialogInterface.OnClickListener { dialog, which -> finishAffinity() })
//                        .create().show()
//                }
//            }
//        }

    }

    fun onMapLoaded() {
        this.map?.fleetFeaturesVisible = EnumSet.of(Map.FleetFeature.TRUCK_RESTRICTIONS)
        this.map?.projectionMode = Map.Projection.MERCATOR

        this.map?.positionIndicator?.setVisible(true)
        this.map?.positionIndicator?.setZIndex(0)
        this.map?.setZoomLevel(15.0)
        this.map?.positionIndicator?.setAccuracyIndicatorVisible(true)

        latitude = PositioningManager.getInstance().lastKnownPosition.coordinate.latitude
        longitude = PositioningManager.getInstance().lastKnownPosition.coordinate.longitude

    }

}
package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.here.android.mpa.common.GeoBoundingBox
import com.here.android.mpa.common.GeoCoordinate
import com.here.android.mpa.common.OnEngineInitListener
import com.here.android.mpa.common.PositioningManager
import com.here.android.mpa.common.ViewRect
import com.here.android.mpa.mapping.AndroidXMapFragment
import com.here.android.mpa.mapping.Map
import com.here.android.mpa.mapping.MapRoute
import com.here.android.mpa.routing.CoreRouter
import com.here.android.mpa.routing.Route
import com.here.android.mpa.routing.RouteOptions
import com.here.android.mpa.routing.RoutePlan
import com.here.android.mpa.routing.RouteResult
import com.here.android.mpa.routing.RouteWaypoint
import com.here.android.mpa.routing.Router
import com.here.android.mpa.routing.RoutingError
import java.util.Arrays
import java.util.EnumSet


class HereMapActivity : FragmentActivity() {

    private val REQUEST_CODE_ASK_PERMISSIONS = 1
    private val REQUIRED_SDK_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    private var map: Map? = null
    var longitude: Double? = null
    var latitude: Double? = null
    var lastCalculatedRouteResults: List<RouteResult> = ArrayList()
    lateinit var coreRouter: CoreRouter
    private var currentMapRoute: MapRoute? = null
    var selectedRoute: Route? = null

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
                map?.setCenter(
                    GeoCoordinate(40.08062896762472, -103.16956310956208),
                    Map.Animation.NONE
                )
                onMapLoaded()
            } else {
                Log.e("MapInitialization", "Cannot initialize Map cFragment: " + error.details)
            }
        }

    }

    fun onMapLoaded() {
        this.map?.fleetFeaturesVisible = EnumSet.of(Map.FleetFeature.TRUCK_RESTRICTIONS)
        this.map?.projectionMode = Map.Projection.MERCATOR
        this.map?.positionIndicator?.setVisible(true)
        this.map?.positionIndicator?.setZIndex(0)
        this.map?.setZoomLevel(15.0)
        this.map?.positionIndicator?.setAccuracyIndicatorVisible(true)

        Handler(Looper.getMainLooper()).postDelayed({
            PositioningManager.getInstance().start(PositioningManager.LocationMethod.GPS)

            latitude = PositioningManager.getInstance().lastKnownPosition.coordinate.latitude
            longitude = PositioningManager.getInstance().lastKnownPosition.coordinate.longitude

            calculateRoutes()
        }, 1000)

    }

    private fun calculateRoutes() {
        val routeOptions = RouteOptions()
        routeOptions.transportMode = RouteOptions.TransportMode.TRUCK
        routeOptions.truckLength = 22f
        routeOptions.truckHeight = 2.5f
        routeOptions.truckTrailersCount = 1
        routeOptions.routeCount = 5

        val waypointEntries: MutableList<RouteWaypoint> = java.util.ArrayList()
        if (latitude != null && longitude != null) {
            waypointEntries.add(RouteWaypoint(GeoCoordinate(latitude!!, longitude!!)))
            waypointEntries.add(RouteWaypoint(GeoCoordinate(12.9801436, 77.5685724)))
            waypointEntries.add(RouteWaypoint(GeoCoordinate(13.2194891, 79.10348599999999)))

            Log.e("latLong:", ""+latitude+", "+longitude)

            calculateRoute(waypointEntries,routeOptions)
        }

    }

    fun calculateRoute(
        waypoints: List<RouteWaypoint?>,
        routeOptions: RouteOptions?
    ) {
        val routePlan = RoutePlan()
        routePlan.routeOptions = routeOptions!!
        for (waypoint in waypoints) {
            routePlan.addWaypoint(waypoint!!)
        }

        this.coreRouter = CoreRouter()
        coreRouter.calculateRoute(
            routePlan,
            object : Router.Listener<List<RouteResult?>, RoutingError> {
                override fun onProgress(i: Int) {}

                override fun onCalculateRouteFinished(routeResults: List<RouteResult?>, routingError: RoutingError) {
                    if (routingError != RoutingError.NONE) {
                        Log.e("RouteCalculation", routingError.toString())
                    } else {
                        lastCalculatedRouteResults = routeResults as List<RouteResult>
                        Log.e("RouteCalculation", lastCalculatedRouteResults.toString())

                        if (map!=null)
                            showRoute(map!!, lastCalculatedRouteResults.get(0))

                    }
                }
            })
    }

    fun showRoute(map: Map, routeResult: RouteResult?) {
        if (routeResult == null) {
            return
        }
        if (currentMapRoute != null) {
            map.removeMapObject(currentMapRoute!!)
        }
        selectedRoute = routeResult.route
        currentMapRoute = MapRoute(selectedRoute!!)

        map.addMapObject(currentMapRoute!!)
        val box = GeoBoundingBox.getBoundingBoxContainingGeoCoordinates(routeResult.route.routeGeometry)
        val viewRect = ViewRect(
            150, 200, map.getWidth() - 150 * 2,
            map.getHeight() - 200 * 2
        )
        map.zoomTo(box!!, viewRect, Map.Animation.LINEAR, Map.MOVE_PRESERVE_ORIENTATION)
    }

}

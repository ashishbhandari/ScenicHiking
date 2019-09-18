package com.scenichiking.features.home

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.api.optimization.v1.MapboxOptimization
import com.mapbox.api.optimization.v1.models.OptimizationResponse
import com.mapbox.core.constants.Constants
import com.mapbox.core.exceptions.ServicesException
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.*
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.maps.SupportMapFragment
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.scenichiking.HikingApplication
import com.scenichiking.R
import com.scenichiking.core.di.ApplicationComponent
import com.scenichiking.core.exception.Failure
import com.scenichiking.core.extension.failure
import com.scenichiking.core.extension.observe
import com.scenichiking.core.extension.viewModel
import com.scenichiking.core.platform.BaseActivity
import com.scenichiking.core.platform.NetworkHandler
import com.scenichiking.core.util.*
import kotlinx.android.synthetic.main.activity_layout.*
import kotlinx.android.synthetic.main.bottom_sheet.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

private const val TAG = "HomeActivity"

class HomeActivity : BaseActivity(), OnMapReadyCallback, PermissionsListener,
    OnCameraTrackingChangedListener, MapboxMap.OnMapLongClickListener,
    MapboxMap.OnMapClickListener {

    private val appComponent: ApplicationComponent by lazy(mode = LazyThreadSafetyMode.NONE) {
        (application as HikingApplication).appComponent
    }

    private lateinit var mapBoxMap: MapboxMap
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var mapBoxOptimization: MapboxOptimization
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    @Inject
    lateinit var bottomSheetAdapter: BottomSheetAdapter
    @Inject
    lateinit var networkHandler: NetworkHandler

    private var featureId = AtomicInteger(0)
    private lateinit var featureViewModel: FeatureViewModel

    private var isInTrackingMode: Boolean = false


    companion object {
        fun callingIntent(context: Context) = Intent(context, HomeActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        val supportMapFragment = SupportMapFragment.newInstance()
        addFragment(savedInstanceState, supportMapFragment)
        if (networkHandler.isConnected!!) {
            supportMapFragment.getMapAsync(this)
        } else {
            handleFailure(Failure.NetworkConnection)
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapBoxMap = mapboxMap
        mapboxMap.setStyle(Style.Builder().fromUri(Style.OUTDOORS), Style.OnStyleLoaded {
            initialMarkerSetup(it)
            initOptimizedRouteLineLayer(it)
            enableLocationComponent(it)
            enableClickListeners()
            initBottomSheet()
        })

        featureViewModel = viewModel(viewModelFactory) {
            observe(featureLiveData, ::renderMainMarkers)
            failure(failure, ::handleFailure)
            observe(routePointLiveData, ::renderRoutePoints)
            observe(scenicPOIsLiveData, ::renderScenicMarkers)
        }

    }

    private fun renderMainMarkers(features: List<Feature>?) {
        if (features?.isNotEmpty()!! && features?.size > 0) {
            var nonBookMarkFeaturesOnly = ArrayList<Feature>()
            var bookMarkFeaturesOnly = ArrayList<Feature>()
            for (feature in features) {
                if (feature.getBooleanProperty(PROPERTY_FAVOURITE)) {
                    bookMarkFeaturesOnly.add(feature)
                } else {
                    nonBookMarkFeaturesOnly.add(feature)
                }
            }
            plotNextDestinationOnMap(nonBookMarkFeaturesOnly!!)
            plotBookMarkedMarkersOnMap(bookMarkFeaturesOnly)
            renderMainMarkerList(features)
        }
    }

    private fun handleFailure(failure: Failure?) {
        when (failure) {
            is Failure.NetworkConnection -> {
                renderFailure(R.string.failure_network_connection)
            }
            is Failure.ServerError -> renderFailure(R.string.failure_server_error)
        }
    }

    private fun renderFailure(@StringRes message: Int) {
        emptyView.visibility = View.VISIBLE
    }

    private fun renderRoutePoints(points: List<Point>?) {
        if (points?.isNotEmpty()!! && points?.size!! > 1) {
            plotRouteToMarkerOnMap(points!!)
        }
    }

    private fun renderScenicMarkers(features: List<Feature>?) {
        if (features?.isNotEmpty()!!) {
            plotScenicPOIsMarkerOnMap(features)
        }
    }


    private fun initBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
        btBottomSheet.setOnClickListener {
            expandCloseSheet()
        }
        fab.setOnClickListener {
            centresTheMapSection()
        }
        bottomSheetBehavior.setBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(p0: View, slideOffset: Float) {
                fab.animate().scaleX(1 - slideOffset).scaleY(1 - slideOffset).setDuration(0)
                    .start()
            }

            override fun onStateChanged(p0: View, newState: Int) {

                if (BottomSheetBehavior.STATE_DRAGGING == newState) {
                    fab.animate().scaleX(0F).scaleY(0F).setDuration(300).start()
                } else if (BottomSheetBehavior.STATE_COLLAPSED == newState) {
                    fab.animate().scaleX(1F).scaleY(1F).setDuration(300).start()
                }
            }

        })
        registerBottomSheetAdapter()
    }

    private fun expandCloseSheet() {
        if (bottomSheetBehavior!!.state != BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_EXPANDED
            btBottomSheet.text = "Close"
        } else {
            bottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
            btBottomSheet.text = "Expand"
        }
    }

    private fun registerBottomSheetAdapter() {
        hiking_list.layoutManager = LinearLayoutManager(this)
        hiking_list.itemAnimator = DefaultItemAnimator()
        hiking_list.adapter = bottomSheetAdapter
        bottomSheetAdapter.clickListener = { feature, position ->
            featureViewModel.toggledDestinationMarker(feature)
            Toast.makeText(
                this,
                " Value : " + feature.getBooleanProperty(PROPERTY_FAVOURITE) + " For iD: " + feature.getStringProperty(
                    PROPERTY_ID
                ),
                Toast.LENGTH_LONG
            ).show()
            bottomSheetAdapter.notifyItemChanged(position)

        }
    }

    private fun renderMainMarkerList(features: List<Feature>?) {
        bottomSheetAdapter.collection = features.orEmpty()
    }


    private fun enableClickListeners() {
        mapBoxMap.addOnMapLongClickListener(this)
        mapBoxMap.addOnMapClickListener(this)
    }

    private fun initialMarkerSetup(loadedMapStyle: Style) {
        // setup main red markers
        setupMainMarkSource(loadedMapStyle)
        setUpMainIconImage(loadedMapStyle)
        setUpMainMarkerLayer(loadedMapStyle)

        //setup for POIS
        setupPOIsMarkSource(loadedMapStyle)
        setUpPOIsIconImage(loadedMapStyle)
        setUpPOIsMarkerLayer(loadedMapStyle)

        // setup for BookMark markers
        setupBookMarkSource(loadedMapStyle)
        setUpBookmarkIconImage(loadedMapStyle)
        setUpBookMarkMarkerLayer(loadedMapStyle)
    }

    private fun setupMainMarkSource(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(ACTUAL_SOURCE_ID))
    }

    private fun setUpMainIconImage(loadedMapStyle: Style) {
        loadedMapStyle.addImage(
            ACTUAL_MARKER_IMAGE_ID, BitmapFactory.decodeResource(
                this.resources, R.drawable.red_marker
            )
        )
    }

    private fun setUpMainMarkerLayer(loadedMapStyle: Style) {
        loadedMapStyle.addLayer(
            SymbolLayer(
                ACTUAL_MARKER_LAYER_ID,
                ACTUAL_SOURCE_ID
            ).withProperties(
                PropertyFactory.iconImage(ACTUAL_MARKER_IMAGE_ID),
                PropertyFactory.iconSize(1f),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconOffset(arrayOf(0f, -7f))
            )
        )
    }

    private fun setupPOIsMarkSource(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(POIS_SOURCE_ID))
    }

    private fun setUpPOIsIconImage(loadedMapStyle: Style) {
        loadedMapStyle.addImage(
            POIS_MARKER_IMAGE_ID, BitmapFactory.decodeResource(
                this.resources, R.drawable.green_marker
            )
        )
    }

    private fun setUpPOIsMarkerLayer(loadedMapStyle: Style) {
        loadedMapStyle.addLayer(
            SymbolLayer(
                POIS_MARKER_LAYER_ID,
                POIS_SOURCE_ID
            ).withProperties(
                PropertyFactory.iconImage(POIS_MARKER_IMAGE_ID),
                PropertyFactory.iconSize(1f),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconOffset(arrayOf(0f, -7f))
            )
        )
    }


    private fun setupBookMarkSource(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(BOOKMARK_SOURCE_ID))
    }

    private fun setUpBookmarkIconImage(loadedMapStyle: Style) {
        loadedMapStyle.addImage(
            BOOKMARK_MARKER_IMAGE_ID, BitmapFactory.decodeResource(
                this.resources, R.drawable.blue_marker
            )
        )
    }

    private fun setUpBookMarkMarkerLayer(loadedMapStyle: Style) {
        loadedMapStyle.addLayer(
            SymbolLayer(
                BOOKMARK_MARKER_LAYER_ID,
                BOOKMARK_SOURCE_ID
            )
                .withProperties(
                    PropertyFactory.iconImage(BOOKMARK_MARKER_IMAGE_ID),
                    PropertyFactory.iconSize(1f),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.iconOffset(arrayOf(0f, -7f))
                )
        )
    }

    private fun initOptimizedRouteLineLayer(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(OPTIMIZED_ROUTE_SOURCE_ID))
        loadedMapStyle.addLayerBelow(
            LineLayer(
                OPTIMIZED_ROUTE_LAYER_ID,
                OPTIMIZED_ROUTE_SOURCE_ID
            )
                .withProperties(
                    PropertyFactory.lineColor(Color.parseColor(TEAL_COLOR)),
                    PropertyFactory.lineWidth(POLYLINE_WIDTH)
                ), ACTUAL_MARKER_IMAGE_ID
        )
    }

    private fun enableLocationComponent(loadedMapStyle: Style) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            showCurrentLocation(loadedMapStyle)
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    private fun showCurrentLocation(loadedMapStyle: Style) {
        val customLocationComponentOptions = LocationComponentOptions.builder(this)
            .trackingGesturesManagement(true)
            .accuracyColor(ContextCompat.getColor(this, R.color.mapboxGreen))
            .build()

        val locationComponentActivationOptions =
            LocationComponentActivationOptions.builder(this, loadedMapStyle)
                .locationComponentOptions(customLocationComponentOptions)
                .build()

        mapBoxMap.locationComponent.apply {

            activateLocationComponent(locationComponentActivationOptions)
            isLocationComponentEnabled = true

            registerCameraTrackingMode()
            renderMode = RenderMode.COMPASS
            setCameraMode(
                CameraMode.TRACKING_GPS_NORTH,
                object : OnLocationCameraTransitionListener {
                    override fun onLocationCameraTransitionFinished(@CameraMode.Mode cameraMode: Int) {
                        Log.e(TAG, "onLocationCameraTransitionFinished")

                        val currentLocation = getCurrentLocation(mapBoxMap.locationComponent)
                        val currentLocFeature = currentLocation?.let { generateFeature(it) }
                        if (currentLocFeature != null) {
                            featureViewModel.generateRouteToMarker(currentLocFeature)
                        }

                        zoomWhileTracking(
                            15.0,
                            750,
                            object : MapboxMap.CancelableCallback {
                                override fun onCancel() {
                                    // No impl
                                }

                                override fun onFinish() {
                                    tiltWhileTracking(45.0)
                                }
                            })
                    }

                    override fun onLocationCameraTransitionCanceled(@CameraMode.Mode cameraMode: Int) {
                        Log.e(TAG, "onLocationCameraTransitionCanceled")
                    }
                })

        }.addOnCameraTrackingChangedListener(this)
    }

    private fun registerCameraTrackingMode() {
        camera_tracking_mode.setOnClickListener {
            if (!isInTrackingMode) {
                isInTrackingMode = true
                mapBoxMap.locationComponent.cameraMode = CameraMode.TRACKING
                mapBoxMap.locationComponent.zoomWhileTracking(16.0)
                Toast.makeText(
                    this, getString(R.string.tracking_enabled),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.tracking_already_enabled),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getCurrentLocation(locationComponent: LocationComponent): LatLng? {
        return locationComponent.lastKnownLocation?.let {
            val latitude = locationComponent.lastKnownLocation!!.latitude
            val longitude = locationComponent.lastKnownLocation!!.longitude
            return LatLng(latitude, longitude)
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG)
            .show()
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent(mapBoxMap.style!!)
        } else {
            Toast.makeText(
                this,
                R.string.user_location_permission_not_granted, Toast.LENGTH_LONG
            )
                .show()
        }
    }

    override fun onCameraTrackingChanged(currentMode: Int) {
        Log.e(TAG, "onCameraTrackingChanged : $currentMode")
    }

    override fun onCameraTrackingDismissed() {
        Log.e(TAG, "onCameraTrackingDismissed ")
        isInTrackingMode = false
    }

    override fun onMapLongClick(point: LatLng): Boolean {
        if (isMaxLimitReached()) {
            Toast.makeText(
                this,
                "Max twelve stops allowed on route",
                Toast.LENGTH_LONG
            ).show()
        } else {
            val feature = generateFeature(point)
            featureViewModel.addNextDestinationOnMap(feature)
            featureViewModel.generateRouteToMarker(feature)
            triggerScenicPOIService(point)// async cal to fetch the scenic and POis for a selected point
        }
        return true
    }

    override fun onMapClick(point: LatLng): Boolean {
        return true
    }

    private fun isMaxLimitReached(): Boolean {
        return featureViewModel.featureLiveData.value?.size == 12
    }

    /**
     * Generate Feature using LatLong position
     */
    private fun generateFeature(point: LatLng): Feature {
        // adding current tappable position into destination marker
        val currentFeature = Feature.fromGeometry(
            Point.fromLngLat(
                point.longitude,
                point.latitude
            )
        )
        val featureId = featureId.incrementAndGet()
        currentFeature.properties()!!.addProperty(PROPERTY_TITLE, "Destination  ${featureId - 1}")
        currentFeature.properties()!!.addProperty(PROPERTY_ID, featureId.toString())
        currentFeature.properties()!!.addProperty(PROPERTY_FAVOURITE, false)
        return currentFeature
    }

    private fun plotNextDestinationOnMap(feature: List<Feature>) {
        // refreshing Main Markers
        val geoJsonSource = mapBoxMap.style?.getSourceAs<GeoJsonSource>(ACTUAL_SOURCE_ID)
        geoJsonSource?.setGeoJson(FeatureCollection.fromFeatures(feature))

    }

    private fun plotScenicPOIsMarkerOnMap(feature: List<Feature>) {
        // refreshing Scenic Markers
        val geoJsonSource = mapBoxMap.style?.getSourceAs<GeoJsonSource>(POIS_SOURCE_ID)
        geoJsonSource?.setGeoJson(FeatureCollection.fromFeatures(feature))
    }

    private fun plotBookMarkedMarkersOnMap(feature: List<Feature>) {
        // refreshing Scenic Markers
        val geoJsonSource = mapBoxMap.style?.getSourceAs<GeoJsonSource>(BOOKMARK_SOURCE_ID)
        geoJsonSource?.setGeoJson(FeatureCollection.fromFeatures(feature))
    }

    /**
     * TODO : could be move into another class or injected through dagger instance
     * Optimization API is limited to 12 coordinate sets only
     */
    private fun plotRouteToMarkerOnMap(routeCoordinates: List<Point>) {
        mapBoxOptimization = MapboxOptimization.builder()
            .source(FIRST)
            .destination(ANY)
            .coordinates(routeCoordinates)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .profile(DirectionsCriteria.PROFILE_WALKING)
            .accessToken(
                (if (Mapbox.getAccessToken() != null) Mapbox.getAccessToken() else getString(
                    R.string.mapbox_access_token
                )).toString()
            )
            .build()

        mapBoxOptimization.enqueueCall(object : Callback<OptimizationResponse> {
            override fun onResponse(
                call: Call<OptimizationResponse>,
                response: Response<OptimizationResponse>
            ) {
                if (!response.isSuccessful) {
                    Toast.makeText(
                        baseContext,
                        R.string.no_success,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    if (response.body() != null) {
                        val routes = response.body()!!.trips()
                        if (routes != null) {
                            if (routes.isEmpty()) {
                                Log.d(
                                    TAG,
                                    "%s size = %s" + " Message : " + getString(R.string.successful_but_no_routes) + " Size : " + routes.size
                                )
                                Toast.makeText(
                                    baseContext, R.string.successful_but_no_routes,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                // Get most optimized route from API response

                                var optimizedRoute = routes[0]

                                drawOptimizedRoute(mapBoxMap.style, optimizedRoute)
                            }
                        } else {
                            Log.d(TAG, "list of routes in the response is null")
                            Toast.makeText(
                                baseContext, String.format(
                                    getString(R.string.null_in_response),
                                    "The Optimization API response's list of routes"
                                ), Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Log.d(TAG, "response.body() is null")
                        Toast.makeText(
                            baseContext, String.format(
                                getString(R.string.null_in_response),
                                "The Optimization API response's body"
                            ), Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onFailure(call: Call<OptimizationResponse>, throwable: Throwable) {
                Log.d(TAG, "Error: %s " + " Message :" + throwable.message)
            }
        })
    }

    /**
     * TODO: Async call could be move to another class so that code can be loosley coupled
     */
    private fun triggerScenicPOIService(latLng: LatLng) {
        try {
            // Build a Mapbox geocoding request, Retrieve places near a specific location
            val client = MapboxGeocoding.builder()
                .accessToken(getString(R.string.mapbox_access_token))
                .query(Point.fromLngLat(latLng.longitude, latLng.latitude))
                .geocodingTypes(GeocodingCriteria.TYPE_POI, GeocodingCriteria.TYPE_NEIGHBORHOOD)
                .mode(GeocodingCriteria.MODE_PLACES)
                .build()

            // unable to use Limit here seems like a bug in a SDK
            client.enqueueCall(object : Callback<GeocodingResponse> {
                override fun onResponse(
                    call: Call<GeocodingResponse>,
                    response: Response<GeocodingResponse>
                ) {
                    if (response.body() != null) {
                        val results = response.body()!!.features()
                        // filter it here as limit functionality is not working, seems like a bug in SDK
                        if (results.size > 0) {
                            Log.e(TAG, "Total Available Scenic Spots and POIs: " + results.size)
                            var bookMarkfeatures = ArrayList<Feature>()
                            for (poi in results) {
                                Log.e(TAG, "POI spot : $poi")
                                val scenicFeature = Feature.fromGeometry(
                                    Point.fromLngLat(
                                        (poi.geometry() as Point).longitude(),
                                        (poi.geometry() as Point).latitude()
                                    )
                                )
                                bookMarkfeatures.add(scenicFeature)
                            }
                            featureViewModel.addScenicPOIsOnMap(bookMarkfeatures)

                        } else {
                            Toast.makeText(
                                baseContext,
                                "No results at this position",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                override fun onFailure(call: Call<GeocodingResponse>, throwable: Throwable) {
                    Log.e(TAG, "Geocoding Failure: " + throwable.message)
                }
            })
        } catch (servicesException: ServicesException) {
            Log.e(TAG, "Error geocoding: $servicesException")
            servicesException.printStackTrace()
        }
    }

    private fun drawOptimizedRoute(style: Style?, optimizedRoute: DirectionsRoute?) {
        val optimizedLineSource = style?.getSourceAs<GeoJsonSource>(OPTIMIZED_ROUTE_SOURCE_ID)
        optimizedLineSource?.setGeoJson(
            FeatureCollection.fromFeature(
                Feature.fromGeometry(
                    LineString.fromPolyline(optimizedRoute?.geometry()!!, Constants.PRECISION_6)
                )
            )
        )
    }

    private fun centresTheMapSection() {

        val features = featureViewModel.featureLiveData.value
        if (features?.isNotEmpty()!! && features?.size >1) {
            val latLngBounds = LatLngBounds.Builder()
                .includes(createLatLngsForCameraBounds(features!!))
                .build()

            mapBoxMap.easeCamera(
                CameraUpdateFactory.newLatLngBounds(
                    latLngBounds, 50
                ), 2000
            )
        }
    }

    private fun createLatLngsForCameraBounds(featureList: List<Feature>): List<LatLng> {
        var latLngList = ArrayList<LatLng>()
        for (singleFeature in featureList) {
            latLngList.add(
                LatLng(
                    (singleFeature.geometry() as Point).latitude(),
                    (singleFeature.geometry() as Point).longitude()

                )
            )
        }
        return latLngList
    }

}
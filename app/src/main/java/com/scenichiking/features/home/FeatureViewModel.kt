package com.scenichiking.features.home

import androidx.lifecycle.MutableLiveData
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.scenichiking.core.util.PROPERTY_FAVOURITE
import com.scenichiking.core.extension.notifyObserver
import com.scenichiking.core.platform.BaseViewModel
import javax.inject.Inject

class FeatureViewModel @Inject constructor() : BaseViewModel() {


    var featureLiveData: MutableLiveData<MutableList<Feature>> = MutableLiveData()

    var routePointLiveData: MutableLiveData<MutableList<Point>> = MutableLiveData()

    var scenicPOIsLiveData: MutableLiveData<MutableList<Feature>> = MutableLiveData()

    init {
        featureLiveData.value = ArrayList()
        routePointLiveData.value = ArrayList()
        scenicPOIsLiveData.value = ArrayList()
    }

    private fun refreshMainMarkers() {
        featureLiveData.notifyObserver()
    }

    fun addNextDestinationOnMap(feature: Feature) {
        featureLiveData.value?.add(feature)
        featureLiveData.notifyObserver()
    }

    fun generateRouteToMarker(feature: Feature) {
        routePointLiveData.value?.add(
            Point.fromLngLat(
                (feature.geometry() as Point).longitude(),
                (feature.geometry() as Point).latitude()
            )
        )
        routePointLiveData.notifyObserver()
    }

    fun addScenicPOIsOnMap(features: List<Feature>) {
        scenicPOIsLiveData.value?.addAll(features)
        scenicPOIsLiveData.notifyObserver()
    }

    fun toggledDestinationMarker(feature: Feature) {
        if (feature.getBooleanProperty(PROPERTY_FAVOURITE)) {
            feature.properties()?.addProperty(PROPERTY_FAVOURITE, false)
        } else {
            feature.properties()?.addProperty(PROPERTY_FAVOURITE, true)
        }
        refreshMainMarkers()
    }

}
package com.scenichiking

import android.app.Application
import com.mapbox.mapboxsdk.Mapbox
import com.scenichiking.core.di.ApplicationComponent
import com.scenichiking.core.di.ApplicationModule
import com.scenichiking.core.di.DaggerApplicationComponent


class HikingApplication : Application() {

    val appComponent: ApplicationComponent by lazy(mode = LazyThreadSafetyMode.NONE) {
        DaggerApplicationComponent
            .builder()
            .applicationModule(ApplicationModule(this))
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        this.injectMembers()
        Mapbox.getInstance(applicationContext, getString(R.string.mapbox_access_token))
    }

    private fun injectMembers() = appComponent.inject(this)
}
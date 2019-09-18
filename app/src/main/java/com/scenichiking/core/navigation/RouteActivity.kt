package com.scenichiking.core.navigation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.scenichiking.HikingApplication
import com.scenichiking.core.di.ApplicationComponent
import com.scenichiking.core.di.DaggerApplicationComponent
import javax.inject.Inject


class RouteActivity : AppCompatActivity() {

    private val appComponent: ApplicationComponent by lazy(mode = LazyThreadSafetyMode.NONE) {
        (application as HikingApplication).appComponent
    }

    /**
     * Adjust your activity to receive an instance of Navigator. This demonstrates the dependency injection
     * with the Navigator.
     */
    @Inject
    internal lateinit var navigator: Navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        navigator.showMain(this)
    }
}

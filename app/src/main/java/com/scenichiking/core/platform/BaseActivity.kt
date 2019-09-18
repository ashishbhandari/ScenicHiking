package com.scenichiking.core.platform

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.mapbox.mapboxsdk.maps.SupportMapFragment
import com.scenichiking.R
import com.scenichiking.core.extension.inTransaction
import kotlinx.android.synthetic.main.toolbar.*
import javax.inject.Inject

/**
 * Base Activity class with helper methods for handling fragment transactions and back button
 * events.
 *
 * @see AppCompatActivity
 */
abstract class BaseActivity : AppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_layout)
        setSupportActionBar(toolbar)

    }

    protected fun addFragment(savedInstanceState: Bundle?, fragment: SupportMapFragment) =
        savedInstanceState ?: supportFragmentManager.inTransaction {
            add(R.id.fragmentContainer, fragment)
        }

}
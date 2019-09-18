package com.scenichiking.core.navigation

import android.content.Context
import com.scenichiking.features.home.HomeActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created a class named Navigator. This class will be injected into an activity or Fragment
 *
 * With the @Inject annotation on the constructor, we instruct Dagger that an object of this class can be
 * injected into other objects. Dagger automatically calls this constructor, if an instance of this class is requested.
 */
@Singleton
class Navigator @Inject constructor() {

    fun showMain(context: Context) = context.startActivity(HomeActivity.callingIntent(context))

}
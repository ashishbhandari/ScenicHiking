package com.scenichiking.core.di


import com.scenichiking.HikingApplication
import com.scenichiking.core.di.viewmodel.ViewModelModule
import com.scenichiking.core.navigation.RouteActivity
import com.scenichiking.features.home.HomeActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class, ViewModelModule::class])
interface ApplicationComponent{

    fun inject(application: HikingApplication)

    fun inject(routeActivity: RouteActivity)

    fun inject(homeActivity: HomeActivity)
}
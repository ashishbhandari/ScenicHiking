package com.scenichiking.core.di

import android.content.Context
import com.scenichiking.HikingApplication
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class ApplicationModule(private val application: HikingApplication) {

    // provide generic level of object from here, helpful if we expand the same

    @Provides
    @Singleton
    fun provideApplicationContext(): Context = application
}
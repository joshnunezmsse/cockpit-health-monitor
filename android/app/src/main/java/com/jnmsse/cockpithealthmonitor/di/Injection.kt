package com.jnmsse.cockpithealthmonitor.di

import android.content.Context
import com.jnmsse.cockpithealthmonitor.datastore.UserPreferencesManager
import com.jnmsse.cockpithealthmonitor.network.RetrofitInstance
import com.jnmsse.cockpithealthmonitor.repository.HealthRepository

object Injection {

    fun provideHealthRepository(context: Context): HealthRepository {
        return HealthRepository(
            apiService = RetrofitInstance.api,
            userPreferencesManager = UserPreferencesManager(context)
        )
    }
}
package com.jnmsse.cockpithealthmonitor.repository

import com.jnmsse.cockpithealthmonitor.data.HealthStatus
import com.jnmsse.cockpithealthmonitor.datastore.UserPreferencesManager
import com.jnmsse.cockpithealthmonitor.network.ApiService
import kotlinx.coroutines.flow.Flow

class HealthRepository(
    private val apiService: ApiService,
    private val userPreferencesManager: UserPreferencesManager
) {

    suspend fun getHealthStatus(url: String): Result<List<HealthStatus>> {
        return try {
            val healthStatus = apiService.getHealthStatus(url)
            Result.success(healthStatus)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    val serverUrl: Flow<String?> = userPreferencesManager.serverUrl

    suspend fun saveServerUrl(url: String) {
        userPreferencesManager.saveServerUrl(url)
    }
}

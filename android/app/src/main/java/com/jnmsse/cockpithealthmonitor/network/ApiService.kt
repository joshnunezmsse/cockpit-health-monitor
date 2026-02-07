package com.jnmsse.cockpithealthmonitor.network

import com.jnmsse.cockpithealthmonitor.data.HealthStatus
import retrofit2.http.GET
import retrofit2.http.Url

interface ApiService {
    @GET
    suspend fun getHealthStatus(@Url url: String): List<HealthStatus>
}

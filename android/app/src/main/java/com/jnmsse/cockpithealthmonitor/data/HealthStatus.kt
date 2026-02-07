package com.jnmsse.cockpithealthmonitor.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthStatus(
    val ip: String,
    val name: String,
    @SerialName("dashboard_url")
    val dashboardUrl: String,
    val status: String,
    val details: String,
    val health: String
)

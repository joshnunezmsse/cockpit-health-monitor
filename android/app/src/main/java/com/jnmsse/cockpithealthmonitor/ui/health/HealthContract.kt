package com.jnmsse.cockpithealthmonitor.ui.health

import com.jnmsse.cockpithealthmonitor.data.HealthStatus

sealed class HealthEvent {
    object OnRefresh : HealthEvent()
    object OnChangeServer : HealthEvent()
    object OnResume : HealthEvent()
    object OnStart : HealthEvent()
    object OnStop : HealthEvent()
}

data class HealthState(
    val isLoading: Boolean = true,
    val isSilentRefresh: Boolean = false,
    val hostname: String = "",
    val healthStatus: List<HealthStatus> = emptyList(),
    val error: String? = null
)

package com.jnmsse.cockpithealthmonitor.ui.discover

import com.jnmsse.cockpithealthmonitor.model.DiscoveredService

sealed class ServerDiscoveryEvent {
    data class OnServerSelected(val service: DiscoveredService) : ServerDiscoveryEvent()
    object OnRefresh : ServerDiscoveryEvent()
    object OnManualEntryTapped : ServerDiscoveryEvent()
    object OnManualEntryTyping : ServerDiscoveryEvent()
    data class OnManualServerSubmitted(val url: String) : ServerDiscoveryEvent()
    object OnManualEntryDismissed : ServerDiscoveryEvent()
    object OnStart : ServerDiscoveryEvent()
    object OnStop : ServerDiscoveryEvent()
    object OnSelectionHandled : ServerDiscoveryEvent() // Event to reset the selection state
}

data class ServerDiscoveryState(
    val isLoading: Boolean = true,
    val services: List<DiscoveredService> = emptyList(),
    val error: String? = null,
    val showManualEntryButton: Boolean = false,
    val isManualEntryDialogVisible: Boolean = false,
    val isDiscovering: Boolean = false,
    val selectedServerUrl: String? = null // The selected URL is now part of the state
)

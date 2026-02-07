package com.jnmsse.cockpithealthmonitor.ui.discover

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jnmsse.cockpithealthmonitor.model.DiscoveredService
import com.jnmsse.cockpithealthmonitor.nsd.NsdServiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ServerDiscoveryViewModel(application: Application) : AndroidViewModel(application) {

    private val nsdServiceManager = NsdServiceManager(application)

    private val _state = MutableStateFlow(ServerDiscoveryState())
    val state: StateFlow<ServerDiscoveryState> = _state.asStateFlow()

    init {
        nsdServiceManager.discoveredServices
            .onEach { services ->
                _state.update {
                    it.copy(isLoading = false, services = services)
                }
            }
            .launchIn(viewModelScope)

        startDiscovery()
    }

    private fun startDiscovery() {
        _state.update { it.copy(isLoading = true, isDiscovering = true, showManualEntryButton = false) }
        nsdServiceManager.startDiscovery()

        viewModelScope.launch(Dispatchers.IO) {
            delay(5000)
            _state.update { it.copy(isDiscovering = false, showManualEntryButton = true) }
        }
    }

    private fun stopDiscovery() {
        nsdServiceManager.stopDiscovery()
    }

    fun handleEvent(event: ServerDiscoveryEvent) {
        when (event) {
            is ServerDiscoveryEvent.OnServerSelected -> selectDiscoveredServer(event.service)
            ServerDiscoveryEvent.OnRefresh -> startDiscovery()
            ServerDiscoveryEvent.OnManualEntryTapped -> _state.update { it.copy(isManualEntryDialogVisible = true) }
            ServerDiscoveryEvent.OnManualEntryTyping -> nsdServiceManager.stopDiscovery()
            is ServerDiscoveryEvent.OnManualServerSubmitted -> {
                selectManualServer(event.url)
                _state.update { it.copy(isManualEntryDialogVisible = false) }
            }
            ServerDiscoveryEvent.OnManualEntryDismissed -> _state.update { it.copy(isManualEntryDialogVisible = false) }
            ServerDiscoveryEvent.OnStart -> startDiscovery()
            ServerDiscoveryEvent.OnStop -> stopDiscovery()
            ServerDiscoveryEvent.OnSelectionHandled -> _state.update { it.copy(selectedServerUrl = null) }
        }
    }

    private fun selectDiscoveredServer(service: DiscoveredService) {
        val path = service.txtRecord["path"] ?: "/mobile-monitor/"
        val endpoint = "http://${service.hostname}:${service.port}$path"
        _state.update { it.copy(selectedServerUrl = endpoint) }
    }

    private fun selectManualServer(hostAndPort: String) {
        val fullUrl = "http://${hostAndPort.trim()}/mobile-monitor/"
        _state.update { it.copy(selectedServerUrl = fullUrl) }
    }

    override fun onCleared() {
        super.onCleared()
        nsdServiceManager.stopDiscovery()
    }
}

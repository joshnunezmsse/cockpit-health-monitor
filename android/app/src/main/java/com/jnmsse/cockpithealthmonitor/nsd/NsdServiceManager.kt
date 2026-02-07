package com.jnmsse.cockpithealthmonitor.nsd

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.jnmsse.cockpithealthmonitor.model.DiscoveredService
import com.jnmsse.cockpithealthmonitor.model.toDiscoveredService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

private const val SERVICE_TYPE = "_cockpitlabmonitor._tcp."
private const val TAG = "NsdServiceManager"

class NsdServiceManager(context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _discoveredServices = MutableSharedFlow<List<DiscoveredService>>(replay = 1)
    val discoveredServices = _discoveredServices.asSharedFlow()

    private val discoveredServicesList = mutableListOf<DiscoveredService>()

    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val resolveListeners = mutableMapOf<String, NsdManager.ResolveListener>()

    fun startDiscovery() {
        stopDiscovery() // Ensure any previous discovery is stopped

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${service.serviceName}")
                val resolveListener = object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.e(TAG, "Resolve failed for ${serviceInfo.serviceName}: error code $errorCode")
                        resolveListeners.remove(serviceInfo.serviceName)
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        Log.d(TAG, "Service resolved: $serviceInfo")
                        serviceInfo.toDiscoveredService()?.let {
                            if (!discoveredServicesList.any { s -> s.name == it.name }) {
                                discoveredServicesList.add(it)
                                emitDiscoveredServices()
                            }
                        }
                        resolveListeners.remove(serviceInfo.serviceName)
                    }
                }
                resolveListeners[service.serviceName] = resolveListener
                nsdManager.resolveService(service, resolveListener)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.e(TAG, "Service lost: $service")
                discoveredServicesList.removeAll { it.name == service.serviceName }
                emitDiscoveredServices()
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed: Error code:$errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop Discovery failed: Error code:$errorCode")
            }
        }
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            nsdManager.stopServiceDiscovery(it)
            discoveryListener = null
        }
        resolveListeners.clear()
        discoveredServicesList.clear()
        emitDiscoveredServices()
    }

    private fun emitDiscoveredServices() {
        coroutineScope.launch {
            _discoveredServices.emit(discoveredServicesList.toList())
        }
    }
}
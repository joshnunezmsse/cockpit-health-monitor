package com.jnmsse.cockpithealthmonitor.model

import android.net.nsd.NsdServiceInfo
import java.nio.charset.StandardCharsets

data class DiscoveredService(
    val name: String,
    val type: String,
    val ipAddress: String,
    val hostname: String,
    val port: Int,
    val txtRecord: Map<String, String>
)

fun NsdServiceInfo.toDiscoveredService(): DiscoveredService? {
    val txtRecordMap = mutableMapOf<String, String>()
    attributes.forEach { (key, value) ->
        txtRecordMap[key] = String(value, StandardCharsets.UTF_8)
    }

    // The hostname is often the first part of the service name.
    // E.g., "cockpit-node (Lab Monitor)" -> "cockpit-node"
    val discoveredHostname = serviceName?.substringBefore(" ") ?: host?.hostName ?: host?.hostAddress ?: return null

    return DiscoveredService(
        name = serviceName,
        type = serviceType,
        ipAddress = host?.hostAddress ?: return null,
        hostname = discoveredHostname.trim(),
        port = port,
        txtRecord = txtRecordMap
    )
}

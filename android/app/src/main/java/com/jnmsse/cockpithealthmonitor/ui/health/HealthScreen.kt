package com.jnmsse.cockpithealthmonitor.ui.health

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jnmsse.cockpithealthmonitor.data.HealthStatus
import com.jnmsse.cockpithealthmonitor.ui.theme.CockpitHealthMonitorTheme
import com.jnmsse.cockpithealthmonitor.ui.theme.GreenHealthy
import com.jnmsse.cockpithealthmonitor.ui.theme.OrangeWarning
import com.jnmsse.cockpithealthmonitor.ui.theme.RedUnhealthy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
//    serverUrlOverride: String?,
    state: HealthState,
    handleEvent: (HealthEvent) -> Unit,
    onChangeServer: () -> Unit,
    onCardClicked: (String) -> Unit,
//    viewModel: HealthViewModel = viewModel(
////        factory = HealthViewModelFactory(
////            LocalContext.current.applicationContext as Application,
////            serverUrlOverride
////        )
//    )
) {
//    val state by viewModel.state.collectAsState()
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    DisposableEffect(lifecycleOwner) {
//        val observer = LifecycleEventObserver { _, event ->
//            if (event == Lifecycle.Event.ON_START) {
//                viewModel.handleEvent(HealthEvent.OnStart)
//            } else if (event == Lifecycle.Event.ON_STOP) {
//                viewModel.handleEvent(HealthEvent.OnStop)
//            }
//        }
//        lifecycleOwner.lifecycle.addObserver(observer)
//        onDispose {
//            lifecycleOwner.lifecycle.removeObserver(observer)
//        }
//    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cockpit Health") },
                actions = {
                    if (state.error == null) {
                        IconButton(onClick = { handleEvent(HealthEvent.OnRefresh) }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                        IconButton(onClick = onChangeServer) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Change Server"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (state.isLoading && !state.isSilentRefresh) {
                CircularProgressIndicator()
            } else if (state.error != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = state.error ?: "An unknown error occurred",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(onClick = onChangeServer) {
                        Text("Select a Server")
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${state.hostname} Health",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.healthStatus) { status ->
                            HealthStatusItem(
                                status = status,
                                onClick = { onCardClicked(status.dashboardUrl) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HealthStatusItem(status: HealthStatus, onClick: () -> Unit) {
    val cardColor = when (status.health.lowercase()) {
        "green" -> GreenHealthy
        "orange" -> OrangeWarning
        "red" -> RedUnhealthy
        else -> MaterialTheme.colorScheme.surface
    }

    val cardBorder = if (cardColor == MaterialTheme.colorScheme.surface) {
        BorderStroke(1.dp, Color.Black)
    } else {
        null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        border = cardBorder
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = status.name, style = MaterialTheme.typography.headlineSmall)
            Text(text = "Status: ${status.status}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Details: ${status.details}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HealthStatusItemPreview() {
    val greenStatus = HealthStatus("192.168.1.1", "pihole-core", "url", "HEALTHY", "All services OK", "green")
    val orangeStatus = HealthStatus("192.168.1.2", "pivpn-node", "url", "WARNING", "High memory usage", "orange")
    val redStatus = HealthStatus("192.168.1.3", "cockpit-node", "url", "OFFLINE", "SSH Connection Failed", "red")
    val unknownStatus = HealthStatus("192.168.1.4", "josh-MacBookAir", "url", "UNKNOWN", "Could not determine status", "gray")

    CockpitHealthMonitorTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            HealthStatusItem(status = greenStatus, onClick = {})
            HealthStatusItem(status = orangeStatus, onClick = {})
            HealthStatusItem(status = redStatus, onClick = {})
            HealthStatusItem(status = unknownStatus, onClick = {})
        }
    }
}

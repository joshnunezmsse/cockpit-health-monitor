package com.jnmsse.cockpithealthmonitor.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jnmsse.cockpithealthmonitor.model.DiscoveredService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDiscoveryScreen(
    state: ServerDiscoveryState,
    handleEvent: (ServerDiscoveryEvent) -> Unit,
    navController: NavController?,
    //viewModel: ServerDiscoveryViewModel = viewModel(),
    onServerSelected: (String) -> Unit,
    onNavigateUp: () -> Unit = {}
) {
    // val state by viewModel.state.collectAsState()
    val canNavigateBack = navController?.previousBackStackEntry != null
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    // This DisposableEffect is the correct, idiomatic way to tie an action
//    // to the lifecycle of a composable to prevent background resource usage.
//    DisposableEffect(lifecycleOwner) {
//        val observer = LifecycleEventObserver { _, event ->
//            if (event == Lifecycle.Event.ON_START) {
//                viewModel.handleEvent(ServerDiscoveryEvent.OnStart)
//            } else if (event == Lifecycle.Event.ON_STOP) {
//                viewModel.handleEvent(ServerDiscoveryEvent.OnStop)
//            }
//        }
//        lifecycleOwner.lifecycle.addObserver(observer)
//        onDispose {
//            lifecycleOwner.lifecycle.removeObserver(observer)
//        }
//    }

    // This is the "consume event from state" pattern. It avoids LaunchedEffect
    // for one-shot events like navigation or callbacks.
    state.selectedServerUrl?.let { url ->
        onServerSelected(url)
        handleEvent(ServerDiscoveryEvent.OnSelectionHandled)
    }

    if (state.isManualEntryDialogVisible) {
        ManualEntryDialog(
            onDismiss = { handleEvent(ServerDiscoveryEvent.OnManualEntryDismissed) },
            onSubmit = { url -> handleEvent(ServerDiscoveryEvent.OnManualServerSubmitted(url)) },
            onTyping = { handleEvent(ServerDiscoveryEvent.OnManualEntryTyping) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cockpit Health") },
                navigationIcon = {
                    if (canNavigateBack) {
                        IconButton(onClick = { navController?.popBackStack() ?: onNavigateUp() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { handleEvent(ServerDiscoveryEvent.OnRefresh) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            if (state.isLoading && state.services.isEmpty()) {
                CircularProgressIndicator()
            } else if (state.error != null) {
                Text(text = state.error)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Monitor Node",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(state.services) { service ->
                            ServiceItem(service = service) {
                                handleEvent(ServerDiscoveryEvent.OnServerSelected(service))
                            }
                        }
                        if (state.isDiscovering) {
                            item {
                                DiscoveryLoadingItem()
                            }
                        }
                    }
                    if (state.showManualEntryButton) {
                        Button(
                            onClick = { handleEvent(ServerDiscoveryEvent.OnManualEntryTapped) },
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("Manually enter server")
                        }
                    }
                }
            }
        }
    }
}

// ... (DiscoveryLoadingItem, ServiceItem, ManualEntryDialog remain the same)
@Composable
fun DiscoveryLoadingItem() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Text(
                text = "Searching...",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
fun ServiceItem(service: DiscoveredService, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = service.name, style = MaterialTheme.typography.headlineSmall)
            Text(text = "Hostname: ${service.hostname}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "IP: ${service.ipAddress}:${service.port}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ManualEntryDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    onTyping: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    var hasTyped by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Server URL") },
        text = {
            OutlinedTextField(
                value = url, onValueChange = {
                    url = it
                    if (!hasTyped) {
                        onTyping()
                        hasTyped = true
                    }
                },
                label = { Text("e.g., cockpit-node:8080") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(url) },
                enabled = url.isNotBlank()
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

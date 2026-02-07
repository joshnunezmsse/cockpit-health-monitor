package com.jnmsse.cockpithealthmonitor.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jnmsse.cockpithealthmonitor.ui.discover.ServerDiscoveryScreen
import com.jnmsse.cockpithealthmonitor.ui.discover.ServerDiscoveryViewModel
import com.jnmsse.cockpithealthmonitor.ui.theme.CockpitHealthMonitorTheme
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class HealthWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            val serverDiscoveryViewModel: ServerDiscoveryViewModel = viewModel()
            val state by serverDiscoveryViewModel.state.collectAsState()

            CockpitHealthMonitorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ServerDiscoveryScreen(
                        state = state,
                        handleEvent = serverDiscoveryViewModel::handleEvent,
                        onServerSelected = { serverUrl ->
                            finishConfiguration(serverUrl)
                        },
                        navController = null,
                        onNavigateUp = { finish() }
                    )
                }
            }
        }
    }

    private fun finishConfiguration(serverUrl: String) {
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(this@HealthWidgetConfigureActivity).getGlanceIdBy(appWidgetId)
            
            updateAppWidgetState(this@HealthWidgetConfigureActivity, HealthWidgetStateDefinition, glanceId) {
                HealthWidgetState(
                    serverUrl = serverUrl,
                    serverName = serverUrl.substringAfter("://").substringBefore(":")
                )
            }
            
            // Enqueue a one-time worker for an immediate update.
            val immediateWorkRequest = OneTimeWorkRequestBuilder<HealthWidgetWorker>().build()
            
            // Enqueue a unique periodic worker for subsequent updates.
            val periodicWorkRequest = PeriodicWorkRequestBuilder<HealthWidgetWorker>(15, TimeUnit.MINUTES).build()
            
            val workManager = WorkManager.getInstance(this@HealthWidgetConfigureActivity)
            workManager.enqueue(immediateWorkRequest)
            workManager.enqueueUniquePeriodicWork(
                HealthWidgetWorker.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Keep the existing worker if it's already running
                periodicWorkRequest
            )

            HealthWidget().update(this@HealthWidgetConfigureActivity, glanceId)

            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}
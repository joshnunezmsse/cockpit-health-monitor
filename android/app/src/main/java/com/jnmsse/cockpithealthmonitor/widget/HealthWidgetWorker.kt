package com.jnmsse.cockpithealthmonitor.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jnmsse.cockpithealthmonitor.di.Injection

private const val TAG = "HealthWidgetWorker"

class HealthWidgetWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    companion object {
        const val UNIQUE_WORK_NAME = "HealthWidgetWorker_Periodic"
    }

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(HealthWidget::class.java)
        val healthRepository = Injection.provideHealthRepository(context)

        glanceIds.forEach { glanceId ->
            try {
                updateAppWidgetState(context, HealthWidgetStateDefinition, glanceId) { currentState ->
                    val serverUrl = currentState.serverUrl
                    if (serverUrl.isNullOrBlank()) {
                        return@updateAppWidgetState currentState.copy(
                            serverName = "Not Configured",
                            healthStatus = emptyList()
                        )
                    }

                    val result = healthRepository.getHealthStatus(serverUrl)
                    if (result.isSuccess) {
                        currentState.copy(
                            healthStatus = result.getOrThrow()
                        )
                    } else {
                        currentState.copy(
                            healthStatus = emptyList()
                        )
                    }
                }
                HealthWidget().update(context, glanceId)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widget", e)
            }
        }
        return Result.success()
    }
}
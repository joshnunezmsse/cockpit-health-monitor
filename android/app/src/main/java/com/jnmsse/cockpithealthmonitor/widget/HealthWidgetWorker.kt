package com.jnmsse.cockpithealthmonitor.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jnmsse.cockpithealthmonitor.di.Injection
import java.util.concurrent.TimeUnit

private const val TAG = "HealthWidgetWorker"

class HealthWidgetWorker(
    private val context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    companion object {
        const val UNIQUE_WORK_NAME = "HealthWidgetWorker_Periodic"
        private const val RECOVERY_WORK_NAME = "HealthWidgetWorker_Recovery"
        private const val KEY_IS_RECOVERY = "is_recovery"
    }

    override suspend fun doWork(): Result {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(HealthWidget::class.java)
        val healthRepository = Injection.provideHealthRepository(context)
        var hasError = false

        // Check if this execution is a recovery attempt
        val isRecovery = inputData.getBoolean(KEY_IS_RECOVERY, false)

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
                        // On failure, set state to disconnected
                        hasError = true
                        currentState.copy(
                            healthStatus = emptyList()
                        )
                    }
                }
                HealthWidget().update(context, glanceId)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widget", e)
                hasError = true
            }
        }

        if (hasError) {
            if (isRecovery) {
                // If we are already in a recovery job and failed, return Retry.
                // WorkManager will handle exponential backoff.
                return Result.retry()
            } else {
                // If we are in a normal/periodic job and failed (e.g. offline),
                // schedule a constrained Recovery job to run as soon as network is back.
                enqueueRecoveryWork()
                // Return success for THIS job so the periodic cycle doesn't get stuck retrying
                // while offline. We rely on the Recovery job to handle the "when connected" part.
                return Result.success()
            }
        }

        return Result.success()
    }

    private fun enqueueRecoveryWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putBoolean(KEY_IS_RECOVERY, true)
            .build()

        val recoveryWork = OneTimeWorkRequestBuilder<HealthWidgetWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                1, TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            RECOVERY_WORK_NAME,
            ExistingWorkPolicy.KEEP, // If one is already waiting, don't add another
            recoveryWork
        )
        Log.d(TAG, "Enqueued network recovery work")
    }
}

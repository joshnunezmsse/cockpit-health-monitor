package com.jnmsse.cockpithealthmonitor.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.WorkManager
import com.jnmsse.cockpithealthmonitor.datastore.UserPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "HealthWidgetReceiver"

class HealthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HealthWidget()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(TAG, "onUpdate: ${appWidgetIds.joinToString()}")
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Log.d(TAG, "onDeleted: ${appWidgetIds.joinToString()}")
        super.onDeleted(context, appWidgetIds)
        // Clean up widget-specific state
        val userPreferencesManager = UserPreferencesManager(context)
        coroutineScope.launch {
            appWidgetIds.forEach { appWidgetId ->
                userPreferencesManager.deleteWidgetServerUrl(appWidgetId)
            }
        }
    }

    override fun onDisabled(context: Context) {
        Log.d(TAG, "onDisabled: Cancelling periodic worker.")
        super.onDisabled(context)
        // Cancel the periodic worker when the last widget is removed
        WorkManager.getInstance(context).cancelUniqueWork(HealthWidgetWorker.UNIQUE_WORK_NAME)
    }
}
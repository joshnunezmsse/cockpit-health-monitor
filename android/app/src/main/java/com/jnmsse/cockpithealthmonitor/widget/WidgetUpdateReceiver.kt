package com.jnmsse.cockpithealthmonitor.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

private const val TAG = "WidgetUpdateReceiver"

class WidgetUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            Log.d(TAG, "Device unlocked, triggering widget update.")
            
            // Enqueue a one-time worker to refresh the widget data.
            val workRequest = OneTimeWorkRequestBuilder<HealthWidgetWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}

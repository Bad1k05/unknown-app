package com.unknown.qqqqq

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** После перезагрузки телефона снова запускает службу, если она была включена. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Prefs(context).enabled && MainActivity.hasUsageAccess(context)) {
            try { WatcherService.start(context) } catch (_: Exception) { }
        }
    }
}

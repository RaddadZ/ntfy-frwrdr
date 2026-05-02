package com.playground.android

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HeartbeatWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "HeartbeatWorker"
        const val WORK_NAME = "heartbeat"
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as? NotifyForwarderApp ?: return Result.failure()

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val now = timeFormat.format(Date())

        val hbTitle = app.settingsDataStore.heartbeatTitleFlow.first()
        val hbMessage = app.settingsDataStore.heartbeatMessageFlow.first()
            .replace("{time}", now)

        val result = app.ntfyNotifier.send(
            title = hbTitle,
            message = hbMessage,
            tags = "heartbeat",
            priority = 2
        )

        return if (result.isSuccess) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Heartbeat sent at $now")
            Result.success()
        } else {
            if (BuildConfig.DEBUG) Log.e(TAG, "Heartbeat failed: ${result.exceptionOrNull()?.message}")
            Result.retry()
        }
    }
}

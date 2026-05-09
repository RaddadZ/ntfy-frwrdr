package io.github.raddadz.ntfyforwarder

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class NotifyForwarderApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getInstance(this) }
    val settingsDataStore by lazy { SettingsDataStore(this) }
    val ntfyNotifier by lazy { NtfyNotifier(settingsDataStore, database.logEntryDao(), applicationScope) }

    @Volatile var cachedEnabledPackages: Set<String> = emptySet(); private set
    @Volatile var cachedAppMetaMap: Map<String, RegisteredAppEntity> = emptyMap(); private set
    @Volatile var cachedCallForwardingEnabled: Boolean = false; private set
    @Volatile var cachedIncludeContactName: Boolean = false; private set
    @Volatile var cachedOtpHandling: String = "forward"; private set
    @Volatile var cachedLoggingEnabled: Boolean = true; private set

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            database.registeredAppDao().getEnabledFlow().collect { apps ->
                cachedEnabledPackages = apps.map { it.packageName }.toSet()
                cachedAppMetaMap = apps.associateBy { it.packageName }
            }
        }
        applicationScope.launch {
            settingsDataStore.callForwardingEnabledFlow.collect { cachedCallForwardingEnabled = it }
        }
        applicationScope.launch {
            settingsDataStore.includeContactNameFlow.collect { cachedIncludeContactName = it }
        }
        applicationScope.launch {
            settingsDataStore.otpHandlingFlow.collect { cachedOtpHandling = it }
        }
        applicationScope.launch {
            settingsDataStore.loggingEnabledFlow.collect { cachedLoggingEnabled = it }
        }
        applicationScope.launch {
            settingsDataStore.heartbeatEnabledFlow
                .combine(settingsDataStore.heartbeatIntervalHoursFlow) { enabled, hours -> enabled to hours }
                .collect { (enabled, hours) -> scheduleHeartbeat(enabled, hours) }
        }
    }

    private fun scheduleHeartbeat(enabled: Boolean, intervalHours: Int) {
        val wm = WorkManager.getInstance(this)
        if (!enabled) {
            wm.cancelUniqueWork(HeartbeatWorker.WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<HeartbeatWorker>(
            intervalHours.toLong(), TimeUnit.HOURS
        ).build()
        wm.enqueueUniquePeriodicWork(
            HeartbeatWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}

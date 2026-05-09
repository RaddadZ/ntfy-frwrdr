// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.raddadz.ntfyforwarder

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val ntfyConfigFlow: Flow<NtfyConfig>
    val callForwardingEnabledFlow: Flow<Boolean>
    val autoDeleteOnSuccessFlow: Flow<Boolean>
    val logRetentionHoursFlow: Flow<Int>
    val heartbeatEnabledFlow: Flow<Boolean>
    val heartbeatIntervalHoursFlow: Flow<Int>
    val includeContactNameFlow: Flow<Boolean>
    val otpHandlingFlow: Flow<String>
    val loggingEnabledFlow: Flow<Boolean>
    val heartbeatTitleFlow: Flow<String>
    val heartbeatMessageFlow: Flow<String>
    val testTitleFlow: Flow<String>
    val testMessageFlow: Flow<String>

    suspend fun saveNtfyConfig(config: NtfyConfig)
    suspend fun saveCallForwardingEnabled(enabled: Boolean)
    suspend fun saveAutoDeleteOnSuccess(enabled: Boolean)
    suspend fun saveLogRetentionHours(hours: Int)
    suspend fun saveHeartbeatEnabled(enabled: Boolean)
    suspend fun saveHeartbeatIntervalHours(hours: Int)
    suspend fun saveIncludeContactName(enabled: Boolean)
    suspend fun saveOtpHandling(mode: String)
    suspend fun saveLoggingEnabled(enabled: Boolean)
    suspend fun saveHeartbeatTitle(title: String)
    suspend fun saveHeartbeatMessage(message: String)
    suspend fun saveTestTitle(title: String)
    suspend fun saveTestMessage(message: String)
}

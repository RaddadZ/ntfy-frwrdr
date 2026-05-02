package com.playground.android

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class NtfyConfig(
    val serverUrl: String = "",
    val topic: String = "",
    val accessToken: String = ""
)

class SettingsDataStore(private val context: Context) : SettingsRepository {

    companion object {
        private val CALL_FORWARDING_ENABLED = booleanPreferencesKey("call_forwarding_enabled")
        private val AUTO_DELETE_ON_SUCCESS = booleanPreferencesKey("auto_delete_on_success")
        private val LOG_RETENTION_HOURS = intPreferencesKey("log_retention_hours")
        private val HEARTBEAT_ENABLED = booleanPreferencesKey("heartbeat_enabled")
        private val HEARTBEAT_INTERVAL_HOURS = intPreferencesKey("heartbeat_interval_hours")
        private val INCLUDE_CONTACT_NAME = booleanPreferencesKey("include_contact_name")
        private val OTP_HANDLING = stringPreferencesKey("otp_handling")
        private val LOGGING_ENABLED = booleanPreferencesKey("logging_enabled")
        private val HEARTBEAT_TITLE = stringPreferencesKey("heartbeat_title")
        private val HEARTBEAT_MESSAGE = stringPreferencesKey("heartbeat_message")
        private val TEST_TITLE = stringPreferencesKey("test_title")
        private val TEST_MESSAGE = stringPreferencesKey("test_message")
        private const val ENCRYPTED_TOKEN_KEY = "ntfy_access_token"
        private const val ENCRYPTED_URL_KEY = "ntfy_server_url"
        private const val ENCRYPTED_TOPIC_KEY = "ntfy_topic"

        fun isUrlSecure(url: String): Boolean {
            val trimmed = url.trim().lowercase()
            if (trimmed.startsWith("https://")) return true
            // Allow all RFC 1918 + loopback + link-local ranges
            val localPatterns = listOf(
                "http://localhost",
                "http://127.0.0.1",
                "http://[::1]",
                "http://10.",
                "http://172.",
                "http://192.168.",
                "http://169.254."
            )
            return localPatterns.any { trimmed.startsWith(it) }
        }
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override val ntfyConfigFlow: Flow<NtfyConfig> = context.dataStore.data.map {
        NtfyConfig(
            serverUrl = encryptedPrefs.getString(ENCRYPTED_URL_KEY, "") ?: "",
            topic = encryptedPrefs.getString(ENCRYPTED_TOPIC_KEY, "") ?: "",
            accessToken = encryptedPrefs.getString(ENCRYPTED_TOKEN_KEY, "") ?: ""
        )
    }

    override suspend fun saveNtfyConfig(config: NtfyConfig) {
        encryptedPrefs.edit()
            .putString(ENCRYPTED_URL_KEY, config.serverUrl)
            .putString(ENCRYPTED_TOPIC_KEY, config.topic)
            .putString(ENCRYPTED_TOKEN_KEY, config.accessToken)
            .apply()
    }

    override val callForwardingEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[CALL_FORWARDING_ENABLED] ?: false
    }

    override suspend fun saveCallForwardingEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[CALL_FORWARDING_ENABLED] = enabled
        }
    }

    override val autoDeleteOnSuccessFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_DELETE_ON_SUCCESS] ?: true
    }

    override suspend fun saveAutoDeleteOnSuccess(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AUTO_DELETE_ON_SUCCESS] = enabled
        }
    }

    override val logRetentionHoursFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[LOG_RETENTION_HOURS] ?: 168
    }

    override suspend fun saveLogRetentionHours(hours: Int) {
        context.dataStore.edit { prefs ->
            prefs[LOG_RETENTION_HOURS] = hours
        }
    }

    override val heartbeatEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HEARTBEAT_ENABLED] ?: false
    }

    override suspend fun saveHeartbeatEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HEARTBEAT_ENABLED] = enabled
        }
    }

    override val heartbeatIntervalHoursFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[HEARTBEAT_INTERVAL_HOURS] ?: 4
    }

    override suspend fun saveHeartbeatIntervalHours(hours: Int) {
        context.dataStore.edit { prefs ->
            prefs[HEARTBEAT_INTERVAL_HOURS] = hours
        }
    }


    override val includeContactNameFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[INCLUDE_CONTACT_NAME] ?: false
    }

    override suspend fun saveIncludeContactName(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[INCLUDE_CONTACT_NAME] = enabled
        }
    }

    override val otpHandlingFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[OTP_HANDLING] ?: "forward"
    }

    override suspend fun saveOtpHandling(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[OTP_HANDLING] = mode
        }
    }

    override val loggingEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[LOGGING_ENABLED] ?: true
    }

    override suspend fun saveLoggingEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[LOGGING_ENABLED] = enabled
        }
    }

    override val heartbeatTitleFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[HEARTBEAT_TITLE] ?: "Heartbeat"
    }

    override val heartbeatMessageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[HEARTBEAT_MESSAGE] ?: "Forwarder alive — last check: {time}"
    }

    override suspend fun saveHeartbeatTitle(title: String) {
        context.dataStore.edit { prefs -> prefs[HEARTBEAT_TITLE] = title }
    }

    override suspend fun saveHeartbeatMessage(message: String) {
        context.dataStore.edit { prefs -> prefs[HEARTBEAT_MESSAGE] = message }
    }

    override val testTitleFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TEST_TITLE] ?: "Test Notification"
    }

    override val testMessageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TEST_MESSAGE] ?: "If you see this in ntfy, it works!"
    }

    override suspend fun saveTestTitle(title: String) {
        context.dataStore.edit { prefs -> prefs[TEST_TITLE] = title }
    }

    override suspend fun saveTestMessage(message: String) {
        context.dataStore.edit { prefs -> prefs[TEST_MESSAGE] = message }
    }
}

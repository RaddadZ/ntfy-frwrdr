package com.playground.android

import android.util.Log
import com.playground.android.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit

class NtfyNotifier(
    private val settingsDataStore: SettingsRepository,
    private val logEntryDao: LogEntryDao,
    private val scope: CoroutineScope
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val sendTimestamps = ArrayDeque<Long>()
    private val rateLock = Any()

    private fun isRateLimited(): Boolean {
        val now = System.currentTimeMillis()
        synchronized(rateLock) {
            while (sendTimestamps.isNotEmpty() && now - sendTimestamps.first() > RATE_WINDOW_MS) {
                sendTimestamps.removeFirst()
            }
            if (sendTimestamps.size >= RATE_LIMIT) return true
            sendTimestamps.addLast(now)
            return false
        }
    }

    fun sendAsync(
        title: String,
        message: String,
        tags: String? = null,
        priority: Int = 3,
        actions: JSONArray? = null,
        icon: String? = null,
        markdown: Boolean = false
    ) {
        scope.launch {
            send(title, message, tags, priority, actions, icon, markdown)
        }
    }

    suspend fun send(
        title: String,
        message: String,
        tags: String? = null,
        priority: Int = 3,
        actions: JSONArray? = null,
        icon: String? = null,
        markdown: Boolean = false
    ): Result<Unit> {
        if (isRateLimited()) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Rate limited — dropping notification: $title")
            return Result.failure(IllegalStateException("Rate limited"))
        }

        val config = settingsDataStore.ntfyConfigFlow.first()

        if (config.serverUrl.isBlank() || config.topic.isBlank()) {
            if (BuildConfig.DEBUG) Log.w(TAG, "ntfy not configured — skipping notification")
            return Result.failure(IllegalStateException("ntfy not configured"))
        }

        val url = config.serverUrl.trimEnd('/')
        if (BuildConfig.DEBUG) Log.d(TAG, "send() called: title=$title, url=$url, topic=${config.topic}")

        // Build ntfy:// deep link so tapping notification opens ntfy client
        val ntfyClickUrl = try {
            val host = URI(url).host ?: "ntfy.sh"
            "ntfy://$host/${config.topic.trim()}"
        } catch (e: Exception) { null }

        val json = JSONObject().apply {
            put("topic", config.topic.trim())
            put("title", title)
            put("message", message)
            put("priority", priority)
            if (!tags.isNullOrBlank()) put("tags", JSONArray().put(tags))
            if (actions != null && actions.length() > 0) put("actions", actions)
            if (!ntfyClickUrl.isNullOrBlank()) put("click", ntfyClickUrl)
            if (!icon.isNullOrBlank()) put("icon", icon)
            if (markdown) put("markdown", true)
        }

        val jsonBytes = json.toString().toByteArray()
        val mediaType = "application/json".toMediaType()

        fun buildRequest(): Request {
            val rb = Request.Builder()
                .url(url)
                .post(jsonBytes.toRequestBody(mediaType))
            if (config.accessToken.isNotBlank()) {
                rb.addHeader("Authorization", "Bearer ${config.accessToken}")
            }
            return rb.build()
        }

        val request = buildRequest()

        val loggingEnabled = settingsDataStore.loggingEnabledFlow.first()
        val autoDelete = settingsDataStore.autoDeleteOnSuccessFlow.first()
        val retentionHours = settingsDataStore.logRetentionHoursFlow.first()
        val redactedMessage = redactOtp(message)

        return withContext(Dispatchers.IO) {
            try {
                if (BuildConfig.DEBUG) Log.d(TAG, "Executing HTTP request to $url")
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "Notification sent: $title")
                        if (loggingEnabled && !autoDelete) {
                            logEntryDao.insert(LogEntryEntity(
                                timestamp = System.currentTimeMillis(),
                                title = title,
                                message = redactedMessage,
                                status = "sent"
                            ))
                            pruneByRetention(retentionHours)
                        }
                        Result.success(Unit)
                    } else {
                        val errBody = response.body?.string() ?: "unknown error"
                        if (BuildConfig.DEBUG) Log.e(TAG, "ntfy error ${response.code}: $errBody")
                        val failedLogId = if (loggingEnabled) {
                            logEntryDao.insert(LogEntryEntity(
                                timestamp = System.currentTimeMillis(),
                                title = title,
                                message = redactedMessage,
                                status = "failed"
                            ))
                        } else null
                        retryOnce(buildRequest(), failedLogId)
                    }
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.e(TAG, "Network error: ${e.message}", e)
                val failedLogId = if (loggingEnabled) {
                    logEntryDao.insert(LogEntryEntity(
                        timestamp = System.currentTimeMillis(),
                        title = title,
                        message = redactedMessage,
                        status = "failed"
                    ))
                } else null
                retryOnce(buildRequest(), failedLogId)
            }
        }
    }

    private fun retryOnce(request: Request, failedLogId: Long?): Result<Unit> {
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Retry succeeded")
                    if (failedLogId != null) {
                        scope.launch { logEntryDao.deleteById(failedLogId) }
                    }
                    Result.success(Unit)
                } else {
                    if (BuildConfig.DEBUG) Log.e(TAG, "Retry failed: ${response.code}")
                    Result.failure(RuntimeException("ntfy retry failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Retry network error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun pruneByRetention(retentionHours: Int) {
        val cutoff = System.currentTimeMillis() - (retentionHours * 3600_000L)
        logEntryDao.deleteOlderThan(cutoff)
    }

    companion object {
        private const val TAG = "NtfyNotifier"
        private const val RATE_LIMIT = 30
        private const val RATE_WINDOW_MS = 60_000L
        private val OTP_PATTERN = Regex("\\b\\d{4,8}\\b")

        fun redactOtp(message: String): String {
            return OTP_PATTERN.replace(message) { match ->
                "*".repeat(match.value.length)
            }
        }
    }
}

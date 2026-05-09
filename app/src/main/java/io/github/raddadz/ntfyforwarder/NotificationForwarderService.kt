package io.github.raddadz.ntfyforwarder

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import io.github.raddadz.ntfyforwarder.BuildConfig
import org.json.JSONArray
import org.json.JSONObject

class NotificationForwarderService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotifForwarder"
        private const val DEDUP_WINDOW_MS = 5_000L

        private val OTP_KEYWORDS = Regex(
            "\\b(code|otp|verification|verify|passcode|pin|password|authenticate|token|2fa)\\b",
            RegexOption.IGNORE_CASE
        )
        private val OTP_CODE = Regex("\\b(\\d{4,8})\\b")
    }

    private val app: NotifyForwarderApp
        get() = application as NotifyForwarderApp

    private val recentKeys = LinkedHashMap<String, Long>(50, 0.75f, true)

    private fun isDuplicate(key: String): Boolean {
        val now = System.currentTimeMillis()
        synchronized(recentKeys) {
            recentKeys.entries.removeAll { now - it.value > DEDUP_WINDOW_MS }
            if (recentKeys.containsKey(key)) return true
            recentKeys[key] = now
            return false
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        // Ignore own notifications
        if (sbn.packageName == packageName) return

        // Ignore ongoing/group summary notifications
        if (sbn.isOngoing) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        if (sbn.packageName !in app.cachedEnabledPackages) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString()

        val content = bigText ?: text
        if (content.isBlank() && title.isBlank()) return

        // Deduplicate within 5-second window
        val dedupKey = "${sbn.packageName}:$title:${content.take(100)}"
        if (isDuplicate(dedupKey)) return

        val meta = app.cachedAppMetaMap[sbn.packageName]
        val appLabel = meta?.label ?: sbn.packageName
        val emojiTag = meta?.emojiTag?.ifBlank { null } ?: appLabel.lowercase().replace(Regex("[^a-z0-9]"), "_")

        val notifTitle = "[$appLabel] $title"

        // OTP detection + privacy mode
        val otpCode = extractOtpCode(content)
        val otpMode = app.cachedOtpHandling

        if (otpCode != null && otpMode == "skip") return

        val notifMessage = when {
            otpCode != null && otpMode == "redact" ->
                content.take(500).replace(otpCode, "****")
            otpCode != null ->
                content.take(500).replace(otpCode, "`$otpCode`")
            else -> content.take(500)
        }

        // Build actions
        val actions = JSONArray()

        // OTP copy button (only in forward mode)
        if (otpCode != null && otpMode == "forward") {
            actions.put(JSONObject().apply {
                put("action", "copy")
                put("label", "Copy code")
                put("value", otpCode)
                put("clear", true)
            })
        }

        // Open-app button
        val openUrl = meta?.openUrl?.ifBlank { null }
        if (openUrl != null) {
            actions.put(JSONObject().apply {
                put("action", "view")
                put("label", "Open $appLabel")
                put("url", openUrl)
                put("clear", false)
            })
        }

        if (BuildConfig.DEBUG) Log.d(TAG, "Forwarding from ${sbn.packageName}: $notifTitle" +
            if (otpCode != null) " [OTP detected]" else "")

        val iconUrl = meta?.iconUrl?.ifBlank { null }
        app.ntfyNotifier.sendAsync(
            title = notifTitle,
            message = notifMessage,
            tags = emojiTag,
            actions = if (actions.length() > 0) actions else null,
            icon = iconUrl,
            markdown = true
        )
    }

    private fun extractOtpCode(text: String): String? {
        if (!OTP_KEYWORDS.containsMatchIn(text)) return null
        return OTP_CODE.find(text)?.groupValues?.get(1)
    }
}

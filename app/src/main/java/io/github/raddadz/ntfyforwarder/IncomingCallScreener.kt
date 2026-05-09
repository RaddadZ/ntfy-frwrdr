package io.github.raddadz.ntfyforwarder

import android.content.ContentResolver
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.core.content.ContextCompat
import io.github.raddadz.ntfyforwarder.BuildConfig
import org.json.JSONArray
import org.json.JSONObject

class IncomingCallScreener : CallScreeningService() {

    companion object {
        private const val TAG = "CallScreener"
    }

    private val app: NotifyForwarderApp
        get() = application as NotifyForwarderApp

    override fun onScreenCall(callDetails: Call.Details) {
        val handle = callDetails.handle
        val number = handle?.schemeSpecificPart ?: "Unknown"

        val isIncoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        } else {
            true
        }

        if (!isIncoming) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        if (!app.cachedCallForwardingEnabled) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Call forwarding disabled — skipping")
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val contactName = if (app.cachedIncludeContactName &&
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            resolveContactName(contentResolver, number)
        } else null
        val displayName = if (contactName != null) "$contactName ($number)" else number

        if (BuildConfig.DEBUG) Log.d(TAG, "Incoming call from: $displayName")

        val actions = JSONArray().apply {
            put(JSONObject().apply {
                put("action", "view")
                put("label", "Call back")
                put("url", "tel:$number")
                put("clear", true)
            })
        }

        app.ntfyNotifier.sendAsync(
            title = "📞 Incoming Call",
            message = "From: **$displayName**",
            tags = "phone",
            priority = 4,
            actions = actions,
            markdown = true
        )

        // Allow the call through — don't block
        respondToCall(callDetails, CallResponse.Builder().build())
    }

    private fun resolveContactName(resolver: ContentResolver, number: String): String? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            resolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                } else null
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Failed to resolve contact: ${e.message}")
            null
        }
    }
}

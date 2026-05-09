package io.github.raddadz.ntfyforwarder.ui.screens

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PermContactCalendar
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhoneDisabled
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.github.raddadz.ntfyforwarder.BuildConfig
import io.github.raddadz.ntfyforwarder.MainActivity
import io.github.raddadz.ntfyforwarder.NotificationForwarderService
import io.github.raddadz.ntfyforwarder.NotifyForwarderApp
import io.github.raddadz.ntfyforwarder.NtfyConfig
import io.github.raddadz.ntfyforwarder.SettingsDataStore
import io.github.raddadz.ntfyforwarder.ui.components.EditableField
import io.github.raddadz.ntfyforwarder.ui.components.EditableFieldGroup
import io.github.raddadz.ntfyforwarder.ui.components.SectionHeader
import io.github.raddadz.ntfyforwarder.ui.components.SettingsSegmentedButton
import io.github.raddadz.ntfyforwarder.ui.components.SettingsToggleItem
import io.github.raddadz.ntfyforwarder.ui.components.StatusCard
import kotlinx.coroutines.launch

// resumeTick: incremented by MainActivity on resume to force recomposition of permission state
@Composable
fun SettingsTab(app: NotifyForwarderApp, resumeTick: Int = 0) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Hoisted once — used across permission section and incoming calls section
    val activity = context as? MainActivity
    val hasPhone = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_PHONE_STATE
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val hasContacts = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CONTACTS
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    val loggingEnabled by app.settingsDataStore.loggingEnabledFlow
        .collectAsState(initial = true)
    val heartbeatEnabled by app.settingsDataStore.heartbeatEnabledFlow
        .collectAsState(initial = false)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { SectionHeader("Required", topPadding = 16.dp) }
        item { PermissionsSection(context, activity, hasPhone, hasContacts) }

        item { SectionHeader("Incoming Calls") }
        item { IncomingCallsSection(app, scope, hasContacts) }

        item { SectionHeader("OTP / Verification Codes") }
        item { OtpSection(app, scope) }

        item { SectionHeader("ntfy Server") }
        NtfyServerSection(app, scope, context)

        item { SectionHeader("Log Settings") }
        item { LogSection(app, scope, loggingEnabled) }
        if (loggingEnabled) {
            item { LogExtrasSection(app, scope) }
        }

        item { SectionHeader("Health Check") }
        item { HealthCheckSection(app, scope, heartbeatEnabled) }
        if (heartbeatEnabled) {
            item { HeartbeatDetailsSection(app, scope) }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun PermissionsSection(
    context: Context,
    activity: MainActivity?,
    hasPhone: Boolean,
    hasContacts: Boolean
) {
    val isListenerEnabled = isNotificationListenerEnabled(context)
    StatusCard(
        title = if (isListenerEnabled) "Notification Listener Active" else "Notification Listener",
        subtitle = if (isListenerEnabled) "Forwarding notifications from selected apps"
        else "Tap to grant access — required for forwarding",
        granted = isListenerEnabled,
        icon = if (isListenerEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.Notifications,
        onToggle = if (!isListenerEnabled) {
            { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        } else null
    )
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isIgnoring = pm.isIgnoringBatteryOptimizations(context.packageName)
    Spacer(modifier = Modifier.height(4.dp))
    StatusCard(
        title = if (isIgnoring) "Battery Unrestricted" else "Battery Optimization",
        subtitle = if (isIgnoring) "Background services won't be killed"
        else "Tap to disable — prevents Android from killing the forwarder",
        granted = isIgnoring,
        icon = if (isIgnoring) Icons.Outlined.BatterySaver else Icons.Outlined.BatteryAlert,
        onToggle = if (!isIgnoring) {
            {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
        } else null
    )
    Spacer(modifier = Modifier.height(4.dp))
    StatusCard(
        title = if (hasPhone) "Phone & Call Log Granted" else "Phone & Call Log",
        subtitle = if (hasPhone) "Call forwarding can identify incoming calls"
        else "Required for incoming call forwarding",
        granted = hasPhone,
        icon = if (hasPhone) Icons.Outlined.Phone else Icons.Outlined.PhoneDisabled,
        onToggle = if (!hasPhone) { { activity?.permissionState?.requestCallPermissions() } } else null
    )
    Spacer(modifier = Modifier.height(4.dp))
    StatusCard(
        title = if (hasContacts) "Contacts Granted" else "Contacts",
        subtitle = if (hasContacts) "Caller name included with forwarded calls"
        else "Optional — shows contact name instead of just number",
        granted = hasContacts,
        icon = if (hasContacts) Icons.Outlined.Contacts else Icons.Outlined.PermContactCalendar,
        onToggle = if (!hasContacts) { { activity?.permissionState?.requestContactsPermission() } } else null
    )
}

@Composable
private fun IncomingCallsSection(
    app: NotifyForwarderApp,
    scope: kotlinx.coroutines.CoroutineScope,
    hasContacts: Boolean
) {
    val includeContactName by app.settingsDataStore.includeContactNameFlow
        .collectAsState(initial = false)
    SettingsToggleItem(
        title = "Include contact name",
        subtitle = if (!hasContacts) "Grant contacts permission above"
        else if (includeContactName) "Caller name + number sent"
        else "Only phone number sent",
        checked = includeContactName && hasContacts,
        onCheckedChange = { checked ->
            if (hasContacts) scope.launch { app.settingsDataStore.saveIncludeContactName(checked) }
        },
        enabled = hasContacts
    )
}

@Composable
private fun OtpSection(app: NotifyForwarderApp, scope: kotlinx.coroutines.CoroutineScope) {
    val otpMode by app.settingsDataStore.otpHandlingFlow.collectAsState(initial = "forward")
    SettingsSegmentedButton(
        label = "How to handle detected OTPs",
        options = listOf("forward" to "Forward", "redact" to "Redact", "skip" to "Skip"),
        selected = otpMode,
        onSelected = { mode -> scope.launch { app.settingsDataStore.saveOtpHandling(mode) } },
        description = when (otpMode) {
            "redact" -> "OTP codes replaced with **** before sending"
            "skip" -> "Notifications with OTP codes are not forwarded"
            else -> "OTP codes sent as-is with copy button"
        }
    )
}

private fun LazyListScope.NtfyServerSection(
    app: NotifyForwarderApp,
    scope: kotlinx.coroutines.CoroutineScope,
    context: Context
) {
    item {
        val config by app.settingsDataStore.ntfyConfigFlow.collectAsState(initial = NtfyConfig())
        var serverUrl by remember(config) { mutableStateOf(config.serverUrl) }
        var topic by remember(config) { mutableStateOf(config.topic) }
        var accessToken by remember(config) { mutableStateOf(config.accessToken) }
        var isTesting by remember { mutableStateOf(false) }
        val testTitle by app.settingsDataStore.testTitleFlow.collectAsState(initial = "Test Notification")
        val testMessage by app.settingsDataStore.testMessageFlow
            .collectAsState(initial = "If you see this in ntfy, it works!")

        fun validateAndSave(onSuccess: suspend () -> Unit) {
            if (serverUrl.isNotBlank() && !SettingsDataStore.isUrlSecure(serverUrl)) {
                Toast.makeText(context, "⚠️ Insecure URL! Use HTTPS to protect your token.", Toast.LENGTH_LONG).show()
                return
            }
            scope.launch {
                app.settingsDataStore.saveNtfyConfig(NtfyConfig(serverUrl, topic, accessToken))
                onSuccess()
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = serverUrl, onValueChange = { serverUrl = it },
                label = { Text("Server URL") },
                placeholder = { Text("https://ntfy.example.com") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = topic, onValueChange = { topic = it },
                label = { Text("Topic") },
                placeholder = { Text("my-secret-topic") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = accessToken, onValueChange = { accessToken = it },
                label = { Text("Access Token (optional)") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        validateAndSave {
                            Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
                OutlinedButton(
                    onClick = {
                        validateAndSave {
                            isTesting = true
                            try {
                                if (BuildConfig.DEBUG) Log.d("SendTest", "Starting send test...")
                                val result = app.ntfyNotifier.send(
                                    title = testTitle, message = testMessage, tags = "white_check_mark"
                                )
                                if (BuildConfig.DEBUG) Log.d("SendTest", "Send result: success=${result.isSuccess}")
                                val msg = if (result.isSuccess) "Test sent!" else "Failed: ${result.exceptionOrNull()?.message}"
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                if (BuildConfig.DEBUG) Log.e("SendTest", "Send test crashed", e)
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isTesting = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isTesting && serverUrl.isNotBlank() && topic.isNotBlank()
                ) { Text(if (isTesting) "Sending…" else "Send Test") }
            }
            val testTitlePref by app.settingsDataStore.testTitleFlow.collectAsState(initial = "Test Notification")
            val testMessagePref by app.settingsDataStore.testMessageFlow
                .collectAsState(initial = "If you see this in ntfy, it works!")
            EditableFieldGroup(
                fields = listOf(
                    EditableField("Test title", testTitlePref),
                    EditableField("Test message", testMessagePref)
                ),
                onSave = { values ->
                    scope.launch {
                        app.settingsDataStore.saveTestTitle(values[0])
                        app.settingsDataStore.saveTestMessage(values[1])
                    }
                },
                saveLabel = "Save Test Message"
            )
            Text(
                "Phone numbers and notification content are forwarded to your configured server only. Nothing is sent to third parties.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun LogSection(
    app: NotifyForwarderApp,
    scope: kotlinx.coroutines.CoroutineScope,
    loggingEnabled: Boolean
) {
    SettingsToggleItem(
        title = "Enable logging",
        subtitle = if (loggingEnabled) "Notification events stored locally"
        else "No notification content stored on device",
        checked = loggingEnabled,
        onCheckedChange = { checked -> scope.launch { app.settingsDataStore.saveLoggingEnabled(checked) } }
    )
}

@Composable
private fun LogExtrasSection(app: NotifyForwarderApp, scope: kotlinx.coroutines.CoroutineScope) {
    val autoDelete by app.settingsDataStore.autoDeleteOnSuccessFlow.collectAsState(initial = true)
    val retentionHours by app.settingsDataStore.logRetentionHoursFlow.collectAsState(initial = 168)
    SettingsToggleItem(
        title = "Auto-delete on success",
        subtitle = "Skip logging when notification is sent successfully",
        checked = autoDelete,
        onCheckedChange = { checked -> scope.launch { app.settingsDataStore.saveAutoDeleteOnSuccess(checked) } }
    )
    SettingsSegmentedButton(
        label = "Log retention",
        options = listOf(1 to "1h", 6 to "6h", 24 to "1d", 72 to "3d", 168 to "1w"),
        selected = retentionHours,
        onSelected = { hours -> scope.launch { app.settingsDataStore.saveLogRetentionHours(hours) } }
    )
}

@Composable
private fun HealthCheckSection(
    app: NotifyForwarderApp,
    scope: kotlinx.coroutines.CoroutineScope,
    heartbeatEnabled: Boolean
) {
    SettingsToggleItem(
        title = "Heartbeat",
        subtitle = "Periodic notification to confirm forwarder is alive",
        checked = heartbeatEnabled,
        onCheckedChange = { checked -> scope.launch { app.settingsDataStore.saveHeartbeatEnabled(checked) } }
    )
}

@Composable
private fun HeartbeatDetailsSection(app: NotifyForwarderApp, scope: kotlinx.coroutines.CoroutineScope) {
    val heartbeatInterval by app.settingsDataStore.heartbeatIntervalHoursFlow.collectAsState(initial = 4)
    val hbTitlePref by app.settingsDataStore.heartbeatTitleFlow.collectAsState(initial = "Heartbeat")
    val hbMessagePref by app.settingsDataStore.heartbeatMessageFlow
        .collectAsState(initial = "Forwarder alive — last check: {time}")
    SettingsSegmentedButton(
        label = "Heartbeat interval",
        options = listOf(1 to "1h", 2 to "2h", 4 to "4h", 8 to "8h", 12 to "12h", 24 to "24h"),
        selected = heartbeatInterval,
        onSelected = { hours -> scope.launch { app.settingsDataStore.saveHeartbeatIntervalHours(hours) } }
    )
    EditableFieldGroup(
        fields = listOf(
            EditableField("Heartbeat title", hbTitlePref),
            EditableField("Heartbeat message", hbMessagePref, placeholder = "Use {time} for timestamp")
        ),
        onSave = { values ->
            scope.launch {
                app.settingsDataStore.saveHeartbeatTitle(values[0])
                app.settingsDataStore.saveHeartbeatMessage(values[1])
            }
        },
        saveLabel = "Save Heartbeat Message"
    )
}

fun isNotificationListenerEnabled(context: Context): Boolean {
    val cn = ComponentName(context, NotificationForwarderService::class.java)
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(cn.flattenToString())
}

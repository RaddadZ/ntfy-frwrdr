package com.playground.android.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.playground.android.MainActivity
import com.playground.android.NotifyForwarderApp
import com.playground.android.RegisteredAppEntity
import com.playground.android.ui.components.SectionHeader
import com.playground.android.ui.components.SettingsToggleItem
import kotlinx.coroutines.launch

@Suppress("UNUSED_PARAMETER")
@Composable
fun AppsTab(app: NotifyForwarderApp, resumeTick: Int = 0) {
    val registeredApps by app.database.registeredAppDao().getAllFlow()
        .collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? MainActivity

    val callForwardingEnabled by app.settingsDataStore.callForwardingEnabledFlow
        .collectAsState(initial = false)

    var showPicker by remember { mutableStateOf(false) }
    var expandedAppId by remember { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val hasCallPermissions = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_PHONE_STATE
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALL_LOG
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Incoming Calls ────────────────────────────────
            item { SectionHeader("Incoming Calls", topPadding = 16.dp) }
            item {
                SettingsToggleItem(
                    title = "Forward incoming calls",
                    subtitle = if (!hasCallPermissions) "Grant phone permission in Settings ⚙"
                    else "Notify when phone rings",
                    checked = callForwardingEnabled && hasCallPermissions,
                    onCheckedChange = { checked ->
                        if (hasCallPermissions) {
                            scope.launch { app.settingsDataStore.saveCallForwardingEnabled(checked) }
                        }
                    },
                    enabled = hasCallPermissions
                )
            }

            // ── App Notifications ─────────────────────────────
            item { SectionHeader("App Notifications") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = {
                        scope.launch {
                            app.database.registeredAppDao().insert(
                                RegisteredAppEntity(
                                    packageName = "",
                                    label = "New App",
                                    enabled = true
                                )
                            )
                        }
                    }) {
                        Text("Custom")
                    }
                    Button(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            activity?.permissionState?.notificationLauncher?.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        }
                        showPicker = true
                    }) {
                        Text("Add App")
                    }
                }
            }
            if (registeredApps.isEmpty()) {
                item {
                    Text(
                        "No apps added yet. Tap \"Add App\" to pick from installed apps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
            items(registeredApps, key = { it.id }) { regApp ->
                SwipeToDismissAppCard(
                    regApp = regApp,
                    expanded = expandedAppId == regApp.id,
                    onToggleExpand = {
                        expandedAppId = if (expandedAppId == regApp.id) null else regApp.id
                    },
                    onToggleEnabled = { enabled ->
                        scope.launch { app.database.registeredAppDao().setEnabled(regApp.id, enabled) }
                    },
                    onSave = { updated ->
                        scope.launch { app.database.registeredAppDao().update(updated) }
                        expandedAppId = null
                    },
                    onDelete = {
                        scope.launch { app.database.registeredAppDao().delete(regApp.id) }
                    },
                    snackbarHostState = snackbarHostState
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (showPicker) {
            AppPickerDialog(
                context = context,
                existingPackages = registeredApps.map { it.packageName }.toSet(),
                onAppSelected = { pkg, label ->
                    scope.launch {
                        app.database.registeredAppDao().insert(
                            RegisteredAppEntity(
                                packageName = pkg,
                                label = label,
                                emojiTag = label.lowercase().replace(Regex("[^a-z0-9]"), "_"),
                                enabled = true
                            )
                        )
                    }
                    showPicker = false
                },
                onDismiss = { showPicker = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissAppCard(
    regApp: RegisteredAppEntity,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSave: (RegisteredAppEntity) -> Unit,
    onDelete: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var pendingDelete by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                pendingDelete = true
                false // Don't actually dismiss — we handle via snackbar
            } else false
        }
    )

    LaunchedEffect(pendingDelete) {
        if (pendingDelete) {
            val result = snackbarHostState.showSnackbar(
                message = "Deleted ${regApp.label}",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result != SnackbarResult.ActionPerformed) {
                onDelete()
            }
            pendingDelete = false
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.errorContainer
                else Color.Transparent,
                label = "swipe-bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        RegisteredAppCard(
            regApp = regApp,
            expanded = expanded,
            onToggleExpand = onToggleExpand,
            onToggleEnabled = onToggleEnabled,
            onSave = onSave,
            onDelete = onDelete
        )
    }
}

@Composable
fun RegisteredAppCard(
    regApp: RegisteredAppEntity,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSave: (RegisteredAppEntity) -> Unit,
    onDelete: () -> Unit
) {
    var editLabel by remember(regApp) { mutableStateOf(regApp.label) }
    var editPackage by remember(regApp) { mutableStateOf(regApp.packageName) }
    var editEmojiTag by remember(regApp) { mutableStateOf(regApp.emojiTag) }
    var editOpenUrl by remember(regApp) { mutableStateOf(regApp.openUrl) }
    var editIconUrl by remember(regApp) { mutableStateOf(regApp.iconUrl) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleExpand() }
                ) {
                    Text(regApp.label, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        regApp.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(checked = regApp.enabled, onCheckedChange = onToggleEnabled)
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editLabel, onValueChange = { editLabel = it },
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = editPackage, onValueChange = { editPackage = it },
                    label = { Text("Package name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = editEmojiTag, onValueChange = { editEmojiTag = it },
                    label = { Text("Emoji tag (ntfy)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = editOpenUrl, onValueChange = { editOpenUrl = it },
                    label = { Text("Open URL (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = editIconUrl, onValueChange = { editIconUrl = it },
                    label = { Text("Icon URL (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                    val context = LocalContext.current
                    Button(onClick = {
                        if (editPackage.isBlank()) {
                            Toast.makeText(context, "Package name cannot be empty", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onSave(regApp.copy(
                            label = editLabel.trim(),
                            packageName = editPackage.trim(),
                            emojiTag = editEmojiTag.trim(),
                            openUrl = editOpenUrl.trim(),
                            iconUrl = editIconUrl.trim()
                        ))
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${regApp.label}?") },
            text = { Text("This will remove the app from forwarding.") },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AppPickerDialog(
    context: Context,
    existingPackages: Set<String>,
    onAppSelected: (packageName: String, label: String) -> Unit,
    onDismiss: () -> Unit
) {
    val pm = context.packageManager
    var installedApps by remember { mutableStateOf<List<Pair<String, String>>?>(null) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val frameworkPrefixes = listOf("com.android.internal", "com.android.providers", "com.android.shell",
                "com.android.systemui", "com.android.server", "android", "com.android.keychain")
            val apps = pm.getInstalledApplications(0)
                .filter { appInfo ->
                    val pkg = appInfo.packageName
                    pkg != context.packageName && frameworkPrefixes.none { pkg == it || pkg.startsWith("$it.") }
                }
                .map { it.packageName to pm.getApplicationLabel(it).toString() }
                .sortedBy { it.second.lowercase() }
            installedApps = apps
        }
    }

    var search by remember { mutableStateOf("") }
    val filtered = remember(search, installedApps) {
        val apps = installedApps ?: return@remember emptyList()
        if (search.isBlank()) apps
        else apps.filter {
            it.second.contains(search, ignoreCase = true) ||
                it.first.contains(search, ignoreCase = true)
        }
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add App") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Search apps...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (installedApps == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(filtered) { (pkg, label) ->
                            val alreadyAdded = pkg in existingPackages
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    if (!alreadyAdded) onAppSelected(pkg, label)
                                },
                                color = if (alreadyAdded) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.surface
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (alreadyAdded) {
                                            Text(
                                                "Added",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Text(
                                        pkg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "Can't find your app? Use Custom to add by package name.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

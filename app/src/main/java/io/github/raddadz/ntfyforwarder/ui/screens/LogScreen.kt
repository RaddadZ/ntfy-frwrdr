package io.github.raddadz.ntfyforwarder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.raddadz.ntfyforwarder.NotifyForwarderApp
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogTab(app: NotifyForwarderApp) {
    val loggingEnabled by app.settingsDataStore.loggingEnabledFlow
        .collectAsState(initial = true)
    val autoDelete by app.settingsDataStore.autoDeleteOnSuccessFlow
        .collectAsState(initial = true)
    val entries by when {
        !loggingEnabled -> flowOf(emptyList())
        autoDelete -> app.database.logEntryDao().getFailedEntries()
        else -> app.database.logEntryDao().getAll()
    }.collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedButton(
            onClick = {
                scope.launch { app.database.logEntryDao().deleteAll() }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Log")
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (!loggingEnabled && entries.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.VisibilityOff,
                title = "Logging is disabled",
                subtitle = "Enable in Settings → Log Settings to record notifications"
            )
        } else if (entries.isEmpty()) {
            EmptyState(
                icon = if (autoDelete) Icons.Outlined.CheckCircle else Icons.Outlined.Inbox,
                title = if (autoDelete) "All notifications forwarded successfully"
                else "No notifications forwarded yet",
                subtitle = if (autoDelete) "Only failed sends will appear here"
                else "Forwarded notifications will appear here"
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries) { entry ->
                    val isFailed = entry.status == "failed"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isFailed)
                                MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        ListItem(
                            overlineContent = if (isFailed) {
                                {
                                    Text(
                                        "FAILED",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            } else null,
                            headlineContent = {
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = entry.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 3
                                )
                            },
                            trailingContent = {
                                Text(
                                    text = dateFormat.format(Date(entry.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

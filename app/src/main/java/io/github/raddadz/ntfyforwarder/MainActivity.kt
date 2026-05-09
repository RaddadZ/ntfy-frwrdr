package io.github.raddadz.ntfyforwarder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import io.github.raddadz.ntfyforwarder.ui.screens.AppsTab
import io.github.raddadz.ntfyforwarder.ui.screens.LogTab
import io.github.raddadz.ntfyforwarder.ui.screens.SettingsTab
import io.github.raddadz.ntfyforwarder.ui.theme.PlaygroundTheme

class MainActivity : ComponentActivity() {

    // On-demand launchers — exposed via PermissionState for composables
    lateinit var permissionState: PermissionState
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as NotifyForwarderApp

        permissionState = PermissionState(
            callPermsLauncher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                permissionState.onCallPermsResult(results, this)
            },
            callScreeningRoleLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { permissionState.onCallScreeningResult(this) },
            contactsLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted -> permissionState.onContactsResult(granted) },
            notificationLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { /* notification permission result handled silently */ },
            scope = app.applicationScope,
            settingsDataStore = app.settingsDataStore
        )

        setContent {
            PlaygroundTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(app)
                }
            }
        }
    }
}

// ── Navigation ─────────────────────────────────────────────────

enum class Tab(val label: String, val icon: ImageVector) {
    APPS("Apps", Icons.Default.Apps),
    LOG("Log", Icons.Default.History)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(app: NotifyForwarderApp) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    val tabs = Tab.entries

    // Force recomposition of permission checks when returning from system settings
    var resumeTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                resumeTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(if (showSettings) "Settings" else "Notification Forwarder") },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    if (showSettings) {
                        IconButton(onClick = { showSettings = false }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    if (!showSettings) {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!showSettings) {
                NavigationBar {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            selected = selectedTab == index,
                            onClick = { selectedTab = index }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Surface(modifier = Modifier.padding(padding)) {
            if (showSettings) {
                BackHandler { showSettings = false }
                SettingsTab(app, resumeTick)
            } else {
                Crossfade(targetState = tabs[selectedTab], label = "tab-transition") { tab ->
                    when (tab) {
                        Tab.APPS -> AppsTab(app, resumeTick)
                        Tab.LOG -> LogTab(app)
                    }
                }
            }
        }
    }
}

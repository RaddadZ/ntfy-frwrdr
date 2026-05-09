package io.github.raddadz.ntfyforwarder

import android.Manifest
import android.content.Intent
import android.os.Build
import android.app.role.RoleManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class PermissionState(
    val callPermsLauncher: ActivityResultLauncher<Array<String>>,
    val callScreeningRoleLauncher: ActivityResultLauncher<Intent>,
    val contactsLauncher: ActivityResultLauncher<String>,
    val notificationLauncher: ActivityResultLauncher<String>,
    private val scope: CoroutineScope,
    private val settingsDataStore: SettingsRepository
) {
    private val callPermsPending = AtomicBoolean(false)

    fun requestCallPermissions() {
        callPermsPending.set(true)
        callPermsLauncher.launch(arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG
        ))
    }

    fun onCallPermsResult(results: Map<String, Boolean>, activity: ComponentActivity) {
        if (!callPermsPending.compareAndSet(true, false)) return
        val allGranted = results.values.all { it }
        if (allGranted) {
            scope.launch { settingsDataStore.saveCallForwardingEnabled(true) }
            // Chain: request call screening role
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = activity.getSystemService(RoleManager::class.java)
                if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                    !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
                ) {
                    callScreeningRoleLauncher.launch(
                        roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    )
                    return
                }
            }
        } else {
            Toast.makeText(activity, "Phone permissions required for call forwarding", Toast.LENGTH_SHORT).show()
        }
    }

    fun onCallScreeningResult(activity: ComponentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(RoleManager::class.java)
            if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                Toast.makeText(activity, "Call screening role required for call forwarding", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun requestContactsPermission() {
        contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    fun onContactsResult(granted: Boolean) {
        if (granted) {
            scope.launch { settingsDataStore.saveIncludeContactName(true) }
        }
    }
}

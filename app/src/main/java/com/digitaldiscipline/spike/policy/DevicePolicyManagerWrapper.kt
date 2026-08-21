package com.digitaldiscipline.spike.policy

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.digitaldiscipline.spike.logging.EventLogger

class DevicePolicyManagerWrapper(private val context: Context) {

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val adminComponent = ComponentName(context, DigitalDisciplineDeviceAdminReceiver::class.java)

    fun isAdminActive(): Boolean {
        return dpm.isAdminActive(adminComponent)
    }

    fun isDeviceOwner(): Boolean {
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun isProfileOwner(): Boolean {
        return dpm.isProfileOwnerApp(context.packageName)
    }

    fun getPolicyAuthorityLevel(): String {
        return when {
            isDeviceOwner() -> "DEVICE_OWNER (Full Enterprise Authority)"
            isProfileOwner() -> "PROFILE_OWNER (Managed Work Profile)"
            isAdminActive() -> "DEVICE_ADMIN (Legacy Policy Authority)"
            else -> "ORDINARY_APP (No Elevated Device Management Authority)"
        }
    }

    fun suspendPackage(packageName: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!isDeviceOwner() && !isProfileOwner()) {
                EventLogger.log(
                    source = "DEVICE_POLICY",
                    packageName = packageName,
                    eventType = "SUSPENSION_FAILED",
                    details = "Requires Device Owner or Profile Owner. Current level: ${getPolicyAuthorityLevel()}"
                )
                return false
            }

            return try {
                val result = dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), true)
                val success = result.isEmpty() // returns list of packages that failed to suspend
                EventLogger.log(
                    source = "DEVICE_POLICY",
                    packageName = packageName,
                    eventType = if (success) "PACKAGE_SUSPENDED" else "SUSPENSION_REJECTED",
                    details = if (success) "App icon disabled by OS" else "OS rejected suspension"
                )
                success
            } catch (e: SecurityException) {
                EventLogger.log(
                    source = "DEVICE_POLICY",
                    packageName = packageName,
                    eventType = "SUSPENSION_SECURITY_EXCEPTION",
                    details = e.message ?: "SecurityException"
                )
                false
            }
        }
        return false
    }

    fun unsuspendPackage(packageName: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (!isDeviceOwner() && !isProfileOwner()) return false
            return try {
                val result = dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), false)
                val success = result.isEmpty()
                EventLogger.log(
                    source = "DEVICE_POLICY",
                    packageName = packageName,
                    eventType = if (success) "PACKAGE_UNSUSPENDED" else "UNSUSPENSION_FAILED",
                    details = if (success) "App reactivated by OS" else "OS failed to unsuspend"
                )
                success
            } catch (e: Exception) {
                false
            }
        }
        return false
    }

    fun setUninstallBlocked(packageName: String, blocked: Boolean): Boolean {
        if (!isDeviceOwner() && !isProfileOwner()) {
            EventLogger.log(
                source = "DEVICE_POLICY",
                packageName = packageName,
                eventType = "UNINSTALL_PROTECTION_FAILED",
                details = "Requires Device Owner / Profile Owner."
            )
            return false
        }

        return try {
            dpm.setUninstallBlocked(adminComponent, packageName, blocked)
            EventLogger.log(
                source = "DEVICE_POLICY",
                packageName = packageName,
                eventType = "UNINSTALL_BLOCK_UPDATED",
                details = "Blocked=$blocked"
            )
            true
        } catch (e: Exception) {
            EventLogger.log(
                source = "DEVICE_POLICY",
                packageName = packageName,
                eventType = "UNINSTALL_BLOCK_ERROR",
                details = e.message ?: "Error setting uninstall block"
            )
            false
        }
    }
}

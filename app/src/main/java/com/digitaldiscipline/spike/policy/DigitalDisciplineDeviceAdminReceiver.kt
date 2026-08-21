package com.digitaldiscipline.spike.policy

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.digitaldiscipline.spike.logging.EventLogger

class DigitalDisciplineDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        EventLogger.log(
            source = "DEVICE_ADMIN",
            packageName = context.packageName,
            eventType = "ADMIN_ENABLED",
            details = "Device admin privileges granted"
        )
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        EventLogger.log(
            source = "DEVICE_ADMIN",
            packageName = context.packageName,
            eventType = "ADMIN_DISABLED",
            details = "Device admin privileges revoked"
        )
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        EventLogger.log(
            source = "DEVICE_ADMIN",
            packageName = context.packageName,
            eventType = "PROVISIONING_COMPLETE",
            details = "Profile/Device provisioning finished"
        )
    }
}

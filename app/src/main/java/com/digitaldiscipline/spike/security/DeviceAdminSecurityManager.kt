package com.digitaldiscipline.spike.security

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.digitaldiscipline.spike.policy.DigitalDisciplineDeviceAdminReceiver

object DeviceAdminSecurityManager {

    fun getAdminComponent(context: Context): ComponentName {
        return ComponentName(context, DigitalDisciplineDeviceAdminReceiver::class.java)
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        return dpm?.isAdminActive(getAdminComponent(context)) == true
    }

    fun requestDeviceAdmin(activity: Activity, requestCode: Int = 1001) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getAdminComponent(activity))
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Enabling Device Admin prevents unauthorized removal of Digital Discipline."
            )
        }
        activity.startActivityForResult(intent, requestCode)
    }

    fun removeDeviceAdmin(context: Context) {
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            dpm?.removeActiveAdmin(getAdminComponent(context))
        } catch (_: Throwable) {}
    }
}

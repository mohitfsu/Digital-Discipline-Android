package com.digitaldiscipline.spike.detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.digitaldiscipline.spike.DigitalDisciplineApp
import com.digitaldiscipline.spike.data.local.DigitalDisciplineDatabase
import com.digitaldiscipline.spike.data.local.entities.ProtectionStateEntity
import com.digitaldiscipline.spike.logging.DiagnosticLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver listening for device reboots.
 * Guarantees Room database, pairing state, and policy persistence across device reboots,
 * and schedules recurring WorkManager background synchronization.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.LOCKED_BOOT_COMPLETED" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            Log.i("BootCompletedReceiver", "Device boot or package update detected: $action")
            DiagnosticLogger.initialize(context)
            DiagnosticLogger.logBootCompleted()

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = DigitalDisciplineDatabase.getInstance(context)
                    val ruleCount = db.appRuleDao().getActiveRules().size
                    DiagnosticLogger.log(
                        eventType = "BOOT_INTEGRITY_VERIFIED",
                        details = "Room DB verified with $ruleCount active app rules post-boot"
                    )

                    // Re-register periodic policy sync worker
                    val app = context.applicationContext as? DigitalDisciplineApp
                    app?.syncManager?.initializeSchedules()

                    // Update protection state heartbeat
                    db.protectionStateDao().updateProtectionState(
                        ProtectionStateEntity(
                            id = 1,
                            isAccessibilityActive = false, // Will be marked active once accessibility service connects
                            isOverlayActive = android.provider.Settings.canDrawOverlays(context),
                            isUsageStatsActive = false,
                            isProtectionEnabledByParent = true,
                            lastHeartbeatElapsedRealtime = SystemClock.elapsedRealtime()
                        )
                    )
                } catch (e: Exception) {
                    Log.e("BootCompletedReceiver", "Error during boot verification: ${e.message}", e)
                    DiagnosticLogger.log(
                        eventType = "BOOT_ERROR",
                        details = "Boot recovery error: ${e.message}"
                    )
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

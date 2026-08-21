package com.digitaldiscipline.spike.policy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.digitaldiscipline.spike.DigitalDisciplineApp
import com.digitaldiscipline.spike.logging.EventLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExactUnlockExpiryReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_EXACT_UNLOCK_EXPIRED = "com.digitaldiscipline.spike.action.EXACT_UNLOCK_EXPIRED"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != ACTION_EXACT_UNLOCK_EXPIRED) return
        val pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return

        EventLogger.log(
            source = "EXACT_ALARM",
            packageName = pkg,
            eventType = "EXACT_EXPIRY_FIRED",
            details = "Exact hardware alarm expired for $pkg"
        )

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = DigitalDisciplineApp.instance
                app.policyEngine.onTemporaryUnlockExpired(pkg)
            } catch (t: Throwable) {
                EventLogger.log(
                    source = "EXACT_ALARM",
                    packageName = pkg,
                    eventType = "EXACT_EXPIRY_ERROR",
                    details = t.message ?: "Failed to process exact alarm expiry"
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}

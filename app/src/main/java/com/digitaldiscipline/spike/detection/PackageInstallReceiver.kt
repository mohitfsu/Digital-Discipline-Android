package com.digitaldiscipline.spike.detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import com.digitaldiscipline.spike.DigitalDisciplineApp
import com.digitaldiscipline.spike.behaviour.templates.GoalTemplateRepository
import com.digitaldiscipline.spike.data.local.entities.AppRuleEntity
import com.digitaldiscipline.spike.data.local.entities.RuleMode
import com.digitaldiscipline.spike.data.local.entities.TriggerCategory
import com.digitaldiscipline.spike.logging.EventLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Real-time package install receiver for Family Mode category auto-blocking.
 * Automatically intercepts newly installed games, social media, and entertainment apps.
 */
class PackageInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_PACKAGE_ADDED) return
        val packageName = intent.data?.schemeSpecificPart ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = DigitalDisciplineApp.instance
                val prefs = app.preferencesManager
                val repo = app.policyRepository

                val autoBlockGames = prefs.autoBlockGamesFlow.first()
                val autoBlockSocial = prefs.autoBlockSocialFlow.first()
                val autoBlockStreaming = prefs.autoBlockStreamingFlow.first()

                if (!autoBlockGames && !autoBlockSocial && !autoBlockStreaming) return@launch

                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                val category = GoalTemplateRepository.categorizeApp(packageName, label)

                val isGame = category == TriggerCategory.GAMING ||
                        (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                                appInfo.category == ApplicationInfo.CATEGORY_GAME)
                val isSocial = category == TriggerCategory.SOCIAL_MEDIA
                val isStreaming = category == TriggerCategory.VIDEO_STREAMING

                val shouldBlock = (autoBlockGames && isGame) ||
                        (autoBlockSocial && isSocial) ||
                        (autoBlockStreaming && isStreaming)

                if (shouldBlock) {
                    val existing = repo.getRuleForPackage(packageName)
                    if (existing == null) {
                        val newRule = AppRuleEntity(
                            packageName = packageName,
                            appDisplayName = label,
                            mode = RuleMode.BLOCK,
                            isEnabled = true,
                            unlockDurationSeconds = 0
                        )
                        repo.saveRule(newRule)
                        EventLogger.log(
                            source = "CATEGORY_ENFORCER",
                            packageName = packageName,
                            eventType = "AUTO_BLOCKED_NEW_INSTALL",
                            details = "Auto-blocked $label ($category) on install"
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore query errors
            }
        }
    }
}

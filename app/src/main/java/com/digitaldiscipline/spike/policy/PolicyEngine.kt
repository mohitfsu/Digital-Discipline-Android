package com.digitaldiscipline.spike.policy

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.digitaldiscipline.spike.analytics.LocalAnalyticsRepository
import com.digitaldiscipline.spike.behaviour.BehaviourPolicyResolver
import com.digitaldiscipline.spike.behaviour.PolicyResolutionResult
import com.digitaldiscipline.spike.data.local.entities.AppRuleEntity
import com.digitaldiscipline.spike.data.local.entities.RuleMode
import com.digitaldiscipline.spike.data.local.entities.UserMode
import com.digitaldiscipline.spike.detection.AccessibilityLaunchDetector
import com.digitaldiscipline.spike.detection.AppLaunchDetector
import com.digitaldiscipline.spike.detection.AppLaunchEvent
import com.digitaldiscipline.spike.detection.DetectorType
import com.digitaldiscipline.spike.detection.DigitalDisciplineAccessibilityService
import com.digitaldiscipline.spike.detection.UsageStatsLaunchDetector
import com.digitaldiscipline.spike.logging.EventLogger
import com.digitaldiscipline.spike.overlay.OverlayManager
import com.digitaldiscipline.spike.wallet.EarnedTimeWalletService
import com.digitaldiscipline.spike.wallet.SessionStartResult
import com.digitaldiscipline.spike.wallet.SessionUpdateResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PolicyEngine(
    private val context: Context,
    val policyRepository: PolicyRepository,
    val analyticsRepository: LocalAnalyticsRepository,
    val overlayManager: OverlayManager,
    val behaviourPolicyResolver: BehaviourPolicyResolver? = null,
    val walletService: EarnedTimeWalletService? = null,
    val interventionEngine: com.digitaldiscipline.spike.intervention.engine.InterventionEngine? = null,
    val preferencesManager: com.digitaldiscipline.spike.data.preferences.PreferencesManager? = null
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    val overlayStrategy = OverlayEnforcementStrategy(overlayManager)
    var currentEnforcementStrategy: AppEnforcementStrategy = overlayStrategy

    val usageStatsDetector = UsageStatsLaunchDetector(context, pollingIntervalMs = 500L)
    val accessibilityDetector = AccessibilityLaunchDetector(context)
    private var activeDetector: AppLaunchDetector = accessibilityDetector

    private val _currentForegroundEvent = MutableStateFlow<AppLaunchEvent?>(null)
    val currentForegroundEvent: StateFlow<AppLaunchEvent?> = _currentForegroundEvent.asStateFlow()

    private val _activeDetectorType = MutableStateFlow(DetectorType.ACCESSIBILITY)
    val activeDetectorType: StateFlow<DetectorType> = _activeDetectorType.asStateFlow()

    private val _lastDetectionLatency = MutableStateFlow(0L)
    val lastDetectionLatency: StateFlow<Long> = _lastDetectionLatency.asStateFlow()

    private var currentForegroundPackage: String? = null
    private var isScreenOff = false
    private var activeUnlockJob: Job? = null
    private var activeWalletHeartbeatJob: Job? = null
    private val currentEscalationLevelMap = ConcurrentHashMap<String, Int>()

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOff = true
                    activeWalletHeartbeatJob?.cancel()
                    activeWalletHeartbeatJob = null
                    scope.launch {
                        walletService?.pauseOrEndSession()
                    }
                }
                Intent.ACTION_USER_PRESENT, Intent.ACTION_SCREEN_ON -> {
                    isScreenOff = false
                    val pkg = currentForegroundPackage
                    if (pkg != null && pkg != context.packageName) {
                        scope.launch {
                            val currentModeStr = preferencesManager?.getUserMode()
                                ?: try { com.digitaldiscipline.spike.DigitalDisciplineApp.instance.preferencesManager.getUserMode() } catch (_: Exception) { UserMode.SELF.name }
                            val currentUserMode = try { UserMode.valueOf(currentModeStr) } catch (_: Exception) { UserMode.SELF }

                            val resolvedResult = behaviourPolicyResolver?.resolvePolicy(pkg, userMode = currentUserMode)
                            val rule = when (resolvedResult) {
                                is PolicyResolutionResult.ParentPolicyMatch -> resolvedResult.appRule
                                is PolicyResolutionResult.BehaviourPolicyMatch -> resolvedResult.resolvedAppRule
                                is PolicyResolutionResult.NoMatch -> policyRepository.getRuleForPackage(pkg)
                                null -> policyRepository.getRuleForPackage(pkg)
                            }
                            if (rule != null && rule.isEnabled && rule.mode != RuleMode.ALLOW && walletService != null) {
                                val nowElapsed = SystemClock.elapsedRealtime()
                                val sessionResult = walletService.startOrResumeSession(pkg, nowElapsed)
                                if (sessionResult is SessionStartResult.Started || sessionResult is SessionStartResult.Resumed) {
                                    startWalletHeartbeat(pkg, rule)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    init {
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            context.registerReceiver(screenReceiver, filter)
        } catch (_: Exception) {}

        overlayManager.onInterventionCompletedListener = { targetPackage, durationMs ->
            scope.launch {
                val durationSec = (durationMs / 1000L).toInt().coerceAtLeast(1)

                // 1. Earn time in wallet if walletService is active
                val idempotencyKey = "earn_${targetPackage}_${System.currentTimeMillis()}"
                walletService?.earnTime(
                    amountSeconds = durationSec,
                    source = "INTERVENTION_COMPLETED",
                    triggerPackage = targetPackage,
                    idempotencyKey = idempotencyKey
                )

                // 2. Advance escalation level for next session
                val currentLevel = currentEscalationLevelMap[targetPackage] ?: 1
                val nextLevel = if (currentLevel >= 3) 1 else currentLevel + 1
                currentEscalationLevelMap[targetPackage] = nextLevel

                EventLogger.log(
                    source = "POLICY_ENGINE",
                    packageName = targetPackage,
                    eventType = "ESCALATION_ADVANCED",
                    details = "Completed Level $currentLevel -> Next Level will be $nextLevel | Earned ${durationSec}s"
                )

                grantTemporaryUnlock(targetPackage, durationMs, "INTERVENTION_COMPLETED")

                // 3. Immediately start or resume wallet session and heartbeat for uninterrupted foreground tracking
                currentForegroundPackage = targetPackage
                val currentModeStr = preferencesManager?.getUserMode()
                    ?: try { com.digitaldiscipline.spike.DigitalDisciplineApp.instance.preferencesManager.getUserMode() } catch (_: Exception) { UserMode.SELF.name }
                val currentUserMode = try { UserMode.valueOf(currentModeStr) } catch (_: Exception) { UserMode.SELF }

                val resolvedResult = behaviourPolicyResolver?.resolvePolicy(targetPackage, userMode = currentUserMode)
                val rule = when (resolvedResult) {
                    is PolicyResolutionResult.ParentPolicyMatch -> resolvedResult.appRule
                    is PolicyResolutionResult.BehaviourPolicyMatch -> resolvedResult.resolvedAppRule
                    is PolicyResolutionResult.NoMatch -> policyRepository.getRuleForPackage(targetPackage)
                    null -> policyRepository.getRuleForPackage(targetPackage)
                }

                if (rule != null && walletService != null) {
                    val nowElapsed = SystemClock.elapsedRealtime()
                    walletService.startOrResumeSession(targetPackage, nowElapsed)
                    startWalletHeartbeat(targetPackage, rule)
                }
            }
        }

        overlayManager.onExitToHomeListener = {
            val oldPkg = currentForegroundPackage
            currentForegroundPackage = null
            activeWalletHeartbeatJob?.cancel()
            activeWalletHeartbeatJob = null
            if (oldPkg != null) {
                scope.launch {
                    walletService?.pauseOrEndSession()
                }
            }
            DigitalDisciplineAccessibilityService.resetLastPackage()
        }

        DigitalDisciplineAccessibilityService.onPeriodicPulse = { pkg ->
            if (pkg == currentForegroundPackage && !isScreenOff) {
                scope.launch(Dispatchers.IO) {
                    analyticsRepository.recordUsageSeconds(pkg, 1L)
                    val nowElapsed = SystemClock.elapsedRealtime()
                    val updateRes = walletService?.heartbeatOrUpdateSession(nowElapsed)
                    if (updateRes is SessionUpdateResult.Expired || updateRes is SessionUpdateResult.RebootInvalidated) {
                        policyRepository.revokeTemporaryUnlock(pkg)
                        val level = currentEscalationLevelMap[pkg] ?: 1
                        val currentModeStr = preferencesManager?.getUserMode()
                            ?: try { com.digitaldiscipline.spike.DigitalDisciplineApp.instance.preferencesManager.getUserMode() } catch (_: Exception) { UserMode.SELF.name }
                        val currentUserMode = try { UserMode.valueOf(currentModeStr) } catch (_: Exception) { UserMode.SELF }

                        val resolvedResult = behaviourPolicyResolver?.resolvePolicy(pkg, userMode = currentUserMode)
                        val rule = when (resolvedResult) {
                            is PolicyResolutionResult.ParentPolicyMatch -> resolvedResult.appRule
                            is PolicyResolutionResult.BehaviourPolicyMatch -> resolvedResult.resolvedAppRule
                            is PolicyResolutionResult.NoMatch -> policyRepository.getRuleForPackage(pkg)
                            null -> policyRepository.getRuleForPackage(pkg)
                        }

                        if (rule != null) {
                            mainHandler.post {
                                currentEnforcementStrategy.enforceRestriction(
                                    packageName = pkg,
                                    appDisplayName = rule.appDisplayName,
                                    unlockDurationSeconds = rule.unlockDurationSeconds,
                                    attemptNumber = level,
                                    ruleMode = rule.mode,
                                    pauseDurationSeconds = rule.pauseDurationSeconds,
                                    breathingDurationSeconds = rule.breathingDurationSeconds,
                                    squatsTargetCount = rule.squatsTargetCount
                                )
                            }
                        }
                    }
                }
            }
        }

        ensureDefaultRules()
    }

    fun ensureDefaultRules() {
        scope.launch {
            val instagram = policyRepository.getRuleForPackage("com.instagram.android")
            if (instagram == null) {
                policyRepository.saveRule(
                    AppRuleEntity(
                        packageName = "com.instagram.android",
                        appDisplayName = "Instagram",
                        mode = RuleMode.EARN,
                        isEnabled = true,
                        unlockDurationSeconds = 300,
                        interventionType = "PAUSE",
                        pauseDurationSeconds = 10,
                        breathingDurationSeconds = 30,
                        squatsTargetCount = 10
                    )
                )
            }
            val youtube = policyRepository.getRuleForPackage("com.google.android.youtube")
            if (youtube == null) {
                policyRepository.saveRule(
                    AppRuleEntity(
                        packageName = "com.google.android.youtube",
                        appDisplayName = "YouTube",
                        mode = RuleMode.EARN,
                        isEnabled = true,
                        unlockDurationSeconds = 300,
                        interventionType = "BREATHING",
                        pauseDurationSeconds = 10,
                        breathingDurationSeconds = 30,
                        squatsTargetCount = 10
                    )
                )
            }
            val freefire = policyRepository.getRuleForPackage("com.dts.freefireth")
            if (freefire == null) {
                policyRepository.saveRule(
                    AppRuleEntity(
                        packageName = "com.dts.freefireth",
                        appDisplayName = "Gaming App (Free Fire)",
                        mode = RuleMode.EARN,
                        isEnabled = true,
                        unlockDurationSeconds = 300,
                        interventionType = "SQUATS",
                        pauseDurationSeconds = 10,
                        breathingDurationSeconds = 30,
                        squatsTargetCount = 10
                    )
                )
            }
        }
    }

    fun start() {
        ensureDefaultRules()
        switchDetector(_activeDetectorType.value)

        // Persistent Fallback Watchdog: if Accessibility Service is killed or unbound by OEM,
        // UsageStatsDetector seamlessly monitors in background to ensure zero protection gaps.
        scope.launch {
            while (isActive) {
                delay(3000L)
                if (_activeDetectorType.value == DetectorType.ACCESSIBILITY && !DigitalDisciplineAccessibilityService.isServiceRunning()) {
                    if (usageStatsDetector.isPermissionGranted() && !usageStatsDetector.isRunning()) {
                        usageStatsDetector.startMonitoring { event ->
                            handleAppLaunchEvent(event)
                        }
                    }
                }
            }
        }
    }

    fun resetAttempts() {
        currentEscalationLevelMap.clear()
        DigitalDisciplineAccessibilityService.resetLastPackage()
        EventLogger.log(
            source = "POLICY_ENGINE",
            packageName = "all",
            eventType = "ATTEMPTS_RESET",
            details = "All escalation counters reset to Level 1 (Mindful Pause)"
        )
    }

    fun switchDetector(type: DetectorType) {
        activeDetector.stopMonitoring()
        _activeDetectorType.value = type

        activeDetector = when (type) {
            DetectorType.USAGE_STATS -> usageStatsDetector
            DetectorType.ACCESSIBILITY -> accessibilityDetector
        }

        activeDetector.startMonitoring { event ->
            handleAppLaunchEvent(event)
        }

        EventLogger.log(
            source = "POLICY_ENGINE",
            packageName = "system",
            eventType = "DETECTOR_SWITCHED",
            details = "Active: $type"
        )
    }

    fun handleAppLaunchEvent(event: AppLaunchEvent) {
        val pkg = event.packageName
        val oldPkg = currentForegroundPackage
        currentForegroundPackage = pkg
        _currentForegroundEvent.value = event
        _lastDetectionLatency.value = event.latencyMs

        scope.launch {
            // If app changed, pause session on previous app
            if (oldPkg != null && oldPkg != pkg) {
                activeWalletHeartbeatJob?.cancel()
                activeWalletHeartbeatJob = null
                walletService?.pauseOrEndSession()
            }

            val currentModeStr = preferencesManager?.getUserMode()
                ?: try { com.digitaldiscipline.spike.DigitalDisciplineApp.instance.preferencesManager.getUserMode() } catch (_: Exception) { com.digitaldiscipline.spike.data.local.entities.UserMode.SELF.name }
            val currentUserMode = try { com.digitaldiscipline.spike.data.local.entities.UserMode.valueOf(currentModeStr) } catch (_: Exception) { com.digitaldiscipline.spike.data.local.entities.UserMode.SELF }

            val resolvedResult = behaviourPolicyResolver?.resolvePolicy(pkg, userMode = currentUserMode)
            val rule = when (resolvedResult) {
                is PolicyResolutionResult.ParentPolicyMatch -> resolvedResult.appRule
                is PolicyResolutionResult.BehaviourPolicyMatch -> resolvedResult.resolvedAppRule
                is PolicyResolutionResult.NoMatch -> policyRepository.getRuleForPackage(pkg)
                null -> policyRepository.getRuleForPackage(pkg)
            }
            val nowElapsed = SystemClock.elapsedRealtime()

            if (rule != null && rule.isEnabled && rule.mode != RuleMode.ALLOW) {
                analyticsRepository.recordAppOpened(pkg, rule.appDisplayName)

                // MANDATORY: Check if Parent Policy is active
                val isParentRule = resolvedResult is PolicyResolutionResult.ParentPolicyMatch

                // 1. Check traditional temporary unlock first
                val isUnlocked = policyRepository.isTemporarilyUnlocked(pkg, nowElapsed)

                // 2. Check Wallet Session (works across multiple sessions until balance runs out)
                var walletHasAccess = false
                if (walletService != null) {
                    val sessionResult = walletService.startOrResumeSession(pkg, nowElapsed)
                    if (sessionResult is SessionStartResult.Started || sessionResult is SessionStartResult.Resumed) {
                        walletHasAccess = true
                        startWalletHeartbeat(pkg, rule)
                    }
                }

                if (isUnlocked || walletHasAccess) {
                    val remaining = if (walletHasAccess) {
                        walletService?.getWallet()?.availableSeconds ?: policyRepository.getRemainingUnlockSeconds(pkg, nowElapsed)
                    } else {
                        policyRepository.getRemainingUnlockSeconds(pkg, nowElapsed)
                    }
                    EventLogger.log(
                        source = "POLICY_ENGINE",
                        packageName = pkg,
                        eventType = "ACCESS_ALLOWED_TEMPORARY",
                        details = "Remaining: ${remaining}s | WalletActive: $walletHasAccess"
                    )
                    mainHandler.post {
                        currentEnforcementStrategy.liftRestriction(pkg)
                    }
                } else {
                    // If overlay is already active for this target, debounce repeated window changes
                    if (overlayManager.isOverlayActive() && currentForegroundPackage == pkg) {
                        return@launch
                    }

                    val level = currentEscalationLevelMap[pkg] ?: 1

                    analyticsRepository.recordAppBlocked(pkg, rule.appDisplayName)

                    EventLogger.log(
                        source = "POLICY_ENGINE",
                        packageName = pkg,
                        eventType = "RESTRICTION_ENFORCED",
                        latencyMs = event.latencyMs,
                        details = "Target: ${rule.appDisplayName} ($pkg) | Mode: ${rule.mode} | Level: $level"
                    )

                    mainHandler.post {
                        currentEnforcementStrategy.enforceRestriction(
                            packageName = pkg,
                            appDisplayName = rule.appDisplayName,
                            unlockDurationSeconds = rule.unlockDurationSeconds,
                            attemptNumber = level,
                            ruleMode = rule.mode,
                            pauseDurationSeconds = rule.pauseDurationSeconds,
                            breathingDurationSeconds = rule.breathingDurationSeconds,
                            squatsTargetCount = rule.squatsTargetCount
                        )
                    }
                }
            } else {
                if (pkg != context.packageName) {
                    mainHandler.post {
                        overlayManager.hideOverlay()
                    }
                }
            }
        }
    }

    private fun startWalletHeartbeat(pkg: String, rule: AppRuleEntity) {
        activeWalletHeartbeatJob?.cancel()
        activeWalletHeartbeatJob = scope.launch(Dispatchers.IO) {
            while (isActive && currentForegroundPackage == pkg && !isScreenOff) {
                delay(1000L)
                if (!isActive || currentForegroundPackage != pkg || isScreenOff) break

                analyticsRepository.recordUsageSeconds(pkg, 1L)
                val updateRes = walletService?.heartbeatOrUpdateSession(SystemClock.elapsedRealtime())
                if (updateRes is SessionUpdateResult.Expired || updateRes is SessionUpdateResult.RebootInvalidated) {
                    policyRepository.revokeTemporaryUnlock(pkg)
                    val level = currentEscalationLevelMap[pkg] ?: 1
                    mainHandler.post {
                        currentEnforcementStrategy.enforceRestriction(
                            packageName = pkg,
                            appDisplayName = rule.appDisplayName,
                            unlockDurationSeconds = rule.unlockDurationSeconds,
                            attemptNumber = level,
                            ruleMode = rule.mode,
                            pauseDurationSeconds = rule.pauseDurationSeconds,
                            breathingDurationSeconds = rule.breathingDurationSeconds,
                            squatsTargetCount = rule.squatsTargetCount
                        )
                    }
                    break
                }
            }
        }
    }

    suspend fun grantTemporaryUnlock(packageName: String, durationMs: Long, reason: String) {
        val rule = policyRepository.getRuleForPackage(packageName)
        val appName = rule?.appDisplayName ?: packageName

        policyRepository.grantTemporaryUnlock(packageName, durationMs, reason)
        analyticsRepository.recordInterventionCompleted(
            packageName = packageName,
            appDisplayName = appName,
            interventionType = reason,
            durationSeconds = 10,
            earnedDurationSeconds = (durationMs / 1000).toInt()
        )

        mainHandler.post {
            currentEnforcementStrategy.liftRestriction(packageName)
        }

        val durationSec = durationMs / 1000L
        EventLogger.log(
            source = "POLICY_ENGINE",
            packageName = packageName,
            eventType = "TEMPORARY_UNLOCK_GRANTED",
            details = "Duration: ${durationSec}s | Reason: $reason"
        )

        // Schedule exact hardware-level alarm (works even if background coroutines or handlers are throttled)
        scheduleExactExpiryAlarm(packageName, durationMs)

        activeUnlockJob?.cancel()
        activeUnlockJob = scope.launch {
            delay(durationMs)
            onTemporaryUnlockExpired(packageName)
        }
    }

    fun scheduleExactExpiryAlarm(packageName: String, durationMs: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ExactUnlockExpiryReceiver::class.java).apply {
                action = ExactUnlockExpiryReceiver.ACTION_EXACT_UNLOCK_EXPIRED
                putExtra(ExactUnlockExpiryReceiver.EXTRA_PACKAGE_NAME, packageName)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(context, packageName.hashCode(), intent, flags)
            val triggerAtElapsed = SystemClock.elapsedRealtime() + durationMs

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtElapsed, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtElapsed, pendingIntent)
            }
            EventLogger.log("POLICY_ENGINE", packageName, "EXACT_ALARM_SCHEDULED", details = "Armed at +${durationMs / 1000}s")
        } catch (t: Throwable) {
            EventLogger.log("POLICY_ENGINE", packageName, "ALARM_SCHEDULE_ERROR", details = t.message ?: "Failed to set exact alarm")
        }
    }

    fun cancelExactExpiryAlarm(packageName: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ExactUnlockExpiryReceiver::class.java).apply {
                action = ExactUnlockExpiryReceiver.ACTION_EXACT_UNLOCK_EXPIRED
                putExtra(ExactUnlockExpiryReceiver.EXTRA_PACKAGE_NAME, packageName)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(context, packageName.hashCode(), intent, flags)
            alarmManager.cancel(pendingIntent)
        } catch (_: Throwable) {}
    }

    suspend fun onTemporaryUnlockExpired(packageName: String) {
        cancelExactExpiryAlarm(packageName)
        policyRepository.revokeTemporaryUnlock(packageName)
        walletService?.heartbeatOrUpdateSession(SystemClock.elapsedRealtime())

        EventLogger.log(
            source = "POLICY_ENGINE",
            packageName = packageName,
            eventType = "TEMPORARY_UNLOCK_EXPIRED",
            details = "Returning to BLOCKED state"
        )

        val currentModeStr = preferencesManager?.getUserMode()
            ?: try { com.digitaldiscipline.spike.DigitalDisciplineApp.instance.preferencesManager.getUserMode() } catch (_: Exception) { UserMode.SELF.name }
        val currentUserMode = try { UserMode.valueOf(currentModeStr) } catch (_: Exception) { UserMode.SELF }

        val resolvedResult = behaviourPolicyResolver?.resolvePolicy(packageName, userMode = currentUserMode)
        val rule = when (resolvedResult) {
            is PolicyResolutionResult.ParentPolicyMatch -> resolvedResult.appRule
            is PolicyResolutionResult.BehaviourPolicyMatch -> resolvedResult.resolvedAppRule
            is PolicyResolutionResult.NoMatch -> policyRepository.getRuleForPackage(packageName)
            null -> policyRepository.getRuleForPackage(packageName)
        }

        if (rule != null) {
            val level = currentEscalationLevelMap[packageName] ?: 1
            mainHandler.post {
                currentEnforcementStrategy.enforceRestriction(
                    packageName = packageName,
                    appDisplayName = rule.appDisplayName,
                    unlockDurationSeconds = rule.unlockDurationSeconds,
                    attemptNumber = level,
                    ruleMode = rule.mode,
                    pauseDurationSeconds = rule.pauseDurationSeconds,
                    breathingDurationSeconds = rule.breathingDurationSeconds,
                    squatsTargetCount = rule.squatsTargetCount
                )
            }
        }
    }

    fun triggerTestUnlock(packageName: String, durationSeconds: Int = 60) {
        scope.launch {
            grantTemporaryUnlock(packageName, durationSeconds * 1000L, "TEST_TRIGGER")
        }
    }

    fun triggerTestBlock(packageName: String) {
        scope.launch {
            policyRepository.revokeTemporaryUnlock(packageName)
            val rule = policyRepository.getRuleForPackage(packageName)
            val appName = rule?.appDisplayName ?: packageName
            val unlockSec = rule?.unlockDurationSeconds ?: 600
            val level = currentEscalationLevelMap[packageName] ?: 1
            val mode = rule?.mode ?: RuleMode.EARN
            val pauseSec = rule?.pauseDurationSeconds ?: 10
            val breathSec = rule?.breathingDurationSeconds ?: 30
            val squatsTarget = rule?.squatsTargetCount ?: 10

            mainHandler.post {
                currentEnforcementStrategy.enforceRestriction(
                    packageName = packageName,
                    appDisplayName = appName,
                    unlockDurationSeconds = unlockSec,
                    attemptNumber = level,
                    ruleMode = mode,
                    pauseDurationSeconds = pauseSec,
                    breathingDurationSeconds = breathSec,
                    squatsTargetCount = squatsTarget
                )
            }
        }
    }
}

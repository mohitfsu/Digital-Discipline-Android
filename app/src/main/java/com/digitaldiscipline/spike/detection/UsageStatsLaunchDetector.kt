package com.digitaldiscipline.spike.detection

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.digitaldiscipline.spike.logging.EventLogger
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class UsageStatsLaunchDetector(
    private val context: Context,
    var pollingIntervalMs: Long = 500L
) : AppLaunchDetector {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pollingJob: Job? = null
    private val _isRunning = AtomicBoolean(false)
    private var lastRecordedEventTime: Long = System.currentTimeMillis()
    private var lastForegroundPackage: String? = null

    override fun startMonitoring(callback: (AppLaunchEvent) -> Unit) {
        if (_isRunning.getAndSet(true)) return

        lastRecordedEventTime = System.currentTimeMillis() - 2000L
        pollingJob = scope.launch {
            while (isActive && _isRunning.get()) {
                val now = System.currentTimeMillis()
                queryLatestForegroundEvent(lastRecordedEventTime, now, callback)
                delay(pollingIntervalMs)
            }
        }

        EventLogger.log(
            source = "USAGE_STATS",
            packageName = "system",
            eventType = "DETECTOR_STARTED",
            details = "Polling interval: ${pollingIntervalMs}ms"
        )
    }

    override fun stopMonitoring() {
        if (!_isRunning.getAndSet(false)) return
        pollingJob?.cancel()
        pollingJob = null

        EventLogger.log(
            source = "USAGE_STATS",
            packageName = "system",
            eventType = "DETECTOR_STOPPED"
        )
    }

    override fun isPermissionGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun getDetectorType(): DetectorType = DetectorType.USAGE_STATS

    override fun isRunning(): Boolean = _isRunning.get()

    private fun queryLatestForegroundEvent(
        beginTime: Long,
        endTime: Long,
        callback: (AppLaunchEvent) -> Unit
    ) {
        if (usageStatsManager == null || !isPermissionGranted()) return

        try {
            val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
            val event = UsageEvents.Event()

            var latestResumedEvent: UsageEvents.Event? = null

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)

                // Track ACTIVITY_RESUMED (API 29+) or legacy MOVE_TO_FOREGROUND
                val isForegroundEvent = when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> true
                    @Suppress("DEPRECATION")
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> true
                    else -> false
                }

                if (isForegroundEvent) {
                    if (latestResumedEvent == null || event.timeStamp >= latestResumedEvent.timeStamp) {
                        latestResumedEvent = UsageEvents.Event().apply {
                            // Copy event data
                            @Suppress("DEPRECATION")
                            val isResumed = event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                            if (isResumed) {
                                // Keep reference to the latest event properties
                            }
                        }
                        // Assign values
                        val pkg = event.packageName
                        val cls = event.className
                        val time = event.timeStamp

                        if (time > lastRecordedEventTime) {
                            lastRecordedEventTime = time
                            if (pkg != lastForegroundPackage) {
                                lastForegroundPackage = pkg
                                val detectionTime = System.currentTimeMillis()
                                val launchEvent = AppLaunchEvent(
                                    packageName = pkg,
                                    className = cls,
                                    eventTimestamp = time,
                                    detectionTimestamp = detectionTime,
                                    source = DetectorType.USAGE_STATS,
                                    eventType = "ACTIVITY_RESUMED"
                                )

                                EventLogger.log(
                                    source = "USAGE_STATS",
                                    packageName = pkg,
                                    eventType = "FOREGROUND",
                                    latencyMs = launchEvent.latencyMs,
                                    details = "Interval=${pollingIntervalMs}ms Class=${cls ?: "N/A"}"
                                )

                                callback(launchEvent)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            EventLogger.log(
                source = "USAGE_STATS",
                packageName = "error",
                eventType = "QUERY_EXCEPTION",
                details = e.message ?: "Unknown query error"
            )
        }
    }
}

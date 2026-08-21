package com.digitaldiscipline.spike.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.digitaldiscipline.spike.DigitalDisciplineApp
import com.digitaldiscipline.spike.behaviour.planner.DailyActionPlanner
import com.digitaldiscipline.spike.data.local.entities.UserMode
import com.digitaldiscipline.spike.logging.EventLogger
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

/**
 * Phase 4D-3 — Action Reminder Worker
 *
 * Fired mid-day by WorkManager (battery-conscious, no foreground service).
 * Evaluates NEXT_ACTION, DISTRACTION_PREEMPTION, and MISSED_ACTION types.
 *
 * Contract:
 * - Does NOT touch the enforcement path.
 * - Does NOT modify wallet balance.
 * - Does NOT alter AppRuleEntity or Parent Mode policies.
 * - Completes in < 500ms on any normal device.
 */
class ActionReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? DigitalDisciplineApp ?: return Result.success()
        return try {
            val ctx = buildNotificationContext(app) ?: run {
                EventLogger.log("NOTIFICATION", "system", "ACTION_WORKER_SKIP",
                    details = "Not Self Mode or no active plan")
                return Result.success()
            }
            val prefs = app.preferencesManager.loadNotificationPreferences()
            val governor = app.notificationFrequencyGovernor
            val historyRepo = app.notificationHistoryRepository

            val typesToEvaluate = listOf(
                NotificationType.NEXT_ACTION,
                NotificationType.DISTRACTION_PREEMPTION,
                NotificationType.MISSED_ACTION
            )

            for (type in typesToEvaluate) {
                val decision = SmartNotificationEngine.evaluate(type, ctx, prefs)
                if (decision is NotificationDecision.Show) {
                    if (governor.canSend(type, prefs)) {
                        val posted = NotificationChannelManager.postNotification(applicationContext, decision.candidate)
                        if (posted) {
                            governor.recordSent(type)
                            historyRepo.appendRecord(
                                NotificationRecord(
                                    type = type.name,
                                    timestampMs = System.currentTimeMillis(),
                                    reason = decision.candidate.reason,
                                    goalId = decision.candidate.goalId,
                                    actionId = decision.candidate.actionId
                                )
                            )
                            EventLogger.log("NOTIFICATION", "system", "ACTION_NOTIF_SENT",
                                details = "${type.name}: ${decision.candidate.title}")
                        }
                        break // Only one per worker run
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            EventLogger.log("NOTIFICATION", "system", "ACTION_WORKER_ERROR", details = e.message ?: "Unknown")
            Result.retry()
        }
    }

    private suspend fun buildNotificationContext(app: DigitalDisciplineApp): NotificationContext? {
        val userMode = app.preferencesManager.userModeFlow.first()
        if (userMode != UserMode.SELF.name) return null

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val cal = Calendar.getInstance()

        val activeGoal = app.behaviourRepository.getAllGoalsFlow().first()
            .firstOrNull { it.active && it.mode == UserMode.SELF.name } ?: return null

        val progress = app.behaviourRepository.getProgressForGoalFlow(activeGoal.goalId)
            .first().firstOrNull { it.dateString == today }

        val behaviour = app.behaviourRepository.getAllBehavioursFlow().first().firstOrNull()
        val policy = app.behaviourRepository.getAllPoliciesFlow().first().firstOrNull()
        val plan = DailyActionPlanner.planDailyActions(activeGoal, progress, behaviour, policy)

        val walletBalanceSec = app.walletService.getWalletFlow().first()?.availableSeconds ?: 0

        val allRecentInterventions = app.analyticsRepository.getRecentInterventionEventsFlow(200).first()
        val since24hMs = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        val since14dMs = System.currentTimeMillis() - 14 * 24 * 60 * 60 * 1000L
        val recentInterventions = allRecentInterventions.filter { it.timestamp >= since24hMs }
        val interventions14d = allRecentInterventions.filter { it.timestamp >= since14dMs }
        val peakHour = interventions14d.map { it.hourOfDay }
            .groupBy { it }.maxByOrNull { it.value.size }?.key

        val lastReflectionDate = app.preferencesManager.lastReflectionDateFlow.first()
        val history = app.notificationHistoryRepository.loadHistory()

        val latestReview = app.personalizationRepository.getLatestWeeklyReview()
        val weeklyReviewDue = latestReview == null ||
                (System.currentTimeMillis() - latestReview.generatedAt) >= 7 * 24 * 60 * 60 * 1000L

        return NotificationContext(
            isSelfMode = true,
            isParentMode = false,
            hasActivePlan = true,
            goalTitle = activeGoal.title,
            goalCategory = activeGoal.category,
            goalUnit = activeGoal.unit,
            dailyTarget = activeGoal.dailyTarget,
            completedToday = progress?.completedCount ?: 0,
            isGoalComplete = plan.isGoalComplete,
            nextActionTitle = plan.nextAction?.title ?: "",
            nextActionId = plan.nextAction?.actionId ?: "",
            walletBalanceSeconds = walletBalanceSec,
            distractionPeakHour = peakHour,
            distractionPeakDayOfWeek = null,
            distractionDataPoints = interventions14d.size,
            recentInterventionCount = recentInterventions.size,
            rapidReopenCount = recentInterventions.count { it.reopenWithin5Minutes },
            behaviourMomentumScore = 50,
            currentHour = cal.get(Calendar.HOUR_OF_DAY),
            currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK),
            reflectionCompletedToday = lastReflectionDate == today,
            weeklyReviewDue = weeklyReviewDue,
            notificationHistory = history
        )
    }
}

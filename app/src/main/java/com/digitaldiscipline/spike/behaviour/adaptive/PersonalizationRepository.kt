package com.digitaldiscipline.spike.behaviour.adaptive

import com.digitaldiscipline.spike.analytics.LocalAnalyticsRepository
import com.digitaldiscipline.spike.behaviour.BehaviourRepository
import com.digitaldiscipline.spike.data.local.dao.PersonalizationProfileDao
import com.digitaldiscipline.spike.data.local.dao.PlanAdjustmentDao
import com.digitaldiscipline.spike.data.local.dao.WeeklyReviewDao
import com.digitaldiscipline.spike.data.local.entities.*
import com.digitaldiscipline.spike.wallet.EarnedTimeWalletService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class PersonalizationRepository(
    private val profileDao: PersonalizationProfileDao,
    private val adjustmentDao: PlanAdjustmentDao,
    private val weeklyReviewDao: WeeklyReviewDao,
    private val behaviourRepository: BehaviourRepository,
    private val analyticsRepository: LocalAnalyticsRepository,
    private val walletService: EarnedTimeWalletService
) {

    fun getProfileFlow(): Flow<PersonalizationProfileEntity?> = profileDao.getProfileFlow()

    fun getLatestPendingAdjustmentFlow(): Flow<PlanAdjustmentEntity?> = adjustmentDao.getLatestPendingAdjustmentFlow()

    fun getLatestWeeklyReviewFlow(): Flow<WeeklyReviewEntity?> = weeklyReviewDao.getLatestWeeklyReviewFlow()

    suspend fun getProfile(): PersonalizationProfileEntity? = profileDao.getProfile()

    suspend fun getLatestPendingAdjustment(): PlanAdjustmentEntity? = adjustmentDao.getLatestPendingAdjustment()

    suspend fun getLatestWeeklyReview(): WeeklyReviewEntity? = weeklyReviewDao.getLatestWeeklyReview()

    /**
     * Recalculates user profile and generates deterministic plan suggestion if warranted.
     * Off-path: Invoked on dashboard load, weekly review, or daily rollup.
     */
    suspend fun recalculateProfileAndSuggestions() {
        val events = analyticsRepository.getRecentInterventionEventsFlow(100).firstOrNull() ?: emptyList()
        val transactions = walletService.getRecentTransactionsFlow("wallet_self", 50).firstOrNull() ?: emptyList()
        val activeGoal = behaviourRepository.getActiveGoals().firstOrNull()
        val policies = if (activeGoal != null) behaviourRepository.getPoliciesForGoal(activeGoal.goalId) else emptyList()
        val behaviours = behaviourRepository.getAllBehaviours()
        val progressList = if (activeGoal != null) behaviourRepository.getProgressForGoalFlow(activeGoal.goalId).firstOrNull() ?: emptyList() else emptyList()

        // 1. Calculate & Save Profile
        val profile = AdaptivePlanEngine.calculatePersonalizationProfile(
            events = events,
            transactions = transactions,
            sessions = emptyList(),
            progressList = progressList
        )
        profileDao.insertOrUpdateProfile(profile)

        // 2. Evaluate Primary Recommendation
        val pastAdjustments = adjustmentDao.getAllAdjustmentsFlow().firstOrNull() ?: emptyList()
        val recommendation = AdaptivePlanEngine.generatePrimaryRecommendation(
            currentGoal = activeGoal,
            currentPolicies = policies,
            currentBehaviours = behaviours,
            events = events,
            transactions = transactions,
            sessions = emptyList(),
            previousAdjustments = pastAdjustments
        )

        // 3. Save as PENDING PlanAdjustmentEntity if not INSUFFICIENT_DATA and no identical pending adjustment exists
        if (recommendation.type != RecommendationType.INSUFFICIENT_DATA && recommendation.type != RecommendationType.KEEP_PLAN) {
            val existingPending = adjustmentDao.getLatestPendingAdjustment()
            if (existingPending == null || existingPending.recommendationType != recommendation.type.name) {
                val adjustment = PlanAdjustmentEntity(
                    adjustmentId = "adj_${UUID.randomUUID()}",
                    goalId = activeGoal?.goalId ?: "",
                    createdAt = System.currentTimeMillis(),
                    reason = recommendation.explanation,
                    recommendationType = recommendation.type.name,
                    currentConfiguration = recommendation.currentConfiguration,
                    suggestedConfiguration = recommendation.suggestedConfiguration,
                    status = AdjustmentStatus.PENDING.name,
                    cooldownSeconds = recommendation.cooldownSeconds
                )
                // Expire older pending adjustments before creating new
                adjustmentDao.expireOtherPendingAdjustments(adjustment.adjustmentId)
                adjustmentDao.insertAdjustment(adjustment)
            }
        }
    }

    /**
     * Explicitly applies a confirmed plan adjustment.
     */
    suspend fun applyAdjustment(adjustment: PlanAdjustmentEntity) {
        val activeGoal = behaviourRepository.getActiveGoals().firstOrNull()
        val policies = if (activeGoal != null) behaviourRepository.getPoliciesForGoal(activeGoal.goalId) else emptyList()
        val behaviours = behaviourRepository.getAllBehaviours()

        when (adjustment.recommendationType) {
            RecommendationType.SHORTER_INTERVENTION.name -> {
                val activePolicy = policies.firstOrNull { it.enabled }
                val activeBehaviour = behaviours.firstOrNull { it.behaviourId == activePolicy?.replacementBehaviourId }
                if (activeBehaviour != null && activeBehaviour.targetCount > 5) {
                    val updated = activeBehaviour.copy(
                        targetCount = 5,
                        title = activeBehaviour.title.replace(activeBehaviour.targetCount.toString(), "5")
                    )
                    behaviourRepository.saveBehaviour(updated)
                }
            }
            RecommendationType.REDUCE_REWARD.name -> {
                policies.forEach { pol ->
                    val updated = pol.copy(earnedSeconds = 300) // 5 mins
                    behaviourRepository.savePolicy(updated)
                }
            }
            RecommendationType.CHANGE_INTERVENTION.name -> {
                val targetBehaviour = behaviours.firstOrNull {
                    it.type.equals(adjustment.suggestedConfiguration, ignoreCase = true)
                }
                if (targetBehaviour != null) {
                    policies.forEach { pol ->
                        val updated = pol.copy(replacementBehaviourId = targetBehaviour.behaviourId)
                        behaviourRepository.savePolicy(updated)
                    }
                }
            }
            RecommendationType.ADD_COOLDOWN.name -> {
                // Configured into adjustment status
            }
        }

        val updatedAdjustment = adjustment.copy(
            status = AdjustmentStatus.ACCEPTED.name,
            appliedAt = System.currentTimeMillis()
        )
        adjustmentDao.updateAdjustment(updatedAdjustment)
    }

    /**
     * Explicitly rejects a suggested adjustment, keeping existing plan unchanged.
     */
    suspend fun rejectAdjustment(adjustment: PlanAdjustmentEntity) {
        val updatedAdjustment = adjustment.copy(
            status = AdjustmentStatus.REJECTED.name,
            rejectedAt = System.currentTimeMillis()
        )
        adjustmentDao.updateAdjustment(updatedAdjustment)
    }

    /**
     * Generates a weekly review snapshot and persists to Room.
     */
    suspend fun generateWeeklyReviewSnapshot(weekStart: Long, weekEnd: Long): WeeklyReviewEntity {
        val events = analyticsRepository.getRecentInterventionEventsFlow(200).firstOrNull() ?: emptyList()
        val transactions = walletService.getRecentTransactionsFlow("wallet_self", 100).firstOrNull() ?: emptyList()
        val activeGoal = behaviourRepository.getActiveGoals().firstOrNull()

        val review = AdaptivePlanEngine.generateWeeklyReview(
            goal = activeGoal,
            events = events,
            transactions = transactions,
            sessions = emptyList(),
            weekStart = weekStart,
            weekEnd = weekEnd
        )
        weeklyReviewDao.insertWeeklyReview(review)
        return review
    }
}

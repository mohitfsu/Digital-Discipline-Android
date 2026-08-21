package com.digitaldiscipline.spike.behaviour.planner

import com.digitaldiscipline.spike.data.local.entities.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class DailyActionItem(
    val actionId: String,
    val title: String,
    val description: String,
    val category: String,
    val type: String,
    val targetCount: Int,
    val unit: String,
    val estimatedDurationSeconds: Int,
    val rewardSeconds: Int
)

data class DailyActionSegment(
    val index: Int,
    val title: String,
    val isCompleted: Boolean,
    val timestampText: String = ""
)

data class DailyActionPlan(
    val goalTitle: String,
    val goalCategory: String,
    val dailyTarget: Int,
    val completedCount: Int,
    val remainingCount: Int,
    val progressPercentage: Int,
    val isGoalComplete: Boolean,
    val nextAction: DailyActionItem?,
    val actionSegments: List<DailyActionSegment>,
    val totalActionsCount: Int,
    val completedActionsCount: Int,
    val completionMessage: String
)

object DailyActionPlanner {

    /**
     * Deterministically calculates today's action plan from active goal, progress, and configured replacement behaviour.
     */
    fun planDailyActions(
        goal: GoalEntity?,
        progress: GoalProgressEntity?,
        behaviour: ReplacementBehaviourEntity?,
        policy: BehaviourPolicyEntity?
    ): DailyActionPlan {
        val goalTitle = goal?.title ?: "Build Discipline"
        val goalCategory = goal?.category ?: GoalCategory.FITNESS.name
        val target = goal?.dailyTarget?.coerceAtLeast(1) ?: 1
        val completed = progress?.completedCount?.coerceAtLeast(0) ?: 0
        val remaining = (target - completed).coerceAtLeast(0)
        val progressPct = ((completed.toFloat() / target.toFloat()) * 100f).toInt().coerceIn(0, 100)
        val isComplete = completed >= target

        // Determine sensible chunk size based on category & behaviour
        val chunkSize = determineChunkSize(goalCategory, target, behaviour)
        val totalActions = ceil(target.toFloat() / chunkSize.toFloat()).toInt().coerceAtLeast(1)
        val completedActions = min(totalActions, (completed / chunkSize))

        // Create action segments
        val segments = (1..totalActions).map { idx ->
            val isSegCompleted = idx <= completedActions
            val unitName = goal?.unit ?: "units"
            val segSize = if (idx == totalActions && target % chunkSize != 0) target % chunkSize else chunkSize
            DailyActionSegment(
                index = idx,
                title = "$segSize $unitName",
                isCompleted = isSegCompleted
            )
        }

        // Determine next action
        val nextAction = if (!isComplete) {
            val actionTarget = min(chunkSize, remaining)
            val actionType = behaviour?.type ?: BehaviourType.SQUATS.name
            val actionTitle = when (goalCategory) {
                GoalCategory.FITNESS.name -> "$actionTarget ${behaviour?.unit ?: goal?.unit ?: "Squats"}"
                GoalCategory.STUDY.name -> "$actionTarget-Minute Study Block"
                GoalCategory.MINDFULNESS.name -> "${behaviour?.durationSeconds ?: 30}s Box Breathing"
                GoalCategory.READING.name -> "Read $actionTarget ${goal?.unit ?: "Pages"}"
                else -> "$actionTarget ${goal?.unit ?: "units"}"
            }
            val estDuration = behaviour?.durationSeconds?.coerceAtLeast(15) ?: when (goalCategory) {
                GoalCategory.STUDY.name -> actionTarget * 60
                GoalCategory.MINDFULNESS.name -> 30
                GoalCategory.READING.name -> actionTarget * 90
                else -> 60
            }
            val rewardSec = policy?.earnedSeconds ?: 600

            DailyActionItem(
                actionId = "action_${behaviour?.behaviourId ?: "default"}_$actionTarget",
                title = actionTitle,
                description = behaviour?.description ?: "Complete this small step toward your goal.",
                category = goalCategory,
                type = actionType,
                targetCount = actionTarget,
                unit = behaviour?.unit ?: goal?.unit ?: "reps",
                estimatedDurationSeconds = estDuration,
                rewardSeconds = rewardSec
            )
        } else {
            null
        }

        val completionMessage = if (isComplete) {
            "You've done what you planned today 🎉"
        } else {
            "$remaining ${goal?.unit ?: "units"} remaining today."
        }

        return DailyActionPlan(
            goalTitle = goalTitle,
            goalCategory = goalCategory,
            dailyTarget = target,
            completedCount = completed,
            remainingCount = remaining,
            progressPercentage = progressPct,
            isGoalComplete = isComplete,
            nextAction = nextAction,
            actionSegments = segments,
            totalActionsCount = totalActions,
            completedActionsCount = completedActions,
            completionMessage = completionMessage
        )
    }

    private fun determineChunkSize(category: String, totalTarget: Int, behaviour: ReplacementBehaviourEntity?): Int {
        if (behaviour != null && behaviour.targetCount > 0) {
            return min(behaviour.targetCount, totalTarget)
        }
        return when (category) {
            GoalCategory.FITNESS.name -> if (totalTarget >= 30) 10 else if (totalTarget >= 15) 5 else totalTarget
            GoalCategory.STUDY.name -> if (totalTarget >= 30) 15 else if (totalTarget >= 10) 10 else totalTarget
            GoalCategory.MINDFULNESS.name -> if (totalTarget >= 10) 5 else if (totalTarget >= 5) 2 else 1
            GoalCategory.READING.name -> if (totalTarget >= 20) 5 else if (totalTarget >= 10) 5 else totalTarget
            else -> max(1, totalTarget / 3).coerceAtLeast(1)
        }
    }
}

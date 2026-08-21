package com.digitaldiscipline.spike.intervention.adaptive

import com.digitaldiscipline.spike.intervention.session.PolicySource
import java.util.Calendar

enum class TimeBucket {
    MORNING,    // 05:00 - 11:59
    AFTERNOON,  // 12:00 - 16:59
    EVENING,    // 17:00 - 21:59
    NIGHT       // 22:00 - 04:59
}

data class InterventionContext(
    val triggerId: String,
    val targetPackage: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val dayOfWeek: Int = Calendar.getInstance().apply { timeInMillis = timestampMs }.get(Calendar.DAY_OF_WEEK),
    val timeBucket: TimeBucket = calculateTimeBucket(timestampMs),
    val policySource: PolicySource = PolicySource.SELF,
    val configuredInterventionId: String? = null,
    val recentInterventionIds: List<String> = emptyList(),
    val walletBalanceSeconds: Int = 0,
    val parentHardBlock: Boolean = false,
    val parentDelaySeconds: Int = 0
) {
    companion object {
        fun calculateTimeBucket(timestampMs: Long): TimeBucket {
            val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..11 -> TimeBucket.MORNING
                in 12..16 -> TimeBucket.AFTERNOON
                in 17..21 -> TimeBucket.EVENING
                else -> TimeBucket.NIGHT
            }
        }
    }
}

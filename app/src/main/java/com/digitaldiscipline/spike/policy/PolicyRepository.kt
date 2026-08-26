package com.digitaldiscipline.spike.policy

import android.os.SystemClock
import com.digitaldiscipline.spike.data.local.dao.AppRuleDao
import com.digitaldiscipline.spike.data.local.dao.GeofenceZoneDao
import com.digitaldiscipline.spike.data.local.dao.ScheduleDao
import com.digitaldiscipline.spike.data.local.dao.TemporaryUnlockDao
import com.digitaldiscipline.spike.data.local.entities.AppRuleEntity
import com.digitaldiscipline.spike.data.local.entities.GeofenceZoneEntity
import com.digitaldiscipline.spike.data.local.entities.RuleMode
import com.digitaldiscipline.spike.data.local.entities.ScheduleEntity
import com.digitaldiscipline.spike.data.local.entities.TemporaryUnlockEntity
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class PolicyRepository(
    private val appRuleDao: AppRuleDao,
    private val scheduleDao: ScheduleDao,
    private val temporaryUnlockDao: TemporaryUnlockDao,
    private val geofenceZoneDao: GeofenceZoneDao? = null,
    private val preferencesManager: PreferencesManager? = null
) {

    fun getAllRulesFlow(): Flow<List<AppRuleEntity>> = appRuleDao.getAllRulesFlow()
    fun getAllSchedulesFlow(): Flow<List<ScheduleEntity>> = scheduleDao.getAllSchedulesFlow()
    fun getAllGeofenceZonesFlow(): Flow<List<GeofenceZoneEntity>> =
        geofenceZoneDao?.getAllZonesFlow() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun getRuleForPackage(packageName: String): AppRuleEntity? {
        return appRuleDao.getRuleByPackage(packageName)
    }

    suspend fun saveRule(rule: AppRuleEntity) {
        appRuleDao.insertOrUpdate(rule)
    }

    suspend fun deleteRule(packageName: String) {
        appRuleDao.deleteByPackage(packageName)
        temporaryUnlockDao.deleteUnlock(packageName)
    }

    suspend fun deleteAllRules() {
        appRuleDao.deleteAllRules()
    }

    suspend fun saveSchedule(schedule: ScheduleEntity): Long {
        return scheduleDao.insertSchedule(schedule)
    }

    suspend fun updateSchedule(schedule: ScheduleEntity) {
        scheduleDao.updateSchedule(schedule)
    }

    suspend fun deleteSchedule(schedule: ScheduleEntity) {
        scheduleDao.deleteSchedule(schedule)
    }

    suspend fun deleteScheduleById(id: Long) {
        scheduleDao.deleteById(id)
    }

    suspend fun getAllSchedules(): List<ScheduleEntity> {
        return scheduleDao.getAllSchedules()
    }

    suspend fun getEnabledGeofenceZones(): List<GeofenceZoneEntity> {
        return geofenceZoneDao?.getEnabledZones() ?: emptyList()
    }

    suspend fun saveGeofenceZone(zone: GeofenceZoneEntity) {
        geofenceZoneDao?.insertZone(zone)
    }

    suspend fun updateGeofenceZone(zone: GeofenceZoneEntity) {
        geofenceZoneDao?.updateZone(zone)
    }

    suspend fun deleteGeofenceZone(zone: GeofenceZoneEntity) {
        geofenceZoneDao?.deleteZone(zone)
    }

    suspend fun deleteGeofenceZoneById(id: String) {
        geofenceZoneDao?.deleteById(id)
    }

    /**
     * Atomically replaces the current active local policy rules & schedules in Room.
     * Guarantees rollback protection if any record fails.
     */
    suspend fun transactionalUpdatePolicy(
        newRules: List<AppRuleEntity>,
        newSchedules: List<ScheduleEntity>
    ) {
        if (newRules.isNotEmpty()) {
            appRuleDao.deleteAllRules()
            appRuleDao.insertAll(newRules)
        }
        if (newSchedules.isNotEmpty()) {
            scheduleDao.deleteAllSchedules()
            scheduleDao.insertAll(newSchedules)
        }
    }

    suspend fun isPackageRestricted(packageName: String): Boolean {
        val rule = appRuleDao.getRuleByPackage(packageName) ?: return false
        if (!rule.isEnabled || rule.mode == RuleMode.ALLOW) return false

        // Check if device is physically inside an active Workplace or School Geofence Zone
        if (preferencesManager?.getIsInsideGeofence() == true) {
            val enabledZones = geofenceZoneDao?.getEnabledZones() ?: emptyList()
            if (enabledZones.isNotEmpty()) {
                return true
            }
        }

        // Check active schedule window
        val schedules = scheduleDao.getSchedulesForPackage(packageName)
        if (schedules.isNotEmpty()) {
            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val currentMinuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

            val matchingSchedule = schedules.firstOrNull { schedule ->
                if (!schedule.isEnabled) return@firstOrNull false

                val matchesDay = if (schedule.daysOfWeekCsv.isNotBlank()) {
                    val days = schedule.daysOfWeekCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
                    days.contains(dayOfWeek)
                } else {
                    schedule.dayOfWeek == dayOfWeek
                }

                val startMin = schedule.startHour * 60 + schedule.startMinute
                val endMin = schedule.endHour * 60 + schedule.endMinute
                val inWindow = if (startMin <= endMin) {
                    currentMinuteOfDay in startMin..endMin
                } else {
                    // Overnight window e.g. 22:00 -> 06:00
                    currentMinuteOfDay >= startMin || currentMinuteOfDay <= endMin
                }

                matchesDay && inWindow
            }

            if (matchingSchedule != null) {
                return matchingSchedule.isBlocked
            }
        }

        return true
    }

    suspend fun isTemporarilyUnlocked(packageName: String, currentElapsedRealtime: Long): Boolean {
        val unlock = temporaryUnlockDao.getUnlock(packageName) ?: return false
        val isValid = unlock.isStillValid(currentElapsedRealtime)
        if (!isValid) {
            temporaryUnlockDao.deleteUnlock(packageName)
            com.digitaldiscipline.spike.logging.DiagnosticLogger.logUnlockExpired(packageName, "Monotonic or wall-clock window elapsed")
        }
        return isValid
    }

    suspend fun getRemainingUnlockSeconds(packageName: String, currentElapsedRealtime: Long): Long {
        val unlock = temporaryUnlockDao.getUnlock(packageName) ?: return 0L
        return unlock.remainingSeconds(currentElapsedRealtime)
    }

    suspend fun grantTemporaryUnlock(packageName: String, durationMs: Long, reason: String = "INTERVENTION_COMPLETED") {
        val nowElapsed = SystemClock.elapsedRealtime()
        val unlock = TemporaryUnlockEntity(
            packageName = packageName,
            unlockGrantedElapsedRealtime = nowElapsed,
            unlockExpiryElapsedRealtime = nowElapsed + durationMs,
            unlockDurationMs = durationMs,
            reason = reason
        )
        temporaryUnlockDao.insertOrUpdate(unlock)
        com.digitaldiscipline.spike.logging.DiagnosticLogger.logUnlockCreated(packageName, (durationMs / 1000).toInt())
    }

    suspend fun revokeTemporaryUnlock(packageName: String) {
        temporaryUnlockDao.deleteUnlock(packageName)
        com.digitaldiscipline.spike.logging.DiagnosticLogger.logUnlockExpired(packageName, "Manual / Emergency revocation")
    }

    suspend fun purgeExpiredUnlocks(currentElapsedRealtime: Long) {
        temporaryUnlockDao.purgeExpiredUnlocks(currentElapsedRealtime)
    }
}

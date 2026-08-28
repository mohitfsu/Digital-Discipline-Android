package com.digitaldiscipline.spike.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "digital_discipline_prefs")

open class PreferencesManager(private val context: Context? = null) {

    companion object {
        val KEY_FIRST_RUN_COMPLETED = booleanPreferencesKey("first_run_completed")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_PARENT_PIN_CONFIGURED = booleanPreferencesKey("parent_pin_configured")
        val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        val KEY_PAIRED_FAMILY_ID = stringPreferencesKey("paired_family_id")
        val KEY_PAIRED_CHILD_ID = stringPreferencesKey("paired_child_id")
        val KEY_PAIRED_CHILD_NAME = stringPreferencesKey("paired_child_name")
        val KEY_DEVICE_ROLE = stringPreferencesKey("device_role") // "PARENT_HUB", "CHILD_DEVICE"
        val KEY_USER_MODE = stringPreferencesKey("user_mode") // "PARENT", "SELF"
        val KEY_POLICY_VERSION = intPreferencesKey("policy_version")
        val KEY_PROTECTION_ENABLED = booleanPreferencesKey("protection_enabled")
        val KEY_LAST_POLICY_SYNC = longPreferencesKey("last_policy_sync")
        val KEY_APP_VERSION = stringPreferencesKey("app_version")
        val KEY_LAST_REFLECTION_DATE = stringPreferencesKey("last_reflection_date")
        val KEY_LAST_REFLECTION_MOOD = stringPreferencesKey("last_reflection_mood")
        val KEY_LAST_REFLECTION_HELPED = stringPreferencesKey("last_reflection_helped")
        val KEY_ACTIVE_POLICY_PROFILE = stringPreferencesKey("active_policy_profile") // CORPORATE, FAMILY, DEEP_WORK, CUSTOM
        val KEY_IS_INSIDE_GEOFENCE = booleanPreferencesKey("is_inside_geofence")
        val KEY_ACTIVE_GEOFENCE_NAME = stringPreferencesKey("active_geofence_name")

        // Phase 4D-3 — Smart Notification Preferences
        val KEY_NOTIF_ENABLED_DAILY_FOCUS = booleanPreferencesKey("notif_enabled_daily_focus")
        val KEY_NOTIF_ENABLED_ACTION_REMINDERS = booleanPreferencesKey("notif_enabled_action_reminders")
        val KEY_NOTIF_ENABLED_DISTRACTION_WINDOW = booleanPreferencesKey("notif_enabled_distraction_window")
        val KEY_NOTIF_ENABLED_SUCCESS = booleanPreferencesKey("notif_enabled_success")
        val KEY_NOTIF_ENABLED_EVENING_REFLECTION = booleanPreferencesKey("notif_enabled_evening_reflection")
        val KEY_NOTIF_ENABLED_WEEKLY_REVIEW = booleanPreferencesKey("notif_enabled_weekly_review")
        val KEY_NOTIF_FREQUENCY_MODE = stringPreferencesKey("notif_frequency_mode") // MINIMAL, BALANCED, HELPFUL

        // Phase 4E-1 — Self Mode First-Run Activation State
        val KEY_SELF_ONBOARDING_STATE = stringPreferencesKey("self_onboarding_state") // NOT_STARTED, IN_PROGRESS, READY, COMPLETED
        val KEY_SELF_ONBOARDING_STEP = intPreferencesKey("self_onboarding_step")

        // Phase 4E-2 — First-Win State Machine & Metrics
        val KEY_FIRST_WIN_STATE = stringPreferencesKey("first_win_state") // NOT_STARTED, PLAN_ACTIVE, ..., FIRST_WIN_COMPLETED
        val KEY_FIRST_WIN_PLAN_ID = stringPreferencesKey("first_win_plan_id")
        val KEY_FIRST_WIN_COMPLETED_AT = longPreferencesKey("first_win_completed_at")
        val KEY_FIRST_WIN_EARNED_SECONDS = intPreferencesKey("first_win_earned_seconds")
        val KEY_FIRST_WIN_USED_SECONDS = intPreferencesKey("first_win_used_seconds")
        val KEY_FIRST_WIN_SAVED_SECONDS = intPreferencesKey("first_win_saved_seconds")
        val KEY_FIRST_WIN_ACTION_TITLE = stringPreferencesKey("first_win_action_title")

        // Phase 4E-4 — Plan Continuity & Refinement
        val KEY_PLAN_CONTINUITY_STATE = stringPreferencesKey("plan_continuity_state")
        val KEY_LAST_PLAN_REVIEW_TIMESTAMP = longPreferencesKey("last_plan_review_timestamp")
        val KEY_PLAN_ACTIVE_WEEK_NUMBER = intPreferencesKey("plan_active_week_number")

        // Phase 4E-5 — Goal Lifecycle & Evolution
        val KEY_PRIMARY_GOAL_LIFECYCLE_STATE = stringPreferencesKey("primary_goal_lifecycle_state")
        val KEY_PRIMARY_GOAL_PAUSED_AT = longPreferencesKey("primary_goal_paused_at")
        val KEY_PRIMARY_GOAL_COMPLETED_AT = longPreferencesKey("primary_goal_completed_at")

        // Custom Intervention Catalog Preferences
        val KEY_ENABLED_INTERVENTIONS = stringSetPreferencesKey("enabled_interventions")
        val KEY_ENABLED_CATEGORIES = stringSetPreferencesKey("enabled_categories")

        // Phase 8D — Premium Onboarding Additional State
        val KEY_USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")
        val KEY_ONBOARDING_BEHAVIOUR_PATTERN = stringPreferencesKey("onboarding_behaviour_pattern")
        val KEY_ONBOARDING_SCREEN_TIME_ESTIMATE = stringPreferencesKey("onboarding_screen_time_estimate")

        // Family & Office Mode Specific State
        val KEY_FAMILY_ROLE = stringPreferencesKey("family_role") // PARENT, CHILD
        val KEY_AUTO_BLOCK_GAMES = booleanPreferencesKey("auto_block_games")
        val KEY_AUTO_BLOCK_SOCIAL = booleanPreferencesKey("auto_block_social")
        val KEY_AUTO_BLOCK_STREAMING = booleanPreferencesKey("auto_block_streaming")

        val KEY_OFFICE_START_HOUR = intPreferencesKey("office_start_hour")
        val KEY_OFFICE_START_MINUTE = intPreferencesKey("office_start_minute")
        val KEY_OFFICE_END_HOUR = intPreferencesKey("office_end_hour")
        val KEY_OFFICE_END_MINUTE = intPreferencesKey("office_end_minute")
        val KEY_OFFICE_DAYS = stringPreferencesKey("office_days") // "2,3,4,5,6" (Mon-Fri)
        val KEY_OFFICE_DEEP_WORK_ACTIVE = booleanPreferencesKey("office_deep_work_active")
        val KEY_OFFICE_DEEP_WORK_EXPIRY = longPreferencesKey("office_deep_work_expiry")
        val KEY_OFFICE_MEETING_MODE = booleanPreferencesKey("office_meeting_mode")
        val KEY_HAS_CONFIGURED_CHALLENGES = booleanPreferencesKey("has_configured_challenges")
    }

    val lastReflectionDateFlow: Flow<String?> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_LAST_REFLECTION_DATE] }

    val lastReflectionMoodFlow: Flow<String?> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_LAST_REFLECTION_MOOD] }

    val lastReflectionHelpedFlow: Flow<String?> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_LAST_REFLECTION_HELPED] }

    val userModeFlow: Flow<String> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_USER_MODE] ?: "PARENT" }

    val firstRunCompletedFlow: Flow<Boolean> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_FIRST_RUN_COMPLETED] ?: false }

    val onboardingCompletedFlow: Flow<Boolean> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_ONBOARDING_COMPLETED] ?: false }

    val parentPinConfiguredFlow: Flow<Boolean> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_PARENT_PIN_CONFIGURED] ?: false }

    val protectionEnabledFlow: Flow<Boolean> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_PROTECTION_ENABLED] ?: true }

    val deviceIdFlow: Flow<String> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_DEVICE_ID] ?: "" }

    val pairedFamilyIdFlow: Flow<String?> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_PAIRED_FAMILY_ID] }

    val pairedChildIdFlow: Flow<String?> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_PAIRED_CHILD_ID] }

    val pairedChildNameFlow: Flow<String> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_PAIRED_CHILD_NAME] ?: "Child" }

    val policyVersionFlow: Flow<Int> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_POLICY_VERSION] ?: 1 }

    val deviceRoleFlow: Flow<String> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_DEVICE_ROLE] ?: "STANDALONE" }

    val lastPolicySyncFlow: Flow<Long> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_LAST_POLICY_SYNC] ?: 0L }

    val activePolicyProfileFlow: Flow<String> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_ACTIVE_POLICY_PROFILE] ?: "CORPORATE" }

    val isInsideGeofenceFlow: Flow<Boolean> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_IS_INSIDE_GEOFENCE] ?: false }

    val activeGeofenceNameFlow: Flow<String> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_ACTIVE_GEOFENCE_NAME] ?: "" }

    open suspend fun getActivePolicyProfile(): String {
        return context?.dataStore?.data?.first()?.get(KEY_ACTIVE_POLICY_PROFILE) ?: "CORPORATE"
    }

    open suspend fun setActivePolicyProfile(profile: String) {
        context?.dataStore?.edit { it[KEY_ACTIVE_POLICY_PROFILE] = profile }
    }

    open suspend fun getIsInsideGeofence(): Boolean {
        return context?.dataStore?.data?.first()?.get(KEY_IS_INSIDE_GEOFENCE) ?: false
    }

    open suspend fun getActiveGeofenceName(): String {
        return context?.dataStore?.data?.first()?.get(KEY_ACTIVE_GEOFENCE_NAME) ?: ""
    }

    open suspend fun setIsInsideGeofence(inside: Boolean, zoneName: String = "") {
        context?.dataStore?.edit {
            it[KEY_IS_INSIDE_GEOFENCE] = inside
            it[KEY_ACTIVE_GEOFENCE_NAME] = if (inside) zoneName else ""
        }
    }

    open suspend fun setFirstRunCompleted(completed: Boolean) {
        context?.dataStore?.edit { it[KEY_FIRST_RUN_COMPLETED] = completed }
    }

    open suspend fun setOnboardingCompleted(completed: Boolean) {
        context?.dataStore?.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    open suspend fun setParentPinConfigured(configured: Boolean) {
        context?.dataStore?.edit { it[KEY_PARENT_PIN_CONFIGURED] = configured }
    }

    open suspend fun setProtectionEnabled(enabled: Boolean) {
        context?.dataStore?.edit { it[KEY_PROTECTION_ENABLED] = enabled }
    }

    open suspend fun getOrCreateDeviceId(): String {
        var id = ""
        context?.dataStore?.edit { prefs ->
            id = prefs[KEY_DEVICE_ID] ?: UUID.randomUUID().toString().also { newId ->
                prefs[KEY_DEVICE_ID] = newId
            }
        }
        return id
    }

    open suspend fun setPairedFamilyId(familyId: String?) {
        context?.dataStore?.edit {
            if (familyId != null) it[KEY_PAIRED_FAMILY_ID] = familyId else it.remove(KEY_PAIRED_FAMILY_ID)
        }
    }

    open suspend fun setPairedChildId(childId: String?) {
        context?.dataStore?.edit {
            if (childId != null) it[KEY_PAIRED_CHILD_ID] = childId else it.remove(KEY_PAIRED_CHILD_ID)
        }
    }

    open suspend fun setPairedChildName(childName: String) {
        context?.dataStore?.edit { it[KEY_PAIRED_CHILD_NAME] = childName }
    }

    open suspend fun setPolicyVersion(version: Int) {
        context?.dataStore?.edit { it[KEY_POLICY_VERSION] = version }
    }

    open suspend fun setUserMode(mode: String) {
        context?.dataStore?.edit { it[KEY_USER_MODE] = mode }
    }

    open suspend fun getUserMode(): String {
        return try {
            userModeFlow.first()
        } catch (_: Exception) {
            "SELF"
        }
    }

    open suspend fun setDeviceRole(role: String) {
        context?.dataStore?.edit { it[KEY_DEVICE_ROLE] = role }
    }

    open suspend fun setLastPolicySync(timestamp: Long) {
        context?.dataStore?.edit { it[KEY_LAST_POLICY_SYNC] = timestamp }
    }

    open suspend fun saveDailyReflection(dateString: String, mood: String, helped: String) {
        context?.dataStore?.edit {
            it[KEY_LAST_REFLECTION_DATE] = dateString
            it[KEY_LAST_REFLECTION_MOOD] = mood
            it[KEY_LAST_REFLECTION_HELPED] = helped
        }
    }

    // -------------------------------------------------------------------------
    // Phase 4D-3 — Smart Notification Preferences
    // -------------------------------------------------------------------------

    /**
     * Load notification preferences from DataStore synchronously (suspend).
     * Returns sensible BALANCED defaults if not yet configured.
     */
    open suspend fun loadNotificationPreferences(): com.digitaldiscipline.spike.notification.NotificationPreferences {
        val dataStoreData = context?.dataStore?.data
            ?.catch { if (it is java.io.IOException) emit(emptyPreferences()) else throw it }
            ?: return com.digitaldiscipline.spike.notification.NotificationPreferences()
        val prefs = dataStoreData.first()
        val modeStr = prefs[KEY_NOTIF_FREQUENCY_MODE] ?: "BALANCED"
        val mode = try {
            com.digitaldiscipline.spike.notification.NotificationFrequencyMode.valueOf(modeStr)
        } catch (e: Exception) {
            com.digitaldiscipline.spike.notification.NotificationFrequencyMode.BALANCED
        }
        return com.digitaldiscipline.spike.notification.NotificationPreferences(
            enableDailyFocus         = prefs[KEY_NOTIF_ENABLED_DAILY_FOCUS] ?: true,
            enableActionReminders    = prefs[KEY_NOTIF_ENABLED_ACTION_REMINDERS] ?: true,
            enableDistractionWindow  = prefs[KEY_NOTIF_ENABLED_DISTRACTION_WINDOW] ?: true,
            enableSuccess            = prefs[KEY_NOTIF_ENABLED_SUCCESS] ?: true,
            enableEveningReflection  = prefs[KEY_NOTIF_ENABLED_EVENING_REFLECTION] ?: true,
            enableWeeklyReview       = prefs[KEY_NOTIF_ENABLED_WEEKLY_REVIEW] ?: true,
            frequencyMode            = mode
        )
    }

    open suspend fun saveNotificationPreferences(prefs: com.digitaldiscipline.spike.notification.NotificationPreferences) {
        context?.dataStore?.edit {
            it[KEY_NOTIF_ENABLED_DAILY_FOCUS]         = prefs.enableDailyFocus
            it[KEY_NOTIF_ENABLED_ACTION_REMINDERS]    = prefs.enableActionReminders
            it[KEY_NOTIF_ENABLED_DISTRACTION_WINDOW]  = prefs.enableDistractionWindow
            it[KEY_NOTIF_ENABLED_SUCCESS]             = prefs.enableSuccess
            it[KEY_NOTIF_ENABLED_EVENING_REFLECTION]  = prefs.enableEveningReflection
            it[KEY_NOTIF_ENABLED_WEEKLY_REVIEW]       = prefs.enableWeeklyReview
            it[KEY_NOTIF_FREQUENCY_MODE]              = prefs.frequencyMode.name
        }
    }

    // -------------------------------------------------------------------------
    // Phase 4E-1 — Self Mode First-Run Activation State
    // -------------------------------------------------------------------------

    val selfOnboardingStateFlow: Flow<String> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_SELF_ONBOARDING_STATE] ?: "NOT_STARTED" }

    val selfOnboardingStepFlow: Flow<Int> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_SELF_ONBOARDING_STEP] ?: 1 }

    open suspend fun setSelfOnboardingState(state: String) {
        context?.dataStore?.edit { it[KEY_SELF_ONBOARDING_STATE] = state }
    }

    open suspend fun setSelfOnboardingStep(step: Int) {
        context?.dataStore?.edit { it[KEY_SELF_ONBOARDING_STEP] = step }
    }

    // -------------------------------------------------------------------------
    // Phase 4E-2 — First-Win State Machine & Metrics
    // -------------------------------------------------------------------------

    val firstWinStateFlow: Flow<String> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_FIRST_WIN_STATE] ?: "NOT_STARTED" }

    val firstWinPlanIdFlow: Flow<String?> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_FIRST_WIN_PLAN_ID] }

    val firstWinCompletedAtFlow: Flow<Long> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_FIRST_WIN_COMPLETED_AT] ?: 0L }

    val firstWinEarnedSecondsFlow: Flow<Int> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_FIRST_WIN_EARNED_SECONDS] ?: 0 }

    val firstWinUsedSecondsFlow: Flow<Int> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_FIRST_WIN_USED_SECONDS] ?: 0 }

    val firstWinSavedSecondsFlow: Flow<Int> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_FIRST_WIN_SAVED_SECONDS] ?: 0 }

    val firstWinActionTitleFlow: Flow<String?> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_FIRST_WIN_ACTION_TITLE] }

    open suspend fun setFirstWinState(
        state: String,
        planId: String? = null,
        completedAt: Long? = null,
        earnedSeconds: Int? = null,
        usedSeconds: Int? = null,
        savedSeconds: Int? = null,
        actionTitle: String? = null
    ) {
        context?.dataStore?.edit { prefs ->
            prefs[KEY_FIRST_WIN_STATE] = state
            planId?.let { prefs[KEY_FIRST_WIN_PLAN_ID] = it }
            completedAt?.let { prefs[KEY_FIRST_WIN_COMPLETED_AT] = it }
            earnedSeconds?.let { prefs[KEY_FIRST_WIN_EARNED_SECONDS] = it }
            usedSeconds?.let { prefs[KEY_FIRST_WIN_USED_SECONDS] = it }
            savedSeconds?.let { prefs[KEY_FIRST_WIN_SAVED_SECONDS] = it }
            actionTitle?.let { prefs[KEY_FIRST_WIN_ACTION_TITLE] = it }
        }
    }

    // -------------------------------------------------------------------------
    // Phase 4E-4 — Plan Continuity & Refinement
    // -------------------------------------------------------------------------

    val planContinuityStateFlow: Flow<String?> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_PLAN_CONTINUITY_STATE] }

    val lastPlanReviewTimestampFlow: Flow<Long> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_LAST_PLAN_REVIEW_TIMESTAMP] ?: 0L }

    val planActiveWeekNumberFlow: Flow<Int> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_PLAN_ACTIVE_WEEK_NUMBER] ?: 1 }

    open suspend fun setPlanContinuityState(state: String) {
        context?.dataStore?.edit { it[KEY_PLAN_CONTINUITY_STATE] = state }
    }

    open suspend fun setLastPlanReviewTimestamp(timestamp: Long) {
        context?.dataStore?.edit { it[KEY_LAST_PLAN_REVIEW_TIMESTAMP] = timestamp }
    }

    open suspend fun setPlanActiveWeekNumber(weekNumber: Int) {
        context?.dataStore?.edit { it[KEY_PLAN_ACTIVE_WEEK_NUMBER] = weekNumber }
    }

    // -------------------------------------------------------------------------
    // Phase 4E-5 — Goal Lifecycle & Evolution
    // -------------------------------------------------------------------------

    val primaryGoalLifecycleStateFlow: Flow<String> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_PRIMARY_GOAL_LIFECYCLE_STATE] ?: "ACTIVE" }

    val primaryGoalPausedAtFlow: Flow<Long> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_PRIMARY_GOAL_PAUSED_AT] ?: 0L }

    val primaryGoalCompletedAtFlow: Flow<Long> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_PRIMARY_GOAL_COMPLETED_AT] ?: 0L }

    open suspend fun setPrimaryGoalLifecycleState(state: String) {
        context?.dataStore?.edit { it[KEY_PRIMARY_GOAL_LIFECYCLE_STATE] = state }
    }

    open suspend fun setPrimaryGoalPausedAt(timestamp: Long) {
        context?.dataStore?.edit { it[KEY_PRIMARY_GOAL_PAUSED_AT] = timestamp }
    }

    open suspend fun setPrimaryGoalCompletedAt(timestamp: Long) {
        context?.dataStore?.edit { it[KEY_PRIMARY_GOAL_COMPLETED_AT] = timestamp }
    }

    // -------------------------------------------------------------------------
    // Custom Intervention Catalog Preferences
    // -------------------------------------------------------------------------

    val enabledInterventionsFlow: Flow<Set<String>> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_ENABLED_INTERVENTIONS] ?: emptySet() }

    val enabledCategoriesFlow: Flow<Set<String>> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_ENABLED_CATEGORIES] ?: emptySet() }

    open suspend fun setEnabledInterventions(ids: Set<String>) {
        context?.dataStore?.edit { it[KEY_ENABLED_INTERVENTIONS] = ids }
    }

    open suspend fun setEnabledCategories(categories: Set<String>) {
        context?.dataStore?.edit { it[KEY_ENABLED_CATEGORIES] = categories }
    }

    val hasConfiguredChallengesFlow: Flow<Boolean> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_HAS_CONFIGURED_CHALLENGES] ?: false }

    open suspend fun setHasConfiguredChallenges(configured: Boolean) {
        context?.dataStore?.edit { it[KEY_HAS_CONFIGURED_CHALLENGES] = configured }
    }

    // -------------------------------------------------------------------------
    // Phase 8D — Premium Onboarding Additional State
    // -------------------------------------------------------------------------

    val userDisplayNameFlow: Flow<String> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_USER_DISPLAY_NAME] ?: "" }

    val onboardingBehaviourPatternFlow: Flow<String> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_ONBOARDING_BEHAVIOUR_PATTERN] ?: "" }

    val onboardingScreenTimeEstimateFlow: Flow<String> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_ONBOARDING_SCREEN_TIME_ESTIMATE] ?: "1–2 hours" }

    open suspend fun setUserDisplayName(name: String) {
        context?.dataStore?.edit { it[KEY_USER_DISPLAY_NAME] = name }
    }

    open suspend fun setOnboardingBehaviourPattern(pattern: String) {
        context?.dataStore?.edit { it[KEY_ONBOARDING_BEHAVIOUR_PATTERN] = pattern }
    }

    open suspend fun setOnboardingScreenTimeEstimate(estimate: String) {
        context?.dataStore?.edit { it[KEY_ONBOARDING_SCREEN_TIME_ESTIMATE] = estimate }
    }

    // -------------------------------------------------------------------------
    // Family & Office Mode Flows & Setters
    // -------------------------------------------------------------------------

    val familyRoleFlow: Flow<String> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_FAMILY_ROLE] ?: "PARENT" }

    val autoBlockGamesFlow: Flow<Boolean> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_AUTO_BLOCK_GAMES] ?: true }

    val autoBlockSocialFlow: Flow<Boolean> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_AUTO_BLOCK_SOCIAL] ?: true }

    val autoBlockStreamingFlow: Flow<Boolean> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_AUTO_BLOCK_STREAMING] ?: true }

    val officeStartHourFlow: Flow<Int> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_OFFICE_START_HOUR] ?: 9 }

    val officeStartMinuteFlow: Flow<Int> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_OFFICE_START_MINUTE] ?: 0 }

    val officeEndHourFlow: Flow<Int> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_OFFICE_END_HOUR] ?: 17 }

    val officeEndMinuteFlow: Flow<Int> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_OFFICE_END_MINUTE] ?: 0 }

    val officeDaysFlow: Flow<String> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_OFFICE_DAYS] ?: "2,3,4,5,6" } // Mon-Fri

    val officeDeepWorkActiveFlow: Flow<Boolean> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_OFFICE_DEEP_WORK_ACTIVE] ?: false }

    val officeDeepWorkExpiryFlow: Flow<Long> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_OFFICE_DEEP_WORK_EXPIRY] ?: 0L }

    val officeMeetingModeFlow: Flow<Boolean> = (context?.dataStore?.data ?: kotlinx.coroutines.flow.flowOf(emptyPreferences()))
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_OFFICE_MEETING_MODE] ?: false }

    open suspend fun setFamilyRole(role: String) {
        context?.dataStore?.edit { it[KEY_FAMILY_ROLE] = role }
    }

    open suspend fun setAutoBlockGames(enabled: Boolean) {
        context?.dataStore?.edit { it[KEY_AUTO_BLOCK_GAMES] = enabled }
    }

    open suspend fun setAutoBlockSocial(enabled: Boolean) {
        context?.dataStore?.edit { it[KEY_AUTO_BLOCK_SOCIAL] = enabled }
    }

    open suspend fun setAutoBlockStreaming(enabled: Boolean) {
        context?.dataStore?.edit { it[KEY_AUTO_BLOCK_STREAMING] = enabled }
    }

    open suspend fun setOfficeSchedule(startH: Int, startM: Int, endH: Int, endM: Int, days: String = "2,3,4,5,6") {
        context?.dataStore?.edit {
            it[KEY_OFFICE_START_HOUR] = startH
            it[KEY_OFFICE_START_MINUTE] = startM
            it[KEY_OFFICE_END_HOUR] = endH
            it[KEY_OFFICE_END_MINUTE] = endM
            it[KEY_OFFICE_DAYS] = days
        }
    }

    open suspend fun setOfficeDeepWork(active: Boolean, durationMinutes: Int = 0) {
        val expiry = if (active) System.currentTimeMillis() + (durationMinutes * 60_000L) else 0L
        context?.dataStore?.edit {
            it[KEY_OFFICE_DEEP_WORK_ACTIVE] = active
            it[KEY_OFFICE_DEEP_WORK_EXPIRY] = expiry
        }
    }

    open suspend fun setOfficeMeetingMode(active: Boolean) {
        context?.dataStore?.edit { it[KEY_OFFICE_MEETING_MODE] = active }
    }
}





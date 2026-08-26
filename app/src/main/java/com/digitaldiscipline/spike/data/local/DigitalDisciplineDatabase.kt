package com.digitaldiscipline.spike.data.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.digitaldiscipline.spike.data.local.dao.*
import com.digitaldiscipline.spike.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AppRuleEntity::class,
        DeviceEntity::class,
        ScheduleEntity::class,
        InterventionRuleEntity::class,
        TemporaryUnlockEntity::class,
        DailyUsageEntity::class,
        InterventionEventEntity::class,
        ProtectionStateEntity::class,
        DiagnosticEventEntity::class,
        GoalEntity::class,
        TriggerEntity::class,
        ReplacementBehaviourEntity::class,
        BehaviourPolicyEntity::class,
        GoalProgressEntity::class,
        EarnedTimeWalletEntity::class,
        WalletTransactionEntity::class,
        WalletSessionEntity::class,
        PlanAdjustmentEntity::class,
        PersonalizationProfileEntity::class,
        WeeklyReviewEntity::class,
        BehaviourExperimentEntity::class,
        InterventionAdaptiveAggregateEntity::class,
        GeofenceZoneEntity::class
    ],
    version = 11,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DigitalDisciplineDatabase : RoomDatabase() {

    abstract fun appRuleDao(): AppRuleDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun geofenceZoneDao(): GeofenceZoneDao
    abstract fun temporaryUnlockDao(): TemporaryUnlockDao
    abstract fun dailyUsageDao(): DailyUsageDao
    abstract fun interventionEventDao(): InterventionEventDao
    abstract fun protectionStateDao(): ProtectionStateDao
    abstract fun diagnosticEventDao(): DiagnosticEventDao
    abstract fun goalDao(): GoalDao
    abstract fun triggerDao(): TriggerDao
    abstract fun replacementBehaviourDao(): ReplacementBehaviourDao
    abstract fun behaviourPolicyDao(): BehaviourPolicyDao
    abstract fun goalProgressDao(): GoalProgressDao
    abstract fun earnedTimeWalletDao(): EarnedTimeWalletDao
    abstract fun walletTransactionDao(): WalletTransactionDao
    abstract fun walletSessionDao(): WalletSessionDao
    abstract fun planAdjustmentDao(): PlanAdjustmentDao
    abstract fun personalizationProfileDao(): PersonalizationProfileDao
    abstract fun weeklyReviewDao(): WeeklyReviewDao
    abstract fun behaviourExperimentDao(): BehaviourExperimentDao
    abstract fun interventionAdaptiveAggregateDao(): InterventionAdaptiveAggregateDao

    companion object {
        @Volatile
        private var INSTANCE: DigitalDisciplineDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS earned_time_wallets (
                        walletId TEXT PRIMARY KEY NOT NULL,
                        ownerId TEXT NOT NULL,
                        mode TEXT NOT NULL,
                        availableSeconds INTEGER NOT NULL,
                        lifetimeEarnedSeconds INTEGER NOT NULL,
                        lifetimeConsumedSeconds INTEGER NOT NULL,
                        dailyEarnedSeconds INTEGER NOT NULL,
                        dailyConsumedSeconds INTEGER NOT NULL,
                        dailyEarnCapSeconds INTEGER NOT NULL,
                        maxBalanceCapSeconds INTEGER NOT NULL,
                        maxSessionSeconds INTEGER NOT NULL,
                        lastDateString TEXT NOT NULL,
                        lastUpdatedElapsedRealtime INTEGER NOT NULL,
                        lastUpdatedWallClock INTEGER NOT NULL,
                        walletVersion INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS wallet_transactions (
                        transactionId TEXT PRIMARY KEY NOT NULL,
                        walletId TEXT NOT NULL,
                        type TEXT NOT NULL,
                        amountSeconds INTEGER NOT NULL,
                        balanceAfterSeconds INTEGER NOT NULL,
                        source TEXT NOT NULL,
                        triggerPackage TEXT,
                        idempotencyKey TEXT,
                        sessionId TEXT,
                        goalId TEXT,
                        timestampWallClock INTEGER NOT NULL,
                        elapsedRealtime INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_wallet_transactions_walletId ON wallet_transactions(walletId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_wallet_transactions_idempotencyKey ON wallet_transactions(idempotencyKey)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS wallet_sessions (
                        sessionId TEXT PRIMARY KEY NOT NULL,
                        walletId TEXT NOT NULL,
                        triggerPackage TEXT NOT NULL,
                        startedElapsedRealtime INTEGER NOT NULL,
                        lastHeartbeatElapsedRealtime INTEGER NOT NULL,
                        startedWallClock INTEGER NOT NULL,
                        initialWalletSeconds INTEGER NOT NULL,
                        consumedSeconds INTEGER NOT NULL,
                        maxAllowedSeconds INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_wallet_sessions_walletId ON wallet_sessions(walletId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_wallet_sessions_status ON wallet_sessions(status)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS plan_adjustments (
                        adjustmentId TEXT PRIMARY KEY NOT NULL,
                        goalId TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        reason TEXT NOT NULL,
                        recommendationType TEXT NOT NULL,
                        currentConfiguration TEXT NOT NULL,
                        suggestedConfiguration TEXT NOT NULL,
                        status TEXT NOT NULL,
                        appliedAt INTEGER NOT NULL,
                        rejectedAt INTEGER NOT NULL,
                        cooldownSeconds INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_plan_adjustments_goalId ON plan_adjustments(goalId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_plan_adjustments_status ON plan_adjustments(status)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS personalization_profiles (
                        profileId TEXT PRIMARY KEY NOT NULL,
                        preferredIntervention TEXT NOT NULL,
                        peakStartHour INTEGER NOT NULL,
                        peakEndHour INTEGER NOT NULL,
                        challengeCompletionRate REAL NOT NULL,
                        rapidReopenRate REAL NOT NULL,
                        averageSessionDurationSeconds INTEGER NOT NULL,
                        rewardEffectiveness TEXT NOT NULL,
                        consistencyScore REAL NOT NULL,
                        currentPlanHealth TEXT NOT NULL,
                        lastCalculatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS weekly_reviews (
                        reviewId TEXT PRIMARY KEY NOT NULL,
                        goalId TEXT NOT NULL,
                        weekStart INTEGER NOT NULL,
                        weekEnd INTEGER NOT NULL,
                        attempts INTEGER NOT NULL,
                        completed INTEGER NOT NULL,
                        earnedSeconds INTEGER NOT NULL,
                        consumedSeconds INTEGER NOT NULL,
                        habitInterruptionRate REAL NOT NULL,
                        rapidReopenRate REAL NOT NULL,
                        bestIntervention TEXT NOT NULL,
                        planHealth TEXT NOT NULL,
                        biggestWin TEXT NOT NULL,
                        suggestedNextStep TEXT NOT NULL,
                        generatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_weekly_reviews_goalId ON weekly_reviews(goalId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_weekly_reviews_weekStart ON weekly_reviews(weekStart)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS behaviour_experiments (
                        experimentId TEXT PRIMARY KEY NOT NULL,
                        goalId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        hypothesis TEXT NOT NULL,
                        baselineStartDate INTEGER NOT NULL,
                        baselineEndDate INTEGER NOT NULL,
                        experimentStartDate INTEGER NOT NULL,
                        experimentEndDate INTEGER NOT NULL,
                        interventionConfiguration TEXT NOT NULL,
                        status TEXT NOT NULL,
                        baselineMetrics TEXT NOT NULL,
                        experimentMetrics TEXT NOT NULL,
                        conclusion TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        completedAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_behaviour_experiments_goalId ON behaviour_experiments(goalId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_behaviour_experiments_status ON behaviour_experiments(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_behaviour_experiments_experimentStartDate ON behaviour_experiments(experimentStartDate)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS intervention_adaptive_aggregates (
                        aggregateKey TEXT PRIMARY KEY NOT NULL,
                        evidenceLevel TEXT NOT NULL,
                        interventionId TEXT NOT NULL,
                        targetPackage TEXT,
                        timeBucket TEXT,
                        startedCount INTEGER NOT NULL,
                        completedCount INTEGER NOT NULL,
                        helpedCount INTEGER NOT NULL,
                        didNotHelpCount INTEGER NOT NULL,
                        totalFeedbackCount INTEGER NOT NULL,
                        lastUpdatedTimestampMs INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_intervention_adaptive_aggregates_interventionId ON intervention_adaptive_aggregates(interventionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_intervention_adaptive_aggregates_targetPackage ON intervention_adaptive_aggregates(targetPackage)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_intervention_adaptive_aggregates_evidenceLevel ON intervention_adaptive_aggregates(evidenceLevel)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE schedules ADD COLUMN label TEXT NOT NULL DEFAULT 'Schedule'")
                db.execSQL("ALTER TABLE schedules ADD COLUMN restrictionMode TEXT NOT NULL DEFAULT 'BLOCK'")
                db.execSQL("ALTER TABLE schedules ADD COLUMN isEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE schedules ADD COLUMN daysOfWeekCsv TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS geofence_zones (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        zoneType TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        radiusMeters REAL NOT NULL,
                        restrictionMode TEXT NOT NULL,
                        isEnabled INTEGER NOT NULL,
                        createdAtMs INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): DigitalDisciplineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DigitalDisciplineDatabase::class.java,
                    "digital_discipline.db"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                    .addCallback(DatabaseCallback(context))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val database = getInstance(context)
                    populateDefaults(database)
                }
            }

            private suspend fun populateDefaults(db: DigitalDisciplineDatabase) {
                // Phase 2A MVP Default Policies (EARN Mode)
                // No hardcoded default app rules. Rules are populated on-demand by user onboarding / dashboard.
                // Initial Protection State
                db.protectionStateDao().updateProtectionState(
                    ProtectionStateEntity(
                        id = 1,
                        isAccessibilityActive = false,
                        isOverlayActive = false,
                        isUsageStatsActive = false,
                        isProtectionEnabledByParent = true,
                        lastHeartbeatElapsedRealtime = android.os.SystemClock.elapsedRealtime()
                    )
                )

                // Phase 4A Default Replacement Behaviours
                val defaultBehaviours = listOf(
                    ReplacementBehaviourEntity(
                        behaviourId = "beh_squats_10",
                        category = BehaviourCategory.PHYSICAL.name,
                        type = BehaviourType.SQUATS.name,
                        title = "10 Bodyweight Squats",
                        description = "Perform 10 full squats with good posture.",
                        targetCount = 10,
                        durationSeconds = 60,
                        unit = "reps"
                    ),
                    ReplacementBehaviourEntity(
                        behaviourId = "beh_pushups_10",
                        category = BehaviourCategory.PHYSICAL.name,
                        type = BehaviourType.PUSHUPS.name,
                        title = "10 Pushups",
                        description = "Complete 10 pushups to activate energy.",
                        targetCount = 10,
                        durationSeconds = 60,
                        unit = "reps"
                    ),
                    ReplacementBehaviourEntity(
                        behaviourId = "beh_breathing_30s",
                        category = BehaviourCategory.MINDFUL.name,
                        type = BehaviourType.BOX_BREATHING.name,
                        title = "30s Box Breathing",
                        description = "Inhale 4s, Hold 4s, Exhale 4s, Hold 4s.",
                        targetCount = 1,
                        durationSeconds = 30,
                        unit = "seconds"
                    ),
                    ReplacementBehaviourEntity(
                        behaviourId = "beh_pause_10s",
                        category = BehaviourCategory.MINDFUL.name,
                        type = BehaviourType.MINDFUL_PAUSE.name,
                        title = "10s Mindful Pause",
                        description = "Take 10 seconds to consider your intention.",
                        targetCount = 1,
                        durationSeconds = 10,
                        unit = "seconds"
                    ),
                    ReplacementBehaviourEntity(
                        behaviourId = "beh_study_timer_25m",
                        category = BehaviourCategory.STUDY.name,
                        type = BehaviourType.STUDY_TIMER.name,
                        title = "25m Focus Block",
                        description = "Complete a 25-minute Pomodoro study block.",
                        targetCount = 25,
                        durationSeconds = 1500,
                        unit = "minutes"
                    ),
                    ReplacementBehaviourEntity(
                        behaviourId = "beh_water_1glass",
                        category = BehaviourCategory.HEALTH.name,
                        type = BehaviourType.DRINK_WATER.name,
                        title = "Drink a Glass of Water",
                        description = "Hydrate before opening your phone.",
                        targetCount = 1,
                        durationSeconds = 15,
                        unit = "glass"
                    )
                )
                db.replacementBehaviourDao().insertAll(defaultBehaviours)

                // Phase 4B-2 Pre-seed default Self Mode Wallet
                db.earnedTimeWalletDao().insertOrUpdateWallet(
                    EarnedTimeWalletEntity(
                        walletId = "wallet_self",
                        ownerId = "self",
                        mode = "SELF",
                        availableSeconds = 0,
                        dailyEarnCapSeconds = 3600,
                        maxBalanceCapSeconds = 3600,
                        maxSessionSeconds = 1800
                    )
                )
            }
        }
    }
}

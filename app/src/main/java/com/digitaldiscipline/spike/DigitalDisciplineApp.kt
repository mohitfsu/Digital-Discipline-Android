package com.digitaldiscipline.spike

import android.app.Application
import androidx.work.Configuration
import com.digitaldiscipline.spike.analytics.LocalAnalyticsRepository
import com.digitaldiscipline.spike.cloud.CloudRepository
import com.digitaldiscipline.spike.cloud.FirebaseAuthManager
import com.digitaldiscipline.spike.cloud.PairingManager
import com.digitaldiscipline.spike.data.local.DigitalDisciplineDatabase
import com.digitaldiscipline.spike.data.preferences.PreferencesManager
import com.digitaldiscipline.spike.logging.EventLogger
import com.digitaldiscipline.spike.overlay.OverlayManager
import com.digitaldiscipline.spike.policy.PolicyEngine
import com.digitaldiscipline.spike.policy.PolicyRepository
import com.digitaldiscipline.spike.security.ParentPinManager
import com.digitaldiscipline.spike.sync.SyncManager
import com.digitaldiscipline.spike.tamper.TamperDetector
import com.google.firebase.FirebaseApp

class DigitalDisciplineApp : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    companion object {
        lateinit var instance: DigitalDisciplineApp
            private set
    }

    lateinit var database: DigitalDisciplineDatabase
        private set
    lateinit var preferencesManager: PreferencesManager
        private set
    lateinit var pinManager: ParentPinManager
        private set
    lateinit var policyRepository: PolicyRepository
        private set
    lateinit var analyticsRepository: LocalAnalyticsRepository
        private set
    lateinit var tamperDetector: TamperDetector
        private set
    lateinit var overlayManager: OverlayManager
        private set
    lateinit var behaviourRepository: com.digitaldiscipline.spike.behaviour.BehaviourRepository
        private set
    lateinit var behaviourPolicyResolver: com.digitaldiscipline.spike.behaviour.BehaviourPolicyResolver
        private set
    lateinit var walletService: com.digitaldiscipline.spike.wallet.EarnedTimeWalletService
        private set
    lateinit var personalizationRepository: com.digitaldiscipline.spike.behaviour.adaptive.PersonalizationRepository
        private set
    lateinit var experimentRepository: com.digitaldiscipline.spike.behaviour.intelligence.ExperimentRepository
        private set
    lateinit var interventionEngine: com.digitaldiscipline.spike.intervention.engine.InterventionEngine
        private set
    lateinit var policyEngine: PolicyEngine
        private set
    lateinit var workplaceGeofenceManager: com.digitaldiscipline.spike.geofence.WorkplaceGeofenceManager
        private set

    // Phase 4D-3 Smart Notifications
    lateinit var notificationHistoryRepository: com.digitaldiscipline.spike.notification.NotificationHistoryRepository
        private set
    lateinit var notificationFrequencyGovernor: com.digitaldiscipline.spike.notification.NotificationFrequencyGovernor
        private set

    // Phase 2B Cloud Control Plane
    lateinit var firebaseAuthManager: FirebaseAuthManager
        private set
    lateinit var cloudRepository: CloudRepository
        private set
    lateinit var pairingManager: PairingManager
        private set
    lateinit var syncManager: SyncManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            EventLogger.log("SYSTEM", packageName, "FIREBASE_APP_INIT_ERROR", details = e.message ?: "Unknown")
        }

        database = DigitalDisciplineDatabase.getInstance(this)
        com.digitaldiscipline.spike.logging.DiagnosticLogger.initialize(this)
        com.digitaldiscipline.spike.logging.DiagnosticLogger.logServiceStarted("DigitalDisciplineApplication")
        preferencesManager = PreferencesManager(this)
        pinManager = ParentPinManager(this)

        workplaceGeofenceManager = com.digitaldiscipline.spike.geofence.WorkplaceGeofenceManager(this)

        policyRepository = PolicyRepository(
            appRuleDao = database.appRuleDao(),
            scheduleDao = database.scheduleDao(),
            temporaryUnlockDao = database.temporaryUnlockDao(),
            geofenceZoneDao = database.geofenceZoneDao(),
            preferencesManager = preferencesManager
        )

        analyticsRepository = LocalAnalyticsRepository(
            dailyUsageDao = database.dailyUsageDao(),
            interventionEventDao = database.interventionEventDao()
        )

        tamperDetector = TamperDetector(
            context = this,
            protectionStateDao = database.protectionStateDao()
        )

        behaviourRepository = com.digitaldiscipline.spike.behaviour.BehaviourRepository(
            goalDao = database.goalDao(),
            triggerDao = database.triggerDao(),
            replacementBehaviourDao = database.replacementBehaviourDao(),
            behaviourPolicyDao = database.behaviourPolicyDao(),
            goalProgressDao = database.goalProgressDao()
        )

        behaviourPolicyResolver = com.digitaldiscipline.spike.behaviour.BehaviourPolicyResolver(
            policyRepository = policyRepository,
            behaviourRepository = behaviourRepository
        )

        walletService = com.digitaldiscipline.spike.wallet.EarnedTimeWalletService(
            walletDao = database.earnedTimeWalletDao(),
            transactionDao = database.walletTransactionDao(),
            sessionDao = database.walletSessionDao()
        )

        personalizationRepository = com.digitaldiscipline.spike.behaviour.adaptive.PersonalizationRepository(
            profileDao = database.personalizationProfileDao(),
            adjustmentDao = database.planAdjustmentDao(),
            weeklyReviewDao = database.weeklyReviewDao(),
            behaviourRepository = behaviourRepository,
            analyticsRepository = analyticsRepository,
            walletService = walletService
        )

        experimentRepository = com.digitaldiscipline.spike.behaviour.intelligence.ExperimentRepository(
            experimentDao = database.behaviourExperimentDao()
        )

        overlayManager = OverlayManager(this, pinManager)

        interventionEngine = com.digitaldiscipline.spike.intervention.engine.InterventionEngine(
            context = this,
            policyRepository = policyRepository,
            behaviourRepository = behaviourRepository,
            walletService = walletService,
            preferencesManager = preferencesManager
        )

        policyEngine = PolicyEngine(
            context = this,
            policyRepository = policyRepository,
            analyticsRepository = analyticsRepository,
            overlayManager = overlayManager,
            behaviourPolicyResolver = behaviourPolicyResolver,
            walletService = walletService,
            interventionEngine = interventionEngine,
            preferencesManager = preferencesManager
        )

        // Phase 2B Cloud Control Plane Initializations
        firebaseAuthManager = FirebaseAuthManager(this)
        cloudRepository = CloudRepository(this)
        pairingManager = PairingManager(this, preferencesManager, cloudRepository)
        syncManager = SyncManager(this)

        EventLogger.log(
            source = "SYSTEM",
            packageName = packageName,
            eventType = "APP_INITIALIZED",
            details = "Phase 2B Cloud Control Plane Ready (Firebase + WorkManager + Room Sync)"
        )

        policyEngine.start()
        syncManager.initializeSchedules()

        // Phase 4D-3 Smart Notifications
        com.digitaldiscipline.spike.notification.NotificationChannelManager.createChannels(this)
        notificationHistoryRepository = com.digitaldiscipline.spike.notification.NotificationHistoryRepository(this)
        notificationFrequencyGovernor = com.digitaldiscipline.spike.notification.NotificationFrequencyGovernor(this)
        com.digitaldiscipline.spike.notification.NotificationScheduler.initializeSchedules(this)
    }
}

package com.digitaldiscipline.spike.intervention

import com.digitaldiscipline.spike.data.local.dao.InterventionAdaptiveAggregateDao
import com.digitaldiscipline.spike.data.local.entities.InterventionAdaptiveAggregateEntity
import com.digitaldiscipline.spike.intervention.adaptive.*
import com.digitaldiscipline.spike.intervention.session.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class FakeInterventionAdaptiveAggregateDao : InterventionAdaptiveAggregateDao {
    val table = ConcurrentHashMap<String, InterventionAdaptiveAggregateEntity>()
    var shouldThrow = false

    override suspend fun upsert(aggregate: InterventionAdaptiveAggregateEntity) {
        if (shouldThrow) throw RuntimeException("Disk I/O Error")
        table[aggregate.aggregateKey] = aggregate
    }

    override suspend fun upsertAll(aggregates: List<InterventionAdaptiveAggregateEntity>) {
        if (shouldThrow) throw RuntimeException("Disk I/O Error")
        aggregates.forEach { table[it.aggregateKey] = it }
    }

    override suspend fun getAllAggregates(): List<InterventionAdaptiveAggregateEntity> {
        if (shouldThrow) throw RuntimeException("Disk I/O Error")
        return table.values.toList()
    }

    override suspend fun getByKey(key: String): InterventionAdaptiveAggregateEntity? {
        if (shouldThrow) throw RuntimeException("Disk I/O Error")
        return table[key]
    }

    override suspend fun deleteAll() {
        if (shouldThrow) throw RuntimeException("Disk I/O Error")
        table.clear()
    }

    override suspend fun deleteOlderThan(cutoffTimestampMs: Long): Int {
        if (shouldThrow) throw RuntimeException("Disk I/O Error")
        val toRemove = table.values.filter { it.lastUpdatedTimestampMs < cutoffTimestampMs }.map { it.aggregateKey }
        toRemove.forEach { table.remove(it) }
        return toRemove.size
    }
}

class PersistentAdaptiveStoreTest {

    private lateinit var fakeDao: FakeInterventionAdaptiveAggregateDao
    private lateinit var store: InterventionAdaptiveStore

    @Before
    fun setup() {
        fakeDao = FakeInterventionAdaptiveAggregateDao()
        store = InterventionAdaptiveStore(
            dao = fakeDao,
            scope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    // 1. Outcome persists to DAO asynchronously
    @Test
    fun testOutcomePersistsToDao() = runBlocking {
        val outcome = InterventionOutcome(
            sessionId = "sess_p1",
            interventionId = "BOX_BREATHING",
            targetPackage = "com.instagram.android",
            startedAtRealtimeMs = 1000L,
            completedAtRealtimeMs = 31000L,
            status = SessionState.COMPLETED,
            rewardSeconds = 300,
            helpfulness = HelpfulnessFeedback.HELPED
        )

        store.recordOutcome(outcome)

        val globalRecord = fakeDao.getByKey("BOX_BREATHING")
        assertNotNull(globalRecord)
        assertEquals(1, globalRecord?.completedCount)
        assertEquals(1, globalRecord?.helpedCount)

        val triggerRecord = fakeDao.getByKey("com.instagram.android:BOX_BREATHING")
        assertNotNull(triggerRecord)
        assertEquals(1, triggerRecord?.completedCount)
    }

    // 2. Persistent state reloads into fresh in-memory store
    @Test
    fun testReloadStateFromDatabase() = runBlocking {
        // Pre-populate DAO with aggregates
        fakeDao.upsert(
            InterventionAdaptiveAggregateEntity(
                aggregateKey = "BOX_BREATHING",
                evidenceLevel = "GLOBAL",
                interventionId = "BOX_BREATHING",
                startedCount = 10,
                completedCount = 10,
                helpedCount = 9,
                didNotHelpCount = 1,
                totalFeedbackCount = 10
            )
        )
        fakeDao.upsert(
            InterventionAdaptiveAggregateEntity(
                aggregateKey = "com.instagram.android:BOX_BREATHING",
                evidenceLevel = "TRIGGER",
                interventionId = "BOX_BREATHING",
                targetPackage = "com.instagram.android",
                startedCount = 5,
                completedCount = 5,
                helpedCount = 5,
                totalFeedbackCount = 5
            )
        )

        val freshStore = InterventionAdaptiveStore(dao = fakeDao, scope = CoroutineScope(Dispatchers.Unconfined))
        freshStore.loadFromDatabase()

        val globalStats = freshStore.getStats("BOX_BREATHING")
        assertEquals(10, globalStats.completedCount)
        assertEquals(0.9f, globalStats.helpfulnessRate, 0.01f)

        val trigStats = freshStore.getStatsForTrigger("BOX_BREATHING", "com.instagram.android")
        assertEquals(5, trigStats.completedCount)
        assertEquals(1.0f, trigStats.helpfulnessRate, 0.01f)
    }

    // 3. Database write failure is isolated and does not disrupt in-memory selection
    @Test
    fun testDatabaseFailureIsIsolated() = runBlocking {
        fakeDao.shouldThrow = true

        val outcome = InterventionOutcome(
            sessionId = "sess_fail_io",
            interventionId = "SQUATS",
            targetPackage = "com.instagram.android",
            startedAtRealtimeMs = 1000L,
            completedAtRealtimeMs = 20000L,
            status = SessionState.COMPLETED,
            rewardSeconds = 300,
            helpfulness = HelpfulnessFeedback.HELPED
        )

        // Must not crash
        store.recordOutcome(outcome)

        // In-memory stats remain 100% functional
        val memoryStats = store.getStats("SQUATS")
        assertEquals(1, memoryStats.completedCount)
        assertEquals(1.0f, memoryStats.helpfulnessRate, 0.01f)
    }

    // 4. Cold-start selection functions immediately before DB load
    @Test
    fun testColdStartFunctionsBeforeDbLoad() {
        val unloadedStore = InterventionAdaptiveStore(dao = null)
        val selector = InterventionSelector(unloadedStore)

        val context = InterventionContext(
            triggerId = "trig_cold",
            targetPackage = "com.instagram.android",
            configuredInterventionId = "BOX_BREATHING"
        )
        val selection = selector.select(context)
        assertEquals("BOX_BREATHING", selection.selectedIntervention.id)
    }
}

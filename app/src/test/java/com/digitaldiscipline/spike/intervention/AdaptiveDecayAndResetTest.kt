package com.digitaldiscipline.spike.intervention

import com.digitaldiscipline.spike.data.local.entities.InterventionAdaptiveAggregateEntity
import com.digitaldiscipline.spike.intervention.adaptive.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AdaptiveDecayAndResetTest {

    private lateinit var fakeDao: FakeInterventionAdaptiveAggregateDao
    private lateinit var store: InterventionAdaptiveStore

    @Before
    fun setup() {
        fakeDao = FakeInterventionAdaptiveAggregateDao()
        store = InterventionAdaptiveStore(dao = fakeDao, scope = CoroutineScope(Dispatchers.Unconfined))
    }

    // 1. Fresh evidence retains full weight (0 days decay)
    @Test
    fun testFreshEvidenceRetainsFullWeight() {
        val stats = InterventionStats(
            key = "BOX_BREATHING",
            startedCount = 10,
            completedCount = 10,
            helpedCount = 10,
            totalFeedbackCount = 10,
            lastUsedTimestampMs = System.currentTimeMillis()
        )
        assertEquals(10.0f, stats.calculateDecayed(10), 0.05f)
        assertEquals(1.0f, stats.confidence, 0.01f)
    }

    // 2. 30-day evidence approximately halves (50% decay)
    @Test
    fun testThirtyDaysEvidenceHalves() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000L)
        val stats = InterventionStats(
            key = "BOX_BREATHING",
            startedCount = 10,
            completedCount = 10,
            helpedCount = 10,
            totalFeedbackCount = 10,
            lastUsedTimestampMs = thirtyDaysAgo
        )
        val decayed = stats.calculateDecayed(10)
        assertEquals(5.0f, decayed, 0.25f) // ~5.0 observations
        assertEquals(0.75f, stats.confidence, 0.05f) // (5 + 2.5) / 10 = 0.75
    }

    // 3. 60-day evidence approximately quarters (25% weight)
    @Test
    fun testSixtyDaysEvidenceQuarters() {
        val sixtyDaysAgo = System.currentTimeMillis() - (60L * 24 * 60 * 60 * 1000L)
        val stats = InterventionStats(
            key = "BOX_BREATHING",
            startedCount = 10,
            completedCount = 10,
            helpedCount = 10,
            totalFeedbackCount = 10,
            lastUsedTimestampMs = sixtyDaysAgo
        )
        val decayed = stats.calculateDecayed(10)
        assertEquals(2.5f, decayed, 0.25f) // ~2.5 observations
    }

    // 4. Reset clears both in-memory cache and persistent database
    @Test
    fun testResetAdaptiveMemoryClearsMemoryAndDao() = runBlocking {
        fakeDao.upsert(
            InterventionAdaptiveAggregateEntity(
                aggregateKey = "BOX_BREATHING",
                evidenceLevel = "GLOBAL",
                interventionId = "BOX_BREATHING",
                startedCount = 10,
                completedCount = 10,
                helpedCount = 10,
                totalFeedbackCount = 10
            )
        )
        store.loadFromDatabase()

        assertEquals(10, store.getStats("BOX_BREATHING").completedCount)

        store.resetAdaptiveMemory()

        assertEquals(0, store.getStats("BOX_BREATHING").completedCount)
        assertEquals(0, fakeDao.table.size)
    }

    // 5. Stale records older than 90 days are purged
    @Test
    fun testPurgeStaleAggregatesOlderThan90Days() = runBlocking {
        val ninetyOneDaysAgo = System.currentTimeMillis() - (91L * 24 * 60 * 60 * 1000L)
        val tenDaysAgo = System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000L)

        fakeDao.upsert(
            InterventionAdaptiveAggregateEntity(
                aggregateKey = "OLD_STALE",
                evidenceLevel = "GLOBAL",
                interventionId = "OLD_STALE",
                lastUpdatedTimestampMs = ninetyOneDaysAgo
            )
        )
        fakeDao.upsert(
            InterventionAdaptiveAggregateEntity(
                aggregateKey = "RECENT_ACTIVE",
                evidenceLevel = "GLOBAL",
                interventionId = "RECENT_ACTIVE",
                lastUpdatedTimestampMs = tenDaysAgo
            )
        )

        val deletedCount = store.purgeStaleAggregates()

        assertEquals(1, deletedCount)
        assertNull(fakeDao.getByKey("OLD_STALE"))
        assertNotNull(fakeDao.getByKey("RECENT_ACTIVE"))
    }
}

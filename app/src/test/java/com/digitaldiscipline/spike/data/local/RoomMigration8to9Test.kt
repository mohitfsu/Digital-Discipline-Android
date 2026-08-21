package com.digitaldiscipline.spike.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class RoomMigration8to9Test {

    @Test
    fun testMigration8to9ExecutesCorrectSql() {
        val executedSqlList = mutableListOf<String>()
        val handler = InvocationHandler { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSqlList.add(args[0] as String)
            }
            null
        }

        val fakeDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
            handler
        ) as SupportSQLiteDatabase

        DigitalDisciplineDatabase.MIGRATION_8_9.migrate(fakeDb)

        val joinedSql = executedSqlList.joinToString("\n")

        // Verify table creation SQL
        assertTrue(joinedSql.contains("CREATE TABLE IF NOT EXISTS intervention_adaptive_aggregates"))

        // Verify index creation SQL
        assertTrue(joinedSql.contains("index_intervention_adaptive_aggregates_interventionId"))
        assertTrue(joinedSql.contains("index_intervention_adaptive_aggregates_targetPackage"))
        assertTrue(joinedSql.contains("index_intervention_adaptive_aggregates_evidenceLevel"))
    }
}

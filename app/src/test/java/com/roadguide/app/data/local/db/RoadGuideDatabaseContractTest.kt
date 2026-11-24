package com.roadguide.app.data.local.db

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.json.JSONObject
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoadGuideDatabaseContractTest {

    private val context = RuntimeEnvironment.application
    private val openDatabases = mutableListOf<RoadGuideDatabase>()

    @After
    fun tearDown() {
        openDatabases.forEach(RoadGuideDatabase::close)
    }

    @Test
    fun `migrates each exported schema and preserves point data`() {
        for (oldVersion in 1..3) {
            val name = "migration_from_v$oldVersion.db"
            context.deleteDatabase(name)
            createLegacyDatabase(name, oldVersion)

            val database = Room.databaseBuilder(
                context,
                RoadGuideDatabase::class.java,
                name
            ).addMigrations(
                RoadGuideDatabase.MIGRATION_1_2,
                RoadGuideDatabase.MIGRATION_2_3,
                RoadGuideDatabase.MIGRATION_3_4
            ).build()
            openDatabases += database

            val sqlite = database.openHelper.writableDatabase
            assertEquals("Upgrading v$oldVersion", 4, sqlite.version)
            assertEquals(2L, scalarLong(sqlite, "SELECT COUNT(*) FROM location_points"))
            assertEquals(
                10L,
                scalarLong(sqlite, "SELECT routeId FROM location_points WHERE id = 40")
            )
            assertEquals(
                0L,
                scalarLong(sqlite, "SELECT excludeFromExposure FROM location_points WHERE id = 40")
            )
            assertEquals(
                1L,
                scalarLong(sqlite, "SELECT COUNT(*) FROM location_points WHERE id = 41 AND routeId IS NULL")
            )
            assertEquals(
                if (oldVersion == 1) 0.0 else 12.5,
                scalarDouble(sqlite, "SELECT rawExposureScore FROM exposure_results WHERE id = 30"),
                0.0
            )
            assertFalse(sqlite.query("PRAGMA foreign_key_check").use { it.moveToFirst() })
        }
    }

    @Test
    fun `route deletion cascades to points segments and stays`() {
        val database = newInMemoryDatabase()
        val sqlite = database.openHelper.writableDatabase
        val executor = executor(sqlite)
        insertRoute(executor, id = 1)
        insertPoint(executor, id = 1, routeId = 1, version = 4)
        insertSegment(executor, id = 1, routeId = 1)
        insertStay(executor, id = 1, routeId = 1)

        sqlite.execSQL("DELETE FROM routes WHERE id = 1")

        assertEquals(0L, scalarLong(sqlite, "SELECT COUNT(*) FROM location_points"))
        assertEquals(0L, scalarLong(sqlite, "SELECT COUNT(*) FROM route_segments"))
        assertEquals(0L, scalarLong(sqlite, "SELECT COUNT(*) FROM stays"))
    }

    @Test(expected = SQLiteConstraintException::class)
    fun `point cannot reference a missing route`() {
        val database = newInMemoryDatabase()
        database.openHelper.writableDatabase.execSQL(
            "INSERT INTO location_points " +
                "(latitude, longitude, timestamp, accuracyMeters, provider, routeId) " +
                "VALUES (1.0, 2.0, 3, 4.0, 'test', 999)"
        )
    }

    @Test
    fun `deleting a place clears stay reference and deleting segment removes exposure`() {
        val database = newInMemoryDatabase()
        val sqlite = database.openHelper.writableDatabase
        val executor = executor(sqlite)
        insertRoute(executor, id = 1)
        insertPlace(executor, id = 1)
        insertSegment(executor, id = 1, routeId = 1)
        insertStay(executor, id = 1, routeId = 1, placeId = 1)
        insertExposureResult(executor, id = 1, segmentId = 1, routeId = 1, stayId = 1)

        sqlite.execSQL("DELETE FROM places WHERE id = 1")
        assertEquals(1L, scalarLong(sqlite, "SELECT COUNT(*) FROM stays WHERE placeId IS NULL"))

        sqlite.execSQL("DELETE FROM route_segments WHERE id = 1")
        assertEquals(0L, scalarLong(sqlite, "SELECT COUNT(*) FROM exposure_results"))
    }

    private fun newInMemoryDatabase(): RoadGuideDatabase =
        Room.inMemoryDatabaseBuilder(context, RoadGuideDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            .also(openDatabases::add)

    private fun createLegacyDatabase(name: String, version: Int) {
        val schemaPath =
            "com.roadguide.app.data.local.db.RoadGuideDatabase/$version.json"
        val schemaFile = File("schemas/$schemaPath")
        assertTrue("Missing exported schema file: ${schemaFile.absolutePath}", schemaFile.isFile)
        val schema = JSONObject(schemaFile.readText()).getJSONObject("database")
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        val sqlite = SQLiteDatabase.openOrCreateDatabase(file.absolutePath, null)
        try {
            sqlite.beginTransaction()
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val tableName = entity.getString("tableName")
                sqlite.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", tableName))
                val indices = entity.getJSONArray("indices")
                for (indexInEntity in 0 until indices.length()) {
                    val sql = indices.getJSONObject(indexInEntity).getString("createSql")
                    sqlite.execSQL(sql.replace("\${TABLE_NAME}", tableName))
                }
            }
            val views = schema.getJSONArray("views")
            for (index in 0 until views.length()) {
                sqlite.execSQL(views.getJSONObject(index).getString("createSql"))
            }
            val setupQueries = schema.getJSONArray("setupQueries")
            for (index in 0 until setupQueries.length()) {
                sqlite.execSQL(setupQueries.getString(index))
            }

            val executor = executor(sqlite)
            insertRoute(executor, id = 10)
            insertSegment(executor, id = 20, routeId = 10)
            insertExposureResult(executor, id = 30, segmentId = 20, routeId = 10, version = version)
            insertPoint(executor, id = 40, routeId = 10, version = version)
            insertPoint(executor, id = 41, routeId = 999, version = version)
            sqlite.setTransactionSuccessful()
        } finally {
            sqlite.endTransaction()
            sqlite.version = version
            sqlite.close()
        }
    }

    private fun executor(database: SupportSQLiteDatabase) =
        SqlExecutor { sql, args -> database.execSQL(sql, args) }

    private fun executor(database: SQLiteDatabase) =
        SqlExecutor { sql, args -> database.execSQL(sql, args) }

    private fun insertRoute(database: SqlExecutor, id: Long) {
        database.execSQL(
            """
            INSERT INTO routes
                (id, startTime, endTime, distanceMeters, status, routeType, confidence,
                 date, totalDurationSeconds, calculationVersion)
            VALUES (?, 1000, 2000, 12.0, 'COMPLETED', 'DAILY_AUTO', 0.9,
                    '2026-01-01', 1000, 1)
            """.trimIndent(),
            arrayOf(id)
        )
    }

    private fun insertSegment(database: SqlExecutor, id: Long, routeId: Long) {
        database.execSQL(
            """
            INSERT INTO route_segments
                (id, routeId, startTime, endTime, activityType, distanceMeters,
                 startLatitude, startLongitude, endLatitude, endLongitude, confidence,
                 isUserCorrected)
            VALUES (?, ?, 1000, 2000, 'WALKING', 12.0, 1.0, 2.0, 3.0, 4.0, 0.9, 0)
            """.trimIndent(),
            arrayOf(id, routeId)
        )
    }

    private fun insertExposureResult(
        database: SqlExecutor,
        id: Long,
        segmentId: Long,
        routeId: Long,
        stayId: Long? = null,
        version: Int = 4
    ) {
        val rawScoreColumn = if (version >= 2) ", rawExposureScore" else ""
        val rawScoreValue = if (version >= 2) ", 12.5" else ""
        database.execSQL(
            """
            INSERT INTO exposure_results
                (id, segmentId, stayId, routeId, exposureScore$rawScoreColumn, pollution,
                 durationMinutes, activityLevel, confidence, coverage, createdTimestamp, updatedTimestamp)
            VALUES (?, ?, ?, ?, 40.0$rawScoreValue, 1.0, 30, 0.8, 0.9, 90.0, 1000, 1000)
            """.trimIndent(),
            arrayOf(id, segmentId, stayId, routeId)
        )
    }

    private fun insertPoint(
        database: SqlExecutor,
        id: Long,
        routeId: Long,
        version: Int
    ) {
        val exposureColumn = if (version >= 3) ", excludeFromExposure" else ""
        val exposureValue = if (version >= 3) ", 0" else ""
        database.execSQL(
            """
            INSERT INTO location_points
                (id, latitude, longitude, timestamp, accuracyMeters, provider,
                 isMock, qualityFlags, routeId, collectionDiagnostics$exposureColumn)
            VALUES (?, 55.0, 37.0, 1000, 5.0, 'test', 0, 0, ?, NULL$exposureValue)
            """.trimIndent(),
            arrayOf(id, routeId)
        )
    }

    private fun insertPlace(database: SqlExecutor, id: Long) {
        database.execSQL(
            """
            INSERT INTO places
                (id, name, category, latitude, longitude, radiusMeters, isPrivate,
                 visitCount, totalDurationSeconds, firstVisitTime, lastVisitTime)
            VALUES (?, 'Test', 'OTHER', 1.0, 2.0, 100.0, 0, 0, 0, 0, 0)
            """.trimIndent(),
            arrayOf(id)
        )
    }

    private fun insertStay(
        database: SqlExecutor,
        id: Long,
        routeId: Long,
        placeId: Long? = null
    ) {
        database.execSQL(
            """
            INSERT INTO stays
                (id, routeId, startTime, endTime, centerLatitude, centerLongitude,
                 radiusMeters, placeId, confidence, privacyStatus, isIndoor, indoorConfidence)
            VALUES (?, ?, 1000, 2000, 1.0, 2.0, 50.0, ?, 0.9, 'PUBLIC', 0, 0.0)
            """.trimIndent(),
            arrayOf(id, routeId, placeId)
        )
    }

    private fun scalarLong(database: SupportSQLiteDatabase, sql: String): Long =
        database.query(sql).use { cursor ->
            assertTrue("No result for query: $sql", cursor.moveToFirst())
            cursor.getLong(0)
        }

    private fun scalarDouble(database: SupportSQLiteDatabase, sql: String): Double =
        database.query(sql).use { cursor ->
            assertTrue("No result for query: $sql", cursor.moveToFirst())
            cursor.getDouble(0)
        }

    private fun interface SqlExecutor {
        fun execSQL(sql: String, bindArgs: Array<out Any?>)
    }
}

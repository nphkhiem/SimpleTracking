package com.khiemnph.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TEST_DB = "migration-test.db"

/**
 * Proves the committed schema is real and that the migration harness works, so the first actual
 * migration has a gate to fail against rather than needing this scaffolding built under pressure at
 * the moment it is already broken.
 *
 * When the database moves to version 2, add a test that creates it at 1, writes a representative
 * row, runs the new Migration via `runMigrationsAndValidate`, and asserts the row survived. Room
 * validates the resulting schema against the committed JSON automatically.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun givenExportedSchemaForVersion1_whenDatabaseCreatedAtThatVersion_thenItOpensAndMatches() {
        // Fails if the exported JSON is missing, unreadable, or does not describe what Room builds.
        helper.createDatabase(TEST_DB, 1).use { db ->
            assertEquals(1, db.version)
        }
    }

    /**
     * The point of a migration test: a user's recorded history must survive the upgrade. Room also
     * validates the resulting schema against the committed `2.json`, so a migration that produces
     * the wrong shape fails here rather than on someone's phone.
     */
    @Test
    fun givenASessionRecordedAtVersion1_whenMigratedTo2_thenItsStatisticsSurvive() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO session
                    (id, startTimestamp, pausedDurationMillis, status, pausedAtTimestamp,
                     stoppedTimestamp, finalDistanceMeters, finalAverageSpeedMps, thumbnailPath)
                VALUES ('s1', 1000, 0, 'STOPPED', NULL, 61000, 1234.5, 2.5, '/files/thumbnails/s1.png')
                """.trimIndent(),
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        migrated.query("SELECT id, finalDistanceMeters, stoppedTimestamp, routePolyline FROM session").use {
            assertTrue("The recorded session was lost by the migration", it.moveToFirst())
            assertEquals("s1", it.getString(0))
            assertEquals(1234.5, it.getDouble(1), 0.0001)
            assertEquals(61000L, it.getLong(2))
            // Pre-existing sessions have no stored geometry: their route was only ever a PNG, which
            // cannot be turned back into coordinates. They render the empty-route placeholder.
            assertTrue(it.isNull(3))
        }
    }

    @Test
    fun givenAVersion1Database_whenMigratedTo2_thenTheThumbnailColumnIsGone() {
        helper.createDatabase(TEST_DB, 1).close()

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        migrated.query("SELECT * FROM session LIMIT 0").use { cursor ->
            assertEquals(-1, cursor.getColumnIndex("thumbnailPath"))
            assertTrue(cursor.getColumnIndex("routePolyline") >= 0)
        }
    }

    @Test
    fun givenASessionRecordedAtVersion2_whenMigratedTo3_thenItSurvivesWithNoMonotonicTiming() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO session
                    (id, startTimestamp, pausedDurationMillis, status, pausedAtTimestamp,
                     stoppedTimestamp, finalDistanceMeters, finalAverageSpeedMps, routePolyline)
                VALUES ('s2', 1000, 0, 'STOPPED', NULL, 61000, 4321.0, 3.5, '2103850,10585420')
                """.trimIndent(),
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        migrated.query(
            "SELECT finalDistanceMeters, routePolyline, startElapsedRealtimeMillis FROM session",
        ).use {
            assertTrue("The recorded session was lost by the migration", it.moveToFirst())
            assertEquals(4321.0, it.getDouble(0), 0.0001)
            assertEquals("2103850,10585420", it.getString(1))
            // elapsedRealtime is measured from boot, so there is no value that could be invented
            // for a session recorded before the column existed. It keeps wall-clock timing.
            assertTrue(it.isNull(2))
        }
    }

    @Test
    fun givenAVersion1Database_whenMigratedAllTheWayTo3_thenBothMigrationsApplyInOrder() {
        helper.createDatabase(TEST_DB, 1).close()

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_1_2, MIGRATION_2_3)

        migrated.query("SELECT * FROM session LIMIT 0").use { cursor ->
            assertEquals(-1, cursor.getColumnIndex("thumbnailPath"))
            assertTrue(cursor.getColumnIndex("routePolyline") >= 0)
            assertTrue(cursor.getColumnIndex("startElapsedRealtimeMillis") >= 0)
            assertTrue(cursor.getColumnIndex("pausedAtElapsedRealtimeMillis") >= 0)
        }
    }

    @Test
    fun givenRecordedPointsAtVersion3_whenMigratedTo4_thenTheySurviveTheIndexChange() {
        helper.createDatabase(TEST_DB, 3).use { db ->
            db.execSQL(
                """
                INSERT INTO session
                    (id, startTimestamp, startElapsedRealtimeMillis, pausedDurationMillis, status,
                     pausedAtTimestamp, pausedAtElapsedRealtimeMillis, stoppedTimestamp,
                     finalDistanceMeters, finalAverageSpeedMps, routePolyline)
                VALUES ('s3', 1000, 500, 0, 'STOPPED', NULL, NULL, 61000, 100.0, 1.5, NULL)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO location_point
                    (sessionId, latitude, longitude, timestamp, horizontalAccuracyMeters, speedMetersPerSec)
                VALUES ('s3', 21.0285, 105.8542, 2000, 5.0, 2.0)
                """.trimIndent(),
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        migrated.query("SELECT count(*) FROM location_point").use {
            assertTrue(it.moveToFirst())
            assertEquals(1, it.getInt(0))
        }
    }

    /**
     * The index existing is not the point; SQLite choosing it is. A `USE TEMP B-TREE FOR ORDER BY`
     * here means the per-fix lookup is sorting the whole session again, which is the regression
     * this migration exists to prevent.
     */
    @Test
    fun givenTheMigratedSchema_whenPlanningThePerFixLookup_thenTheIndexServesTheOrderingToo() {
        helper.createDatabase(TEST_DB, 3).close()
        val migrated = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        val plan = StringBuilder()
        migrated.query(
            "EXPLAIN QUERY PLAN SELECT * FROM location_point WHERE sessionId = 's3' " +
                "ORDER BY timestamp DESC LIMIT 1",
        ).use { cursor ->
            while (cursor.moveToNext()) plan.append(cursor.getString(cursor.columnCount - 1)).append(' ')
        }

        assertTrue("Query plan was: $plan", plan.contains("index_location_point_sessionId_timestamp"))
        assertFalse("Query plan still sorts: $plan", plan.contains("TEMP B-TREE"))
    }

    /**
     * A nullable ADD COLUMN, which SQLite has always supported and which cannot lose data. The
     * point of the test is the "cannot lose data" half: an existing run must survive with every
     * field intact and simply gain a null title.
     */
    @Test
    fun givenSessionsRecordedBeforeTitles_whenMigratingTo5_thenTheySurviveWithNoTitle() {
        helper.createDatabase(TEST_DB, 4).use { db ->
            db.execSQL(
                """
                INSERT INTO session
                    (id, startTimestamp, startElapsedRealtimeMillis, pausedDurationMillis, status,
                     pausedAtTimestamp, pausedAtElapsedRealtimeMillis, stoppedTimestamp,
                     finalDistanceMeters, finalAverageSpeedMps, routePolyline)
                VALUES ('s1', 1000, 500, 0, 'STOPPED', NULL, NULL, 61000, 1200.0, 20.0, 'poly')
                """.trimIndent(),
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)

        migrated.query("SELECT id, finalDistanceMeters, routePolyline, title FROM session").use {
            assertTrue(it.moveToFirst())
            assertEquals("s1", it.getString(0))
            assertEquals(1200.0, it.getDouble(1), 0.001)
            assertEquals("poly", it.getString(2))
            assertTrue("an existing run must have no title, not an empty one", it.isNull(3))
        }
    }

    @Test
    fun givenTheMigratedSchema_whenATitleIsWritten_thenItReadsBack() {
        helper.createDatabase(TEST_DB, 4).close()
        val migrated = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)

        migrated.execSQL(
            """
            INSERT INTO session
                (id, startTimestamp, startElapsedRealtimeMillis, pausedDurationMillis, status,
                 pausedAtTimestamp, pausedAtElapsedRealtimeMillis, stoppedTimestamp,
                 finalDistanceMeters, finalAverageSpeedMps, routePolyline, title)
            VALUES ('s2', 1000, 500, 0, 'STOPPED', NULL, NULL, 61000, 1200.0, 20.0, NULL, 'Morning loop')
            """.trimIndent(),
        )

        migrated.query("SELECT title FROM session WHERE id = 's2'").use {
            assertTrue(it.moveToFirst())
            assertEquals("Morning loop", it.getString(0))
        }
    }

    @Test
    fun givenFixesRecordedByWallClock_whenMigratingTo6_thenTheirIntervalsAreUnchanged() {
        // The backfill is the whole point. Left at the column default of zero, every point in every
        // historic run would share one instant, and their distance and splits would collapse.
        helper.createDatabase(TEST_DB, 5).use { db ->
            db.execSQL(
                """
                INSERT INTO session
                    (id, startTimestamp, startElapsedRealtimeMillis, pausedDurationMillis, status,
                     pausedAtTimestamp, pausedAtElapsedRealtimeMillis, stoppedTimestamp,
                     finalDistanceMeters, finalAverageSpeedMps, routePolyline, title)
                VALUES ('s1', 1000, 500, 0, 'STOPPED', NULL, NULL, 61000, 1200.0, 20.0, 'poly', NULL)
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO location_point
                    (sessionId, latitude, longitude, timestamp, horizontalAccuracyMeters, speedMetersPerSec)
                VALUES ('s1', 21.0, 105.8, 10000, 5.0, 3.0), ('s1', 21.001, 105.8, 15000, 5.0, 3.0)
                """.trimIndent(),
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        migrated.query(
            "SELECT timestamp, elapsedRealtimeMillis FROM location_point WHERE sessionId = 's1' ORDER BY timestamp",
        ).use {
            assertTrue(it.moveToFirst())
            assertEquals(10000L, it.getLong(0))
            assertEquals("backfilled from the wall clock, not left at zero", 10000L, it.getLong(1))
            assertTrue(it.moveToNext())
            assertEquals(15000L, it.getLong(1))
        }
    }

    @Test
    fun givenTheMigratedSchema_whenAMonotonicFixIsWritten_thenItReadsBackIndependentlyOfTheWallClock() {
        helper.createDatabase(TEST_DB, 5).close()
        val migrated = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        migrated.execSQL(
            """
            INSERT INTO session
                (id, startTimestamp, startElapsedRealtimeMillis, pausedDurationMillis, status,
                 pausedAtTimestamp, pausedAtElapsedRealtimeMillis, stoppedTimestamp,
                 finalDistanceMeters, finalAverageSpeedMps, routePolyline, title)
            VALUES ('s2', 1000, 500, 0, 'RUNNING', NULL, NULL, NULL, 0.0, 0.0, NULL, NULL)
            """.trimIndent(),
        )
        migrated.execSQL(
            """
            INSERT INTO location_point
                (sessionId, latitude, longitude, timestamp, horizontalAccuracyMeters,
                 speedMetersPerSec, elapsedRealtimeMillis)
            VALUES ('s2', 21.0, 105.8, 1764600000000, 5.0, 3.0, 42000)
            """.trimIndent(),
        )

        migrated.query("SELECT timestamp, elapsedRealtimeMillis FROM location_point WHERE sessionId = 's2'").use {
            assertTrue(it.moveToFirst())
            assertEquals(1764600000000L, it.getLong(0))
            assertEquals(42000L, it.getLong(1))
        }
    }
}

package com.khiemnph.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
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
}

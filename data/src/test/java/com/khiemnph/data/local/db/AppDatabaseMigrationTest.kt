package com.khiemnph.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
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
}

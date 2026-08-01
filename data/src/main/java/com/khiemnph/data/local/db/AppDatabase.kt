package com.khiemnph.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Schema export is on and the JSON under `data/schemas` is committed. An unexported schema cannot be recovered
 * later: the JSON for version `n` has to already exist to write and test a `Migration` from `n` to
 * `n + 1`, so this has to be in place before the first version bump rather than when one is needed.
 *
 * Migration policy. `DatabaseModule` registers no migrations and configures no destructive
 * fallback, so the moment the version below is bumped without a matching `Migration`, Room throws
 * `IllegalStateException: A migration from 1 to 2 was required but not found` on the first database
 * access. For this app that is `MainActivity.onStart`, which makes it a launch-crash loop for every
 * installed user rather than a recoverable error. Therefore:
 *
 * - Pre-release, a breaking change may bump the version and add `fallbackToDestructiveMigration`.
 * - Post-release, every bump needs a real `Migration` registered in `DatabaseModule`, plus a test
 *   covering it, with the previous version's committed JSON as the baseline.
 */
@Database(
    entities = [SessionEntity::class, LocationPointEntity::class],
    version = 6,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao

    abstract fun locationPointDao(): LocationPointDao
}

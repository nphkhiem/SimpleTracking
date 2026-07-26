package com.khiemnph.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 1 to 2: route thumbnails move from a rendered PNG on disk to the route's own geometry.
 *
 * `thumbnailPath` goes, `routePolyline` arrives. Sessions recorded before this cannot be backfilled:
 * deriving a polyline means reading every row of `location_point` for a session and encoding it,
 * which is Kotlin work rather than a statement. Those sessions keep their row and every statistic
 * and simply render the empty-route placeholder. That one-time cost falls entirely on sessions
 * recorded before release.
 *
 * The column is removed by recreating the table rather than with `ALTER TABLE ... DROP COLUMN`,
 * which SQLite only supports from 3.35 (2021). `minSdk` is 29, whose bundled SQLite predates that,
 * so the concise form throws `near "DROP": syntax error` on exactly the older devices least likely
 * to be tested on. Recreating works everywhere.
 *
 * Room disables foreign-key enforcement for the duration of a migration, so dropping `session` here
 * does not cascade into `location_point`. The rows are re-inserted before enforcement resumes, and
 * the child table's reference resolves again once the new table takes the name.
 *
 * The PNG files under `filesDir/thumbnails` are not deleted here: a migration must not depend on the
 * filesystem, and a file that cannot be deleted must never fail an upgrade.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE session_new (
                id TEXT NOT NULL PRIMARY KEY,
                startTimestamp INTEGER NOT NULL,
                pausedDurationMillis INTEGER NOT NULL,
                status TEXT NOT NULL,
                pausedAtTimestamp INTEGER,
                stoppedTimestamp INTEGER,
                finalDistanceMeters REAL,
                finalAverageSpeedMps REAL,
                routePolyline TEXT
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO session_new (
                id, startTimestamp, pausedDurationMillis, status, pausedAtTimestamp,
                stoppedTimestamp, finalDistanceMeters, finalAverageSpeedMps, routePolyline
            )
            SELECT
                id, startTimestamp, pausedDurationMillis, status, pausedAtTimestamp,
                stoppedTimestamp, finalDistanceMeters, finalAverageSpeedMps, NULL
            FROM session
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE session")
        db.execSQL("ALTER TABLE session_new RENAME TO session")
    }
}

/**
 * 2 to 3: the session clock becomes monotonic.
 *
 * Durations were derived entirely from `System.currentTimeMillis`, which is the wrong clock for
 * measuring elapsed time: it moves when an NTP correction lands or a user edits the date, and it
 * can move backwards, which rendered the running timer as text like `-0:-15`. Two nullable
 * `elapsedRealtime` columns are added so a session can be timed with a clock that only moves
 * forward.
 *
 * Both are nullable and left null here rather than backfilled. `elapsedRealtime` is measured from
 * boot, so there is no value that could be invented for a session recorded before this column
 * existed. Those sessions keep using wall-clock timing, which is exactly what they were recorded
 * with, and new sessions get the monotonic treatment.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE session ADD COLUMN startElapsedRealtimeMillis INTEGER")
        db.execSQL("ALTER TABLE session ADD COLUMN pausedAtElapsedRealtimeMillis INTEGER")
    }
}

/**
 * 3 to 4: the per-fix lookup gets an index that can actually serve it.
 *
 * `getMostRecentPoint` filters on `sessionId` and orders by `timestamp`, and runs on every accepted
 * GPS fix. The old `sessionId`-only index served the filter but not the ordering, so SQLite
 * materialised and sorted the whole session each time: on a two-hour ride that is roughly 3,600
 * sorts growing to 3,600 rows each, on the Room executor, on battery, with the screen off.
 *
 * Room identifies indices by name, so the old one is dropped rather than left behind. Indices carry
 * no data, so nothing here can lose any.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS index_location_point_sessionId")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_location_point_sessionId_timestamp " +
                "ON location_point (sessionId, timestamp)",
        )
    }
}

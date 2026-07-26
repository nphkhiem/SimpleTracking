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

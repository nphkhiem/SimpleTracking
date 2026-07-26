package com.khiemnph.simpletracking

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Enforces the one boundary claim `:data` makes about itself.
 *
 * `FusedLocationTrackingRepository`'s KDoc states that `android.location.Location` and
 * `com.google.android.gms.location.*` types never leak past that layer. They did: `:app` built its
 * own `LocationSettingsRequest` from a `:data` internal, handling Play Services location types
 * directly. Documented, not enforced, so it drifted.
 *
 * Two things are deliberately allowed, because the claim is about *collecting location*, not about
 * the word "location". `com.google.android.gms.maps` is a UI library and belongs in `:app`. And
 * `android.location.LocationManager` is used for exactly one thing here, `PROVIDERS_CHANGED_ACTION`,
 * a broadcast-action string that drives auto-pause. `android.location.Location` is the type whose
 * presence would mean `:app` had started handling fixes itself, so that one is matched exactly
 * rather than by prefix, or it would also catch `LocationManager`.
 */
class ModuleBoundaryTest {

    /** Whole packages `:app` must not reach into. */
    private val forbiddenPackages = listOf("com.google.android.gms.location.")

    /** Individual types, matched exactly so a longer class name starting the same way is not caught. */
    private val forbiddenClasses = listOf("android.location.Location")

    @Test
    fun `app never handles Play Services location types itself`() {
        val violations = sourceFiles().flatMap { file ->
            importsIn(file).filter(::isForbidden).map { "${file.name}: $it" }
        }

        assertEquals(
            "Location collection belongs behind `:data`. Add it to a `:data` abstraction instead.",
            emptyList<String>(),
            violations,
        )
    }

    @Test
    fun `the boundary check is actually reading source files`() {
        assertTrue("expected to find :app sources to scan", sourceFiles().size > 10)
    }

    @Test
    fun `the rule distinguishes the banned type from a longer name that merely starts the same way`() {
        assertTrue(isForbidden("android.location.Location"))
        assertTrue(isForbidden("com.google.android.gms.location.LocationRequest"))
        assertFalse(isForbidden("android.location.LocationManager"))
        assertFalse(isForbidden("com.google.android.gms.maps.GoogleMap"))
    }

    private fun isForbidden(import: String): Boolean =
        forbiddenPackages.any(import::startsWith) ||
            forbiddenClasses.any { import == it || import.startsWith("$it.") }

    private fun sourceFiles(): List<File> =
        File("src/main").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun importsIn(file: File): List<String> =
        file.readLines()
            .mapNotNull { line -> line.trim().removePrefix("import ").takeIf { line.trim().startsWith("import ") } }
            .map { it.substringBefore(" as ") }
}

package com.khiemnph.domain

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Enforces what `:domain` claims to be: a pure JVM module with no Android, Google or AndroidX types
 * in it.
 *
 * The architecture was documented but nothing checked it, so two violations reached `:app` before
 * an audit noticed. A KDoc promise that types never leak past a layer is worth exactly as much as
 * the test that fails when they do.
 */
class ModulePurityTest {

    private val forbiddenPrefixes = listOf(
        "android.",
        "androidx.",
        "com.google.",
        "com.khiemnph.data.",
        "com.khiemnph.simpletracking.",
    )

    @Test
    fun `domain imports nothing from Android, Google or the outer layers`() {
        val violations = sourceFiles().flatMap { file ->
            importsIn(file)
                .filter { import -> forbiddenPrefixes.any(import::startsWith) }
                .map { "${file.name}: $it" }
        }

        assertEquals("`:domain` must stay a pure JVM module", emptyList<String>(), violations)
    }

    @Test
    fun `the purity check is actually reading source files`() {
        // Guards the assertion above from passing because it found nothing to inspect.
        assertTrue("expected to find :domain sources to scan", sourceFiles().size > 10)
    }

    private fun sourceFiles(): List<File> =
        File("src/main").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun importsIn(file: File): List<String> =
        file.readLines()
            .mapNotNull { line -> line.trim().removePrefix("import ").takeIf { line.trim().startsWith("import ") } }
            .map { it.substringBefore(" as ") }
}
